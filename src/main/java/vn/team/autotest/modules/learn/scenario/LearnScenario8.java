
package vn.team.autotest.modules.learn.scenario;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;

import vn.team.autotest.modules.learn.page.LearnFilterPage;
import vn.team.autotest.modules.learn.page.LearnFilterPage.Topic;

/**
 * PWW_AUTO_08 – USER ONLY.
 *
 * KB-08-000: kiểm tra giao diện ban đầu. KB-08-001..006: 6 test case chính.
 * KB-08-007..017: 11 nhóm kiểm thử mở rộng.
 *
 * Không đọc CSV/Excel đầu vào. Vẫn xuất Excel kết quả với đầy đủ: Test Case ID,
 * Tên test, Steps, Expected, Actual, Result.
 */
public class LearnScenario8 {

	private static final String BASE = "https://learnenglish.plt.pro.vn/";

	private WebDriver driver;
	private WebDriverWait wait;
	private LearnFilterPage page;

	private final List<Result> results = new ArrayList<>();
	private List<Topic> baseline = List.of();

	private static final List<String> COLORS = List.of("purple", "pink", "green", "indigo", "orange");

	private static final class Result {
		final String id, name, steps;
		final String expected, actual, status, shot;

		Result(String id, String name, String expected, String actual, String status, String shot) {
			this.id = id;
			this.name = fullName(id);
			this.steps = stepsFor(id);
			this.expected = expected;
			this.actual = actual;
			this.status = status;
			this.shot = shot;
		}
	}

	// ======================================================
	// TÊN TEST CASE ĐẦY ĐỦ
	// ======================================================

	private static String fullName(String id) {

		if (id.startsWith("KB-08-003-")) {
			Map<String, String> colors = Map.of("purple", "Tím", "pink", "Hồng", "green", "Xanh lá", "indigo", "Chàm",
					"orange", "Cam");

			String color = id.substring("KB-08-003-".length());

			return "Kiểm tra lọc danh sách chủ đề theo màu " + colors.getOrDefault(color, color);
		}

		if (id.startsWith("KB-08-005")) {
			String value = id.endsWith("A") ? "0–10" : id.endsWith("B") ? "10–10" : "14–14";

			return "Kiểm tra giá trị biên số lượng từ vựng " + "Min–Max (" + value + ")";
		}

		if (id.startsWith("KB-08-017-")) {
			return "Kiểm tra bộ dữ liệu biên bằng " + "TestNG DataProvider: " + id.substring("KB-08-017-".length());
		}

		switch (id) {
		case "KB-08-000":
			return "Kiểm tra URL, giao diện và đầy đủ " + "thành phần Sort, Color, Min–Max";
		case "KB-08-001":
			return "Kiểm tra sắp xếp chủ đề theo " + "Gần nhất (Newest)";
		case "KB-08-002":
			return "Kiểm tra sắp xếp chủ đề theo " + "Cũ nhất (Oldest)";
		case "KB-08-004":
			return "Kiểm tra lọc số lượng từ trong " + "khoảng hợp lệ Min=10, Max=14";
		case "KB-08-006":
			return "Kiểm tra xử lý khoảng Min=20 " + "lớn hơn Max=10 (Negative)";
		case "KB-08-007":
			return "Kiểm tra phục hồi danh sách khi " + "chuyển từ màu Hồng sang Tất cả";
		case "KB-08-008":
			return "Kiểm tra khôi phục danh sách khi " + "xóa cả hai trường Min và Max";
		case "KB-08-009":
			return "Kiểm tra lọc chủ đề khi chỉ nhập " + "giá trị Min=10";
		case "KB-08-010":
			return "Kiểm tra lọc chủ đề khi chỉ nhập " + "giá trị Max=14";
		case "KB-08-011":
			return "Kiểm tra giá trị biên Min=0 và Max=0";
		case "KB-08-012":
			return "Kiểm tra validation khi nhập " + "số âm Min=-1";
		case "KB-08-013":
			return "Kiểm tra kết hợp lọc màu Hồng " + "với Min=10, Max=14";
		case "KB-08-014":
			return "Kiểm tra kết hợp sắp xếp Cũ nhất " + "và lọc màu Xanh lá";
		case "KB-08-015":
			return "Kiểm tra reset toàn bộ bộ lọc " + "sau khi thiết lập nhiều điều kiện";
		case "KB-08-016":
			return "Kiểm tra tính hợp lệ của thuộc tính " + "data-color trên tất cả chủ đề";
		default:
			return id;
		}
	}

	// ======================================================
	// STEPS CHI TIẾT CỦA TỪNG TEST CASE
	// ======================================================

	private static String stepsFor(String id) {
		String pre = "1. Đăng nhập User, mở trang Học tập.\n" + "2. Đặt lại bộ lọc mặc định.\n";

		if (id.startsWith("KB-08-003-")) {
			String color = id.substring(10);
			return pre + "3. Chọn màu " + color + ".\n" + "4. Đọc danh sách chủ đề sau lọc.\n"
					+ "5. So sánh màu và số lượng card với Expected.";
		}

		if (id.startsWith("KB-08-005") || id.startsWith("KB-08-017-")) {
			String range = id.startsWith("KB-08-005")
					? (id.endsWith("A") ? "0–10" : id.endsWith("B") ? "10–10" : "14–14")
					: id.substring("KB-08-017-".length());

			return pre + "3. Nhập Min–Max = " + range + ".\n" + "4. Đọc số từ của từng card.\n"
					+ "5. So sánh danh sách với Expected.";
		}

		switch (id) {
		case "KB-08-000":
			return "1. Đăng nhập User, mở trang Học tập.\n" + "2. Kiểm tra URL và các bộ lọc.\n"
					+ "3. Đọc tùy chọn Sort, Color, Min, Max.\n" + "4. So sánh với giao diện mong đợi.";

		case "KB-08-001":
		case "KB-08-002":
			return pre + "3. Chọn Sort " + (id.endsWith("001") ? "Gần nhất." : "Cũ nhất.")
					+ "\n4. Đọc thứ tự các card.\n" + "5. So sánh với quy tắc sắp xếp.";

		case "KB-08-004":
			return pre + "3. Nhập Min=10, Max=14.\n" + "4. Đọc số từ của từng card.\n"
					+ "5. Kiểm tra tất cả nằm trong khoảng 10–14.";

		case "KB-08-006":
			return pre + "3. Nhập Min=20, Max=10.\n" + "4. Kiểm tra thông báo và danh sách.\n"
					+ "5. Xác nhận trang không lỗi và xử lý an toàn.";

		case "KB-08-007":
			return pre + "3. Chọn màu Hồng, sau đó chọn Tất cả.\n" + "4. Đọc danh sách chủ đề.\n"
					+ "5. So sánh với danh sách ban đầu.";

		case "KB-08-008":
			return pre + "3. Nhập Min=10, Max=14 rồi xóa cả hai.\n" + "4. Đọc danh sách sau khi xóa.\n"
					+ "5. Kiểm tra danh sách được khôi phục.";

		case "KB-08-009":
			return pre + "3. Nhập Min=10, để trống Max.\n" + "4. Đọc danh sách chủ đề.\n"
					+ "5. Kiểm tra mọi card có số từ >= 10.";

		case "KB-08-010":
			return pre + "3. Để trống Min, nhập Max=14.\n" + "4. Đọc danh sách chủ đề.\n"
					+ "5. Kiểm tra mọi card có số từ <= 14.";

		case "KB-08-011":
			return pre + "3. Nhập Min=0, Max=0.\n" + "4. Đọc danh sách chủ đề.\n" + "5. Kiểm tra chỉ còn card có 0 từ.";

		case "KB-08-012":
			return pre + "3. Nhập Min=-1.\n" + "4. Đọc giá trị và trạng thái validation.\n"
					+ "5. Kiểm tra số âm bị đánh dấu không hợp lệ.";

		case "KB-08-013":
			return pre + "3. Chọn màu Hồng và Min=10, Max=14.\n" + "4. Đọc danh sách chủ đề.\n"
					+ "5. Kiểm tra cả điều kiện màu và số từ.";

		case "KB-08-014":
			return pre + "3. Chọn màu Xanh lá và Sort Cũ nhất.\n" + "4. Đọc thứ tự các card.\n"
					+ "5. Kiểm tra màu và thứ tự tăng dần.";

		case "KB-08-015":
			return pre + "3. Chọn màu Hồng và Min=10, Max=14.\n" + "4. Reset tất cả bộ lọc.\n"
					+ "5. Kiểm tra giá trị mặc định và danh sách.";

		case "KB-08-016":
			return pre + "3. Đọc data-color của từng card.\n" + "4. Đối chiếu với các màu được hỗ trợ.\n"
					+ "5. Ghi nhận card có màu không hợp lệ.";

		default:
			return pre + "3. Thực hiện thao tác kiểm thử.\n" + "4. Đọc kết quả thực tế.\n"
					+ "5. So sánh Expected và Actual.";
		}
	}

	// ======================================================
	// HÀM HỖ TRỢ
	// ======================================================

	private static void check(boolean test, String message) {
		if (!test) {
			throw new AssertionError(message);
		}
	}

	private static String arg(String key) {
		return System.getProperty(key, "").trim();
	}

	private static List<String> ids(List<Topic> topics) {
		return topics.stream().map(t -> t.id + ":" + t.name).sorted().collect(Collectors.toList());
	}

	private static String summary(List<Topic> values) {
		return "count=" + values.size() + "; "
				+ values.stream().map(t -> t.name + "[" + t.id + ", " + t.color + ", " + t.total + "]")
						.collect(Collectors.joining(", "));
	}

	private static String compareSet(List<Topic> expected, List<Topic> actual) {
		return "Expected=" + ids(expected) + "; Actual=" + ids(actual);
	}

	private void save(String id, String name, String expected, String actual, Runnable assertions) {
		try {
			assertions.run();

			results.add(new Result(id, name, expected, actual, "PASS", ""));

		} catch (Throwable error) {
			String proof = screenshot(id);

			results.add(new Result(id, name, expected, actual + "\nERROR: " + error.getMessage(), "FAIL", proof));

			throw error instanceof AssertionError ? (AssertionError) error : new AssertionError(error);
		}
	}

	private String screenshot(String id) {
		try {
			Path folder = Paths.get("LearnFilter_Screenshots_USER");

			Files.createDirectories(folder);

			Path file = folder.resolve(id + "_" + System.currentTimeMillis() + ".png");

			Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), file);

			return file.toAbsolutePath().toString();

		} catch (Exception error) {
			return "Screenshot error: " + error.getMessage();
		}
	}

	// ======================================================
	// ĐĂNG NHẬP USER VÀ MỞ TRANG HỌC TẬP
	// ======================================================

	private void login() {
		String username = arg("test.username");
		String password = arg("test.password");

		check(!username.isBlank() && !password.isBlank(),
				"Thiếu -Dtest.username / -Dtest.password " + "trong VM arguments");

		driver.get(BASE);

		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='login.php']"))).click();

		WebElement user = wait.until(d -> {
			for (WebElement e : d.findElements(By.cssSelector("input[name='username']," + "input[name='email'],"
					+ "input[type='email']," + "input[autocomplete='username']," + "input[type='text']"))) {

				if (e.isDisplayed() && e.isEnabled()) {
					return e;
				}
			}
			return null;
		});

		WebElement pass = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));

		user.sendKeys(username);
		pass.sendKeys(password);

		wait.until(ExpectedConditions
				.elementToBeClickable(By.cssSelector("button[type='submit']," + "input[type='submit']"))).click();

		wait.until(d -> d.getCurrentUrl().contains("dashboard.php") || d.getCurrentUrl().contains("admin.php"));

		check(driver.getCurrentUrl().contains("dashboard.php"), "Kịch bản 8 chỉ dùng User: " + driver.getCurrentUrl());

		System.out.println("[LOGIN PASS][USER] " + driver.getCurrentUrl());
	}

	@BeforeClass(alwaysRun = true)
	public void setup() {
		ChromeOptions options = new ChromeOptions();

		options.addArguments("--start-maximized", "--disable-notifications");

		driver = new ChromeDriver(options);

		wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		login();

		WebElement learnLink = wait.until(ExpectedConditions.elementToBeClickable(
				By.cssSelector("a.nav-item[href='learn.php']" + "[title='Học tập']," + "a[href='learn.php']")));

		learnLink.click();

		wait.until(d -> d.getCurrentUrl().contains("learn.php"));

		page = new LearnFilterPage(driver);
		page.ready();
		page.reset();

		baseline = List.copyOf(page.cards());

		System.out.println("[SCENARIO 8] USER Baseline=" + summary(baseline));
	}

	@BeforeMethod(alwaysRun = true)
	public void beforeEach() {
		if (page != null) {
			page.reset();
		}
	}

	@AfterClass(alwaysRun = true)
	public void finish() {
		try {
			report();
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	// ======================================================
	// KB-08-000: UI
	// ======================================================

	@Test(priority = 0)
	public void KB_08_000_Components() {
		String actual = "URL=" + driver.getCurrentUrl() + "; sort=" + page.options("sortOrder") + "; colors="
				+ page.options("colorFilter") + "; Min=" + page.value("minWords") + "; Max=" + page.value("maxWords");

		save("KB-08-000", "UI và dữ liệu bộ lọc",
				"URL learn.php; newest/oldest; " + "all/purple/pink/green/indigo/orange; " + "Min/Max number", actual,
				() -> {
					check(driver.getCurrentUrl().contains("learn.php"), "URL sai");

					check(page.options("sortOrder").stream().anyMatch(x -> x.startsWith("newest=")), "Thiếu newest");

					check(page.options("sortOrder").stream().anyMatch(x -> x.startsWith("oldest=")), "Thiếu oldest");

					for (String color : COLORS) {
						check(page.options("colorFilter").stream().anyMatch(x -> x.startsWith(color + "=")),
								"Thiếu " + color);
					}

					check(page.options("colorFilter").stream().anyMatch(x -> x.startsWith("all=")), "Thiếu all");

					check(!baseline.isEmpty(), "Không có card baseline");
				});
	}

	// ======================================================
	// KB-08-001 / KB-08-002: SORT
	// ======================================================

	private void sortCase(String id, String direction) {
		page.choose("sortOrder", direction);

		List<Topic> actual = page.cards();

		String expected = "sort=" + direction + "; mọi card baseline; ID "
				+ (direction.equals("newest") ? "giảm dần" : "tăng dần") + " (ID làm proxy ngày tạo, "
				+ "cần xác minh với DB nếu created_at khác)";

		save(id, "Sắp xếp " + direction, expected, summary(actual), () -> {
			check(ids(actual).equals(ids(baseline)), "Sort làm mất/thêm card: " + compareSet(baseline, actual));

			long previous = direction.equals("newest") ? Long.MAX_VALUE : Long.MIN_VALUE;

			for (Topic t : actual) {
				long current;

				try {
					current = Long.parseLong(t.id);
				} catch (NumberFormatException error) {
					throw new AssertionError("Thiếu ID số để xác minh sort", error);
				}

				check(direction.equals("newest") ? current <= previous : current >= previous,
						"Sai thứ tự ID tại " + t.name + " id=" + t.id);

				previous = current;
			}
		});
	}

	@Test(priority = 1)
	public void KB_08_001_Newest() {
		sortCase("KB-08-001", "newest");
	}

	@Test(priority = 2)
	public void KB_08_002_Oldest() {
		sortCase("KB-08-002", "oldest");
	}

	// ======================================================
	// KB-08-003: COLOR FILTER
	// ======================================================

	@Test(priority = 3)
	public void KB_08_003_AllColors() {
		for (String color : COLORS) {
			page.choose("colorFilter", color);

			List<Topic> expected = baseline.stream().filter(t -> t.color.equals(color)).collect(Collectors.toList());

			List<Topic> actual = page.cards();

			save("KB-08-003-" + color, "Filter màu " + color, "data-color=" + color + "; " + summary(expected),
					summary(actual), () -> {
						check(ids(actual).equals(ids(expected)), "Color mismatch: " + compareSet(expected, actual));

						check(actual.stream().allMatch(t -> t.color.equals(color)), "Card có màu khác " + color);
					});
		}
	}

	// ======================================================
	// KB-08-004 / KB-08-005: RANGE VÀ BOUNDARY
	// ======================================================

	private void range(String id, int min, int max) {
		page.number("minWords", String.valueOf(min));
		page.number("maxWords", String.valueOf(max));

		List<Topic> actual = page.cards();

		List<Topic> expected = baseline.stream().filter(t -> t.total >= min && t.total <= max)
				.collect(Collectors.toList());

		save(id, "Range " + min + "-" + max, "Mọi card có " + min + "<=data-total<=" + max + "; " + summary(expected),

				"Min=" + page.value("minWords") + ", Max=" + page.value("maxWords") + "; " + summary(actual),

				() -> {
					check(ids(actual).equals(ids(expected)), "Range mismatch: " + compareSet(expected, actual));

					check(actual.stream().allMatch(t -> t.total >= min && t.total <= max), "Có card vượt biên");
				});
	}

	@Test(priority = 4)
	public void KB_08_004_ValidRange() {
		range("KB-08-004", 10, 14);
	}

	@Test(priority = 5)
	public void KB_08_005_BoundaryRanges() {
		range("KB-08-005A", 0, 10);

		page.reset();
		range("KB-08-005B", 10, 10);

		page.reset();
		range("KB-08-005C", 14, 14);
	}

	// ======================================================
	// KB-08-006: NEGATIVE MIN > MAX
	// ======================================================

	@Test(priority = 6)
	public void KB_08_006_InvalidMinGreaterMax() {
		page.number("minWords", "20");
		page.number("maxWords", "10");

		List<Topic> actualCards = page.cards();
		boolean notice = page.noResultsVisible();

		String actual = "Min=" + page.value("minWords") + ", Max=" + page.value("maxWords") + ", count="
				+ actualCards.size() + ", noResults=" + notice + ", URL=" + driver.getCurrentUrl();

		save("KB-08-006", "Negative Min>Max", "Không crash; không hiển thị card " + "nằm ngoài khoảng vô nghiệm",
				actual, () -> {
					check(driver.getCurrentUrl().contains("learn.php"), "Trang bị crash/điều hướng sai");

					check(actualCards.isEmpty(), "Range 20>10 vẫn hiển thị card");
				});
	}

	// ======================================================
	// KB-08-007: RESET COLOR
	// ======================================================

	@Test(priority = 7)
	public void KB_08_007_AllReset() {
		page.choose("colorFilter", "pink");
		page.choose("colorFilter", "all");

		List<Topic> actual = page.cards();

		save("KB-08-007", "Màu Tất cả khôi phục", "Tập card = baseline " + baseline.size(), summary(actual),
				() -> check(ids(actual).equals(ids(baseline)), "All không phục hồi: " + compareSet(baseline, actual)));
	}

	// ======================================================
	// KB-08-008: CLEAR MIN/MAX
	// ======================================================

	@Test(priority = 8)
	public void KB_08_008_EmptyBounds() {
		page.number("minWords", "");
		page.number("maxWords", "");

		List<Topic> actual = page.cards();

		save("KB-08-008", "Xóa cả hai biên", "Tất cả " + baseline.size() + " card", summary(actual),
				() -> check(ids(actual).equals(ids(baseline)), "Xóa Min/Max không phục hồi"));
	}

	// ======================================================
	// KB-08-009: ONLY MIN
	// ======================================================

	@Test(priority = 9)
	public void KB_08_009_MinOnly() {
		page.number("minWords", "10");

		List<Topic> actual = page.cards();

		List<Topic> expected = baseline.stream().filter(t -> t.total >= 10).collect(Collectors.toList());

		save("KB-08-009", "Chỉ nhập Min=10", "data-total>=10: " + summary(expected), summary(actual),
				() -> check(ids(actual).equals(ids(expected)), "Chỉ Min lọc sai"));
	}

	// ======================================================
	// KB-08-010: ONLY MAX
	// ======================================================

	@Test(priority = 10)
	public void KB_08_010_MaxOnly() {
		page.number("maxWords", "14");

		List<Topic> actual = page.cards();

		List<Topic> expected = baseline.stream().filter(t -> t.total <= 14).collect(Collectors.toList());

		save("KB-08-010", "Chỉ nhập Max=14", "data-total<=14: " + summary(expected), summary(actual),
				() -> check(ids(actual).equals(ids(expected)), "Chỉ Max lọc sai"));
	}

	// ======================================================
	// KB-08-011: ZERO BOUNDARY
	// ======================================================

	@Test(priority = 11)
	public void KB_08_011_ZeroBoundary() {
		range("KB-08-011", 0, 0);
	}

	// ======================================================
	// KB-08-012: NEGATIVE INPUT
	// ======================================================

	@Test(priority = 12)
	public void KB_08_012_NegativeInput() {
		page.number("minWords", "-1");

		String entered = page.value("minWords");
		String validity = page.validation("minWords");

		save("KB-08-012", "Biên âm Min=-1",
				"HTML min=0; số âm phải bị " + "HTML validity từ chối hoặc form có cảnh báo",

				"value=" + entered + "; validity=" + validity,

				() -> {
					boolean invalid = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
							"let e=document.getElementById('minWords');" + "return e && !e.checkValidity();"));

					check(entered.isEmpty() || invalid, "Không phát hiện validation " + "đối với Min âm");
				});
	}

	// ======================================================
	// KB-08-013: COLOR + RANGE
	// ======================================================

	@Test(priority = 13)
	public void KB_08_013_ColorRangeCombination() {
		page.choose("colorFilter", "pink");
		page.number("minWords", "10");
		page.number("maxWords", "14");

		List<Topic> actual = page.cards();

		List<Topic> expected = baseline.stream().filter(t -> t.color.equals("pink") && t.total >= 10 && t.total <= 14)
				.collect(Collectors.toList());

		save("KB-08-013", "Màu + khoảng từ", "pink AND 10<=total<=14; " + summary(expected), summary(actual),

				() -> check(ids(actual).equals(ids(expected)), "Giao bộ lọc sai"));
	}

	// ======================================================
	// KB-08-014: SORT + COLOR
	// ======================================================

	@Test(priority = 14)
	public void KB_08_014_SortAndColor() {
		page.choose("colorFilter", "green");
		page.choose("sortOrder", "oldest");

		List<Topic> actual = page.cards();

		List<Topic> expected = baseline.stream().filter(t -> t.color.equals("green")).collect(Collectors.toList());

		save("KB-08-014", "Sort + màu", "Chỉ màu green, ID tăng dần; " + summary(expected), summary(actual),

				() -> {
					check(ids(actual).equals(ids(expected)), "Tập card sai");

					long previous = Long.MIN_VALUE;

					for (Topic t : actual) {
						long current = Long.parseLong(t.id);

						check(current >= previous, "Oldest sai: " + t);

						previous = current;
					}
				});
	}

	// ======================================================
	// KB-08-015: RESET ALL
	// ======================================================

	@Test(priority = 15)
	public void KB_08_015_Recovery() {
		page.choose("colorFilter", "pink");
		page.number("minWords", "10");
		page.number("maxWords", "14");

		page.reset();

		List<Topic> actual = page.cards();

		save("KB-08-015", "Reset bộ lọc kết hợp", "newest/all/Min rỗng/Max rỗng; " + "baseline " + baseline.size(),

				"sort=" + page.selected("sortOrder") + ", color=" + page.selected("colorFilter") + ", min='"
						+ page.value("minWords") + "', max='" + page.value("maxWords") + "'; " + summary(actual),

				() -> {
					check(page.selected("sortOrder").equals("newest"), "Sort chưa reset");

					check(page.selected("colorFilter").equals("all"), "Màu chưa reset");

					check(page.value("minWords").isEmpty() && page.value("maxWords").isEmpty(), "Range chưa reset");

					check(ids(actual).equals(ids(baseline)), "Card không trở về baseline");
				});
	}

	// ======================================================
	// KB-08-016: INVALID COLOR METADATA
	// ======================================================

	@Test(priority = 16)
	public void KB_08_016_InvalidColorMetadata() {
		List<Topic> unusual = baseline.stream().filter(t -> !t.color.isEmpty() && !COLORS.contains(t.color))
				.collect(Collectors.toList());

		String actual = summary(unusual);

		save("KB-08-016", "Audit data-color",
				"Tất cả card có data-color thuộc " + "tập màu hỗ trợ hoặc có fallback " + "được đặc tả",

				actual, () -> check(unusual.isEmpty(),
						"Có data-color ngoài 5 màu filter: " + actual + ". Đây là kiểm tra chất lượng dữ liệu."));
	}

	// ======================================================
	// KB-08-017: DATAPROVIDER
	// ======================================================

	@DataProvider(name = "rangeCases")
	public Object[][] rangeCases() {
		return new Object[][] { { 0, 10 }, { 10, 14 }, { 10, 10 }, { 14, 14 }, { 0, 0 } };
	}

	@Test(priority = 17, dataProvider = "rangeCases", description = "KB-08-017 DataProvider Boundary")
	public void KB_08_017_DataProvider(int min, int max) {
		range("KB-08-017-" + min + "-" + max, min, max);
	}

	// ======================================================
	// CONSOLE – HIỂN THỊ ĐẦY ĐỦ, TỰ ĐỘNG XUỐNG DÒNG
	// ======================================================

	private static List<String> wrapCell(String value, int width) {

		List<String> lines = new ArrayList<>();

		for (String part : value.split("\\n", -1)) {
			if (part.isEmpty()) {
				lines.add("");
				continue;
			}

			while (part.length() > width) {
				int cut = part.lastIndexOf(' ', width);

				if (cut < width / 3) {
					cut = width;
				}

				lines.add(part.substring(0, cut));
				part = part.substring(cut).stripLeading();
			}

			lines.add(part);
		}

		return lines;
	}

	private static String aligned(String text, int width) {
		return text + " ".repeat(Math.max(0, width - text.length()));
	}

	private static void printConsoleRow(String[] values, int[] widths) {

		List<List<String>> wrapped = new ArrayList<>();
		int height = 0;

		for (int col = 0; col < values.length; col++) {
			List<String> lines = wrapCell(values[col], widths[col]);

			wrapped.add(lines);
			height = Math.max(height, lines.size());
		}

		for (int line = 0; line < height; line++) {
			StringBuilder row = new StringBuilder("|");

			for (int col = 0; col < widths.length; col++) {
				List<String> lines = wrapped.get(col);

				String text = line < lines.size() ? lines.get(line) : "";

				row.append(' ').append(aligned(text, widths[col])).append(" |");
			}

			System.out.println(row);
		}
	}

	private void printTable() {
		int[] widths = { 18, 48, 62, 66, 66, 7 };

		String[] headings = { "TEST CASE ID", "TÊN TEST ĐẦY ĐỦ", "STEPS", "EXPECTED", "ACTUAL", "RESULT" };

		StringBuilder line = new StringBuilder("+");

		for (int width : widths) {
			line.append("-".repeat(width + 2)).append('+');
		}

		String border = line.toString();

		System.out.println("\nPWW_AUTO_08 | USER | FULL TEST CASE REPORT");

		System.out.println(border);
		printConsoleRow(headings, widths);
		System.out.println(border);

		for (Result result : results) {
			printConsoleRow(new String[] { result.id, result.name, result.steps, result.expected, result.actual,
					result.status }, widths);

			if (!result.shot.isBlank()) {
				System.out.println("SCREENSHOT FAIL: " + result.shot);
			}

			System.out.println(border);
		}
	}

	// ======================================================
	// EXCEL – 8 CỘT CHI TIẾT
	// ======================================================

	private void report() {
		printTable();

		long pass = results.stream().filter(r -> r.status.equals("PASS")).count();

		System.out.println(
				"[SCENARIO 8][USER] Rows=" + results.size() + " PASS=" + pass + " FAIL=" + (results.size() - pass));

		Path output = Paths.get("LearnFilter_TestResults_USER.xlsx");

		try (XSSFWorkbook book = new XSSFWorkbook()) {
			Sheet sheet = book.createSheet("Scenario 8");

			String[] columns = { "Test Case ID", "Vai trò", "Tên test đầy đủ", "Steps", "Expected", "Actual", "Result",
					"Screenshot" };

			Row header = sheet.createRow(0);

			CellStyle title = book.createCellStyle();
			title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			title.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			org.apache.poi.ss.usermodel.Font font = book.createFont();

			font.setBold(true);
			font.setColor(IndexedColors.WHITE.getIndex());
			title.setFont(font);

			CellStyle wrap = book.createCellStyle();
			wrap.setWrapText(true);
			wrap.setVerticalAlignment(VerticalAlignment.TOP);

			CellStyle success = book.createCellStyle();
			success.cloneStyleFrom(wrap);
			success.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			success.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle failure = book.createCellStyle();
			failure.cloneStyleFrom(wrap);
			failure.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			failure.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			for (int col = 0; col < columns.length; col++) {
				Cell cell = header.createCell(col);
				cell.setCellValue(columns[col]);
				cell.setCellStyle(title);
			}

			for (int i = 0; i < results.size(); i++) {
				Result result = results.get(i);
				Row row = sheet.createRow(i + 1);

				String[] values = { result.id, "USER", result.name, result.steps, result.expected, result.actual,
						result.status, result.shot };

				for (int col = 0; col < values.length; col++) {
					Cell cell = row.createCell(col);
					cell.setCellValue(values[col]);

					cell.setCellStyle(col == 6 ? (result.status.equals("PASS") ? success : failure) : wrap);
				}

				row.setHeightInPoints(240);
			}

			int[] widths = { 22, 13, 58, 85, 100, 100, 12, 55 };

			for (int i = 0; i < widths.length; i++) {
				sheet.setColumnWidth(i, Math.min(255, widths[i]) * 256);
			}

			sheet.createFreezePane(0, 1);

			sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, results.size()), 0, 7));

			try (OutputStream out = Files.newOutputStream(output)) {
				book.write(out);
			}

			System.out.println("[EXCEL] " + output.toAbsolutePath());

		} catch (Exception error) {
			System.err.println("[EXCEL ERROR] " + error.getMessage());
		}
	}
}
