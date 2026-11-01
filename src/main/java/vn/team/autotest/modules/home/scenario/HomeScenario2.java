package vn.team.autotest.modules.home.scenario;

import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.annotations.Test;
import vn.team.autotest.modules.home.page.HomePage;

public class HomeScenario2 extends HomeBaseScenario {
    private static final String HOME_URL = "https://learnenglish.plt.pro.vn/";

    @Test(description = "PWW_AUTO_02 - Integration Test Logo/Brand -> Trang chủ",
            groups = {"home", "thao", "integration", "branding", "high"})
    public void PWW_AUTO_02_verifyHomeBrandIntegration() {
        driver().manage().window().setSize(new Dimension(1366, 768));
        open("/");
        HomePage home = new HomePage(driver());
        home.waitUntilHeaderReady();

        check("KB-02-001", "Logo hiển thị", true, home.isLogoPltDisplayed(), home.isLogoPltDisplayed());
        String src = home.getLogoSrc();
        check("KB-02-001", "Logo src", "Không rỗng", src, !src.isBlank());

        check("KB-02-002", "Brand hiển thị", true, home.isWordWiseDisplayed(), home.isWordWiseDisplayed());
        String brand = home.getWordWiseText();
        check("KB-02-002", "Brand text", "WordWise", brand, "WordWise".equals(brand));

        check("KB-02-003", "Logo có liên kết", true, home.hasLogoHomeLink(), home.hasLogoHomeLink());
        check("KB-02-003", "Logo link hiển thị", true, home.isLogoHomeLinkDisplayed(), home.isLogoHomeLinkDisplayed());
        check("KB-02-003", "Logo link enable", true, home.isLogoHomeLinkEnabled(), home.isLogoHomeLinkEnabled());
        String href = home.getLogoHomeHref();
        check("KB-02-003", "Logo href", "Không rỗng", href, !href.isBlank());

        home.clickLogoHome();
        home.waitUntilHeaderReady();
        home.waitUntilHeroReady();
        String url = driver().getCurrentUrl();
        check("KB-02-003", "URL sau click logo", HOME_URL, url, isHomeUrl(url));
        check("KB-02-003", "Header sau điều hướng", true, home.isHeaderDisplayed(), home.isHeaderDisplayed());
        check("KB-02-003", "Hero sau điều hướng", true, home.isHeroTitleDisplayed(), home.isHeroTitleDisplayed());
        String hero = home.getHeroTitleText();
        check("KB-02-003", "Hero text", "Không rỗng", hero, !hero.isBlank());
        check("KB-02-003", "CTA sau điều hướng", true, home.isHeroPrimaryCTADisplayed(), home.isHeroPrimaryCTADisplayed());
        String cta = home.getHeroPrimaryCTAText();
        check("KB-02-003", "CTA text", "Bắt đầu học miễn phí", cta, "Bắt đầu học miễn phí".equals(cta));

        boolean complete = home.isLogoComplete();
        long width = home.getLogoNaturalWidth();
        long height = home.getLogoNaturalHeight();
        check("KB-02-004", "Image complete", true, complete, complete);
        check("KB-02-004", "Image naturalWidth", "> 0", width, width > 0);
        check("KB-02-004", "Image naturalHeight", "> 0", height, height > 0);
        System.out.println("PWW_AUTO_02 RESULT: PASS");
    }

    private void check(String id, String check, Object expected, Object actual, boolean passed) {
        System.out.printf("| %s | %s | Expected: %s | Actual: %s | %s |%n",
                id, check, expected, actual, passed ? "PASS" : "FAIL");
        Assert.assertTrue(passed, id + " | " + check + " | Expected=" + expected + " | Actual=" + actual);
    }

    private boolean isHomeUrl(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "learnenglish.plt.pro.vn".equalsIgnoreCase(uri.getHost())
                    && (uri.getPath().isEmpty() || uri.getPath().equals("/") || uri.getPath().equals("/index.php"));
        } catch (IllegalArgumentException ex) { return false; }
    }
}
