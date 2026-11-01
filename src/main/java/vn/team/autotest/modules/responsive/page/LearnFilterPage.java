package vn.team.autotest.modules.responsive.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** Bộ lọc Học tập: thao tác thật với native select/input và đọc snapshot card từ DOM. */
public final class LearnFilterPage {
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final WebDriverWait wait;

    public record Topic(String id, String name, String color, int total) {}

    public LearnFilterPage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    public void ready() {
        wait.until(d -> d.getCurrentUrl().contains("learn.php"));
        for (String id : List.of("sortOrder", "colorFilter", "minWords", "maxWords", "categoryGrid")) {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
        }
    }

    public String selected(String id) {
        return new Select(driver.findElement(By.id(id))).getFirstSelectedOption().getDomAttribute("value");
    }

    public Map<String,String> options(String id) {
        Map<String,String> options = new LinkedHashMap<>();
        for (WebElement opt : new Select(driver.findElement(By.id(id))).getOptions())
            options.put(opt.getDomAttribute("value"), opt.getText().trim());
        return options;
    }

    public void select(String id, String value) {
        WebElement e = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
        new Select(e).selectByValue(value);
        wait.until(d -> value.equals(selected(id)));
        stable();
    }

    public String input(String id) { return driver.findElement(By.id(id)).getDomProperty("value"); }

    public void fill(String id, String value) {
        WebElement e = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
        e.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        e.sendKeys(Keys.BACK_SPACE);
        if (!value.isEmpty()) e.sendKeys(value);
        // Invalid number inputs may reject text; caller checks HTML validity explicitly.
        stable();
    }

    public boolean valid(String id) {
        return Boolean.TRUE.equals(js.executeScript(
            "return document.getElementById(arguments[0]).checkValidity()", id));
    }

    public void reset() {
        select("sortOrder", "newest");
        select("colorFilter", "all");
        fill("minWords", "");
        fill("maxWords", "");
        WebElement q = driver.findElement(By.id("searchQuery"));
        if (!q.getDomProperty("value").isEmpty()) {
            q.sendKeys(Keys.chord(Keys.CONTROL,"a"));
            q.sendKeys(Keys.BACK_SPACE);
        }
        stable();
    }

    @SuppressWarnings("unchecked")
    public List<Topic> cards() {
        List<?> raw = (List<?>) js.executeScript("""
            return [...document.querySelectorAll('#categoryGrid .cat-card')]
              .filter(e => {const s=getComputedStyle(e); return e.getClientRects().length>0 && s.display!=='none' && s.visibility!=='hidden';})
              .map(e => ({id:e.dataset.id||'',
                  name:(e.querySelector('h3,.cat-title,.category-title')?.textContent||'').trim(),
                  color:(e.dataset.color||'').trim().toLowerCase(),
                  total:Number(e.dataset.total??-1)}));
            """);
        List<Topic> result = new ArrayList<>();
        for (Object obj : raw) {
            Map<String,Object> m = (Map<String,Object>)obj;
            result.add(new Topic(String.valueOf(m.get("id")),String.valueOf(m.get("name")),
                String.valueOf(m.get("color")),((Number)m.get("total")).intValue()));
        }
        return result;
    }

    public void stable() {
        final String[] last = {null};
        final int[] repeats = {0};
        WebDriverWait poll = new WebDriverWait(driver,Duration.ofSeconds(6));
        poll.pollingEvery(Duration.ofMillis(140));
        poll.until(d -> {
            String signature = cards().toString();
            repeats[0] = signature.equals(last[0]) ? repeats[0]+1 : 0;
            last[0] = signature;
            return repeats[0] >= 3;
        });
    }

    public boolean noResults() {
        return Boolean.TRUE.equals(js.executeScript("""
            const e=document.getElementById('noResults');
            return !!e && e.getClientRects().length>0 && getComputedStyle(e).display!=='none';
            """));
    }

    public long overflow() {
        return ((Number) js.executeScript("""
            return Math.max(0,Math.max(document.body.scrollWidth,document.documentElement.scrollWidth)
                   -document.documentElement.clientWidth);
            """)).longValue();
    }

    public long viewportWidth() {
        return ((Number)js.executeScript("return document.documentElement.clientWidth")).longValue();
    }
}
