package vn.team.autotest.modules.flashcard.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** Page Object for WordWise Quiz and Match; no new helper class required. */
public class QuizMatchPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    private final By result = By.cssSelector(
        "#quizResult,#matchResult,#results,.quiz-result,.match-result,.result-panel,"
        + ".summary-panel,#summary,.quiz-summary,.match-summary,[role='dialog']");
    private final By questions = By.cssSelector(
        "#question,.quiz-question,.question-text,[data-question-id],.question-card");
    private final By options = By.cssSelector(
        "label.quiz-option,label.answer-option,.option-item,.answer-option,"
        + "[data-option-id],input[type='radio'] + label");
    private final By matchItems = By.cssSelector(
        "[data-pair-id],[data-match-id],[data-pair]");

    public QuizMatchPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.js = (JavascriptExecutor) driver;
    }

    private static String norm(String s) { return s == null ? "" : s.replaceAll("\\s+", " ").trim(); }
    private boolean visible(WebElement e) {
        try { return e.isDisplayed(); } catch (StaleElementReferenceException ex) { return false; }
    }
    private String body() { return driver.findElement(By.tagName("body")).getText(); }
    private boolean contains(String s) { return body().contains(s); }
    private String read(By by) {
        return driver.findElements(by).stream().filter(this::visible)
            .map(WebElement::getText).map(QuizMatchPage::norm)
            .filter(v -> !v.isBlank()).findFirst().orElse("");
    }

    private Optional<WebElement> findAction(String... names) {
        for (WebElement e : driver.findElements(By.cssSelector("button,a,[role='button']"))) {
            if (!visible(e) || !e.isEnabled()) continue;
            String text = norm(e.getText()).toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (text.contains(name.toLowerCase(Locale.ROOT))) return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private void clickAction(String... names) {
        WebElement button = wait.until(d -> findAction(names).orElse(null));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", button);
        wait.until(ExpectedConditions.elementToBeClickable(button)).click();
    }

    public String title() {
        String heading = read(By.cssSelector("h1,h2,.page-title,.quiz-title,.match-title"));
        if (!heading.isBlank()) return heading;
        return read(By.cssSelector("header,.game-header"));
    }

    public String question() {
        String q = read(questions);
        if (!q.isBlank()) return q;
        // Real Quiz screenshot shows: 'Question 1 / 4' and 'Chọn định nghĩa đúng:'.
        q = read(By.cssSelector("h3,h4,.font-bold"));
        return q;
    }

    public int answerOptionsCount() {
        long explicit = driver.findElements(options).stream().filter(this::visible).count();
        if (explicit > 0) return (int) explicit;
        // Quiz screenshot has A/B/C/D answer blocks without predictable CSS.
        return (int) driver.findElements(By.xpath(
            "//*[self::button or self::label or @role='button'][.//*[normalize-space()='A' or normalize-space()='B' or normalize-space()='C' or normalize-space()='D']]")
        ).stream().filter(this::visible).count();
    }

    public int matchItemsCount() {
        long n = driver.findElements(matchItems).stream().filter(this::visible).count();
        if (n > 0) return (int) n;
        // When data-pair-id is absent do not fabricate pair counts.
        return 0;
    }

    public boolean resultVisible() {
        if (contains("Chúc mừng bạn đã hoàn thành bài học!")
                || contains("CÁC TỪ CHƯA CHÍNH XÁC CẦN HỌC LẠI")) return true;
        for (WebElement e : driver.findElements(result)) {
            if (!visible(e)) continue;
            String t = e.getText().toLowerCase(Locale.ROOT);
            if (t.contains("trả lời đúng") || t.contains("câu đúng")
                || t.contains("hoàn thành bài học") || t.contains("tổng điểm đạt được")) return true;
        }
        return false;
    }

    public String resultText() {
        wait.until(d -> resultVisible());
        String panel = read(result);
        return panel.isBlank() ? norm(body()) : panel;
    }

    public void quizReady() {
        wait.until(d -> contains("Question 1") || !question().isBlank() || resultVisible());
    }

    /** Handles quiz configuration shown in the user's screenshot. */
    public void startQuiz() {
        if (contains("CẤU HÌNH BÀI HỌC QUIZ")) clickAction("BẮT ĐẦU HỌC NGAY");
        quizReady();
    }

    /** Handles match configuration shown in the user's screenshot. */
    public void startMatch() {
        if (contains("Bài Tập Ghép Từ Vựng")) clickAction("Bắt đầu làm bài tập");
        wait.until(d -> contains("Đã ghép") || resultVisible());
    }

    private List<WebElement> quizOptions() {
        List<WebElement> result = driver.findElements(options).stream()
            .filter(this::visible).filter(WebElement::isEnabled).toList();
        if (!result.isEmpty()) return result;
        return driver.findElements(By.xpath(
            "//*[self::button or self::label or @role='button'][.//*[normalize-space()='A' or normalize-space()='B' or normalize-space()='C' or normalize-space()='D']]")
        ).stream().filter(this::visible).filter(WebElement::isEnabled).toList();
    }

    /** Workflow test, not an assertion that the chosen answer is correct. */
    public int completeQuiz(int maxQuestions) {
        startQuiz();
        int answered = 0;
        for (int i = 0; i < maxQuestions && !resultVisible(); i++) {
            String before = body();
            List<WebElement> choices = quizOptions();
            if (choices.isEmpty()) throw new IllegalStateException(
                "Quiz: Không tìm thấy phương án câu " + (i + 1) + "; kiểm tra DOM đáp án");
            choices.get(0).click();
            answered++;
            if (resultVisible()) break;
            Optional<WebElement> next = findAction("tiếp theo", "câu tiếp", "next", "xác nhận", "kiểm tra");
            if (next.isPresent()) next.get().click();
            else {
                Optional<WebElement> finish = findAction("kết thúc bài học", "nộp bài", "hoàn thành");
                if (finish.isPresent()) { finish.get().click(); confirmSubmission(); }
                else if (body().equals(before)) throw new IllegalStateException(
                    "Quiz không chuyển câu, không có nút Tiếp theo/Kết thúc");
            }
        }
        if (!resultVisible()) {
            clickAction("kết thúc bài học", "nộp bài", "hoàn thành");
            confirmSubmission();
        }
        wait.until(d -> resultVisible());
        return answered;
    }

    private void confirmSubmission() {
        for (WebElement e : driver.findElements(By.cssSelector(".modal.show,.swal2-popup,[role='dialog']"))) {
            if (!visible(e)) continue;
            for (WebElement b : e.findElements(By.cssSelector("button"))) {
                if (!visible(b) || !b.isEnabled()) continue;
                String text = norm(b.getText()).toLowerCase(Locale.ROOT);
                if (text.contains("xác nhận") || text.contains("đồng ý")
                    || text.contains("nộp") || text.equals("ok")) { b.click(); return; }
            }
        }
    }

    private String pairId(WebElement e) {
        for (String attr : List.of("data-pair-id", "data-match-id", "data-pair")) {
            String value = e.getDomAttribute(attr);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    /** Does not guess pair mappings when the DOM lacks stable pair IDs. */
    public int completeMatch(int maxPairs) {
        startMatch();
        Map<String, List<WebElement>> groups = new LinkedHashMap<>();
        for (WebElement e : driver.findElements(matchItems)) {
            if (!visible(e)) continue;
            String id = pairId(e);
            if (!id.isBlank()) groups.computeIfAbsent(id, k -> new ArrayList<>()).add(e);
        }
        if (groups.isEmpty()) throw new IllegalStateException(
            "Match chưa có data-pair-id/data-match-id. Cần HTML DOM hoặc dữ liệu đáp án chuẩn; không ghép tùy ý.");
        int count = 0;
        for (Map.Entry<String, List<WebElement>> entry : groups.entrySet()) {
            if (count >= maxPairs || resultVisible()) break;
            if (entry.getValue().size() != 2) throw new IllegalStateException(
                "Match pair=" + entry.getKey() + " có " + entry.getValue().size() + " phần tử, cần 2");
            for (WebElement e : entry.getValue()) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", e);
                wait.until(ExpectedConditions.elementToBeClickable(e)).click();
            }
            count++;
        }
        if (!resultVisible()) clickAction("Hoàn thành bài tập", "Kết thúc bài học");
        wait.until(d -> resultVisible());
        return count;
    }

    /** Tests the actual UI Back control, never browser history fallback. */
    public void backToLearn() {
        clickAction("Quay lại danh sách", "Chọn chủ đề khác", "Quay lại");
        wait.until(d -> d.getCurrentUrl().contains("learn.php")
            && !d.findElements(By.id("categoryGrid")).isEmpty());
    }
}
