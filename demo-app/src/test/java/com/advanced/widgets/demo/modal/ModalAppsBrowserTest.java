/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.modal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;

/**
 * Adds, edits and deletes members of the team of {@link ModalDialogPage} in its modal dialogs, in
 * the Wicket, React and Svelte tabs, in Chrome and Firefox: the same markup, Escape closing a
 * dialog and the focus going back to the button that opened it, and the messages of the Wicket
 * form. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ModalAppsBrowserTest extends FrameworkAppsTestCase
{
	/** The fields of the form, in Wicket, React and Svelte. */
	private static final By NAME = By.cssSelector("input[autocomplete='name']");

	private static final By EMAIL = By.cssSelector("input[autocomplete='email']");

	/**
	 * @return every browser with Wicket and every framework
	 */
	static Stream<Arguments> tabs()
	{
		return Stream.of(SeleniumBrowser.values())
			.flatMap(browser -> Stream.concat(Stream.of("wicket"),
				Stream.of(FrameworkTabs.FRAMEWORKS)).map(tab -> Arguments.of(browser, tab)));
	}

	@ParameterizedTest
	@MethodSource("tabs")
	void addsEditsAndDeletesInAModalDialog(SeleniumBrowser browser, String tab)
	{
		openTab(browser, tab);
		wait.until(d -> names().contains("Ada Lovelace"));

		// Escape closes the dialog and the focus goes back to the button
		WebElement add = panel().findElement(By.xpath(".//button[normalize-space()='Add member']"));
		add.click();
		WebElement dialog = dialog();
		assertEquals("New member", dialog.findElement(By.cssSelector(".modal-dialog-header"))
			.getText());
		wait.until(d -> "name".equals(d.switchTo().activeElement()
			.getDomAttribute("autocomplete")));
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(d -> panel().findElements(By.cssSelector(".modal-dialog")).isEmpty());
		wait.until(d -> "Add member".equals(d.switchTo().activeElement().getText()));

		// saved empty, the messages of the Wicket form
		add = panel().findElement(By.xpath(".//button[normalize-space()='Add member']"));
		add.click();
		save();
		wait.until(d -> dialog().getText().contains("'Name' is required."));
		assertTrue(dialog().getText().contains("'Email' is required."), dialog().getText());

		dialog().findElement(NAME).sendKeys("Margaret Hamilton");
		dialog().findElement(EMAIL).sendKeys("margaret@example.com");
		save();
		wait.until(d -> names().contains("Margaret Hamilton"));
		wait.until(d -> panel().getText().contains("Added Margaret Hamilton."));

		row("Margaret Hamilton").findElement(By.xpath(".//button[normalize-space()='Edit']"))
			.click();
		assertEquals("Edit Margaret Hamilton", dialog().findElement(
			By.cssSelector(".modal-dialog-header")).getText());
		WebElement name = dialog().findElement(NAME);
		name.sendKeys(Keys.END);
		name.sendKeys(Keys.BACK_SPACE.toString().repeat("Hamilton".length()) + "H.");
		save();
		wait.until(d -> names().contains("Margaret H."));

		row("Margaret H.").findElement(By.xpath(".//button[normalize-space()='Delete']")).click();
		assertTrue(dialog().getText().contains("Delete Margaret H. from the team?"));
		dialog().findElement(By.xpath(".//button[normalize-space()='Delete']")).click();
		wait.until(d -> !names().contains("Margaret H."));
		assertNoErrorsButTheInvalidMember();
	}

	/**
	 * Fails on errors in Chrome's console but the 400 the team resource answers the member saved
	 * empty with, which Chrome logs as any failed request.
	 */
	private void assertNoErrorsButTheInvalidMember()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<String> errors = driver.manage().logs().get("browser").getAll().stream()
			.filter(entry -> entry.getLevel().intValue() >= Level.SEVERE.intValue())
			.map(LogEntry::getMessage)
			.filter(message -> !message.contains("/favicon.ico"))
			.filter(message -> !(message.contains("/api/team") && message.contains("400")))
			.toList();
		assertEquals(List.of(), errors, "errors in the console");
	}

	private void openTab(SeleniumBrowser browser, String tab)
	{
		if (!"wicket".equals(tab))
		{
			open(browser, tab, "modal-dialog");
			return;
		}
		this.browser = browser;
		this.framework = tab;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/modal-dialog");
	}

	/**
	 * @return the panel of the tab
	 */
	private WebElement panel()
	{
		return driver.findElement(By.cssSelector("[data-framework-panel='" + framework + "']"));
	}

	private WebElement dialog()
	{
		return wait.until(d -> d.findElements(By.cssSelector(
			".dialog-theme-default .modal-dialog-overlay > .modal-dialog[role='dialog'], " +
				".dialog-theme-default .modal-dialog-overlay > .modal-dialog")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElse(null));
	}

	private void save()
	{
		dialog().findElement(By.xpath(".//button[normalize-space()='Save']")).click();
	}

	private List<String> names()
	{
		return panel().findElements(By.cssSelector(".team-demo__name")).stream()
			.map(WebElement::getText).toList();
	}

	private WebElement row(String name)
	{
		return panel().findElements(By.cssSelector(".team-demo__table tbody tr")).stream()
			.filter(row -> row.findElement(By.cssSelector(".team-demo__name")).getText()
				.equals(name))
			.findFirst().orElseThrow();
	}
}
