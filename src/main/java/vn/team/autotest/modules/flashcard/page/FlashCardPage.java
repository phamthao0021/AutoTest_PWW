package vn.team.autotest.modules.flashcard.page;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** Page Object for WordWise Flashcards. Keeps API used by Scenario 9/10. */
public class FlashCardPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    public FlashCardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        this.wait.pollingEvery(Duration.ofMillis(180));
        this.js = (JavascriptExecutor) driver;
    }

    private static String norm(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private boolean shown(WebElement e) {
        try { return e.isDisplayed(); } catch (StaleElementReferenceException ex) { return false; }
    }

    private String body() { return driver.findElement(By.tagName("body")).getText(); }

    public boolean hasText(String text) { return body().contains(text); }

    /**
     * Viền đỏ dùng riêng cho tự động kiểm thử, tự xóa viền trước đó.
     * Chỉ thay đổi CSS hiển thị; không thay đổi trạng thái học.
     */
    public void highlight(WebElement target, String description) {
        js.executeScript("""
            document.querySelectorAll('[data-pww-test-focus]').forEach(e => {
              e.style.outline = e.getAttribute('data-pww-original-outline') || '';
              e.style.outlineOffset = e.getAttribute('data-pww-original-offset') || '';
              e.removeAttribute('data-pww-test-focus');
              e.removeAttribute('data-pww-original-outline');
              e.removeAttribute('data-pww-original-offset');
            });
            const e = arguments[0];
            e.setAttribute('data-pww-original-outline', e.style.outline);
            e.setAttribute('data-pww-original-offset', e.style.outlineOffset);
            e.setAttribute('data-pww-test-focus', 'true');
            e.style.outline = '4px solid #ef4444';
            e.style.outlineOffset = '4px';
            e.scrollIntoView({block:'center', inline:'nearest'});
            let badge = document.getElementById('pww-test-marker');
            if (!badge) {
                badge = document.createElement('div');
                badge.id = 'pww-test-marker';
                badge.style.cssText = 'position:fixed;right:12px;bottom:12px;z-index:2147483646;'
                    + 'background:#b91c1c;color:#fff;padding:8px 12px;border-radius:8px;'
                    + 'font:600 13px Arial;pointer-events:none;max-width:440px;';
                document.body.appendChild(badge);
            }
            badge.textContent = 'SELENIUM TEST: ' + arguments[1];
            """, target, description);
        System.out.println("[FLASHCARD HIGHLIGHT] " + description);
    }

    /** Hiển thị viền đỏ quanh Flashcard đang được kiểm tra. */
    public void highlightCard(String description) {
        WebElement card = frontOrBackRegion();
        highlight(card, description);
    }

    /** Tìm phần tử theo text bằng MỘT JavaScript call thay vì quét Selenium từng node. */
    private WebElement label(String keyword) {
        return wait.until(d -> (WebElement) js.executeScript("""
            const expected = arguments[0].replace(/\\s+/g, ' ').trim().toLowerCase();
            const elements = document.querySelectorAll('button,a,[role="button"],span,p,div,h1,h2,h3');
            let exact = null, partial = null, partialLen = Number.MAX_SAFE_INTEGER;
            for (const e of elements) {
                if (!e.getClientRects().length || getComputedStyle(e).visibility === 'hidden') continue;
                const t = (e.innerText || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                if (t === expected) { exact = e; break; }
                if (t.includes(expected) && t.length < partialLen) {
                    partial = e; partialLen = t.length;
                }
            }
            return exact || partial;
            """, keyword));
    }

    public void clickText(String text) {
        WebElement e = label(text);
        WebElement target = (WebElement) js.executeScript(
            "let e=arguments[0]; for(let n=e,i=0;n&&i<5;n=n.parentElement,i++){"
            + "if(n.matches('button,a,[role=button]'))return n;}return e;", e);
        highlight(target, "Nhấn: " + text);
        wait.until(ExpectedConditions.elementToBeClickable(target)).click();
    }

    private WebElement frontOrBackRegion() {
        // Trước tiên lấy mặt trước; nếu thẻ đã lật thì dùng nhãn mặt sau.
        WebElement anchor;
        Object visibleAudio = js.executeScript("""
            return [...document.querySelectorAll('button,span,p,div')].find(e =>
              e.getClientRects().length && (e.innerText||'').trim()==='Nghe phát âm') || null;
            """);
        anchor = visibleAudio instanceof WebElement ? (WebElement) visibleAudio : label("Ý NGHĨA & VÍ DỤ");
        return (WebElement) js.executeScript(
            "let n=arguments[0];for(let i=0;n&&i<8;i++,n=n.parentElement){"
            + "let r=n.getBoundingClientRect();if(r.width>280&&r.height>160)return n;}return arguments[0].parentElement;", anchor);
    }

    public void chooseStartFromBeginning() {
        wait.until(d -> hasText("Lựa chọn chế độ học") || hasText("Card 1 of"));
        if (hasText("Lựa chọn chế độ học")) {
            clickText("Học lại từ đầu (Toàn bộ bài)");
        }
        ready();
    }

    public void ready() {
        wait.until(d -> Boolean.TRUE.equals(js.executeScript(
            "return /Card\\s+\\d+\\s+of\\s+\\d+/i.test(document.body.innerText);")));
    }

    public int[] cardIndex() {
        Matcher m = Pattern.compile("Card\\s+(\\d+)\\s+of\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
            .matcher(body());
        if (!m.find()) throw new IllegalStateException("Không tìm thấy Card x of y; URL=" + driver.getCurrentUrl());
        return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
    }

    public String header() { int[] x = cardIndex(); return "Card " + x[0] + " of " + x[1]; }
    public int current() { return cardIndex()[0]; }
    public int total() { return cardIndex()[1]; }

    /** Dùng JS lấy nhãn tiến độ trong một lần truy cập DOM để chạy nhanh hơn. */
    public double progress() {
        WebElement e = (WebElement) js.executeScript("""
            const els = document.querySelectorAll('[role="progressbar"],span,div,p');
            for (const e of els) {
                if (!e.getClientRects().length) continue;
                const t = (e.innerText || '').trim();
                if (/^\\d{1,3}(?:\\.\\d+)?\\s*%$/.test(t)) return e;
                const a = e.getAttribute('aria-valuenow');
                if (a && /^\\d+(?:\\.\\d+)?$/.test(a)) return e;
            }
            return null;
            """);
        if (e == null) throw new IllegalStateException("Không tìm thấy nhãn Progress trên Flashcard");
        String aria = e.getDomAttribute("aria-valuenow");
        String raw = aria != null && !aria.isBlank() ? aria : norm(e.getText()).replace("%", "");
        double value = Double.parseDouble(raw.trim());
        if (value < 0 || value > 100) throw new IllegalStateException("Progress ngoài khoảng 0-100: " + value);
        highlight(e, "Đọc tiến độ " + value + "%");
        return value;
    }

    private WebElement frontRegion() {
        WebElement audio = label("Nghe phát âm");
        return (WebElement) js.executeScript(
            "let n=arguments[0];for(let i=0;n&&i<8;i++,n=n.parentElement){"
            + "let r=n.getBoundingClientRect();if(r.width>280&&r.height>160)return n;}return arguments[0].parentElement;", audio);
    }

    public String word() {
        String[] lines = frontRegion().getText().split("\\R");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().matches("/[^/]{2,100}/")) {
                String candidate = norm(lines[i - 1]);
                if (!candidate.isBlank()) return candidate;
            }
        }
        throw new IllegalStateException("Không đọc được từ phía trước IPA: " + frontRegion().getText());
    }

    public String ipa() {
        Matcher m = Pattern.compile("/[^/\\r\\n]{2,100}/").matcher(frontRegion().getText());
        if (!m.find()) throw new IllegalStateException("Không tìm thấy IPA");
        return m.group();
    }

    public String difficulty() {
        for (WebElement e : driver.findElements(By.cssSelector("span,div,p"))) {
            if (!shown(e)) continue;
            String v = norm(e.getText()).toLowerCase(Locale.ROOT);
            if (List.of("low", "medium", "hard", "easy", "dễ", "vừa", "khó").contains(v)) return v;
        }
        throw new IllegalStateException("Không tìm thấy mức độ từ vựng");
    }

    public void audio() { clickText("Nghe phát âm"); }

    public void flip() {
        WebElement hint = label("Chạm để lật mặt thẻ");
        WebElement face = (WebElement) js.executeScript(
            "let n=arguments[0];for(let i=0;n&&i<8;i++,n=n.parentElement){"
            + "let r=n.getBoundingClientRect();if(r.width>280&&r.height>160)return n;}return null;", hint);
        if (face == null) throw new IllegalStateException("Không xác định được bề mặt thẻ");
        highlight(face, "Lật mặt thẻ Flashcard");
        face.click();
        wait.until(d -> hasText("Ý NGHĨA & VÍ DỤ"));
    }

    public String backFaceText() {
        WebElement heading = label("Ý NGHĨA & VÍ DỤ");
        WebElement face = (WebElement) js.executeScript(
            "let n=arguments[0];for(let i=0;n&&i<8;i++,n=n.parentElement){"
            + "let r=n.getBoundingClientRect();if(r.width>280&&r.height>160)return n;}return arguments[0].parentElement;", heading);
        highlight(face, "Kiểm tra Meaning + Example");
        return norm(face.getText());
    }

    public String meaning() {
        String s = backFaceText();
        Matcher m = Pattern.compile("Ý NGHĨA\\s*&\\s*VÍ DỤ\\s+(.+?)(?=\\s*[\"“]|$)", Pattern.CASE_INSENSITIVE).matcher(s);
        if (m.find() && !m.group(1).isBlank()) return m.group(1).trim();
        throw new IllegalStateException("Không trích được Meaning: " + s);
    }

    public String example() {
        Matcher m = Pattern.compile("[\"“](.+?)[\"”]").matcher(backFaceText());
        if (m.find()) return m.group(1).trim();
        throw new IllegalStateException("Không tìm thấy ví dụ đặt trong dấu ngoặc kép");
    }

    private void selectStatus(String status) {
        int old = current();
        clickText(status);
        wait.until(d -> summaryVisible() || safeCurrent() > old);
    }

    private int safeCurrent() {
        try { return current(); } catch (RuntimeException ex) { return -1; }
    }

    public void learned() { selectStatus("Đã thuộc"); }
    public void unlearned() { selectStatus("Chưa thuộc"); }

    public void finish() {
        clickText("Kết thúc bài học");
        wait.until(d -> summaryVisible());
    }

    public boolean summaryVisible() {
        return hasText("Hoàn thành bài học!");
    }

    public String summary() {
        wait.until(d -> summaryVisible());
        return norm(body());
    }

    public String summaryText() { return summary(); }

    public int summaryNumber(String label) {
        String marker;
        if (label.equalsIgnoreCase("Đã thuộc")) marker = "TỪ THUỘC";
        else if (label.equalsIgnoreCase("Chưa thuộc")) marker = "TỪ CHƯA THUỘC";
        else marker = label;

        WebElement text = label(marker);
        WebElement box = (WebElement) js.executeScript(
            "let n=arguments[0];for(let i=0;n&&i<6;i++,n=n.parentElement){"
            + "let t=(n.innerText||'').trim();if(t.includes(arguments[1])&&t.length<140&&/\\d/.test(t))return n;}"
            + "return arguments[0].parentElement;", text, marker);
        String raw = norm(box.getText());
        Matcher m = Pattern.compile("(?:^|\\s)(\\d+)(?:\\s|$)").matcher(raw);
        if (!m.find()) throw new IllegalStateException("Không tìm thấy số liệu " + marker + ": " + raw);
        return Integer.parseInt(m.group(1));
    }

    public void restart() {
        clickText("Học lại từ đầu");
        wait.until(d -> !summaryVisible() && safeCurrent() == 1);
    }

    // Các test 09-012..017: hành động thoát phải được xác nhận thật sự.
    // Không dùng driver.navigate().back() và không chuyển URL trực tiếp.
    private static final By EXIT_DIALOG = By.cssSelector(
        ".swal2-popup,.swal2-container,.modal.show,[role='alertdialog']," +
        "[role='dialog'],[aria-modal='true'],#exitConfirmModal," +
        "#confirmExitModal,.confirm-dialog,.confirmation-modal,.modal[style*='display']");

    private static String lower(String value) {
        return norm(value).toLowerCase(Locale.ROOT);
    }

    private String metadata(WebElement e) {
        StringBuilder result = new StringBuilder();
        for (String key : List.of("aria-label", "title", "href", "data-bs-target", "onclick")) {
            String value = e.getDomAttribute(key);
            if (value != null) result.append(' ').append(value);
        }
        result.append(' ').append(e.getText());
        return lower(result.toString());
    }

    private boolean isSideNavigation(WebElement e) {
        return Boolean.TRUE.equals(js.executeScript(
            "return !!arguments[0].closest('aside,.sidebar,[class*=sidebar]," +
            "[id*=sidebar],nav[aria-label*=sidebar]');", e));
    }

    private boolean isExitConfirmation(String label) {
        String t = lower(label);
        return t.contains("xác nhận thoát") || t.contains("đồng ý thoát")
            || t.contains("thoát bài") || t.contains("rời khỏi")
            || t.contains("rời trang") || t.contains("thoát khỏi")
            || t.equals("thoát") || t.equals("rời đi")
            || t.equals("xác nhận") || t.equals("đồng ý")
            || t.equals("tiếp tục thoát") || t.equals("ok")
            || t.equals("yes") || t.equals("leave");
    }

    private WebElement exitButtonIn(WebElement dialog) {
        for (WebElement b : dialog.findElements(By.cssSelector(
                "button,[role='button'],a,.swal2-confirm,input[type='button']"))) {
            if (!shown(b) || !b.isEnabled()) continue;
            String value = metadata(b);
            if (value.contains("hủy") || value.contains("ở lại")
                    || value.contains("cancel") || value.contains("không thoát")) continue;
            if (isExitConfirmation(value)) return b;
        }
        return null;
    }

    /** Chờ hộp xác nhận, nhấn nút THOÁT, rồi mới kiểm tra Learn. */
    private void confirmExitAndReturn(String source) {
        final boolean[] confirmed = {false};
        WebDriverWait exitWait = new WebDriverWait(driver, Duration.ofSeconds(8));
        exitWait.pollingEvery(Duration.ofMillis(180));

        try {
            exitWait.until(d -> {
                // window.confirm / alert JavaScript của trình duyệt.
                try {
                    Alert alert = d.switchTo().alert();
                    System.out.println("[SC09 EXIT] " + source
                        + " | Native confirm=" + alert.getText());
                    alert.accept();
                    confirmed[0] = true;
                    return true;
                } catch (NoAlertPresentException ignored) { }

                // HTML modal/SweetAlert/custom dialog xuất hiện sau cú click.
                for (WebElement dialog : d.findElements(EXIT_DIALOG)) {
                    if (!shown(dialog)) continue;
                    WebElement button = exitButtonIn(dialog);
                    if (button == null) continue;
                    System.out.println("[SC09 EXIT] " + source
                        + " | Dialog=" + norm(dialog.getText())
                        + " | Confirm=" + norm(button.getText()));
                    highlight(button, "XÁC NHẬN THOÁT: " + source);
                    button.click();
                    confirmed[0] = true;
                    return true;
                }
                return false;
            });
        } catch (TimeoutException ex) {
            // Không giả PASS nếu chưa xác nhận thoát, kể cả khi website đã chuyển trang.
            throw new IllegalStateException(
                "Chưa tìm thấy nút XÁC NHẬN THOÁT sau " + source
                + ". URL=" + driver.getCurrentUrl()
                + ". Hãy kiểm tra HTML dialog / native confirm.", ex);
        }

        if (!confirmed[0]) throw new AssertionError("Chưa thực hiện xác nhận thoát: " + source);
        wait.until(d -> inLearn());
        System.out.println("[SC09 EXIT] CONFIRMED => " + driver.getCurrentUrl());
    }

    /** Chỉ click mũi tên Quay về trong vùng header của chế độ học. */
    public void backArrow() {
        WebElement back = wait.until(d -> {
            List<WebElement> all = d.findElements(By.cssSelector(
                "main a,main button,.main-content a,.main-content button," +
                "header a,header button,.content a,.content button," +
                "a,button,[role='button']"));
            WebElement iconCandidate = null;
            for (WebElement e : all) {
                if (!shown(e) || !e.isEnabled() || isSideNavigation(e)) continue;
                String meta = metadata(e);
                // Nhấn đúng nút Back, không nhầm menu học tập hoặc nút kết thúc.
                if (meta.contains("quay về") || meta.contains("quay lại")
                        || meta.contains("trở về") || meta.contains("back")
                        || meta.contains("arrow-left") || meta.contains("chevron-left")) {
                    if (meta.contains("kết thúc") || meta.contains("đóng phiên")) continue;
                    return e;
                }
                Boolean icon = (Boolean) js.executeScript(
                    "let e=arguments[0],r=e.getBoundingClientRect();" +
                    "let s=(e.innerText||'').trim();" +
                    "let hasArrow=!!e.querySelector(" +
                    "'[class*=arrow-left],[class*=chevron-left],.fa-arrow-left,'+" +
                    "'[class*=bi-arrow-left],[data-lucide=arrow-left],svg');" +
                    "return r.width>15&&r.width<95&&r.height>15&&r.height<95" +
                    "&&r.top>=0&&r.top<220&&(hasArrow||s==='←'||s==='‹');", e);
                if (Boolean.TRUE.equals(icon) && iconCandidate == null) iconCandidate = e;
            }
            return iconCandidate;
        });
        highlight(back, "Mũi tên Quay về khi đang học");
        back.click();
        confirmExitAndReturn("Back Arrow");
    }

    /** Click đúng mục Học tập trong sidebar, rồi xác nhận thoát phiên. */
    public void clickLearnSidebar() {
        WebElement link = wait.until(d -> {
            for (WebElement e : d.findElements(By.cssSelector(
                "aside a,aside button,.sidebar a,.sidebar button," +
                "[class*='sidebar'] a,[class*='sidebar'] button," +
                "[id*='sidebar'] a,[id*='sidebar'] button," +
                "nav a,nav button"))) {
                if (!shown(e) || !e.isEnabled()) continue;
                String text = lower(e.getText());
                String href = lower(e.getDomAttribute("href"));
                if (text.equals("học tập") || text.equals("học từ vựng")
                        || href.endsWith("/learn.php") || href.equals("learn.php"))
                    return e;
            }
            return null;
        });
        highlight(link, "Sidebar > Học tập (khi đang học)");
        link.click();
        confirmExitAndReturn("Sidebar Học tập");
    }

    public boolean inLearn() {
        return driver.getCurrentUrl().contains("learn.php")
            && !driver.findElements(By.id("categoryGrid")).isEmpty();
    }

    public void backToLearn() {
        clickText("Quay lại danh sách");
        wait.until(d -> inLearn());
    }

    // These setup operations are used by Scenario 9 when the same Page Object is shared.
    public void startQuiz() {
        wait.until(d -> hasText("CẤU HÌNH BÀI HỌC QUIZ"));
        clickText("BẮT ĐẦU HỌC NGAY");
        wait.until(d -> hasText("Question 1 / ") || hasText("Question 1/") || hasText("QUIZ · TEST"));
    }

    public void startMatch() {
        wait.until(d -> hasText("Bài Tập Ghép Từ Vựng"));
        clickText("Bắt đầu làm bài tập");
        wait.until(d -> hasText("Đã ghép 0 /") || hasText("Đã ghép 0/") || hasText("BÀI TẬP GHÉP CẶP"));
    }
}
