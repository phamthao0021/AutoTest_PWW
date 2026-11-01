
package vn.team.autotest.modules.learn.scenario;

import java.io.OutputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;

import vn.team.autotest.modules.learn.page.LearnModePage;
import vn.team.autotest.modules.flashcard.page.FlashCardPage;

/**
 * PWW_AUTO_09 - Learn / Flashcard / Quiz / Match 19 test cases, có kiểm tra
 * popup xác nhận thoát.
 */
public class LearnScenario9 {

	private static final String BASE = "https://learnenglish.plt.pro.vn/";

	private WebDriver driver;
	private WebDriverWait wait;
	private LearnModePage learn;
	private FlashCardPage flash;

	private final LinkedHashMap<String, String[]> specs = new LinkedHashMap<>();

	private final LinkedHashMap<String, String[]> results = new LinkedHashMap<>();

	@FunctionalInterface
	private interface CaseAction {
		String execute() throws Exception;
	}

	public LearnScenario9() {

		add("KB-09-001", "Kiểm tra tiêu đề đầy đủ trên tất cả card chủ đề",
				"1. Trang chủ > Đăng nhập > Dashboard > Học tập.\n" + "2. Đọc các card trong #categoryGrid.\n"
						+ "3. Cuộn từng card và đọc tiêu đề DOM.\n" + "4. Kiểm tra tiêu đề không rỗng, không bị cắt.\n"
						+ "5. Kiểm tra chủ đề Test (5 từ).",
				"Tên card đầy đủ, không bị cắt; Test có 5 từ.");

		add("KB-09-002", "Chọn card Test và kiểm tra 3 chế độ học", "1. Mở Learn.\n2. Chọn Test.\n"
				+ "3. Kiểm tra panel inline.\n" + "4. Kiểm tra Flashcards, Quiz, Match.",
				"Panel Test hiển thị đủ ba chế độ.");

		add("KB-09-003", "Kiểm tra hộp lựa chọn cách học Flashcard", "1. Chọn Test > Flashcards.\n"
				+ "2. Kiểm tra hộp Lựa chọn chế độ học.\n" + "3. Kiểm tra ba phương án học.",
				"Có đủ ba lựa chọn học Flashcard.");

		add("KB-09-004", "Kiểm tra trạng thái Flashcard ban đầu",
				"1. Chọn Học lại từ đầu.\n" + "2. Kiểm tra Card 1/5.\n" + "3. Kiểm tra Progress 0% và các nút.",
				"Card 1/5, Progress 0%, đủ thành phần.");

		add("KB-09-005", "Kiểm tra nút Nghe phát âm",
				"1. Đọc Card và Progress.\n" + "2. Nhấn Audio.\n" + "3. So sánh trạng thái trước/sau.",
				"Card và Progress không thay đổi.");

		add("KB-09-006", "Kiểm tra lật Flashcard",
				"1. Đọc Card.\n2. Lật thẻ.\n" + "3. Kiểm tra nghĩa và ví dụ.\n" + "4. Kiểm tra chỉ số Card.",
				"Có nghĩa và ví dụ; Card không đổi.");

		add("KB-09-007", "Kiểm tra nút Chưa thuộc", "1. Đọc Card.\n2. Nhấn Chưa thuộc.\n" + "3. Kiểm tra chuyển thẻ.",
				"Card tăng một đơn vị.");

		add("KB-09-008", "Kiểm tra nút Đã thuộc", "1. Đọc Card.\n2. Nhấn Đã thuộc.\n" + "3. Kiểm tra chuyển thẻ.",
				"Card tăng một đơn vị.");

		add("KB-09-009", "Kiểm tra Kết thúc bài học và Summary",
				"1. Học một thẻ.\n2. Kết thúc bài học.\n" + "3. Kiểm tra XP, Từ thuộc, Từ chưa thuộc.",
				"Summary hiển thị các chỉ số.");

		add("KB-09-010", "Kiểm tra Học lại từ đầu",
				"1. Kết thúc phiên.\n2. Nhấn Học lại.\n" + "3. Kiểm tra Card và Progress.",
				"Card về 1/5 và Progress 0%.");

		add("KB-09-011", "Quay lại danh sách sau khi kết thúc",
				"1. Kết thúc bài.\n" + "2. Nhấn Quay lại danh sách.\n" + "3. Kiểm tra Learn và Test.",
				"Trở về learn.php, Test còn tồn tại.");

		add("KB-09-012", "Flashcard – Quay về khi chưa kết thúc",
				"1. Mở Flashcard.\n2. Nhấn mũi tên Quay về.\n" + "3. Xác nhận Thoát ngay.\n" + "4. Kiểm tra Learn.",
				"Popup được xác nhận; trở về Learn.");

		add("KB-09-013", "Quiz – Quay về khi chưa kết thúc",
				"1. Mở Quiz.\n2. Nhấn mũi tên Quay về.\n" + "3. Nhấn Xác nhận thoát.\n" + "4. Kiểm tra Learn.",
				"Popup được xác nhận; trở về Learn.");

		add("KB-09-014", "Match – Quay về khi chưa kết thúc",
				"1. Mở Match.\n2. Nhấn mũi tên Quay về.\n" + "3. Nhấn Xác nhận thoát.\n" + "4. Kiểm tra Learn.",
				"Popup được xác nhận; trở về Learn.");

		add("KB-09-015", "Flashcard – Sidebar Học tập",
				"1. Mở Flashcard.\n2. Nhấn Sidebar Học tập.\n" + "3. Nhấn Thoát ngay.\n" + "4. Kiểm tra Learn.",
				"Xác nhận thoát và trở về Learn.");

		add("KB-09-016", "Quiz – Sidebar Học tập",
				"1. Mở Quiz.\n2. Nhấn Sidebar Học tập.\n" + "3. Nhấn Xác nhận thoát.\n" + "4. Kiểm tra Learn.",
				"Xác nhận thoát và trở về Learn.");

		add("KB-09-017", "Match – Sidebar Học tập",
				"1. Mở Match.\n2. Nhấn Sidebar Học tập.\n" + "3. Nhấn Xác nhận thoát.\n" + "4. Kiểm tra Learn.",
				"Xác nhận thoát và trở về Learn.");

		add("KB-09-018", "Quiz – Kết thúc khi chưa chọn đáp án",
				"1. Mở Quiz.\n2. Không chọn đáp án.\n" + "3. Nhấn Kết thúc bài học.\n" + "4. Kiểm tra Summary.",
				"Có kết quả XP, trả lời đúng và trả lời sai.");

		add("KB-09-019", "Match – Kết thúc bài và kiểm tra Summary", "1. Mở Match.\n2. Không ghép cặp.\n"
				+ "3. Nhấn Hoàn thành bài tập.\n" + "4. Kiểm tra kết quả và điều hướng.",
				"Có kết quả đúng/sai, XP và các nút.");
	}

	private void add(String id, String title, String steps, String expected) {

		specs.put(id, new String[] { title, steps, expected });
	}

	// ==================================================
	// 1. SETUP / LOGIN
	// ==================================================

	@BeforeClass(alwaysRun = true)
	public void setup() {

		String user = System.getProperty("test.username", "").trim();

		String password = System.getProperty("test.password", "").trim();

		Assert.assertFalse(user.isEmpty() || password.isEmpty(), "Cần -Dtest.username và -Dtest.password");

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--start-maximized");

		options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);

		driver = new ChromeDriver(options);

		wait = new WebDriverWait(driver, Duration.ofSeconds(18));

		driver.get(BASE);

		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='login.php']"))).click();

		WebElement input = wait
				.until(d -> d
						.findElements(By.cssSelector("input[name='username']," + "input[name='email'],"
								+ "input[type='email']," + "input[type='text']"))
						.stream().filter(WebElement::isDisplayed).findFirst().orElse(null));

		input.sendKeys(user);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")))
				.sendKeys(password);

		wait.until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit'],input[type='submit']")))
				.click();

		wait.until(d -> d.getCurrentUrl().contains("dashboard.php"));

		learn = new LearnModePage(driver);
		flash = new FlashCardPage(driver);

		System.out.println("[SC09] LOGIN PASS -> " + driver.getCurrentUrl());
	}

	// ==================================================
	// 2. NAVIGATION
	// ==================================================

	private void gotoLearn() {

		driver.get(BASE + "dashboard.php");

		WebElement link = wait.until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='learn.php'],a[href$='/learn.php']")));

		link.click();

		wait.until(d -> d.getCurrentUrl().contains("learn.php"));

		learn.ready();

		System.out.println("[SC09] Dashboard -> Learn -> " + driver.getCurrentUrl());
	}

	private void openFlashFromBeginning() {

		gotoLearn();

		learn.openTestTopic();
		learn.chooseMode("flashcard");

		flash.chooseStartFromBeginning();
	}

	private void openModeUnfinished(String mode) {

		gotoLearn();

		learn.openTestTopic();
		learn.chooseMode(mode);

		if ("flashcard".equals(mode)) {

			flash.chooseStartFromBeginning();

			Assert.assertEquals(flash.current(), 1);

		} else if ("quiz".equals(mode)) {

			flash.startQuiz();

		} else if ("match".equals(mode)) {

			flash.startMatch();
		}

		Assert.assertFalse(flash.hasText("Hoàn thành bài học!"), "Chưa học nhưng đã có Summary");
	}

	private String confirmLearn(String source) {

		Assert.assertTrue(flash.inLearn(), "Không quay về trang Learn");

		LearnModePage.Topic t = learn.getTestTopic();

		Assert.assertEquals(t.name, "Test");

		return "Source=" + source + "; URL=" + driver.getCurrentUrl() + "; Topic=" + t;
	}

	// ==================================================
	// 3. SCREENSHOT AND REPORT
	// ==================================================

	private String screenshot(String id) {

		try {
			Path folder = Paths.get("SC09_Screenshots");
			Files.createDirectories(folder);

			Path dest = folder.resolve(id + "_" + System.currentTimeMillis() + ".png");

			Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), dest);

			return dest.toAbsolutePath().toString();

		} catch (Exception ex) {
			return "Screenshot error: " + ex.getMessage();
		}
	}

	private void check(String id, CaseAction action) throws Exception {

		String[] spec = specs.get(id);

		if (spec == null) {
			throw new IllegalArgumentException("Không có TC: " + id);
		}

		String state = "PASS";
		String actual;
		String screenshot = "";
		Throwable failure = null;

		try {
			actual = action.execute();

		} catch (Throwable ex) {

			state = "FAIL";

			actual = ex.getClass().getSimpleName() + ": " + ex.getMessage() + "; URL="
					+ (driver == null ? "" : driver.getCurrentUrl());

			screenshot = driver == null ? "" : screenshot(id);

			failure = ex;
		}

		results.put(id, new String[] { id, spec[0], spec[1], spec[2], actual, state, screenshot });

		System.out.println("\n" + id + " | " + spec[0] + "\nSTEPS:\n" + spec[1] + "\nEXPECTED: " + spec[2]
				+ "\nACTUAL: " + actual + "\nRESULT: " + state);

		if (failure != null) {
			throw new AssertionError("TC " + id + " FAIL: " + actual, failure);
		}
	}

	// ==================================================
	// KB-09-001
	// ==================================================

	@Test(priority = 1)
	public void TEST_01_FullTitlesOnCards() throws Exception {

		check("KB-09-001", () -> {

			gotoLearn();

			List<WebElement> cards = driver.findElements(By.cssSelector("#categoryGrid .cat-card"));

			Assert.assertFalse(cards.isEmpty());

			List<String> names = new ArrayList<>();

			for (WebElement card : cards) {

				WebElement title = card.findElements(By.cssSelector("h3,.cat-title,.category-title")).stream()
						.findFirst().orElseThrow(() -> new AssertionError("Card không có tiêu đề"));

				((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(" + "{block:'center'});",
						title);

				String text = title.getText().trim();

				Assert.assertFalse(text.isBlank());

				Boolean clipped = (Boolean) ((JavascriptExecutor) driver).executeScript("let e=arguments[0],"
						+ "s=getComputedStyle(e);" + "return " + "e.scrollWidth>e.clientWidth+2&&"
						+ "(s.overflow==='hidden'||" + "s.textOverflow==='ellipsis'||" + "s.whiteSpace==='nowrap');",
						title);

				Assert.assertFalse(clipped, "Tiêu đề bị cắt: " + text);

				names.add(text);
			}

			LearnModePage.Topic test = learn.getTestTopic();

			Assert.assertEquals(test.count, 5);
			Assert.assertTrue(names.contains("Test"));

			return "Cards=" + cards.size() + "; Titles=" + names + "; Test=" + test;
		});
	}

	// ==================================================
	// KB-09-002
	// ==================================================

	@Test(priority = 2)
	public void KB_09_002_ModePanel() throws Exception {

		check("KB-09-002", () -> {

			gotoLearn();
			learn.openTestTopic();

			Assert.assertTrue(learn.panelVisible());

			for (String mode : List.of("flashcard", "quiz", "match")) {

				Assert.assertTrue(learn.modeAvailable(mode), "Thiếu: " + mode);
			}

			return "Topic=" + learn.getTestTopic() + "; Panel=" + learn.panelText();
		});
	}

	// ==================================================
	// KB-09-003
	// ==================================================

	@Test(priority = 3)
	public void KB_09_003_FlashModes() throws Exception {

		check("KB-09-003", () -> {

			gotoLearn();
			learn.openTestTopic();
			learn.chooseMode("flashcard");

			wait.until(d -> flash.hasText("Lựa chọn chế độ học"));

			for (String option : List.of("Học tiếp tục", "Chỉ học những từ chưa thuộc", "Học lại từ đầu")) {

				Assert.assertTrue(flash.hasText(option), "Thiếu: " + option);
			}

			return "Đầy đủ 3 lựa chọn Flashcard";
		});
	}

	// ==================================================
	// KB-09-004
	// ==================================================

	@Test(priority = 4)
	public void KB_09_004_Initial() throws Exception {

		check("KB-09-004", () -> {

			openFlashFromBeginning();

			int[] card = flash.cardIndex();
			double progress = flash.progress();

			Assert.assertEquals(card[0], 1);
			Assert.assertEquals(card[1], 5);
			Assert.assertEquals(progress, 0.0, 0.01);

			for (String name : List.of("Flashcards", "Nghe phát âm", "Đã thuộc", "Chưa thuộc")) {

				Assert.assertTrue(flash.hasText(name));
			}

			return "Card=" + card[0] + "/" + card[1] + "; Progress=" + progress + "; Header=" + flash.header();
		});
	}

	// ==================================================
	// KB-09-005
	// ==================================================

	@Test(priority = 5)
	public void KB_09_005_Audio() throws Exception {

		check("KB-09-005", () -> {

			openFlashFromBeginning();

			int[] before = flash.cardIndex();
			double p1 = flash.progress();

			flash.audio();

			int[] after = flash.cardIndex();
			double p2 = flash.progress();

			Assert.assertEquals(after[0], before[0]);
			Assert.assertEquals(p2, p1, 0.01);

			return "Card=" + before[0] + " -> " + after[0] + "; Progress=" + p1 + " -> " + p2;
		});
	}

	// ==================================================
	// KB-09-006
	// ==================================================

	@Test(priority = 6)
	public void KB_09_006_Flip() throws Exception {

		check("KB-09-006", () -> {

			openFlashFromBeginning();

			int old = flash.current();

			flash.flip();

			String back = flash.backFaceText();

			Assert.assertTrue(back.contains("Ý NGHĨA & VÍ DỤ"));

			Assert.assertTrue(back.length() > "Ý NGHĨA & VÍ DỤ".length() + 5);

			Assert.assertEquals(flash.current(), old);

			return "Card=" + old + "; Back=" + back;
		});
	}

	// ==================================================
	// KB-09-007
	// ==================================================

	@Test(priority = 7)
	public void KB_09_007_Unlearned() throws Exception {

		check("KB-09-007", () -> {

			openFlashFromBeginning();

			int before = flash.current();

			flash.unlearned();

			wait.until(d -> flash.current() > before);

			Assert.assertEquals(flash.current(), before + 1);

			return "Chưa thuộc: " + before + " -> " + flash.current();
		});
	}

	// ==================================================
	// KB-09-008
	// ==================================================

	@Test(priority = 8)
	public void KB_09_008_Learned() throws Exception {

		check("KB-09-008", () -> {

			openFlashFromBeginning();

			int before = flash.current();

			flash.learned();

			wait.until(d -> flash.current() > before);

			Assert.assertEquals(flash.current(), before + 1);

			return "Đã thuộc: " + before + " -> " + flash.current();
		});
	}

	// ==================================================
	// KB-09-009
	// ==================================================

	@Test(priority = 9)
	public void KB_09_009_Finish() throws Exception {

		check("KB-09-009", () -> {

			openFlashFromBeginning();

			flash.learned();
			flash.finish();

			String summary = flash.summary();

			for (String label : List.of("Hoàn thành bài học!", "XP NHẬN ĐƯỢC", "TỪ THUỘC", "TỪ CHƯA THUỘC")) {

				Assert.assertTrue(summary.contains(label), "Thiếu: " + label);
			}

			return "Summary=" + summary;
		});
	}

	// ==================================================
	// KB-09-010
	// ==================================================

	@Test(priority = 10)
	public void KB_09_010_Restart() throws Exception {

		check("KB-09-010", () -> {

			openFlashFromBeginning();

			flash.finish();
			flash.restart();

			Assert.assertEquals(flash.current(), 1);
			Assert.assertEquals(flash.progress(), 0.0, 0.01);

			return "Card=" + flash.current() + "/" + flash.total() + "; Progress=" + flash.progress();
		});
	}

	// ==================================================
	// KB-09-011
	// ==================================================

	@Test(priority = 11)
	public void KB_09_011_ReturnAfterFinish() throws Exception {

		check("KB-09-011", () -> {

			openFlashFromBeginning();

			flash.finish();
			flash.backToLearn();

			Assert.assertTrue(flash.inLearn());

			Assert.assertEquals(learn.getTestTopic().name, "Test");

			return "Returned=" + driver.getCurrentUrl() + "; Topic=" + learn.getTestTopic();
		});
	}

	// ==================================================
	// KB-09-012: FLASHCARD BACK + CONFIRM
	// ==================================================

	@Test(priority = 12)
	public void KB_09_012_FlashcardBackArrow() throws Exception {

		check("KB-09-012", () -> {

			openModeUnfinished("flashcard");

			// Page Object nhấn Back + "Thoát ngay".
			flash.backArrow();

			return confirmLearn("Flashcard Back + Thoát ngay");
		});
	}

	// ==================================================
	// KB-09-013: QUIZ BACK + CONFIRM
	// ==================================================

	@Test(priority = 13)
	public void KB_09_013_QuizBackArrow() throws Exception {

		check("KB-09-013", () -> {

			openModeUnfinished("quiz");

			// Page Object nhấn Back + "Xác nhận thoát".
			flash.backArrow();

			return confirmLearn("Quiz Back + Xác nhận thoát");
		});
	}

	// ==================================================
	// KB-09-014: MATCH BACK + CONFIRM
	// ==================================================

	@Test(priority = 14)
	public void KB_09_014_MatchBackArrow() throws Exception {

		check("KB-09-014", () -> {

			openModeUnfinished("match");

			// Page Object nhấn Back + "Xác nhận thoát".
			flash.backArrow();

			return confirmLearn("Match Back + Xác nhận thoát");
		});
	}

	// ==================================================
	// KB-09-015: FLASHCARD SIDEBAR + CONFIRM
	// ==================================================

	@Test(priority = 15)
	public void KB_09_015_FlashcardSidebar() throws Exception {

		check("KB-09-015", () -> {

			openModeUnfinished("flashcard");

			flash.clickLearnSidebar();

			return confirmLearn("Flashcard Sidebar + Thoát ngay");
		});
	}

	// ==================================================
	// KB-09-016: QUIZ SIDEBAR + CONFIRM
	// ==================================================

	@Test(priority = 16)
	public void KB_09_016_QuizSidebar() throws Exception {

		check("KB-09-016", () -> {

			openModeUnfinished("quiz");

			flash.clickLearnSidebar();

			return confirmLearn("Quiz Sidebar + Xác nhận thoát");
		});
	}

	// ==================================================
	// KB-09-017: MATCH SIDEBAR + CONFIRM
	// ==================================================

	@Test(priority = 17)
	public void KB_09_017_MatchSidebar() throws Exception {

		check("KB-09-017", () -> {

			openModeUnfinished("match");

			flash.clickLearnSidebar();

			return confirmLearn("Match Sidebar + Xác nhận thoát");
		});
	}

	// ==================================================
	// KB-09-018: QUIZ FINISH
	// ==================================================

	@Test(priority = 18)
	public void KB_09_018_QuizEndWithoutAnswers() throws Exception {

		check("KB-09-018", () -> {

			openModeUnfinished("quiz");

			flash.clickText("Kết thúc bài học");

			wait.until(d -> flash.hasText("Chúc mừng bạn đã hoàn thành bài học!"));

			String summary = driver.findElement(By.tagName("body")).getText();

			Assert.assertTrue(summary.contains("TRẢ LỜI ĐÚNG"));

			Assert.assertTrue(summary.contains("TRẢ LỜI SAI"));

			return "Quiz Summary=" + summary;
		});
	}

	// ==================================================
	// KB-09-019: MATCH FINISH
	// ==================================================

	@Test(priority = 19)
	public void KB_09_019_MatchFinish() throws Exception {

		check("KB-09-019", () -> {

			openModeUnfinished("match");

			Assert.assertTrue(flash.hasText("Đã ghép 0 /"));

			flash.clickText("Hoàn thành bài tập");

			wait.until(d -> flash.hasText("CÂU ĐÚNG") && flash.hasText("CÂU SAI"));

			String result = driver.findElement(By.tagName("body")).getText();

			for (String label : List.of("Học lại các từ đã sai ngay", "Làm bài tập mới", "Chọn chủ đề khác")) {

				Assert.assertTrue(result.contains(label), "Thiếu nút: " + label);
			}

			return "Match Summary=" + result;
		});
	}

	// ==================================================
	// 4. EXPORT EXCEL
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

	private void export() {

		try (XSSFWorkbook wb = new XSSFWorkbook()) {

			Sheet sheet = wb.createSheet("PWW_AUTO_09_TCs");

			String[] headers = { "STT", "TCs ID", "Title Test", "Steps", "Expected", "Actual", "Result", "Screenshot" };

			CellStyle headerStyle = wb.createCellStyle();

			headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());

			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			org.apache.poi.ss.usermodel.Font font = wb.createFont();

			font.setBold(true);
			font.setColor(IndexedColors.WHITE.getIndex());

			headerStyle.setFont(font);

			CellStyle bodyStyle = wb.createCellStyle();
			bodyStyle.setWrapText(true);
			bodyStyle.setVerticalAlignment(VerticalAlignment.TOP);

			Row header = sheet.createRow(0);

			for (int j = 0; j < headers.length; j++) {

				Cell cell = header.createCell(j);

				cell.setCellValue(headers[j]);
				cell.setCellStyle(headerStyle);
			}

			int rowIndex = 0;
			int passed = 0;
			int failed = 0;

			for (Map.Entry<String, String[]> entry : specs.entrySet()) {

				String id = entry.getKey();
				String[] spec = entry.getValue();
				String[] actual = results.get(id);

				String[] fields = { String.valueOf(++rowIndex), id, spec[0], spec[1], spec[2],
						actual == null ? "Chưa chạy" : actual[4], actual == null ? "NOT RUN" : actual[5],
						actual == null ? "" : actual[6] };

				Row row = sheet.createRow(rowIndex);

				row.setHeightInPoints(110);

				for (int j = 0; j < fields.length; j++) {

					Cell cell = row.createCell(j);

					cell.setCellValue(fields[j]);
					cell.setCellStyle(bodyStyle);
				}

				if ("PASS".equals(fields[6])) {
					passed++;
				} else if ("FAIL".equals(fields[6])) {
					failed++;
				}
			}

			int[] widths = { 8, 17, 56, 82, 75, 90, 15, 55 };

			for (int j = 0; j < widths.length; j++) {

				sheet.setColumnWidth(j, widths[j] * 256);
			}

			sheet.createFreezePane(0, 1);

			Path output = Paths.get("LearnScenario9_Flashcard_TCs.xlsx");

			try (OutputStream out = Files.newOutputStream(output)) {

				wb.write(out);
			}

			System.out.println("[EXCEL] " + output.toAbsolutePath() + " | PASS=" + passed + " FAIL=" + failed
					+ " NOT RUN=" + (specs.size() - passed - failed));

		} catch (Exception e) {

			System.err.println("Lỗi xuất Excel: " + e.getMessage());
		}
	}
}
