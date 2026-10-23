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
package com.antiliacode.widgets.demo.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * Drives {@link SplitPanelPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core: dragging the dividers, the keyboard, a double click,
 * the collapse buttons, the Ajax keeping what the user changed and the check boxes switching the
 * third split panel. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class SplitPanelBrowserTest extends JettyTestCase
{
	private static final String START =
		"The files take 220 px of the width, the editor 65 % of the height.";

	private WebDriver driver;

	private WebDriverWait wait;

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
	@EnumSource(SeleniumBrowser.class)
	void sizesThePanesAsThePositionsSay(SeleniumBrowser browser)
	{
		open(browser);

		assertEquals(START, sizes());
		assertEquals(220, Splits.size(driver, layout()), 1);
		assertEquals(65, Splits.share(driver, editor()), 1);
		assertEquals("vertical", Splits.divider(layout()).getDomAttribute("aria-orientation"));
		assertEquals("horizontal", Splits.divider(editor()).getDomAttribute("aria-orientation"));
		assertEquals(String.valueOf(Math.round(220 / Splits.space(driver, layout()) * 100)),
			Splits.divider(layout()).getDomAttribute("aria-valuenow"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void onlyTheBodyOfAPaneScrollsWithTheScrollBarsOfTheWidgets(SeleniumBrowser browser)
	{
		open(browser);
		// the console takes the editor's height away, so that the code does not fit
		Splits.divider(editor()).sendKeys(Keys.HOME);

		assertEquals(0, Splits.scrollBody(driver, editor()), 0.5, "the title stays");
		assertFalse(editor().findElements(By.cssSelector(
			".antilia-split-first .antilia-scrollbar-track--y:not([hidden])")).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void followsTheDividersTheUserDragsAndKeepsThem(SeleniumBrowser browser)
	{
		open(browser);
		double width = Splits.space(driver, layout());

		Splits.drag(driver, Splits.divider(layout()), 80, 0);
		wait.until(d -> sizes().startsWith("The files take 300 px"));
		assertEquals(300, Splits.size(driver, layout()), 1);

		double height = Splits.space(driver, editor());
		Splits.drag(driver, Splits.divider(editor()), 0, -(int)Math.round(height * 0.25));
		wait.until(d -> sizes().endsWith("the editor 40 % of the height."));
		assertEquals(40, Splits.share(driver, editor()), 1);

		// beyond its limit, half the width
		Splits.drag(driver, Splits.divider(layout()), (int)Math.round(width * 0.6), 0);
		long half = Math.round(width / 2);
		wait.until(d -> Math.abs(Splits.size(driver, layout()) - half) <= 1);
		wait.until(d -> sizes().startsWith("The files take " + half + " px"));

		driver.navigate().refresh();
		waitForTheScript();
		assertEquals("The files take " + half + " px of the width, the editor 40 % of the height.",
			sizes());
		assertEquals(half, Splits.size(driver, layout()), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void keepsTheFilesInPixelsWhenTheWindowChanges(SeleniumBrowser browser)
	{
		open(browser);
		double share = Splits.share(driver, editor());
		double before = Splits.space(driver, layout());

		driver.manage().window().setSize(new Dimension(900, 1000));
		wait.until(d -> Splits.space(driver, layout()) < before - 100);

		assertEquals(220, Splits.size(driver, layout()), 1);
		assertEquals(share, Splits.share(driver, editor()), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void movesWithTheKeyboardAndGoesBackOnADoubleClick(SeleniumBrowser browser)
	{
		open(browser);
		WebElement divider = Splits.divider(layout());

		divider.sendKeys(Keys.ARROW_RIGHT);
		wait.until(d -> Splits.size(driver, layout()) > 230);
		divider.sendKeys(Keys.HOME);
		wait.until(d -> sizes().startsWith("The files take 140 px"));
		assertEquals(140, Splits.size(driver, layout()), 1);

		Splits.doubleClick(driver, divider);
		wait.until(d -> sizes().equals(START));
		assertEquals(220, Splits.size(driver, layout()), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void collapsesAPaneAndRestoresIt(SeleniumBrowser browser)
	{
		open(browser);

		Splits.collapse(driver, layout(), "first");
		wait.until(d -> sizes().startsWith("The files take none"));
		assertTrue(Splits.collapsed(layout(), "first"));
		assertEquals(0, Splits.size(driver, layout()), 1);
		assertEquals("0", Splits.divider(layout()).getDomAttribute("aria-valuenow"));

		// the other button restores the files, where they were
		Splits.collapse(driver, layout(), "second");
		wait.until(d -> sizes().equals(START));
		assertEquals(220, Splits.size(driver, layout()), 1);

		// Enter collapses the editor, giving the console all the height
		Splits.divider(editor()).sendKeys(Keys.ENTER);
		wait.until(d -> sizes().endsWith("the editor none of the height."));
		assertEquals(0, Splits.size(driver, editor()), 1);

		driver.navigate().refresh();
		waitForTheScript();
		assertTrue(Splits.collapsed(editor(), "first"), "kept in the page");
		Splits.divider(editor()).sendKeys(Keys.ENTER);
		wait.until(d -> sizes().equals(START));
		assertEquals(65, Splits.share(driver, editor()), 1);
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theCheckBoxesSwitchTheThirdSplitPanel(SeleniumBrowser browser)
	{
		open(browser);
		assertTrue(settings().getDomAttribute("class").contains("antilia-split-horizontal"));

		driver.findElement(By.name("vertical")).click();
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-split-vertical"));
		double height = Splits.space(driver, settings());
		Splits.drag(driver, Splits.divider(settings()), 0, -(int)Math.round(height * 0.2));
		assertEquals(30, Splits.share(driver, settings()), 1);

		driver.findElement(By.name("collapsible")).click();
		wait.until(d -> !settings().getDomAttribute("class").contains("antilia-split-collapsible"));
		assertTrue(Splits.divider(settings()).findElements(By.xpath("./*")).isEmpty());

		driver.findElement(By.name("resizable")).click();
		wait.until(d -> settings().getDomAttribute("class").contains("antilia-split-fixed"));
		assertEquals("true", Splits.divider(settings()).getDomAttribute("aria-disabled"));
		Splits.drag(driver, Splits.divider(settings()), 0, (int)Math.round(height * 0.3));
		assertEquals(30, Splits.share(driver, settings()), 1, "kept, and fixed");
		assertFalse(Splits.collapsed(settings(), "first"));
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/split-panel");
		waitForTheScript();
	}

	/**
	 * Waits for the script to have made split panels of all three: it gives their dividers a
	 * value, which the server does not render for the files' in pixels.
	 */
	private void waitForTheScript()
	{
		wait.until(d -> d.findElements(By.cssSelector(".antilia-split-divider[aria-valuenow]"))
			.size() == 3);
	}

	private WebElement layout()
	{
		return driver.findElement(By.cssSelector(".split-demo__layout > .antilia-split"));
	}

	private WebElement editor()
	{
		return layout().findElement(By.cssSelector(".antilia-split-second > .antilia-split"));
	}

	private WebElement settings()
	{
		return driver.findElement(By.cssSelector(".split-demo__settings > .antilia-split"));
	}

	private String sizes()
	{
		return driver.findElement(By.cssSelector(".split-demo__sizes")).getText();
	}
}
