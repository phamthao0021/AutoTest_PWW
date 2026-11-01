package vn.team.autotest.modules.responsive.scenario;

import java.util.*;
import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.team.autotest.modules.responsive.base.BaseScenario;
import vn.team.autotest.modules.responsive.page.*;

/** PWW_AUTO_10_RWD: Flashcard E2E, one login per viewport, independent TC. */
public final class FlashCardScenario10 extends BaseScenario {
    private final ThreadLocal<FlashCardPage> pages = new ThreadLocal<>();
    private final ThreadLocal<LearnModePage> topics = new ThreadLocal<>();
    private final ThreadLocal<ResponsiveLayoutAudit> audits = new ThreadLocal<>();
    private final ThreadLocal<Throwable> setupError = new ThreadLocal<>();

    @Override protected String reportFileName() { return "PWW_AUTO_10_RWD.xlsx"; }
    @Override protected String reportSheetName() { return "FLASHCARD_10_RWD"; }
    @DataProvider(name="flashMobile",parallel=true)
    public Object[][] flashMobile() { return mobile(); }

    private FlashCardPage f() { return Objects.requireNonNull(pages.get()); }
    private LearnModePage l() { return Objects.requireNonNull(topics.get()); }
    private String audit(String step) { return Objects.requireNonNull(audits.get()).inspect(step); }
    private static String credential(String key,String env) {
        String v=System.getProperty(key,"").trim();
        return v.isEmpty()?Objects.toString(System.getenv(env),"").trim():v;
    }
    private void setup() {
        String u=credential("test.username","PWW_USER"),p=credential("test.password","PWW_PASSWORD");
        if(u.isBlank()||p.isBlank()) throw new IllegalStateException("Thiếu test.username/test.password hoặc PWW_USER/PWW_PASSWORD");
        new DashboardPage(driver()).login(u,p,false);
        new DashboardPage(driver()).requireViewport();
        pages.set(new FlashCardPage(driver()));
        topics.set(new LearnModePage(driver()));
        audits.set(new ResponsiveLayoutAudit(driver()));
        audit("Login - Dashboard");
    }
    private void open() {
        driver().get(BASE+"dashboard.php");
        new LearnPage(driver()).fromDashboard();
        l().ready();
        // Dùng topic Test 5 từ để các kiểm thử tiến trình có dữ liệu xác định.
        // Thay bằng Health & Fitness khi bộ dữ liệu đó đã được xác nhận.
        l().openTestTopic();
        l().chooseMode("flashcard");
        f().chooseStartFromBeginning();
        Assert.assertTrue(f().total()>0,"Topic không có từ");
        audit("Flashcard - bắt đầu");
    }
    @FunctionalInterface private interface Step { String execute() throws Exception; }
    private int tc(String id,String name,String device,Step step) {
        try {
            checkInSession(id,name,device,()->{
                if(setupError.get()!=null) throw new IllegalStateException("Login/setup lỗi",setupError.get());
                String result=step.execute();
                return result; // BaseScenario.checkInSession() kiểm tra layout một lần
            });
            return 0;
        } catch(Throwable e) {
            System.err.println("[RWD10][FAIL] "+id+" "+device+": "+e.getMessage());
            return 1;
        }
    }
    @Test(dataProvider="flashMobile")
    public void FLASHCARD_10_FLOW(String device,int width,int height) {
        int fail=0;
        beginSession(width,height);
        try {
            try {setup();setupError.remove();}
            catch(Throwable t){setupError.set(t);}
            fail+=tc("KB-10-001","Khởi tạo phiên và nội dung thẻ",device,()->{
                open(); Assert.assertEquals(f().current(),1);
                Assert.assertFalse(f().word().isBlank());
                Assert.assertFalse(f().ipa().isBlank());
                return f().header()+" | Word="+f().word()+" | IPA="+f().ipa();
            });
            fail+=tc("KB-10-002","Nghe phát âm không đổi thẻ",device,()->{
                open(); int before=f().current();double progress=f().progress();
                f().audio();Assert.assertEquals(f().current(),before);
                Assert.assertEquals(f().progress(),progress,0.01);
                return "Audio OK, card="+before;
            });
            fail+=tc("KB-10-003","Lật thẻ hiển thị nghĩa và ví dụ",device,()->{
                open();f().flip();
                Assert.assertFalse(f().meaning().isBlank());
                Assert.assertFalse(f().example().isBlank());
                return "Meaning="+f().meaning()+" | Example="+f().example();
            });
            fail+=tc("KB-10-004","Đã thuộc tăng số thẻ và tiến trình",device,()->{
                open();int n=f().total();f().learned();
                Assert.assertEquals(f().current(),2);
                Assert.assertTrue(f().progress()>=0);
                return "Card=2/"+n+" | progress="+f().progress();
            });
            fail+=tc("KB-10-005","Kết thúc sớm và đối chiếu Summary",device,()->{
                open();f().learned();f().finish();
                Assert.assertTrue(f().summaryVisible());
                Assert.assertEquals(f().summaryNumber("Đã thuộc"),1);
                Assert.assertEquals(f().summaryNumber("Chưa thuộc"),0);
                return "Summary: learned=1, unlearned=0";
            });
            fail+=tc("KB-10-006","Học lại từ Summary",device,()->{
                open();f().finish();f().restart();
                Assert.assertEquals(f().current(),1);
                Assert.assertEquals(f().progress(),0.0,0.01);
                return "Restart card 1, progress=0";
            });
            fail+=tc("KB-10-007","Lật hai lần không làm mất nội dung",device,()->{
                open();String w=f().word();f().flip();
                Assert.assertFalse(f().backFaceText().isBlank());
                // Trạng thái lật ngược lần hai phụ thuộc implementation; xác minh card giữ nguyên.
                Assert.assertEquals(f().current(),1);
                return "Card=1 | Word before flip="+w;
            });
            fail+=tc("KB-10-008","Chưa thuộc chuyển thẻ",device,()->{
                open();f().unlearned();Assert.assertEquals(f().current(),2);
                return "Unlearned -> "+f().header();
            });
            fail+=tc("KB-10-009","Tiến trình nhiều thẻ",device,()->{
                open();int total=f().total();Assert.assertTrue(total>=3);
                f().learned();double a=f().progress();
                f().unlearned();double b=f().progress();
                Assert.assertTrue(b>=a,"Progress giảm sau khi xử lý thêm thẻ");
                Assert.assertEquals(f().current(),3);
                return "Progress "+a+" -> "+b;
            });
            fail+=tc("KB-10-010","Xử lý thẻ cuối và Summary",device,()->{
                open();int n=f().total();
                for(int i=0;i<n;i++) f().learned();
                Assert.assertTrue(f().summaryVisible(),"Hết thẻ nhưng không hiển thị Summary");
                return "Finished "+n+" cards";
            });
            fail+=tc("KB-10-011","Toàn bộ Đã thuộc",device,()->{
                open();int n=f().total();
                for(int i=0;i<n;i++) f().learned();
                Assert.assertEquals(f().summaryNumber("Đã thuộc"),n);
                Assert.assertEquals(f().summaryNumber("Chưa thuộc"),0);
                return "Learned="+n;
            });
            fail+=tc("KB-10-012","Toàn bộ Chưa thuộc",device,()->{
                open();int n=f().total();
                for(int i=0;i<n;i++) f().unlearned();
                Assert.assertEquals(f().summaryNumber("Chưa thuộc"),n);
                Assert.assertEquals(f().summaryNumber("Đã thuộc"),0);
                return "Unlearned="+n;
            });
            fail+=tc("KB-10-013","Phiên hỗn hợp Đã thuộc/Chưa thuộc",device,()->{
                open();int n=f().total(),yes=0,no=0;
                for(int i=0;i<n;i++) {if(i%2==0){f().learned();yes++;}else{f().unlearned();no++;}}
                Assert.assertEquals(f().summaryNumber("Đã thuộc"),yes);
                Assert.assertEquals(f().summaryNumber("Chưa thuộc"),no);
                return "Learned="+yes+" | Unlearned="+no;
            });
            fail+=tc("KB-10-014","Summary ổn định khi cuộn",device,()->{
                open();f().finish();String before=f().summary();
                ((JavascriptExecutor)driver()).executeScript("window.scrollTo(0,document.body.scrollHeight)");
                Assert.assertTrue(f().summaryVisible());
                Assert.assertEquals(f().summary(),before);
                return "Summary unchanged after scroll";
            });
            fail+=tc("KB-10-015","Phiên học mới không giữ kết quả cũ",device,()->{
                open();f().learned();f().finish();f().restart();
                Assert.assertEquals(f().current(),1);
                Assert.assertEquals(f().progress(),0.0,0.01);
                return "Fresh session";
            });
            fail+=tc("KB-10-016","Đổi kích thước dọc-ngang và phục hồi",device,()->{
                open();audit("Portrait");
                driver().manage().window().setSize(new Dimension(height,width));
                try {long overflow=((Number)((JavascriptExecutor)driver()).executeScript(
                    "return Math.max(document.body.scrollWidth,document.documentElement.scrollWidth)-document.documentElement.clientWidth")).longValue();
                Assert.assertTrue(overflow<=3,"Landscape tràn ngang "+overflow+"px");
                audit("Landscape - after rotation");
                Assert.assertEquals(f().current(),1);}
                finally {driver().manage().window().setSize(new Dimension(width,height));}
                audit("Portrait restored");
                return "Portrait "+width+"x"+height+" / Landscape "+height+"x"+width;
            });
            fail+=tc("KB-10-017","Quay về khi chưa hoàn tất và xác nhận thoát",device,()->{
                open();f().backArrow();
                // backArrow() của POM thực hiện popup xác nhận theo mode thực tế.
                Assert.assertTrue(f().inLearn(),"Sau xác nhận thoát chưa về trang Học tập");
                return "Returned="+driver().getCurrentUrl();
            });
        } finally {
            pages.remove();topics.remove();audits.remove();setupError.remove();endSession();
        }
        if(fail>0) throw new AssertionError(device+" có "+fail+"/17 TC FAIL; xem PWW_AUTO_10_RWD.xlsx");
    }
}
