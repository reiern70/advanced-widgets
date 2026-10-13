/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link TabsPage} in Chrome and in Firefox, in Wicket, React and Svelte, Wicket in
 * deployment mode so through the minified script built from frontend/core: selecting tabs with the
 * mouse and the keyboard, the mark of changes, closing tabs with and without asking, the action of
 * a document, new documents and the second tabs switched by the form. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class TabsBrowserTest extends FrameworkAppsTestCase
{
	private static final String START = "Close a document with its ×, Delete or the middle " +
		"button; one with changes asks first.";

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void selectsTabsWithTheMouseAndTheKeyboard(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);

		assertEquals(List.of("Welcome", "notes.txt", "todo.txt"), titles(documents()));
		assertTrue(panel(documents(), "Welcome").isDisplayed());
		assertEquals("Documents", documents().findElement(By.cssSelector(".antilia-tabs-list"))
			.getDomAttribute("aria-label"));

		button(documents(), "notes.txt").click();
		wait.until(d -> panel(documents(), "notes.txt").isDisplayed());
		assertFalse(panel(documents(), "Welcome").isDisplayed());

		button(documents(), "notes.txt").sendKeys(Keys.ARROW_RIGHT);
		wait.until(d -> selected(documents()).equals("todo.txt"));
		assertEquals(button(documents(), "todo.txt"), driver.switchTo().activeElement());
		driver.switchTo().activeElement().sendKeys(Keys.HOME);
		wait.until(d -> selected(documents()).equals("Welcome"));
		driver.switchTo().activeElement().sendKeys(Keys.END);
		wait.until(d -> selected(documents()).equals("todo.txt"));
		assertEquals(START, status());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void asksBeforeClosingADocumentWithChanges(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		button(documents(), "notes.txt").click();
		WebElement editor = panel(documents(), "notes.txt").findElement(By.tagName("textarea"));
		editor.sendKeys(Keys.END, "\nWater the plants");

		// the server, or the app, hears it and marks the tab
		wait.until(d -> marked(documents(), "notes.txt"));
		assertFalse(marked(documents(), "todo.txt"));
		screenshot("modified");

		close(documents(), "notes.txt");
		WebElement dialog = wait.until(d -> d.findElement(By.cssSelector(
			".antilia-tabs-confirm .modal-dialog")));
		assertEquals("Close notes.txt?", dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		screenshot("confirm");
		dialog.findElement(By.cssSelector("[data-confirm='keep']")).click();
		wait.until(d -> d.findElements(By.cssSelector(".antilia-tabs-confirm")).isEmpty());
		assertEquals(List.of("Welcome", "notes.txt", "todo.txt"), titles(documents()));
		assertTrue(editor.getDomProperty("value").endsWith("Water the plants"));

		close(documents(), "notes.txt");
		wait.until(d -> d.findElement(By.cssSelector(".antilia-tabs-confirm [data-confirm='discard']")))
			.click();
		wait.until(d -> titles(documents()).equals(List.of("Welcome", "todo.txt")));
		assertEquals("todo.txt", selected(documents()), "the next one is selected");
		wait.until(d -> status().equals("The server forgot notes.txt, closed."));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void savesADocumentAndClosesItWithoutAsking(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		button(documents(), "todo.txt").click();
		WebElement document = panel(documents(), "todo.txt");
		document.findElement(By.tagName("textarea")).sendKeys(Keys.END, "\n- Ship it");
		wait.until(d -> marked(documents(), "todo.txt"));

		document.findElement(By.cssSelector("button[type='submit']")).click();
		wait.until(d -> !marked(documents(), "todo.txt"));
		wait.until(d -> status().equals("Saved todo.txt."));

		// with Delete, while the tab has the focus
		button(documents(), "todo.txt").sendKeys(Keys.DELETE);
		wait.until(d -> titles(documents()).equals(List.of("Welcome", "notes.txt")));
		assertTrue(driver.findElements(By.cssSelector(".antilia-tabs-confirm")).isEmpty());
		assertEquals("notes.txt", selected(documents()), "the previous one, the last one closed");
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void describesADocumentAndOpensNewOnes(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		button(documents(), "todo.txt").click();
		WebElement about = tab(documents(), "todo.txt").findElement(By.cssSelector(
			".antilia-tab-actions button"));
		assertEquals("About this document", about.getDomAttribute("aria-label"));
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", about);
		about.click();
		// a popover pointing at the action, above its tab
		WebElement popover = wait.until(d -> d.findElements(By.cssSelector(".antilia-popover"))
			.stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
		assertEquals("todo.txt", popover.findElement(By.cssSelector(".antilia-popover-title"))
			.getText());
		assertEquals("todo.txt: 2 lines, 51 characters; all saved.", popover.findElement(
			By.cssSelector(".antilia-popover-body")).getText());
		assertEquals("top", popover.getDomAttribute("data-side"));
		assertTrue(popover.getRect().getY() + popover.getRect().getHeight() <=
			tab(documents(), "todo.txt").getRect().getY(), "above the tab");
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-default"));
		assertEquals("true", about.getDomAttribute("aria-expanded"));
		double arrow = (Double)((JavascriptExecutor)driver).executeScript("var a = arguments[0]" +
			".querySelector('.antilia-popover-arrow').getBoundingClientRect(), b = arguments[1]" +
			".getBoundingClientRect(); return (a.left + a.width / 2) - (b.left + b.width / 2) + 0.0;",
			popover, about);
		assertEquals(0, arrow, 2, "the arrow points at the action");
		assertEquals("todo.txt", selected(documents()), "the action does not select its tab");
		screenshot("popover");
		// the action again closes it
		about.click();
		wait.until(d -> d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
		assertEquals("false", about.getDomAttribute("aria-expanded"));
		about.click();
		wait.until(d -> !d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(d -> d.findElements(By.cssSelector(".antilia-popover")).isEmpty());
		assertEquals(about, driver.switchTo().activeElement(), "the action gets the focus back");

		panel(documents(), "todo.txt").findElement(By.tagName("textarea")).sendKeys(Keys.END, "!");
		app().findElement(By.cssSelector(".tabs-demo__new")).click();
		wait.until(d -> titles(documents()).equals(List.of("Welcome", "notes.txt", "todo.txt",
			"untitled-1.txt")));
		wait.until(d -> selected(documents()).equals("untitled-1.txt"));
		assertTrue(tab(documents(), "untitled-1.txt").findElement(By.cssSelector(
			".antilia-tab-close")).isDisplayed());
		wait.until(d -> status().equals("Opened untitled-1.txt."));
		// what was typed in the others stays
		assertTrue(panel(documents(), "todo.txt").findElement(By.tagName("textarea"))
			.getDomProperty("value").endsWith("frameworks!"));
		assertTrue(marked(documents(), "todo.txt"));

		// the middle button closes the new one, without changes
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].dispatchEvent(new MouseEvent('auxclick', {bubbles: true, button: 1}));",
			button(documents(), "untitled-1.txt"));
		wait.until(d -> titles(documents()).equals(List.of("Welcome", "notes.txt", "todo.txt")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void switchesTheOrientationAndTheDesignOfTheSecondTabs(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		selectCase("Any design, along the top or down the side");
		assertTrue(settings().getDomAttribute("class").contains("antilia-tabs-vertical"));
		assertTrue(settings().getDomAttribute("class").contains("antilia-tabs-pills"));
		assertEquals("vertical", settings().findElement(By.cssSelector(".antilia-tabs-list"))
			.getDomAttribute("aria-orientation"));
		assertEquals("The server knows the tab selected: Profile.", sentence());

		button(settings(), "Privacy").click();
		wait.until(d -> sentence().equals("The server knows the tab selected: Privacy."));

		app().findElement(By.cssSelector(".tabs-demo__controls input[type='checkbox']")).click();
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-tabs-horizontal"));
		new Select(app().findElement(By.cssSelector(".tabs-demo__controls select")))
			.selectByVisibleText("Underline");
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-tabs-underline"));
		screenshot("underline");
		assertEquals("horizontal", settings().findElement(By.cssSelector(".antilia-tabs-list"))
			.getDomAttribute("aria-orientation"));
		// the tab selected stays so, rendered again
		wait.until(d -> selected(settings()).equals("Privacy"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheGalleryOfDesigns(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		selectCase("A gallery of designs");
		List<WebElement> examples = app().findElements(By.cssSelector(".tabs-demo__example"));
		assertEquals(List.of("Segmented", "Folder", "Lifted", "Side bar", "Steps", "Cards"),
			examples.stream().map(example -> example.findElement(By.tagName("figcaption"))
				.getText()).toList());
		WebElement steps = gallery("Steps");
		assertTrue(steps.getDomAttribute("class").contains("antilia-tabs-vertical"));
		assertEquals("Payment", selected(steps));
		// the steps before the selected one are done: a check mark instead of their number
		assertEquals("\"✓\"", ((JavascriptExecutor)driver).executeScript("return getComputedStyle(" +
			"arguments[0], '::before').content;", button(steps, "Account")));
		button(steps, "Shipping").click();
		wait.until(d -> selected(gallery("Steps")).equals("Shipping"));
		assertTrue(gallery("Segmented").getDomAttribute("class").contains("antilia-tabs-horizontal"));
		screenshot("gallery", gallerySection());
		assertNoErrors();

		if (!"wicket".equals(framework))
		{
			return;
		}
		// in the colors of the theme: the dark one
		// the select of the theme is under a drop-down of the widgets
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === 'Dark'); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select")));
		wait.until(d -> !((String)((JavascriptExecutor)d).executeScript("return getComputedStyle(" +
			"arguments[0]).backgroundColor;", gallery("Lifted").findElement(By.cssSelector(
				".antilia-tabs-panels")))).equals("rgb(255, 255, 255)"));
		selectCase("A gallery of designs");
		screenshot("gallery-dark", gallerySection());
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void drawsTheTabsInTheBootstrapLook(SeleniumBrowser browser, String framework)
	{
		openTabs(browser, framework);
		chooseLook("Bootstrap");
		wait.until(d -> titles(documents()).size() >= 2);

		// tabs, pills and underline are Bootstrap's navs, the other designs the components' own
		WebElement list = documents().findElement(By.cssSelector(".antilia-tabs-list"));
		assertTrue(list.getDomAttribute("class").contains("nav nav-tabs"), list.getDomAttribute(
			"class"));
		WebElement first = tab(documents(), titles(documents()).get(0));
		assertTrue(first.getDomAttribute("class").contains("nav-item"));
		// the corners of Bootstrap's tabs, its nav-tabs-border-radius
		assertEquals("6px", ((JavascriptExecutor)driver).executeScript("return getComputedStyle(" +
			"arguments[0]).borderTopLeftRadius;", first));
		List<WebElement> closes = documents().findElements(By.cssSelector(".antilia-tab-close"));
		assertTrue(!closes.isEmpty() && closes.stream().allMatch(each -> each.getDomAttribute(
			"class").contains("btn-close")));
		assertEquals("antilia-tabs-list", gallery("Folder").findElement(By.cssSelector(
			".antilia-tabs-list")).getDomAttribute("class"));
		screenshot("bootstrap");
		selectCase("A gallery of designs");
		screenshot("bootstrap-gallery", gallerySection());
		assertNoErrors();
	}

	private WebElement gallerySection()
	{
		return app().findElement(By.cssSelector(".tabs-demo__gallery"))
			.findElement(By.xpath("ancestor::section[1]"));
	}

	private WebElement gallery(String name)
	{
		return app().findElements(By.cssSelector(".tabs-demo__example")).stream()
			// its text even while the case of the gallery is hidden
			.filter(example -> name.equals(example.findElement(By.tagName("figcaption"))
				.getDomProperty("textContent")))
			.findFirst().orElseThrow().findElement(By.cssSelector(".antilia-tabs"));
	}

	/**
	 * Takes a screenshot of the page into the folder of the system property tabs.screenshots, if
	 * set.
	 */
	private void screenshot(String name)
	{
		screenshot(name, null);
	}

	/**
	 * Takes a screenshot of an element, or of the page when null, as {@link #screenshot(String)}.
	 */
	private void screenshot(String name, WebElement element)
	{
		String folder = System.getProperty("tabs.screenshots");
		if (folder == null)
		{
			return;
		}
		try
		{
			Path path = Path.of(folder, "tabs-" + name + "-" + framework + "-" +
				browser.name().toLowerCase(Locale.ROOT) + ".png");
			Files.createDirectories(path.getParent());
			Files.write(path, (element != null ? element : (TakesScreenshot)driver)
				.getScreenshotAs(OutputType.BYTES));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private void openTabs(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "tabs");
		wait.until(d -> !documents().findElements(By.cssSelector(".antilia-tab-button[role='tab']"))
			.isEmpty() && documents().findElement(By.cssSelector(".antilia-tabs-list"))
			.getDomAttribute("aria-orientation") != null);
	}

	private WebElement documents()
	{
		return app().findElement(By.cssSelector(".tabs-demo__documents.antilia-tabs, " +
			".tabs-demo__documents > .antilia-tabs"));
	}

	private WebElement settings()
	{
		return app().findElement(By.cssSelector(".tabs-demo__settings.antilia-tabs, " +
			".tabs-demo__settings > .antilia-tabs"));
	}

	/**
	 * @return the titles of the open tabs
	 */
	private static List<String> titles(WebElement tabs)
	{
		return tabs.findElements(By.cssSelector(":scope > .antilia-tabs-list > .antilia-tab"))
			.stream().filter(WebElement::isDisplayed)
			.map(tab -> tab.findElement(By.cssSelector(".antilia-tab-title")).getText()).toList();
	}

	private static WebElement tab(WebElement tabs, String title)
	{
		return tabs.findElements(By.cssSelector(":scope > .antilia-tabs-list > .antilia-tab"))
			.stream().filter(tab -> tab.findElement(By.cssSelector(".antilia-tab-title")).getText()
				.equals(title)).findFirst().orElseThrow();
	}

	private static WebElement button(WebElement tabs, String title)
	{
		return tab(tabs, title).findElement(By.cssSelector(".antilia-tab-button"));
	}

	private static WebElement panel(WebElement tabs, String title)
	{
		String id = button(tabs, title).getDomAttribute("aria-controls");
		return tabs.findElement(By.id(id));
	}

	private static String selected(WebElement tabs)
	{
		return tabs.findElement(By.cssSelector(":scope > .antilia-tabs-list > .antilia-tab > " +
			"[aria-selected='true'] .antilia-tab-title")).getText();
	}

	private static boolean marked(WebElement tabs, String title)
	{
		return tab(tabs, title).findElements(By.cssSelector(".antilia-tab-mark")).stream()
			.anyMatch(WebElement::isDisplayed);
	}

	private static void close(WebElement tabs, String title)
	{
		tab(tabs, title).findElement(By.cssSelector(".antilia-tab-close")).click();
	}

	private String status()
	{
		return app().findElements(By.cssSelector(".tabs-demo__status")).get(0).getText();
	}

	private String sentence()
	{
		return app().findElements(By.cssSelector(".tabs-demo__status")).get(1).getText();
	}
}
