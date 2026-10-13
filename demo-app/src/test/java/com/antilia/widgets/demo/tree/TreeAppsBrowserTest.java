/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.antilia.widgets.demo.Cases;
import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the trees of {@link TreePage} and {@link TreeGridPage}: those of Wicket components and
 * of JSON in the Wicket tab, and the React and Svelte apps in theirs. A click on a group selects
 * and expands it, showing its card; a click on a species, or Enter on it, shows its lineage, the
 * card of its group and its own. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class TreeAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theTreeOfAnAppSelectsAGroupAndASpecies(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "tree");
		wait.until(d -> node(app(), "Theropods") != null);

		node(app(), "Theropods").findElement(By.cssSelector(".antilia-tree-content")).click();
		wait.until(d -> details(app()).contains("Theropoda"));
		wait.until(d -> node(app(), "Tyrannosaurus rex") != null);
		assertEquals("true", node(app(), "Theropods").getAttribute("aria-selected"));

		node(app(), "Tyrannosaurus rex").click();
		wait.until(d -> details(app()).contains("Tyrannosaurus rex"));
		assertTrue(details(app()).contains("Animals"), details(app()));
		assertTrue(details(app()).contains("Theropoda"), details(app()));
		assertEquals("true", node(app(), "Tyrannosaurus rex").getAttribute("aria-selected"));

		// the keyboard: Down to the next species, Enter selects it
		node(app(), "Tyrannosaurus rex").sendKeys(Keys.ARROW_DOWN);
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);
		wait.until(d -> details(app()).contains("Allosaurus fragilis"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theTreeGridOfAnAppShowsTheColumnsAndWhatIsSelected(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "tree-grid");
		wait.until(d -> node(app(), "Theropods") != null);

		WebElement row = node(app(), "Theropods");
		assertEquals("Theropoda", row.findElements(By.tagName("td")).get(1).getText());
		// a cell, as the user clicks: geckodriver does not scroll to a row
		row.findElement(By.cssSelector(".antilia-tree-content")).click();
		wait.until(d -> details(app()).contains("Theropoda"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bothTreesOfTheWicketTabShowWhatIsSelected(SeleniumBrowser browser)
	{
		openWicketTab(browser, "tree");
		bothTreesShowWhatIsSelected(".antilia-tree:not(.json-tree)", null, null);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void bothTreeGridsOfTheWicketTabShowWhatIsSelected(SeleniumBrowser browser)
	{
		openWicketTab(browser, "tree-grid");
		bothTreesShowWhatIsSelected(".antilia-tree-grid:not(.json-tree)",
			"TreeGrid · Wicket components", "JsonBasedTreeGrid · Rendered from JSON");
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
	 * Selects a group in the tree of components, then a species in the JSON tree, the way to the
	 * dinosaurs expanded in both.
	 *
	 * @param componentsCase
	 *            the case showing the tree of components, {@code null} when both trees show
	 * @param jsonCase
	 *            the case showing the JSON tree, {@code null} when both trees show
	 */
	private void bothTreesShowWhatIsSelected(String componentsTree, String componentsCase,
		String jsonCase)
	{
		WebElement wicket = driver.findElement(By.cssSelector("[data-framework-panel='wicket']"));
		if (componentsCase != null)
		{
			Cases.select(wait, wicket, componentsCase);
		}
		WebElement tree = wicket.findElement(By.cssSelector(componentsTree));
		wait.until(d -> tree.findElements(By.cssSelector(".antilia-tree-node-button")).stream()
			.anyMatch(button -> button.getText().equals("Dinosaurs")));
		tree.findElements(By.cssSelector(".antilia-tree-node-button")).stream()
			.filter(button -> button.getText().equals("Dinosaurs")).findFirst().orElseThrow()
			.click();
		wait.until(d -> details(wicket).contains("Dinosauria"));

		if (jsonCase != null)
		{
			Cases.select(wait, wicket, jsonCase);
		}
		WebElement jsonTree = wicket.findElement(By.cssSelector(".json-tree"));
		wait.until(d -> node(jsonTree, "Ornithischians") != null);
		node(jsonTree, "Ornithischians").findElement(By.cssSelector(".antilia-tree-content"))
			.click();
		wait.until(d -> node(jsonTree, "Triceratops") != null);
		node(jsonTree, "Triceratops").findElement(By.cssSelector(".antilia-tree-content")).click();
		wait.until(d -> details(wicket).contains("Triceratops horridus"));
		assertTrue(details(wicket).contains("Ornithischia"), details(wicket));
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
		return context.findElements(By.cssSelector(".antilia-tree-item")).stream()
			.filter(item -> item.findElement(By.cssSelector(".antilia-tree-content")).getText()
				.equals(name))
			.skip(nth - 1L)
			.findFirst()
			.orElse(null);
	}

	private static String details(SearchContext context)
	{
		return context.findElement(By.cssSelector(".tree-demo-taxon")).getText();
	}
}
