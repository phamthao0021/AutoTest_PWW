
package vn.team.autotest.modules.responsive.scenario;

import java.util.*;

import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import vn.team.autotest.modules.responsive.base.BaseScenario;
import vn.team.autotest.modules.responsive.page.DashboardPage;

/**
 * PWW_AUTO_06_RWD
 *
 * USER:
 * Home -> Login -> dashboard.php -> 9 TC
 *
 * ADMIN:
 * Home -> Login -> admin.php (Người dùng)
 * -> Sidebar Dashboard -> 7 TC
 *
 * Đăng nhập đúng 1 lần cho mỗi vai trò.
 */
public class DashboardScenario extends BaseScenario {

    private final ThreadLocal<DashboardPage> pages =
        new ThreadLocal<>();

    private final ThreadLocal<Throwable> setupFailure =
        new ThreadLocal<>();

    private DashboardPage page() {
        return Objects.requireNonNull(
            pages.get(),
            "DashboardPage chưa tạo"
        );
    }

    @Override
    protected String reportFileName() {
        return "PWW_AUTO_06_RWD.xlsx";
    }

    @Override
    protected String reportSheetName() {
        return "DASHBOARD_06_RWD";
    }

    @DataProvider(name = "mobileFlow", parallel = true)
    public Object[][] flowDevices() {
        return mobile();
    }

    @DataProvider(name = "mobileUser")
    public Object[][] userDevices() {
        return mobile();
    }

    @DataProvider(name = "mobileAdmin")
    public Object[][] adminDevices() {
        return mobile();
    }

    // ========================================
    // CONFIGURATION
    // ========================================

    private String property(String key) {
        String value = System.getProperty(key);

        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        String envName = switch (key) {
            case "test.username" -> "PWW_USER";
            case "test.password" -> "PWW_PASSWORD";
            case "test.admin.username" -> "PWW_ADMIN_USER";
            case "test.admin.password" -> "PWW_ADMIN_PASSWORD";
            default -> null;
        };

        return envName == null
            ? ""
            : Objects.toString(
                System.getenv(envName), ""
            ).trim();
    }

    private void login(boolean admin) {

        String userKey = admin
            ? "test.admin.username"
            : "test.username";

        String passKey = admin
            ? "test.admin.password"
            : "test.password";

        String user = property(userKey);
        String pass = property(passKey);

        if (user.isBlank() || pass.isBlank()) {
            throw new IllegalStateException(
                "THIẾU CẤU HÌNH TÀI KHOẢN "
                + (admin ? "SUPERADMIN" : "USER")
                + ": cần -D" + userKey
                + " và -D" + passKey
                + " trong VM arguments."
            );
        }

        pages.set(
            new DashboardPage(driver())
        );

        page().login(user, pass, admin);
        page().requireViewport();
    }

    // ========================================
    // TEST EXECUTION
    // ========================================

    private String id(boolean admin, int index) {
        return String.format(
            "PWW_AUTO_06_RWD_%s_TC%02d",
            admin ? "ADMIN" : "USER",
            index
        );
    }

    private void run(
            boolean admin,
            int index,
            String title,
            String device,
            int w,
            int h,
            Action action) {

        checkInSession(
            id(admin, index),
            title,
            device,
            () -> {
                Throwable setup = setupFailure.get();

                if (setup != null) {
                    throw new IllegalStateException(
                        "Đăng nhập "
                        + (admin ? "ADMIN" : "USER")
                        + " chưa thành công: "
                        + setup.getMessage(),
                        setup
                    );
                }

                // Sử dụng phiên đăng nhập hiện tại.
                if (!page().url().contains("dashboard.php")) {
                    page().openDashboard();
                }

                return action.perform();
            }
        );
    }

    @FunctionalInterface
    private interface Step {
        void execute();
    }

    private int executeStep(Step step) {
        try {
            step.execute();
            return 0;
        } catch (Throwable ex) {
            System.err.println(
                "[RWD06][CASE FAIL] "
                + ex.getMessage()
            );
            return 1;
        }
    }

    private Throwable openRole(boolean admin) {
        try {
            login(admin);

            System.out.println(
                "[RWD06][LOGIN SUCCESS] "
                + (admin ? "SUPERADMIN" : "USER")
            );

            return null;

        } catch (Throwable ex) {
            System.err.println(
                "[RWD06][LOGIN FAILED] "
                + ex.getMessage()
            );

            return ex;
        }
    }

    private void switchToAdmin() {

        pages.remove();

        // Hủy cookies của User trước khi login Admin.
        driver().manage().deleteAllCookies();

        driver().get(BASE);

        System.out.println(
            "[RWD06] Kết thúc phiên User"
        );
    }

    // ========================================
    // MAIN FLOW
    // ========================================

    @Test(
        dataProvider = "mobileFlow",
        description = "Dashboard User -> Admin, login một lần mỗi role"
    )
    public void DASHBOARD_06_FLOW(
            String device,
            int w,
            int h) {

        int failures = 0;

        beginSession(w, h);

        try {

            // ===== USER =====

            System.out.println(
                "[RWD06] DEVICE=" + device
                + " | USER LOGIN -> 9 TC"
            );

            setupFailure.set(
                openRole(false)
            );

            failures += executeStep(
                () -> USER_01_url(device, w, h)
            );

            failures += executeStep(
                () -> USER_02_greeting(device, w, h)
            );

            failures += executeStep(
                () -> USER_03_scholar(device, w, h)
            );

            failures += executeStep(
                () -> USER_04_topicChart(device, w, h)
            );

            failures += executeStep(
                () -> USER_05_leaderboard(device, w, h)
            );

            failures += executeStep(
                () -> USER_06_weeklyChart(device, w, h)
            );

            failures += executeStep(
                () -> USER_07_focus(device, w, h)
            );

            failures += executeStep(
                () -> USER_08_actions(device, w, h)
            );

            failures += executeStep(
                () -> USER_09_mobileSidebar(device, w, h)
            );

            // ===== ADMIN =====

            try {
                switchToAdmin();

                setupFailure.set(
                    openRole(true)
                );

            } catch (Throwable ex) {
                setupFailure.set(ex);
            }

            System.out.println(
                "[RWD06] DEVICE=" + device
                + " | ADMIN LOGIN -> 7 TC"
            );

            failures += executeStep(
                () -> ADMIN_01_navigation(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_02_statistics(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_03_newUsersChart(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_04_actionsVocabulary(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_05_topFive(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_06_topTen(device, w, h)
            );

            failures += executeStep(
                () -> ADMIN_07_mobileSidebar(device, w, h)
            );

        } finally {
            pages.remove();
            setupFailure.remove();
            endSession();
        }

        if (failures > 0) {
            throw new AssertionError(
                device + " có " + failures
                + "/16 TC FAIL. Xem báo cáo Excel."
            );
        }
    }

    // ========================================
    // HELPERS
    // ========================================

    private void reportOverflow(String scope) {
        long px = page().overflow();

        if (px > 3) {
            System.err.println(
                "[RWD06][UI DEFECT] "
                + scope
                + " tràn ngang " + px + "px; "
                + page().overflowDetails()
            );
        }
    }

    private void assertKpi(
            String label,
            String key,
            int actual) {

        Assert.assertTrue(
            actual >= 0,
            "KPI âm: " + label + "=" + actual
        );

        // Chỉ so sánh expected khi đã cấu hình.
        String configured = property(key);

        if (!configured.isEmpty()) {
            int expected = Integer.parseInt(
                configured.replaceAll("[^0-9]", "")
            );

            Assert.assertEquals(
                actual,
                expected,
                "KPI " + label + " sai"
            );
        }
    }

    // ========================================
    // USER TC01 - URL
    // ========================================

    @Test(enabled = false)
    public void USER_01_url(
            String device, int w, int h) {

        run(false, 1,
            "User Dashboard URL & viewport",
            device, w, h,
            () -> {
                Assert.assertTrue(
                    page().url().contains("dashboard.php")
                );

                page().assertNoOverflow();

                return "URL=" + page().url()
                    + "; overflow=" + page().overflow();
            }
        );
    }

    // ========================================
    // USER TC02 - GREETING
    // ========================================

    @Test(enabled = false)
    public void USER_02_greeting(
            String device, int w, int h) {

        run(false, 2,
            "Lời chào và thống kê User",
            device, w, h,
            () -> {
                String h1 = page().visible(
                    By.tagName("h1")
                ).getText().trim();

                Assert.assertTrue(
                    h1.startsWith("Chào"),
                    "Lời chào không đúng: " + h1
                );

                Assert.assertTrue(
                    page().body().contains("điểm")
                    || page().body().contains("Từ vựng"),
                    "Thiếu thống kê User"
                );

                Assert.assertTrue(
                    page().body().contains(
                        "Tiến trình Scholar"
                    ),
                    "Thiếu tiến trình Scholar"
                );

                reportOverflow("USER");

                return "H1=" + h1;
            }
        );
    }

    // ========================================
    // USER TC03 - SCHOLAR
    // ========================================

    @Test(enabled = false)
    public void USER_03_scholar(
            String device, int w, int h) {

        run(false, 3,
            "Tiến trình Scholar",
            device, w, h,
            () -> {
                WebElement card =
                    page().card("Tiến trình Scholar");

                page().mark(card, "Scholar");

                String content = card.getText();

                Assert.assertTrue(
                    content.matches(
                        "(?s).*(CẤP|Cấp|Scholar).*"
                    ),
                    "Thiếu cấp độ"
                );

                Assert.assertTrue(
                    content.toLowerCase(Locale.ROOT)
                        .contains("điểm"),
                    "Thiếu điểm"
                );

                List<WebElement> bars =
                    card.findElements(
                        By.cssSelector(
                            "[role='progressbar'],"
                            + "progress,[style*='width:']"
                        )
                    );

                Assert.assertTrue(
                    bars.stream().anyMatch(
                        WebElement::isDisplayed
                    ),
                    "Thiếu progress bar"
                );

                reportOverflow("USER");

                return content.replaceAll("\\s+", " ");
            }
        );
    }

    // ========================================
    // USER TC04 - TOPIC CHART
    // ========================================

    @Test(enabled = false)
    public void USER_04_topicChart(
            String device, int w, int h) {

        run(false, 4,
            "Biểu đồ chủ đề",
            device, w, h,
            () -> {
                page().checkChart("topicChart", 1);

                reportOverflow("USER");

                return String.valueOf(
                    page().chart("topicChart")
                );
            }
        );
    }

    // ========================================
    // USER TC05 - LEADERBOARD
    // ========================================

    @Test(enabled = false)
    public void USER_05_leaderboard(
            String device, int w, int h) {

        run(false, 5,
            "BXH Chuỗi / Điểm / Từ vựng",
            device, w, h,
            () -> {
                String[] keys = {
                    "streak", "score", "words"
                };

                String[] names = {
                    "Chuỗi", "Điểm", "Từ vựng"
                };

                List<String> counts =
                    new ArrayList<>();

                for (int i = 0; i < 3; i++) {
                    int count =
                        page().leaderboardCount(
                            names[i], keys[i]
                        );

                    Assert.assertTrue(
                        count >= 5,
                        "BXH " + names[i]
                        + " thiếu dữ liệu"
                    );

                    counts.add(
                        names[i] + "=" + count
                    );
                }

                page().click(
                    page().tab("Chuỗi"),
                    "Khôi phục BXH Chuỗi"
                );

                reportOverflow("USER");

                return String.join("; ", counts);
            }
        );
    }

    // ========================================
    // USER TC06 - WEEKLY CHART
    // ========================================

    @Test(enabled = false)
    public void USER_06_weeklyChart(
            String device, int w, int h) {

        run(false, 6,
            "Biểu đồ hoạt động tuần",
            device, w, h,
            () -> {
                page().checkChart("weeklyChart", 7);

                Map<String, Object> chart =
                    page().chart("weeklyChart");

                Assert.assertEquals(
                    ((List<?>) chart.get("labels")).size(),
                    7
                );

                reportOverflow("USER");

                return String.valueOf(chart);
            }
        );
    }

    // ========================================
    // USER TC07 - FOCUS VOCABULARY
    // ========================================

    @Test(enabled = false)
    public void USER_07_focus(
            String device, int w, int h) {

        run(false, 7,
            "TỪ VỰNG TIÊU ĐIỂM",
            device, w, h,
            () -> {
                WebElement card =
                    page().focusCard();

                page().mark(
                    card,
                    "TỪ VỰNG TIÊU ĐIỂM"
                );

                String word =
                    page().focusValue(card, "wod-word");

                String ipa =
                    page().focusValue(card, "wod-ipa");

                String meaning =
                    page().focusValue(
                        card, "wod-definition"
                    );

                WebElement speak =
                    card.findElement(
                        By.cssSelector(
                            "button[onclick='speakCurrentWord()']"
                        )
                    );

                Assert.assertTrue(
                    speak.isDisplayed()
                    && speak.isEnabled(),
                    "Thiếu nút phát âm"
                );

                List<WebElement> examples =
                    card.findElements(
                        By.id("wod-example")
                    );

                String example =
                    examples.isEmpty()
                    ? "NOT_FOUND"
                    : examples.get(0).isDisplayed()
                        ? "VISIBLE" : "HIDDEN";

                reportOverflow("USER");

                return "word=" + word
                    + "; IPA=" + ipa
                    + "; meaning=" + meaning
                    + "; example=" + example;
            }
        );
    }

    // ========================================
    // USER TC08 - QUICK ACTIONS
    // ========================================

    @Test(enabled = false)
    public void USER_08_actions(
            String device, int w, int h) {

        run(false, 8,
            "4 lối tắt User",
            device, w, h,
            () -> {
                String[] labels = {
                    "Học tập",
                    "Trò chơi",
                    "Thư viện",
                    "Thẻ ghi nhớ"
                };

                for (String label : labels) {
                    WebElement link =
                        page().visible(
                            By.xpath(
                                "//a[normalize-space(.)='"
                                + label
                                + "' or .//*[normalize-space(.)='"
                                + label + "']]"
                            )
                        );

                    Assert.assertTrue(
                        link.isDisplayed(),
                        "Thiếu: " + label
                    );

                    page().mark(
                        link, "Action: " + label
                    );
                }

                reportOverflow("USER");

                return Arrays.toString(labels);
            }
        );
    }

    // ========================================
    // USER TC09 - SIDEBAR
    // ========================================

    @Test(enabled = false)
    public void USER_09_mobileSidebar(
            String device, int w, int h) {

        run(false, 9,
            "Sidebar User mobile",
            device, w, h,
            () -> {
                boolean initial =
                    page().sidebarOpen();

                page().setSidebar(true);

                Assert.assertTrue(
                    page().sidebarOpen()
                );

                Assert.assertTrue(
                    page().href("dashboard.php")
                        .isDisplayed()
                );

                page().setSidebar(false);

                Assert.assertFalse(
                    page().sidebarOpen()
                );

                page().scrollDown();

                Assert.assertTrue(
                    page().scrollY() > 0
                );

                reportOverflow("USER");

                return "Sidebar initial="
                    + initial
                    + "; open/close OK";
            }
        );
    }

    // ========================================
    // ADMIN TC01 - NAVIGATION
    // ========================================

    @Test(enabled = false)
    public void ADMIN_01_navigation(
            String device, int w, int h) {

        run(true, 1,
            "Admin → Dashboard",
            device, w, h,
            () -> {
                String h1 = page().visible(
                    By.tagName("h1")
                ).getText();

                Assert.assertTrue(
                    h1.contains(
                        "Chào Siêu Cấp Quản Trị Viên!"
                    ),
                    "Sai lời chào Admin"
                );

                page().assertNoOverflow();

                return "H1=" + h1
                    + "; URL=" + page().url();
            }
        );
    }

    // ========================================
    // ADMIN TC02 - KPI
    // ========================================

    @Test(enabled = false)
    public void ADMIN_02_statistics(
            String device, int w, int h) {

        run(true, 2,
            "4 KPI Dashboard Admin",
            device, w, h,
            () -> {
                String[] labels = {
                    "NGƯỜI DÙNG (2026)",
                    "TỪ VỰNG TỔNG",
                    "CHỦ ĐỀ",
                    "LƯỢT HỌC (2026)"
                };

                String[] keys = {
                    "admin.expectedUsers2026",
                    "admin.expectedVocabularyTotal",
                    "admin.expectedTopicsTotal",
                    "admin.expectedLearning2026"
                };

                List<String> actuals =
                    new ArrayList<>();

                for (int i = 0; i < 4; i++) {
                    int actual =
                        page().adminStatValue(labels[i]);

                    assertKpi(
                        labels[i],
                        keys[i],
                        actual
                    );

                    actuals.add(
                        labels[i] + "=" + actual
                    );
                }

                reportOverflow("ADMIN");

                return String.join("; ", actuals);
            }
        );
    }

    // ========================================
    // ADMIN TC03 - CHART AND YEAR FILTER
    // ========================================

    @Test(enabled = false)
    public void ADMIN_03_newUsersChart(
            String device, int w, int h) {

        run(true, 3,
            "Thống kê Người Dùng Mới",
            device, w, h,
            () -> {
                WebElement section =
                    page().adminSection(
                        "Thống kê Người Dùng Mới"
                    );

                WebElement year =
                    section.findElement(
                        By.id("inputYear")
                    );

                Assert.assertEquals(
                    year.getDomAttribute("type"),
                    "number"
                );

                Assert.assertEquals(
                    year.getDomAttribute("min"),
                    "2000"
                );

                Assert.assertEquals(
                    year.getDomAttribute("max"),
                    "2099"
                );

                for (String mode :
                        new String[]{
                            "week", "month", "year"
                        }) {

                    WebElement tab =
                        section.findElement(
                            By.cssSelector(
                                "button.chart-tab"
                                + "[onclick*=\"'"
                                + mode + "'\"]"
                            )
                        );

                    page().click(
                        tab, "Chart mode: " + mode
                    );

                    Assert.assertTrue(
                        tab.getDomAttribute("class")
                            .contains("active")
                    );

                    page().checkChart(
                        "adminUserChart", 1
                    );
                }

                WebElement submit =
                    section.findElement(
                        By.xpath(
                            ".//form[.//*[@id='inputYear']]"
                            + "//button[@type='submit']"
                        )
                    );

                year.clear();
                year.sendKeys("2025");

                page().click(
                    submit, "Lọc 2025"
                );

                Assert.assertTrue(
                    page().url()
                        .contains("year=2025")
                );

                Assert.assertEquals(
                    page().visible(
                        By.id("inputYear")
                    ).getDomProperty("value"),
                    "2025"
                );

                reportOverflow("ADMIN");

                return "Chart week/month/year OK; "
                    + page().url();
            }
        );
    }

    // ========================================
    // ADMIN TC04 - ACTIONS / VOCABULARY
    // ========================================

    @Test(enabled = false)
    public void ADMIN_04_actionsVocabulary(
            String device, int w, int h) {

        run(true, 4,
            "Admin actions + quản lý từ vựng",
            device, w, h,
            () -> {
                String[] paths = {
                    "admin_type.php",
                    "admin_vocab.php",
                    "admin.php",
                    "notifications.php"
                };

                for (String path : paths) {
                    page().clickHref(path);
                    page().openDashboard();
                }

                WebElement panel =
                    page().adminSection(
                        "Danh sách từ vựng"
                    );

                page().click(
                    panel.findElement(
                        By.id("topicDropdownBtn")
                    ),
                    "Bộ lọc chủ đề"
                );

                WebElement menu =
                    page().visible(
                        By.id("topicDropdownMenu")
                    );

                List<WebElement> checks =
                    menu.findElements(
                        By.cssSelector(
                            "#topicCheckboxList "
                            + "input.topic-checkbox"
                        )
                    );

                Assert.assertFalse(
                    checks.isEmpty(),
                    "Thiếu checkbox chủ đề"
                );

                Assert.assertTrue(
                    menu.findElement(
                        By.id("selectAllTopics")
                    ).isSelected(),
                    "Mặc định phải chọn tất cả"
                );

                // Tương tác qua label/control thực tế.
                page().setAllTopicCheckboxes(false);
                page().assertAllTopicsSelected(false);

                page().setAllTopicCheckboxes(true);
                page().assertAllTopicsSelected(true);

                WebElement list =
                    page().visible(
                        By.id("vocabListContainer")
                    );

                Assert.assertFalse(
                    list.findElements(
                        By.xpath("./*")
                    ).isEmpty()
                );

                reportOverflow("ADMIN");

                return "Actions OK; checkbox="
                    + checks.size();
            }
        );
    }

    // ========================================
    // ADMIN TC05 - TOP 5
    // ========================================

    @Test(enabled = false)
    public void ADMIN_05_topFive(
            String device, int w, int h) {

        run(true, 5,
            "Top 5 BXH Admin",
            device, w, h,
            () -> {
                String[] names = {
                    "Chuỗi", "Điểm", "Từ vựng"
                };

                String[] keys = {
                    "streak", "score", "words"
                };

                List<String> result =
                    new ArrayList<>();

                for (int i = 0; i < 3; i++) {

                    page().click(
                        page().tab(names[i]),
                        "Top 5 " + names[i]
                    );

                    List<WebElement> rows =
                        page().leaderboard(
                            keys[i]
                        ).findElements(
                            By.cssSelector(".rank-row")
                        );

                    Assert.assertEquals(
                        rows.size(), 5
                    );

                    int previous =
                        Integer.MAX_VALUE;

                    for (int j = 0;
                            j < rows.size(); j++) {

                        WebElement row =
                            rows.get(j);

                        String rank =
                            row.findElement(
                                By.cssSelector(
                                    ".rounded-full.font-bold"
                                )
                            ).getText().trim();

                        Assert.assertEquals(
                            rank,
                            String.valueOf(j + 1)
                        );

                        String raw =
                            row.findElement(
                                By.cssSelector(
                                    "div.shrink-0"
                                )
                            ).getText().replaceAll(
                                "[^0-9]", ""
                            );

                        Assert.assertFalse(
                            raw.isEmpty()
                        );

                        int score =
                            Integer.parseInt(raw);

                        Assert.assertTrue(
                            score <= previous
                        );

                        previous = score;
                    }

                    result.add(
                        names[i] + "=" + rows.size()
                    );
                }

                reportOverflow("ADMIN");

                return String.join("; ", result);
            }
        );
    }

    // ========================================
    // ADMIN TC06 - TOP 10
    // ========================================

    @Test(enabled = false)
    public void ADMIN_06_topTen(
            String device, int w, int h) {

        run(true, 6,
            "Top 10 chủ đề và biểu đồ",
            device, w, h,
            () -> {
                WebElement table =
                    page().adminSection(
                        "Từ vựng theo chủ đề"
                    ).findElement(
                        By.tagName("table")
                    );

                String[] columns = {
                    "STT",
                    "Tên loại từ",
                    "Tổng lượt truy cập"
                };

                List<WebElement> heads =
                    table.findElements(
                        By.cssSelector("thead th")
                    );

                Assert.assertEquals(
                    heads.size(), 3
                );

                // Không phân biệt hoa/thường.
                for (int i = 0; i < 3; i++) {
                    Assert.assertEquals(
                        heads.get(i).getText()
                            .trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase(Locale.ROOT),
                        columns[i].toLowerCase(
                            Locale.ROOT
                        )
                    );
                }

                List<WebElement> rows =
                    table.findElements(
                        By.cssSelector("tbody tr")
                    );

                Assert.assertEquals(
                    rows.size(), 10
                );

                List<String> topics =
                    new ArrayList<>();

                List<Integer> counts =
                    new ArrayList<>();

                int previous =
                    Integer.MAX_VALUE;

                for (int i = 0; i < 10; i++) {
                    List<WebElement> cells =
                        rows.get(i).findElements(
                            By.tagName("td")
                        );

                    Assert.assertEquals(
                        cells.size(), 3
                    );

                    Assert.assertEquals(
                        cells.get(0).getText().trim(),
                        "#" + (i + 1)
                    );

                    String topic =
                        cells.get(1).getText().trim();

                    String digits =
                        cells.get(2).getText()
                            .replaceAll("[^0-9]", "");

                    Assert.assertFalse(
                        topic.isEmpty()
                    );

                    Assert.assertFalse(
                        digits.isEmpty()
                    );

                    int count =
                        Integer.parseInt(digits);

                    Assert.assertTrue(
                        count <= previous
                    );

                    previous = count;
                    topics.add(topic);
                    counts.add(count);
                }

                Map<String, Object> chart =
                    page().chart("adminTopicChart");

                Assert.assertEquals(
                    chart.get("type"), "bar"
                );

                List<?> labels =
                    (List<?>) chart.get("labels");

                List<?> datasets =
                    (List<?>) chart.get("datasets");

                Assert.assertEquals(
                    labels.size(), 10
                );

                Assert.assertFalse(
                    datasets.isEmpty()
                );

                List<?> values =
                    (List<?>) datasets.get(0);

                Assert.assertEquals(
                    values.size(), 10
                );

                for (int i = 0; i < 10; i++) {
                    Assert.assertEquals(
                        String.valueOf(labels.get(i)),
                        topics.get(i)
                    );

                    Assert.assertEquals(
                        ((Number) values.get(i))
                            .intValue(),
                        counts.get(i).intValue()
                    );
                }

                reportOverflow("ADMIN");

                return "Top 10 bảng/biểu đồ khớp";
            }
        );
    }

    // ========================================
    // ADMIN TC07 - SIDEBAR
    // ========================================

    @Test(enabled = false)
    public void ADMIN_07_mobileSidebar(
            String device, int w, int h) {

        run(true, 7,
            "Sidebar Admin mobile",
            device, w, h,
            () -> {
                boolean initial =
                    page().sidebarOpen();

                page().setSidebar(true);

                Assert.assertTrue(
                    page().sidebarOpen()
                );

                Assert.assertTrue(
                    page().href("dashboard.php")
                        .isDisplayed()
                );

                page().setSidebar(false);

                Assert.assertFalse(
                    page().sidebarOpen()
                );

                page().scrollDown();

                Assert.assertTrue(
                    page().scrollY() > 0
                );

                reportOverflow("ADMIN");

                return "Sidebar initial="
                    + initial
                    + "; open/close OK";
            }
        );
    }
}
