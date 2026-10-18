/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Opens and closes the dialogs of the pages in the browser alone, in Chrome and in Firefox: what
 * runs where from the footer, what a demo is about from the index of the home page, and the
 * explanation of a demo from its page; Escape, the × and the button of the footer close them.
 * Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
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

		// only the body scrolls, the header stays where it is
		WebElement header = driver.findElement(By.cssSelector("#what-runs-where .modal-dialog-header"));
		WebElement body = driver.findElement(By.cssSelector("#what-runs-where .modal-dialog-body"));
		String top = "return arguments[0].getBoundingClientRect().top;";
		Object before = ((JavascriptExecutor)driver).executeScript(top, header);
		new Actions(driver).scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(header), 0, 600)
			.perform();
		new Actions(driver).scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(body), 0, 600)
			.perform();
		wait.until(d -> ((Number)((JavascriptExecutor)d).executeScript(
			"return arguments[0].scrollTop;", body)).intValue() > 0);
		assertEquals(before, ((JavascriptExecutor)driver).executeScript(top, header));
		assertEquals(0L, ((Number)((JavascriptExecutor)driver).executeScript(
			"return arguments[0].closest('.modal-dialog-content').scrollTop;", body)).longValue());

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
	void theDetailsOfADemoOpenFromTheIndex(SeleniumBrowser browser)
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

		driver.findElement(By.cssSelector("[data-browser-modal='demo-details-contacts'].index__details"))
			.click();
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
