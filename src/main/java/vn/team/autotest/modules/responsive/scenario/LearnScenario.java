package vn.team.autotest.modules.responsive.scenario;

import java.util.*;
import java.util.stream.Collectors;
import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.*;
import vn.team.autotest.modules.responsive.base.BaseScenario;
import vn.team.autotest.modules.responsive.page.DashboardPage;
import vn.team.autotest.modules.responsive.page.LearnPage;
import vn.team.autotest.modules.responsive.page.LearnPage.Topic;

/** PWW_AUTO_07_RWD: one USER login per viewport; 13 checks per viewport, true data-driven search. */
public final class LearnScenario extends BaseScenario {
    private final ThreadLocal<LearnPage> pages=new ThreadLocal<>();
    private final ThreadLocal<List<Topic>> baselines=new ThreadLocal<>();
    private final ThreadLocal<Throwable> loginFailure=new ThreadLocal<>();
    @Override protected String reportFileName(){return "PWW_AUTO_07_RWD.xlsx";}
    @Override protected String reportSheetName(){return "LEARN_07_RWD";}
    @DataProvider(name="learningMobile", parallel=true)
    public Object[][] devices(){return mobile();}
    private LearnPage learn(){return Objects.requireNonNull(pages.get(),"LearnPage chưa khởi tạo");}
    private List<Topic> baseline(){return Objects.requireNonNull(baselines.get(),"Baseline chưa khởi tạo");}
    private String config(String prop,String env) {
        String value=System.getProperty(prop,"");
        return value.isBlank()?Objects.toString(System.getenv(env),"").trim():value.trim();
    }
    @FunctionalInterface private interface Step {String execute() throws Exception;}
    private int step(int id,String title,String device,Step test){
        try {
            checkInSession(String.format("PWW_AUTO_07_RWD_TC%02d",id),title,device,()->{
                if(loginFailure.get()!=null) throw new IllegalStateException("Không thể kiểm thử do thiết lập đăng nhập/học tập thất bại",loginFailure.get());
                if(!learn().url().contains("learn.php")) {
                    driver().get(BASE+"learn.php");
                    learn().ready();
                }
                learn().reset();
                return test.execute();
            });
            return 0;
        } catch(Throwable e) {
            System.err.println("[RWD07][TC FAIL] "+title+" | "+e.getMessage());
            return 1;
        }
    }
    private void setup() {
        String username=config("test.username","PWW_USER");
        String password=config("test.password","PWW_PASSWORD");
        if(username.isBlank()||password.isBlank())
            throw new IllegalStateException("Thiếu User credentials: -Dtest.username / -Dtest.password hoặc PWW_USER / PWW_PASSWORD");
        DashboardPage dashboard=new DashboardPage(driver());
        dashboard.login(username,password,false); // Home -> Login -> User dashboard
        dashboard.requireViewport();
        LearnPage page=new LearnPage(driver());
        page.fromDashboard(); // Click real Dashboard navigation, not direct URL.
        pages.set(page);
        page.search("");
        List<Topic> baseline=page.cards();
        if(baseline.isEmpty()) throw new AssertionError("Danh sách chủ đề trước khi tìm kiếm đang rỗng; không thể tạo expected độc lập");
        baselines.set(List.copyOf(baseline));
        System.out.println("[RWD07][LOGIN ONCE] USER -> Dashboard -> Learn | baseline="+baseline);
    }
    private static String normalized(String s){return s==null?"":s.replace('\u00a0',' ').replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);}
    private static List<String> canonical(List<Topic> topics){
        List<String> r=topics.stream().map(t->normalized(t.name)).sorted().collect(Collectors.toList());
        return r;
    }
    private List<Topic> expectedSearch(String query){
        return baseline().stream().filter(t->normalized(t.name).contains(normalized(query))).collect(Collectors.toList());
    }
    private String searchCase(String keyword,String expectedTopic,Integer expectedCount){
        learn().search(keyword);
        List<Topic> actual=learn().cards();
        List<Topic> expected=expectedSearch(keyword);
        Assert.assertEquals(canonical(actual),canonical(expected),"Tìm kiếm '"+keyword+"' phải lọc đúng baseline");
        if(expectedTopic!=null&&!expectedTopic.isBlank())
            Assert.assertTrue(canonical(actual).contains(normalized(expectedTopic)),"Thiếu chủ đề mong đợi: "+expectedTopic);
        if(expectedCount!=null) Assert.assertEquals(actual.size(),expectedCount.intValue(),"Sai số card từ nguồn dữ liệu");
        if(actual.isEmpty()) Assert.assertTrue(learn().noResults()||learn().cards().isEmpty(),"Không có card nhưng trạng thái rỗng không hợp lệ");
        return "Keyword="+keyword+"; Expected="+canonical(expected)+"; Actual="+canonical(actual)+"; Count="+actual.size();
    }
    /**
     * Dữ liệu kiểm thử khai báo trực tiếp trong Java, không phụ thuộc CSV / XLSX.
     * Các tên chủ đề và số lượng lấy từ log chạy WordWise ngày 01/11/2026.
     * TC04 và TC05 vẫn là các TC tìm kiếm hiện có, không thêm testcase.
     */
    private record SearchInput(String keyword, String expectedTopic, int expectedCount) {}

    private String runInlineSearchCases(String caseName, List<SearchInput> data) {
        List<String> results = new ArrayList<>();
        for (SearchInput input : data) {
            results.add(searchCase(
                input.keyword(), input.expectedTopic(), input.expectedCount()
            ));
        }
        return caseName + " | cases=" + results.size()
            + "; " + String.join(" | ", results);
    }

    // TC04: tìm chủ đề hợp lệ + tìm chủ đề không tồn tại.
    private String inlinePositiveNegative() {
        return runInlineSearchCases("Inline positive/negative", List.of(
            new SearchInput("Medical", "Medical_Terms", 2),
            new SearchInput("TOEIC", "TOEIC Environment", 1),
            new SearchInput("khong_co_chu_de_987654", "", 0)
        ));
    }

    // TC05: tìm kiếm có nhiều kết quả, một kết quả và không phân biệt hoa/thường.
    private String inlinePositiveBoundary() {
        return runInlineSearchCases("Inline positive/boundary", List.of(
            new SearchInput("Travel", "Travel & Adventure", 2),
            new SearchInput("Technology", "Advanced Technology", 1),
            new SearchInput("hEaLtH", "Health & Fitness", 1)
        ));
    }

    private String optionsAndSelect(String id) {
        List<String> options=learn().options(id);
        Assert.assertTrue(options.size()>=2,id+" phải có nhiều lựa chọn");
        for(String option:options){
            String value=option.split("=",2)[0];
            if(value.isBlank())continue;
            learn().choose(id,value);
            Assert.assertEquals(learn().selected(id),value);
        }
        return id+" options="+options;
    }
    private String colorFilter(){
        List<Topic> original=baseline();
        for(String option:learn().options("colorFilter")){
            String color=option.split("=",2)[0];
            if(color.isBlank())continue;
            learn().choose("colorFilter",color);
            List<Topic> actual=learn().cards();
            List<Topic> expected=original.stream()
                .filter(t->color.equals("all") || normalized(t.color).equals(normalized(color)))
                .collect(Collectors.toList());
            Assert.assertEquals(canonical(actual),canonical(expected),"Màu "+color+" lọc không khớp baseline");
        }
        return "Color choices="+learn().options("colorFilter");
    }
    private String range(String min,String max){
        learn().setNumber("minWords",min);
        learn().setNumber("maxWords",max);
        int lower=min.isEmpty()?0:Integer.parseInt(min),upper=max.isEmpty()?Integer.MAX_VALUE:Integer.parseInt(max);
        List<Topic> expected=baseline().stream().filter(t->t.total>=lower&&t.total<=upper).collect(Collectors.toList());
        Assert.assertEquals(canonical(learn().cards()),canonical(expected),"Range "+min+".."+max+" không khớp baseline");
        return "Range="+min+".."+max+"; count="+learn().cards().size();
    }
    @Test(dataProvider="learningMobile",description="PWW_AUTO_07_RWD - login once, 13 separate report cases")
    public void LEARN_07_FLOW(String device,int w,int h){
        int fails=0;
        beginSession(w,h);
        try {
            try {setup();loginFailure.remove();}
            catch(Throwable e){loginFailure.set(e);System.err.println("[RWD07][SETUP FAIL] "+e.getMessage());}
            fails+=step(1,"Dashboard → Học tập: URL, tiêu đề, mô tả",device,()->{
                Assert.assertTrue(learn().url().contains("learn.php"));
                Assert.assertTrue(normalized(learn().heading()).contains("chọn một chủ đề"),"H1 không đúng: "+learn().heading());
                Assert.assertTrue(normalized(learn().visibleText()).contains("chọn một chủ đề, sau đó chọn chế độ chơi để bắt đầu học"),"Thiếu mô tả Học tập");
                return "URL="+learn().url()+"; H1="+learn().heading();
            });
            fails+=step(2,"Tìm kiếm Health (positive)",device,()->searchCase("Health","Health & Fitness",null));
            fails+=step(3,"DataProvider: Health / Travel / Technology",device,()->{
                StringJoiner j=new StringJoiner(" | ");
                for(String q:List.of("Health","Travel","Technology"))j.add(searchCase(q,"",null));
                return j.toString();
            });
            fails+=step(4,"Dữ liệu trực tiếp Java: tìm kiếm hợp lệ / không tồn tại",device,()->inlinePositiveNegative());
            fails+=step(5,"Dữ liệu trực tiếp Java: nhiều kết quả / hoa thường",device,()->inlinePositiveBoundary());
            fails+=step(6,"Negative: keyword không tồn tại và phục hồi",device,()->{
                String s=searchCase("abcdef_xyz_123","",0);
                learn().search("");
                Assert.assertEquals(canonical(learn().cards()),canonical(baseline()),"Xóa search không khôi phục đủ card");
                return s+"; recovery="+learn().cards().size();
            });
            fails+=step(7,"Boundary: chữ hoa thường / một ký tự / rỗng",device,()->{
                List<String> checks=new ArrayList<>();
                checks.add(searchCase("hEaLtH","",null));
                checks.add(searchCase("a","",null));
                checks.add(searchCase("","",baseline().size()));
                return String.join(" | ",checks);
            });
            fails+=step(8,"Sort: gần nhất / cũ nhất",device,()->optionsAndSelect("sortOrder"));
            fails+=step(9,"Filter: tất cả / tím / hồng / xanh lá / chàm / cam",device,()->colorFilter());
            fails+=step(10,"Range: số từ vựng tối thiểu - tối đa",device,()->range("0","10")+"; "+range("10","14"));
            fails+=step(11,"Cards: tên chủ đề, số từ, tiến độ",device,()->{
                for(Topic t:learn().cards()){
                    Assert.assertFalse(t.name.isBlank(),"Card thiếu tên");
                    Assert.assertFalse(t.id.isBlank(),"Card thiếu data-id: "+t.name);
                    Assert.assertTrue(t.total>=0,"Card thiếu số từ: "+t.name);
                }
                int progress=learn().visibleCardsWithProgress();
                Assert.assertEquals(progress,learn().cards().size(),"Card thiếu nội dung tiến độ / số từ vựng");
                return "Cards="+learn().cards().size()+"; progress/words="+progress;
            });
            fails+=step(12,"Chọn một chủ đề → panel Flashcards / Quiz / Match",device,()->{
                String name=learn().openFirstTopicModes();
                return "Topic="+name+"; inline learning modes=Flashcards,Quiz,Match";
            });
            fails+=step(13,"Responsive: viewport, search, filter, card và overflow",device,()->{
                Assert.assertTrue(learn().viewportWidth()<=500,"Viewport không mobile");
                learn().assertNoOverflow();
                WebElement search=driver().findElement(By.id("searchQuery"));
                Assert.assertTrue(learn().fits(search),"Ô tìm kiếm bị cắt ngang");
                for(String id:List.of("sortOrder","colorFilter","minWords","maxWords"))
                    Assert.assertTrue(driver().findElement(By.id(id)).isDisplayed(),"Filter thiếu: "+id);
                return "Viewport="+learn().viewportWidth()+"; overflow="+learn().overflow();
            });
        } finally {
            pages.remove();baselines.remove();loginFailure.remove();endSession();
        }
        if(fails>0) throw new AssertionError("RWD07 "+device+": "+fails+"/13 TC FAIL. Xem PWW_AUTO_07_RWD.xlsx");
    }
}