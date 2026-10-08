/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the trees of {@link TreePage} and {@link TreeGridPage}: those of Wicket components and
 * of JSON in the Wicket tab, and the React and Svelte apps in theirs. A click on a folder expands
 * it, a click on a file, or Enter on it, shows the file next to the tree. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class TreeAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theTreeOfAnAppExpandsAFolderAndShowsTheFileSelected(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "tree");
		wait.until(d -> node(app(), "package.json") != null);

		node(app(), "ui-components").click();
		wait.until(d -> node(app(), "pom.xml", 2) != null);

		node(app(), "package.json").click();
		wait.until(d -> details(app()).contains("frontend/package.json"));
		assertEquals("true", node(app(), "package.json").getAttribute("aria-selected"));

		// the keyboard: Down to the next file, Enter selects it
		node(app(), "package.json").sendKeys(Keys.ARROW_DOWN);
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		wait.until(d -> !details(app()).contains("frontend/package.json"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theTreeGridOfAnAppShowsTheColumnsAndTheFileSelected(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "tree-grid");
		wait.until(d -> node(app(), "package.json") != null);

		WebElement row = node(app(), "package.json");
		assertEquals("JSON", row.findElements(By.tagName("td")).get(1).getText());
		// a cell, as the user clicks: geckodriver does not scroll to a row
		row.findElement(By.cssSelector(".wicket-tree-content")).click();
		wait.until(d -> details(app()).contains("frontend/package.json"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bothTreesOfTheWicketTabShowTheFileSelected(SeleniumBrowser browser)
	{
		openWicketTab(browser, "tree");
		bothTreesShowTheFileSelected(".wicket-tree:not(.json-tree)");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bothTreeGridsOfTheWicketTabShowTheFileSelected(SeleniumBrowser browser)
	{
		openWicketTab(browser, "tree-grid");
		bothTreesShowTheFileSelected(".wicket-tree-grid:not(.json-tree)");
	}

	private void openWicketTab(SeleniumBrowser browser, String path)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + path);
	}

	/**
	 * Selects a file in the tree of components, then one in the JSON tree, the folder frontend
	 * expanded in both.
	 */
	private void bothTreesShowTheFileSelected(String componentsTree)
	{
		WebElement wicket = driver.findElement(By.cssSelector("[data-framework-panel='wicket']"));
		WebElement tree = wicket.findElement(By.cssSelector(componentsTree));
		wait.until(d -> tree.findElements(By.cssSelector(".wicket-tree-node-button")).stream()
			.anyMatch(button -> button.getText().equals("package.json")));
		tree.findElements(By.cssSelector(".wicket-tree-node-button")).stream()
			.filter(button -> button.getText().equals("package.json")).findFirst().orElseThrow()
			.click();
		wait.until(d -> details(wicket).contains("frontend/package.json"));

		WebElement jsonTree = wicket.findElement(By.cssSelector(".json-tree"));
		wait.until(d -> node(jsonTree, "README.md") != null);
		node(jsonTree, "README.md").findElement(By.cssSelector(".wicket-tree-content")).click();
		wait.until(d -> details(wicket).contains("frontend/README.md"));
		assertNoErrors();
	}

	/**
	 * @return the item of the first node shown under a name, {@code null} if none is
	 */
	private static WebElement node(SearchContext context, String name)
	{
		return node(context, name, 1);
	}

	/**
	 * @return the item of the nth node shown under a name, {@code null} if there is none
	 */
	private static WebElement node(SearchContext context, String name, int nth)
	{
		return context.findElements(By.cssSelector(".wicket-tree-item")).stream()
			.filter(item -> item.findElement(By.cssSelector(".wicket-tree-content")).getText()
				.equals(name))
			.skip(nth - 1L)
			.findFirst()
			.orElse(null);
	}

	private static String details(SearchContext context)
	{
		return context.findElement(By.cssSelector(".tree-demo-file")).getText();
	}
}
