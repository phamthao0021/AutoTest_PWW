package vn.team.autotest.modules.learn.page;

import java.time.Duration;
import java.util.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

/** POM for PWW_AUTO_08; does not alter LearnPage used by Scenario 7. */
public class LearnFilterPage {
	private final WebDriver driver;
	private final JavascriptExecutor js;
	private final WebDriverWait wait;

	public LearnFilterPage(WebDriver driver) {
		this.driver = driver;
		this.js = (JavascriptExecutor) driver;
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	}

	public static final class Topic {
		public final String id, name, color;
		public final int total;

		public Topic(String id, String name, String color, int total) {
			this.id = id;
			this.name = name;
			this.color = color;
			this.total = total;
		}

		@Override
		public String toString() {
			return name + "{id=" + id + ", color=" + color + ", words=" + total + "}";
		}
	}

	public void ready() {
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sortOrder")));
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("colorFilter")));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("categoryGrid")));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("minWords")));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("maxWords")));
	}

	public List<String> options(String id) {
		List<String> result = new ArrayList<>();

		for (WebElement e : new Select(driver.findElement(By.id(id))).getOptions()) {
			result.add(e.getDomAttribute("value") + "=" + e.getText().trim());
		}

		return result;
	}

	public String selected(String id) {
		return new Select(driver.findElement(By.id(id))).getFirstSelectedOption().getDomAttribute("value");
	}

	public void choose(String id, String value) {
		WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));

		highlight(element, "ĐANG TEST: " + id + "=" + value);

		if (value.equals(new Select(element).getFirstSelectedOption().getDomAttribute("value"))) {
			return;
		}

		new Select(element).selectByValue(value);
		waitStable();
	}

	public void number(String id, String value) {
		WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));

		highlight(element, "ĐANG TEST: " + id + "=" + value);

		if (value.equals(element.getDomProperty("value"))) {
			return;
		}

		element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		element.sendKeys(Keys.BACK_SPACE);

		if (!value.isEmpty()) {
			element.sendKeys(value);
		}

		// Native number inputs may reject invalid tokens.
		// The Scenario records the actual value separately.
		waitStable();
	}

	public String value(String id) {
		return driver.findElement(By.id(id)).getDomProperty("value");
	}

	public String validation(String id) {
		return String.valueOf(js.executeScript("let e=document.getElementById(arguments[0]);"
				+ "return e?JSON.stringify({" + "valid:e.checkValidity()," + "min:e.min,max:e.max,"
				+ "rangeUnderflow:e.validity.rangeUnderflow," + "badInput:e.validity.badInput" + "}):'missing';", id));
	}

	public void reset() {
		choose("sortOrder", "newest");
		choose("colorFilter", "all");

		number("minWords", "");
		number("maxWords", "");

		WebElement q = driver.findElement(By.id("searchQuery"));

		if (!q.getDomProperty("value").isEmpty()) {
			q.sendKeys(Keys.chord(Keys.CONTROL, "a"));
			q.sendKeys(Keys.BACK_SPACE);
			waitStable();
		}
	}

	private void highlight(WebElement e, String text) {
		js.executeScript("arguments[0].style.outline='3px solid #e11d48';" + "arguments[0].setAttribute("
				+ "'data-test-status',arguments[1]);", e, text);
	}

	@SuppressWarnings("unchecked")
	public List<Topic> cards() {
		Object raw = js.executeScript("""
				return [...document.querySelectorAll('#categoryGrid .cat-card')]
				  .filter(e =>
				    e.getClientRects().length > 0
				    && getComputedStyle(e).visibility !== 'hidden'
				    && getComputedStyle(e).display !== 'none'
				  )
				  .map(e => ({
				    id: e.dataset.id || '',
				    name: (e.querySelector('h3')?.textContent || '').trim(),
				    color: (e.dataset.color || '').trim().toLowerCase(),
				    total: Number(e.dataset.total)
				  }));
				""");

		List<Topic> out = new ArrayList<>();

		for (Object item : (List<?>) raw) {
			Map<String, Object> v = (Map<String, Object>) item;
			Object total = v.get("total");

			out.add(new Topic(String.valueOf(v.get("id")), String.valueOf(v.get("name")),
					String.valueOf(v.get("color")), total instanceof Number ? ((Number) total).intValue() : -1));
		}

		return out;
	}

	public boolean noResultsVisible() {
		return Boolean.TRUE.equals(js.executeScript("let e=document.getElementById('noResults');"
				+ "return !!e && e.getClientRects().length>0" + " && getComputedStyle(e).display!=='none';"));
	}

	public void waitStable() {
		final String[] previous = { null };
		final int[] stable = { 0 };

		WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));

		shortWait.pollingEvery(Duration.ofMillis(120));

		shortWait.until(d -> {
			String state = cards().toString();

			stable[0] = state.equals(previous[0]) ? stable[0] + 1 : 0;

			previous[0] = state;

			return stable[0] >= 2;
		});
	}
}