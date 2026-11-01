package vn.team.autotest.modules.learn.scenario;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;
import vn.team.autotest.modules.learn.page.LearnPage;
import vn.team.autotest.modules.learn.page.LearnPage.Topic;

/**
 * PWW_AUTO_07 - USER ONLY: Search chủ đề, positive/negative/boundary/recovery.
 * Requirements: -Dtest.username=... -Dtest.password=... Place
 * LearnSearchData.csv and LearnSearchData.xlsx in the project working
 * directory. Optional overrides: -Dlearn.url=https://.../learn.php
 * -Dlearn.searchCss=... -Dlearn.cardCss=... -Dlearn.titleCss=...
 * -Dlearn.expectedCsv=... -Dlearn.expectedExcel=...
 *
 * Actual is ALWAYS read from real UI; baseline expected derives from unfiltered
 * UI, never from search results. CSV/Excel expectedTopic and expectedCount are
 * extra oracles.
 */
public class LearnScenario7 {
	private static final String BASE = "https://learnenglish.plt.pro.vn/";
	private static final Duration WAIT = Duration.ofSeconds(15);
	private WebDriver driver;
	private WebDriverWait wait;
	private JavascriptExecutor js;
	private LearnPage learn;
	private final List<Outcome> outcomes = new ArrayList<>();
	private String initialUrl;
	private String dashboardUrl;
	private List<Topic> baseline = List.of();
	private boolean loginOk;
	private final String role = "user";

	private static class CaseData {
		final String source, keyword, expectedTopic;
		final Integer expectedCount;

		CaseData(String source, String keyword, String expectedTopic, Integer expectedCount) {
			this.source = source;
			this.keyword = keyword;
			this.expectedTopic = expectedTopic;
			this.expectedCount = expectedCount;
		}
	}

	private static class Outcome {
		final String id, name, source, expected, actual, status, evidence;

		Outcome(String id, String name, String source, String expected, String actual, String status, String evidence) {
			this.id = id;
			this.name = name;
			this.source = source;
			this.expected = expected;
			this.actual = actual;
			this.status = status;
			this.evidence = evidence;
		}
	}

	private static String normalize(String s) {
		return s == null ? "" : s.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
	}

	private static String lower(String s) {
		return normalize(s).toLowerCase(Locale.ROOT);
	}

	private static String prop(String key, String fallback) {
		String s = System.getProperty(key);
		return s == null || s.isBlank() ? fallback : s.trim();
	}

	private static boolean contains(String candidate, String keyword) {
		return lower(candidate).contains(lower(keyword));
	}

	private static void require(boolean test, String message) {
		if (!test)
			throw new AssertionError(message);
	}

	@BeforeClass(alwaysRun = true)
	public void setup() {
		String username = prop("test.username", prop("test.user.username", "phamthao_306"));
		String password = prop("test.password", prop("test.user.password", "Thao3006@"));
		require(!username.isBlank() && !password.isBlank(),
				"Thiếu User credentials: -Dtest.username=... -Dtest.password=...");
		System.out.println("[SCENARIO 7] ROLE=" + role.toUpperCase(Locale.ROOT));
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--start-maximized", "--disable-notifications");
		driver = new ChromeDriver(options);
		wait = new WebDriverWait(driver, WAIT);
		js = (JavascriptExecutor) driver;
		login(username, password);
		dashboardUrl = driver.getCurrentUrl();
		require(dashboardUrl.contains("dashboard.php"),
				"User phải vào dashboard.php sau đăng nhập; Actual=" + dashboardUrl);
		// Điều hướng bằng sidebar thật, không mở learn.php trực tiếp.
		By learnLink = By.cssSelector("a.nav-item[href='learn.php'][title='Học tập'],a[href='learn.php']");
		WebElement navigation = wait.until(ExpectedConditions.elementToBeClickable(learnLink));
		highlight(navigation, "ĐANG TEST: NAVIGATE LEARN");
		navigation.click();
		wait.until(d -> d.getCurrentUrl().contains("learn.php"));
		learn = new LearnPage(driver, wait);
		learn.attach();
		initialUrl = learn.url();
		learn.clear();
		baseline = List.copyOf(learn.cards());
		if (baseline.isEmpty()) {
			System.err.println("[LEARN DIAGNOSTIC] Baseline=0. categoryGrid="
					+ driver.findElements(By.id("categoryGrid")).size() + "; cat-card DOM="
					+ driver.findElements(By.cssSelector("#categoryGrid .cat-card")).size() + "; searchQuery="
					+ driver.findElements(By.id("searchQuery")).size() + "; URL=" + driver.getCurrentUrl());
			screenshot("SETUP_NO_TOPIC_CARDS");
		}
		loginOk = true;
		System.out.println("[LEARN BASELINE] " + baseline.size() + " card: " + baseline);
	}

	private void login(String username, String password) {
		driver.get(BASE);
		By loginLink = By.cssSelector("a[href*='login.php']");
		WebElement entry = wait.until(d -> {
			for (WebElement e : d.findElements(loginLink))
				if (e.isDisplayed() && e.isEnabled())
					return e;
			return null;
		});
		highlight(entry, "ĐANG TEST: MỞ LOGIN");
		entry.click();
		WebElement pass = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));
		WebElement user = wait.until(d -> {
			for (WebElement e : d.findElements(By.cssSelector(
					"input[name='username'],input[name='email'],input[type='email'],input[autocomplete='username'],input[type='text']")))
				if (e.isDisplayed() && e.isEnabled())
					return e;
			return null;
		});
		user.clear();
		user.sendKeys(username);
		pass.clear();
		pass.sendKeys(password);
		WebElement submit = wait.until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit'],input[type='submit']")));
		highlight(submit, "ĐANG TEST: LOGIN");
		submit.click();
		wait.until(d -> !d.getCurrentUrl().contains("login.php")
				&& (d.getCurrentUrl().contains("dashboard.php") || d.getCurrentUrl().contains("admin.php")
						|| !d.findElements(By.cssSelector("a[href*='logout']")).isEmpty()));
		System.out.println("[LOGIN PASS][" + role.toUpperCase(Locale.ROOT) + "] " + driver.getCurrentUrl());
	}

	private void highlight(WebElement target, String label) {
		try {
			js.executeScript(
					"arguments[0].style.outline='3px solid #e11d48';arguments[0].setAttribute('data-test-status',arguments[1]);",
					target, label);
		} catch (WebDriverException ignored) {
		}
	}

	private static List<String> names(List<Topic> cards) {
		return cards.stream().map(t -> t.name).collect(Collectors.toList());
	}

	private static List<String> canonical(List<Topic> topics) {
		List<String> l = topics.stream().map(t -> lower(t.name)).collect(Collectors.toCollection(ArrayList::new));
		Collections.sort(l);
		return l;
	}

	private List<Topic> expectedFromBaseline(String keyword) {
		return baseline.stream().filter(t -> contains(t.name, keyword)).collect(Collectors.toList());
	}

	private String describe(List<Topic> topics) {
		return "count=" + topics.size() + "; cards=" + names(topics);
	}

	private void checkSearch(CaseData row) {
		learn.enter(row.keyword);
		List<Topic> actual = learn.cards();
		List<Topic> expected = expectedFromBaseline(row.keyword);
		String expect = "- Keyword: " + row.keyword + "\n- Expected source: danh sách gốc trước search"
				+ "\n- Expected " + describe(expected)
				+ (row.expectedTopic.isBlank() ? ""
						: "\n- Expected topic from " + row.source + ": " + row.expectedTopic)
				+ (row.expectedCount == null ? "" : "\n- Expected count from " + row.source + ": " + row.expectedCount);
		String got = "- Search input: " + learn.search().getDomProperty("value") + "\n- Actual " + describe(actual)
				+ "\n- URL: " + learn.url();
		try {
			require(!baseline.isEmpty(), "Danh sách chủ đề ban đầu rỗng; không thể xác minh Search");
			require(canonical(actual).equals(canonical(expected)),
					"Tập card sau search khác tập lọc từ danh sách ban đầu");
			if (row.expectedCount != null)
				require(actual.size() == row.expectedCount,
						"Expected count file=" + row.expectedCount + " Actual=" + actual.size());
			if (!row.expectedTopic.isBlank()) {
				List<String> expectedTopics = Arrays.stream(row.expectedTopic.split("[|;]")).map(LearnScenario7::lower)
						.filter(x -> !x.isBlank()).collect(Collectors.toList());
				List<String> actualNames = canonical(actual);
				require(actualNames.containsAll(expectedTopics),
						"Thiếu expectedTopic từ " + row.source + ": " + expectedTopics);
			}
			outcomes.add(new Outcome("PWW_AUTO_07", row.source, row.source, expect, got, "PASS", ""));
			// Detailed rows are printed together as a console table after the suite.
		} catch (AssertionError ex) {
			String shot = screenshot(row.source);
			outcomes.add(new Outcome("PWW_AUTO_07", row.source, row.source, expect,
					got + "\n- FAIL: " + ex.getMessage(), "FAIL", shot));
			throw ex;
		}
	}

	private void record(String id, String title, String kind, String exp, String actual, Runnable assertions) {
		try {
			assertions.run();
			outcomes.add(new Outcome(id, title, kind, exp, actual, "PASS", ""));
		} catch (Throwable ex) {
			String shot = screenshot(id);
			outcomes.add(new Outcome(id, title, kind, exp, actual + "\n- ERROR: " + ex.getMessage(), "FAIL", shot));
			throw ex instanceof AssertionError ? (AssertionError) ex : new AssertionError(ex);
		}
	}

	private String screenshot(String id) {
		try {
			Path dir = Paths.get(prop("learn.screenshotDir", "LearnSearch_Screenshots_" + role));
			Files.createDirectories(dir);
			String safe = id.replaceAll("[^a-zA-Z0-9_-]", "_");
			Path target = dir.resolve(safe + "_" + System.currentTimeMillis() + ".png");
			Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), target);
			return target.toAbsolutePath().toString();
		} catch (Exception e) {
			return "Screenshot error: " + e.getMessage();
		}
	}

	private void restore() {
		if (driver != null && learn != null) {
			try {
				if (driver.getCurrentUrl().contains("login.php"))
					throw new AssertionError("Session hết hạn");
				learn.clear();
			} catch (WebDriverException e) {
				throw new AssertionError("Không thể reset Learn sau test", e);
			}
		}
	}
	// Each test sets the search input explicitly. Avoid an extra clear + debounce
	// after every test.

	// ===== TEST 01: ĐIỀU HƯỚNG + TIÊU ĐỀ, đúng HTML trang Learn =====
	@Test(priority = 0, description = "KB-07-000: Dashboard/sidebar -> Learn, URL và header")
	public void KB_07_000_NavigationAndHeading() {
		String exp = "- Vai trò: USER đăng nhập thành công\n- Từ Dashboard nhấn Học tập\n- Link Học tập href=learn.php"
				+ "\n- URL kết thúc bằng learn.php\n- H1: Chọn một chủ đề"
				+ "\n- P: Chọn một chủ đề, sau đó chọn chế độ chơi để bắt đầu học.";
		StringBuilder got = new StringBuilder();
		try {
			// Giữ đúng phiên sau đăng nhập, không dùng URL trực tiếp để bỏ qua kiểm thử
			// điều hướng.
			driver.get(BASE + "dashboard.php");
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
			require(!driver.getCurrentUrl().contains("login.php"), "Bị chuyển về đăng nhập");
			By link = By.cssSelector("a.nav-item[href='learn.php'][title='Học tập'],a[href='learn.php']");
			WebElement target = wait.until(ExpectedConditions.elementToBeClickable(link));
			got.append("- Vai trò: ").append(role).append("\n- Trang nguồn: ").append(driver.getCurrentUrl())
					.append('\n');
			got.append("- Link href: ").append(target.getDomAttribute("href")).append('\n');
			highlight(target, "ĐANG TEST: HỌC TẬP");
			target.click();
			wait.until(d -> d.getCurrentUrl().matches(".*[/]learn[.]php(?:[?].*)?$"));
			got.append("- URL sau click: ").append(driver.getCurrentUrl()).append('\n');
			WebElement h = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".learn-header h1")));
			WebElement para = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".learn-header p")));
			got.append("- H1: ").append(h.getText()).append("\n- P: ").append(para.getText());
			record("KB-07-000", "Điều hướng và header", "NAVIGATION", exp, got.toString(), () -> {
				require("Chọn một chủ đề".equals(normalize(h.getText())), "H1 không đúng");
				require("Chọn một chủ đề, sau đó chọn chế độ chơi để bắt đầu học.".equals(normalize(para.getText())),
						"Mô tả không đúng");
			});
		} catch (Throwable ex) {
			if (outcomes.stream().noneMatch(x -> x.id.equals("KB-07-000")))
				outcomes.add(new Outcome("KB-07-000", "Điều hướng và header", "NAVIGATION", exp,
						got + "\n- ERROR: " + ex.getMessage(), "FAIL", screenshot("KB-07-000")));
			throw ex instanceof AssertionError ? (AssertionError) ex : new AssertionError(ex);
		} finally {
			learn.attach();
			learn.clear();
		}
	}

	@Test(priority = 1, description = "KB-07-001 Positive - Manual Health")
	public void KB_07_001_ManualHealth() {
		checkSearch(new CaseData("KB-07-001 Manual", "Health", "Health & Fitness", null));
	}

	@DataProvider(name = "topicKeywords")
	public Object[][] topicKeywords() {
		return new Object[][] { { "Health", "Health & Fitness" }, { "Travel", "" }, // multiple matching topics are
																					// possible
				{ "Technology", "Advanced Technology" } };
	}

	@Test(priority = 2, dataProvider = "topicKeywords", description = "KB-07-002 Positive - DataProvider")
	public void KB_07_002_DataProvider(String keyword, String expectedTopic) {
		checkSearch(new CaseData("KB-07-002 DataProvider " + keyword, keyword, expectedTopic, null));
	}

	/** Supports Maven src/main/resources/testdata or project root. */
	private Path dataPath(String property, String filename) {
		String override = System.getProperty(property);
		if (override != null && !override.isBlank())
			return Paths.get(override.trim());
		for (String candidate : List.of("src/main/resources/testdata/" + filename,
				"src/test/resources/testdata/" + filename, filename)) {
			Path path = Paths.get(candidate);
			if (Files.isRegularFile(path))
				return path;
		}
		return Paths.get("src/main/resources/testdata/", filename);
	}

	/**
	 * Classpath resources work in Eclipse/Maven JAR; optional VM path overrides
	 * remain supported.
	 */
	private InputStream openTestData(String property, String filename) throws IOException {
		String override = System.getProperty(property);
		if (override != null && !override.isBlank()) {
			Path path = Paths.get(override.trim());
			require(Files.isRegularFile(path), "Không thấy file cấu hình " + property + ": " + path.toAbsolutePath());
			return Files.newInputStream(path);
		}
		for (String name : List.of("testdata/" + filename, filename)) {
			InputStream in = LearnScenario7.class.getClassLoader().getResourceAsStream(name);
			if (in != null)
				return in;
		}
		Path local = dataPath(property, filename);
		require(Files.isRegularFile(local),
				"Thiếu " + filename + ". Đặt file vào src/main/resources/testdata hoặc dùng -D" + property
						+ "=đường_dẫn. Đang tìm: " + local.toAbsolutePath());
		return Files.newInputStream(local);
	}

	private List<CaseData> readCsv() {
		List<CaseData> data = new ArrayList<>();
		try (InputStream in = openTestData("learn.expectedCsv", "LearnSearchData.csv");
				BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line = br.readLine();
			require(line != null, "CSV rỗng");
			String[] headings = csvFields(line.replace("\uFEFF", "")).toArray(String[]::new);
			int key = indexOf(headings, "keyword"), topic = indexOf(headings, "expectedTopic"),
					count = indexOf(headings, "expectedCount");
			require(key >= 0, "CSV thiếu cột keyword");
			int number = 1;
			while ((line = br.readLine()) != null) {
				number++;
				if (line.isBlank())
					continue;
				List<String> f = csvFields(line);
				String keyword = value(f, key), expectedTopic = value(f, topic), numberText = value(f, count);
				if (keyword.isBlank())
					continue;
				data.add(new CaseData("KB-07-003 CSV row " + number, keyword, expectedTopic,
						numberText.isBlank() ? null : Integer.valueOf(numberText)));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Đọc CSV thất bại: " + e.getMessage(), e);
		}
		require(!data.isEmpty(), "CSV không có dataset");
		return data;
	}

	private static int indexOf(String[] headings, String expected) {
		for (int i = 0; i < headings.length; i++)
			if (headings[i].trim().equalsIgnoreCase(expected))
				return i;
		return -1;
	}

	private static String value(List<String> items, int i) {
		return i >= 0 && i < items.size() ? normalize(items.get(i)) : "";
	}

	/**
	 * RFC-style quoted fields including embedded delimiters; no external OpenCSV
	 * runtime needed.
	 */
	private static List<String> csvFields(String s) {
		List<String> values = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"') {
				if (quoted && i + 1 < s.length() && s.charAt(i + 1) == '"') {
					cur.append('"');
					i++;
				} else
					quoted = !quoted;
			} else if (c == ',' && !quoted) {
				values.add(cur.toString());
				cur.setLength(0);
			} else
				cur.append(c);
		}
		require(!quoted, "CSV chứa dấu nháy chưa đóng");
		values.add(cur.toString());
		return values;
	}

	@Test(priority = 3, description = "KB-07-003 Positive - CSV rows")
	public void KB_07_003_CSV() {
		List<String> errors = new ArrayList<>();
		List<CaseData> rows;
		try {
			rows = readCsv();
		} catch (Throwable e) {
			outcomes.add(new Outcome("KB-07-003", "Đọc dữ liệu CSV", "CSV", "File CSV tồn tại; đọc được các dataset",
					"ERROR: " + e.getMessage(), "FAIL", screenshot("KB-07-003")));
			throw e instanceof AssertionError ? (AssertionError) e : new AssertionError(e);
		}
		for (CaseData row : rows)
			try {
				checkSearch(row);
			} catch (AssertionError e) {
				errors.add(row.source + ": " + e.getMessage());
			}
		require(errors.isEmpty(), "CSV dataset FAIL: " + errors);
	}

	private List<CaseData> readExcel() {
		List<CaseData> result = new ArrayList<>();
		try (InputStream stream = openTestData("learn.expectedExcel", "LearnSearchData.xlsx");
				Workbook book = new XSSFWorkbook(stream)) {
			Sheet sheet = book.getSheetAt(0);
			DataFormatter fmt = new DataFormatter();
			Row heading = sheet.getRow(sheet.getFirstRowNum());
			require(heading != null, "Excel thiếu header");
			Map<String, Integer> indexes = new HashMap<>();
			for (Cell c : heading)
				indexes.put(fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT), c.getColumnIndex());
			require(indexes.containsKey("keyword"), "Excel thiếu cột keyword");
			for (int r = heading.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (row == null)
					continue;
				String key = cell(row, indexes.get("keyword"), fmt);
				if (key.isBlank())
					continue;
				String topic = cell(row, indexes.get("expectedtopic"), fmt);
				String count = cell(row, indexes.get("expectedcount"), fmt);
				result.add(new CaseData("KB-07-004 Excel row " + (r + 1), key, topic,
						count.isBlank() ? null : Integer.parseInt(count)));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Không đọc được Excel: " + e.getMessage(), e);
		}
		require(!result.isEmpty(), "Excel không có dataset");
		return result;
	}

	private static String cell(Row row, Integer index, DataFormatter formatter) {
		if (index == null)
			return "";
		Cell c = row.getCell(index);
		return c == null ? "" : normalize(formatter.formatCellValue(c));
	}

	@Test(priority = 4, description = "KB-07-004 Positive - Excel rows")
	public void KB_07_004_Excel() {
		List<String> errors = new ArrayList<>();
		List<CaseData> rows;
		try {
			rows = readExcel();
		} catch (Throwable e) {
			outcomes.add(
					new Outcome("KB-07-004", "Đọc dữ liệu Excel", "Excel", "File Excel tồn tại; đọc được các dataset",
							"ERROR: " + e.getMessage(), "FAIL", screenshot("KB-07-004")));
			throw e instanceof AssertionError ? (AssertionError) e : new AssertionError(e);
		}
		for (CaseData row : rows)
			try {
				checkSearch(row);
			} catch (AssertionError e) {
				errors.add(row.source + ": " + e.getMessage());
			}
		require(errors.isEmpty(), "Excel dataset FAIL: " + errors);
	}

	@Test(priority = 5, description = "KB-07-005 Negative invalid keyword + recovery clear")
	public void KB_07_005_NegativeAndClear() {
		learn.enter("abcdef_xyz_123");
		List<Topic> negative = learn.cards();
		record("KB-07-005A", "Negative no-match", "NEGATIVE", "- Keyword abcdef_xyz_123 trả 0 card", describe(negative),
				() -> require(negative.isEmpty(), "Có kết quả không liên quan: " + negative));
		learn.clear();
		List<Topic> after = learn.cards();
		record("KB-07-005B", "Clear restores initial list", "RECOVERY",
				"- Count=" + baseline.size() + "; Expected cards=" + names(baseline), describe(after),
				() -> require(canonical(after).equals(canonical(baseline)), "Clear không phục hồi danh sách ban đầu"));
	}

	@Test(priority = 6, description = "Extra Negative: whitespace-only keyword")
	public void KB_07_006_Whitespace() {
		learn.enter("   ");
		List<Topic> after = learn.cards();
		record("KB-07-006", "Whitespace search", "BOUNDARY",
				"- Whitespace chỉ tương đương tìm kiếm rỗng; count=" + baseline.size(), describe(after),
				() -> require(canonical(after).equals(canonical(baseline)),
						"Search whitespace không trả lại danh sách gốc"));
	}

	@Test(priority = 7, description = "Extra Positive: mixed-case search")
	public void KB_07_007_CaseInsensitive() {
		CaseData d = new CaseData("KB-07-007 MixedCase", "hEaLtH", "Health & Fitness", null);
		checkSearch(d);
	}

	@Test(priority = 8, description = "Extra Boundary: one-character search")
	public void KB_07_008_SingleCharacter() {
		checkSearch(new CaseData("KB-07-008 OneChar", "a", "", null));
	}

	// ===== SEARCH EXTENSIONS: Positive / Negative / Boundary / Recovery =====
	@Test(priority = 9, description = "KB-07-009 Positive - Exact topic")
	public void KB_07_009_ExactTitle() {
		checkSearch(new CaseData("KB-07-009 Exact Topic", "Health & Fitness", "Health & Fitness", null));
	}

	@Test(priority = 10, description = "KB-07-010 Positive - Middle substring")
	public void KB_07_010_PartialKeyword() {
		checkSearch(new CaseData("KB-07-010 Partial", "ealth", "Health & Fitness", null));
	}

	@Test(priority = 11, description = "KB-07-011 Positive - Uppercase")
	public void KB_07_011_Uppercase() {
		checkSearch(new CaseData("KB-07-011 Uppercase", "HEALTH", "Health & Fitness", null));
	}

	@Test(priority = 12, description = "KB-07-012 Negative - Special characters")
	public void KB_07_012_SpecialCharacters() {
		checkSearch(new CaseData("KB-07-012 Special", "@@@###@@@", "", 0));
	}

	@Test(priority = 13, description = "KB-07-013 Negative - Digits")
	public void KB_07_013_Digits() {
		checkSearch(new CaseData("KB-07-013 Digits", "999999999", "", 0));
	}

	@Test(priority = 14, description = "KB-07-014 Recovery - Change keyword")
	public void KB_07_014_ChangeKeyword() {
		learn.enter("Health");
		List<Topic> first = learn.cards();
		learn.enter("Travel");
		List<Topic> second = learn.cards();
		List<Topic> e1 = expectedFromBaseline("Health"), e2 = expectedFromBaseline("Travel");
		record("KB-07-014", "Chuyển Search không reload", "RECOVERY",
				"Health -> " + describe(e1) + "\nTravel -> " + describe(e2),
				"Health -> " + describe(first) + "\nTravel -> " + describe(second), () -> {
					require(!baseline.isEmpty(), "Baseline rỗng");
					require(canonical(first).equals(canonical(e1)), "Health không đúng");
					require(canonical(second).equals(canonical(e2)), "Travel vẫn giữ kết quả cũ");
				});
	}

	@Test(priority = 15, description = "KB-07-015 Recovery - Negative then valid")
	public void KB_07_015_NegativeToValid() {
		learn.enter("abcdef_xyz_123");
		List<Topic> negative = learn.cards();
		learn.enter("Health");
		List<Topic> positive = learn.cards();
		List<Topic> expected = expectedFromBaseline("Health");
		record("KB-07-015", "Không tìm thấy -> tìm hợp lệ", "RECOVERY", "Invalid=0; Health=" + describe(expected),
				"Invalid=" + describe(negative) + "; Health=" + describe(positive), () -> {
					require(negative.isEmpty(), "Negative có kết quả");
					require(!baseline.isEmpty(), "Baseline rỗng");
					require(canonical(positive).equals(canonical(expected)), "Không phục hồi sau negative");
				});
	}

	@Test(priority = 16, description = "KB-07-016 Boundary - Empty Search")
	public void KB_07_016_EmptySearch() {
		learn.clear();
		List<Topic> actual = learn.cards();
		record("KB-07-016", "Ô tìm kiếm rỗng", "BOUNDARY", "Search value=''; " + describe(baseline),
				"Search value='" + learn.search().getDomProperty("value") + "'; " + describe(actual), () -> {
					require(!baseline.isEmpty(), "Baseline rỗng");
					require(learn.search().getDomProperty("value").isEmpty(), "Input chưa rỗng");
					require(canonical(actual).equals(canonical(baseline)), "Search rỗng không hiện toàn bộ chủ đề");
				});
	}

	@Test(priority = 17, description = "KB-07-017 Boundary - Multiple matching topics")
	public void KB_07_017_MultipleMatches() {
		checkSearch(new CaseData("KB-07-017 Multi Results", "Travel", "", null));
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() {
		try {
			writeReport();
		} finally {
			if (driver != null)
				try {
					driver.quit();
				} catch (Exception ignored) {
				}
		}
	}

	private void writeReport() {
		Path out = Paths.get(prop("learn.resultExcel", "LearnSearch_TestResults_USER.xlsx"));
		try (XSSFWorkbook book = new XSSFWorkbook()) {
			Sheet sheet = book.createSheet("Search Results");
			String[] headers = { "Test Case ID", "Vai trò", "Tên test", "Nguồn dữ liệu", "Expected", "Actual", "Result",
					"Screenshot" };
			Row h = sheet.createRow(0);
			CellStyle head = book.createCellStyle();
			head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			Font font = book.createFont();
			font.setBold(true);
			font.setColor(IndexedColors.WHITE.getIndex());
			head.setFont(font);
			CellStyle body = book.createCellStyle();
			body.setWrapText(true);
			body.setVerticalAlignment(VerticalAlignment.TOP);
			CellStyle ok = book.createCellStyle();
			ok.cloneStyleFrom(body);
			ok.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			ok.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			CellStyle fail = book.createCellStyle();
			fail.cloneStyleFrom(body);
			fail.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			fail.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			for (int c = 0; c < headers.length; c++) {
				Cell cell = h.createCell(c);
				cell.setCellValue(headers[c]);
				cell.setCellStyle(head);
			}
			for (int i = 0; i < outcomes.size(); i++) {
				Outcome e = outcomes.get(i);
				Row row = sheet.createRow(i + 1);
				String[] vals = { e.id, role.toUpperCase(Locale.ROOT), e.name, e.source, e.expected, e.actual, e.status,
						e.evidence };
				for (int c = 0; c < vals.length; c++) {
					Cell cell = row.createCell(c);
					cell.setCellValue(vals[c]);
					cell.setCellStyle(c == 6 ? ("PASS".equals(e.status) ? ok : fail) : body);
				}
				row.setHeightInPoints(
						Math.min(330, Math.max(54, 18 * (Math.max(lines(e.expected), lines(e.actual)) + 1))));
			}
			int[] widths = { 20, 14, 34, 30, 75, 85, 12, 45 };
			for (int i = 0; i < widths.length; i++)
				sheet.setColumnWidth(i, Math.min(255, widths[i]) * 256);
			sheet.createFreezePane(0, 1);
			sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, outcomes.size()), 0, 7));
			try (OutputStream stream = Files.newOutputStream(out)) {
				book.write(stream);
			}
			long passes = outcomes.stream().filter(x -> "PASS".equals(x.status)).count();
			printConsoleTable();
			System.out.println("[SCENARIO 7][" + role.toUpperCase(Locale.ROOT) + "] Rows=" + outcomes.size() + " PASS="
					+ passes + " FAIL=" + (outcomes.size() - passes));
			System.out.println("[EXCEL] " + out.toAbsolutePath());
		} catch (Exception e) {
			System.err.println("Không thể xuất Excel: " + e.getMessage());
		}
	}

	// Aligned console report: full details remain in the Excel workbook.
	private static String shorten(String s, int width) {
		String clean = normalize(s).replace('|', '/');
		return clean.length() <= width ? clean : clean.substring(0, Math.max(0, width - 3)) + "...";
	}

	private static String pad(String s, int width) {
		String t = shorten(s, width);
		return t + " ".repeat(Math.max(0, width - t.length()));
	}

	private void printConsoleTable() {
		int[] w = { 23, 30, 44, 48, 7 };
		String border = "+" + "-".repeat(w[0] + 2) + "+" + "-".repeat(w[1] + 2) + "+" + "-".repeat(w[2] + 2) + "+"
				+ "-".repeat(w[3] + 2) + "+" + "-".repeat(w[4] + 2) + "+";
		System.out.println("\n=================== PWW_AUTO_07 | USER | SEARCH RESULTS ===================");
		System.out.println(border);
		System.out.printf("| %s | %s | %s | %s | %s |%n", pad("TEST CASE ID", w[0]), pad("TEN TEST", w[1]),
				pad("EXPECTED", w[2]), pad("ACTUAL", w[3]), pad("RESULT", w[4]));
		System.out.println(border);
		for (Outcome e : outcomes) {
			System.out.printf("| %s | %s | %s | %s | %s |%n", pad(e.id, w[0]), pad(e.name, w[1]), pad(e.expected, w[2]),
					pad(e.actual, w[3]), pad(e.status, w[4]));
		}
		System.out.println(border);
		System.out
				.println("Expected/Actual đầy đủ từng bước xem file Excel; bảng Console được rút gọn theo chiều rộng.");
	}

	private static int lines(String s) {
		return s.split("\\n", -1).length;
	}
}
