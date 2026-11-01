package vn.team.autotest.modules.dashboard.scenario;

import java.io.File;

import java.io.FileOutputStream;

import java.time.Duration;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

import java.util.Arrays;

import java.util.List;

import java.util.function.Predicate;

import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;

import org.apache.poi.ss.usermodel.CellStyle;

import org.apache.poi.ss.usermodel.FillPatternType;

import org.apache.poi.ss.usermodel.Font;

import org.apache.poi.ss.usermodel.HorizontalAlignment;

import org.apache.poi.ss.usermodel.IndexedColors;

import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.ss.usermodel.VerticalAlignment;

import org.apache.poi.ss.util.CellRangeAddress;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.testng.SkipException;

import org.openqa.selenium.JavascriptExecutor;

import org.openqa.selenium.OutputType;

import org.openqa.selenium.StaleElementReferenceException;

import org.openqa.selenium.TakesScreenshot;

import org.openqa.selenium.TimeoutException;

import org.openqa.selenium.WebDriver;

import org.openqa.selenium.WebElement;

import org.openqa.selenium.chrome.ChromeDriver;

import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.support.ui.ExpectedConditions;

import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;

import org.testng.annotations.AfterClass;

import org.testng.annotations.BeforeClass;

import org.testng.annotations.Test;

/**
 * 
 * DashboardScenario2 - standalone Selenium/TestNG scenario.
 * 
 * Does not depend on BaseScenario, LoginPage, DashboardPage, or AppConfig.
 * 
 * Configure VM arguments: -Dtest.username=... -Dtest.password=...
 * 
 */

public class DashboardScenario2 {

	private static final String BASE_URL = "https://learnenglish.plt.pro.vn/";

	private static final String DASHBOARD_URL_PART = "dashboard.php";

	private static final int WAIT_SECONDS = 15;

	private WebDriver driver;

	private WebDriverWait wait;

	private JavascriptExecutor js;

	private final List<TestResult> results = new ArrayList<>();

	private String activeTest = "Đang chuẩn bị đăng nhập";

	private boolean loginSucceeded = false;
	private boolean browserSessionLost = false;

	private static final class TestResult {

		final String id;

		final String name;

		final String expected;

		String actual;

		String status;

		String details = "";

		String screenshot = "";

		boolean printed = false;

		TestResult(String id, String name, String expected) {

			this.id = id;
			this.name = name;
			this.expected = expected;

		}

	}

	@BeforeClass(alwaysRun = true)

	public void setUpAndLogin() {

		String username = System.getProperty("test.username");

		String password = System.getProperty("test.password");

		if (username == null || username.isBlank() || password == null || password.isBlank()) {

			throw new IllegalStateException("Thiếu thông tin đăng nhập. Thêm VM arguments: "

					+ "-Dtest.username=TEN_TAI_KHOAN -Dtest.password=MAT_KHAU");

		}

		ChromeOptions options = new ChromeOptions();

		options.addArguments("--start-maximized", "--disable-notifications");

		// Bỏ comment dòng dưới nếu muốn chạy không hiện cửa sổ Chrome:

		// options.addArguments("--headless=new", "--window-size=1440,1000");

		try {

			driver = new ChromeDriver(options);

			wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));

			js = (JavascriptExecutor) driver;

			login(username, password);

			loginSucceeded = true;

		} catch (RuntimeException e) {

			if (driver != null) {

				takeScreenshot("SETUP_LOGIN_FAIL");

			}

			throw e;

		}

	}

	/**
	 * Login flow: homepage -> click visible Đăng nhập -> fill login form ->
	 * Dashboard.
	 */

	private void login(String username, String password) {

		driver.get(BASE_URL);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

		showBanner("LOGIN 1/4: Đang kiểm tra trang chủ");

		System.out.println("[LOGIN 1] Trang chủ: " + driver.getCurrentUrl());

		// Tìm nút/link Đăng nhập theo text thực tế, kể cả khi text nằm trong span.

		// Ưu tiên phần tử hiển thị; không chọn link ẩn trong menu responsive.

		By homeLoginLocator = By.xpath(

				"//*[self::a or self::button or self::input or @role='button']"

						+ "[contains(translate(normalize-space(string(.)),'ĐĂNG NHẬPLOGIN','đăng nhậplogin'),'đăng nhập')"

						+ " or contains(translate(@aria-label,'ĐĂNG NHẬPLOGIN','đăng nhậplogin'),'đăng nhập')"

						+ " or contains(translate(@title,'ĐĂNG NHẬPLOGIN','đăng nhậplogin'),'đăng nhập')"

						+ " or contains(translate(@value,'ĐĂNG NHẬPLOGIN','đăng nhậplogin'),'đăng nhập')"

						+ " or contains(translate(@href,'LOGIN','login'),'login')]"

		);

		WebElement homeLogin = wait.until(d -> {

			for (WebElement element : d.findElements(homeLoginLocator)) {

				try {

					if (element.isDisplayed() && element.isEnabled()

							&& element.getRect().getHeight() > 0 && element.getRect().getWidth() > 0) {

						return element;

					}

				} catch (StaleElementReferenceException ignored) {
				}

			}

			return null;

		});

		// Cuộn nút vào vùng nhìn thấy và thử click chuẩn trước.

		js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", homeLogin);

		highlightElement(homeLogin, "LOGIN 2/4: Nhấn Đăng nhập trên trang chủ");

		String beforeClickUrl = driver.getCurrentUrl();

		try {

			wait.until(ExpectedConditions.elementToBeClickable(homeLogin)).click();

		} catch (Exception clickError) {

			// Một số giao diện có lớp phủ/animation khiến click chuẩn bị chặn.

			// Chỉ fallback JS trên đúng phần tử đã tìm thấy và đang hiển thị.

			try {

				js.executeScript("arguments[0].click();", homeLogin);

			} catch (Exception jsError) {

				throw new AssertionError("Tìm thấy nút Đăng nhập nhưng không click được. URL="

						+ driver.getCurrentUrl() + "; buttonText=" + safeElementText(homeLogin)

						+ "; clickError=" + clickError.getMessage(), jsError);

			}

		}

		// Chờ điều hướng hoặc form đăng nhập; nếu click không làm gì thì in thông tin
		// để dễ sửa locator.

		try {

			wait.until(d -> !d.getCurrentUrl().equals(beforeClickUrl)

					|| !d.findElements(By.cssSelector("input[type='password']")).isEmpty());

		} catch (TimeoutException e) {

			throw new AssertionError("Đã thử nhấn nút Đăng nhập nhưng trang không chuyển. URL hiện tại: "

					+ driver.getCurrentUrl() + ". Nút tìm được: " + safeElementText(homeLogin)

					+ ". Các nút/link nhìn thấy trên trang: " + visibleClickableSummary(), e);

		}

		// Chờ URL đổi hoặc form mật khẩu xuất hiện. Không tự điều hướng thẳng đến
		// login.

		wait.until(d -> !d.getCurrentUrl().equals(beforeClickUrl)

				|| !d.findElements(By.cssSelector("input[type='password']")).isEmpty());

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

		showBanner("LOGIN 3/4: Đã mở trang đăng nhập");

		System.out.println("[LOGIN 2] Sau khi nhấn nút: " + driver.getCurrentUrl());

		By passwordLocator = By.cssSelector("input[type='password']");

		WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordLocator));

		By usernameLocator = By.cssSelector(

				"input[name='username'], input[name='email'], input[type='email'], "

						+ "input[autocomplete='username'], input[type='text']");

		WebElement usernameInput = wait.until(d -> {

			for (WebElement element : d.findElements(usernameLocator)) {

				try {

					if (element.isDisplayed() && element.isEnabled()

							&& !"password".equalsIgnoreCase(element.getDomAttribute("type")))
						return element;

				} catch (StaleElementReferenceException ignored) {
				}

			}

			return null;

		});

		highlightElement(usernameInput, "LOGIN 3/4: Nhập tài khoản");

		usernameInput.clear();

		usernameInput.sendKeys(username);

		highlightElement(passwordInput, "LOGIN 3/4: Nhập mật khẩu");

		passwordInput.clear();

		passwordInput.sendKeys(password);

		By submitLocator = By.cssSelector("button[type='submit'], input[type='submit'], button[name='login']");

		WebElement submit = wait.until(d -> {

			for (WebElement element : d.findElements(submitLocator)) {

				try {

					if (element.isDisplayed() && element.isEnabled())
						return element;

				} catch (StaleElementReferenceException ignored) {
				}

			}

			// Fallback: button with visible login text, used by some PHP login forms.

			for (WebElement element : d.findElements(By.xpath(
					"//button[contains(.,'Đăng nhập') or contains(.,'Login')] | //input[@type='button' and contains(@value,'Đăng nhập')]"))) {

				try {
					if (element.isDisplayed() && element.isEnabled())
						return element;
				}

				catch (StaleElementReferenceException ignored) {
				}

			}

			return null;

		});

		highlightElement(submit, "LOGIN 4/4: Xác nhận đăng nhập");

		submit.click();

		try {

			wait.until(d -> d.getCurrentUrl().matches("https?://[^/]+/(?:admin|dashboard)\\.php(?:[?#].*)?"));

		} catch (TimeoutException e) {

			String pageText = safeBodyText();

			throw new AssertionError(
					"Đăng nhập chưa vào trang Admin hoặc Dashboard. URL hiện tại: " + driver.getCurrentUrl()

							+ ". Nội dung trang: " + truncate(pageText, 500),
					e);

		}

		showBanner("LOGIN HOÀN TẤT: Đã xác thực, URL=" + driver.getCurrentUrl());

		System.out.println("[LOGIN PASS] URL sau đăng nhập: " + driver.getCurrentUrl());

	}

	private static final String ADMIN_URL = "https://learnenglish.plt.pro.vn/admin.php";
	private static final String DASH_URL = "https://learnenglish.plt.pro.vn/dashboard.php";

	private void verify(String id, String title, String expected, Runnable body) {
		activeTest = id + " - " + title;
		TestResult r = new TestResult(id, title, expected);
		results.add(r);
		showBanner("ĐANG TEST: " + title);
		StringBuilder steps = new StringBuilder();
		observed = steps;
		try {
			if (browserSessionLost) {
				throw new SkipException(
						"ChromeDriver mất phiên ở test trước; không thể tiếp tục kiểm thử. Cần chạy lại với phiên Chrome mới.");
			}
			body.run();
			r.actual = steps.toString();
			r.status = "PASS";
			r.details = "Tất cả assertion của test case đã đạt.";
		} catch (SkipException skipped) {
			r.status = "SKIP";
			r.actual = steps.toString() + "\n- SKIP: " + skipped.getMessage();
			r.details = skipped.toString();
			throw skipped;
		} catch (Throwable error) {
			if (isSessionFailure(error))
				browserSessionLost = true;
			r.status = "FAIL";
			r.actual = steps.toString() + "\n- FAIL: " + truncate(String.valueOf(error.getMessage()), 900);
			r.details = truncate(error.toString(), 1600);
			r.screenshot = takeScreenshot(id + "_FAIL");
			if (error instanceof AssertionError)
				throw (AssertionError) error;
			throw new AssertionError("[" + id + "] " + error.getMessage(), error);
		} finally {
			observed = null;
			clearMarker();
		}
	}

	private boolean isSessionFailure(Throwable t) {
		for (Throwable cause = t; cause != null; cause = cause.getCause()) {
			if (cause instanceof NoSuchSessionException)
				return true;
			String message = cause.getMessage();
			if (message != null
					&& (message.contains("invalid session id") || message.contains("not connected to DevTools")))
				return true;
		}
		return false;
	}

	private StringBuilder observed;

	private void note(String label, Object value) {
		if (observed != null)
			observed.append("- ").append(label).append(": ").append(value).append('\n');
	}

	private void require(boolean ok, String failure) {
		if (!ok)
			throw new AssertionError("[" + activeTest + "] " + failure);
	}

	private WebElement visible(By locator, String label) {
		try {
			return wait.until(d -> {
				for (WebElement x : d.findElements(locator)) {
					try {
						if (x.isDisplayed())
							return x;
					} catch (StaleElementReferenceException ignored) {
					}
				}
				return null;
			});
		} catch (TimeoutException e) {
			throw new AssertionError("Không tìm thấy " + label + "; locator=" + locator, e);
		}
	}

	private void mark(WebElement e, String text) {
		js.executeScript("const e=arguments[0];e.scrollIntoView({block:'center'});" + "e.style.outline='3px solid red';"
				+ "const b=document.getElementById('__adminTestBadge');if(b)b.remove();"
				+ "const n=document.createElement('div');n.id='__adminTestBadge';n.textContent=arguments[1];"
				+ "Object.assign(n.style,{position:'fixed',top:'10px',right:'10px',zIndex:'2147483647',background:'#c00',color:'white',padding:'8px',fontWeight:'bold'});"
				+ "document.body.appendChild(n);window.__adminTestElement=e;", e, text);
	}

	private void clearMarker() {
		try {
			js.executeScript("if(window.__adminTestElement)window.__adminTestElement.style.removeProperty('outline');"
					+ "const b=document.getElementById('__adminTestBadge');if(b)b.remove();");
		} catch (Exception ignored) {
		}
	}

	private String text(WebElement e) {
		return e.getText().trim().replaceAll("\\s+", " ");
	}

	private int integer(String s) {
		String normalized = s.replaceAll("[^0-9]", "");
		require(!normalized.isEmpty(), "Không đọc được số từ: " + s);
		return Integer.parseInt(normalized);
	}

	private void restoreDashboard() {
		if (browserSessionLost)
			throw new SkipException("Chrome đã đóng hoặc mất kết nối trong test trước");
		if (!driver.getCurrentUrl().contains("dashboard.php")) {
			driver.get(DASH_URL);
			visible(By.cssSelector("h1"), "h1 Dashboard");
		}
	}

	private WebElement section(String heading) {
		String xpath = "//h3[normalize-space(.)='" + heading
				+ "']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')][1]";
		return visible(By.xpath(xpath), heading);
	}

	@Test(priority = 1, description = "TC_ADASH_001 - Admin navigation")
	public void TC_ADASH_001_adminNavigation() throws org.openqa.selenium.ElementClickInterceptedException {
		verify("TC_ADASH_001", "Điều hướng Admin sang Dashboard",
				"- Đăng nhập tài khoản Admin thành công\n- Mở admin.php trong phiên đã đăng nhập\n- Điều hướng tới dashboard.php thông qua liên kết trên admin.php\n- URL đích đúng dashboard.php\n- H1 chào đúng Siêu Cấp Quản Trị Viên",
				() -> {
					driver.get(ADMIN_URL);
					note("URL Admin", driver.getCurrentUrl());
					require(driver.getCurrentUrl().contains("admin.php"),
							"Không truy cập được admin.php bằng tài khoản hiện tại");
					// Locator lấy trực tiếp từ HTML sidebar Admin được cung cấp.
					// Không dùng get(DASH_URL) vì TC này phải xác minh thao tác click thực tế.
					By dashboardLink = By.cssSelector("a.nav-item[href='dashboard.php'][title='Dashboard']");
					WebElement nav = visible(dashboardLink, "sidebar Dashboard");
					require(nav.isEnabled(), "Link Dashboard đang bị vô hiệu hóa: " + dashboardLink);
					note("Link Dashboard", nav.getDomAttribute("href"));
					note("Link hiển thị", nav.isDisplayed());
					mark(nav, "ĐANG TEST: NHẤN DASHBOARD");
					js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", nav);
					String beforeClick = driver.getCurrentUrl();
					try {
						wait.until(ExpectedConditions.elementToBeClickable(nav)).click();
					} catch (org.openqa.selenium.ElementNotInteractableException e) {
						// Fallback JS chỉ khi Selenium báo bị che/chặn; vẫn click đúng anchor.
						note("Click thường bị chặn", e.getClass().getSimpleName());
						js.executeScript("arguments[0].click();", nav);
					}
					try {
						wait.until(d -> d.getCurrentUrl().matches("https?://[^/]+/dashboard\\.php(?:[?#].*)?"));
					} catch (TimeoutException e) {
						note("URL sau click", driver.getCurrentUrl());
						throw new AssertionError("Đã nhấn liên kết Dashboard nhưng không điều hướng. " + "Trước="
								+ beforeClick + "; Sau=" + driver.getCurrentUrl() + "; locator=" + dashboardLink, e);
					}
					note("URL Dashboard", driver.getCurrentUrl());
					require(driver.getCurrentUrl().matches("https?://[^/]+/dashboard\\.php(?:[?#].*)?"),
							"URL không phải dashboard.php");
					WebElement h1 = visible(By.cssSelector("h1"), "H1 chào Admin");
					mark(h1, "ĐANG TEST: H1 ADMIN");
					note("H1", text(h1));
					require(text(h1).contains("Chào Siêu Cấp Quản Trị Viên!"), "H1 không đúng lời chào admin");
				});
	}

	@Test(priority = 2, description = "TC_ADASH_002 - Admin stats", dependsOnMethods = "TC_ADASH_001_adminNavigation")
	public void TC_ADASH_002_adminStatistics() {
		verify("TC_ADASH_002", "4 chỉ số thống kê Admin",
				"- NGƯỜI DÙNG (2026): 79\n- TỪ VỰNG TỔNG: 146\n- CHỦ ĐỀ: 21\n- LƯỢT HỌC (2026): 372\n- Baseline ban đầu: 79/146/21/372; chỉ cập nhật Expected khi có dữ liệu xác minh\n- Đối chiếu nguồn DB/API độc lập nếu có; chỉ đọc UI không chứng minh số liệu thật",
				() -> {
					restoreDashboard();
					String[] labels = { "NGƯỜI DÙNG (2026)", "TỪ VỰNG TỔNG", "CHỦ ĐỀ", "LƯỢT HỌC (2026)" };
					// Baseline mặc định là số liệu nghiệp vụ được người kiểm thử cung cấp.
					// Chỉ thay baseline khi đã xác minh dữ liệu nguồn, không dùng số đọc từ UI làm
					// Expected.
					String[] keys = { "admin.expectedUsers2026", "admin.expectedVocabularyTotal",
							"admin.expectedTopicsTotal", "admin.expectedLearning2026" };
					int[] baseline = { 79, 146, 21, 372 };
					int[] expected = new int[baseline.length];
					for (int i = 0; i < baseline.length; i++) {
						String configured = System.getProperty(keys[i]);
						try {
							expected[i] = (configured == null || configured.isBlank()) ? baseline[i]
									: Integer.parseInt(configured.trim());
						} catch (NumberFormatException e) {
							throw new AssertionError("Expected không hợp lệ tại VM argument -D" + keys[i] + "="
									+ configured + "; yêu cầu số nguyên", e);
						}
						require(expected[i] >= 0, "Expected không thể âm: " + keys[i]);
					}
					note("Nguồn Expected", "Baseline yêu cầu ban đầu (79/146/21/372),"
							+ " hoặc giá trị đã xác minh qua VM arguments admin.expected*");
					StringBuilder diff = new StringBuilder();
					for (int i = 0; i < labels.length; i++) {
						String xp = "//div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')][.//div[normalize-space(.)='"
								+ labels[i] + "']]";
						WebElement card = visible(By.xpath(xp), "thẻ " + labels[i]);
						mark(card, "ĐANG TEST: " + labels[i]);
						String labelXp = ".//div[normalize-space(.)='" + labels[i] + "']";
						WebElement key = card.findElement(By.xpath(labelXp));
						WebElement val = key.findElement(By.xpath("following-sibling::div[1]"));
						int actual = integer(text(val));
						note(labels[i] + " / expected=" + expected[i], actual);
						if (actual != expected[i])
							diff.append(labels[i]).append(" expected=").append(expected[i]).append(" actual=")
									.append(actual).append("; ");
					}
					note("Đối chiếu DB/API độc lập", "CHƯA CÓ – chưa thể xác nhận các số trên UI là dữ liệu thật");
					if (diff.length() > 0) {
						note("Sai lệch cần điều tra", diff.toString());
						throw new AssertionError("Số liệu UI khác baseline nghiệp vụ: " + diff
								+ " Vui lòng đối chiếu DB/API trước khi cập nhật Expected.");
					}
				});
	}

	@Test(priority = 3, description = "TC_ADASH_003 - New users charts", dependsOnMethods = "TC_ADASH_001_adminNavigation")
	public void TC_ADASH_003_newUserStatistics() {
		verify("TC_ADASH_003", "Thống kê Người Dùng Mới",
				"- Hiện tiêu đề, mô tả\n- Input năm #inputYear là number, khoảng 2000-2099\n- Nút Lọc gửi năm bằng GET\n- Tab Tuần (7 ngày), Tháng (Năm 2026), Theo Các Năm tương tác được\n- adminUserChart tạo dữ liệu và đổi chế độ",
				() -> {
					restoreDashboard();
					WebElement panel = section("Thống kê Người Dùng Mới");
					mark(panel, "ĐANG TEST: THỐNG KÊ NGƯỜI DÙNG");
					String subtitle = text(panel.findElement(By.xpath(".//h3/following-sibling::p")));
					note("Mô tả", subtitle);
					require(subtitle.contains("Theo dõi tốc độ tăng trưởng người dùng mới theo mốc thời gian"),
							"Sai mô tả thống kê");
					WebElement year = panel.findElement(By.id("inputYear"));
					require(year.isDisplayed() && "number".equals(year.getDomAttribute("type")),
							"Bộ lọc năm không phải input number");
					note("Năm đang chọn", year.getDomProperty("value"));
					require("2000".equals(year.getDomAttribute("min")) && "2099".equals(year.getDomAttribute("max")),
							"Giới hạn năm không đúng");
					WebElement filter = panel
							.findElement(By.xpath(".//form[.//*[@id='inputYear']]//button[@type='submit']"));
					require(filter.isDisplayed() && filter.isEnabled() && text(filter).equals("Lọc"),
							"Nút Lọc không khả dụng");
					for (String mode : new String[] { "week", "month", "year" }) {
						WebElement tab = panel
								.findElement(By.cssSelector("button.chart-tab[onclick*=\"'" + mode + "'\"]"));
						require(tab.isDisplayed() && tab.isEnabled(), "Thiếu nút chế độ=" + mode);
						tab.click();
						wait.until(d -> tab.getDomAttribute("class").contains("active"));
						String info = chartSnapshot("adminUserChart");
						note("Chế độ " + mode, text(tab) + "; " + info);
						require(info.contains("chartFound=true") && !info.contains("labels=[]"),
								"Biểu đồ thiếu data chế độ " + mode);
					}
					year.clear();
					year.sendKeys("2025");
					filter.click();
					wait.until(d -> d.getCurrentUrl().contains("year=2025"));
					note("URL sau lọc 2025", driver.getCurrentUrl());
					require("2025".equals(visible(By.id("inputYear"), "input năm").getDomProperty("value")),
							"Năm không được giữ sau lọc");
					// Restore the original year for downstream tests.
					driver.get(DASH_URL);
				});
	}

	@Test(priority = 4, description = "TC_ADASH_004 - Actions and vocabulary", dependsOnMethods = "TC_ADASH_001_adminNavigation")
	public void TC_ADASH_004_adminActionsAndVocabulary() {
		verify("TC_ADASH_004", "Thao tác nhanh và danh sách từ vựng",
				"- 4 liên kết: admin_type.php, admin_vocab.php, admin.php, notifications.php\n- Danh sách từ vựng và bộ lọc checkbox tất cả chủ đề\n- Mỗi thẻ: tên, IPA, nghĩa, chủ đề, hình ảnh hoặc placeholder theo UI\n- Quản lý → mở admin_vocab.php\n- Ghi số thẻ đang render (không đồng nhất tổng DB)",
				() -> {
					restoreDashboard();
					String[][] links = { { "Quản Lý Loại Từ", "admin_type.php" },
							{ "Quản Lý Từ Vựng", "admin_vocab.php" }, { "Quản Lý Người Dùng", "admin.php" },
							{ "Thông Báo", "notifications.php" } };
					for (String[] link : links) {
						WebElement a = visible(
								By.xpath("//a[@href='" + link[1] + "' and normalize-space(.)='" + link[0] + "']"),
								link[0]);
						note("Liên kết " + link[0], a.getDomAttribute("href"));
						a.click();
						wait.until(d -> d.getCurrentUrl().contains(link[1]));
						note("URL sau click " + link[0], driver.getCurrentUrl());
						restoreDashboard();
					}
					WebElement panel = section("Danh sách từ vựng");
					mark(panel, "ĐANG TEST: DANH SÁCH TỪ VỰNG");
					WebElement dropdown = panel.findElement(By.id("topicDropdownBtn"));
					dropdown.click();
					WebElement menu = visible(By.id("topicDropdownMenu"), "popup chủ đề");
					List<WebElement> checkboxes = menu
							.findElements(By.cssSelector("#topicCheckboxList input.topic-checkbox"));
					require(!checkboxes.isEmpty(), "Không có checkbox chủ đề");
					note("Số chủ đề trong bộ lọc", checkboxes.size());
					WebElement all = menu.findElement(By.id("selectAllTopics"));
					note("Tất cả chủ đề đang chọn", all.isSelected());
					require(all.isSelected(), "Mặc định Tất cả chủ đề chưa được chọn");
					all.click();
					require(checkboxes.stream().noneMatch(WebElement::isSelected),
							"Bỏ chọn tất cả không cập nhật checkbox");
					note("Bỏ chọn tất cả", "Các checkbox đã được bỏ chọn");
					all.click();
					require(checkboxes.stream().allMatch(WebElement::isSelected),
							"Chọn tất cả không cập nhật checkbox");
					note("Chọn lại tất cả", "Các checkbox đã được chọn");
					WebElement container = visible(By.id("vocabListContainer"), "danh sách từ vựng");
					// Lấy snapshot DOM trong một lần thay vì gọi WebDriver hàng trăm lần.
					// Điều này giảm nguy cơ nghẽn Chrome/DevTools khi danh sách có nhiều thẻ.
					@SuppressWarnings("unchecked")
					List<java.util.Map<String, Object>> entries = (List<java.util.Map<String, Object>>) js
							.executeScript("return Array.from(arguments[0].children).map((r,i)=>{"
									+ "const q=s=>r.querySelector(s);const t=e=>e?(e.textContent||'').trim():'';"
									+ "const img=q('img');const icon=q('svg');"
									+ "return {row:i+1,word:t(q('span.font-extrabold')),"
									+ "ipa:t(q('span.font-mono')),meaning:t(q('div.line-clamp-1')),"
									+ "topic:t(q('div.text-right span')),hasImg:!!img,"
									+ "imgSrc:img?(img.getAttribute('src')||''):'',"
									+ "imgLoaded:img?img.complete&&img.naturalWidth>0:false,placeholder:!!icon};"
									+ "});", container);
					require(!entries.isEmpty(), "Danh sách từ vựng rỗng");
					note("Số từ vựng đang render", entries.size());
					int withImage = 0, placeholders = 0;
					for (int i = 0; i < entries.size(); i++) {
						java.util.Map<String, Object> row = entries.get(i);
						String word = String.valueOf(row.get("word"));
						String ipa = String.valueOf(row.get("ipa"));
						String meaning = String.valueOf(row.get("meaning"));
						String topic = String.valueOf(row.get("topic"));
						require(!word.isBlank() && !ipa.isBlank() && !meaning.isBlank() && !topic.isBlank(),
								"Thiếu tên/IPA/nghĩa/chủ đề ở dòng " + (i + 1) + ": " + row);
						if (Boolean.TRUE.equals(row.get("hasImg"))) {
							require(Boolean.TRUE.equals(row.get("imgLoaded")),
									"Ảnh lỗi tải tại dòng " + (i + 1) + "; src=" + row.get("imgSrc"));
							withImage++;
						} else {
							require(Boolean.TRUE.equals(row.get("placeholder")),
									"Thiếu ảnh và placeholder tại dòng " + (i + 1));
							placeholders++;
						}
						if (i < 5)
							note("Từ " + (i + 1), word + " | " + ipa + " | " + meaning + " | " + topic);
					}
					note("Ảnh thật / placeholder", withImage + " / " + placeholders);
					dropdown.click();
					// Tìm lại phần tử sau khi bộ lọc render lại DOM.
					WebElement manage = visible(
							By.xpath("//a[@href='admin_vocab.php' and contains(normalize-space(.),'Quản lý') ]"),
							"nút Quản lý từ vựng");
					manage.click();
					wait.until(d -> d.getCurrentUrl().contains("admin_vocab.php"));
					note("URL sau Quản lý →", driver.getCurrentUrl());
					require(driver.getCurrentUrl().contains("admin_vocab.php"), "Điều hướng Quản lý sai");
					restoreDashboard();
					// Placeholder là thiết kế sẵn có trong HTML, không mặc định là lỗi dữ liệu.
					// Nếu yêu cầu nghiệp vụ bắt buộc ảnh thật cho tất cả mục, bật VM argument dưới
					// đây.
					// Giao diện nguồn có SVG placeholder có chủ ý khi một bản ghi chưa có ảnh.
					// Chỉ bật chế độ bắt buộc ảnh thật khi yêu cầu nghiệp vụ quy định rõ.
					boolean requireImages = Boolean
							.parseBoolean(System.getProperty("admin.requireRealImages", "false"));
					note("Cấu hình yêu cầu ảnh thật", requireImages);
					note("Quy tắc ảnh", requireImages ? "Bắt buộc mọi mục có ảnh thật (cấu hình strict)"
							: "Chấp nhận ảnh thật hoặc SVG placeholder theo giao diện thực tế");
					if (requireImages && placeholders > 0) {
						throw new AssertionError("Có " + placeholders + "/" + entries.size()
								+ " từ vựng dùng placeholder. Nếu UI cho phép ảnh mặc định, XÓA VM argument "
								+ "-Dadmin.requireRealImages=true hoặc đặt =false."
								+ " Nếu đặc tả bắt buộc ảnh thật, đây là lỗi dữ liệu thực, không được ép PASS.");
					}
				});
	}

	@Test(priority = 5, description = "TC_ADASH_005 - Top five ranking", dependsOnMethods = "TC_ADASH_001_adminNavigation")
	public void TC_ADASH_005_topFiveRankings() {
		verify("TC_ADASH_005", "Top 5 bảng xếp hạng",
				"- 3 tab Chuỗi, Điểm, Từ vựng\n- Mỗi tab đúng 5 dòng, đủ hạng/tên/giá trị\n- Giá trị theo thứ tự giảm dần; đọc dữ liệu DOM thực tế",
				() -> {
					restoreDashboard();
					String[][] modes = { { "streak", "Chuỗi" }, { "score", "Điểm" }, { "words", "Từ vựng" } };
					for (String[] mode : modes) {
						WebElement button = visible(By.cssSelector("button.lb-tab[onclick*=\"'" + mode[0] + "'\"]"),
								"tab " + mode[1]);
						button.click();
						WebElement panel = visible(By.id("tab-" + mode[0]), "panel " + mode[1]);
						mark(panel, "ĐANG TEST: TOP 5 " + mode[1]);
						List<WebElement> rows = panel.findElements(By.cssSelector(".rank-row"));
						note("Tab " + mode[1] + " / số dòng", rows.size());
						require(rows.size() == 5, "Tab " + mode[1] + " không có đúng 5 dòng");
						int previous = Integer.MAX_VALUE;
						for (int i = 0; i < rows.size(); i++) {
							WebElement r = rows.get(i);
							List<WebElement> spans = r.findElements(By.cssSelector("span"));
							require(!spans.isEmpty(), "Thiếu username hạng " + (i + 1));
							String username = text(spans.get(0));
							String rank = text(r.findElement(By.cssSelector(".rounded-full.font-bold")));
							String valueString = text(r.findElement(By.cssSelector("div.shrink-0")));
							int value = integer(valueString);
							note(mode[1] + " #" + (i + 1), username + " | " + valueString);
							require(String.valueOf(i + 1).equals(rank), "Sai STT hạng " + (i + 1) + " / " + rank);
							require(!username.isBlank() && value <= previous,
									"Thiếu user hoặc bảng chưa sắp giảm: " + mode[1]);
							previous = value;
						}
					}
				});
	}

	@Test(priority = 6, description = "TC_ADASH_006 - Topic usage top ten", dependsOnMethods = "TC_ADASH_001_adminNavigation")
	public void TC_ADASH_006_topicUsage() {
		verify("TC_ADASH_006", "Top 10 truy cập theo chủ đề và biểu đồ",
				"- Tiêu đề Từ vựng theo chủ đề\n- Bảng gồm STT, Tên loại từ, Tổng lượt truy cập\n- Top 10 đúng 10 dòng có số truy cập giảm dần\n- adminTopicChart có dữ liệu Chart.js; nhãn/giá trị khớp bảng khi biểu đồ biểu diễn cùng Top 10",
				() -> {
					restoreDashboard();
					WebElement panel = section("Từ vựng theo chủ đề");
					mark(panel, "ĐANG TEST: TOP 10 CHỦ ĐỀ");
					WebElement table = panel.findElement(By.cssSelector("table"));
					List<WebElement> heads = table.findElements(By.cssSelector("thead th"));
					require(heads.size() == 3, "Bảng không đủ 3 cột");
					String[] expected = { "STT", "Tên loại từ", "Tổng lượt truy cập" };
					for (int i = 0; i < 3; i++)
						require(text(heads.get(i)).equalsIgnoreCase(expected[i]), "Sai tên cột " + (i + 1));
					List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));
					note("Số dòng bảng", rows.size());
					require(rows.size() == 10, "Top 10 phải có đúng 10 dòng");
					List<String> topics = new ArrayList<>();
					List<Integer> counts = new ArrayList<>();
					int previous = Integer.MAX_VALUE;
					for (int i = 0; i < rows.size(); i++) {
						List<WebElement> cells = rows.get(i).findElements(By.tagName("td"));
						require(cells.size() == 3, "Dòng " + (i + 1) + " thiếu cột");
						require(text(cells.get(0)).equals("#" + (i + 1)), "Sai STT dòng " + (i + 1));
						String topic = text(cells.get(1));
						int count = integer(text(cells.get(2)));
						require(!topic.isBlank() && count <= previous, "Tên chủ đề rỗng hoặc chưa sắp giảm");
						previous = count;
						topics.add(topic);
						counts.add(count);
						note("#" + (i + 1), topic + " = " + count);
					}
					WebElement canvas = panel.findElement(By.id("adminTopicChart"));
					require(canvas.isDisplayed(), "adminTopicChart không hiển thị");
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> chart = (java.util.Map<String, Object>) js.executeScript(
							"const e=arguments[0];const c=window.Chart&&Chart.getChart?Chart.getChart(e):null;"
									+ "return c?{type:c.config.type,labels:c.data.labels,datasets:c.data.datasets.map(x=>x.data)}:null;",
							canvas);
					require(chart != null, "Không tìm thấy instance Chart.js của adminTopicChart");
					note("Chart.js", chart);
					require("bar".equals(String.valueOf(chart.get("type"))), "adminTopicChart không phải biểu đồ cột");
					List<?> labels = (List<?>) chart.get("labels");
					List<?> datasets = (List<?>) chart.get("datasets");
					require(labels != null && datasets != null && !datasets.isEmpty(), "Biểu đồ không có dữ liệu");
					List<?> values = (List<?>) datasets.get(0);
					require(labels.size() == 10 && values.size() == 10, "Biểu đồ phải có 10 nhãn/cột tương ứng Top 10");
					for (int i = 0; i < 10; i++) {
						require(topics.get(i).equals(String.valueOf(labels.get(i))),
								"Tên biểu đồ khác bảng tại " + (i + 1));
						require(counts.get(i) == ((Number) values.get(i)).intValue(),
								"Giá trị chart khác bảng tại " + (i + 1));
					}
				});
	}

	private String chartSnapshot(String id) {
		Object o = js.executeScript("const e=document.getElementById(arguments[0]);"
				+ "const c=e&&window.Chart&&Chart.getChart?Chart.getChart(e):null;"
				+ "return c?{chartFound:true,labels:c.data.labels,values:c.data.datasets.map(d=>d.data)}:{chartFound:false,labels:[]};",
				id);
		return String.valueOf(o);
	}

	private void printResultOnce(TestResult result) {

		// Gom kết quả và in một bảng tổng hợp duy nhất trong @AfterClass.

		// Không ghi đè result.details để Excel vẫn giữ thông tin lỗi đầy đủ.

		result.printed = true;

	}

	private void printResult(TestResult r) {

		// Bảng console theo từng testcase; các cột Expected/Actual tự xuống dòng để dễ
		// đọc.

		final int idW = 14, nameW = 25, expectedW = 34, actualW = 48, statusW = 8;

		String border = "+" + repeat('-', idW + 2) + "+" + repeat('-', nameW + 2)

				+ "+" + repeat('-', expectedW + 2) + "+" + repeat('-', actualW + 2)

				+ "+" + repeat('-', statusW + 2) + "+";

		System.out.println("\n" + border);

		System.out.println("| " + pad("TEST CASE ID", idW) + " | " + pad("TÊN TEST", nameW)

				+ " | " + pad("EXPECTED", expectedW) + " | " + pad("ACTUAL", actualW)

				+ " | " + pad("RESULT", statusW) + " |");

		System.out.println(border);

		List<String> idLines = wrapLines(r.id, idW);

		List<String> nameLines = wrapLines(r.name, nameW);

		List<String> expectedLines = wrapLines(r.expected, expectedW);

		List<String> actualLines = wrapLines(r.actual == null ? "(chưa có dữ liệu)" : r.actual, actualW);

		List<String> statusLines = wrapLines(r.status == null ? "NOT RUN" : r.status, statusW);

		int lines = Math.max(Math.max(idLines.size(), nameLines.size()),

				Math.max(Math.max(expectedLines.size(), actualLines.size()), statusLines.size()));

		String color = "PASS".equals(r.status) ? "\u001B[32m" : "FAIL".equals(r.status) ? "\u001B[31m" : "\u001B[33m";

		for (int i = 0; i < lines; i++) {

			String status = i < statusLines.size() ? statusLines.get(i) : "";

			if (i == 0)
				status = color + status + "\u001B[0m";

			System.out.println("| " + pad(at(idLines, i), idW) + " | " + pad(at(nameLines, i), nameW)

					+ " | " + pad(at(expectedLines, i), expectedW) + " | " + pad(at(actualLines, i), actualW)

					+ " | " + pad(status, statusW) + " |");

		}

		System.out.println(border);

		if (r.screenshot != null && !r.screenshot.isBlank())
			System.out.println("Evidence: " + r.screenshot);

	}

	private static String at(List<String> lines, int index) {

		return index < lines.size() ? lines.get(index) : "";

	}

	private static String pad(String value, int width) {

		if (value == null)
			value = "";

		// ANSI color codes do not affect visible width.

		String plain = value.replaceAll("\u001B\\[[0-9;]*m", "");

		int spaces = Math.max(0, width - plain.length());

		return value + " ".repeat(spaces);

	}

	private static String repeat(char c, int count) {

		return String.valueOf(c).repeat(Math.max(0, count));

	}

	private static List<String> wrapLines(String value, int width) {
		List<String> lines = new ArrayList<>();
		if (value == null || value.isBlank()) {
			lines.add("");
			return lines;
		}
		for (String paragraph : value.replace("\r", "").split("\n", -1)) {
			if (paragraph.isBlank()) {
				lines.add("");
				continue;
			}
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.trim().split("\\s+")) {
				if (word.length() > width) {
					if (line.length() > 0) {
						lines.add(line.toString());
						line.setLength(0);
					}
					for (int start = 0; start < word.length(); start += width)
						lines.add(word.substring(start, Math.min(word.length(), start + width)));
				} else {
					if (line.length() > 0 && line.length() + 1 + word.length() > width) {
						lines.add(line.toString());
						line.setLength(0);
					}
					if (line.length() > 0)
						line.append(' ');
					line.append(word);
				}
			}
			if (line.length() > 0)
				lines.add(line.toString());
		}
		return lines;
	}

	private void printConsoleTable() {

		final int idW = 14, nameW = 25, expectedW = 34, actualW = 48, statusW = 8;

		String border = "+" + repeat('-', idW + 2) + "+" + repeat('-', nameW + 2)

				+ "+" + repeat('-', expectedW + 2) + "+" + repeat('-', actualW + 2)

				+ "+" + repeat('-', statusW + 2) + "+";

		System.out.println("\n====================== DASHBOARD TEST RESULTS ======================");

		System.out.println(border);

		System.out.println("| " + pad("TEST CASE ID", idW) + " | " + pad("TÊN TEST", nameW)

				+ " | " + pad("EXPECTED", expectedW) + " | " + pad("ACTUAL", actualW)

				+ " | " + pad("RESULT", statusW) + " |");

		System.out.println(border);

		for (TestResult r : results) {

			List<String> idLines = wrapLines(r.id, idW);

			List<String> nameLines = wrapLines(r.name, nameW);

			List<String> expectedLines = wrapLines(r.expected, expectedW);

			List<String> actualLines = wrapLines(r.actual == null ? "(chưa có dữ liệu)" : r.actual, actualW);

			List<String> statusLines = wrapLines(r.status == null ? "NOT RUN" : r.status, statusW);

			int lines = Math.max(Math.max(idLines.size(), nameLines.size()),

					Math.max(Math.max(expectedLines.size(), actualLines.size()), statusLines.size()));

			String color = "PASS".equals(r.status) ? "\u001B[32m"

					: "FAIL".equals(r.status) ? "\u001B[31m" : "\u001B[33m";

			for (int i = 0; i < lines; i++) {

				String status = at(statusLines, i);

				if (i == 0)
					status = color + status + "\u001B[0m";

				System.out.println("| " + pad(at(idLines, i), idW) + " | " + pad(at(nameLines, i), nameW)

						+ " | " + pad(at(expectedLines, i), expectedW) + " | " + pad(at(actualLines, i), actualW)

						+ " | " + pad(status, statusW) + " |");

			}

			if (r.screenshot != null && !r.screenshot.isBlank())

				System.out.println("| Evidence: " + truncate(r.screenshot, 125));

			System.out.println(border);

		}

		System.out.println("=====================================================================");

	}

	private String chartInfo(String canvasId) {

		List<WebElement> canvases = driver.findElements(By.id(canvasId));

		boolean displayed = !canvases.isEmpty() && canvases.get(0).isDisplayed();

		Object data = js.executeScript(

				"const el=document.getElementById(arguments[0]);"

						+ "if(!el) return {chartFound:false,labels:[],values:[]};"

						+ "let c=(window.Chart&&Chart.getChart)?Chart.getChart(el):null;"

						+ "if(!c && window.Chart && Chart.instances){"

						+ "for(const k in Chart.instances){if(Chart.instances[k]&&Chart.instances[k].canvas===el){c=Chart.instances[k];break;}}}"

						+ "if(!c) return {chartFound:false,labels:[],values:[]};"

						+ "return {chartFound:true,labels:(c.data.labels||[]),values:(c.data.datasets||[]).map(d=>d.data||[])};",

				canvasId);

		String dataString = String.valueOf(data);

		int labelCount = 0;

		if (data instanceof java.util.Map<?, ?> map && map.get("labels") instanceof List<?> labels)
			labelCount = labels.size();

		return "canvasDisplayed=" + displayed + "; chartFound=" + dataString.contains("chartFound=true")

				+ "; labelCount=" + labelCount + "; chart=" + dataString;

	}

	private void highlightBestMatchingElement(String testName) {

		String lower = testName.toLowerCase();

		By locator;

		if (lower.contains("url"))
			locator = By.tagName("body");

		else if (lower.contains("lời chào") || lower.contains("thống kê"))
			locator = By.xpath("//*[contains(normalize-space(.),'Chào')]");

		else if (lower.contains("scholar") || lower.contains("tiến độ"))
			locator = By.cssSelector("[role='progressbar'], .progress, [class*='progress']");

		else if (lower.contains("chủ đề"))
			locator = By.id("topicChart");

		else if (lower.contains("xếp hạng"))
			locator = By.id("tab-streak");

		else if (lower.contains("tuần"))
			locator = By.id("weeklyChart");

		else if (lower.contains("từ vựng"))
			locator = By.cssSelector(".glass-card");

		else
			locator = By.xpath("//*[contains(normalize-space(.),'Học tập')]");

		for (WebElement e : driver.findElements(locator)) {

			try {
				if (e.isDisplayed()) {
					highlightElement(e, "ĐANG TEST: " + testName);
					return;
				}
			}

			catch (StaleElementReferenceException ignored) {
			}

		}

		highlightElement(driver.findElement(By.tagName("body")), "ĐANG TEST: " + testName);

	}

	private void highlightElement(WebElement element, String label) {

		try {

			js.executeScript(

					"if(window.__seleniumTestOutline){window.__seleniumTestOutline.remove();}"

							+ "const e=arguments[0]; e.scrollIntoView({block:'center',inline:'nearest'});"

							+ "e.style.setProperty('outline','3px solid #ff0000','important');"

							+ "e.style.setProperty('outline-offset','2px','important');"

							+ "e.style.setProperty('box-shadow','0 0 0 5px rgba(255,0,0,.20)','important');"

							+ "const b=document.createElement('div'); b.id='__seleniumTestOutline';"

							+ "b.textContent=arguments[1]; Object.assign(b.style,{position:'fixed',top:'8px',right:'8px',"

							+ "zIndex:'2147483647',background:'#c90000',color:'#fff',padding:'10px 14px',"

							+ "font:'bold 14px Arial,sans-serif',border:'2px solid white',borderRadius:'6px',"

							+ "boxShadow:'0 2px 10px rgba(0,0,0,.35)',maxWidth:'55vw',whiteSpace:'normal'});"

							+ "document.body.appendChild(b); window.__seleniumTestOutline=b;",

					element, label);

		} catch (Exception ignored) {
			showBanner(label);
		}

	}

	private void showBanner(String label) {

		if (js == null)
			return;

		try {

			js.executeScript(

					"if(window.__seleniumTestOutline)window.__seleniumTestOutline.remove();"

							+ "const b=document.createElement('div');b.id='__seleniumTestOutline';b.textContent=arguments[0];"

							+ "Object.assign(b.style,{position:'fixed',top:'8px',right:'8px',zIndex:'2147483647',"

							+ "background:'#c90000',color:'#fff',padding:'10px 14px',font:'bold 14px Arial,sans-serif',"

							+ "border:'2px solid white',borderRadius:'6px',maxWidth:'55vw'});document.body.appendChild(b);"

							+ "window.__seleniumTestOutline=b;",
					label);

		} catch (Exception ignored) {
		}

	}

	private String takeScreenshot(String filePrefix) {

		if (!(driver instanceof TakesScreenshot))
			return "";

		try {

			File dir = new File("DashboardAdmin_Screenshots");

			if (!dir.exists() && !dir.mkdirs())
				return "Không tạo được thư mục screenshot";

			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));

			File target = new File(dir, filePrefix + "_" + timestamp + ".png");

			File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

			java.nio.file.Files.copy(source.toPath(), target.toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);

			return target.getAbsolutePath();

		} catch (Exception e) {

			return "Chụp ảnh thất bại: " + e.getMessage();

		}

	}

	@AfterClass(alwaysRun = true)

	public void tearDownAndWriteReport() {

		try {
			writeExcelReport();
		}

		catch (Exception e) {
			System.err.println("Không ghi được Excel report: " + e.getMessage());
		}

		printConsoleTable();

		if (driver != null) {

			try {
				driver.quit();
			} catch (Exception ignored) {
			}

		}

		System.out.println("\n========== TỔNG KẾT DASHBOARD TEST ==========");

		long pass = results.stream().filter(r -> "PASS".equals(r.status)).count();

		long fail = results.stream().filter(r -> "FAIL".equals(r.status)).count();

		System.out.println("Login: " + (loginSucceeded ? "PASS" : "FAIL/CHƯA HOÀN TẤT"));

		System.out.println("Test đã ghi nhận: " + results.size() + " | PASS=" + pass + " | FAIL=" + fail);

		System.out.println("Excel: " + new File("DashboardAdmin_TestResults.xlsx").getAbsolutePath());

		System.out.println("Screenshots: " + new File("DashboardAdmin_Screenshots").getAbsolutePath());

	}

	private void writeExcelReport() throws Exception {

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {

			XSSFSheet sheet = workbook.createSheet("Dashboard Test Results");

			sheet.createFreezePane(0, 1);

			sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, results.size()), 0, 6));

			CellStyle header = workbook.createCellStyle();

			header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());

			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			header.setAlignment(HorizontalAlignment.CENTER);

			header.setVerticalAlignment(VerticalAlignment.CENTER);

			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());

			header.setFont(headerFont);
			header.setWrapText(true);

			CellStyle normal = workbook.createCellStyle();
			normal.setWrapText(true);
			normal.setVerticalAlignment(VerticalAlignment.TOP);

			CellStyle passStyle = workbook.createCellStyle();
			passStyle.cloneStyleFrom(normal);

			passStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle failStyle = workbook.createCellStyle();
			failStyle.cloneStyleFrom(normal);

			failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			String[] headers = { "Test Case ID", "Test Case", "Expected", "Actual", "Result", "Details", "Screenshot" };

			Row head = sheet.createRow(0);
			head.setHeightInPoints(30);

			for (int i = 0; i < headers.length; i++) {
				Cell c = head.createCell(i);
				c.setCellValue(headers[i]);
				c.setCellStyle(header);
			}

			int rowNum = 1;

			for (TestResult result : results) {

				Row row = sheet.createRow(rowNum++);

				String[] values = { result.id, result.name, result.expected,

						result.actual == null ? "Chưa có dữ liệu" : result.actual,

						result.status == null ? "NOT RUN" : result.status,

						result.details == null ? "" : result.details,

						result.screenshot == null ? "" : result.screenshot };

				CellStyle style = "PASS".equals(result.status) ? passStyle
						: "FAIL".equals(result.status) ? failStyle : normal;

				int maxLines = 1;

				for (int i = 0; i < values.length; i++) {

					Cell cell = row.createCell(i);
					cell.setCellValue(values[i]);
					cell.setCellStyle(i == 4 ? style : normal);

					maxLines = Math.max(maxLines,
							Math.min(19, values[i].split("\\n").length + values[i].length() / 70));

				}

				row.setHeightInPoints(Math.min(350, 18f * maxLines));

			}

			int[] widths = { 16, 26, 48, 62, 12, 42, 55 };

			for (int i = 0; i < widths.length; i++)
				sheet.setColumnWidth(i, widths[i] * 256);

			try (FileOutputStream out = new FileOutputStream("DashboardAdmin_TestResults.xlsx")) {
				workbook.write(out);
			}

		}

	}

	private String safeElementText(WebElement element) {

		try {

			String text = element.getText();

			if (text == null || text.isBlank())
				text = element.getDomAttribute("aria-label");

			if (text == null || text.isBlank())
				text = element.getDomAttribute("value");

			if (text == null || text.isBlank())
				text = element.getDomAttribute("href");

			return text == null ? "(không có text/label/href)" : text.trim();

		} catch (Exception e) {

			return "(không đọc được text: " + e.getClass().getSimpleName() + ")";

		}

	}

	private String visibleClickableSummary() {

		List<String> items = new ArrayList<>();

		for (WebElement element : driver.findElements(
				By.cssSelector("a, button, input[type='button'], input[type='submit'], [role='button']"))) {

			try {

				if (element.isDisplayed()) {

					String item = element.getTagName() + "[text=" + safeElementText(element)

							+ ", href=" + element.getDomAttribute("href") + "]";

					items.add(item);

					if (items.size() >= 30)
						break;

				}

			} catch (StaleElementReferenceException ignored) {
			}
		}

		return items.toString();

	}

	private String safeBodyText() {

		try {
			return driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
		}

		catch (Exception e) {
			return "";
		}

	}

	private static String findFirstText(String body, String... candidates) {

		for (String candidate : candidates) {

			int index = body.indexOf(candidate);

			if (index >= 0)
				return body.substring(index, Math.min(body.length(), index + 100));

		}

		return "";

	}

	private static String xpathLiteral(String value) {
		if (!value.contains("'"))
			return "'" + value + "'";
		if (!value.contains("\""))
			return "\"" + value + "\"";
		String[] parts = value.split("'", -1);
		return "concat('" + String.join("',\"'\",'", parts) + "')";
	}

	private static String wrap(String value, int width) {

		if (value == null)
			return "(null)";

		StringBuilder out = new StringBuilder();

		for (String paragraph : value.split("\\n")) {

			String line = "";

			for (String word : paragraph.split("\\s+")) {

				if (!line.isEmpty() && line.length() + word.length() + 1 > width) {

					out.append("\n            ").append(line);
					line = word;

				} else
					line = line.isEmpty() ? word : line + " " + word;

			}

			if (!line.isEmpty())
				out.append(out.length() == 0 ? "" : "\n            ").append(line);

		}

		return out.toString();

	}

	private static String truncate(String value, int max) {

		if (value == null || value.length() <= max)
			return value;

		return value.substring(0, max) + "...";

	}

}
