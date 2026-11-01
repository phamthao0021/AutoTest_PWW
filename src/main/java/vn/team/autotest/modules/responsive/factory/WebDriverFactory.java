
package vn.team.autotest.modules.responsive.factory;

import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class WebDriverFactory {

    private WebDriverFactory() {
    }

    public static WebDriver mobile(
            int width,
            int height) {

        ChromeOptions opts = new ChromeOptions();

        opts.addArguments(
            "--disable-notifications"
        );

        opts.setExperimentalOption(
            "mobileEmulation",
            Map.of(
                "deviceMetrics",
                Map.of(
                    "width", width,
                    "height", height,
                    "pixelRatio", 3,
                    "mobile", true,
                    "touch", true
                )
            )
        );

        return new ChromeDriver(opts);
    }
}
