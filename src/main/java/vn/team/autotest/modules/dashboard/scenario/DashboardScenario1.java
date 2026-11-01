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
 * DashboardScenario1 - standalone Selenium/TestNG scenario.
 * 
 * Does not depend on BaseScenario, LoginPage, DashboardPage, or AppConfig.
 * 
 * Configure VM arguments: -Dtest.username=... -Dtest.password=...
 * 
 */

public class DashboardScenario1 {

	private static final String BASE_URL = "https://learnenglish.plt.pro.vn/";

	private static final String DASHBOARD_URL_PART = "dashboard.php";

	private static final int WAIT_SECONDS = 15;

	private WebDriver driver;

	private WebDriverWait wait;

	private JavascriptExecutor js;

	private final List<TestResult> results = new ArrayList<>();

	private String activeTest = "Đang chuẩn bị đăng nhập";

	private boolean loginSucceeded = false;

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

			wait.until(d -> d.getCurrentUrl().contains(DASHBOARD_URL_PART));

		} catch (TimeoutException e) {

			String pageText = safeBodyText();

			throw new AssertionError("Đăng nhập chưa vào Dashboard. URL hiện tại: " + driver.getCurrentUrl()

					+ ". Nội dung trang: " + truncate(pageText, 500), e);

		}

		showBanner("LOGIN HOÀN TẤT: Dashboard đã mở");

		System.out.println("[LOGIN PASS] Dashboard URL: " + driver.getCurrentUrl());

	}

	@Test(description = "TC_DASH_001 - Dashboard URL")

	public void TC_DASH_001_dashboardUrl() {

		check("TC_DASH_001", "Dashboard URL", "URL chứa " + DASHBOARD_URL_PART,

				() -> driver.getCurrentUrl(), value -> value.contains(DASHBOARD_URL_PART));

	}

	@Test(description = "TC_DASH_002 - Greeting and statistics", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_002_dashboardGreetingAndStats() {

		check("TC_DASH_002", "Lời chào và thống kê", "Có lời chào người dùng và khu vực thống kê",

				() -> {

					String body = safeBodyText();

					String greeting = findFirstText(body, "Chào Phạm Thị Thanh Thảo", "Chào");

					boolean stats = body.contains("Tổng") || body.contains("điểm") || body.contains("Từ vựng");

					return "Greeting=" + (greeting.isBlank() ? "không tìm thấy" : greeting)

							+ "; có từ khóa thống kê=" + stats;

				}, value -> !value.contains("Greeting=không tìm thấy") && value.endsWith("true"));

	}

	@Test(description = "TC_DASH_003 - Scholar progress", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_003_scholarProgress() {

		check("TC_DASH_003", "Tiến độ Scholar",
				"Có cấp/điểm và thanh tiến độ (kể cả thanh dùng inline style width: 5%)",

				() -> {

					String body = safeBodyText();

					boolean hasLevel = body.contains("CẤP") || body.contains("Cấp") || body.contains("Scholar");

					boolean hasScore = body.contains("điểm") || body.contains("Điểm");

					@SuppressWarnings("unchecked")

					List<String> visibleBars = (List<String>) js.executeScript(

							"const all=[...document.querySelectorAll('body *')];"

									+ "const visible=e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);"

									+ "return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'};"

									+ "let level=all.find(e=>e.children.length===0&&/CẤP|SCHOLAR/i.test(e.textContent||''));"

									+ "let scope=level; for(let i=0;i<5&&scope&&scope.parentElement;i++) scope=scope.parentElement;"

									+ "let roots=scope?[scope]:all; let found=[];"

									+ "for(const root of roots){for(const e of root.querySelectorAll('[style*=width], [role=progressbar], progress')){"

									+ "if(!visible(e)) continue; const w=e.style.width||getComputedStyle(e).width;"

									+ "if(e.matches('[role=progressbar],progress')||/%/.test(w))"

									+ "found.push(e.tagName.toLowerCase()+'.'+(e.className&&typeof e.className==='string'?e.className:'')+' width='+w);}}"

									+ "return [...new Set(found)].slice(0,20);");

					return "Có cấp/Scholar=" + hasLevel + "; có điểm=" + hasScore

							+ "; progress elements=" + visibleBars.size() + " " + visibleBars;

				}, value -> value.contains("Có cấp/Scholar=true") && value.contains("có điểm=true")

						&& !value.contains("progress elements=0"));

	}

	@Test(description = "TC_DASH_004 - Topic chart", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_004_topicChart() {

		check("TC_DASH_004", "Biểu đồ chủ đề", "Canvas topicChart hiển thị và Chart.js có dữ liệu",

				() -> chartInfo("topicChart"), value -> value.contains("canvasDisplayed=true")

						&& value.contains("chartFound=true") && value.contains("labels=")

						&& !value.contains("labels=[]"));

	}

	@Test(description = "TC_DASH_005 - Leaderboards", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_005_leaderboards() {

		check("TC_DASH_005", "Bảng xếp hạng", "Có 3 tab Chuỗi/Điểm/Từ vựng; mỗi tab hiển thị ít nhất 5 thẻ xếp hạng",

				() -> {

					String[] tabNames = { "Chuỗi", "Điểm", "Từ vựng" };

					String[] tabKeys = { "streak", "score", "words" };

					List<String> details = new ArrayList<>();

					int tabsFound = 0;

					int tabsWithFiveRows = 0;

					for (int i = 0; i < tabKeys.length; i++) {

						final String key = tabKeys[i];

						WebElement tab = null;

						for (WebElement candidate : driver.findElements(By.cssSelector("button.lb-tab"))) {

							try {

								if (candidate.isDisplayed() && candidate.getText().trim().equals(tabNames[i])) {

									tab = candidate;

									break;

								}

							} catch (StaleElementReferenceException ignored) {
							}

						}

						if (tab == null) {

							details.add(tabNames[i] + ": không tìm thấy tab");

							continue;

						}

						tabsFound++;

						try {

							js.executeScript("arguments[0].click();", tab);

							WebElement panel = wait.until(d -> {

								WebElement p = d.findElement(By.id("tab-" + key));

								return p.isDisplayed() ? p : null;

							});

							// Mỗi mục trong leaderboard là một div card, không phải <table><tr>.

							List<WebElement> rows = panel.findElements(By.xpath("./div"));

							int visibleRows = 0;

							for (WebElement row : rows) {

								try {
									if (row.isDisplayed() && !row.getText().isBlank())
										visibleRows++;
								}

				catch (StaleElementReferenceException ignored) {
								}

							}

							details.add(tabNames[i] + "=" + visibleRows + " dòng");

							if (visibleRows >= 5)
								tabsWithFiveRows++;

						} catch (Exception e) {

							details.add(tabNames[i] + ": lỗi đọc panel - " + e.getClass().getSimpleName());

						}

					}

					// Trả về tab mặc định Chuỗi để không làm thay đổi trạng thái Dashboard sau
					// testcase.

					try {

						WebElement streakTab = driver.findElement(
								By.xpath("//button[contains(@class,'lb-tab') and normalize-space()='Chuỗi']"));

						js.executeScript("arguments[0].click();", streakTab);

					} catch (Exception ignored) {
					}

					return "tabsFound=" + tabsFound + "; tabsWithAtLeast5Rows=" + tabsWithFiveRows

							+ "; details=" + details;

				}, value -> value.contains("tabsFound=3") && value.contains("tabsWithAtLeast5Rows=3"));

	}

	@Test(description = "TC_DASH_006 - Weekly chart", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_006_weeklyChart() {

		check("TC_DASH_006", "Biểu đồ hoạt động tuần", "Canvas weeklyChart hiển thị; có 7 nhãn ngày T2 đến CN",

				() -> chartInfo("weeklyChart"), value -> value.contains("canvasDisplayed=true")

						&& value.contains("chartFound=true") && value.contains("labelCount=7"));

	}

	@Test(description = "TC_DASH_007 - Focus vocabulary", dependsOnMethods = "TC_DASH_001_dashboardUrl")
	public void TC_DASH_007_focusVocabulary() {
		check("TC_DASH_007", "Từ vựng tiêu điểm",
				"Card hiển thị; từ tiếng Anh, IPA, nghĩa tiếng Việt không rỗng; "
						+ "nút phát âm có mặt; trạng thái câu ví dụ được ghi nhận (UI hiện ẩn ví dụ)",
				() -> inspectFocusVocabulary(), actual -> actual.startsWith("VALIDATED:"));
	}

	// Selectors grounded in the authenticated HTML provided for dashboard.php.
	// Limit all field lookups to the unique, visible .glass-card containing the
	// heading.
	private static final By FOCUS_CARD = By
			.xpath("//div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')]"
					+ "[contains(normalize-space(.),'TỪ VỰNG TIÊU ĐIỂM')]");
	private static final By FOCUS_WORD = By.cssSelector("#wod-word");
	private static final By FOCUS_IPA = By.cssSelector("#wod-ipa");
	private static final By FOCUS_MEANING = By.cssSelector("#wod-definition");
	private static final By FOCUS_EXAMPLE = By.cssSelector("#wod-example");
	private static final By FOCUS_SPEAK = By.cssSelector("button[onclick='speakCurrentWord()']");

	private String inspectFocusVocabulary() {
		WebElement card;
		try {
			card = wait.until(d -> {
				List<WebElement> visible = new ArrayList<>();
				for (WebElement candidate : d.findElements(FOCUS_CARD)) {
					try {
						if (candidate.isDisplayed())
							visible.add(candidate);
					} catch (StaleElementReferenceException ignored) {
					}
				}
				if (visible.size() > 1) {
					throw new AssertionError("[TC_DASH_007] Có " + visible.size()
							+ " card cùng tiêu đề; locator chưa đủ duy nhất: " + FOCUS_CARD);
				}
				return visible.size() == 1 ? visible.get(0) : null;
			});
		} catch (TimeoutException e) {
			throw new AssertionError("[TC_DASH_007] Không tìm thấy card hiển thị với tiêu đề "
					+ "'TỪ VỰNG TIÊU ĐIỂM'; locator=" + FOCUS_CARD, e);
		}

		highlightFocusCard(card);
		String word = readRequiredFocusField(card, FOCUS_WORD, "Từ tiếng Anh");
		String ipa = readRequiredFocusField(card, FOCUS_IPA, "Phiên âm IPA");
		String meaning = readRequiredFocusField(card, FOCUS_MEANING, "Nghĩa tiếng Việt");
		// A speaker button is present in the supplied DOM; validate presence and
		// usability,
		// not successful audio playback (which requires separate instrumentation).
		try {
			wait.until(d -> {
				for (WebElement item : card.findElements(FOCUS_SPEAK)) {
					if (item.isDisplayed() && item.isEnabled())
						return item;
				}
				return null;
			});
		} catch (TimeoutException e) {
			throw new AssertionError("[TC_DASH_007] Thiếu nút phát âm hiển thị/khả dụng; selector=" + FOCUS_SPEAK, e);
		}

		// The supplied HTML uses class='hidden' on the example. This is an
		// existing UI state, not a reason to silently PASS a visibility requirement.
		String exampleState = inspectFocusExample(card);
		String actual = "VALIDATED: card=" + FOCUS_CARD + "; word=" + word + "; IPA=" + ipa + "; meaning=" + meaning
				+ "; speakButton=visible/enabled; " + exampleState;
		System.out.println("[TC_DASH_007] " + actual);
		return actual;
	}

	private String readRequiredFocusField(WebElement card, By selector, String name) {
		try {
			return wait.until(d -> {
				for (WebElement child : card.findElements(selector)) {
					if (!child.isDisplayed())
						continue;
					String value = child.getText().trim();
					if (!value.isEmpty())
						return value;
				}
				return null;
			});
		} catch (TimeoutException e) {
			throw new AssertionError(
					"[TC_DASH_007] Thiếu/trống/ẩn trường bắt buộc '" + name + "' trong card; selector=" + selector, e);
		}
	}

	private String inspectFocusExample(WebElement card) {
		List<WebElement> examples = card.findElements(FOCUS_EXAMPLE);
		if (examples.isEmpty())
			return "example=NOT_FOUND (optional; selector=" + FOCUS_EXAMPLE + ")";
		if (examples.size() != 1) {
			throw new AssertionError(
					"[TC_DASH_007] Có " + examples.size() + " trường ví dụ; selector không duy nhất: " + FOCUS_EXAMPLE);
		}
		WebElement example = examples.get(0);
		boolean visible = example.isDisplayed();
		// textContent captures a DOM value even if the element has display:none.
		String raw = String
				.valueOf(((JavascriptExecutor) driver).executeScript("return arguments[0].textContent || '';", example))
				.trim();
		String value = raw.replaceAll("\\s+", " ");
		return "example=" + (visible ? "VISIBLE" : "HIDDEN") + "; exampleText=" + truncate(value, 180)
				+ "; exampleNote=" + (visible ? "Displayed in UI" : "Not displayed; not asserted as visible");
	}

	private void highlightFocusCard(WebElement card) {
		js.executeScript("const e=arguments[0];e.scrollIntoView({block:'center'});"
				+ "e.style.setProperty('outline','3px solid red','important');"
				+ "e.style.setProperty('outline-offset','2px','important');"
				+ "const old=document.getElementById('__seleniumTestOutline');if(old)old.remove();"
				+ "const b=document.createElement('div');b.id='__seleniumTestOutline';" + "b.textContent='ĐANG TEST';"
				+ "Object.assign(b.style,{position:'fixed',zIndex:'2147483647',background:'#c90000',"
				+ "color:'#fff',padding:'7px 12px',borderRadius:'5px',font:'bold 14px Arial'});"
				+ "const r=e.getBoundingClientRect();b.style.top=Math.max(4,r.top-34)+'px';"
				+ "b.style.left=Math.max(4,r.left)+'px';document.body.appendChild(b);"
				+ "window.__seleniumFocusCard=e;window.__seleniumTestOutline=b;", card);
	}

	private void clearFocusHighlight() {
		if (js == null)
			return;
		try {
			js.executeScript(
					"if(window.__seleniumFocusCard){window.__seleniumFocusCard.style.removeProperty('outline');"
							+ "window.__seleniumFocusCard.style.removeProperty('outline-offset');"
							+ "window.__seleniumFocusCard=null;}"
							+ "if(window.__seleniumTestOutline){window.__seleniumTestOutline.remove();"
							+ "window.__seleniumTestOutline=null;}");
		} catch (Exception ignored) {
		}
	}

	@Test(description = "TC_DASH_008 - Quick actions", dependsOnMethods = "TC_DASH_001_dashboardUrl")

	public void TC_DASH_008_quickActions() {

		check("TC_DASH_008", "Lối tắt nhanh", "Có link Học tập, Trò chơi, Thư viện và Thẻ ghi nhớ",

				() -> {

					String[] labels = { "Học tập", "Trò chơi", "Thư viện", "Thẻ ghi nhớ" };

					List<String> found = new ArrayList<>();

					List<String> missing = new ArrayList<>();

					for (String label : labels) {

						boolean exists = false;

						for (WebElement element : driver.findElements(By.xpath(

								"//*[self::a or self::button]//*[normalize-space(text())=" + xpathLiteral(label) + "]"

										+ " | //*[self::a or self::button][normalize-space(.)=" + xpathLiteral(label)
										+ "]"))) {

							try {
								if (element.isDisplayed()) {
									exists = true;
									break;
								}
							}

				catch (StaleElementReferenceException ignored) {
							}

						}

						(exists ? found : missing).add(label);

					}

					return "found=" + found + "; missing=" + missing;

				}, value -> value.contains("missing=[]"));

	}

	// Expected describes EACH verification requirement; Actual reports observed
	// values,
	// never substitutes predetermined expected values for DOM observations.
	private String detailedExpected(String id, String original) {
		switch (id) {
		case "TC_DASH_001":
			return "- Bước 1: Đăng nhập thành công.\n- Bước 2: Điều hướng tới Dashboard."
					+ "\n- URL chứa 'dashboard.php'.";
		case "TC_DASH_002":
			return "- Bước 1: Mở Dashboard.\n- Bước 2: Hiển thị lời chào người dùng."
					+ "\n- Bước 3: Hiển thị các chỉ số thống kê." + "\n- Lời chào không rỗng; có khu vực thống kê.";
		case "TC_DASH_003":
			return "- Bước 1: Hiển thị khu vực Tiến trình Scholar."
					+ "\n- Bước 2: Hiển thị cấp độ Scholar và thông tin điểm."
					+ "\n- Bước 3: Hiển thị thanh tiến trình có giá trị độ rộng."
					+ "\n- Nếu đặc tả quy định dạng 1/100: đối chiếu cấp độ đọc từ DOM (chưa xác minh đặc tả)."
					+ "\n- Thanh tiến trình nhìn thấy được; không yêu cầu cố định 5%.";
		case "TC_DASH_004":
			return "- Bước 1: Hiển thị canvas #topicChart." + "\n- Bước 2: Chart.js khởi tạo biểu đồ."
					+ "\n- Bước 3: Danh sách nhãn chủ đề không rỗng; có dataset.";
		case "TC_DASH_005":
			return "- Bước 1: Hiển thị ba tab Chuỗi, Điểm, Từ vựng." + "\n- Bước 2: Lần lượt chọn từng tab."
					+ "\n- Bước 3: Mỗi tab hiển thị ít nhất 5 dòng xếp hạng." + "\n- Bước 4: Khôi phục tab Chuỗi.";
		case "TC_DASH_006":
			return "- Bước 1: Hiển thị canvas #weeklyChart." + "\n- Bước 2: Chart.js khởi tạo biểu đồ tuần."
					+ "\n- Bước 3: Hiển thị đủ 7 nhãn ngày T2, T3, T4, T5, T6, T7, CN.";
		case "TC_DASH_007":
			return "- Bước 1: Hiển thị card 'TỪ VỰNG TIÊU ĐIỂM'." + "\n- Bước 2: Từ tiếng Anh (#wod-word) không rỗng."
					+ "\n- Bước 3: IPA (#wod-ipa) không rỗng." + "\n- Bước 4: Nghĩa (#wod-definition) không rỗng."
					+ "\n- Bước 5: Nút phát âm hiển thị và khả dụng."
					+ "\n- Bước 6: Ghi nhận ví dụ (#wod-example) là hiển thị hoặc ẩn."
					+ "\n- Ví dụ ẩn KHÔNG được báo là hiển thị.";
		case "TC_DASH_008":
			return "- Bước 1: Mở Dashboard." + "\n- Bước 2: Hiển thị lối tắt Học tập."
					+ "\n- Bước 3: Hiển thị lối tắt Trò chơi." + "\n- Bước 4: Hiển thị lối tắt Thư viện."
					+ "\n- Bước 5: Hiển thị lối tắt Thẻ ghi nhớ.";
		default:
			return original;
		}
	}

	private String detailedActual(String id, String raw) {
		String v = raw == null ? "(không có dữ liệu)" : raw;
		switch (id) {
		case "TC_DASH_001":
			return "- URL thực tế: " + v + "\n- Chứa dashboard.php: " + v.contains(DASHBOARD_URL_PART);
		case "TC_DASH_002":
			return "- Lời chào đọc từ giao diện: " + extractBetween(v, "Greeting=", "; có từ khóa thống kê=")
					+ "\n- Có chỉ số/từ khóa thống kê: " + v.endsWith("true") + "\n- Kết quả thô: " + v;
		case "TC_DASH_003":
			return "- Có nhãn CẤP/Scholar: " + v.contains("Có cấp/Scholar=true") + "\n- Có thông tin điểm: "
					+ v.contains("có điểm=true") + "\n- Thanh tiến trình: " + extractFrom(v, "progress elements=")
					+ "\n- Nội dung tiến trình trên DOM: " + scholarDisplayedDetails();
		case "TC_DASH_004":
			return "- Canvas #topicChart hiển thị: " + v.contains("canvasDisplayed=true") + "\n- Chart.js tìm thấy: "
					+ v.contains("chartFound=true") + "\n- Nhãn và dữ liệu thực tế: " + extractFrom(v, "chart=");
		case "TC_DASH_005":
			return "- Số tab tìm thấy: " + extractBetween(v, "tabsFound=", ";") + "\n- Số tab có >=5 dòng: "
					+ extractBetween(v, "tabsWithAtLeast5Rows=", ";") + "\n- Chi tiết từng tab: "
					+ extractFrom(v, "details=");
		case "TC_DASH_006":
			return "- Canvas #weeklyChart hiển thị: " + v.contains("canvasDisplayed=true") + "\n- Chart.js tìm thấy: "
					+ v.contains("chartFound=true") + "\n- Số nhãn ngày: " + extractBetween(v, "labelCount=", ";")
					+ "\n- Nhãn và dữ liệu thực tế: " + extractFrom(v, "chart=");
		case "TC_DASH_007":
			return "- Card và các trường đã xác minh: " + v.replace("; ", "\n- ");
		case "TC_DASH_008":
			return "- Các lối tắt tìm thấy: " + extractBetween(v, "found=", ";") + "\n- Các lối tắt còn thiếu: "
					+ extractFrom(v, "missing=");
		default:
			return v;
		}
	}

	private static String extractBetween(String text, String start, String end) {
		int a = text.indexOf(start);
		if (a < 0)
			return "Không xác định";
		a += start.length();
		int b = text.indexOf(end, a);
		return (b < 0 ? text.substring(a) : text.substring(a, b)).trim();
	}

	private static String extractFrom(String text, String start) {
		int a = text.indexOf(start);
		return a < 0 ? "Không xác định" : text.substring(a + start.length()).trim();
	}

	// Reads real visible DOM text near Scholar instead of assuming "1/100".
	private String scholarDisplayedDetails() {
		try {
			Object out = js.executeScript("const all=[...document.querySelectorAll('body *')];"
					+ "const show=e=>{const s=getComputedStyle(e),r=e.getBoundingClientRect();"
					+ "return s.display!=='none'&&s.visibility!=='hidden'&&r.width>0&&r.height>0};"
					+ "const head=all.find(e=>e.children.length===0&&show(e)&&/Scholar|Tiến trình Scholar/i.test(e.textContent||''));"
					+ "if(!head)return 'Không tìm thấy nhãn Scholar trong DOM hiển thị';"
					+ "let node=head;for(let i=0;i<3&&node.parentElement;i++)node=node.parentElement;"
					+ "return (node.innerText||'').replace(/\\s+/g,' ').trim().slice(0,350);");
			return String.valueOf(out);
		} catch (Exception e) {
			return "Không đọc được DOM Scholar: " + e.getClass().getSimpleName();
		}
	}

	private void check(String id, String name, String expected, Supplier<String> actualSupplier,

			Predicate<String> condition) {

		activeTest = id + " - " + name;

		showBanner("ĐANG TEST: " + name);

		TestResult result = new TestResult(id, name, detailedExpected(id, expected));

		results.add(result);

		try {

			String rawActual = actualSupplier.get();
			result.actual = detailedActual(id, rawActual);

			if (!"TC_DASH_007".equals(id))
				highlightBestMatchingElement(name);

			if (condition.test(rawActual)) {

				result.status = "PASS";

				result.details = "Điều kiện kiểm thử thỏa mãn.";

			} else {

				result.status = "FAIL";

				result.details = "Actual không đáp ứng Expected.";

				result.screenshot = takeScreenshot(id + "_FAIL");

				Assert.fail("[" + id + "] FAIL - " + name + "\nExpected: " + expected

						+ "\nActual: " + result.actual);

			}

		} catch (AssertionError e) {

			if (result.status == null) {

				result.status = "FAIL";

				result.actual = result.actual == null ? "AssertionError: " + e.getMessage() : result.actual;

				result.details = e.getMessage();

				if (result.screenshot.isBlank())
					result.screenshot = takeScreenshot(id + "_FAIL");

			}

			printResultOnce(result);

			throw e;

		} catch (Exception e) {

			result.status = "FAIL";

			result.actual = "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage();

			result.details = e.toString();

			result.screenshot = takeScreenshot(id + "_ERROR");

			printResultOnce(result);

			Assert.fail("[" + id + "] Lỗi khi chạy test: " + e.getMessage(), e);

		} finally {
			if ("TC_DASH_007".equals(id))
				clearFocusHighlight();
		}

		printResultOnce(result);

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

			File dir = new File("DashboardUser_Screenshots");

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

		System.out.println("Excel: " + new File("DashboardUser_TestResults.xlsx").getAbsolutePath());

		System.out.println("Screenshots: " + new File("DashboardUser_Screenshots").getAbsolutePath());

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

			try (FileOutputStream out = new FileOutputStream("DashboardUser_TestResults.xlsx")) {
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
