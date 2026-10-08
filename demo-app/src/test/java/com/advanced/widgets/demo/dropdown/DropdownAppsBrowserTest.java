/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;

/**
 * Drives the React and Svelte apps of {@link DropdownPage} in their tabs: the same form as the
 * Wicket one, the sentence below it following every choice in the browser. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DropdownAppsBrowserTest extends FrameworkAppsTestCase
{
	private static final String START =
		"Size M, country Germany; skills: Java, Apache Wicket; languages: nothing.";

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void startsWithTheChoicesOfThePage(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dropdown");

		wait.until(d -> choices().equals(START));
		assertEquals("M", toggle("size").getText());
		assertEquals("Germany", toggle("country").getText());
		assertEquals(List.of("Java", "Apache Wicket"), chips("skills"));
		assertEquals("Choose languages", toggle("languages").getText());
		assertFalse(toggle("disabled").isEnabled());
		assertFalse(driver.findElement(By.id("framework-panel-wicket")).isDisplayed());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theSentenceFollowsEveryChoice(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dropdown");
		wait.until(d -> choices().equals(START));

		toggle("country").click();
		WebElement filter = wrapper("country").findElement(
			By.cssSelector(".wicket-dropdown-filter"));
		filter.sendKeys("zeal");
		filter.sendKeys(Keys.ENTER);
		wait.until(d -> choices().contains("country New Zealand;"));

		toggle("size").click();
		option("size", "XL").click();
		wait.until(d -> choices().startsWith("Size XL,"));

		toggle("languages").click();
		option("languages", "Español").click();
		option("languages", "Deutsch").click();
		wait.until(d -> choices().endsWith("languages: Deutsch, Español."));
		assertTrue(wrapper("languages").getDomAttribute("class").contains("wicket-dropdown-open"),
			"a multiple drop-down stays open");

		app().findElement(By.cssSelector(".dropdown-demo__send")).click();
		wait.until(d -> app().findElement(By.cssSelector(".feedbackPanel")).getText()
			.equals("Sent: Size XL, country New Zealand; skills: Java, Apache Wicket; " +
				"languages: Deutsch, Español."));
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theTabChosenStaysForTheNextDemo(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dropdown");

		driver.findElement(By.cssSelector(".demo__step:last-child")).click();

		wait.until(d -> d.getCurrentUrl().contains("/autocomplete"));
		wait.until(d -> "true".equals(tab(framework).getDomAttribute("aria-selected")));
		assertFalse(driver.findElement(By.id("framework-panel-wicket")).isDisplayed());
		tab("wicket").click();
		assertTrue(driver.findElement(By.id("framework-panel-wicket")).isDisplayed());
		for (String other : FrameworkTabs.FRAMEWORKS)
		{
			assertFalse(driver.findElement(By.id("framework-panel-" + other)).isDisplayed());
		}
	}

	private String choices()
	{
		return app().findElement(By.cssSelector(".dropdown-demo__choices")).getText();
	}

	private WebElement wrapper(String name)
	{
		return app().findElement(By.name(name)).findElement(By.xpath(".."));
	}

	private WebElement toggle(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".wicket-dropdown-toggle"));
	}

	private List<String> chips(String name)
	{
		return wrapper(name).findElements(By.cssSelector(".wicket-chip")).stream()
			.map(WebElement::getText)
			.toList();
	}

	private WebElement option(String name, String text)
	{
		return wrapper(name).findElements(By.cssSelector(".wicket-dropdown-option")).stream()
			.filter(option -> option.getText().equals(text))
			.findFirst()
			.orElseThrow();
	}
}
