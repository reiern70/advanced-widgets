/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.frameworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.advanced.widgets.demo.JettyTestCase;
import com.advanced.widgets.demo.SeleniumBrowser;

/**
 * Drives the React and the Svelte demo of {@link FrameworksPage} in Chrome and in Firefox: they
 * load their choices from the mounted {@link ChoicesResource}, draw the drop-down of the core over
 * the select their framework renders, and their state follows each choice. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class FrameworksBrowserTest extends JettyTestCase
{
	private WebDriver driver;

	private WebDriverWait wait;

	private String framework;

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

	@ParameterizedTest
	@MethodSource("frameworks")
	void choosesACountryThroughTheFilter(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);
		assertEquals("Choose a country", toggle("country").getText());
		assertEquals("Country nothing; skills: nothing.", chosen());

		toggle("country").click();
		WebElement filter = wrapper("country").findElement(By.cssSelector(".wicket-dropdown-filter"));
		filter.sendKeys("zeal");
		filter.sendKeys(Keys.ENTER);

		wait.until(d -> chosen().equals("Country New Zealand; skills: nothing."));
		assertEquals("New Zealand", toggle("country").getText());
		assertEquals("NZ", select("country").getDomProperty("value"));
	}

	@ParameterizedTest
	@MethodSource("frameworks")
	void togglesSkillsWhileTheListStaysOpen(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);

		toggle("skills").click();
		option("skills", "Svelte").click();
		option("skills", "Java").click();

		wait.until(d -> chosen().equals("Country nothing; skills: Java, Svelte."));
		assertTrue(wrapper("skills").getDomAttribute("class").contains("wicket-dropdown-open"));
		assertEquals(List.of("Java", "Svelte"), wrapper("skills")
			.findElements(By.cssSelector(".wicket-chip")).stream()
			.map(WebElement::getText)
			.toList());

		option("skills", "Java").click();
		wait.until(d -> chosen().equals("Country nothing; skills: Svelte."));
	}

	@ParameterizedTest
	@MethodSource("frameworks")
	void choosesColleaguesWithTheAutocomplete(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);
		WebElement text = root().findElement(By.cssSelector(".wicket-autocomplete-text"));

		text.sendKeys("an");
		wait.until(d -> !suggestions().isEmpty());
		WebElement first = suggestions().get(0);
		String name = first.findElement(By.tagName("b")).getText();
		first.click();

		wait.until(d -> colleagues().equals(name));
		assertEquals(List.of(name), root().findElements(
			By.cssSelector(".wicket-autocomplete .wicket-chip-text")).stream()
			.map(WebElement::getText)
			.toList());

		root().findElement(By.cssSelector(".wicket-autocomplete .wicket-chip-remove")).click();
		wait.until(d -> colleagues().equals("nothing"));
	}

	@ParameterizedTest
	@MethodSource("frameworks")
	void listsTheUsersLoadingMoreAsTheListScrolls(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);
		wait.until(d -> users().size() == 20);

		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollTop = arguments[0].scrollHeight;", list());

		wait.until(d -> users().size() == 40);
		WebElement user = users().get(1);
		String id = user.getDomAttribute("data-key");
		user.click();
		wait.until(d -> root().findElement(By.cssSelector(".framework-demo__picked")).getText()
			.equals("Picked the user with ID " + id + "."));
	}

	@ParameterizedTest
	@MethodSource("frameworks")
	void rendersOneDropdownPerSelect(SeleniumBrowser browser, String framework)
	{
		open(browser, framework);

		assertEquals(2, root().findElements(By.cssSelector(".wicket-dropdown-toggle")).size(),
			"React's StrictMode mounts twice, the core draws once");
		if (browser == SeleniumBrowser.CHROME)
		{
			List<LogEntry> errors = driver.manage().logs().get("browser").getAll().stream()
				.filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE
					.intValue())
				// the demo application has no icon
				.filter(entry -> !entry.getMessage().contains("/favicon.ico"))
				.toList();
			assertEquals(List.of(), errors, "errors in the console, a blocked script for one");
		}
	}

	static Stream<Arguments> frameworks()
	{
		return Stream.of(SeleniumBrowser.values())
			.flatMap(browser -> Stream.of(Arguments.of(browser, "react"),
				Arguments.of(browser, "svelte")));
	}

	private void open(SeleniumBrowser browser, String framework)
	{
		this.framework = framework;
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/react-svelte");
		wait.until(d -> root().findElements(By.cssSelector(".wicket-dropdown-toggle")).size() == 2);
	}

	private WebElement root()
	{
		return driver.findElement(By.cssSelector("[data-framework-demo='" + framework + "']"));
	}

	private WebElement select(String name)
	{
		return root().findElement(By.name(name));
	}

	private WebElement wrapper(String name)
	{
		return select(name).findElement(By.xpath(".."));
	}

	private WebElement toggle(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".wicket-dropdown-toggle"));
	}

	private WebElement option(String name, String text)
	{
		return wrapper(name).findElements(By.cssSelector(".wicket-dropdown-option")).stream()
			.filter(option -> option.getText().equals(text))
			.findFirst()
			.orElseThrow();
	}

	private List<WebElement> suggestions()
	{
		return root().findElements(By.cssSelector(".wicket-autocomplete-option"));
	}

	private String colleagues()
	{
		return root().findElement(By.cssSelector(".framework-demo__colleagues")).getText();
	}

	private WebElement list()
	{
		return root().findElement(By.cssSelector(".json-infinite-list"));
	}

	private List<WebElement> users()
	{
		return list().findElements(By.cssSelector(".json-infinite-list-item"));
	}

	private String chosen()
	{
		return root().findElement(By.cssSelector(".framework-demo__chosen")).getText();
	}
}
