/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.modal;

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
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.Toasts;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;

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
		assertEquals("New member", dialog.findElement(By.cssSelector(".modal-dialog-title"))
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
		// in Wicket the page's message is a notification, in the apps under the team
		wait.until(d -> ("wicket".equals(tab) ? Toasts.latest(d) : panel().getText())
			.contains("Added Margaret Hamilton."));

		row("Margaret Hamilton").findElement(By.xpath(".//button[normalize-space()='Edit']"))
			.click();
		assertEquals("Edit Margaret Hamilton", dialog().findElement(
			By.cssSelector(".modal-dialog-title")).getText());
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

	@ParameterizedTest
	@MethodSource("tabs")
	void closesFoldsAndDragsTheDialogAskingBeforeDiscardingChanges(SeleniumBrowser browser,
		String tab)
	{
		openTab(browser, tab);
		wait.until(d -> names().contains("Ada Lovelace"));

		// Cancel and the close button close a form not modified, without asking
		addMember();
		dialog().findElement(By.xpath(".//*[contains(@class,'modal-dialog-footer')]" +
			"//button[normalize-space()='Cancel']")).click();
		wait.until(d -> dialogs().isEmpty());
		addMember();
		WebElement close = dialog().findElement(By.cssSelector(".modal-dialog-header " +
			".modal-dialog-close"));
		assertEquals("Close", close.getDomAttribute("aria-label"));
		close.click();
		wait.until(d -> dialogs().isEmpty());

		// a modified form asks first: dismissed it stays open, accepted it closes
		addMember();
		dialog().findElement(NAME).sendKeys("Grace");
		dialog().findElement(By.cssSelector(".modal-dialog-close")).click();
		Alert alert = wait.until(ExpectedConditions.alertIsPresent());
		assertEquals("Discard the changes?", alert.getText());
		alert.dismiss();
		assertEquals(1, dialogs().size());
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		wait.until(ExpectedConditions.alertIsPresent()).accept();
		wait.until(d -> dialogs().isEmpty());

		// a double click on the title folds the dialog to its header, another unfolds it
		addMember();
		WebElement title = dialog().findElement(By.cssSelector(".modal-dialog-title"));
		new Actions(driver).doubleClick(title).perform();
		wait.until(d -> dialog().getDomAttribute("class").contains("modal-dialog-folded"));
		assertTrue(!dialog().findElement(By.cssSelector(".modal-dialog-footer")).isDisplayed());
		new Actions(driver).doubleClick(title).perform();
		wait.until(d -> !dialog().getDomAttribute("class").contains("modal-dialog-folded"));

		// dragging the title moves the dialog
		Point before = dialog().getLocation();
		new Actions(driver).dragAndDropBy(title, -60, 40).perform();
		wait.until(d -> !dialog().getLocation().equals(before));
		Point after = dialog().getLocation();
		assertEquals(before.getX() - 60, after.getX(), 2);
		assertEquals(before.getY() + 40, after.getY(), 2);

		// a click on the page around the dialog closes it, in this demo
		new Actions(driver).moveToLocation(5, 5).click().perform();
		wait.until(d -> dialogs().isEmpty());
		assertNoErrorsButTheInvalidMember();
	}

	@ParameterizedTest
	@MethodSource("tabs")
	void opensADialogWithTheOptionsChosenAndNestsDialogsInIt(SeleniumBrowser browser, String tab)
	{
		openTab(browser, tab);
		wait.until(d -> names().contains("Ada Lovelace"));

		// Escape off, a large dialog
		WebElement options = panel().findElement(By.cssSelector(".dialog-options"));
		options.findElement(By.xpath(".//label[normalize-space()='Escape closes it']/input"))
			.click();
		options.findElement(By.xpath(".//label[normalize-space()='Large']/input")).click();
		options.findElement(By.xpath(".//button[normalize-space()='Open a dialog']")).click();
		WebElement first = dialogTitled("Dialog 1");
		assertTrue(first.getDomAttribute("class").contains("modal-dialog-large"));
		assertEquals(1, dialogs().size());

		// Escape leaves it open; dragged, the dialog nested in it still covers the window
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		new Actions(driver).dragAndDropBy(first.findElement(By.cssSelector(".modal-dialog-title")),
			-80, 30).perform();
		first.findElement(By.xpath(".//button[normalize-space()='Open a nested dialog']")).click();
		WebElement second = dialogTitled("Dialog 2");
		assertEquals(2, dialogs().size());
		WebElement overlay = second.findElement(By.xpath(
			"./ancestor::*[contains(@class,'modal-dialog-overlay')][1]"));
		assertEquals(0, overlay.getRect().getX());
		assertEquals(0, overlay.getRect().getY());
		Number width = (Number)((org.openqa.selenium.JavascriptExecutor)driver).executeScript(
			"return document.documentElement.clientWidth;");
		assertEquals(width.intValue(), overlay.getRect().getWidth(), 1);

		// and another in that one; Close and OK close the one on top only
		second.findElement(By.xpath(".//button[normalize-space()='Open a nested dialog']"))
			.click();
		WebElement third = dialogTitled("Dialog 3");
		assertTrue(third.getText().contains("as deep as this demo goes"));
		third.findElement(By.xpath("./*/*[contains(@class,'modal-dialog-footer')]" +
			"//button[normalize-space()='Close']")).click();
		wait.until(d -> dialogs().size() == 2);
		dialogTitled("Dialog 2").findElement(By.xpath("./*/*[contains(@class," +
			"'modal-dialog-footer')]//button[normalize-space()='OK']")).click();
		wait.until(d -> dialogs().size() == 1);
		dialogTitled("Dialog 1").findElement(By.xpath("./*/*[contains(@class," +
			"'modal-dialog-footer')]//button[normalize-space()='OK']")).click();
		wait.until(d -> dialogs().isEmpty());
		assertNoErrorsButTheInvalidMember();
	}

	/**
	 * @return the dialog shown with a title
	 */
	private WebElement dialogTitled(String title)
	{
		return wait.until(d -> d.findElements(By.xpath("//*[contains(@class,'modal-dialog-title')]" +
			"[normalize-space()='" + title + "']/ancestor::*[@role='dialog'][1]")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElse(null));
	}

	private void addMember()
	{
		panel().findElement(By.xpath(".//button[normalize-space()='Add member']")).click();
		wait.until(d -> "name".equals(d.switchTo().activeElement()
			.getDomAttribute("autocomplete")));
	}

	private List<WebElement> dialogs()
	{
		return driver.findElements(By.cssSelector(".modal-dialog")).stream()
			.filter(WebElement::isDisplayed).toList();
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
