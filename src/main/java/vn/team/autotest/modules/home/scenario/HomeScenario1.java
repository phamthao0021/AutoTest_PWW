package vn.team.autotest.modules.home.scenario;

import java.util.List;

import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.annotations.Test;


import vn.team.autotest.modules.home.page.HomePage;

public final class HomeScenario1 extends HomeBaseScenario {

	private static final int WIDTH = 1366;

	private static final int HEIGHT = 768;

	private static final List<String> EXPECTED_MENU = List.of("Trang chủ", "Giới thiệu", "Phương pháp học", "Bài viết",
			"Học thử ngay", "Đăng nhập", "Đăng ký ngay");

	// =========================================================
	// KỊCH BẢN 1
	//
	// PWW_AUTO_01
	//
	// HOM-001
	// Hiển thị khu vực giới thiệu chính
	//
	// HOM-002
	// Kiểm tra Header/Menu
	// =========================================================

	@Test(description = "KỊCH BẢN 1 - Kiểm tra giao diện Trang chủ - HOM-001 + HOM-002",

			groups = { "home", "thao", "kichban1", "critical" })
	public void KICH_BAN_01_verifyHomePageUI() {

		printLine();

		System.out.println("KỊCH BẢN 1");

		System.out.println("KIỂM TRA GIAO DIỆN TRANG CHỦ");

		System.out.println();

		System.out.println("HOM-001 - Hiển thị khu vực giới thiệu chính");

		System.out.println("HOM-002 - Kiểm tra Header/Menu");

		printLine();

		// =====================================================
		// BƯỚC 1
		// KHỞI TẠO CHROME + VIEWPORT
		// =====================================================

		driver().manage().window().setSize(new Dimension(WIDTH, HEIGHT));

		System.out.println();

		System.out.println("BƯỚC 1: Mở trình duyệt Chrome");

		System.out.println("Expected: Chrome được mở với viewport 1366x768");

		System.out.println("Actual  : Chrome đã được BaseScenario khởi tạo");

		// =====================================================
		// BƯỚC 2
		// TRUY CẬP PWW
		// =====================================================

		open("/");

		String actualUrl = driver().getCurrentUrl();

		System.out.println();

		System.out.println("BƯỚC 2: Truy cập Trang chủ PWW");

		System.out.println("Expected: https://learnenglish.plt.pro.vn/");

		System.out.println("Actual  : " + actualUrl);

		Assert.assertTrue(actualUrl.contains("learnenglish.plt.pro.vn"), "Không truy cập đúng website PWW");

		HomePage homePage = new HomePage(driver());

		// =====================================================
		// HOM-001
		// =====================================================

		System.out.println();

		printSubLine();

		System.out.println("HOM-001");

		System.out.println("HIỂN THỊ KHU VỰC GIỚI THIỆU CHÍNH");

		printSubLine();

		// =====================================================
		// BƯỚC 3
		// HERO TITLE DISPLAY
		// =====================================================

		homePage.waitUntilHeroReady();

		boolean heroDisplayed = homePage.isHeroTitleDisplayed();

		System.out.println();

		System.out.println("BƯỚC 3: Kiểm tra Hero Title");

		System.out.println("Expected: Hero Title hiển thị = true | Actual  : Hero Title hiển thị = " + heroDisplayed);

		Assert.assertTrue(heroDisplayed, "Hero Title không hiển thị");

		// =====================================================
		// BƯỚC 4
		// HERO TITLE TEXT
		// =====================================================

		String actualHeroTitle = homePage.getHeroTitleText();

		System.out.println();

		System.out.println("BƯỚC 4: Kiểm tra nội dung Hero Title");

		System.out.println("Expected: Có WordWise hoặc Chinh Phục Từ Vựng Tiếng Anh | Actual  : " + actualHeroTitle);

		Assert.assertTrue(
				actualHeroTitle.contains("WordWise") || actualHeroTitle.contains("Chinh Phục Từ Vựng Tiếng Anh"),

				"Hero Title không đúng");

		// =====================================================
		// BƯỚC 5
		// HERO DESCRIPTION
		// =====================================================

		boolean descriptionDisplayed = homePage.isHeroDescriptionDisplayed();

		String actualDescription = homePage.getHeroDescriptionText();

		System.out.println();

		System.out.println("BƯỚC 5: Kiểm tra Hero Description");

		System.out.println(
				"Expected: Description hiển thị và không rỗng | Actual  : Displayed = " + descriptionDisplayed);

		System.out.println("Actual Text: " + actualDescription);

		Assert.assertTrue(descriptionDisplayed, "Hero Description không hiển thị");

		Assert.assertFalse(actualDescription.isBlank(), "Hero Description đang rỗng");

		// =====================================================
		// BƯỚC 6
		// CTA
		// =====================================================

		String actualCTA = homePage.getHeroPrimaryCTAText();

		boolean ctaDisplayed = homePage.isHeroPrimaryCTADisplayed();

		boolean ctaEnabled = homePage.isHeroPrimaryCTAEnabled();

		System.out.println();

		System.out.println("BƯỚC 6: Kiểm tra CTA chính");

		System.out.println("Expected: Bắt đầu học miễn phí");

		System.out.println("Actual  : " + actualCTA);

		System.out.println("Expected: Displayed = true");

		System.out.println("Actual  : Displayed = " + ctaDisplayed);

		System.out.println("Expected: Enabled = true");

		System.out.println("Actual  : Enabled = " + ctaEnabled);

		Assert.assertEquals(actualCTA, "Bắt đầu học miễn phí", "Sai text CTA");

		Assert.assertTrue(ctaDisplayed, "CTA không hiển thị");

		Assert.assertTrue(ctaEnabled, "CTA không enable");

		System.out.println();

		System.out.println("HOM-001 RESULT: PASS");

		// =====================================================
		// HOM-002
		// =====================================================

		System.out.println();

		printSubLine();

		System.out.println("HOM-002");

		System.out.println("KIỂM TRA HEADER/MENU");

		printSubLine();

		// =====================================================
		// BƯỚC 7
		// HEADER DISPLAY
		// =====================================================

		homePage.waitUntilHeaderReady();

		boolean headerDisplayed = homePage.isHeaderDisplayed();

		System.out.println();

		System.out.println("BƯỚC 7: Kiểm tra Header");

		System.out.println("Expected: Header hiển thị = true");

		System.out.println("Actual  : Header hiển thị = " + headerDisplayed);

		Assert.assertTrue(headerDisplayed, "Header không hiển thị");

		// =====================================================
		// BƯỚC 8
		// ACTUAL MENU
		// =====================================================

		List<String> actualMenu = homePage.getVisibleHeaderLabels();

		System.out.println();

		System.out.println("BƯỚC 8: Lấy danh sách menu");

		System.out.println("Expected: " + EXPECTED_MENU);

		System.out.println("Actual  : " + actualMenu);

		// =====================================================
		// BƯỚC 9
		// COUNT
		// =====================================================

		System.out.println();

		System.out.println("BƯỚC 9: So sánh số lượng menu");

		System.out.println("Expected: " + EXPECTED_MENU.size());

		System.out.println("Actual  : " + actualMenu.size());

		Assert.assertEquals(actualMenu.size(), EXPECTED_MENU.size(), "Sai số lượng menu");

		// =====================================================
		// BƯỚC 10
		// TEXT + ORDER
		// =====================================================

		System.out.println();

		System.out.println("BƯỚC 10: So sánh tên và thứ tự menu");

		System.out.println("Expected: " + EXPECTED_MENU);

		System.out.println("Actual  : " + actualMenu);

		Assert.assertEquals(actualMenu, EXPECTED_MENU, "Tên hoặc thứ tự menu không đúng");

		// =====================================================
		// BƯỚC 11
		// DISPLAYED + ENABLED
		// =====================================================

		System.out.println();

		System.out.println("BƯỚC 11: Kiểm tra Displayed / Enabled");

		for (String menu : EXPECTED_MENU) {

			boolean displayed = homePage.isHeaderItemDisplayed(menu);

			boolean enabled = homePage.isHeaderItemEnabled(menu);

			System.out.println();

			System.out.println("Menu     : " + menu);

			System.out.println("Expected : Displayed = true | Enabled = true");

			System.out.println("Actual   : Displayed = " + displayed + " | Enabled = " + enabled);

			Assert.assertTrue(displayed, "Menu không hiển thị: " + menu);

			Assert.assertTrue(enabled, "Menu không enable: " + menu);
		}

		// =====================================================
		// BƯỚC 12
		// KIỂM TRA XPATH MOBILE LOGIN / REGISTER TRONG DOM
		// =====================================================

		boolean mobileContainerPresent = homePage.isMobileAuthContainerPresent();

		boolean mobileLoginPresent = homePage.isMobileLoginPresent();

		boolean mobileRegisterPresent = homePage.isMobileRegisterPresent();

		System.out.println();

		System.out.println("BƯỚC 12: Kiểm tra mobile Login/Register trong DOM");

		System.out.println("XPath Container:");

		System.out.println("//*[@id='mobileMenuContainer']/div/div");

		System.out.println("Expected: Container tồn tại = true");

		System.out.println("Actual  : Container tồn tại = " + mobileContainerPresent);

		Assert.assertTrue(mobileContainerPresent, "Không tìm thấy mobile auth container");

		System.out.println();

		System.out.println("XPath Login:");

		System.out.println("//*[@id='mobileMenuContainer']/div/div/a[1]");

		System.out.println("Expected: Đăng nhập tồn tại = true");

		System.out.println("Actual  : " + mobileLoginPresent);

		Assert.assertTrue(mobileLoginPresent, "Không tìm thấy Đăng nhập mobile");

		System.out.println();

		System.out.println("XPath Register:");

		System.out.println("//*[@id='mobileMenuContainer']/div/div/a[2]");

		System.out.println("Expected: Đăng ký tồn tại = true");

		System.out.println("Actual  : " + mobileRegisterPresent);

		Assert.assertTrue(mobileRegisterPresent, "Không tìm thấy Đăng ký mobile");

		// =====================================================
		// BƯỚC 13
		// SO SÁNH TEXT MOBILE
		// =====================================================

		String actualMobileLogin = homePage.getMobileLoginText();

		String actualMobileRegister = homePage.getMobileRegisterText();

		System.out.println();

		System.out.println("BƯỚC 13: So sánh text Login/Register mobile");

		System.out.println("Expected Login   : Đăng nhập");

		System.out.println("Actual Login     : " + actualMobileLogin);

		System.out.println("Expected Register: Đăng ký");

		System.out.println("Actual Register  : " + actualMobileRegister);

		Assert.assertEquals(actualMobileLogin, "Đăng nhập", "Sai text Đăng nhập mobile");

		Assert.assertEquals(actualMobileRegister, "Đăng ký", "Sai text Đăng ký mobile");

		// =====================================================
		// BƯỚC 14
		// SO SÁNH HREF MOBILE
		// =====================================================

		String loginHref = homePage.getMobileLoginHref();

		String registerHref = homePage.getMobileRegisterHref();

		System.out.println();

		System.out.println("BƯỚC 14: Kiểm tra href mobile");

		System.out.println("Expected Login href   : login.php");

		System.out.println("Actual Login href     : " + loginHref);

		System.out.println("Expected Register href: register.php");

		System.out.println("Actual Register href  : " + registerHref);

		Assert.assertTrue(loginHref.endsWith("login.php"), "Href Đăng nhập mobile không đúng: " + loginHref);

		Assert.assertTrue(registerHref.endsWith("register.php"), "Href Đăng ký mobile không đúng: " + registerHref);

		System.out.println();

		System.out.println("HOM-002 RESULT: PASS");

		// =====================================================
		// KẾT QUẢ CUỐI
		// =====================================================

		System.out.println();

		printLine();

		System.out.println("KỊCH BẢN 1 RESULT: PASS");

		System.out.println("HOM-001: PASS");

		System.out.println("HOM-002: PASS");

		printLine();
	}

	// =========================================================
	// FORMAT CONSOLE
	// =========================================================

	private void printLine() {

		System.out.println("============================================================");
	}

	private void printSubLine() {

		System.out.println("------------------------------------------------------------");
	}
}