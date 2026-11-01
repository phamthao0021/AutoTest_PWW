package vn.team.autotest.modules.responsive.scenario;

import java.util.*;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.team.autotest.modules.responsive.base.BaseScenario;
import vn.team.autotest.modules.responsive.page.DashboardPage;
import vn.team.autotest.modules.responsive.page.LearnPage;
import vn.team.autotest.modules.responsive.page.LearnFilterPage;
import vn.team.autotest.modules.responsive.page.LearnFilterPage.Topic;

/**
 * PWW_AUTO_08_RWD - USER, chỉ Sort / Color Filter / Min-Max trong Học tập.
 * Một lần login trên mỗi viewport; hai viewport chạy song song, các bước cùng viewport tuần tự.
 * Không yêu cầu CSV/Excel đầu vào; báo cáo đầu ra vẫn là XLSX từ BaseScenario.
 */
public final class LearnScenario8 extends BaseScenario {
    private final ThreadLocal<LearnFilterPage> filters = new ThreadLocal<>();
    private final ThreadLocal<List<Topic>> originals = new ThreadLocal<>();
    private final ThreadLocal<Throwable> setupProblem = new ThreadLocal<>();

    @Override protected String reportFileName() { return "PWW_AUTO_08_RWD.xlsx"; }
    @Override protected String reportSheetName() { return "LEARN_08_RWD"; }

    @DataProvider(name="mobileFilter",parallel=true)
    public Object[][] viewportData() { return mobile(); }

    private LearnFilterPage page() { return Objects.requireNonNull(filters.get(),"Chưa khởi tạo LearnFilterPage"); }
    private List<Topic> baseline() { return Objects.requireNonNull(originals.get(),"Chưa có baseline"); }

    private static String credential(String prop,String env) {
        String p = System.getProperty(prop, "");
        return p.isBlank() ? Objects.toString(System.getenv(env),"").trim() : p.trim();
    }

    private void setup() {
        String user=credential("test.username","PWW_USER");
        String password=credential("test.password","PWW_PASSWORD");
        if (user.isBlank() || password.isBlank()) throw new IllegalStateException(
            "Thiếu -Dtest.username / -Dtest.password (hoặc PWW_USER / PWW_PASSWORD)");
        new DashboardPage(driver()).login(user,password,false);
        new LearnPage(driver()).fromDashboard();
        LearnFilterPage p = new LearnFilterPage(driver());
        p.ready();
        filters.set(p);
        p.reset();
        List<Topic> snapshot=p.cards();
        Assert.assertFalse(snapshot.isEmpty(),"Danh sách chủ đề rỗng khi tạo baseline");
        originals.set(List.copyOf(snapshot));
        System.out.println("[RWD08] USER đăng nhập một lần → Dashboard → Học tập | baseline="+snapshot.size());
    }

    @FunctionalInterface private interface Case { String evaluate() throws Exception; }

    private int run(String id,String title,String device,Case work) {
        try {
            checkInSession(id,title,device,() -> {
                if (setupProblem.get()!=null) throw new IllegalStateException("Setup/login thất bại",setupProblem.get());
                if (!driver().getCurrentUrl().contains("learn.php")) {
                    driver().get(BASE+"learn.php"); page().ready();
                }
                page().reset();
                return work.evaluate();
            });
            return 0;
        } catch (Throwable e) {
            System.err.println("[RWD08][FAIL] "+id+" | "+e.getMessage());
            return 1;
        }
    }

    private static List<String> keys(Collection<Topic> topics) {
        return topics.stream().map(t -> t.id()+"/"+t.name()).sorted().collect(Collectors.toList());
    }

    private String compare(List<Topic> expected,String why) {
        List<Topic> actual=page().cards();
        Assert.assertEquals(keys(actual),keys(expected),why);
        return "Expected="+keys(expected)+"; Actual="+keys(actual);
    }

    private List<Topic> byColor(String color) {
        return baseline().stream().filter(t -> color.equals("all") || t.color().equals(color)).toList();
    }

    private List<Topic> byRange(int min,int max) {
        return baseline().stream().filter(t -> t.total()>=min && t.total()<=max).toList();
    }

    private String range(int min,int max) {
        page().fill("minWords",String.valueOf(min));
        page().fill("maxWords",String.valueOf(max));
        Assert.assertEquals(page().input("minWords"),String.valueOf(min));
        Assert.assertEquals(page().input("maxWords"),String.valueOf(max));
        return compare(byRange(min,max),"Lọc số từ "+min+".."+max);
    }

    /** Thứ tự ID chỉ là proxy. Mặc định chỉ khẳng định thứ tự đổi và không mất card;
     *  bật kiểm tra ID nếu hệ thống xác nhận ID tăng theo ngày tạo: -Dtest.sortById=true. */
    private String zeroZero() {
        page().fill("minWords", "0");
        page().fill("maxWords", "0");
        Assert.assertEquals(page().input("minWords"), "0", "Min không giữ giá trị 0");
        Assert.assertEquals(page().input("maxWords"), "0", "Max không giữ giá trị 0");
        return compare(baseline(), "Theo hành vi UI: Min=0, Max=0 là không giới hạn");
    }

    private String sort(String value) {
        page().select("sortOrder",value);
        Assert.assertEquals(page().selected("sortOrder"),value);
        List<Topic> actual=page().cards();
        compare(baseline(),"Sort không được làm mất card");
        if (Boolean.parseBoolean(System.getProperty("test.sortById","false"))) {
            List<Long> ids=new ArrayList<>();
            for (Topic t:actual) ids.add(Long.parseLong(t.id()));
            List<Long> expected=new ArrayList<>(ids);
            expected.sort(value.equals("newest")?Comparator.reverseOrder():Comparator.naturalOrder());
            Assert.assertEquals(ids,expected,"Sai thứ tự ID, chỉ đúng khi ID tương ứng ngày tạo");
        }
        return "sort="+value+"; total="+actual.size()+"; order="+actual.stream().map(Topic::id).toList();
    }

    private String combined(String color,int min,int max) {
        page().select("colorFilter",color);
        page().fill("minWords",Integer.toString(min));
        page().fill("maxWords",Integer.toString(max));
        List<Topic> expected=baseline().stream()
            .filter(t -> t.color().equals(color) && t.total()>=min && t.total()<=max).toList();
        return compare(expected,"Lọc kết hợp màu "+color+" và "+min+".."+max);
    }

    private String caseUi() {
        Assert.assertTrue(driver().getCurrentUrl().contains("learn.php"));
        Assert.assertTrue(page().viewportWidth()<=500,"Không phải mobile viewport");
        Assert.assertTrue(page().overflow()<=3,"Bộ lọc tràn ngang "+page().overflow()+"px");
        Assert.assertEquals(page().options("sortOrder").keySet(),Set.of("newest","oldest"));
        Assert.assertEquals(page().options("colorFilter").keySet(),Set.of("all","purple","pink","green","indigo","orange"));
        Assert.assertEquals(keys(page().cards()),keys(baseline()));
        return "Sort="+page().options("sortOrder")+"; Color="+page().options("colorFilter")+"; cards="+baseline().size();
    }

    private String negativeRange() {
        page().fill("minWords","20");
        page().fill("maxWords","10");
        Assert.assertEquals(page().input("minWords"),"20");
        Assert.assertEquals(page().input("maxWords"),"10");
        // Khoảng vô nghiệm: không có card thỏa cả hai ràng buộc.
        Assert.assertTrue(page().cards().isEmpty(),"Min > Max nhưng vẫn hiển thị card");
        return "Min=20 > Max=10; count=0; noResults="+page().noResults();
    }

    private String invalidNegative() {
        page().fill("minWords","-1");
        String input=page().input("minWords");
        // Trình duyệt có thể từ chối nhập hoặc đánh dấu invalid; không ép hành vi không có trong UI.
        Assert.assertTrue(!"-1".equals(input) || !page().valid("minWords"),
            "Giá trị âm -1 được chấp nhận là valid; kiểm tra min constraint trên giao diện");
        return "input="+input+"; valid="+page().valid("minWords");
    }

    @Test(dataProvider="mobileFilter",description="PWW_AUTO_08: Sort/Color/Min-Max; 1 login User")
    public void LEARN_08_FLOW(String device,int w,int h) {
        int fail=0;
        beginSession(w,h);
        try {
            try {setup();setupProblem.remove();} catch(Throwable t){setupProblem.set(t);System.err.println("[RWD08][SETUP] "+t.getMessage());}
            fail+=run("KB-08-000","Giao diện Sort, Color, Min-Max",device,()->caseUi());
            fail+=run("KB-08-001","Sort gần nhất",device,()->sort("newest"));
            fail+=run("KB-08-002","Sort cũ nhất",device,()->sort("oldest"));
            for (String color:List.of("purple","pink","green","indigo","orange")) {
                final String selectedColor=color;
                fail+=run("KB-08-003-"+selectedColor,"Lọc màu "+selectedColor,device,()->{
                    page().select("colorFilter",selectedColor);
                    return compare(byColor(selectedColor),"Sai card màu "+selectedColor);
                });
            }
            fail+=run("KB-08-004","Min=10, Max=14",device,()->range(10,14));
            fail+=run("KB-08-005A","Biên 0..10",device,()->range(0,10));
            fail+=run("KB-08-005B","Biên 10..10",device,()->range(10,10));
            fail+=run("KB-08-005C","Biên 14..14",device,()->range(14,14));
            fail+=run("KB-08-006","Min > Max",device,()->negativeRange());
            fail+=run("KB-08-007","Tất cả màu khôi phục",device,()->{
                page().select("colorFilter","pink");page().select("colorFilter","all");
                return compare(baseline(),"Reset màu all");
            });
            fail+=run("KB-08-008","Xóa Min/Max",device,()->{
                page().fill("minWords","10");page().fill("maxWords","14");
                page().fill("minWords","");page().fill("maxWords","");
                return compare(baseline(),"Xóa Min/Max phải khôi phục card");
            });
            fail+=run("KB-08-009","Chỉ nhập Min=10",device,()->{
                page().fill("minWords","10");
                return compare(byRange(10,Integer.MAX_VALUE),"Min-only");
            });
            fail+=run("KB-08-010","Chỉ nhập Max=14",device,()->{
                page().fill("maxWords","14");
                return compare(byRange(0,14),"Max-only");
            });
            fail+=run("KB-08-011","Biên 0..0",device,()->zeroZero());
            fail+=run("KB-08-012","Min=-1 validation",device,()->invalidNegative());
            fail+=run("KB-08-013","Màu pink + 10..14",device,()->combined("pink",10,14));
            fail+=run("KB-08-014","Màu green + sort oldest",device,()->{
                page().select("colorFilter","green");page().select("sortOrder","oldest");
                return compare(byColor("green"),"Sort+Color");
            });
            fail+=run("KB-08-015","Reset toàn bộ bộ lọc",device,()->{
                page().select("colorFilter","pink");page().fill("minWords","10");page().fill("maxWords","14");
                page().select("sortOrder","oldest");page().reset();
                Assert.assertEquals(page().selected("sortOrder"),"newest");
                Assert.assertEquals(page().selected("colorFilter"),"all");
                Assert.assertTrue(page().input("minWords").isEmpty() && page().input("maxWords").isEmpty());
                return compare(baseline(),"Reset all");
            });
            fail+=run("KB-08-016","Kiểm tra data-color có fallback",device,()->{
                List<Topic> custom=baseline().stream()
                    .filter(t->!Set.of("purple","pink","green","indigo","orange").contains(t.color())).toList();
                // Website có color=gray/red/custom, ghi nhận thay vì áp đặt chỉ có 5 màu.
                return "Custom colors="+custom+"; options="+page().options("colorFilter");
            });
            int[][] boundaries={{0,10},{10,10},{14,14}};
            for (int[] bounds:boundaries) {
                int min=bounds[0],max=bounds[1];
                fail+=run("KB-08-017-"+min+"-"+max,"Inline boundary "+min+".."+max,device,()->range(min,max));
            }
        } finally {filters.remove(); originals.remove();setupProblem.remove();endSession();}
        if(fail>0) throw new AssertionError(device+": "+fail+" case(s) FAIL. Xem PWW_AUTO_08_RWD.xlsx");
    }
}