/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Opens and closes the dialogs of the pages in the browser alone, in Chrome and in Firefox: what
 * runs where from the footer, what a demo is about from the index and the carousel of the home
 * page, and the explanation of a demo from its page; Escape, the × and the button of the footer
 * close them. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class BrowserModalBrowserTest extends FrameworkAppsTestCase
{
	private void open(SeleniumBrowser browser, String path)
	{
		this.browser = browser;
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + path);
	}

	private WebElement dialog(String id)
	{
		return driver.findElement(By.cssSelector("#" + id + " .modal-dialog"));
	}

	private void waitOpen(String id)
	{
		wait.until(d -> dialog(id).isDisplayed());
		// the focus goes into the dialog
		wait.until(d -> (Boolean)((org.openqa.selenium.JavascriptExecutor)d).executeScript(
			"return document.getElementById(arguments[0]).contains(document.activeElement);", id));
	}

	private void waitClosed(String id)
	{
		wait.until(d -> !dialog(id).isDisplayed());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFooterOpensWhatRunsWhereWideAndEscapeClosesIt(SeleniumBrowser browser)
	{
		open(browser, "");

		WebElement link = driver.findElement(By.cssSelector(".footer__link"));
		link.click();
		waitOpen("what-runs-where");
		assertTrue(dialog("what-runs-where").getSize().getWidth() > 1100,
			"wide: " + dialog("what-runs-where").getSize());
		assertEquals(Demos.all().size(), driver.findElements(
			By.cssSelector("#what-runs-where .support-matrix__table tbody tr")).size());

		new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		waitClosed("what-runs-where");
		wait.until(d -> link.equals(d.switchTo().activeElement()));

		// and again, from the button at the top
		driver.findElement(By.cssSelector(".showcase__secondary")).click();
		waitOpen("what-runs-where");
		driver.findElement(By.cssSelector("#what-runs-where .modal-dialog-close")).click();
		waitClosed("what-runs-where");
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theDetailsOfADemoOpenFromTheIndexAndTheCarousel(SeleniumBrowser browser)
	{
		open(browser, "");

		driver.findElement(By.cssSelector("[data-browser-modal='demo-details-tree'].index__details"))
			.click();
		waitOpen("demo-details-tree");
		assertEquals("Tree", driver.findElement(
			By.cssSelector("#demo-details-tree .modal-dialog-title")).getText());
		assertTrue(driver.findElement(By.cssSelector("#demo-details-tree .demo-details__explanation"))
			.getText().contains("tree of life"));
		driver.findElement(By.cssSelector("#demo-details-tree [data-modal-close]")).click();
		waitClosed("demo-details-tree");

		// the slide shown first is the Address Book's
		driver.findElement(By.cssSelector(".carousel__slide:first-child .carousel__details")).click();
		waitOpen("demo-details-contacts");
		driver.findElement(By.cssSelector("#demo-details-contacts .modal-dialog-footer a")).click();
		wait.until(d -> d.getCurrentUrl().contains("/address-book"));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDemoOpensItsExplanationInADialog(SeleniumBrowser browser)
	{
		open(browser, "tree");

		driver.findElement(By.cssSelector(".demo__about")).click();
		String id = driver.findElement(By.cssSelector(".demo__about"))
			.getAttribute("data-browser-modal");
		waitOpen(id);
		assertTrue(dialog(id).getText().contains("This is a demo of the two trees"));
		new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		waitClosed(id);
		assertNoErrors();
	}
}
