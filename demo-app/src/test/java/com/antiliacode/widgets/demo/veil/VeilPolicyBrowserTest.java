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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * Typing in a filter and in the editors, dragging a split panel's divider and moving a desktop's
 * window report to the server in the background and never raise the page veil of
 * {@link VeilPolicyPage}, while a click on a link does, in Chrome and in Firefox. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class VeilPolicyBrowserTest extends JettyTestCase
{
	/**
	 * Counts the veils put over the page, Wicket's and the one of the library's add-on, in
	 * {@code window.pageVeils}, and the Ajax requests sent, in {@code window.requests}, those
	 * carrying the parameter of no veil in {@code window.unveiled}.
	 */
	private static final String RECORD = """
		window.pageVeils = 0; window.requests = 0; window.unveiled = 0;
		new MutationObserver(function (records) {
			records.forEach(function (record) {
				record.addedNodes.forEach(function (node) {
					if (node.classList && (node.classList.contains('wicket-veil') ||
						node.classList.contains('antilia-veil-guard'))) {
						window.pageVeils++;
					}
				});
			});
		}).observe(document.body, { childList: true });
		Wicket.Event.subscribe('/ajax/call/beforeSend', function (event, attrs) {
			window.requests++;
			var ep = attrs.ep;
			var marked = Array.isArray(ep) ?
				ep.some(function (p) { return p && p.name === 'wicket_nb'; }) :
				!!ep && Object.prototype.hasOwnProperty.call(ep, 'wicket_nb');
			if (marked) {
				window.unveiled++;
			}
		});
		""";

	private WebDriver driver;

	private WebDriverWait wait;

	@Override
	@BeforeEach
	public void before() throws Exception
	{
		super.before();
		VeilRace.reset();
	}

	@Override
	@AfterEach
	public void after() throws Exception
	{
		VeilRace.reset();
		if (driver != null)
		{
			driver.quit();
		}
		super.after();
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1200, 1100);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/wicket/bookmarkable/" +
			VeilPolicyPage.class.getName());
		wait.until(d -> Boolean.TRUE.equals(script("return document.readyState === 'complete' " +
			"&& typeof Wicket.Veil === 'object';")));
		script(RECORD);
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private int number(String global)
	{
		return ((Number)script("return window." + global + ";")).intValue();
	}

	/**
	 * Waits until the page has no request under way, and still has none a moment later.
	 */
	private void awaitQuiet() throws InterruptedException
	{
		String quiet = "return Object.keys(Wicket.channelManager.channels).length === 0 && " +
			"!document.querySelector('.wicket-veil, .antilia-veil-guard');";
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
		Thread.sleep(700);
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
	}

	private void assertThePageWasLeftAlone() throws InterruptedException
	{
		awaitQuiet();
		assertEquals(0, number("pageVeils"), "the page veiled");
		assertTrue(number("requests") >= 1, "the server was not told");
		assertEquals(number("requests"), number("unveiled"), "a request veiled");
	}

	private void drag(WebElement element, int dx, int dy)
	{
		script("arguments[0].scrollIntoView({block: 'center'});", element);
		new Actions(driver).moveToElement(element).clickAndHold().moveByOffset(dx / 2, dy / 2)
			.moveByOffset(dx - dx / 2, dy - dy / 2).release().perform();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void typingInTheFilterLeavesThePageAlone(SeleniumBrowser browser) throws Exception
	{
		open(browser);
		new Actions(driver).click(driver.findElement(By.name("filter"))).sendKeys("tides")
			.perform();
		wait.until(d -> VeilRace.runs("filter") >= 1);

		assertThePageWasLeftAlone();

		// what the user does is veiled: the recording sees a veil
		WebElement act = driver.findElement(By.linkText("Act"));
		script("arguments[0].scrollIntoView({block: 'center'});", act);
		new Actions(driver).click(act).perform();
		wait.until(d -> VeilRace.runs("act") == 1);
		awaitQuiet();
		assertTrue(number("pageVeils") >= 1, "no veil over a click");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void typingInTheCodeEditorLeavesThePageAlone(SeleniumBrowser browser) throws Exception
	{
		open(browser);
		WebElement content = wait.until(d -> d.findElements(By.cssSelector(
			".veil-policy__code .cm-content")).stream().findFirst().orElse(null));
		new Actions(driver).click(content).sendKeys(" // the moon").perform();
		wait.until(d -> number("requests") >= 1);

		assertThePageWasLeftAlone();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void typingInTheRichTextEditorLeavesThePageAlone(SeleniumBrowser browser) throws Exception
	{
		open(browser);
		WebElement content = wait.until(d -> d.findElements(By.cssSelector(
			".veil-policy__rich .ProseMirror")).stream().findFirst().orElse(null));
		new Actions(driver).click(content).sendKeys(" And the moon.").perform();
		wait.until(d -> number("requests") >= 1);

		assertThePageWasLeftAlone();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void draggingTheDividerOfTheSplitPanelLeavesThePageAlone(SeleniumBrowser browser)
		throws Exception
	{
		open(browser);
		WebElement divider = wait.until(d -> d.findElements(By.cssSelector(
			".veil-policy__split .antilia-split-divider[aria-valuenow]")).stream().findFirst()
			.orElse(null));
		drag(divider, 80, 0);
		wait.until(d -> number("requests") >= 1);

		assertThePageWasLeftAlone();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void movingAWindowOfTheDesktopLeavesThePageAlone(SeleniumBrowser browser) throws Exception
	{
		open(browser);
		WebElement title = wait.until(d -> d.findElements(By.cssSelector(
			".veil-policy__desktop .antilia-window-title")).stream().findFirst().orElse(null));
		drag(title, 100, 50);
		wait.until(d -> number("requests") >= 1);

		assertThePageWasLeftAlone();
	}
}
