
package vn.team.autotest.modules.dashboard.page;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By body = By.tagName("body");
    private final By headings = By.cssSelector("h1, h2");
    private final By links = By.tagName("a");
    private final By buttons = By.tagName("button");
    private final By inputs = By.tagName("input");
    private final By images = By.tagName("img");

    public DashboardPage(WebDriver driver) {
        this.driver = Objects.requireNonNull(
                driver, "WebDriver không được null."
        );

        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(15)
        );
    }

    public void open() {
        driver.get("https://learnenglish.plt.pro.vn/");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(body)
        );
    }

    public boolean isDashboardDisplayed() {
        try {
            WebElement element = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(body)
            );

            return element.getText() != null
                    && !element.getText().trim().isEmpty()
                    && !isRedirectedToLogin();

        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRedirectedToLogin() {
        String url = driver.getCurrentUrl().toLowerCase();

        return url.contains("login")
                || url.contains("signin")
                || url.contains("sign-in");
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageText() {
        try {
            return driver.findElement(body).getText();
        } catch (Exception e) {
            return "";
        }
    }

    public List<String> getHeadings() {
        List<String> result = new ArrayList<>();

        for (WebElement element : driver.findElements(headings)) {
            if (element.isDisplayed()) {
                String text = element.getText().trim();

                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }

        return result;
    }

    public int getVisibleLinkCount() {
        return countVisible(links);
    }

    public int getVisibleButtonCount() {
        return countVisible(buttons);
    }

    public int getVisibleInputCount() {
        return countVisible(inputs);
    }

    public int getVisibleImageCount() {
        return countVisible(images);
    }

    public boolean areVisibleImagesLoaded() {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (WebElement image : driver.findElements(images)) {
            if (!image.isDisplayed()) {
                continue;
            }

            Object loaded = js.executeScript(
                    "return arguments[0].complete && " +
                    "arguments[0].naturalWidth > 0;",
                    image
            );

            if (!Boolean.TRUE.equals(loaded)) {
                return false;
            }
        }

        return true;
    }

    public boolean hasInteractiveElements() {
        return hasEnabledVisible(buttons)
                || hasEnabledVisible(inputs)
                || hasEnabledVisible(links);
    }

    public void refresh() {
        driver.navigate().refresh();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(body)
        );
    }

    public boolean containsText(String expectedText) {
        return getPageText().toLowerCase()
                .contains(expectedText.toLowerCase());
    }

    private int countVisible(By locator) {
        int count = 0;

        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed()) {
                count++;
            }
        }

        return count;
    }

    private boolean hasEnabledVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            if (element.isDisplayed() && element.isEnabled()) {
                return true;
            }
        }

        return false;
    }
}
