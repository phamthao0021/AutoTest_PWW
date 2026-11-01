
package vn.team.autotest.modules.home.scenario;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;


import vn.team.autotest.modules.home.page.HomePage;
import vn.team.autotest.modules.home.page.HomePage.FooterSnapshot;

/**
 * PWW_AUTO_05 - Footer Functional, Integration, Negative và Business Flow.
 *
 * Tối ưu:
 * 1. Chỉ thử mở Home tối đa 2 lần.
 * 2. Không lặp lại các checkpoint không cần thiết.
 * 3. Chỉ kết luận PASS khi assertion thực tế thành công.
 * 4. Phân loại href '#' là PLACEHOLDER, không nhầm với href rỗng.
 * 5. Không fallback sang URL khác khi click menu chưa được xác minh.
 */
public class HomeScenario5 extends HomeBaseScenario {

    private static final String HOME_URL =
            "https://learnenglish.plt.pro.vn/";

    private static final int MAX_HOME_ATTEMPTS = 2;

    private static final int COL_ID = 10;
    private static final int COL_SCREEN = 22;
    private static final int COL_CHECK = 28;
    private static final int COL_EXPECTED = 32;
    private static final int COL_ACTUAL = 32;
    private static final int COL_STATUS = 6;

    // =========================================================
    // KB-05-001: HOME FOOTER BASELINE
    // =========================================================

    @Test(
        description = "KB-05-001 - Home Footer Baseline",
        groups = {"home", "thao", "footer", "functional", "positive"}
    )
    public void PWW_AUTO_05_verifyHomeFooterBaseline() {

        startCase("KB-05-001", "Home Footer Baseline");

        HomePage page = openHomeSafely();

        assertCheck(
            "KB-05-001", "Home / Footer", "Footer hiển thị",
            "Footer phải hiển thị",
            page.waitUntilFooterReadySafe(),
            "Footer đã sẵn sàng"
        );

        String footerText = page.getFooterText();

        assertCheck(
            "KB-05-001", "Home / Footer", "Footer text",
            "Nội dung Footer không được rỗng",
            footerText != null && !footerText.isBlank(),
            "Text length = " + (footerText == null ? 0 : footerText.length())
        );

        List<String> links = page.getFooterLinkTexts();
        List<String> hrefs = page.getFooterHrefs();

        assertCheck(
            "KB-05-001", "Home / Footer", "Footer links",
            "Phải có ít nhất một link có text",
            !links.isEmpty(),
            "Link text count = " + links.size()
        );

        assertCheck(
            "KB-05-001", "Home / Footer", "Footer hrefs",
            "Phải có ít nhất một href",
            !hrefs.isEmpty(),
            "Href count = " + hrefs.size()
        );

        assertCheck(
            "KB-05-001", "Home", "Trang lỗi",
            "Không phải trang lỗi 404/500",
            !page.isErrorPage(),
            "URL = " + page.getCurrentUrlSafe()
        );

        finishCase("KB-05-001");
    }

    // =========================================================
    // KB-05-002: FOOTER / GIỚI THIỆU
    // =========================================================

    @Test(
        description = "KB-05-002 - Footer Integration / Giới thiệu",
        groups = {"home", "thao", "footer", "integration", "regression"}
    )
    public void PWW_AUTO_05_verifyAboutFooter() {
        verifyFooterPage("KB-05-002", "Giới thiệu", "introduce.php");
    }

    // =========================================================
    // KB-05-003: FOOTER / PHƯƠNG PHÁP HỌC
    // =========================================================

    @Test(
        description = "KB-05-003 - Footer Integration / Phương pháp học",
        groups = {"home", "thao", "footer", "integration", "regression"}
    )
    public void PWW_AUTO_05_verifyMethodFooter() {
        verifyFooterPage("KB-05-003", "Phương pháp học", "method.php");
    }

    // =========================================================
    // KB-05-004: FOOTER / HỌC THỬ NGAY
    // =========================================================

    @Test(
        description = "KB-05-004 - Footer Integration / Học thử ngay",
        groups = {"home", "thao", "footer", "integration", "regression"}
    )
    public void PWW_AUTO_05_verifyTrialFooter() {
        verifyFooterPage("KB-05-004", "Học thử ngay", "learnnow.php");
    }

    // =========================================================
    // SHARED FOOTER INTEGRATION
    // =========================================================

    private void verifyFooterPage(
            String caseId,
            String menuText,
            String expectedPath) {

        startCase(caseId, "Footer Integration / " + menuText);

        HomePage home = openHomeSafely();

        Assert.assertTrue(
            home.waitUntilFooterReadySafe(),
            "Không đọc được Footer baseline tại Home"
        );

        FooterSnapshot baseline = home.readFooterSnapshot();

        assertCheck(
            caseId, "Home / Header", "Menu hiển thị",
            "Menu " + menuText + " phải hiển thị",
            home.isHeaderItemDisplayed(menuText),
            "Menu = " + menuText
        );

        String href = home.getHeaderItemHref(menuText);

        assertCheck(
            caseId, "Home / Header", "Menu href",
            "Href phải chứa " + expectedPath,
            href != null && href.contains(expectedPath),
            "Href = " + href
        );

        String beforeUrl = home.getCurrentUrlSafe();

        // Click menu thật để kiểm tra luồng điều hướng của người dùng.
        try {
            home.clickHeaderItem(menuText);
        } catch (TimeoutException ex) {
            home.stopPageLoading();
            throw new AssertionError(
                "Click menu bị timeout: " + menuText, ex
            );
        }

        String afterUrl = home.getCurrentUrlSafe();

        assertCheck(
            caseId, "Điều hướng", "URL trang đích",
            "URL phải chứa " + expectedPath,
            afterUrl != null && afterUrl.contains(expectedPath),
            "Before = " + beforeUrl + "; After = " + afterUrl
        );

        assertCheck(
            caseId, menuText, "Body hiển thị",
            "Trang đích phải có body",
            home.waitUntilBodyReady(),
            "URL = " + afterUrl
        );

        assertCheck(
            caseId, menuText, "Footer hiển thị",
            "Footer phải xuất hiện trên trang đích",
            home.waitUntilFooterReadySafe(),
            "Footer đã sẵn sàng"
        );

        FooterSnapshot actual = home.readFooterSnapshot();

        assertCheck(
            caseId, menuText + " / Footer", "Danh sách link",
            "Danh sách Footer link giống Home",
            baseline.linkTexts().equals(actual.linkTexts()),
            "Expected = " + baseline.linkTexts()
                + "; Actual = " + actual.linkTexts()
        );

        assertCheck(
            caseId, menuText + " / Footer", "Danh sách href",
            "Danh sách href giống Home",
            baseline.hrefs().equals(actual.hrefs()),
            "Expected = " + baseline.hrefs()
                + "; Actual = " + actual.hrefs()
        );

        assertCheck(
            caseId, menuText + " / Footer", "Nội dung Footer",
            "Footer text giống Home sau chuẩn hóa",
            normalize(baseline.text()).equals(normalize(actual.text())),
            "Expected length = " + normalize(baseline.text()).length()
                + "; Actual length = " + normalize(actual.text()).length()
        );

        assertCheck(
            caseId, menuText, "Trang lỗi",
            "Không phải trang lỗi 404/500",
            !home.isErrorPage(),
            "URL = " + home.getCurrentUrlSafe()
        );

        finishCase(caseId);
    }

    // =========================================================
    // KB-05-005: NEGATIVE FOOTER HREF VALIDATION
    // =========================================================

    @Test(
        description = "KB-05-005 - Negative Footer Link Validation",
        groups = {"home", "thao", "footer", "negative", "data-validation"}
    )
    public void PWW_AUTO_05_verifyInvalidFooterLinks() {

        String caseId = "KB-05-005";

        startCase(caseId, "Footer Negative Link Validation");

        HomePage page = openHomeSafely();

        Assert.assertTrue(
            page.waitUntilFooterReadySafe(),
            "Footer Home không load được"
        );

        List<String> hrefs = page.getFooterHrefs();
        List<String> texts = page.getFooterLinkTexts();

        Assert.assertFalse(
            hrefs.isEmpty(),
            "Không có href để kiểm tra"
        );

        int validCount = 0;
        int placeholderCount = 0;
        int invalidCount = 0;

        List<String> invalidLinks = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (int i = 0; i < hrefs.size(); i++) {

            String href = hrefs.get(i);
            String text = i < texts.size()
                    ? texts.get(i)
                    : "(không có text)";

            String normalizedHref = href == null
                    ? ""
                    : href.trim();

            boolean blank = normalizedHref.isBlank();
            boolean javascript = normalizedHref
                    .toLowerCase()
                    .startsWith("javascript:");
            boolean placeholder = normalizedHref.equals("#");

            String classification;
            boolean validForTechnicalCheck;

            if (blank || javascript) {
                invalidCount++;
                invalidLinks.add(text + " -> " + normalizedHref);
                classification = "INVALID";
                validForTechnicalCheck = false;

            } else if (placeholder) {
                placeholderCount++;
                placeholders.add(text + " -> #");
                classification = "PLACEHOLDER";
                validForTechnicalCheck = true;

            } else {
                validCount++;
                classification = "VALID";
                validForTechnicalCheck = true;
            }

            // '#' được ghi nhận riêng vì là placeholder điều hướng,
            // không phải href rỗng hoặc javascript:.
            printRow(
                caseId,
                "Footer",
                "Href #" + (i + 1) + " / " + text,
                "Không rỗng, không javascript:",
                classification + " | href = " + normalizedHref,
                validForTechnicalCheck ? "PASS" : "FAIL"
            );
        }

        printRow(
            caseId, "Footer", "Tổng hợp href",
            "Không có href rỗng hoặc javascript:",
            "Valid = " + validCount
                + "; Placeholder = " + placeholderCount
                + "; Invalid = " + invalidCount,
            invalidCount == 0 ? "PASS" : "FAIL"
        );

        if (!placeholders.isEmpty()) {
            System.out.println(
                "Placeholder cần xem xét về nghiệp vụ: " + placeholders
            );
        }

        Assert.assertEquals(
            invalidCount,
            0,
            "Phát hiện href không hợp lệ: " + invalidLinks
        );

        assertCheck(
            caseId, "Home", "Trang vẫn hoạt động",
            "Không phải trang lỗi 404/500",
            !page.isErrorPage(),
            "URL = " + page.getCurrentUrlSafe()
        );

        finishCase(caseId);
    }

    // =========================================================
    // KB-05-006: BUSINESS FLOW
    // HOME -> ABOUT -> HOME -> METHOD -> HOME -> TRIAL
    // =========================================================

    @Test(
        description = "KB-05-006 - Footer Cross Page Business Flow",
        groups = {"home", "thao", "footer", "integration", "flow", "regression"}
    )
    public void PWW_AUTO_05_verifyFooterBusinessFlow() {

        String caseId = "KB-05-006";

        startCase(caseId, "Footer Cross Page Business Flow");

        HomePage home = openHomeSafely();

        Assert.assertTrue(
            home.waitUntilFooterReadySafe(),
            "Không đọc được Footer baseline"
        );

        FooterSnapshot baseline = home.readFooterSnapshot();

        // Giữ nguyên một browser session và thực hiện đúng luồng.
        navigateFlowStep(home, baseline, "Giới thiệu", "introduce.php");

        home = returnHome(home, "Giới thiệu -> Home");

        navigateFlowStep(home, baseline, "Phương pháp học", "method.php");

        home = returnHome(home, "Phương pháp học -> Home");

        navigateFlowStep(home, baseline, "Học thử ngay", "learnnow.php");

        printRow(
            caseId, "Business Flow", "Kết thúc luồng",
            "Hoàn thành cả ba trang đích",
            "Đã kiểm tra Giới thiệu, Phương pháp học, Học thử ngay",
            "PASS"
        );

        finishCase(caseId);
    }

    private void navigateFlowStep(
            HomePage home,
            FooterSnapshot baseline,
            String menuText,
            String expectedPath) {

        String caseId = "KB-05-006";

        Assert.assertTrue(
            home.isHeaderItemDisplayed(menuText),
            "Không tìm thấy menu: " + menuText
        );

        String href = home.getHeaderItemHref(menuText);

        Assert.assertTrue(
            href != null && href.contains(expectedPath),
            "Href sai tại " + menuText + ": " + href
        );

        try {
            home.clickHeaderItem(menuText);
        } catch (TimeoutException ex) {
            home.stopPageLoading();
            throw new AssertionError(
                "Timeout khi click " + menuText, ex
            );
        }

        String actualUrl = home.getCurrentUrlSafe();

        assertCheck(
            caseId, "Business Flow", "Đi tới " + menuText,
            "URL chứa " + expectedPath,
            actualUrl.contains(expectedPath),
            "URL = " + actualUrl
        );

        Assert.assertTrue(
            home.waitUntilFooterReadySafe(),
            "Footer không hiển thị tại " + menuText
        );

        FooterSnapshot actual = home.readFooterSnapshot();

        boolean textMatch = normalize(baseline.text())
                .equals(normalize(actual.text()));

        boolean linksMatch = baseline.linkTexts()
                .equals(actual.linkTexts());

        boolean hrefsMatch = baseline.hrefs()
                .equals(actual.hrefs());

        assertCheck(
            caseId, menuText + " / Footer", "Toàn vẹn Footer",
            "Text, link và href phải giống baseline",
            textMatch && linksMatch && hrefsMatch,
            "Text = " + textMatch
                + "; Links = " + linksMatch
                + "; Hrefs = " + hrefsMatch
        );

        Assert.assertFalse(
            home.isErrorPage(),
            "Trang lỗi sau khi mở " + menuText
        );
    }

    private HomePage returnHome(HomePage previousPage, String flowName) {

        // Điều hướng về Home trong cùng browser session.
        try {
            driver().navigate().to(HOME_URL);
        } catch (TimeoutException ex) {
            previousPage.stopPageLoading();
        }

        HomePage home = new HomePage(driver());

        String url = home.getCurrentUrlSafe();

        boolean homeUrl = isHomeUrl(url);

        assertCheck(
            "KB-05-006", "Business Flow", flowName,
            "URL phải là Home",
            homeUrl,
            "URL = " + url
        );

        Assert.assertTrue(homeUrl, "Không quay về Home: " + url);

        Assert.assertTrue(
            home.waitUntilFooterReadySafe(),
            "Footer không hiển thị sau khi quay về Home"
        );

        return home;
    }

    // =========================================================
    // HOME / ASSERTION HELPERS
    // =========================================================

    private HomePage openHomeSafely() {

        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_HOME_ATTEMPTS; attempt++) {

            try {
                open("/");
            } catch (TimeoutException ex) {
                lastException = ex;
                stopLoadingSafe();
            } catch (RuntimeException ex) {
                lastException = ex;
                stopLoadingSafe();
            }

            HomePage page = new HomePage(driver());

            boolean bodyReady = page.waitUntilBodyReady();
            boolean homeUrl = isHomeUrl(page.getCurrentUrlSafe());

            if (bodyReady && homeUrl) {
                return page;
            }
        }

        String message = "Không mở được Home sau "
                + MAX_HOME_ATTEMPTS + " lần thử. URL = "
                + safeCurrentUrl();

        if (lastException != null) {
            message += " | Lỗi cuối = " + lastException.getMessage();
        }

        Assert.fail(message);

        // Chỉ để compiler nhận diện giá trị trả về;
        // Assert.fail sẽ kết thúc testcase.
        return new HomePage(driver());
    }

    private void assertCheck(
            String id,
            String screen,
            String check,
            String expected,
            boolean passed,
            String actual) {

        printRow(
            id,
            screen,
            check,
            expected,
            actual,
            passed ? "PASS" : "FAIL"
        );

        Assert.assertTrue(
            passed,
            id + " | " + screen + " | " + check
                + " | Expected: " + expected
                + " | Actual: " + actual
        );
    }

    private void startCase(String id, String name) {
        System.out.println();
        System.out.println("=".repeat(90));
        System.out.println("PWW_AUTO_05 | " + id + " - " + name);
        System.out.println("=".repeat(90));

        printRow(
            "ID", "Màn hình", "Kiểm tra",
            "Expected", "Actual", "Status"
        );
    }

    private void finishCase(String id) {
        System.out.println(id + " - TEST METHOD COMPLETED");
    }

    // =========================================================
    // TABLE OUTPUT
    // =========================================================

    private void printRow(
            String id,
            String screen,
            String check,
            String expected,
            String actual,
            String status) {

        System.out.printf(
            "| %-10s | %-22s | %-28s | %-32s | %-32s | %-6s |%n",
            clip(id, COL_ID),
            clip(screen, COL_SCREEN),
            clip(check, COL_CHECK),
            clip(expected, COL_EXPECTED),
            clip(actual, COL_ACTUAL),
            clip(status, COL_STATUS)
        );
    }

    // Giới hạn độ dài từng ô để tránh Console bị tràn ngang.
    // Log đầy đủ chi tiết lỗi vẫn được ghi qua AssertionError.
    private String clip(String value, int width) {
        if (value == null) {
            return "(null)";
        }

        String text = value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ").trim();

        if (text.length() <= width) {
            return text;
        }

        if (width <= 3) {
            return text.substring(0, width);
        }

        return text.substring(0, width - 3) + "...";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    private boolean isHomeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String value = url.trim();

        return value.equals(HOME_URL)
                || value.equals("https://learnenglish.plt.pro.vn")
                || value.endsWith("/index.php");
    }

    private void stopLoadingSafe() {
        try {
            ((JavascriptExecutor) driver())
                    .executeScript("window.stop();");
        } catch (RuntimeException ignored) {
            // Không làm lỗi gốc bị che bởi lỗi dừng tải trang.
        }
    }

    private String safeCurrentUrl() {
        try {
            String url = driver().getCurrentUrl();
            return url == null ? "" : url.trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }
}
