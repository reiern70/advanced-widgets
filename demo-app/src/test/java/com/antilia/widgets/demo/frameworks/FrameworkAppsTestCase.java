/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.frameworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.Cases;
import com.antilia.widgets.demo.JettyTestCase;
import com.antilia.widgets.demo.SeleniumBrowser;

/**
 * Drives the React and Svelte apps of a demo in its {@link FrameworkTabs}, in Chrome and in
 * Firefox. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
public abstract class FrameworkAppsTestCase extends JettyTestCase
{
	protected WebDriver driver;

	protected WebDriverWait wait;

	protected SeleniumBrowser browser;

	protected String framework;

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

	/**
	 * @return every browser with every framework
	 */
	static Stream<Arguments> frameworks()
	{
		return Stream.of(SeleniumBrowser.values())
			.flatMap(browser -> Stream.of(FrameworkTabs.FRAMEWORKS)
				.map(framework -> Arguments.of(browser, framework)));
	}

	/**
	 * Opens a demo and shows the tab of a framework.
	 *
	 * @param path
	 *            the path the demo is mounted at
	 */
	protected void open(SeleniumBrowser browser, String framework, String path)
	{
		this.browser = browser;
		this.framework = framework;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + path);
		tab(framework).click();
		wait.until(d -> app().isDisplayed() && !app().findElements(By.xpath("*")).isEmpty());
	}

	/**
	 * Chooses a look in the top bar, which every page of the session is in then, and shows the tab
	 * of the framework again in the page loaded anew.
	 *
	 * @param name
	 *            the name of the look, as the top bar has it: Bootstrap for example
	 */
	protected void chooseLook(String name)
	{
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === arguments[1]); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select[name$='look']")), name);
		String look = "antilia-look-" + name.toLowerCase(Locale.ROOT);
		wait.until(d -> d.findElement(By.tagName("body")).getDomAttribute("class").contains(look));
		tab(framework).click();
		wait.until(d -> app().isDisplayed() && !app().findElements(By.xpath("*")).isEmpty());
	}

	/**
	 * Shows a case of the demo, in the app of the framework or in the Wicket demo.
	 *
	 * @param title
	 *            the title of the case, as its pill has it
	 */
	protected void selectCase(String title)
	{
		Cases.select(wait, app(), title);
	}

	/**
	 * @return the tab of a framework
	 */
	protected WebElement tab(String framework)
	{
		return driver.findElement(By.id("framework-tab-" + framework));
	}

	/**
	 * @return every browser with Wicket and every framework
	 */
	static Stream<Arguments> wicketAndFrameworks()
	{
		return Stream.of(SeleniumBrowser.values())
			.flatMap(browser -> Stream.concat(Stream.of("wicket"),
				Stream.of(FrameworkTabs.FRAMEWORKS)).map(framework -> Arguments.of(browser, framework)));
	}

	/**
	 * @return the element the app of the framework renders into, the panel of the Wicket demo for
	 *         {@code wicket}
	 */
	protected WebElement app()
	{
		if ("wicket".equals(framework))
		{
			return driver.findElement(By.id("framework-panel-wicket"));
		}
		return driver.findElement(By.cssSelector("[data-framework-app='" + framework + "']"));
	}

	/**
	 * Fails on errors in Chrome's console, a script the policy blocked for one.
	 */
	protected void assertNoErrors()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<LogEntry> errors = driver.manage().logs().get("browser").getAll().stream()
			.filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE
				.intValue())
			// the demo application has no icon
			.filter(entry -> !entry.getMessage().contains("/favicon.ico"))
			.toList();
		assertEquals(List.of(), errors, "errors in the console");
	}
}
