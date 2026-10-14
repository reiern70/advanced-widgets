/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.Cases;
import com.antilia.widgets.demo.Dropdowns;
import com.antilia.widgets.demo.JettyTestCase;
import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.Toasts;

/**
 * Downloads from {@link DownloadsPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core, into a folder of the build: via Ajax in each location,
 * the buttons busy while the server is slow, the failures each location can tell, by links and
 * after the form, in the Antilia look and in Bootstrap's. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DownloadsBrowserTest extends JettyTestCase
{
	private static final Duration DOWNLOAD = Duration.ofSeconds(20);

	private SeleniumBrowser browser;

	private WebDriver driver;

	private WebDriverWait wait;

	private Path downloads;

	@Override
	@AfterEach
	public void after() throws Exception
	{
		if (driver != null)
		{
			driver.quit();
		}
		super.after();
	}

	private void open(SeleniumBrowser browser)
	{
		this.browser = browser;
		// below the home folder and outside of hidden folders, which Firefox in a snap writes to
		String basedir = System.getProperty("basedir", ".");
		downloads = Path.of(basedir, "target", "browser-downloads",
			browser.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID());
		try
		{
			Files.createDirectories(downloads);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		driver = browser.newDriver(1300, 1000, downloads);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/downloads");
		wait.until(d -> !d.findElements(By.cssSelector(".downloads-demo__item")).isEmpty());
		recordToasts();
	}

	/**
	 * Keeps the text of every notification the page shows, as it shows it: one telling of a
	 * download in the same window shows while Firefox's driver waits for the window, and is gone
	 * after its five seconds when the driver is back.
	 */
	private void recordToasts()
	{
		js("window.toasts = []; new MutationObserver(records => records.forEach(record => " +
			"record.addedNodes.forEach(node => node.querySelectorAll && node.querySelectorAll(" +
			"'.antilia-notification-body').forEach(body => window.toasts.push(" +
			"body.textContent))))).observe(document.body, {childList: true, subtree: true});");
	}

	/**
	 * @return the button, or the link drawn as a button, of the case shown with the label
	 */
	private WebElement button(String label)
	{
		return wait.until(d -> d.findElements(By.cssSelector(
			"#framework-panel-wicket :is(button, a)")).stream()
			.filter(WebElement::isDisplayed)
			.filter(button -> label.equals(button.getText().trim()))
			.findFirst().orElse(null));
	}

	private WebElement select(String name)
	{
		return driver.findElement(By.cssSelector("#framework-panel-wicket select[name$='" + name +
			"']"));
	}

	private Object js(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	/**
	 * @return the files downloaded, but those the browser is still writing
	 */
	private List<String> downloaded()
	{
		try (Stream<Path> files = Files.list(downloads))
		{
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Waits until the browser has saved a file, and reads it.
	 */
	private byte[] waitForFile(String name)
	{
		Path file = downloads.resolve(name);
		new WebDriverWait(driver, DOWNLOAD, Duration.ofMillis(100)).until(d -> {
			try
			{
				return Files.isRegularFile(file) && Files.size(file) > 0 && downloaded().stream()
					.noneMatch(each -> each.endsWith(".part") || each.endsWith(".crdownload"));
			}
			catch (IOException e)
			{
				return false;
			}
		});
		try
		{
			return Files.readAllBytes(file);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private String waitForText(String name)
	{
		return new String(waitForFile(name), StandardCharsets.UTF_8);
	}

	/**
	 * Waits until the page has shown a notification with the text, since it last loaded.
	 */
	private void waitForToast(String text)
	{
		new WebDriverWait(driver, DOWNLOAD, Duration.ofMillis(50))
			.withMessage(() -> "the page showed " + js("return window.toasts;") + ", busy: " +
				js("return Array.from(document.querySelectorAll('[aria-busy]')).map(b => " +
					"b.textContent.trim());"))
			.until(d -> text.equals(Toasts.latest(d)) ||
				((List<?>)js("return window.toasts || [];")).contains(text));
	}

	private boolean isBusy(WebElement button)
	{
		return "true".equals(button.getDomAttribute("aria-busy")) &&
			button.getDomAttribute("class").contains("ant-button--busy");
	}

	/**
	 * @return the animation of the spinner of a button, {@code none} when it has none
	 */
	private String spinner(WebElement button)
	{
		return (String)js("return getComputedStyle(arguments[0], '::before').animationName;",
			button);
	}

	/**
	 * Fails on errors in Chrome's console, a script or a frame the policy blocked for one.
	 */
	private void assertNoErrors()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<LogEntry> errors = driver.manage().logs().get("browser").getAll().stream()
			.filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE
				.intValue())
			.filter(entry -> !entry.getMessage().contains("/favicon.ico"))
			.toList();
		assertEquals(List.of(), errors, "errors in the console");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void downloadsIntoTheBrowserAndThroughAHiddenIframe(SeleniumBrowser browser)
	{
		open(browser);

		button("Into the browser").click();
		assertTrue(waitForText("countries.csv").startsWith("\"Code\",\"Name\"\r\n\"AF\","));
		waitForToast("countries.csv: Into the browser.");

		Dropdowns.chooseByValue(driver, select("file"), "JSON");
		button("Through a hidden iframe").click();
		assertTrue(waitForText("countries.json").contains("\"title\": \"Countries of the world\""));
		waitForToast("countries.json: Through a hidden iframe.");
		wait.until(d -> d.findElements(By.tagName("iframe")).stream()
			.noneMatch(frame -> frame.getDomAttribute("hidden") != null));
		assertEquals(List.of("countries.csv", "countries.json"), downloaded());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void opensTheFileInANewTab(SeleniumBrowser browser)
	{
		open(browser);
		String page = driver.getWindowHandle();

		Dropdowns.chooseByValue(driver, select("file"), "SVG");
		button("In a new tab").click();

		wait.until(d -> d.getWindowHandles().size() == 2);
		waitForToast("countries.svg: In a new tab.");
		String tab = driver.getWindowHandles().stream().filter(handle -> !handle.equals(page))
			.findFirst().orElseThrow();
		driver.switchTo().window(tab);
		wait.until(d -> d.getCurrentUrl().contains("/downloads"));
		wait.until(d -> !d.findElements(By.tagName("svg")).isEmpty());
		assertEquals("Countries of the world", driver.findElement(By.tagName("svg"))
			.findElement(By.tagName("title")).getDomProperty("textContent"));
		driver.close();
		driver.switchTo().window(page);
		assertEquals(List.of(), downloaded(), "shown, not saved");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void downloadsInTheSameWindowAndStaysOnThePage(SeleniumBrowser browser)
	{
		open(browser);
		Dropdowns.chooseByValue(driver, select("file"), "ZIP");

		button("In the same window").click();

		assertTrue(waitForFile("countries.zip").length > 1000);
		waitForToast("countries.zip: In the same window.");
		assertTrue(driver.getCurrentUrl().contains("/downloads"));
		assertTrue(button("In the same window").isEnabled());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aButtonIsBusyWhileTheServerIsSlowAndIgnoresClicks(SeleniumBrowser browser)
	{
		open(browser);
		Dropdowns.chooseByValue(driver, select("answer"), "SLOWLY");
		WebElement button = button("Into the browser");
		assertEquals("none", spinner(button));

		button.click();
		wait.until(d -> isBusy(button));
		assertEquals("ant-button-spin", spinner(button));
		assertFalse(button.findElement(By.cssSelector(".ant-button__icon")).isDisplayed(),
			"the spinner in the place of the icon");
		assertEquals(button, driver.switchTo().activeElement(), "the button keeps the focus");
		button.click();
		js("arguments[0].click();", button);

		waitForText("countries.csv");
		waitForToast("countries.csv: Into the browser.");
		wait.until(d -> !isBusy(button));
		assertEquals("none", spinner(button));
		// the clicks while it was busy started nothing
		js("return new Promise(resolve => setTimeout(resolve, 4000));");
		assertEquals(List.of("countries.csv"), downloaded());
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aFailureIsToldWhereTheBrowserCanTellIt(SeleniumBrowser browser)
	{
		open(browser);
		String page = driver.getWindowHandle();
		Dropdowns.chooseByValue(driver, select("answer"), "FAILS");
		wait.until(d -> !button("In the same window").isEnabled());
		assertTrue(driver.findElement(By.cssSelector(".downloads-demo__blocked")).isDisplayed());

		button("Into the browser").click();
		waitForToast("The server could not send countries.csv: Into the browser.");
		wait.until(d -> !isBusy(button("Into the browser")));

		button("Through a hidden iframe").click();
		waitForToast("The server could not send countries.csv: Through a hidden iframe.");

		button("In a new tab").click();
		waitForToast("The server could not send countries.csv: In a new tab.");
		wait.until(d -> d.getWindowHandles().equals(java.util.Set.of(page)));

		assertEquals(List.of(), downloaded());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void linksDownloadWithoutAjax(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Links");

		button("countries.txt").click();
		assertTrue(waitForText("countries.txt").startsWith("Countries of the world\n"));
		button("countries.csv").click();
		waitForFile("countries.csv");
		button("countries.json").click();
		waitForFile("countries.json");
		button("downloads.png").click();
		byte[] png = waitForFile("downloads.png");
		assertEquals(0x89, png[0] & 0xff);
		assertEquals("PNG", new String(png, 1, 3, StandardCharsets.US_ASCII));

		assertEquals(List.of("countries.csv", "countries.json", "countries.txt", "downloads.png"),
			downloaded());
		assertTrue(driver.getCurrentUrl().contains("/downloads"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFormDownloadsTheReportOnceItIsValid(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "After a form");
		WebElement title = driver.findElement(By.cssSelector(
			"#framework-panel-wicket input[name$='title']"));

		title.clear();
		button("Download").click();
		waitForToast("'Title' is required.");

		title.sendKeys("Europe");
		WebElement rows = driver.findElement(By.cssSelector(
			"#framework-panel-wicket input[name$='rows']"));
		rows.clear();
		rows.sendKeys("3");
		Dropdowns.chooseByValue(driver, select("format"), "CSV");
		button("Download").click();

		assertEquals(4, waitForText("europe.csv").split("\r\n").length);
		waitForToast("europe.csv: Into the browser.");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theButtonsAreBusyInTheBootstrapLookToo(SeleniumBrowser browser)
	{
		open(browser);
		js("var s = arguments[0]; s.selectedIndex = Array.from(s.options).findIndex(o => " +
			"o.text === 'Bootstrap'); s.dispatchEvent(new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select[name$='look']")));
		wait.until(d -> d.findElement(By.tagName("body")).getDomAttribute("class")
			.contains("antilia-look-bootstrap"));
		recordToasts();
		Dropdowns.chooseByValue(driver, select("answer"), "SLOWLY");

		WebElement button = button("Into the browser");
		assertEquals("btn btn-primary", button.getDomAttribute("class"));
		button.click();
		wait.until(d -> isBusy(button));
		assertEquals("ant-button-spin", spinner(button));

		waitForFile("countries.csv");
		wait.until(d -> !isBusy(button));
		assertNoErrors();
	}
}
