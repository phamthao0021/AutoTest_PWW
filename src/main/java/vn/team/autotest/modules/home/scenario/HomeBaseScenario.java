package vn.team.autotest.modules.home.scenario;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/** Independent fixture: one fresh Guest browser per test invocation. */
public abstract class HomeBaseScenario {
    private WebDriver browser;
    private static final String BASE_URL = "https://learnenglish.plt.pro.vn/";

    @BeforeMethod(alwaysRun = true)
    public void startHomeBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1366,768");
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }
        String executable = System.getProperty("home.chromedriver", "").trim();
        if (executable.isEmpty()) {
            browser = new ChromeDriver(options); // Selenium Manager resolves ChromeDriver.
        } else {
            ChromeDriverService service = new ChromeDriverService.Builder()
                    .usingDriverExecutable(Path.of(executable).toFile()).build();
            browser = new ChromeDriver(service, options);
        }
        browser.manage().timeouts().implicitlyWait(Duration.ZERO);
        browser.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
        browser.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
    }

    protected final WebDriver driver() {
        if (browser == null) throw new IllegalStateException("WebDriver chưa được khởi tạo");
        return browser;
    }

    protected final void open(String path) {
        driver().get(URI.create(BASE_URL).resolve(path).toString());
    }

    @AfterMethod(alwaysRun = true)
    public void closeHomeBrowser() {
        try {
            if (browser != null) browser.quit();
        } finally { browser = null; }
    }
}
