package vn.team.autotest.modules.learn.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** Page Object dùng chung User/Admin cho trang Học tập. */
/**
 * Encapsulates locators and waits. CSS can be overridden without code edits.
 */
public class LearnPage {
	private final String[] searchCandidates = { "input#searchQuery", "input#searchInput", "input#topicSearch",
			"input#searchTopic", "input[name='search']", "input[name='keyword']", "input[placeholder*='Tìm']",
			"input[placeholder*='tìm']", "input[placeholder*='Search']", "input[type='search']" };
	private final String[] cardCandidates = { "#categoryGrid .cat-card", ".topic-card", ".category-card",
			"[data-topic-id]", "[data-category-id]", "#topicList .glass-card", "#topicsContainer .glass-card",
			".topics-grid > div", ".topic-grid > div", "a[href*='topic_id=']", "a[href*='category_id=']" };
	private final String[] titleCandidates = { "[data-topic-name]", ".topic-name", ".category-name", "h3", "h4", "h2",
			".font-bold" };
	private final WebDriver driver;
	private final WebDriverWait wait;
	private final JavascriptExecutor js;

	public LearnPage(WebDriver driver, WebDriverWait wait) {
		this.driver = driver;
		this.wait = wait;
		this.js = (JavascriptExecutor) driver;
	}

	public static final class Topic {
		public final String name;
		public final String id;

		public Topic(String name, String id) {
			this.name = normalize(name);
			this.id = id == null ? "" : id;
		}

		@Override
		public String toString() {
			return name + (id.isEmpty() ? "" : " [id=" + id + "]");
		}
	}

	private static String normalize(String s) {
		return s == null ? "" : s.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
	}

	private static String prop(String key, String fallback) {
		String s = System.getProperty(key);
		return s == null || s.isBlank() ? fallback : s.trim();
	}

	private void require(boolean ok, String message) {
		if (!ok)
			throw new AssertionError(message);
	}

	private void highlight(WebElement element, String label) {
		try {
			js.executeScript(
					"arguments[0].style.outline='3px solid #e11d48';arguments[0].setAttribute('data-test-status',arguments[1]);",
					element, label);
		} catch (WebDriverException ignored) {
		}
	}

	private static final String BASE = "https://learnenglish.plt.pro.vn/";
	private By searchBy, cardBy;
	private String titleCss;

	/** Attach to learn.php reached via a real UI navigation. */
	public void attach() {
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
		require(driver.getCurrentUrl().contains("learn.php"), "Not on Learn: " + driver.getCurrentUrl());
		locate();
		if (searchBy != null)
			highlight(driver.findElement(searchBy), "ĐANG TEST: SEARCH CHỦ ĐỀ");
	}

	public void open() {
		String url = prop("learn.url", BASE + "learn.php");
		driver.get(url);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
		try {
			new WebDriverWait(driver, Duration.ofSeconds(12))
					.until(d -> !d.findElements(By.id("categoryGrid")).isEmpty()
							|| !d.findElements(By.id("searchQuery")).isEmpty());
		} catch (TimeoutException e) {
			System.err.println("[LEARN DIAGNOSTIC] Learn controls did not appear after 12s: " + driver.getCurrentUrl());
		}
		require(!driver.getCurrentUrl().contains("login.php"), "Học tập chuyển về login: " + driver.getCurrentUrl());
		locate();
		highlight(driver.findElement(searchBy), "ĐANG TEST: SEARCH CHỦ ĐỀ");
	}

	private void locate() {
		String explicitSearch = System.getProperty("learn.searchCss");
		String explicitCard = System.getProperty("learn.cardCss");
		searchBy = locateFirst(explicitSearch, searchCandidates, true);
		// DOM may contain an empty category grid; keep a grounded selector for
		// diagnostics.
		cardBy = locateFirst(explicitCard, cardCandidates, false);
		if (cardBy == null)
			cardBy = By.cssSelector(
					explicitCard != null && !explicitCard.isBlank() ? explicitCard : "#categoryGrid .cat-card");
		titleCss = prop("learn.titleCss", "");
		require(searchBy != null, "Không tìm được ô Search. URL=" + driver.getCurrentUrl() + "; các input="
				+ summarize("input") + ". Thêm -Dlearn.searchCss=CSS_THUC_TE");
		// Card presence must be verified by test assertions, not @BeforeClass.
		// Missing/empty grids now generate a precise error instead of skipping the
		// entire suite.
		if (driver.findElements(By.id("categoryGrid")).isEmpty())
			System.err.println("[LEARN DIAGNOSTIC] #categoryGrid missing; URL=" + driver.getCurrentUrl() + "; heading="
					+ summarize("h1,h2,h3") + "; inputs=" + summarize("input"));
		System.out.println("[LEARN LOCATORS] search=" + searchBy + "; cards=" + cardBy + "; title=" + titleCss);
	}

	private By locateFirst(String explicit, String[] options, boolean input) {
		if (explicit != null && !explicit.isBlank()) {
			By by = By.cssSelector(explicit);
			require(!driver.findElements(by).isEmpty(), "Selector override không tìm được phần tử: " + explicit);
			return by;
		}
		for (String option : options) {
			By by = By.cssSelector(option);
			List<WebElement> nodes = driver.findElements(by);
			if (nodes.isEmpty())
				continue;
			if (input) {
				for (WebElement e : nodes) {
					try {
						if (e.isDisplayed() && e.isEnabled())
							return by;
					} catch (StaleElementReferenceException ignored) {
					}
				}
			} else {
				if (option.equals("#categoryGrid .cat-card"))
					return by;
				int named = 0;
				for (int i = 0; i < Math.min(nodes.size(), 8); i++) {
					try {
						if (!topicName(nodes.get(i)).isBlank())
							named++;
					} catch (Exception ignored) {
					}
				}
				if (named > 0)
					return by;
			}
		}
		return null;
	}

	private String topicName(WebElement card) {
		if (!titleCss.isBlank()) {
			List<WebElement> matches = card.findElements(By.cssSelector(titleCss));
			return matches.isEmpty() ? "" : normalize(matches.get(0).getText());
		}
		for (String selector : titleCandidates) {
			List<WebElement> matches = card.findElements(By.cssSelector(selector));
			for (WebElement e : matches) {
				String name = normalize(e.getText());
				if (!name.isBlank())
					return name;
			}
		}
		return ""; // require an identifiable title, never use entire card body as topic name
	}

	private String summarize(String selector) {
		List<String> out = new ArrayList<>();
		for (WebElement e : driver.findElements(By.cssSelector(selector))) {
			if (out.size() == 12)
				break;
			out.add(e.getTagName() + "#" + e.getDomAttribute("id") + "." + e.getDomAttribute("class") + " placeholder="
					+ e.getDomAttribute("placeholder"));
		}
		return out.toString();
	}

	/**
	 * Read the entire rendered card list in one browser command (much faster than
	 * many WebElement calls).
	 */
	@SuppressWarnings("unchecked")
	public List<Topic> cards() {
		String selector = (cardBy == null ? "#categoryGrid .cat-card"
				: cardBy.toString().replaceFirst("^By\\.cssSelector: ", ""));
		Object result = js.executeScript(
				"""
						const selector=arguments[0], titleCss=arguments[1];
						return [...document.querySelectorAll(selector)].filter(e=>e.getClientRects().length>0)
						    .map(e=>{
						        let title=titleCss ? e.querySelector(titleCss) : null;
						        if(!title)title=e.querySelector('[data-topic-name],.topic-name,.category-name,h3,h4,h2,.font-bold');
						        return {name:(title?.textContent||'').trim(), id:e.getAttribute('data-id')||e.getAttribute('data-topic-id')||e.getAttribute('data-category-id')||''};
						    }).filter(e=>e.name);
						""",
				selector, titleCss);
		List<Topic> out = new ArrayList<>();
		for (Object item : (List<?>) result) {
			Map<String, Object> row = (Map<String, Object>) item;
			out.add(new Topic(String.valueOf(row.get("name")), String.valueOf(row.get("id"))));
		}
		return out;
	}

	public WebElement search() {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(searchBy));
	}

	public void enter(String keyword) {
		WebElement input = search();
		require(input.isEnabled(), "Ô Search disabled");
		highlight(input, "ĐANG TEST: " + keyword);
		input.click();
		input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		if (!normalize(input.getDomProperty("value")).isBlank())
			input.sendKeys(Keys.BACK_SPACE);
		if (!keyword.isEmpty())
			input.sendKeys(keyword);
		require(normalize(input.getDomProperty("value")).equals(normalize(keyword)),
				"Ô Search không nhận đúng keyword. Actual=" + input.getDomProperty("value"));
		// Search UI may debounce. Wait until card set stabilizes after input (not fixed
		// Thread.sleep).
		waitStableCards();
	}

	public void waitStableCards() {
		final long[] last = { Long.MIN_VALUE };
		final int[] stable = { 0 };
		WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(4));
		fastWait.pollingEvery(Duration.ofMillis(125));
		fastWait.until(d -> {
			long fingerprint = cards().toString().hashCode();
			if (fingerprint == last[0])
				stable[0]++;
			else
				stable[0] = 0;
			last[0] = fingerprint;
			return stable[0] >= 2;
		});
	}

	public void clear() {
		enter("");
	}

	public String url() {
		return driver.getCurrentUrl();
	}
}
