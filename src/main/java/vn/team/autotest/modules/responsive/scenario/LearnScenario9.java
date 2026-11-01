package vn.team.autotest.modules.responsive.scenario;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.team.autotest.modules.responsive.base.BaseScenario;
import vn.team.autotest.modules.responsive.page.DashboardPage;
import vn.team.autotest.modules.responsive.page.LearnPage;
import vn.team.autotest.modules.responsive.page.LearnModePage;
import vn.team.autotest.modules.responsive.page.FlashCardPage;
import vn.team.autotest.modules.responsive.page.ResponsiveLayoutAudit;

/** KB-09-001..019 chính xác theo kịch bản Desktop; User login once / viewport. */
public final class LearnScenario9 extends BaseScenario {
    private final ThreadLocal<LearnModePage> topicPage = new ThreadLocal<>();
    private final ThreadLocal<FlashCardPage> flashPage = new ThreadLocal<>();
    private final ThreadLocal<WebDriverWait> waiter = new ThreadLocal<>();
    private final ThreadLocal<Throwable> setupError = new ThreadLocal<>();
    private final ThreadLocal<ResponsiveLayoutAudit> audits = new ThreadLocal<>();

    @Override protected String reportFileName() { return "PWW_AUTO_09_RWD.xlsx"; }
    @Override protected String reportSheetName() { return "LEARN_09_RWD"; }

    @DataProvider(name="mobileFlash", parallel=true)
    public Object[][] mobileFlash() { return mobile(); }

    private LearnModePage learn() { return Objects.requireNonNull(topicPage.get(), "LearnModePage chưa sẵn sàng"); }
    private FlashCardPage flash() { return Objects.requireNonNull(flashPage.get(), "FlashCardPage chưa sẵn sàng"); }
    private WebDriverWait webWait() { return Objects.requireNonNull(waiter.get(), "WebDriverWait chưa sẵn sàng"); }
    private String audit(String stage) { return Objects.requireNonNull(audits.get(), "Responsive audit chưa khởi tạo").inspect(stage); }

    private static String config(String prop,String env) {
        String s = System.getProperty(prop, "");
        return s.isBlank() ? Objects.toString(System.getenv(env), "").trim() : s.trim();
    }

    private void setup() {
        String user=config("test.username","PWW_USER");
        String pass=config("test.password","PWW_PASSWORD");
        if(user.isBlank() || pass.isBlank()) throw new IllegalStateException(
            "Thiếu credentials: -Dtest.username và -Dtest.password hoặc PWW_USER/PWW_PASSWORD");
        new DashboardPage(driver()).login(user,pass,false);
        new DashboardPage(driver()).requireViewport();
        new LearnPage(driver()).fromDashboard();
        topicPage.set(new LearnModePage(driver()));
        flashPage.set(new FlashCardPage(driver()));
        waiter.set(new WebDriverWait(driver(),Duration.ofSeconds(18)));
        audits.set(new ResponsiveLayoutAudit(driver()));
        learn().ready();
        audit("Học tập - sau đăng nhập");
        Assert.assertEquals(learn().getTestTopic().count,5,"Kịch bản 09 yêu cầu Topic Test có 5 từ");
        System.out.println("[RWD09] USER login once -> Dashboard -> Học tập; device viewport="
            + ((JavascriptExecutor) driver()).executeScript("return innerWidth"));
    }

    private void gotoLearn() {
        // Dùng lại session User, không logout/re-login giữa các TC.
        driver().get(BASE+"dashboard.php");
        new LearnPage(driver()).fromDashboard();
        learn().ready();
        audit("Danh sách chủ đề");
    }

    private void openFlashFromBeginning() {
        gotoLearn(); learn().openTestTopic(); learn().chooseMode("flashcard");
        flash().chooseStartFromBeginning();
        audit("Flashcards - đang học");
    }

    private void openModeUnfinished(String mode) {
        gotoLearn(); learn().openTestTopic(); learn().chooseMode(mode);
        switch(mode) {
            case "flashcard" -> { flash().chooseStartFromBeginning(); Assert.assertEquals(flash().cardIndex()[0],1); }
            case "quiz" -> flash().startQuiz();
            case "match" -> flash().startMatch();
            default -> throw new IllegalArgumentException("Mode="+mode);
        }
        Assert.assertFalse(flash().hasText("Hoàn thành bài học!"),"Chưa bắt đầu đã có Summary");
        audit(mode + " - đang học");
    }

    private String confirmLearn(String source) {
        Assert.assertTrue(flash().inLearn(),"Chưa quay về learn.php sau XÁC NHẬN THOÁT");
        LearnModePage.Topic test = learn().getTestTopic();
        Assert.assertEquals(test.name,"Test");
        audit("Sau xác nhận thoát: " + source);
        return "Source="+source+"; Return="+driver().getCurrentUrl()+"; Topic="+test;
    }

    @FunctionalInterface private interface Case { String perform() throws Exception; }

    private int run(String id,String title,String device,Case work) {
        try {
            checkInSession(id,title,device,()->{
                if(setupError.get()!=null)
                    throw new IllegalStateException("Login/Setup không thành công",setupError.get());
                String actual = work.perform();
                String layout = audit(id + " - " + title);
                return actual + "; " + layout;
            });
            return 0;
        } catch(Throwable failure) {
            System.err.println("[RWD09][FAIL] "+id+" "+device+": "+failure.getMessage());
            return 1;
        }
    }

    @Test(dataProvider="mobileFlash",description="KB-09-001..019: login một lần / viewport")
    public void LEARN_09_FLOW(String device,int w,int h) {
        int failures=0;
        beginSession(w,h);
        try {
            try { setup(); setupError.remove(); }
            catch(Throwable e) { setupError.set(e); System.err.println("[RWD09][SETUP FAIL] "+e); }

            failures += run("KB-09-001", "Kiểm tra tiêu đề đầy đủ trên tất cả card chủ đề", device, this::case001);
            failures += run("KB-09-002", "Chọn card Test và kiểm tra khung 3 chế độ học", device, this::case002);
            failures += run("KB-09-003", "Nhấn Flashcards và kiểm tra hộp lựa chọn cách học", device, this::case003);
            failures += run("KB-09-004", "Bắt đầu Flashcard từ đầu và kiểm tra trạng thái ban đầu", device, this::case004);
            failures += run("KB-09-005", "Kiểm tra nút Nghe phát âm không thay đổi Card", device, this::case005);
            failures += run("KB-09-006", "Kiểm tra lật Flashcard hiển thị nghĩa và ví dụ", device, this::case006);
            failures += run("KB-09-007", "Kiểm tra nút Chưa thuộc chuyển thẻ", device, this::case007);
            failures += run("KB-09-008", "Kiểm tra nút Đã thuộc chuyển thẻ", device, this::case008);
            failures += run("KB-09-009", "Kiểm tra nút Kết thúc bài học và Summary", device, this::case009);
            failures += run("KB-09-010", "Kiểm tra nút Học lại từ đầu ở Summary", device, this::case010);
            failures += run("KB-09-011", "Kiểm tra nút Quay lại danh sách sau khi kết thúc", device, this::case011);
            failures += run("KB-09-012", "Flashcard – mũi tên Quay về khi chưa kết thúc", device, this::case012);
            failures += run("KB-09-013", "Quiz – mũi tên Quay về khi chưa kết thúc", device, this::case013);
            failures += run("KB-09-014", "Match – mũi tên Quay về khi chưa kết thúc", device, this::case014);
            failures += run("KB-09-015", "Flashcard – điều hướng Học tập từ sidebar khi đang học", device, this::case015);
            failures += run("KB-09-016", "Quiz – điều hướng Học tập từ sidebar khi đang làm bài", device, this::case016);
            failures += run("KB-09-017", "Match – điều hướng Học tập từ sidebar khi đang làm bài", device, this::case017);
            failures += run("KB-09-018", "Quiz – Kết thúc bài trước khi chọn đáp án", device, this::case018);
            failures += run("KB-09-019", "Match – Kết thúc bài và kiểm tra Summary", device, this::case019);
        } finally {
            topicPage.remove(); flashPage.remove(); waiter.remove();
            setupError.remove(); audits.remove(); endSession();
        }

        if(failures>0)
            throw new AssertionError(device+" có "+failures+"/19 TC FAIL; xem PWW_AUTO_09_RWD.xlsx");
    }

    private String case001() throws Exception {
        gotoLearn();
        List<WebElement> cards = driver().findElements(By.cssSelector("#categoryGrid .cat-card"));
        Assert.assertFalse(cards.isEmpty(), "Không có card chủ đề");
        List<String> names = new ArrayList<>();

        for (WebElement card : cards) {
            WebElement title = card.findElements(By.cssSelector("h3,.cat-title,.category-title"))
                .stream().findFirst().orElseThrow(() -> new AssertionError("Card không có tiêu đề"));

            ((JavascriptExecutor) driver()).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", title);

            String text = title.getText().trim();
            Assert.assertFalse(text.isBlank(), "Có tiêu đề card rỗng");

            Boolean clipped = (Boolean) ((JavascriptExecutor) driver()).executeScript(
                "let e=arguments[0],s=getComputedStyle(e);return e.scrollWidth>e.clientWidth+2&&"
                + "(s.overflow==='hidden'||s.textOverflow==='ellipsis'||s.whiteSpace==='nowrap');",
                title
            );

            Assert.assertFalse(clipped, "Tiêu đề hiển thị bị cắt: " + text);
            names.add(text);
        }

        LearnModePage.Topic topic = learn().getTestTopic();
        Assert.assertEquals(topic.count, 5, "Số từ chủ đề Test khác 5");
        Assert.assertTrue(names.contains("Test"));

        return "Cards=" + cards.size() + "; Titles=" + names + "; Test=" + topic;
    }

    private String case002() throws Exception {
        gotoLearn();
        learn().openTestTopic();

        Assert.assertTrue(learn().panelVisible());

        for (String m : List.of("flashcard", "quiz", "match"))
            Assert.assertTrue(learn().modeAvailable(m), "Thiếu chế độ: " + m);

        return "Topic=" + learn().getTestTopic() + "; Panel=" + learn().panelText();
    }

    private String case003() throws Exception {
        gotoLearn();
        learn().openTestTopic();
        learn().chooseMode("flashcard");

        webWait().until(d -> flash().hasText("Lựa chọn chế độ học"));

        for (String name : List.of(
                "Học tiếp tục", "Chỉ học những từ chưa thuộc", "Học lại từ đầu"))
            Assert.assertTrue(flash().hasText(name), "Thiếu lựa chọn: " + name);

        return "Hộp lựa chọn Flashcard có đủ 3 nút";
    }

    private String case004() throws Exception {
        openFlashFromBeginning();

        int[] c = flash().cardIndex();
        double p = flash().progress();

        Assert.assertEquals(c[0], 1);
        Assert.assertEquals(c[1], 5);
        Assert.assertEquals(p, 0);

        for (String name : List.of(
                "Flashcards", "Nghe phát âm", "Đã thuộc", "Chưa thuộc"))
            Assert.assertTrue(flash().hasText(name), "Thiếu: " + name);

        return "Card="+c[0]+"/"+c[1]+"; Progress="+p+"%; Header="+flash().header();
    }

    private String case005() throws Exception {
        openFlashFromBeginning();

        int[] b = flash().cardIndex();
        double before = flash().progress();

        flash().audio();

        int[] a = flash().cardIndex();
        double after = flash().progress();

        Assert.assertEquals(a[0], b[0]);
        Assert.assertEquals(after, before);

        return "Card="+b[0]+"->"+a[0]+"; Progress="+before+"->"+after;
    }

    private String case006() throws Exception {
        openFlashFromBeginning();

        int old = flash().current();
        flash().flip();
        String face = flash().backFaceText();

        Assert.assertTrue(face.contains("Ý NGHĨA & VÍ DỤ"));
        Assert.assertTrue(face.length() > "Ý NGHĨA & VÍ DỤ".length()+5);
        Assert.assertEquals(flash().current(), old);

        return "Card="+old+"; Back="+face;
    }

    private String case007() throws Exception {
        openFlashFromBeginning();

        int old = flash().current();
        flash().unlearned();

        webWait().until(d -> flash().current() > old);
        Assert.assertEquals(flash().current(), old+1);

        return "Chưa thuộc Card="+old+"->"+flash().current();
    }

    private String case008() throws Exception {
        openFlashFromBeginning();

        int old = flash().current();
        flash().learned();

        webWait().until(d -> flash().current() > old);
        Assert.assertEquals(flash().current(), old+1);

        return "Đã thuộc Card="+old+"->"+flash().current();
    }

    private String case009() throws Exception {
        openFlashFromBeginning();
        flash().learned();
        flash().finish();

        String summary = flash().summary();

        for (String name : List.of(
                "Hoàn thành bài học!",
                "XP NHẬN ĐƯỢC",
                "TỪ THUỘC",
                "TỪ CHƯA THUỘC"))
            Assert.assertTrue(summary.contains(name), "Thiếu: " + name);

        return "Hoàn thành sớm: " + summary;
    }

    private String case010() throws Exception {
        openFlashFromBeginning();
        flash().finish();
        flash().restart();

        Assert.assertEquals(flash().current(), 1);
        Assert.assertEquals(flash().progress(), 0);

        return "Card="+flash().current()+"/"+flash().total()
            +"; Progress="+flash().progress();
    }

    private String case011() throws Exception {
        openFlashFromBeginning();
        flash().finish();
        flash().backToLearn();

        Assert.assertTrue(flash().inLearn());
        Assert.assertEquals(learn().getTestTopic().name, "Test");

        return "Returned="+driver().getCurrentUrl()
            +"; Topic="+learn().getTestTopic();
    }

    private String case012() throws Exception {
        openModeUnfinished("flashcard");
        flash().backArrow();

        return confirmLearn("Flashcard header arrow + đã xác nhận thoát");
    }

    private String case013() throws Exception {
        openModeUnfinished("quiz");
        flash().backArrow();

        return confirmLearn("Quiz header arrow + đã xác nhận thoát");
    }

    private String case014() throws Exception {
        openModeUnfinished("match");
        flash().backArrow();

        String url = driver().getCurrentUrl();

        Assert.assertTrue(
            url.contains("select_topic.php") && url.contains("type=match"),
            "Match Back Arrow phải về trang chọn chủ đề Match: " + url
        );

        audit("Match - sau thoát bằng mũi tên");

        return "Match Back Arrow + xác nhận thoát; Return=" + url;
    }

    private String case015() throws Exception {
        openModeUnfinished("flashcard");
        flash().clickLearnSidebar();

        return confirmLearn("Flashcard sidebar > Học tập + đã xác nhận thoát");
    }

    private String case016() throws Exception {
        openModeUnfinished("quiz");
        flash().clickLearnSidebar();

        return confirmLearn("Quiz sidebar > Học tập + đã xác nhận thoát");
    }

    private String case017() throws Exception {
        openModeUnfinished("match");
        flash().clickLearnSidebar();

        return confirmLearn("Match sidebar > Học tập + đã xác nhận thoát");
    }

    private String case018() throws Exception {
        openModeUnfinished("quiz");
        flash().clickText("Kết thúc bài học");

        webWait().until(
            d -> flash().hasText("Chúc mừng bạn đã hoàn thành bài học!")
        );

        String summary = driver().findElement(By.tagName("body")).getText();

        Assert.assertTrue(summary.contains("TRẢ LỜI ĐÚNG"));
        Assert.assertTrue(summary.contains("TRẢ LỜI SAI"));

        return "Quiz kết thúc khi chưa chọn đáp án: " + summary;
    }

    private String case019() throws Exception {
        openModeUnfinished("match");

        Assert.assertTrue(flash().hasText("Đã ghép 0 /"));

        String result = flash().finishMatchAndReadResult();

        Assert.assertTrue(
            result.contains("CÂU ĐÚNG") && result.contains("CÂU SAI"),
            "Match không hiển thị hai chỉ số kết quả ĐÚNG/SAI"
        );

        for (String name : List.of(
                "Học lại các từ đã sai ngay",
                "Làm bài tập mới",
                "Chọn chủ đề khác"))
            Assert.assertTrue(result.contains(name), "Thiếu nút: " + name);

        return "Kết quả Match: " + result;
    }
}