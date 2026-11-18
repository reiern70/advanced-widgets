/*
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
package com.antiliacode.widgets.demo.veil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.ComponentNotFoundException;
import org.apache.wicket.util.lang.Exceptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * Clicks and keys sent twice to {@link VeilRacePage} while the first request is held at its gate
 * ({@link VeilRace}), by real input ({@link Actions}: a script's {@code click()} goes round a
 * veil), in Chrome and in Firefox. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class VeilRaceBrowserTest extends JettyTestCase
{
	/** The name of the demo application, as its filter in web.xml. */
	private static final String APPLICATION = "wicket.demo-app";

	private WebDriver driver;

	private WebDriverWait wait;

	@Override
	@BeforeEach
	public void before() throws Exception
	{
		super.before();
		VeilRace.reset();
		Application.get(APPLICATION).getRequestCycleListeners().add(VeilRace.EXCEPTION_RECORDER);
	}

	@Override
	@AfterEach
	public void after() throws Exception
	{
		// a request still held lets the server stop
		VeilRace.reset();
		if (driver != null)
		{
			driver.quit();
		}
		super.after();
	}

	private void open(SeleniumBrowser browser, String query)
	{
		driver = browser.newDriver(1000, 800);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/wicket/bookmarkable/" +
			VeilRacePage.class.getName() + "?" + query);
		wait.until(d -> Boolean.TRUE.equals(script(
			"return document.readyState === 'complete' && !!(window.Wicket && Wicket.Ajax);")));
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private WebElement button(String name)
	{
		return driver.findElement(By.cssSelector("button[name$='" + name + "']"));
	}

	/**
	 * Waits until the server ran an action as many times.
	 */
	private void awaitRuns(String action, int runs)
	{
		wait.until(d -> VeilRace.runs(action) >= runs);
	}

	/**
	 * Waits until the page has no request under way nor queued, nor a veil, and still has none a
	 * moment later: a request queued or a click given back after the veil has had its turn.
	 */
	private void awaitQuiet() throws InterruptedException
	{
		String quiet = "return !window.Wicket || !Wicket.channelManager || " +
			"(Object.keys(Wicket.channelManager.channels).length === 0 && " +
			"!document.querySelector('.wicket-veil, .antilia-veil-guard'));";
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
		Thread.sleep(700);
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
	}

	private static boolean isComponentNotFound(Exception exception)
	{
		return Exceptions.findCause(exception, ComponentNotFoundException.class) != null;
	}

	/**
	 * Case 1 of #142 without a veil: the second click of a double click waits in the queue of the
	 * Ajax channel while the first request runs, and is sent once its answer is in. That answer
	 * made the row anew, so the element the click was on is gone; an element with the same markup
	 * id took its place, so Wicket's Ajax, which drops a queued request whose element is no longer
	 * in the page, sends it, to the path of a button that is no more.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDoubleClickWithoutAVeilEndsInAComponentNotFoundException(SeleniumBrowser browser)
		throws Exception
	{
		open(browser, "veil=off");
		VeilRace.close("approve");
		new Actions(driver).doubleClick(button("approve")).perform();
		awaitRuns("approve", 1);
		VeilRace.open("approve");

		wait.until(d -> VeilRace.exceptions().stream()
			.anyMatch(VeilRaceBrowserTest::isComponentNotFound));
		assertEquals(1, VeilRace.runs("approve"));
	}

	/**
	 * Case 1 of #142 with the veil: the second click falls on the veil.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDoubleClickWithTheVeilRunsOnce(SeleniumBrowser browser) throws Exception
	{
		open(browser, "veil=on");
		VeilRace.close("approve");
		new Actions(driver).doubleClick(button("approve")).perform();
		awaitRuns("approve", 1);
		VeilRace.open("approve");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("approve"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * Without a veil, the second click on a button its answer hides is not sent: Wicket's Ajax
	 * drops a queued request whose element is a placeholder when its turn comes.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDoubleClickOnAButtonItsAnswerHidesRunsOnceWithoutAVeil(SeleniumBrowser browser)
		throws Exception
	{
		open(browser, "veil=off");
		VeilRace.close("hide");
		new Actions(driver).doubleClick(button("hide")).perform();
		awaitRuns("hide", 1);
		VeilRace.open("hide");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("hide"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * Case 2 of #142: Enter pressed twice on the focused button.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void enterPressedTwiceWithTheVeilRunsOnce(SeleniumBrowser browser) throws Exception
	{
		open(browser, "veil=on");
		script("arguments[0].focus();", button("save"));
		VeilRace.close("save");
		new Actions(driver).sendKeys(Keys.ENTER).perform();
		awaitRuns("save", 1);
		new Actions(driver).sendKeys(Keys.ENTER).perform();
		VeilRace.open("save");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("save"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * Case 3 of #142: a click while a request the veil leaves alone runs waits in the queue, and a
	 * second click comes before the first one's request is sent.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aSecondClickWhileAnUnveiledRequestRunsRunsOnce(SeleniumBrowser browser) throws Exception
	{
		open(browser, "veil=on");
		VeilRace.close("slow");
		new Actions(driver).click(driver.findElement(By.linkText("Slow"))).perform();
		awaitRuns("slow", 1);
		new Actions(driver).click(button("save")).perform();
		new Actions(driver).click(button("save")).perform();
		VeilRace.open("slow");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("save"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * Case 4 of #142: the mouse goes down on "Save" while the field has a change, whose round trip
	 * raises the veil before the mouse comes up.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aClickLeavingAFieldWithARoundTripRunsOnce(SeleniumBrowser browser) throws Exception
	{
		open(browser, "veil=on");
		WebElement field = driver.findElement(By.name("field"));
		new Actions(driver).click(field).sendKeys("12").perform();
		VeilRace.close("field");
		new Actions(driver).click(button("save")).perform();
		awaitRuns("field", 1);
		VeilRace.open("field");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("field"));
		assertEquals(1, VeilRace.runs("save"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * #143 without a veil: the second click of a double click on a button that stays is sent, with
	 * the version the form had at the click, which the server already took.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aDoubleClickOnAVersionedFormRunsOnceWithoutAVeil(SeleniumBrowser browser)
		throws Exception
	{
		open(browser, "veil=off&version=on");
		VeilRace.close("save");
		new Actions(driver).doubleClick(button("save")).perform();
		awaitRuns("save", 1);
		VeilRace.open("save");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("save"));
		assertTrue(driver.findElement(By.cssSelector(".feedbackPanelERROR")).getText()
			.contains("already sent"));
		assertEquals(List.of(), VeilRace.exceptions());
	}

	/**
	 * #143 without a veil: Enter pressed twice on the focused button.
	 */
	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void enterPressedTwiceOnAVersionedFormRunsOnceWithoutAVeil(SeleniumBrowser browser)
		throws Exception
	{
		open(browser, "veil=off&version=on");
		script("arguments[0].focus();", button("save"));
		VeilRace.close("save");
		new Actions(driver).sendKeys(Keys.ENTER).perform();
		awaitRuns("save", 1);
		new Actions(driver).sendKeys(Keys.ENTER).perform();
		VeilRace.open("save");

		awaitQuiet();
		assertEquals(1, VeilRace.runs("save"));
		assertTrue(driver.findElement(By.cssSelector(".feedbackPanelERROR")).getText()
			.contains("already sent"));
		assertEquals(List.of(), VeilRace.exceptions());
	}
}
