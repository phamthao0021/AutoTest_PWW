package vn.team.autotest.modules.home.page;

import java.time.Duration;
import java.text.Normalizer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public final class HomePage {

	private final WebDriver homeDriver;
	private final WebDriverWait wait;

	// =========================================================
	// PWW_AUTO_01 - HERO
	// =========================================================

	private final By heroTitle = By.cssSelector(".hero-section h1");
	private final By heroDescription = By.cssSelector(".hero-section .hero-subtitle");
	private final By heroPrimaryCTA = By.cssSelector(".hero-section .hero-actions .btn-hero-primary");

	// =========================================================
	// HEADER
	// =========================================================

	private final By header = By.tagName("header");
	private final By headerLinks = By.cssSelector("header a");

	// =========================================================
	// PWW_AUTO_02 - LOGO / BRAND
	// =========================================================

	private final By logoPlt = By.cssSelector("header .landing-logo img");
	private final By wordWiseBrand = By.cssSelector("header .landing-logo span");
	private final By logoHomeLink = By.cssSelector("header a.landing-logo");

	// =========================================================
	// MOBILE MENU
	// =========================================================

	private final By mobileAuthContainer = By.xpath("//*[@id='mobileMenuContainer']/div/div");
	private final By mobileLoginLink = By.xpath("//*[@id='mobileMenuContainer']/div/div/a[1]");
	private final By mobileRegisterLink = By.xpath("//*[@id='mobileMenuContainer']/div/div/a[2]");

	// =========================================================
	// PWW_AUTO_05 - FOOTER
	// =========================================================

	private final By footer = By.tagName("footer");
	private final By footerLinks = By.cssSelector("footer a");

	// =========================================================
	// CONSTRUCTOR
	// =========================================================

	public HomePage(WebDriver driver) {
		this(driver, Duration.ofSeconds(10));
	}

	public HomePage(WebDriver driver, Duration timeout) {
		this.homeDriver = java.util.Objects.requireNonNull(driver, "WebDriver không được null");
		java.util.Objects.requireNonNull(timeout, "Timeout không được null");
		if (timeout.isNegative() || timeout.isZero()) {
			throw new IllegalArgumentException("Timeout phải lớn hơn 0");
		}
		this.wait = new WebDriverWait(homeDriver, timeout);
	}

	// =========================================================
	// HERO
	// =========================================================

	public void waitUntilHeroReady() {
		wait.until(ExpectedConditions.visibilityOfElementLocated(heroTitle));
	}

	public boolean isHeroTitleDisplayed() {
		return isDisplayed(heroTitle);
	}

	public String getHeroTitleText() {
		try {
			return wait.until(ExpectedConditions.visibilityOfElementLocated(heroTitle)).getText().trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public boolean isHeroDescriptionDisplayed() {
		return isDisplayed(heroDescription);
	}

	public String getHeroDescriptionText() {
		return getTextSafe(heroDescription);
	}

	public boolean isHeroPrimaryCTADisplayed() {
		return isDisplayed(heroPrimaryCTA);
	}

	public boolean isHeroPrimaryCTAEnabled() {
		return isEnabled(heroPrimaryCTA);
	}

	public String getHeroPrimaryCTAText() {
		return getTextSafe(heroPrimaryCTA);
	}

	// =========================================================
	// HEADER
	// =========================================================

	public void waitUntilHeaderReady() {
		wait.until(ExpectedConditions.visibilityOfElementLocated(header));
		wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(headerLinks, 0));
	}

	public boolean waitUntilHeaderReadySafe() {
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(header));
			wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(headerLinks, 0));
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isHeaderPresent() {
		try {
			return !homeDriver.findElements(header).isEmpty();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isHeaderDisplayed() {
		return isDisplayed(header);
	}

	public List<String> getVisibleHeaderLabels() {
		List<String> labels = new ArrayList<>();

		for (WebElement link : homeDriver.findElements(headerLinks)) {
			if (!link.isDisplayed()) {
				continue;
			}

			String text = normalizeText(link.getText());
			if (text.isBlank()) {
				continue;
			}

			if (text.equalsIgnoreCase("PLT") || text.equalsIgnoreCase("WordWise")) {
				continue;
			}

			labels.add(text);
		}

		return labels;
	}

	public WebElement getVisibleHeaderItem(String menuText) {
		return findVisibleNamedElement("//header//a", menuText, "menu Header");
	}

	public boolean isHeaderItemDisplayed(String menuText) {
		try {
			return getVisibleHeaderItem(menuText).isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isHeaderItemEnabled(String menuText) {
		try {
			return getVisibleHeaderItem(menuText).isEnabled();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getHeaderItemHref(String menuText) {
		try {
			String href = getVisibleHeaderItem(menuText).getDomAttribute("href");
			return href == null ? "" : href.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public void clickHeaderItem(String menuText) {
		WebElement element = getVisibleHeaderItem(menuText);
		wait.until(ExpectedConditions.elementToBeClickable(element));
		element.click();
	}

	// =========================================================
	// PWW_AUTO_02 - LOGO / BRAND
	// =========================================================

	public WebElement getLogoPlt() {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(logoPlt));
	}

	public boolean isLogoPltDisplayed() {
		try {
			return getLogoPlt().isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getLogoSrc() {
		try {
			String src = getLogoPlt().getDomAttribute("src");
			return src == null ? "" : src.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public boolean isLogoComplete() {
		try {
			Object result = ((JavascriptExecutor) homeDriver).executeScript("return arguments[0].complete;",
					getLogoPlt());
			return Boolean.TRUE.equals(result);
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public long getLogoNaturalWidth() {
		try {
			Object result = ((JavascriptExecutor) homeDriver).executeScript("return arguments[0].naturalWidth;",
					getLogoPlt());
			return result instanceof Number number ? number.longValue() : 0;
		} catch (RuntimeException ex) {
			return 0;
		}
	}

	public long getLogoNaturalHeight() {
		try {
			Object result = ((JavascriptExecutor) homeDriver).executeScript("return arguments[0].naturalHeight;",
					getLogoPlt());
			return result instanceof Number number ? number.longValue() : 0;
		} catch (RuntimeException ex) {
			return 0;
		}
	}

	public boolean isWordWiseDisplayed() {
		try {
			return wait.until(ExpectedConditions.visibilityOfElementLocated(wordWiseBrand)).isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getWordWiseText() {
		try {
			return normalizeText(wait.until(ExpectedConditions.visibilityOfElementLocated(wordWiseBrand)).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public boolean hasLogoHomeLink() {
		return !homeDriver.findElements(logoHomeLink).isEmpty();
	}

	public boolean isLogoHomeLinkDisplayed() {
		return isDisplayed(logoHomeLink);
	}

	public boolean isLogoHomeLinkEnabled() {
		return isEnabled(logoHomeLink);
	}

	public String getLogoHomeHref() {
		return getHrefSafe(logoHomeLink);
	}

	public void clickLogoHome() {
		wait.until(ExpectedConditions.elementToBeClickable(logoHomeLink)).click();
	}

	// =========================================================
	// PWW_AUTO_03 - CTA / NAVIGATION
	// =========================================================

	public WebElement getVisibleAction(String actionText) {
		return findVisibleNamedElement("//*[self::a or self::button]", actionText, "CTA");
	}

	public boolean isActionPresent(String actionText) {
		try {
			getVisibleAction(actionText);
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isActionDisplayed(String actionText) {
		try {
			return getVisibleAction(actionText).isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isActionEnabled(String actionText) {
		try {
			return getVisibleAction(actionText).isEnabled();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getActionText(String actionText) {
		try {
			return normalizeText(getVisibleAction(actionText).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public String getActionHref(String actionText) {
		try {
			String href = getVisibleAction(actionText).getDomAttribute("href");
			return href == null ? "" : href.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public String getActionTagName(String actionText) {
		try {
			return getVisibleAction(actionText).getTagName().trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public void clickAction(String actionText) {
		WebElement element = getVisibleAction(actionText);
		wait.until(ExpectedConditions.elementToBeClickable(element));
		element.click();
	}

	public void waitForUrlChange(String oldUrl) {
		try {
			wait.until(d -> !d.getCurrentUrl().equals(oldUrl));
		} catch (RuntimeException ex) {
			// Một số CTA có thể giữ nguyên URL.
		}
	}

	// =========================================================
	// COMMON PAGE STATE
	// =========================================================

	public boolean waitUntilBodyReady() {
		try {
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
			return true;
		} catch (TimeoutException ex) {
			return false;
		}
	}

	public String getPageTitle() {
		try {
			String title = homeDriver.getTitle();
			return title == null ? "" : title.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public boolean isBodyDisplayed() {
		return isDisplayed(By.tagName("body"));
	}

	public String getBodyText() {
		try {
			return normalizeText(homeDriver.findElement(By.tagName("body")).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public String getCurrentUrlSafe() {
		try {
			String url = homeDriver.getCurrentUrl();
			return url == null ? "" : url.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public boolean isErrorPage() {
		String url = normalizeText(getCurrentUrlSafe()).toLowerCase();
		String title = normalizeText(getPageTitle()).toLowerCase();
		String body = normalizeText(getBodyText()).toLowerCase();

		return url.contains("404") || url.contains("500") || title.contains("404") || title.contains("500")
				|| title.contains("not found") || body.contains("404 not found")
				|| body.contains("500 internal server error");
	}

	public void stopPageLoading() {
		try {
			((JavascriptExecutor) homeDriver).executeScript("window.stop();");
		} catch (RuntimeException ex) {
			// Không làm test crash nếu browser đã mất kết nối.
		}
	}

	// =========================================================
	// PWW_AUTO_04 - FILE-DRIVEN CARD SNAPSHOT
	// Expected chỉ nằm ở Scenario/CSV, không dùng để tìm DOM.
	// =========================================================

	public record HomeCard(int index, String text, String body, int titleNodeCount, boolean displayed,
			boolean inViewport) {
	}

	public static String normalizeHomeContent(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value, Normalizer.Form.NFC).replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
	}

	private By homeCardLocator(String group) {
		if (group == null || group.isBlank()) {
			throw new IllegalArgumentException("Group không được rỗng");
		}
		return switch (group.trim().toUpperCase(Locale.ROOT)) {
		case "STATISTICS" -> By.cssSelector(".stats-grid .stat-card");
		case "AUDIENCE" -> By.cssSelector(".audience-card");
		case "BENEFIT" -> By.cssSelector(".benefits-grid .feature-card");
		case "MODE" -> By.cssSelector("#phuong-phap .feature-card");
		default -> throw new IllegalArgumentException("Group không hợp lệ: " + group);
		};
	}

	/**
	 * Chụp dữ liệu tất cả card theo thứ tự DOM. Không lọc card rỗng/ẩn hoặc loại
	 * trùng để tránh che lỗi UI. Thiếu toàn bộ khu vực gây TimeoutException để
	 * Scenario báo FAIL.
	 */
	public List<HomeCard> readHomeCards(String group) {
		By locator = homeCardLocator(group);
		boolean statistics = "STATISTICS".equalsIgnoreCase(group.trim());
		By fieldLocator = By.cssSelector(statistics ? ".stat-number" : "h3");
		JavascriptExecutor js = (JavascriptExecutor) homeDriver;

		wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
		wait.until(ExpectedConditions.presenceOfElementLocated(locator));

		List<WebElement> elements = homeDriver.findElements(locator);
		List<HomeCard> result = new ArrayList<>();
		WebDriverWait scrollWait = new WebDriverWait(homeDriver, Duration.ofSeconds(2));

		for (int i = 0; i < elements.size(); i++) {
			WebElement card = elements.get(i);

			// Không dùng scrollToElement() ở đây vì hàm cũ nuốt lỗi scroll.
			// Lỗi thao tác phải đi lên Scenario để báo FAIL rõ ràng.
			js.executeScript("arguments[0].scrollIntoView({behavior:'instant',block:'center',inline:'nearest'});",
					card);

			boolean displayed = card.isDisplayed();
			boolean viewport = false;
			if (displayed) {
				try {
					viewport = scrollWait.until(d -> isHomeCardInViewport(card));
				} catch (TimeoutException ex) {
					viewport = false;
				}
			}

			List<WebElement> fields = card.findElements(fieldLocator);
			String text = fields.size() == 1 ? fields.get(0).getText() : "";
			result.add(new HomeCard(i + 1, normalizeHomeContent(text), normalizeHomeContent(card.getText()),
					fields.size(), displayed, viewport));
		}
		return List.copyOf(result);
	}

	/** Tương thích HomeScenario4 đã gửi: readHomeCards(homeDriver(), group). */
	public List<HomeCard> readHomeCards(WebDriver webDriver, String group) {
		if (webDriver != this.homeDriver) {
			throw new IllegalArgumentException("WebDriver không khớp với HomePage hiện tại");
		}
		return readHomeCards(group);
	}

	private boolean isHomeCardInViewport(WebElement card) {
		Object result = ((JavascriptExecutor) homeDriver).executeScript("const r=arguments[0].getBoundingClientRect();"
				+ "return r.width>0 && r.height>0 && r.top<innerHeight && r.bottom>0"
				+ " && r.left<innerWidth && r.right>0;", card);
		return Boolean.TRUE.equals(result);
	}

	public List<HomeCard> readStatisticsCards() {
		return readHomeCards("STATISTICS");
	}

	public List<HomeCard> readAudienceCards() {
		return readHomeCards("AUDIENCE");
	}

	public List<HomeCard> readBenefitCards() {
		return readHomeCards("BENEFIT");
	}

	public List<HomeCard> readLearningModeCards() {
		return readHomeCards("MODE");
	}

	// =========================================================
	// PWW_AUTO_04 - CONTENT
	// =========================================================

	public WebElement getVisibleContentElement(String expectedText) {
		String value = normalizeHomeContent(expectedText);
		if (value.isBlank()) {
			throw new IllegalArgumentException("Content text không được rỗng");
		}
		String literal = xpathLiteral(value);
		List<By> locators = List.of(By.xpath("//*[normalize-space(.)=" + literal + "]"),
				By.xpath("//*[contains(normalize-space(.)," + literal + ")]" + "[not(.//*[contains(normalize-space(.),"
						+ literal + ")])]"));
		for (By locator : locators) {
			for (WebElement element : homeDriver.findElements(locator)) {
				if (element.isDisplayed()) {
					return element;
				}
			}
		}
		throw new IllegalStateException("Không tìm thấy content hiển thị: " + expectedText);
	}

	public boolean isContentPresent(String expectedText) {
		try {
			getVisibleContentElement(expectedText);
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isContentDisplayed(String expectedText) {
		try {
			return getVisibleContentElement(expectedText).isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getContentText(String expectedText) {
		try {
			return normalizeText(getVisibleContentElement(expectedText).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public int countVisibleExactText(String expectedText) {
		String value = normalizeHomeContent(expectedText);
		if (value.isBlank()) {
			return 0;
		}
		By locator = By.xpath("//*[normalize-space(.)=" + xpathLiteral(value) + "]");
		int count = 0;
		for (WebElement element : homeDriver.findElements(locator)) {
			if (element.isDisplayed()) {
				count++;
			}
		}
		return count;
	}

	public void scrollToContent(String expectedText) {
		WebElement element = getVisibleContentElement(expectedText);
		scrollToElement(element);
		wait.until(d -> element.isDisplayed());
	}

	public boolean isElementInViewport(String expectedText) {
		try {
			WebElement element = getVisibleContentElement(expectedText);

			Object result = ((JavascriptExecutor) homeDriver)
					.executeScript("var r=arguments[0].getBoundingClientRect();"
							+ "var vh=window.innerHeight||document.documentElement.clientHeight;"
							+ "var vw=window.innerWidth||document.documentElement.clientWidth;"
							+ "return r.bottom>0 && r.right>0 && r.top<vh && r.left<vw;", element);

			return Boolean.TRUE.equals(result);
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean pageContainsText(String text) {
		try {
			String bodyText = normalizeText(homeDriver.findElement(By.tagName("body")).getText());
			return bodyText.contains(normalizeText(text));
		} catch (RuntimeException ex) {
			return false;
		}
	}

	// =========================================================
	// PWW_AUTO_05 - FOOTER
	// =========================================================

	public void waitUntilFooterReady() {
		wait.until(ExpectedConditions.presenceOfElementLocated(footer));

		WebElement footerElement = homeDriver.findElement(footer);
		scrollToElement(footerElement);

		wait.until(ExpectedConditions.visibilityOf(footerElement));
	}

	public boolean waitUntilFooterReadySafe() {
		try {
			WebElement footerElement = wait.until(ExpectedConditions.presenceOfElementLocated(footer));

			scrollToElement(footerElement);
			wait.until(ExpectedConditions.visibilityOf(footerElement));
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public boolean isFooterDisplayed() {
		try {
			WebElement element = homeDriver.findElement(footer);
			if (!element.isDisplayed()) {
				scrollToElement(element);
			}
			return element.isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	public String getFooterText() {
		try {
			return normalizeText(homeDriver.findElement(footer).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	public List<String> getFooterLinkTexts() {
		List<String> result = new ArrayList<>();
		for (FooterLink link : readVisibleFooterLinks()) {
			result.add(link.text());
		}
		return List.copyOf(result);
	}

	public List<String> getFooterHrefs() {
		List<String> result = new ArrayList<>();
		for (FooterLink link : readVisibleFooterLinks()) {
			result.add(link.href()); // Giữ href rỗng để Scenario5 phát hiện FAIL.
		}
		return List.copyOf(result);
	}

	public FooterSnapshot readFooterSnapshot() {
		waitUntilFooterReady();
		List<String> texts = new ArrayList<>();
		List<String> hrefs = new ArrayList<>();
		for (FooterLink link : readVisibleFooterLinks()) {
			texts.add(link.text());
			hrefs.add(link.href());
		}
		return new FooterSnapshot(getFooterText(), List.copyOf(texts), List.copyOf(hrefs));
	}

	public record FooterSnapshot(String text, List<String> linkTexts, List<String> hrefs) {
	}

	public int getFooterLinkCount() {
		return homeDriver.findElements(footerLinks).size();
	}

	public int getVisibleFooterLinkCount() {
		int count = 0;
		for (WebElement element : homeDriver.findElements(footerLinks)) {
			if (element.isDisplayed()) {
				count++;
			}
		}
		return count;
	}

	public boolean isFooterHrefValid(String href) {
		if (href == null || href.isBlank()) {
			return false;
		}

		String value = href.trim().toLowerCase();
		return !value.equals("#") && !value.startsWith("javascript:");
	}

	public boolean hasBlankFooterHref() {
		for (WebElement element : homeDriver.findElements(footerLinks)) {
			if (!element.isDisplayed()) {
				continue;
			}

			String href = element.getDomAttribute("href");
			if (href == null || href.isBlank()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasHashOnlyFooterHref() {
		for (WebElement element : homeDriver.findElements(footerLinks)) {
			if (!element.isDisplayed()) {
				continue;
			}

			String href = element.getDomAttribute("href");
			if (href != null && href.trim().equals("#")) {
				return true;
			}
		}
		return false;
	}

	public boolean hasJavascriptFooterHref() {
		for (WebElement element : homeDriver.findElements(footerLinks)) {
			if (!element.isDisplayed()) {
				continue;
			}

			String href = element.getDomAttribute("href");
			if (href != null && href.trim().toLowerCase().startsWith("javascript:")) {
				return true;
			}
		}
		return false;
	}

	// =========================================================
	// MOBILE AUTH
	// =========================================================

	public boolean isMobileAuthContainerPresent() {
		return !homeDriver.findElements(mobileAuthContainer).isEmpty();
	}

	public boolean isMobileLoginPresent() {
		return !homeDriver.findElements(mobileLoginLink).isEmpty();
	}

	public boolean isMobileRegisterPresent() {
		return !homeDriver.findElements(mobileRegisterLink).isEmpty();
	}

	public String getMobileLoginText() {
		return getDomTextSafe(mobileLoginLink);
	}

	public String getMobileRegisterText() {
		return getDomTextSafe(mobileRegisterLink);
	}

	public String getMobileLoginHref() {
		return getHrefSafe(mobileLoginLink);
	}

	public String getMobileRegisterHref() {
		return getHrefSafe(mobileRegisterLink);
	}

	// =========================================================
	// COMMON SCROLL / HELPERS
	// =========================================================

	public void scrollToElement(WebElement element) {
		try {
			((JavascriptExecutor) homeDriver).executeScript(
					"arguments[0].scrollIntoView({behavior:'instant', block:'center', inline:'nearest'});", element);
		} catch (RuntimeException ex) {
			// Không làm test crash chỉ vì scroll.
		}
	}

	public void scrollToTop() {
		try {
			((JavascriptExecutor) homeDriver).executeScript("window.scrollTo(0, 0);");
		} catch (RuntimeException ex) {
			// Ignore
		}
	}

	public void scrollToBottom() {
		try {
			((JavascriptExecutor) homeDriver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
		} catch (RuntimeException ex) {
			// Ignore
		}
	}

	private boolean isDisplayed(By locator) {
		try {
			return homeDriver.findElement(locator).isDisplayed();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	private boolean isEnabled(By locator) {
		try {
			return homeDriver.findElement(locator).isEnabled();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	private String getTextSafe(By locator) {
		try {
			return normalizeText(homeDriver.findElement(locator).getText());
		} catch (RuntimeException ex) {
			return "";
		}
	}

	private String getHrefSafe(By locator) {
		try {
			String href = homeDriver.findElement(locator).getDomAttribute("href");
			return href == null ? "" : href.trim();
		} catch (RuntimeException ex) {
			return "";
		}
	}

	private String normalizeText(String value) {
		return normalizeHomeContent(value);
	}

	// =========================================================
	// INTERNAL HELPERS - NAMED LINKS / DOM TEXT / FOOTER ROWS
	// =========================================================

	private WebElement findVisibleNamedElement(String root, String text, String type) {
		String value = normalizeHomeContent(text);
		if (value.isBlank()) {
			throw new IllegalArgumentException("Tên " + type + " không được rỗng");
		}
		String literal = xpathLiteral(value);
		for (String predicate : List.of("[normalize-space(.)=" + literal + "]",
				"[contains(normalize-space(.)," + literal + ")]")) {
			for (WebElement element : homeDriver.findElements(By.xpath(root + predicate))) {
				if (element.isDisplayed()) {
					return element;
				}
			}
		}
		throw new IllegalStateException("Không tìm thấy " + type + " hiển thị: " + text);
	}

	private String xpathLiteral(String value) {
		if (!value.contains("'")) {
			return "'" + value + "'";
		}
		if (!value.contains("\"")) {
			return "\"" + value + "\"";
		}
		String[] parts = value.split("'", -1);
		List<String> args = new ArrayList<>();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				args.add("\"'\"");
			}
			args.add("'" + parts[i] + "'");
		}
		return "concat(" + String.join(",", args) + ")";
	}

	private String getDomTextSafe(By locator) {
		try {
			// KB1 kiểm tra text trong DOM kể cả khi menu mobile đang ẩn.
			// Không dùng cách này cho các kiểm tra nội dung hiển thị KB4.
			return normalizeHomeContent(homeDriver.findElement(locator).getDomProperty("textContent"));
		} catch (org.openqa.selenium.NoSuchElementException ex) {
			return "";
		}
	}

	private record FooterLink(String text, String href) {
	}

	private List<FooterLink> readVisibleFooterLinks() {
		List<FooterLink> rows = new ArrayList<>();
		for (WebElement element : homeDriver.findElements(footerLinks)) {
			if (element.isDisplayed()) {
				String href = element.getDomAttribute("href");
				rows.add(new FooterLink(normalizeHomeContent(element.getText()), href == null ? "" : href.trim()));
			}
		}
		return rows;
	}
}
