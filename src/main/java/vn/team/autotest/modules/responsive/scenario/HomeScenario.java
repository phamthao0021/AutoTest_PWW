
package vn.team.autotest.modules.responsive.scenario;

import java.util.List;

import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import vn.team.autotest.modules.responsive.base.BaseScenario;

/**
 * Five standalone responsive Home scenarios,
 * two viewports each.
 */
public class HomeScenario extends BaseScenario {

    @Test(priority = 1, dataProvider = "mobile")
    public void PWW_AUTO_01_RWD(
            String device, int w, int h) {

        check(
            "PWW_AUTO_01_RWD",
            "Header/Menu/Hero",
            device, w, h,
            () -> {
                home().open(BASE);

                Assert.assertTrue(
                    home().header().isDisplayed(),
                    "Thiếu Header"
                );

                Assert.assertTrue(
                    home().hero().isDisplayed(),
                    "Thiếu Hero"
                );

                home().toggleMenu(true);

                Assert.assertTrue(
                    home().menuLink("Trang chủ")
                        .isDisplayed(),
                    "Menu thiếu Trang chủ"
                );

                home().toggleMenu(false);

                Assert.assertTrue(
                    home().hero().isDisplayed(),
                    "Hero bị ẩn sau khi đóng menu"
                );

                // TC01 chỉ kiểm tra Header/Menu/Hero.
                // Lỗi KPI được kiểm tra riêng ở TC04.
                long headerOverflow =
                    home().elementOverflow(home().header());

                long heroOverflow =
                    home().elementOverflow(home().hero());

                Assert.assertTrue(
                    headerOverflow <= 3
                        && heroOverflow <= 3,
                    "Header/Hero tràn ngang: header="
                        + headerOverflow + "px, hero="
                        + heroOverflow + "px"
                );

                if (home().overflow() > 3) {
                    System.out.println(
                        "[RWD][KNOWN DEFECT][STATS] 01: "
                        + "page overflow="
                        + home().overflow()
                        + "px; "
                        + home().overflowDiagnostics()
                    );
                }

                return "Header/Hero/Menu OK"
                    + "; headerOverflow="
                    + headerOverflow
                    + "px; heroOverflow="
                    + heroOverflow
                    + "px; wholePageOverflow="
                    + home().overflow()
                    + "px";
            }
        );
    }

    @Test(priority = 2, dataProvider = "mobile")
    public void PWW_AUTO_02_RWD(
            String device, int w, int h) {

        check(
            "PWW_AUTO_02_RWD",
            "Logo PLT và WordWise",
            device, w, h,
            () -> {
                home().open(BASE);

                WebElement brand = home().brand();

                home().mark(
                    brand,
                    "Kiểm tra Branding"
                );

                Assert.assertTrue(
                    home().fits(brand),
                    "Brand bị cắt/tràn khung"
                );

                String headerText =
                    home().header().getText();

                Assert.assertTrue(
                    headerText.toLowerCase()
                        .contains("wordwise")
                    || home().text().toLowerCase()
                        .contains("wordwise"),
                    "Thiếu thương hiệu WordWise"
                );

                List<WebElement> imgs =
                    home().visible(
                        By.cssSelector(
                            "header img,nav img,"
                            + ".navbar-brand img"
                        )
                    );

                for (WebElement img : imgs) {
                    Assert.assertTrue(
                        home().imageOk(img),
                        "Ảnh logo bị broken"
                    );
                }

                WebElement link =
                    brand.getTagName()
                        .equalsIgnoreCase("a")
                    ? brand
                    : brand.findElements(
                        By.xpath("ancestor::a[1]")
                    ).stream()
                        .findFirst()
                        .orElse(null);

                Assert.assertNotNull(
                    link,
                    "Logo/Branding không có liên kết"
                );

                home().click(
                    link,
                    "Chạm Logo"
                );

                Assert.assertTrue(
                    home().url().matches(
                        "https?://learnenglish"
                        + "\\.plt\\.pro\\.vn/?"
                        + "(?:index\\.php)?"
                        + "(?:[?#].*)?"
                    ),
                    "Logo không quay về Home: "
                        + home().url()
                );

                return "Brand OK; images="
                    + imgs.size()
                    + "; URL=" + home().url();
            }
        );
    }

    @Test(priority = 3, dataProvider = "mobile")
    public void PWW_AUTO_03_RWD(
            String device, int w, int h) {

        check(
            "PWW_AUTO_03_RWD",
            "CTA/Register/Login và Browser Back",
            device, w, h,
            () -> {
                // Tạo history entry để tránh
                // Chrome Back trở về data:,.
                home().open(BASE);
                home().open(BASE + "index.php");

                Assert.assertTrue(
                    home().onHome(),
                    "Chưa ở Home trước CTA: "
                        + home().url()
                );

                long historyBefore =
                    home().historyLength();

                WebElement cta = home().cta();

                home().mark(
                    cta,
                    "Kiểm tra CTA"
                );

                Assert.assertTrue(
                    home().fits(cta),
                    "CTA bị cắt ngang"
                );

                home().click(
                    cta,
                    "Chạm CTA chính"
                );

                String destination =
                    home().url();

                Assert.assertTrue(
                    destination.contains("register")
                    || destination.contains("login")
                    || destination.contains("learn"),
                    "CTA chuyển đến trang ngoài dự kiến: "
                        + destination
                );

                home().back();

                Assert.assertTrue(
                    home().onHome(),
                    "Browser Back không trở về Home: actual="
                    + home().url()
                    + "; historyBeforeCTA="
                    + historyBefore
                    + "; destination="
                    + destination
                );

                Assert.assertTrue(
                    home().homeContentPresent(),
                    "Trở về Home nhưng thiếu CTA/branding. URL="
                    + home().url()
                );

                home().toggleMenu(true);

                WebElement login =
                    home().menuLink("Đăng nhập");

                home().click(
                    login,
                    "Chạm Đăng nhập từ menu"
                );

                Assert.assertTrue(
                    home().url().contains("login"),
                    "Menu Đăng nhập sai URL: "
                    + home().url()
                );

                Assert.assertFalse(
                    home().visible(
                        By.cssSelector(
                            "input[type='password']"
                        )
                    ).isEmpty(),
                    "Thiếu ô mật khẩu"
                );

                return "CTA=" + destination
                    + "; Login=" + home().url()
                    + "; Back OK";
            }
        );
    }

    @Test(priority = 4, dataProvider = "mobile")
    public void PWW_AUTO_04_RWD(
            String device, int w, int h) {

        check(
            "PWW_AUTO_04_RWD",
            "Hero/KPI/Audience/Learning Modes",
            device, w, h,
            () -> {
                home().open(BASE);

                String content = home().text();

                for (String text : List.of(
                    "15,000+", "50,000+",
                    "98%", "4.9/5"
                )) {
                    Assert.assertTrue(
                        content.contains(text),
                        "Thiếu KPI: " + text
                    );
                }

                for (String text : List.of(
                    "WordWise", "Flashcards",
                    "Quiz", "Match"
                )) {
                    Assert.assertTrue(
                        content.toLowerCase()
                            .contains(text.toLowerCase()),
                        "Thiếu nội dung: " + text
                    );
                }

                Assert.assertTrue(
                    content.contains("Phù Hợp")
                    || content.contains("Phù Hợp Cho Ai")
                    || content.contains("Phù hợp"),
                    "Thiếu nhóm đối tượng"
                );

                Assert.assertTrue(
                    home().overflow() <= 3,
                    "Tràn ngang: "
                    + home().overflow()
                    + "px; Elements="
                    + home().overflowDiagnostics()
                );

                home().scrollBottom();

                Assert.assertTrue(
                    home().scroll() > 0,
                    "Trang không cuộn được"
                );

                return "4 KPI + 3 phương pháp OK"
                    + "; scroll=" + home().scroll()
                    + "; overflow=" + home().overflow();
            }
        );
    }

    @Test(priority = 5, dataProvider = "mobile")
    public void PWW_AUTO_05_RWD(
            String device, int w, int h) {

        check(
            "PWW_AUTO_05_RWD",
            "Footer trên các trang công khai",
            device, w, h,
            () -> {
                String[] labels = {
                    "",
                    "Giới thiệu",
                    "Phương pháp học",
                    "Học thử ngay"
                };

                StringBuilder observed =
                    new StringBuilder();

                for (String label : labels) {
                    home().open(BASE);

                    if (!label.isEmpty()) {
                        WebElement navigation =
                            home().firstVisible(
                                By.xpath(
                                    "//a[contains("
                                    + "normalize-space(.), '"
                                    + label + "')]"
                                )
                            );

                        Assert.assertNotNull(
                            navigation,
                            "Không tìm thấy link: " + label
                        );

                        home().click(
                            navigation,
                            "Đi tới " + label
                        );
                    }

                    WebElement footer =
                        home().footer();

                    home().mark(
                        footer,
                        "Footer: "
                        + (label.isEmpty()
                            ? "Home" : label)
                    );

                    Assert.assertTrue(
                        footer.isDisplayed(),
                        "Footer không hiển thị: " + label
                    );

                    Assert.assertTrue(
                        home().fits(footer),
                        "Footer bị tràn ngang: " + label
                    );

                    Assert.assertTrue(
                        footer.getText()
                            .contains("WordWise"),
                        "Thiếu WordWise ở Footer: " + label
                    );

                    long footerOverflow =
                        home().footerOverflow(footer);

                    Assert.assertTrue(
                        footerOverflow <= 3,
                        "Footer tự tràn ngang: "
                        + label + " ("
                        + footerOverflow + "px)"
                    );

                    if (home().overflow() > 3) {
                        System.out.println(
                            "[RWD][DEFECT][PAGE_OVERFLOW] "
                            + label + ": "
                            + home().overflow()
                            + "px; "
                            + home().overflowDiagnostics()
                        );
                    }

                    observed.append(
                        label.isEmpty()
                            ? "Home" : label
                    ).append(" OK; ");
                }

                return observed.toString();
            }
        );
    }
}
