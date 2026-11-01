package vn.team.autotest.modules.responsive.page;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Trang Learn: chọn card rồi hiển thị khung chế độ học NỘI TUYẾN, không phải modal. */
public class LearnModePage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;
    private final By cards = By.cssSelector("#categoryGrid .cat-card");
    private String selectedTopicId = "";
    private String selectedTopicName = "";

    public static final class Topic {
        public final String id;
        public final String name;
        public final int count;
        public Topic(String id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
        @Override public String toString() {
            return "Topic[id=" + id + ", name=" + name + ", count=" + count + "]";
        }
    }

    public LearnModePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.js = (JavascriptExecutor) driver;
    }
    private static String attr(WebElement el, String name) {
        String s = el.getDomAttribute(name);
        return s == null ? "" : s.trim();
    }
    private static String norm(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
    private static boolean shown(WebElement e) {
        try { return e.isDisplayed(); }
        catch (StaleElementReferenceException ex) { return false; }
    }

    public void ready() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("categoryGrid")));
        wait.until(d -> !d.findElements(cards).isEmpty());
    }
    private Topic read(WebElement card) {
        String id = attr(card, "data-id");
        String name = card.findElements(By.cssSelector("h3,.cat-title,.category-title"))
                .stream().filter(LearnModePage::shown).map(WebElement::getText)
                .findFirst().orElse("").trim();
        String raw = attr(card, "data-total");
        if (id.isEmpty() || !raw.matches("\\d+"))
            throw new IllegalStateException("Card topic sai data-id/data-total: " + name);
        return new Topic(id, name, Integer.parseInt(raw));
    }
    public List<Topic> topics() {
        ready();
        List<Topic> result = new ArrayList<>();
        for (WebElement c : driver.findElements(cards)) result.add(read(c));
        return result;
    }
    public Topic getTestTopic() {
        return topics().stream().filter(t -> "Test".equals(t.name)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Không có chủ đề tên chính xác Test"));
    }
    private WebElement cardById(String id) {
        return wait.until(d -> d.findElements(cards).stream()
                .filter(c -> id.equals(attr(c, "data-id"))).findFirst().orElse(null));
    }

    /** Đầu đề trong ảnh: "Chọn chế độ học cho chủ đề Test:" */
    private Optional<WebElement> heading() {
        By headings = By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4 "
                + "or self::h5 or self::p or self::strong or self::div]"
                + "[contains(normalize-space(.), 'Chọn chế độ học cho chủ đề')]");
        // Lấy node sâu nhất có nội dung gần nhất với chính dòng tiêu đề; tránh div toàn trang.
        WebElement best = null;
        int len = Integer.MAX_VALUE;
        for (WebElement e : driver.findElements(headings)) {
            if (!shown(e)) continue;
            String txt = norm(e.getText());
            if (!txt.contains("Chọn chế độ học cho chủ đề")) continue;
            if (txt.length() < len) { best = e; len = txt.length(); }
        }
        return Optional.ofNullable(best);
    }
    private boolean hasModes(WebElement e) {
        String s = norm(e.getText()).toLowerCase(Locale.ROOT);
        return s.contains("flashcards") && s.contains("quiz") && s.contains("match");
    }
    private Optional<WebElement> inlinePanel() {
        Optional<WebElement> title = heading();
        if (title.isEmpty()) return Optional.empty();
        WebElement current = title.get();
        // Đi từ tiêu đề lên ancestor gần nhất bao trọn 3 lựa chọn.
        for (int i = 0; i < 6; i++) {
            if (shown(current) && hasModes(current)
                    && !"BODY".equalsIgnoreCase(current.getTagName())) {
                return Optional.of(current);
            }
            List<WebElement> parent = current.findElements(By.xpath(".."));
            if (parent.isEmpty()) break;
            current = parent.get(0);
        }
        return Optional.empty();
    }
    public boolean panelVisible() { return inlinePanel().isPresent(); }
    public WebElement panel() {
        try {
            return wait.until(d -> inlinePanel().orElse(null));
        } catch (TimeoutException ex) {
            String title = heading().map(WebElement::getText).orElse("<không có>");
            throw new IllegalStateException("Không thấy KHUNG NỘI TUYẾN chọn chế độ. "
                    + "Heading=" + title + "; URL=" + driver.getCurrentUrl(), ex);
        }
    }
    public String panelText() { return panel().getText().trim(); }
    public String panelTopicId() {
        WebElement p = panel();
        for (String key : List.of("data-topic-id", "data-category-id")) {
            String id = attr(p, key);
            if (!id.isEmpty()) return id;
        }
        // Panel là UI không có data-ID theo ảnh; ID từ card nguồn không được giả làm ID của panel.
        return "";
    }

    public void openTopic(String id) {
        Topic topic = topics().stream().filter(t -> id.equals(t.id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Topic ID=" + id));
        WebElement card = cardById(id);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", card);
        wait.until(ExpectedConditions.elementToBeClickable(card)).click();
        selectedTopicId = topic.id;
        selectedTopicName = topic.name;
        wait.until(d -> {
            Optional<WebElement> title = heading();
            return title.isPresent() && norm(title.get().getText()).contains(topic.name)
                    && inlinePanel().isPresent();
        });
        System.out.println("[LEARN] INLINE PANEL: " + norm(panelText())
                + "; topic=" + topic);
    }
    public void openTestTopic() { openTopic(getTestTopic().id); }

    private static String modeLabel(String mode) {
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "flashcard": case "flashcards": return "Flashcards";
            case "quiz": return "Quiz";
            case "match": return "Match";
            default: throw new IllegalArgumentException("Mode không hỗ trợ: " + mode);
        }
    }
    private Optional<WebElement> modeTile(String mode) {
        WebElement root = panel();
        String label = modeLabel(mode);
        // Dò nhãn cụ thể; thẻ chế độ có thể là div onclick, không chỉ button hoặc a.
        List<WebElement> labels = root.findElements(By.xpath(
                ".//*[normalize-space(.)='" + label + "']"));
        for (WebElement text : labels) {
            if (!shown(text)) continue;
            WebElement tile = (WebElement) js.executeScript(
                "let n=arguments[0],root=arguments[1];"
              + "while(n && n!==root){"
              + " if(n.matches('button,a,[role=button],[onclick]') || "
              + " getComputedStyle(n).cursor==='pointer') return n;"
              + " n=n.parentElement; }"
              + "return arguments[0].parentElement;", text, root);
            if (tile != null && shown(tile) && tile.isEnabled()) return Optional.of(tile);
        }
        return Optional.empty();
    }
    public boolean modeAvailable(String mode) { return modeTile(mode).isPresent(); }
    public void chooseMode(String mode) {
        String oldUrl = driver.getCurrentUrl();
        WebElement tile = modeTile(mode).orElseThrow(() ->
                new NoSuchElementException("Không tìm thấy ô chế độ " + modeLabel(mode)
                        + " trong khung: " + panelText()));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", tile);
        wait.until(ExpectedConditions.elementToBeClickable(tile)).click();
        try {
            wait.until(d -> !d.getCurrentUrl().equals(oldUrl)
                    || !panelVisible()
                    || d.findElement(By.tagName("body")).getText().contains("Lựa chọn chế độ học")
                    || d.findElement(By.tagName("body")).getText().contains("CẤU HÌNH BÀI HỌC QUIZ")
                    || d.findElement(By.tagName("body")).getText().contains("Bài Tập Ghép Từ Vựng"));
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Đã click " + mode
                    + " nhưng chưa thấy điều hướng. URL=" + driver.getCurrentUrl(), ex);
        }
        System.out.println("[LEARN] " + mode + " => " + driver.getCurrentUrl());
    }
    public void closePanel() {
        WebElement root = panel();
        // Nếu có nút đóng riêng.
        for (WebElement e : root.findElements(By.cssSelector("button,[role='button']"))) {
            if (!shown(e) || !e.isEnabled()) continue;
            String s = norm(e.getText()).toLowerCase(Locale.ROOT);
            if (s.equals("đóng") || s.equals("hủy") || s.equals("×")) {
                e.click();
                wait.until(d -> !panelVisible());
                return;
            }
        }
        // UI dạng accordion thường có thể click lại card để thu gọn.
        if (!selectedTopicId.isEmpty()) {
            WebElement card = cardById(selectedTopicId);
            wait.until(ExpectedConditions.elementToBeClickable(card)).click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(d -> !panelVisible());
                return;
            } catch (TimeoutException ex) {
                throw new IllegalStateException("Khung chế độ không có nút Đóng và click lại "
                        + "card không thu gọn. Cần điều chỉnh KB-09-006 theo UI thực tế.", ex);
            }
        }
        throw new IllegalStateException("Không xác định được cách đóng khung nội tuyến.");
    }
    public String destinationContext() {
        String heading = driver.findElements(By.cssSelector("h1,h2,h3,.page-title"))
                .stream().filter(LearnModePage::shown).map(WebElement::getText)
                .filter(s -> !s.isBlank()).findFirst().orElse("");
        return "URL=" + driver.getCurrentUrl() + "; Heading=" + norm(heading);
    }
    public boolean destinationHasTopic(Topic topic) {
        Pattern p = Pattern.compile("(?:[?&])(?:topic_id|category_id|id)="
                + Pattern.quote(topic.id) + "(?:[&#]|$)");
        if (p.matcher(driver.getCurrentUrl()).find()) return true;
        for (WebElement e : driver.findElements(
                By.cssSelector("[data-topic-id],[data-category-id]"))) {
            if (shown(e) && (topic.id.equals(attr(e, "data-topic-id"))
                    || topic.id.equals(attr(e, "data-category-id")))) return true;
        }
        return false; // Không kết luận PASS nếu trang không cung cấp bằng chứng ID.
    }
}
