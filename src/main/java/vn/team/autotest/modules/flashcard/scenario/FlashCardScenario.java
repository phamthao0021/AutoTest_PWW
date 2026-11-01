
package vn.team.autotest.modules.flashcard.scenario;

import java.io.OutputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.lang.reflect.Method;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.ITestResult;
import org.testng.annotations.*;

import vn.team.autotest.modules.flashcard.page.FlashCardPage;
import vn.team.autotest.modules.learn.page.LearnModePage;
import vn.team.autotest.modules.learn.page.LearnModePage.Topic;

public class FlashCardScenario {

	private static final String BASE = "https://learnenglish.plt.pro.vn/";

	private WebDriver driver;
	private WebDriverWait wait;
	private FlashCardPage flash;
	private Topic topic;

	private final List<String[]> rows = new ArrayList<>();
	private final Set<String> recorded = new HashSet<>();

	private String stage = "CHƯA KHỞI TẠO";
	private long startedAt;

	// ==================================================
	// 1. LOG CONSOLE
	// ==================================================

	private static void log(String message) {
		System.out.println("[PWW_AUTO_10] " + message);
	}

	private void step(String message) {
		stage = message;

		log("STEP: " + message + (driver == null ? "" : " | URL=" + driver.getCurrentUrl()));
	}

	// ==================================================
	// 2. SCREENSHOT KHI FAIL
	// ==================================================

	private String shot(String id) {

		try {
			Path folder = Paths.get("Flashcard_Fail_Screenshots");

			Files.createDirectories(folder);

			Path dest = folder.resolve(id + "_" + System.currentTimeMillis() + ".png");

			Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), dest);

			return dest.toAbsolutePath().toString();

		} catch (Exception e) {
			return "Không chụp được: " + e.getMessage();
		}
	}

	// ==================================================
	// 3. GHI LỖI TESTNG NẾU CHƯA RECORD
	// ==================================================

	@AfterMethod(alwaysRun = true)
	public void afterEach(ITestResult result) {

		String method = result.getMethod().getMethodName();

		String id = method.startsWith("KB_10_") && method.length() >= 9 ? method.substring(0, 9).replace('_', '-')
				: method;

		if (recorded.contains(id)) {
			return;
		}

		if (result.getStatus() == ITestResult.SUCCESS) {
			return;
		}

		String state = result.getStatus() == ITestResult.SKIP ? "SKIP" : "FAIL";

		Throwable cause = result.getThrowable();

		String msg = cause == null ? "Không rõ nguyên nhân" : String.valueOf(cause.getMessage());

		String proof = state.equals("FAIL") && driver != null ? shot(id) : "";

		rows.add(new String[] { id, method, "Bước bị dừng: " + stage, "Thực hiện trọn vẹn luồng Flashcard",
				"Lỗi: " + msg, state, proof });

		recorded.add(id);

		log(id + " " + state + " tại " + stage + " | " + msg + " | duration="
				+ String.format(java.util.Locale.US, "%.2fs", (System.nanoTime() - startedAt) / 1e9));
	}

	// ==================================================
	// 4. KHỞI TẠO CHROME VÀ ĐĂNG NHẬP USER
	// ==================================================

	@BeforeClass(alwaysRun = true)
	public void setup() {

		ChromeOptions options = new ChromeOptions();

		options.addArguments("--start-maximized", "--disable-notifications");

		driver = new ChromeDriver(options);

		wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		String user = System.getProperty("test.username", "").trim();

		String pass = System.getProperty("test.password", "").trim();

		Assert.assertFalse(user.isBlank() || pass.isBlank(),
				"Thiếu -Dtest.username và " + "-Dtest.password trong VM arguments");

		step("Mở trang chủ");

		driver.get(BASE);

		if (!driver.getCurrentUrl().contains("dashboard.php")) {

			step("Mở form đăng nhập");

			if (!driver.getCurrentUrl().contains("login.php")) {

				wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='login.php']"))).click();
			}

			step("Nhập thông tin tài khoản User");

			WebElement input = wait.until(d -> d
					.findElements(By.cssSelector("input[name='username']," + "input[name='email'],"
							+ "input[type='email']," + "input[type='text']"))
					.stream().filter(e -> e.isDisplayed() && e.isEnabled()).findFirst().orElse(null));

			input.clear();
			input.sendKeys(user);

			WebElement pwd = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));

			pwd.sendKeys(pass);

			step("Nhấn Đăng nhập");

			wait.until(ExpectedConditions
					.elementToBeClickable(By.cssSelector("button[type='submit']," + "input[type='submit']"))).click();
		}

		step("Chờ Dashboard User");

		wait.until(d -> d.getCurrentUrl().contains("dashboard.php"));

		log("LOGIN PASS: " + driver.getCurrentUrl());
	}

	// ==================================================
	// 5. MỞ TRANG LEARN
	// ==================================================

	private LearnModePage learn() {

		step("Mở danh sách chủ đề Learn");

		driver.get(BASE + "learn.php");

		LearnModePage p = new LearnModePage(driver);

		p.ready();

		log("Đã tải Học tập: " + driver.getCurrentUrl());

		return p;
	}

	// ==================================================
	// 6. CHỌN TOPIC TEST -> FLASHCARD
	// ==================================================

	private void open(Topic t, LearnModePage p) {

		topic = t;

		step("Chọn topic " + t.id + ": " + t.name);

		p.openTopic(t.id);

		log("Mode Panel: " + p.panelText());

		step("Chọn chế độ Flashcard");

		p.chooseMode("flashcard");

		step("Kiểm tra trang Flashcard đã mở");

		flash = new FlashCardPage(driver);

		// Chọn chế độ học lại toàn bộ bài.
		flash.chooseStartFromBeginning();

		flash.ready();

		// Đánh dấu đỏ thẻ đang kiểm thử.
		flash.highlightCard("Kiểm tra khởi tạo thẻ Flashcard");

		log("Flashcard READY: " + flash.current() + "/" + flash.total());
	}

	// ==================================================
	// 7. CHUẨN BỊ MỖI TEST CASE
	// ==================================================

	@BeforeMethod(alwaysRun = true)
	public void before(Method method) {

		startedAt = System.nanoTime();

		step("BẮT ĐẦU TC: " + method.getName());

		LearnModePage p = learn();

		boolean oneCard = method.getName().startsWith("KB_10_016");

		Topic t;

		if (oneCard) {

			// Tìm chủ đề có đúng 1 từ.
			t = p.topics().stream().filter(x -> x.count == 1).findFirst()
					.orElseThrow(() -> new SkipException("Không có chủ đề nào chứa đúng 1 từ"));

			log("KB-10-016: Chọn chủ đề 1 từ: " + t.name + " | ID=" + t.id + " | Total=" + t.count);

		} else {

			// Các test còn lại dùng chủ đề Test.
			t = p.getTestTopic();
		}

		open(t, p);

		flash.highlightCard("Đang kiểm tra: " + method.getName());
	}

	// ==================================================
	// 8. KẾT THÚC, XUẤT EXCEL
	// ==================================================

	@AfterClass(alwaysRun = true)
	public void teardown() {

		try {
			export();
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	private String pre() {

		return "1. Trang chủ → Login → Dashboard " + "→ Học tập → chọn chủ đề " + topic.name + ".\n"
				+ "2. Nhấn Flashcards → " + "Học lại từ đầu (Toàn bộ bài).\n";
	}

	// ==================================================
	// 9. GHI KẾT QUẢ TEST CASE
	// ==================================================

	private void record(String id, String title, String steps, String expected, String actual, Runnable verify) {

		String state = "PASS";
		Throwable err = null;

		try {
			verify.run();

		} catch (Throwable t) {

			state = "FAIL";
			err = t;

			actual += " | ERROR: " + t.getMessage();
		}

		String proof = state.equals("FAIL") ? shot(id) : "";

		rows.add(new String[] { id, title, steps, expected, actual, state, proof });

		recorded.add(id);

		System.out.println("\n" + id + " | " + title + "\nSTEPS:\n" + steps + "\nEXPECTED: " + expected + "\nACTUAL: "
				+ actual + "\nRESULT: " + state + " | DURATION: "
				+ String.format(java.util.Locale.US, "%.2fs", (System.nanoTime() - startedAt) / 1e9));

		if (err != null) {
			throw new AssertionError(err);
		}
	}

	// ==================================================
	// 10. XUẤT BẢNG TCS EXCEL
	// ==================================================

	private void export() {

		String filename = "Flashcard_TestResults_USER.xlsx";

		try (XSSFWorkbook book = new XSSFWorkbook()) {

			Sheet sheet = book.createSheet("PWW_AUTO_10");

			String[] heads = { "STT", "TCs ID", "Title Test", "Steps", "Expected", "Actual", "Result", "Screenshot" };

			Row h = sheet.createRow(0);

			CellStyle header = book.createCellStyle();

			header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());

			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			org.apache.poi.ss.usermodel.Font f = book.createFont();

			f.setBold(true);

			f.setColor(IndexedColors.WHITE.getIndex());

			header.setFont(f);

			CellStyle body = book.createCellStyle();
			body.setWrapText(true);

			for (int j = 0; j < heads.length; j++) {

				Cell c = h.createCell(j);

				c.setCellValue(heads[j]);
				c.setCellStyle(header);
			}

			int pass = 0;

			for (int i = 0; i < rows.size(); i++) {

				Row row = sheet.createRow(i + 1);

				row.setHeightInPoints(108);

				String[] values = rows.get(i);

				row.createCell(0).setCellValue(i + 1);

				for (int j = 0; j < values.length; j++) {

					Cell c = row.createCell(j + 1);

					c.setCellValue(values[j]);
					c.setCellStyle(body);
				}

				if (values[5].equals("PASS")) {
					pass++;
				}
			}

			int[] width = { 8, 22, 60, 75, 75, 90, 12, 60 };

			for (int i = 0; i < width.length; i++) {

				sheet.setColumnWidth(i, width[i] * 256);
			}

			sheet.createFreezePane(0, 1);

			try (OutputStream os = Files.newOutputStream(Paths.get(filename))) {

				book.write(os);
			}

			System.out.println("[PWW_AUTO_10] Rows=" + rows.size() + " PASS=" + pass + " FAIL=" + (rows.size() - pass));

		} catch (Exception e) {

			System.err.println("Lỗi xuất báo cáo: " + e.getMessage());
		}
	}

	// ==================================================
	// 11. HỖ TRỢ HOÀN THÀNH FLASHCARD
	// ==================================================

	private int[] finishMixed() {

		int total = flash.total();
		int yes = 0;
		int no = 0;

		for (int i = 0; i < total; i++) {

			if (i % 2 == 0) {
				flash.learned();
				yes++;

			} else {
				flash.unlearned();
				no++;
			}
		}

		Assert.assertTrue(flash.summaryVisible(), "Không xuất hiện Summary");

		return new int[] { yes, no, total };
	}

	private void finishAll(boolean learned) {

		int total = flash.total();

		for (int i = 0; i < total; i++) {

			if (learned) {
				flash.learned();
			} else {
				flash.unlearned();
			}
		}

		Assert.assertTrue(flash.summaryVisible(), "Không xuất hiện Summary");
	}

	// ==================================================
	// KB-10-001: INITIAL STATE
	// ==================================================

	@Test(priority = 1)
	public void KB_10_001_InitialState() {

		int current = flash.current();
		int total = flash.total();

		String w = flash.word();
		String ipa = flash.ipa();
		String difficulty = flash.difficulty();

		double p = flash.progress();

		record("KB-10-001", "Kiểm tra trạng thái khởi tạo Flashcard",

				pre() + "3. Đọc Card/Total/Progress.\n" + "4. Đọc Word/IPA/Difficulty.\n" + "5. Đối chiếu ban đầu.",

				"Card=1; Total>0; Progress từ 0–100; " + "Word khác rỗng",

				"Card=" + current + "/" + total + "; Progress=" + p + "; Word=" + w + "; IPA=" + ipa + "; Difficulty="
						+ difficulty,

				() -> {
					Assert.assertEquals(current, 1);
					Assert.assertTrue(total > 0);
					Assert.assertTrue(p >= 0 && p <= 100);
					Assert.assertFalse(w.isBlank());
				});
	}

	// ==================================================
	// KB-10-002: AUDIO
	// ==================================================

	@Test(priority = 2)
	public void KB_10_002_Audio() {

		int old = flash.current();
		String w = flash.word();
		double p = flash.progress();

		flash.audio();

		int now = flash.current();
		String w2 = flash.word();
		double p2 = flash.progress();

		record("KB-10-002", "Kiểm tra Audio không làm đổi thẻ và Progress",

				pre() + "3. Ghi Card/Word/Progress.\n" + "4. Nhấn Audio.\n" + "5. Đọc lại và so sánh.",

				"Không thay đổi Card, Word, Progress",

				"Card=" + old + "→" + now + "; Word=" + w + "→" + w2 + "; Progress=" + p + "→" + p2,

				() -> {
					Assert.assertEquals(now, old);
					Assert.assertEquals(w2, w);
					Assert.assertEquals(p2, p, 0.01);
				});
	}

	// ==================================================
	// KB-10-003: FLIP
	// ==================================================

	@Test(priority = 3)
	public void KB_10_003_Flip() {

		String w = flash.word();

		flash.flip();

		String meaning = flash.meaning();
		String example = flash.example();

		record("KB-10-003", "Kiểm tra lật thẻ hiển thị nghĩa và ví dụ",

				pre() + "3. Đọc Word.\n" + "4. Lật thẻ.\n" + "5. Đọc Meaning/Example.\n" + "6. Kiểm tra nội dung.",

				"Meaning và Example có nội dung",

				"Word=" + w + "; Meaning=" + meaning + "; Example=" + example,

				() -> {
					Assert.assertFalse(meaning.isBlank());
					Assert.assertFalse(example.isBlank());
				});
	}

	// ==================================================
	// KB-10-004: LEARNED + PROGRESS
	// ==================================================

	@Test(priority = 4)
	public void KB_10_004_LearnedProgress() {

		int a = flash.current();
		double p = flash.progress();

		flash.learned();

		int b = flash.current();
		double q = flash.progress();

		record("KB-10-004", "Kiểm tra Đã thuộc cập nhật thẻ và Progress",

				pre() + "3. Ghi Card/Progress.\n" + "4. Nhấn Đã thuộc.\n" + "5. Đọc và so sánh giá trị mới.",

				"Card tăng, Progress tăng",

				"Card=" + a + "→" + b + "; Progress=" + p + "→" + q,

				() -> {
					Assert.assertTrue(b > a);
					Assert.assertTrue(q > p);
				});
	}

	// ==================================================
	// KB-10-005: SUMMARY DYNAMIC
	// ==================================================

	@Test(priority = 5)
	public void KB_10_005_SummaryDynamic() {

		int[] counts = finishMixed();

		String actual = flash.summaryText();

		record("KB-10-005", "Kiểm tra Summary bằng bộ đếm độc lập của Selenium",

				pre() + "3. Học toàn bộ thẻ.\n" + "4. Đếm Đã thuộc/Chưa thuộc.\n" + "5. Mở Summary.\n"
						+ "6. Đối chiếu bộ đếm.",

				"Learned=" + counts[0] + ", Unlearned=" + counts[1] + ", Total=" + counts[2],

				actual,

				() -> {
					Assert.assertEquals(flash.summaryNumber("Đã thuộc"), counts[0]);

					Assert.assertEquals(flash.summaryNumber("Chưa thuộc"), counts[1]);

					Assert.assertEquals(counts[0] + counts[1], counts[2]);
				});
	}

	// ==================================================
	// KB-10-006: RESTART
	// ==================================================

	@Test(priority = 6)
	public void KB_10_006_Restart() {

		finishAll(true);

		flash.restart();

		int current = flash.current();
		double progress = flash.progress();

		record("KB-10-006", "Kiểm tra khởi động lại phiên Flashcard",

				pre() + "3. Hoàn thành phiên.\n" + "4. Nhấn Học lại.\n" + "5. Kiểm tra Card/Progress.",

				"Card=1; Progress ở trạng thái đầu",

				"Card=" + current + "; Progress=" + progress,

				() -> {
					Assert.assertEquals(current, 1);
					Assert.assertTrue(progress >= 0 && progress <= 100);
				});
	}

	// ==================================================
	// KB-10-007: FLIP TWICE
	// ==================================================

	@Test(priority = 7)
	public void KB_10_007_FlipTwice() {

		String word = flash.word();
		int index = flash.current();

		flash.flip();
		flash.flip();

		String actualWord = flash.word();
		int actualIndex = flash.current();

		record("KB-10-007", "Kiểm tra lật hai lần không đổi từ hoặc thẻ",

				pre() + "3. Ghi Word/Card.\n" + "4. Lật hai lần.\n" + "5. So sánh trạng thái.",

				"Word/Card giữ nguyên",

				"Word=" + actualWord + "; Card=" + actualIndex,

				() -> {
					Assert.assertEquals(actualWord, word);
					Assert.assertEquals(actualIndex, index);
				});
	}

	// ==================================================
	// KB-10-008: UNLEARNED
	// ==================================================

	@Test(priority = 8)
	public void KB_10_008_Unlearned() {

		int a = flash.current();

		flash.unlearned();

		int b = flash.current();

		record("KB-10-008", "Kiểm tra Chưa thuộc chuyển đúng sang thẻ tiếp",

				pre() + "3. Ghi chỉ số thẻ.\n" + "4. Nhấn Chưa thuộc.\n" + "5. So sánh chỉ số mới.",

				"Card tiếp theo tăng",

				"Card=" + a + "→" + b,

				() -> Assert.assertTrue(b > a));
	}

	// ==================================================
	// KB-10-009: MULTI PROGRESS
	// ==================================================

	@Test(priority = 9)
	public void KB_10_009_ProgressMulti() {

		int n = Math.min(3, flash.total() - 1);

		if (n < 2) {
			throw new SkipException("Cần tối thiểu 3 thẻ");
		}

		List<Double> values = new ArrayList<>();

		values.add(flash.progress());

		for (int i = 0; i < n; i++) {
			flash.learned();
			values.add(flash.progress());
		}

		record("KB-10-009", "Kiểm tra Progress tăng sau nhiều thẻ",

				pre() + "3. Ghi Progress.\n" + "4. Học nhiều thẻ.\n" + "5. Ghi mỗi lượt.\n"
						+ "6. Kiểm tra tăng và <=100%.",

				"Progress tăng, không vượt 100%",

				values.toString(),

				() -> {
					for (int i = 1; i < values.size(); i++) {

						Assert.assertTrue(values.get(i) > values.get(i - 1));

						Assert.assertTrue(values.get(i) <= 100);
					}
				});
	}

	// ==================================================
	// KB-10-010: FINAL CARD
	// ==================================================

	@Test(priority = 10)
	public void KB_10_010_FinalCard() {

		int total = flash.total();

		for (int i = 1; i < total; i++) {
			flash.learned();
		}

		int before = flash.current();

		flash.learned();

		boolean summary = flash.summaryVisible();

		record("KB-10-010", "Kiểm tra thao tác trên thẻ cuối mở Summary",

				pre() + "3. Học đến thẻ cuối.\n" + "4. Ghi Card cuối.\n" + "5. Nhấn Đã thuộc.\n"
						+ "6. Kiểm tra Summary.",

				"Card cuối=Total, Summary hiển thị",

				"Card=" + before + "/" + total + "; Summary=" + summary,

				() -> {
					Assert.assertEquals(before, total);
					Assert.assertTrue(summary);
				});
	}

	// ==================================================
	// KB-10-011: ALL LEARNED
	// ==================================================

	@Test(priority = 11)
	public void KB_10_011_AllLearned() {

		int total = flash.total();

		finishAll(true);

		String text = flash.summaryText();

		record("KB-10-011", "Kiểm tra tất cả các thẻ đều Đã thuộc",

				pre() + "3. Chọn Đã thuộc cho tất cả.\n" + "4. Mở Summary.\n" + "5. Đối chiếu bộ đếm.",

				"Đã thuộc=" + total + ", Chưa thuộc=0",

				text,

				() -> {
					Assert.assertEquals(flash.summaryNumber("Đã thuộc"), total);

					Assert.assertEquals(flash.summaryNumber("Chưa thuộc"), 0);
				});
	}

	// ==================================================
	// KB-10-012: ALL UNLEARNED
	// ==================================================

	@Test(priority = 12)
	public void KB_10_012_AllUnlearned() {

		int total = flash.total();

		finishAll(false);

		String text = flash.summaryText();

		record("KB-10-012", "Kiểm tra tất cả các thẻ đều Chưa thuộc",

				pre() + "3. Chọn Chưa thuộc cho tất cả.\n" + "4. Mở Summary.\n" + "5. Đối chiếu bộ đếm.",

				"Đã thuộc=0, Chưa thuộc=" + total,

				text,

				() -> {
					Assert.assertEquals(flash.summaryNumber("Đã thuộc"), 0);

					Assert.assertEquals(flash.summaryNumber("Chưa thuộc"), total);
				});
	}

	// ==================================================
	// KB-10-013: MIXED
	// ==================================================

	@Test(priority = 13)
	public void KB_10_013_Mixed() {

		int[] count = finishMixed();

		String text = flash.summaryText();

		record("KB-10-013", "Kiểm tra xen kẽ Đã thuộc và Chưa thuộc",

				pre() + "3. Đánh dấu xen kẽ.\n" + "4. Selenium tự đếm.\n" + "5. Đối chiếu Summary.",

				"Learned=" + count[0] + ", Unlearned=" + count[1],

				text,

				() -> {
					Assert.assertEquals(flash.summaryNumber("Đã thuộc"), count[0]);

					Assert.assertEquals(flash.summaryNumber("Chưa thuộc"), count[1]);
				});
	}

	// ==================================================
	// KB-10-014: SUMMARY STABILITY
	// ==================================================

	@Test(priority = 14)
	public void KB_10_014_StableSummary() {

		finishAll(true);

		String a = flash.summaryText();
		String b = flash.summaryText();

		record("KB-10-014", "Kiểm tra Summary không tự thay đổi",

				pre() + "3. Hoàn thành phiên.\n" + "4. Đọc Summary hai lần.\n" + "5. So sánh.",

				"Hai lần đọc trùng nhau",

				"Lần 1=" + a + "; Lần 2=" + b,

				() -> Assert.assertEquals(b, a));
	}

	// ==================================================
	// KB-10-015: RESTART INDEPENDENT
	// ==================================================

	@Test(priority = 15)
	public void KB_10_015_RestartIndependent() {

		finishMixed();

		flash.restart();

		int first = flash.current();
		String word = flash.word();

		flash.learned();

		int next = flash.current();

		record("KB-10-015", "Kiểm tra phiên mới sau Restart hoạt động độc lập",

				pre() + "3. Hoàn thành phiên.\n" + "4. Restart.\n" + "5. Kiểm tra Card đầu.\n" + "6. Học một thẻ mới.",

				"Restart về 1 và chuyển thẻ bình thường",

				"First=" + first + ", Word=" + word + ", Next=" + next,

				() -> {
					Assert.assertEquals(first, 1);
					Assert.assertFalse(word.isBlank());
					Assert.assertTrue(next > first);
				});
	}

	// ==================================================
	// KB-10-016: ONE CARD
	// ==================================================

	@Test(priority = 16)
	public void KB_10_016_OneCard() {

		int current = flash.current();
		int total = flash.total();
		double initialProgress = flash.progress();

		String word = flash.word();

		Assert.assertEquals(total, 1, "Chủ đề được chọn phải có đúng một từ");

		flash.learned();

		boolean summary = flash.summaryVisible();

		int learned = flash.summaryNumber("Đã thuộc");
		int unlearned = flash.summaryNumber("Chưa thuộc");

		record("KB-10-016", "Kiểm tra Flashcard của chủ đề có đúng một từ",

				pre() + "3. Chọn chủ đề có đúng một từ.\n" + "4. Kiểm tra Card 1/1 và từ vựng.\n"
						+ "5. Nhấn Đã thuộc.\n" + "6. Kiểm tra Summary và bộ đếm.",

				"Card=1/1; từ vựng không rỗng; " + "Summary hiển thị; " + "Đã thuộc=1; Chưa thuộc=0.",

				"Topic=" + topic.name + "; Card=" + current + "/" + total + "; Word=" + word + "; InitialProgress="
						+ initialProgress + "; Summary=" + summary + "; Learned=" + learned + "; Unlearned="
						+ unlearned,

				() -> {
					Assert.assertEquals(current, 1);
					Assert.assertEquals(total, 1);
					Assert.assertFalse(word.isBlank());
					Assert.assertTrue(summary);

					Assert.assertEquals(learned, 1);
					Assert.assertEquals(unlearned, 0);
				});
	}

}
