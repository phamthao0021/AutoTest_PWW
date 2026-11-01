package vn.team.autotest.modules.responsive.page;

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

    // KB-09-012..017: bắt buộc click nút xác nhận trong popup thực tế.
    // Flashcard: "Thoát ngay"; Quiz / Match: "Xác nhận thoát".
    // Không can thiệp window.location hoặc driver.navigate().back().
    private static String normalized(String raw) {
        return raw == null ? "" : raw.replace('\u00a0', ' ')
            .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private boolean insideSidebar(WebElement element) {
        return Boolean.TRUE.equals(js.executeScript(
            "return !!arguments[0].closest('aside,.sidebar,[class*=sidebar],[id*=sidebar],nav[aria-label*=sidebar]');",
            element));
    }

    private String exitButtonsDiagnostics() {
        return String.valueOf(js.executeScript("""
            return [...document.querySelectorAll('button,a,[role="button"]')]
              .filter(e => e.getClientRects().length > 0)
              .map(e => ({tag:e.tagName, id:e.id,
                text:(e.innerText||e.textContent||'').trim().slice(0,100),
                className:String(e.className).slice(0,60)}))
              .filter(e => /thoát|ở lại|xác nhận|quay|back|học tập|kết thúc/i
                .test(e.text+' '+e.id+' '+e.className)).slice(0,30);
        """));
    }

    /** Lọc nút XÁC NHẬN nằm trong popup, không chọn nút "Ở lại". */
    private WebElement popupConfirmButton() {
        return (WebElement) js.executeScript("""
            const path = location.pathname.toLowerCase();
            const expected = path.includes('flashcard.php')
                ? ['thoát ngay','xác nhận thoát']
                : ['xác nhận thoát','thoát ngay'];
            const norm = s => (s||'').replace(/\\s+/g,' ').trim().toLowerCase();
            const shown = e => {
                if (!e || !e.isConnected) return false;
                const r=e.getBoundingClientRect(), s=getComputedStyle(e);
                return r.width>0 && r.height>0 && s.display!=='none'
                   && s.visibility!=='hidden' && +s.opacity!==0;
            };
            const containers = [...document.querySelectorAll(
              '.swal2-popup,.swal2-container,.modal.show,[role="alertdialog"],'
              +'[role="dialog"],[aria-modal="true"],#exitConfirmModal,'
              +'#confirmExitModal,.confirm-dialog,.confirmation-modal')].filter(shown);
            const buttons = [...document.querySelectorAll(
              'button,a,[role="button"],input[type="button"],input[type="submit"]')]
              .filter(shown);
            for (const label of expected) {
              for (const b of buttons) {
                const t=norm(b.innerText||b.textContent||b.value||b.getAttribute('aria-label'));
                if (t!==label || b.disabled) continue;
                if (/ở lại|hủy|huỷ|không thoát|cancel/i.test(t)) continue;
                const inContainer = containers.some(p=>p.contains(b));
                let inCustomPopup = false;
                for(let p=b.parentElement,i=0;p&&i<9&&p!==document.body;p=p.parentElement,i++) {
                   if (!shown(p)) continue;
                   const content=norm(p.innerText||p.textContent);
                   if (content.length>1800) continue;
                   const hasQuestion=/xác nhận thoát|thoát bài quiz|thoát bài làm|bạn có chắc.*thoát/.test(content);
                   const hasStay=[...p.querySelectorAll('button,a,[role="button"]')]
                     .some(x=>shown(x)&&/^(ở lại|hủy|huỷ|cancel)/.test(norm(x.innerText||x.textContent)));
                   if(hasQuestion&&hasStay){ inCustomPopup=true; break; }
                }
                if(inContainer || inCustomPopup) return b;
              }
            }
            return null;
        """);
    }

    private void confirmExitAndReturn(String source) {
        final boolean[] confirmed = {false};
        WebDriverWait confirmWait = new WebDriverWait(driver, Duration.ofSeconds(12));
        confirmWait.pollingEvery(Duration.ofMillis(200));
        try {
            confirmWait.until(d -> {
                try {
                    Alert alert = d.switchTo().alert();
                    String text = alert.getText();
                    System.out.println("[RWD09 EXIT] " + source + " | Native alert=" + text);
                    alert.accept();
                    confirmed[0] = true;
                    return true;
                } catch (NoAlertPresentException ignored) { }

                WebElement button = popupConfirmButton();
                if (button == null) return false;
                String text = norm(button.getText());
                System.out.println("[RWD09 EXIT] " + source + " | Popup button=" + text);
                highlight(button, "Popup -> " + text);
                try {
                    button.click();
                } catch (ElementNotInteractableException ex) {
                    js.executeScript("arguments[0].click();", button);
                }
                confirmed[0] = true;
                return true;
            });
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Không thấy nút Thoát ngay/Xác nhận thoát trong popup sau "
                + source + "; URL=" + driver.getCurrentUrl()
                + "; các nút=" + exitButtonsDiagnostics(), ex);
        }
        if (!confirmed[0]) throw new AssertionError("Chưa xác nhận thoát: " + source);
        try {
            wait.until(d -> inLearn());
        } catch (TimeoutException ex) {
            throw new AssertionError("Đã xác nhận thoát nhưng chưa về learn.php: "
                + driver.getCurrentUrl(), ex);
        }
        System.out.println("[RWD09 EXIT PASS] " + source + " -> " + driver.getCurrentUrl());
    }

    private String elementMetadata(WebElement element) {
        StringBuilder b = new StringBuilder();
        for (String attr : List.of("aria-label", "title", "href", "onclick", "class")) {
            String value = element.getDomAttribute(attr);
            if (value != null) b.append(' ').append(value);
        }
        b.append(' ').append(element.getText());
        return normalized(b.toString());
    }

    /** Click nút mũi tên Quay về của chế độ học, không dùng browser Back. */
    public void backArrow() {
        WebElement arrow = wait.until(d -> {
            WebElement fallback = null;
            for (WebElement e : d.findElements(By.cssSelector("header a,header button,main a,main button,a,button"))) {
                if (!shown(e) || !e.isEnabled() || insideSidebar(e)) continue;
                String meta = elementMetadata(e);
                if (meta.contains("kết thúc") || meta.contains("đăng xuất")) continue;
                if (meta.contains("quay về") || meta.contains("trở về")
                    || meta.contains("arrow-left") || meta.contains("chevron-left")
                    || meta.contains("back") || meta.contains("quay lại")) {
                    return e;
                }
                if (fallback == null && Boolean.TRUE.equals(js.executeScript(
                    "const e=arguments[0],r=e.getBoundingClientRect();"
                    + "return r.top>=0&&r.top<220&&r.width>15&&r.width<95"
                    + "&&r.height>15&&r.height<95"
                    + "&&!!e.querySelector('[class*=arrow-left],[class*=chevron-left],.fa-arrow-left');", e))) {
                    fallback = e;
                }
            }
            return fallback;
        });
        highlight(arrow, "Mũi tên Quay về");
        try {
            arrow.click();
        } catch (UnhandledAlertException ignored) {
            // window.confirm được xử lý trong confirmExitAndReturn
        }
        confirmExitAndReturn("Back Arrow");
    }

    /** Với mobile, mở hamburger TRƯỚC, sau đó mới click Học tập trên Sidebar. */
    public void clickLearnSidebar() {
        By learnLinks = By.cssSelector(
            "aside a,aside button,.sidebar a,.sidebar button,"
            + "[class*='sidebar'] a,[class*='sidebar'] button,"
            + "[id*='sidebar'] a,[id*='sidebar'] button,nav a,nav button");
        java.util.function.Supplier<WebElement> visibleLearn = () -> {
            for (WebElement e : driver.findElements(learnLinks)) {
                if (!shown(e) || !e.isEnabled()) continue;
                String t = normalized(e.getText());
                String h = normalized(e.getDomAttribute("href"));
                if (t.equals("học tập") || t.equals("học từ vựng")
                    || h.endsWith("/learn.php") || h.equals("learn.php")) return e;
            }
            return null;
        };
        WebElement link = visibleLearn.get();
        if (link == null) {
            By hamburgers = By.cssSelector(
                "#mobile-menu-btn,button.mobile-hamburger-btn,"
                + "button[aria-label*='menu'],button[aria-label*='Menu'],"
                + "button[aria-controls*='sidebar']");
            WebElement toggle = null;
            for (WebElement b : driver.findElements(hamburgers)) {
                if (shown(b) && b.isEnabled()) {toggle=b;break;}
            }
            if (toggle == null) throw new AssertionError(
                "Sidebar mobile ẩn nhưng không tìm thấy Hamburger. URL=" + driver.getCurrentUrl());
            highlight(toggle, "Mở Sidebar mobile");
            toggle.click();
            link = wait.until(d -> visibleLearn.get());
        }
        highlight(link, "Sidebar -> Học tập");
        try {
            link.click();
        } catch (UnhandledAlertException ignored) { }
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

public String finishMatchAndReadResult() {
    String[] labels = {
        "Hoàn thành bài tập",
        "Kết thúc bài tập",
        "Kết thúc bài học"
    };

    WebDriverWait shortWait = new WebDriverWait(
        driver, Duration.ofSeconds(6)
    );

    WebElement button = shortWait.until(d -> {
        for (String label : labels) {
            for (WebElement e : d.findElements(
                By.xpath(
                    "//button[normalize-space(.)='" + label + "']"
                    + " | //a[normalize-space(.)='" + label + "']"
                )
            )) {
                if (e.isDisplayed() && e.isEnabled()) {
                    return e;
                }
            }
        }
        return null;
    });

    button.click();

    wait.until(d -> {
        String text = d.findElement(By.tagName("body")).getText();
        return text.contains("CÂU ĐÚNG")
            && text.contains("CÂU SAI");
    });

    return driver.findElement(By.tagName("body")).getText();
}

}
