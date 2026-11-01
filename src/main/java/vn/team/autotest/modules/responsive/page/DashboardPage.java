package vn.team.autotest.modules.responsive.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** Page Object: locators grounded in the supplied desktop DashboardScenario1/2. */
public class DashboardPage {
    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final WebDriverWait wait;
    private static final String ROOT = "https://learnenglish.plt.pro.vn/";

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor)driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(16));
    }
    public WebElement visible(By by) {
        return wait.until(d -> {
            for (WebElement e : d.findElements(by)) {
                try { if(e.isDisplayed()) return e; }
                catch(StaleElementReferenceException ignored) { }
            }
            return null;
        });
    }
    public List<WebElement> shown(By by) {
        List<WebElement> out = new ArrayList<>();
        for(WebElement e : driver.findElements(by)) {
            try { if(e.isDisplayed()) out.add(e); } catch(StaleElementReferenceException ignored) { }
        }
        return out;
    }
    public void mark(WebElement element, String label) {
        // No scrollIntoView here for fixed mobile hamburger: scrolling caused interception.
        js.executeScript("const e=arguments[0];document.querySelectorAll('[data-rwd-mark]').forEach(x=>{"
            +"x.style.outline=x.dataset.oldOutline||'';x.removeAttribute('data-rwd-mark');});"
            +"e.dataset.oldOutline=e.style.outline||'';e.style.outline='3px solid red';"
            +"e.dataset.rwdMark=arguments[1];",element,label);
        System.out.println("[RWD06] "+label);
    }
    public void click(WebElement element, String label) {
        mark(element,label);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException intercepted) {
            // SVG/icon ở giữa Hamburger có thể nhận hit-test: click vào đúng button cha.
            js.executeScript("arguments[0].click()", element);
        }
    }
    public void openHome() {
        driver.get(ROOT);
        visible(By.tagName("body"));
        wait.until(d -> "complete".equals(js.executeScript("return document.readyState")));
    }
    public String url() { return driver.getCurrentUrl(); }
    public String body() { return driver.findElement(By.tagName("body")).getText(); }
    public void openDashboard() { driver.get(ROOT+"dashboard.php"); ready(); }
    public void ready() {
        wait.until(d -> d.getCurrentUrl().contains("dashboard.php"));
        visible(By.tagName("h1"));
        System.out.println("[RWD06][DASHBOARD] " + url()
            + "; H1=" + visible(By.tagName("h1")).getText().replaceAll("\\s+", " "));
    }
    // Dashboard (sau đăng nhập) dùng #mobile-menu-btn, khác menu Trang chủ.
    public WebElement menuButton() {
        return visible(By.cssSelector("#mobile-menu-btn,button.mobile-hamburger-btn"));
    }

    private static final By MOBILE_SIDEBAR = By.cssSelector(
        "#sidebar, #mobile-sidebar, aside.sidebar, aside[class*='sidebar']"
    );

    public WebElement sidebar() {
        return wait.until(d -> {
            for (WebElement e : d.findElements(MOBILE_SIDEBAR)) {
                if (e.getRect().getWidth() > 0) return e;
            }
            return null;
        });
    }

    /**
     * Kiểm tra trạng thái thực sự của panel, không dựa vào aside bất kỳ.
     * Những sidebar có transform:translateX(-100%) có thể còn display:block,
     * nhưng không được tính là đang mở.
     */
    public boolean sidebarOpen() {
        return Boolean.TRUE.equals(js.executeScript("""
            const panels = Array.from(document.querySelectorAll(
                '#sidebar, #mobile-sidebar, aside.sidebar, aside[class*="sidebar"]'
            ));
            const shown = el => {
                const s = getComputedStyle(el), r = el.getBoundingClientRect();
                if (s.display === 'none' || s.visibility === 'hidden' ||
                    +s.opacity === 0 || r.width <= 40 || r.height <= 40) return false;
                // Panel bị đẩy hoàn toàn ra ngoài màn hình là đóng.
                if (r.right <= 8 || r.left >= innerWidth - 8) return false;
                // Ít nhất một nửa bề ngang panel phải nằm trong viewport.
                const inside = Math.max(0, Math.min(r.right, innerWidth) - Math.max(r.left, 0));
                return inside >= Math.min(40, r.width * .5);
            };
            return panels.some(shown);
            """));
    }

    /** Thông tin chẩn đoán; không in token, cookie hoặc mật khẩu. */
    public String sidebarDiagnostics() {
        return String.valueOf(js.executeScript("""
            return Array.from(document.querySelectorAll(
                '#sidebar, #mobile-sidebar, aside.sidebar, aside[class*="sidebar"]'
            )).map(e => {
                const r=e.getBoundingClientRect(), c=getComputedStyle(e);
                return {tag:e.tagName, id:e.id, className:String(e.className).slice(0,120),
                    left:Math.round(r.left), right:Math.round(r.right),
                    width:Math.round(r.width), display:c.display,
                    visibility:c.visibility, transform:c.transform,
                    ariaHidden:e.getAttribute('aria-hidden')};
            });
            """));
    }

    private void clickToggle(String label) {
        WebElement toggle = menuButton();
        try {
            click(toggle, label);
        } catch (ElementNotInteractableException | TimeoutException ex) {
            // Dùng DOM click trên đúng button, không click một phần tử tùy ý.
            js.executeScript("arguments[0].click()", toggle);
        }
    }

    /** Thử đóng bằng nút X trong panel hoặc backdrop, nếu UI có hỗ trợ. */
    private boolean closeViaDedicatedControl() {
        By closeButton = By.cssSelector(
            "#sidebar button[aria-label*='Đóng'],"+
            "#sidebar button[aria-label*='Close'],"+
            "#sidebar button[onclick*='close'],"+
            "#mobile-sidebar button[aria-label*='Đóng'],"+
            "aside.sidebar button[aria-label*='Đóng'],"+
            "aside.sidebar button[aria-label*='Close']"
        );
        for (WebElement button : shown(closeButton)) {
            try {
                click(button, "Đóng Sidebar qua nút X");
                if (waitUntilSidebar(false, 2)) return true;
            } catch (WebDriverException ignored) { }
        }
        By backdrops = By.cssSelector(
            "#sidebar-overlay, #mobile-menu-overlay, .sidebar-overlay, " +
            ".mobile-menu-overlay, [data-sidebar-backdrop]"
        );
        for (WebElement backdrop : shown(backdrops)) {
            try {
                click(backdrop, "Đóng Sidebar qua overlay");
                if (waitUntilSidebar(false, 2)) return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    private boolean waitUntilSidebar(boolean expected, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(d -> sidebarOpen() == expected);
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public void setSidebar(boolean open) {
        if (sidebarOpen() == open) return;
        if (open) {
            clickToggle("Mở Sidebar Dashboard");
            if (waitUntilSidebar(true, 5)) return;
        } else {
            // Trên mobile, lớp overlay có thể chặn nút hamburger khi menu đang mở.
            if (closeViaDedicatedControl()) return;
            clickToggle("Đóng Sidebar Dashboard");
            if (waitUntilSidebar(false, 5)) return;
            // Nếu nút toggle không đóng được, thử Esc (một số giao diện hỗ trợ).
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                    .sendKeys(Keys.ESCAPE).perform();
                if (waitUntilSidebar(false, 2)) return;
            } catch (WebDriverException ignored) { }
        }
        throw new AssertionError(
            "Sidebar Dashboard không đạt trạng thái " + (open ? "MỞ" : "ĐÓNG")
            + "; actualOpen=" + sidebarOpen()
            + "; DOM=" + sidebarDiagnostics()
            + "; URL=" + url()
            + ". Nếu nút không đóng được bằng thao tác thực tế, đây là lỗi website."
        );
    }
    /**
     * Luồng đã kiểm chứng ở PWW_AUTO_01_RWD:
     * Trang chủ -> Hamburger của HomePage -> Đăng nhập.
     * Không dùng locator #mobile-menu-btn của Dashboard trên Trang chủ.
     */
    private WebElement homeLoginLink() {
        By locator = By.xpath(
            "//a[contains(@href,'login.php') or contains(@href,'/login') "
          + "or normalize-space(.)='Đăng nhập' "
          + "or .//*[normalize-space(.)='Đăng nhập']]"
        );
        List<WebElement> links = shown(locator);
        for (WebElement item : links) {
            String href = String.valueOf(item.getDomAttribute("href"));
            if (href.contains("login")) return item;
        }
        return links.isEmpty() ? null : links.get(0);
    }

    private void openLoginFromHome() {
        openHome();
        System.out.println("[RWD06][LOGIN 1] Trang chủ: " + url());

        WebElement loginLink = homeLoginLink();
        if (loginLink == null) {
            // Dùng lại Hamburger/nhận diện menu của kịch bản Home đã PASS.
            HomePage home = new HomePage(driver);
            System.out.println("[RWD06][LOGIN 2] Mở Hamburger Trang chủ");
            try {
                home.toggleMenu(true);
            } catch (RuntimeException | AssertionError e) {
                throw new AssertionError("Không mở được Hamburger Trang chủ. "
                    + "URL=" + url() + "; links=" + home.visibleHeaderLinks(), e);
            }
            try {
                wait.until(d -> homeLoginLink() != null);
            } catch (TimeoutException e) {
                throw new AssertionError("Đã mở Hamburger nhưng không thấy liên kết Đăng nhập. "
                    + "URL=" + url() + "; links=" + home.visibleHeaderLinks(), e);
            }
            loginLink = homeLoginLink();
        }

        String before = url();
        click(loginLink, "Trang chủ → Đăng nhập");
        try {
            wait.until(d -> !shown(By.cssSelector("input[type='password']")).isEmpty());
        } catch (TimeoutException e) {
            throw new AssertionError("Nhấn Đăng nhập nhưng form mật khẩu không xuất hiện. "
                + "Trước=" + before + "; Sau=" + url(), e);
        }
        System.out.println("[RWD06][LOGIN 3] Form đăng nhập: " + url());
    }

    public void login(String username,String password,boolean admin) {
        openLoginFromHome();
        WebElement u=visible(By.cssSelector("input[name='username'],input[name='email'],"
            +"input[type='email'],input[autocomplete='username'],input[type='text']"));
        WebElement p=visible(By.cssSelector("input[type='password']"));
        u.clear();u.sendKeys(username);p.clear();p.sendKeys(password);
        click(visible(By.cssSelector("button[type='submit'],input[type='submit']")),"Xác nhận đăng nhập");
        try {wait.until(d -> d.getCurrentUrl().contains("dashboard.php") || d.getCurrentUrl().contains("admin.php"));}
        catch(TimeoutException ex) {throw new AssertionError("Đăng nhập không vào Admin/Dashboard; URL="+url()
            +"; body="+body().replaceAll("\\s+"," ").substring(0,Math.min(220,body().length())),ex);}
        if(admin) {
            // Exactly as Desktop: authenticated Admin -> admin.php -> click dashboard link.
            if(!url().contains("admin.php")) driver.get(ROOT+"admin.php");
            wait.until(d -> d.getCurrentUrl().contains("admin.php"));
            By dashboardBy=By.cssSelector("a.nav-item[href='dashboard.php'][title='Dashboard'],"
               +"a.nav-item[href*='dashboard.php'],a[href='dashboard.php']");
            List<WebElement> links=shown(dashboardBy);
            if(links.isEmpty()) {
                List<WebElement> toggles=shown(By.cssSelector(
                    "#mobile-menu-btn,.mobile-hamburger-btn,button[aria-label*='menu'],"
                    +"button[aria-label*='Menu'],button[aria-controls*='sidebar']"));
                if(toggles.isEmpty()) throw new AssertionError(
                    "Admin sidebar đang ẩn và không có Hamburger; URL="+url());
                click(toggles.get(0),"Admin → mở Sidebar");
                try {wait.until(d -> !shown(dashboardBy).isEmpty());}
                catch(TimeoutException ex) {throw new AssertionError(
                    "Admin Sidebar đã mở nhưng không có liên kết Dashboard; URL="+url(),ex);}
                links=shown(dashboardBy);
            }
            click(links.get(0),"Admin Sidebar → Dashboard");
        }
        ready();
        String h1=visible(By.tagName("h1")).getText();
        if(admin && !h1.contains("Siêu Cấp Quản Trị Viên"))
            throw new AssertionError("Đã vào Dashboard nhưng không phải SuperAdmin; H1="+h1);
        if(!admin && h1.contains("Siêu Cấp Quản Trị Viên"))
            throw new AssertionError("Đăng nhập User nhưng nhận Dashboard SuperAdmin; H1="+h1);
    }
    public WebElement card(String heading) {
        return visible(By.xpath("//div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')]"
            +"[.//*[self::h2 or self::h3 or self::h4 or self::div][contains(normalize-space(.),'"+heading+"')]]"));
    }
    public WebElement adminStat(String label) {
        return visible(By.xpath("//div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')]"
            +"[.//div[normalize-space(.)='"+label+"']]"));
    }
    public int adminStatValue(String label) {
        WebElement card=adminStat(label);mark(card,label);
        WebElement key=card.findElement(By.xpath(".//div[normalize-space(.)='"+label+"']"));
        String value=key.findElement(By.xpath("following-sibling::div[1]")).getText().replaceAll("[^0-9]", "");
        if(value.isEmpty()) throw new AssertionError("Không đọc được số liệu: "+label);
        return Integer.parseInt(value);
    }
    public Map<String,Object> chart(String id) {
        WebElement canvas=visible(By.id(id));mark(canvas,"Biểu đồ "+id);
        Object value=js.executeScript("const el=document.getElementById(arguments[0]);"
            +"let c=el&&window.Chart&&Chart.getChart?Chart.getChart(el):null;"
            +"if(!c&&window.Chart&&Chart.instances)for(const k in Chart.instances){"
            +"if(Chart.instances[k].canvas===el)c=Chart.instances[k];}"
            +"return c?{found:true,labels:c.data.labels||[],datasets:c.data.datasets.map(d=>d.data||[]),type:c.config.type}:"
            +"{found:false,labels:[],datasets:[],type:''};",id);
        @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)value;
        return result;
    }
    public void checkChart(String id,int minimumLabels) {
        Map<String,Object> chart=chart(id);
        if(!Boolean.TRUE.equals(chart.get("found")))throw new AssertionError("Chart.js không tạo instance: "+id);
        List<?> labels=(List<?>)chart.get("labels");
        if(labels.size()<minimumLabels)throw new AssertionError("Biểu đồ "+id+" chỉ có "+labels.size()+" nhãn");
        List<?> datasets=(List<?>)chart.get("datasets");
        if(datasets.isEmpty())throw new AssertionError("Biểu đồ "+id+" không có dataset");
        if(datasets.stream().noneMatch(x -> x instanceof List<?> values && !values.isEmpty()))
            throw new AssertionError("Biểu đồ "+id+" có dataset nhưng không có dữ liệu");
    }
    public WebElement tab(String label) {
        return visible(By.xpath("//button[contains(concat(' ',normalize-space(@class),' '),' lb-tab ') and normalize-space(.)='"+label+"']"));
    }
    public WebElement leaderboard(String key) { return visible(By.id("tab-"+key)); }
    public int leaderboardCount(String label,String key) {
        click(tab(label),"Bảng xếp hạng: "+label);
        WebElement panel=leaderboard(key);
        wait.until(d->panel.isDisplayed());
        List<WebElement> items=panel.findElements(By.cssSelector(".rank-row"));
        if(items.isEmpty()) items=panel.findElements(By.xpath("./div"));
        int n=0;
        for(WebElement e:items) if(e.isDisplayed()&&!e.getText().isBlank())n++;
        return n;
    }
    public WebElement focusCard() {
        return visible(By.xpath("//div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')]"
           +"[.//*[contains(normalize-space(.),'TỪ VỰNG TIÊU ĐIỂM')]][.//*[@id='wod-word']]"));
    }
    public String focusValue(WebElement card,String id) {
        WebElement e=card.findElement(By.id(id));
        if(!e.isDisplayed()||e.getText().isBlank()) throw new AssertionError("Thiếu hoặc rỗng: #"+id);
        return e.getText().trim();
    }
    public long overflow() {
        return ((Number)js.executeScript("return Math.max(0,Math.max(document.body.scrollWidth,document.documentElement.scrollWidth)-document.documentElement.clientWidth)")).longValue();
    }
    public String overflowDetails() {
        return String.valueOf(js.executeScript("return [...document.body.querySelectorAll('*')].filter(e=>{"
            +"const r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&"
            +"s.display!=='none'&&s.visibility!=='hidden'&&r.right>document.documentElement.clientWidth+3})"
            +".slice(0,6).map(e=>e.tagName+'.'+String(e.className||'').slice(0,60)).join(' | ')"));
    }
    public void assertNoOverflow() {
        long n=overflow();if(n>3)throw new AssertionError("Dashboard tràn ngang "+n+"px: "+overflowDetails());
    }
    public void scrollDown() { js.executeScript("window.scrollBy(0,Math.max(280,innerHeight*.65))"); }
    public long scrollY() { return ((Number)js.executeScript("return Math.round(window.scrollY)")).longValue(); }
    public WebElement href(String fragment) { return visible(By.cssSelector("a[href*='"+fragment+"']")); }
    public void clickHref(String fragment) { click(href(fragment),"Đi tới "+fragment);wait.until(d->d.getCurrentUrl().contains(fragment)); }
    /**
     * Checkbox select-all có thể được ẩn bằng CSS để dùng nhãn custom.
     * Ưu tiên nhãn hiển thị; fallback kích hoạt change bằng click trên input
     * (không sửa thuộc tính checked trực tiếp).
     */
    public void setAllTopicCheckboxes(boolean selected) {
        By allBy = By.id("selectAllTopics");
        By dropdownBy = By.id("topicDropdownMenu");
        if (shown(dropdownBy).isEmpty()) {
            click(visible(By.id("topicDropdownBtn")), "Mở lại bộ lọc chủ đề");
            visible(dropdownBy);
        }
        WebElement input = driver.findElement(allBy);
        if (input.isSelected() == selected) return;

        WebElement target = (WebElement) js.executeScript("""
            const input=arguments[0];
            const label=document.querySelector('label[for="selectAllTopics"]')
                || input.closest('label');
            const candidates=[label, input.parentElement, input];
            return candidates.find(e=>{
                if(!e)return false;
                const r=e.getBoundingClientRect();
                const cs=getComputedStyle(e);
                return r.width>2 && r.height>2 && cs.display!=='none'
                    && cs.visibility!=='hidden';
            }) || null;
            """, input);

        if (target != null && !target.equals(input)) {
            try {
                click(target, selected ? "Chọn tất cả chủ đề" : "Bỏ chọn tất cả chủ đề");
            } catch (ElementNotInteractableException | TimeoutException ex) {
                // Một số checkbox custom chỉ phản hồi sự kiện click của input.
                js.executeScript("arguments[0].click()", input);
            }
        } else if (input.isDisplayed()) {
            click(input, selected ? "Chọn tất cả chủ đề" : "Bỏ chọn tất cả chủ đề");
        } else {
            // Không thay đổi checked trực tiếp: click vẫn kích hoạt handler hiện hữu.
            js.executeScript("arguments[0].click()", input);
        }

        // Nếu click trên phần bao không kích hoạt checkbox, thử event click của input.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(d -> d.findElement(allBy).isSelected() == selected);
        } catch (TimeoutException ex) {
            WebElement updated = driver.findElement(allBy);
            if (updated.isSelected() != selected) {
                js.executeScript("arguments[0].click()", updated);
            }
        }
        wait.until(d -> d.findElement(allBy).isSelected() == selected);
        System.out.println("[RWD06][TOPICS] selectAll=" + selected);
    }

    /** Bắt lỗi nếu nút all thay đổi nhưng danh sách checkbox không đồng bộ. */
    public void assertAllTopicsSelected(boolean selected) {
        wait.until(d -> {
            List<WebElement> items = d.findElements(By.cssSelector(
                "#topicCheckboxList input.topic-checkbox"));
            return !items.isEmpty() && items.stream().allMatch(e -> e.isSelected() == selected);
        });
    }

    public WebElement adminSection(String title) {
        return visible(By.xpath("//h3[normalize-space(.)='"+title+"']/ancestor::div[contains(concat(' ',normalize-space(@class),' '),' glass-card ')][1]"));
    }
    public void requireViewport() {
        long width=((Number)js.executeScript("return document.documentElement.clientWidth")).longValue();
        if(width>500)throw new AssertionError("Không ở viewport mobile: "+width);
    }
}
