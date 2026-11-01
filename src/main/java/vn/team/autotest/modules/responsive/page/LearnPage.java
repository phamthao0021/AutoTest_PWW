package vn.team.autotest.modules.responsive.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** WordWise Learn responsive POM; CSS names grounded in original Desktop LearnPage/FilterPage. */
public final class LearnPage {
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final WebDriverWait wait;
    private static final By SEARCH = By.id("searchQuery");
    private static final By CARDS = By.cssSelector("#categoryGrid .cat-card");

    public static final class Topic {
        public final String id, name, color;
        public final int total;
        public Topic(String id, String name, String color, int total) {
            this.id=id; this.name=name; this.color=color; this.total=total;
        }
        public String toString() { return name+" [id="+id+", color="+color+", total="+total+"]"; }
    }
    public LearnPage(WebDriver driver) {
        this.driver=driver;
        this.js=(JavascriptExecutor)driver;
        this.wait=new WebDriverWait(driver,Duration.ofSeconds(12));
    }
    public String url() { return driver.getCurrentUrl(); }
    public void ready() {
        wait.until(d -> d.getCurrentUrl().contains("learn.php"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("categoryGrid")));
    }
    public void fromDashboard() {
        if (!driver.getCurrentUrl().contains("dashboard.php"))
            throw new AssertionError("Chưa ở Dashboard trước khi vào Học tập: "+url());
        By link = By.cssSelector("a.nav-item[href='learn.php'][title='Học tập'],a[href='learn.php'],a[href*='/learn.php']");
        WebElement e=visibleOrNull(link);
        if(e==null) {
            By menu=By.cssSelector("#mobile-menu-btn,button.mobile-hamburger-btn");
            WebElement toggle=visibleOrNull(menu);
            if(toggle==null) throw new AssertionError("Không tìm thấy link Học tập hoặc Hamburger trên Dashboard");
            click(toggle);
            e=wait.until(d -> visibleOrNull(link));
        }
        click(e);
        ready();
    }
    private WebElement visibleOrNull(By by) {
        for (WebElement e: driver.findElements(by)) {
            try { if(e.isDisplayed() && e.isEnabled()) return e; }
            catch(StaleElementReferenceException ignored) {}
        }
        return null;
    }
    private void click(WebElement e) {
        js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});",e);
        try { wait.until(ExpectedConditions.elementToBeClickable(e)).click(); }
        catch(ElementClickInterceptedException ex) { js.executeScript("arguments[0].click()",e); }
    }
    public String heading() { return wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1"))).getText().trim(); }
    public String visibleText() { return driver.findElement(By.tagName("body")).getText(); }
    public String inputValue() { return driver.findElement(SEARCH).getDomProperty("value"); }
    public void search(String keyword) {
        WebElement q=wait.until(ExpectedConditions.elementToBeClickable(SEARCH));
        q.sendKeys(Keys.chord(Keys.CONTROL,"a"));
        q.sendKeys(Keys.BACK_SPACE);
        if(!keyword.isEmpty()) q.sendKeys(keyword);
        if(!keyword.equals(inputValue())) throw new AssertionError("Search input expected="+keyword+" actual="+inputValue());
        stableCards();
    }
    public void reset() {
        choose("sortOrder","newest");
        choose("colorFilter","all");
        setNumber("minWords","");
        setNumber("maxWords","");
        search("");
    }
    public void choose(String id,String value) {
        WebElement e=wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
        Select dropdown=new Select(e);
        if(!value.equals(dropdown.getFirstSelectedOption().getDomAttribute("value"))) {
            dropdown.selectByValue(value);
            stableCards();
        }
        if(!value.equals(new Select(driver.findElement(By.id(id))).getFirstSelectedOption().getDomAttribute("value")))
            throw new AssertionError(id+" không chọn được "+value);
    }
    public List<String> options(String id) {
        List<String> values=new ArrayList<>();
        for(WebElement e: new Select(driver.findElement(By.id(id))).getOptions())
            values.add(e.getDomAttribute("value")+"="+e.getText().trim());
        return values;
    }
    public String selected(String id) {
        return new Select(driver.findElement(By.id(id))).getFirstSelectedOption().getDomAttribute("value");
    }
    public String number(String id) { return driver.findElement(By.id(id)).getDomProperty("value"); }
    public void setNumber(String id, String value) {
        WebElement e=wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
        e.sendKeys(Keys.chord(Keys.CONTROL,"a"));
        e.sendKeys(Keys.BACK_SPACE);
        if(!value.isEmpty()) e.sendKeys(value);
        if(!value.equals(number(id))) throw new AssertionError(id+" actual="+number(id)+", expected="+value);
        stableCards();
    }
    public boolean noResults() {
        return Boolean.TRUE.equals(js.executeScript("let e=document.getElementById('noResults');return !!e && e.getClientRects().length>0 && getComputedStyle(e).display!=='none' && getComputedStyle(e).visibility!=='hidden';"));
    }
    @SuppressWarnings("unchecked")
    public List<Topic> cards() {
        Object raw=js.executeScript("""
            return [...document.querySelectorAll('#categoryGrid .cat-card')]
              .filter(e=>e.getClientRects().length>0 && getComputedStyle(e).display!=='none' && getComputedStyle(e).visibility!=='hidden')
              .map(e=>({id:e.dataset.id||'',name:(e.querySelector('h3,.cat-title,.category-title')?.textContent||'').trim(),
                        color:(e.dataset.color||'').trim().toLowerCase(),total:Number(e.dataset.total??-1)}));
        """);
        List<Topic> out=new ArrayList<>();
        for(Object x:(List<?>)raw) {
            Map<String,Object> o=(Map<String,Object>)x;
            out.add(new Topic(String.valueOf(o.get("id")),String.valueOf(o.get("name")),
                String.valueOf(o.get("color")),((Number)o.get("total")).intValue()));
        }
        return out;
    }
    public void stableCards() {
        final String[] prev={null}; final int[] count={0};
        WebDriverWait small=new WebDriverWait(driver,Duration.ofSeconds(6));
        small.pollingEvery(Duration.ofMillis(160));
        small.until(d -> {
            String now=cards().toString()+"|"+noResults();
            count[0]=now.equals(prev[0])?count[0]+1:0;
            prev[0]=now;
            return count[0]>=3;
        });
    }
    public long overflow() {
        return ((Number)js.executeScript("return Math.max(0,Math.max(document.body.scrollWidth,document.documentElement.scrollWidth)-document.documentElement.clientWidth)")).longValue();
    }
    public void assertNoOverflow() { if(overflow()>3) throw new AssertionError("Learn tràn ngang "+overflow()+"px"); }
    public boolean fits(WebElement e) {
        return Boolean.TRUE.equals(js.executeScript("let r=arguments[0].getBoundingClientRect();return r.width>0&&r.left>=-3&&r.right<=innerWidth+3",e));
    }
    public int visibleCardsWithProgress() {
        int n=0;
        for(WebElement card:driver.findElements(CARDS)) {
            if(!card.isDisplayed()) continue;
            String text=card.getText().toLowerCase(Locale.ROOT);
            boolean progress=!card.findElements(By.cssSelector("[role='progressbar'],progress,[style*='width:']")).isEmpty();
            if(progress || text.contains("đã học") || text.contains("từ vựng")) n++;
        }
        return n;
    }
    public String openFirstTopicModes() {
        List<WebElement> list=driver.findElements(CARDS);
        WebElement first=null;
        for(WebElement e:list) if(e.isDisplayed()) {first=e;break;}
        if(first==null) throw new AssertionError("Không có card để mở lựa chọn chế độ");
        String name=first.findElement(By.cssSelector("h3,.cat-title,.category-title")).getText().trim();
        click(first);
        // WordWise thực tế dùng inline panel, không phải modal.
        wait.until(d -> {
            String txt=d.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
            return txt.contains("chọn chế độ học cho chủ đề") && txt.contains("flashcards") && txt.contains("quiz") && txt.contains("match");
        });
        return name;
    }
    public long viewportWidth() {return ((Number)js.executeScript("return document.documentElement.clientWidth")).longValue();}
}
