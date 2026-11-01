package vn.team.autotest.modules.home.scenario;

import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import vn.team.autotest.modules.home.page.HomePage;

public class HomeScenario3 extends HomeBaseScenario {

	private static final int WIDTH = 1366;
	private static final int HEIGHT = 768;

	private static final String HOME_URL = "https://learnenglish.plt.pro.vn/";

	// =========================================================
	// DATA PROVIDER
	// POSITIVE / INTEGRATION
	// =========================================================

	@DataProvider(name = "ctaNavigationData")
	public Object[][] ctaNavigationData() {

		return new Object[][] {

				{ "KB-03-001", "Đăng nhập", "login.php", true },

				{ "KB-03-002", "Đăng ký ngay", "register.php", true },

				{ "KB-03-003", "Bắt đầu học miễn phí", "", false } };
	}

	// =========================================================
	// PWW_AUTO_03
	//
	// KB-03-001
	// HOME -> LOGIN
	//
	// KB-03-002
	// HOME -> REGISTER
	//
	// KB-03-003
	// HERO CTA
	//
	// DATA-DRIVEN + POSITIVE + INTEGRATION
	// =========================================================

	@Test(dataProvider = "ctaNavigationData",

			description = "PWW_AUTO_03 - CTA Navigation Integration",

			groups = { "home", "thao", "functional", "positive", "integration", "data-driven", "high" })
	public void PWW_AUTO_03_verifyCTAIntegration(

			String caseId,

			String expectedText,

			String expectedPath,

			boolean verifyExactPath

	) {

		printScenarioHeader(caseId, expectedText);

		// =====================================================
		// BƯỚC 1
		// VIEWPORT
		// =====================================================

		driver().manage().window().setSize(new Dimension(WIDTH, HEIGHT));

		Dimension actualSize = driver().manage().window().getSize();

		boolean viewportPass = actualSize.getWidth() > 0 && actualSize.getHeight() > 0;

		checkpoint(caseId, "Browser", "Viewport hợp lệ", "width > 0, height > 0",
				actualSize.getWidth() + "x" + actualSize.getHeight(), viewportPass);

		Assert.assertTrue(viewportPass, "Viewport không hợp lệ");

		// =====================================================
		// BƯỚC 2
		// MỞ TRANG CHỦ
		// =====================================================

		open("/");

		HomePage homePage = new HomePage(driver());

		homePage.waitUntilHeaderReady();

		String startUrl = driver().getCurrentUrl();

		boolean startHome = isHomeUrl(startUrl);

		checkpoint(caseId, "Trang chủ", "URL bắt đầu", HOME_URL, startUrl, startHome);

		Assert.assertTrue(startHome, "Không bắt đầu tại Trang chủ PWW");

		// =====================================================
		// BƯỚC 3
		// CTA TỒN TẠI
		// =====================================================

		boolean actualPresent = homePage.isActionPresent(expectedText);

		checkpoint(caseId, "Trang chủ", "CTA tồn tại", true, actualPresent, actualPresent);

		Assert.assertTrue(actualPresent, "Không tìm thấy CTA: " + expectedText);

		// =====================================================
		// BƯỚC 4
		// CTA DISPLAYED
		// =====================================================

		boolean actualDisplayed = homePage.isActionDisplayed(expectedText);

		checkpoint(caseId, "Trang chủ", "CTA hiển thị", true, actualDisplayed, actualDisplayed);

		Assert.assertTrue(actualDisplayed, "CTA không hiển thị: " + expectedText);

		// =====================================================
		// BƯỚC 5
		// CTA ENABLED
		// =====================================================

		boolean actualEnabled = homePage.isActionEnabled(expectedText);

		checkpoint(caseId, "Trang chủ", "CTA enable", true, actualEnabled, actualEnabled);

		Assert.assertTrue(actualEnabled, "CTA không enable: " + expectedText);

		// =====================================================
		// BƯỚC 6
		// TEXT
		// =====================================================

		String actualText = homePage.getActionText(expectedText);

		boolean textPass = expectedText.equals(actualText);

		checkpoint(caseId, "Trang chủ", "Text CTA", expectedText, actualText, textPass);

		Assert.assertEquals(actualText, expectedText, "Text CTA không đúng");

		// =====================================================
		// BƯỚC 7
		// TAG NAME
		// =====================================================

		String tagName = homePage.getActionTagName(expectedText);

		boolean tagValid = "a".equalsIgnoreCase(tagName) || "button".equalsIgnoreCase(tagName);

		checkpoint(caseId, "Trang chủ", "Loại phần tử", "a hoặc button", tagName, tagValid);

		Assert.assertTrue(tagValid, "CTA không phải a/button");

		// =====================================================
		// BƯỚC 8
		// HREF
		// =====================================================

		String actualHref = homePage.getActionHref(expectedText);

		boolean hrefRequired = "a".equalsIgnoreCase(tagName);

		if (hrefRequired) {

			boolean hrefNotBlank = !actualHref.isBlank();

			checkpoint(caseId, "Trang chủ", "Href không rỗng", true, hrefNotBlank, hrefNotBlank);

			Assert.assertTrue(hrefNotBlank, "href CTA đang rỗng");

			// =================================================
			// NEGATIVE
			// HREF KHÔNG ĐƯỢC #
			// =================================================

			boolean hashOnly = "#".equals(actualHref);

			checkpoint(caseId, "Trang chủ", "Href chỉ là #", false, hashOnly, !hashOnly);

			Assert.assertFalse(hashOnly, "CTA chỉ trỏ tới #");

			// =================================================
			// NEGATIVE
			// JAVASCRIPT
			// =================================================

			boolean javascriptHref = actualHref.toLowerCase().startsWith("javascript:");

			checkpoint(caseId, "Trang chủ", "Href javascript", false, javascriptHref, !javascriptHref);

			Assert.assertFalse(javascriptHref, "CTA sử dụng javascript:");
		}

		// =====================================================
		// BƯỚC 9
		// EXPECTED PATH
		// =====================================================

		if (verifyExactPath && !expectedPath.isBlank()) {

			boolean hrefMatch = actualHref.contains(expectedPath);

			checkpoint(caseId, "Trang chủ", "Href đích", expectedPath, actualHref, hrefMatch);

			Assert.assertTrue(hrefMatch,
					"Href CTA không đúng. Expected path: " + expectedPath + " | Actual: " + actualHref);
		}

		// =====================================================
		// BƯỚC 10
		// CLICK
		// =====================================================

		String beforeClickUrl = driver().getCurrentUrl();

		homePage.clickAction(expectedText);

		homePage.waitForUrlChange(beforeClickUrl);

		String actualUrl = driver().getCurrentUrl();

		// =====================================================
		// BƯỚC 11
		// URL SAU CLICK
		// =====================================================

		if (verifyExactPath && !expectedPath.isBlank()) {

			boolean urlPass = actualUrl.contains(expectedPath);

			checkpoint(caseId, "Trang đích", "URL sau click", expectedPath, actualUrl, urlPass);

			Assert.assertTrue(urlPass, "URL sau click không đúng");

		} else {

			boolean urlValid = actualUrl != null && !actualUrl.isBlank();

			checkpoint(caseId, "Trang đích", "URL hợp lệ", true, urlValid, urlValid);

			Assert.assertTrue(urlValid, "URL sau click bị rỗng");
		}

		// =====================================================
		// BƯỚC 12
		// BODY DISPLAYED
		// =====================================================

		boolean bodyDisplayed = homePage.isBodyDisplayed();

		checkpoint(caseId, "Trang đích", "Body hiển thị", true, bodyDisplayed, bodyDisplayed);

		Assert.assertTrue(bodyDisplayed, "Trang đích không hiển thị body");

		// =====================================================
		// BƯỚC 13
		// BODY KHÔNG RỖNG
		// =====================================================

		String bodyText = homePage.getBodyText();

		boolean bodyNotBlank = !bodyText.isBlank();

		checkpoint(caseId, "Trang đích", "Trang không trắng", true, bodyNotBlank, bodyNotBlank);

		Assert.assertTrue(bodyNotBlank, "Trang đích bị trắng");

		// =====================================================
		// BƯỚC 14
		// PAGE TITLE
		// =====================================================

		String actualTitle = homePage.getPageTitle();

		boolean titleNotBlank = !actualTitle.isBlank();

		checkpoint(caseId, "Trang đích", "Title không rỗng", true, titleNotBlank, titleNotBlank);

		Assert.assertTrue(titleNotBlank, "Title trang đích bị rỗng");

		// =====================================================
		// BƯỚC 15
		// NEGATIVE ERROR PAGE
		// =====================================================

		boolean errorPage = homePage.isErrorPage();

		checkpoint(caseId, "Trang đích", "Không 404/500", false, errorPage, !errorPage);

		Assert.assertFalse(errorPage, "CTA điều hướng tới trang lỗi");

		System.out.println();

		System.out.println(caseId + " RESULT: PASS");
	}

	// =========================================================
	// KB-03-004
	//
	// BUSINESS FLOW
	//
	// HOME
	// ->
	// LOGIN
	// ->
	// BACK HOME
	// ->
	// REGISTER
	// =========================================================

	@Test(description = "KB-03-004 - Home Login Register Flow",

			groups = { "home", "thao", "functional", "positive", "integration", "flow", "high" })
	public void PWW_AUTO_03_verifyCTAFlow() {

		printScenarioHeader("KB-03-004", "HOME -> LOGIN -> HOME -> REGISTER");

		// =====================================================
		// STEP 1
		// HOME
		// =====================================================

		open("/");

		HomePage homePage = new HomePage(driver());

		homePage.waitUntilHeaderReady();

		String homeUrl = driver().getCurrentUrl();

		boolean homePass = isHomeUrl(homeUrl);

		checkpoint("KB-03-004", "Trang chủ", "Bắt đầu tại Home", HOME_URL, homeUrl, homePass);

		Assert.assertTrue(homePass);

		// =====================================================
		// STEP 2
		// HOME -> LOGIN
		// =====================================================

		homePage.clickAction("Đăng nhập");

		String loginUrl = driver().getCurrentUrl();

		boolean loginPass = loginUrl.contains("login.php");

		checkpoint("KB-03-004", "Integration", "Home -> Login", "login.php", loginUrl, loginPass);

		Assert.assertTrue(loginPass, "Không tới Login");

		Assert.assertFalse(homePage.isErrorPage(), "Login page bị lỗi");

		// =====================================================
		// STEP 3
		// LOGIN -> HOME
		// =====================================================

		driver().navigate().back();

		homePage.waitUntilHeaderReady();

		String backHomeUrl = driver().getCurrentUrl();

		boolean backHomePass = isHomeUrl(backHomeUrl);

		checkpoint("KB-03-004", "Integration", "Login -> Home", HOME_URL, backHomeUrl, backHomePass);

		Assert.assertTrue(backHomePass, "Back không về Trang chủ");

		// =====================================================
		// STEP 4
		// HOME -> REGISTER
		// =====================================================

		homePage.clickAction("Đăng ký ngay");

		String registerUrl = driver().getCurrentUrl();

		boolean registerPass = registerUrl.contains("register.php");

		checkpoint("KB-03-004", "Integration", "Home -> Register", "register.php", registerUrl, registerPass);

		Assert.assertTrue(registerPass, "Không tới Register");

		Assert.assertFalse(homePage.isErrorPage(), "Register page bị lỗi");

		System.out.println();

		System.out.println("KB-03-004 RESULT: PASS");
	}

	// =========================================================
	// KB-03-005
	//
	// NEGATIVE
	//
	// CTA KHÔNG TỒN TẠI
	// =========================================================

	@Test(description = "KB-03-005 - Negative CTA Validation",

			groups = { "home", "thao", "functional", "negative", "high" })
	public void PWW_AUTO_03_verifyNegativeCTA() {

		printScenarioHeader("KB-03-005", "NEGATIVE CTA");

		open("/");

		HomePage homePage = new HomePage(driver());

		homePage.waitUntilHeaderReady();

		String invalidCTA = "CTA KHÔNG TỒN TẠI 123456";

		String beforeUrl = driver().getCurrentUrl();

		// =====================================================
		// CHECK 1
		// KHÔNG TÌM THẤY CTA
		// =====================================================

		boolean actualPresent = homePage.isActionPresent(invalidCTA);

		checkpoint("KB-03-005", "Trang chủ", "CTA giả tồn tại", false, actualPresent, !actualPresent);

		Assert.assertFalse(actualPresent, "Hệ thống tìm thấy CTA không tồn tại");

		// =====================================================
		// CHECK 2
		// URL KHÔNG THAY ĐỔI
		// =====================================================

		String afterUrl = driver().getCurrentUrl();

		boolean urlUnchanged = beforeUrl.equals(afterUrl);

		checkpoint("KB-03-005", "Trang chủ", "URL không thay đổi", beforeUrl, afterUrl, urlUnchanged);

		Assert.assertEquals(afterUrl, beforeUrl, "URL thay đổi dù không có thao tác hợp lệ");

		// =====================================================
		// CHECK 3
		// VẪN Ở HOME
		// =====================================================

		boolean stillHome = isHomeUrl(afterUrl);

		checkpoint("KB-03-005", "Trang chủ", "Vẫn ở Home", true, stillHome, stillHome);

		Assert.assertTrue(stillHome);

		// =====================================================
		// CHECK 4
		// KHÔNG ERROR
		// =====================================================

		boolean errorPage = homePage.isErrorPage();

		checkpoint("KB-03-005", "Trang chủ", "Không 404/500", false, errorPage, !errorPage);

		Assert.assertFalse(errorPage);

		System.out.println();

		System.out.println("KB-03-005 RESULT: PASS");
	}

	// =========================================================
	// HELPER
	// =========================================================

	private boolean isHomeUrl(String url) {

		if (url == null || url.isBlank()) {

			return false;
		}

		String value = url.trim();

		return value.equals(HOME_URL) || value.equals("https://learnenglish.plt.pro.vn")
				|| value.endsWith("/index.php");
	}

	private void checkpoint(

			String id, String screen, String check, Object expected, Object actual, boolean passed

	) {

		System.out.printf("| %-10s | %-18s | %-28s | %-35s | %-38s | %-6s |%n", id, screen, check, shorten(expected),
				shorten(actual), passed ? "PASS" : "FAIL");
	}

	private String shorten(Object value) {

		String text = String.valueOf(value);

		int max = 35;

		if (text.length() <= max) {

			return text;
		}

		return text.substring(0, max - 3) + "...";
	}

	private void printScenarioHeader(

			String id, String scenario

	) {

		System.out.println();

		System.out.println(
				"==========================================================================================================================================================");

		System.out.println("PWW_AUTO_03");

		System.out.println(id + " - " + scenario);

		System.out.println(
				"==========================================================================================================================================================");

		System.out.printf("| %-10s | %-18s | %-28s | %-35s | %-38s | %-6s |%n", "ID", "Màn hình", "Kiểm tra",
				"Expected", "Actual", "Status");

		System.out.println(
				"==========================================================================================================================================================");
	}
}