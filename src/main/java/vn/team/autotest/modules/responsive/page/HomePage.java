
package vn.team.autotest.modules.responsive.page;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class HomePage {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final WebDriverWait wait;

    private static final By HAMBURGER = By.cssSelector(
        "button[aria-label*='enu'],"
        + "button[aria-controls],"
        + "button.navbar-toggler,"
        + "#menuToggle,.hamburger,.mobile-menu-toggle"
    );

    private static final By BRAND =
        By.cssSelector("header a,nav a,.navbar-brand");

    private static final By CTA = By.xpath(
        "//a[contains(.,'Bắt đầu học miễn phí')"
        + " or contains(.,'Đăng ký ngay')"
        + " or contains(.,'Học thử')"
        + " or contains(.,'Học Ngay')"
        + " or contains(.,'Khám Phá')]"
        + "|//button[contains(.,'Bắt đầu học miễn phí')"
        + " or contains(.,'Học Ngay')]"
    );

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(
            driver, Duration.ofSeconds(12)
        );
        this.wait.pollingEvery(
            Duration.ofMillis(200)
        );
    }

    public void open(String url) {
        driver.get(url);
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.tagName("body")
            )
        );
    }

    public WebElement firstVisible(By locator) {
        return wait.until(d ->
            d.findElements(locator).stream()
                .filter(e -> {
                    try {
                        return e.isDisplayed();
                    } catch (StaleElementReferenceException x) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null)
        );
    }

    public String text() {
        return driver.findElement(
            By.tagName("body")
        ).getText();
    }

    public WebElement hero() {
        return firstVisible(By.cssSelector("h1"));
    }

    public WebElement header() {
        return firstVisible(
            By.cssSelector("header,nav,.navbar")
        );
    }

    public WebElement hamburger() {
        List<WebElement> buttons =
            driver.findElements(HAMBURGER);

        for (WebElement e : buttons) {
            if (e.isDisplayed() && e.isEnabled()) {
                return e;
            }
        }

        WebElement found = (WebElement)
            js.executeScript("""
                const buttons=[
                    ...document.querySelectorAll(
                        'header button,nav button,button'
                    )
                ];
                return buttons.find(e=>{
                    const r=e.getBoundingClientRect();
                    const s=getComputedStyle(e);
                    return r.width>=20
                        &&r.height>=20
                        &&r.top>=0
                        &&r.top<120
                        &&r.right>innerWidth*0.65
                        &&r.right<=innerWidth+5
                        &&s.display!=='none'
                        &&s.visibility!=='hidden'
                        &&(
                            e.querySelector('svg')
                            ||e.querySelector('i')
                            ||e.innerText.trim()===''
                        );
                })||null;
            """);

        if (found != null) {
            return found;
        }

        throw new org.openqa.selenium.NoSuchElementException(
            "Không tìm được nút Hamburger "
            + "ở góc phải Header"
        );
    }

    public WebElement cta() {
        return firstVisible(CTA);
    }

    public WebElement brand() {
        return firstVisible(BRAND);
    }

    public List<WebElement> visible(By by) {
        return driver.findElements(by).stream()
            .filter(WebElement::isDisplayed)
            .collect(Collectors.toList());
    }

    public void mark(WebElement e, String title) {
        js.executeScript(
            "document.querySelectorAll('[data-rwd-active]')"
            + ".forEach(x=>{"
            + "x.style.outline=x.dataset.oldOutline||'';"
            + "x.removeAttribute('data-rwd-active')});"
            + "arguments[0].dataset.oldOutline="
            + "arguments[0].style.outline||'';"
            + "arguments[0].style.outline='3px solid red';"
            + "arguments[0].setAttribute("
            + "'data-rwd-active',arguments[1]);"
            + "arguments[0].scrollIntoView({block:'center'});",
            e, title
        );
        System.out.println("[RWD] " + title);
    }

    public void click(WebElement e, String title) {
        mark(e, title);
        wait.until(
            ExpectedConditions.elementToBeClickable(e)
        ).click();
    }

    /**
     * Kiểm tra các liên kết navigation đang hiển thị.
     * Không phụ thuộc hoàn toàn vào aria-expanded.
     */
    public boolean menuExpanded() {
        return ((Number) js.executeScript("""
            const labels=[
                'trang chủ','giới thiệu',
                'phương pháp học','bài viết',
                'học thử ngay','đăng nhập',
                'đăng ký ngay'
            ];

            const entries=[
                ...document.querySelectorAll('a,button')
            ].filter(e=>{
                const text=(e.innerText||'')
                    .trim().toLowerCase()
                    .replace(/\\s+/g,' ');
                const r=e.getBoundingClientRect();
                const style=getComputedStyle(e);
                return labels.includes(text)
                    &&r.width>0&&r.height>0
                    &&r.top>=-2&&r.top<500
                    &&r.left>=-2
                    &&style.visibility!=='hidden'
                    &&style.display!=='none'
                    &&!e.closest('footer');
            });
            return entries.length;
        """)).intValue() >= 2;
    }

    public void toggleMenu(boolean shouldOpen) {
        if (menuExpanded() != shouldOpen) {
            click(
                hamburger(),
                shouldOpen
                    ? "Mở Hamburger"
                    : "Đóng Hamburger"
            );
        }

        try {
            wait.until(
                d -> menuExpanded() == shouldOpen
            );
        } catch (TimeoutException ex) {
            throw new AssertionError(
                "Hamburger không chuyển sang trạng thái "
                + (shouldOpen ? "MỞ" : "ĐÓNG")
                + "; visible header links="
                + visibleHeaderLinks()
                + "; toggle="
                + hamburger().getAttribute("outerHTML"),
                ex
            );
        }
    }

    public String visibleHeaderLinks() {
        Object links = js.executeScript("""
            return [
                ...document.querySelectorAll('a')
            ].filter(e=>{
                let r=e.getBoundingClientRect();
                return r.width>0
                    &&r.height>0
                    &&r.top>=0
                    &&r.top<500
                    &&getComputedStyle(e).visibility
                        !=='hidden'
                    &&!e.closest('footer');
            }).map(e=>(e.innerText||'').trim())
              .filter(Boolean).join(' | ');
        """);

        return String.valueOf(links);
    }

    public WebElement menuLink(String text) {
        By by = By.xpath(
            "//header//a[contains(normalize-space(.),"
            + xpathLiteral(text) + ")]"
            + "|//nav//a[contains(normalize-space(.),"
            + xpathLiteral(text) + ")]"
            + "|//*[@id='mobileMenu']//a"
            + "[contains(normalize-space(.),"
            + xpathLiteral(text) + ")]"
        );
        return firstVisible(by);
    }

    private static String xpathLiteral(String s) {
        return "'" + s.replace("'", "") + "'";
    }

    public long overflow() {
        return ((Number) js.executeScript(
            "return Math.max(0,"
            + "Math.max("
            + "document.documentElement.scrollWidth,"
            + "document.body.scrollWidth)"
            + "-document.documentElement.clientWidth)"
        )).longValue();
    }

    public String overflowDiagnostics() {
        return String.valueOf(js.executeScript("""
            return [
                ...document.body.querySelectorAll('*')
            ].filter(e=>{
                const r=e.getBoundingClientRect();
                const s=getComputedStyle(e);
                if(
                    r.width<2||r.height<2
                    ||s.display==='none'
                    ||s.visibility==='hidden'
                )return false;
                if(s.position==='fixed')return false;
                return r.right>
                    document.documentElement.clientWidth+3
                    ||r.left<-3;
            }).sort((a,b)=>
                b.getBoundingClientRect().right
                -a.getBoundingClientRect().right
            ).slice(0,8).map(e=>{
                let r=e.getBoundingClientRect();
                return e.tagName.toLowerCase()
                    +'#'+(e.id||'')+'.'
                    +(typeof e.className==='string'
                        ?e.className.trim()
                          .replace(/\\s+/g,'.')
                          .slice(0,65)
                        :'')
                    +'[left='+Math.round(r.left)
                    +',right='+Math.round(r.right)+']';
            }).join(' | ');
        """));
    }

    public void scrollBottom() {
        js.executeScript(
            "window.scrollTo(0,document.body.scrollHeight);"
        );
    }

    public WebElement footer() {
        scrollBottom();
        return firstVisible(
            By.cssSelector("footer")
        );
    }

    public long scroll() {
        return ((Number) js.executeScript(
            "return window.scrollY"
        )).longValue();
    }

    public boolean fits(WebElement e) {
        return Boolean.TRUE.equals(
            js.executeScript(
                "let r=arguments[0].getBoundingClientRect();"
                + "return r.width>0"
                + "&&r.left>=-3"
                + "&&r.right<=innerWidth+3;",
                e
            )
        );
    }

    public boolean imageOk(WebElement e) {
        return Boolean.TRUE.equals(
            js.executeScript(
                "return arguments[0].tagName!=='IMG'"
                + "||(arguments[0].complete"
                + "&&arguments[0].naturalWidth>0);",
                e
            )
        );
    }

    public WebElement linkByHref(String path) {
        return firstVisible(
            By.cssSelector(
                "a[href*='" + path + "']"
            )
        );
    }

    public void back() {
        String previous = driver.getCurrentUrl();

        driver.navigate().back();

        wait.until(
            d -> !d.getCurrentUrl().equals(previous)
        );

        wait.until(
            d -> "complete".equals(
                js.executeScript(
                    "return document.readyState"
                )
            )
        );

        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.tagName("body")
            )
        );
    }

    public boolean onHome() {
        String current = driver.getCurrentUrl()
            .split("[?#]", 2)[0];

        return current.matches(
            "https?://learnenglish"
            + "\\.plt\\.pro\\.vn/"
            + "(?:index\\.php)?"
        );
    }

    public boolean homeContentPresent() {
        if (!onHome()) {
            return false;
        }

        try {
            return cta().isDisplayed()
                && text().toLowerCase(Locale.ROOT)
                    .contains("wordwise");
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public long footerOverflow(WebElement footer) {
        return ((Number) js.executeScript("""
            const footer=arguments[0];
            const vw=document.documentElement.clientWidth;
            const nodes=[
                footer,...footer.querySelectorAll('*')
            ];
            let outside=0;

            for(const e of nodes){
                const cs=getComputedStyle(e);
                const r=e.getBoundingClientRect();
                if(
                    cs.display==='none'
                    ||cs.visibility==='hidden'
                    ||r.width<1||r.height<1
                )continue;

                outside=Math.max(
                    outside,r.right-vw,-r.left
                );
            }
            return Math.max(0,Math.ceil(outside));
        """, footer)).longValue();
    }

    public long historyLength() {
        return ((Number) js.executeScript(
            "return history.length"
        )).longValue();
    }

    public long elementOverflow(WebElement root) {
        return ((Number) js.executeScript("""
            const root=arguments[0];
            const vw=document.documentElement.clientWidth;
            let outside=0;

            for(const el of [
                root,...root.querySelectorAll('*')
            ]){
                const cs=getComputedStyle(el);
                const r=el.getBoundingClientRect();

                if(
                    cs.display==='none'
                    ||cs.visibility==='hidden'
                    ||r.width<1||r.height<1
                )continue;

                outside=Math.max(
                    outside,r.right-vw,-r.left
                );
            }

            return Math.max(0,Math.ceil(outside));
        """, root)).longValue();
    }

    public String url() {
        return driver.getCurrentUrl();
    }
}
