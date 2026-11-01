
package vn.team.autotest.modules.auth.page;

import java.time.Duration;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final String BASE_URL =
            "https://learnenglish.plt.pro.vn";

    private static final String LOGIN_PATH = "/login.php";

    // Các locator cần đối chiếu với giao diện đăng nhập thực tế.
    private final By usernameInput = By.cssSelector(
            "input[name='username'], input[name='email'], input[type='email']"
    );

    private final By passwordInput = By.cssSelector(
            "input[name='password'], input[type='password']"
    );

    private final By loginButton = By.cssSelector(
            "button[type='submit'], input[type='submit']"
    );

    private final By body = By.tagName("body");

    public LoginPage(WebDriver driver) {
        this.driver = Objects.requireNonNull(
                driver, "WebDriver không được null."
        );

        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(15)
        );
    }

    public void open() {
        driver.get(BASE_URL + LOGIN_PATH);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(body)
        );
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + LOGIN_PATH);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(body)
        );
    }

    public boolean isLoginPageDisplayed() {
        return !driver.findElements(usernameInput).isEmpty()
                && !driver.findElements(passwordInput).isEmpty();
    }

    public void enterUsername(String username) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        usernameInput
                )
        );

        element.clear();
        element.sendKeys(username);
    }

    public void enterPassword(String password) {
        WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        passwordInput
                )
        );

        element.clear();
        element.sendKeys(password);
    }

    public void clickLogin() {
        wait.until(
                ExpectedConditions.elementToBeClickable(loginButton)
        ).click();
    }

    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getPageText() {
        try {
            return driver.findElement(body).getText();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isLoginErrorDisplayed() {
        String text = getPageText().toLowerCase();

        return text.contains("invalid password")
                || text.contains("invalid username")
                || text.contains("incorrect password")
                || text.contains("đăng nhập thất bại")
                || text.contains("mật khẩu không đúng");
    }
}
