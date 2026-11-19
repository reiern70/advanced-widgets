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
package com.antiliacode.widgets.demo.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * The facets of the {@link ContactsPage}, {@link com.antiliacode.widgets.collapsible.CollapsiblePanel
 * collapsible panels} rendered expanded that remember the user's choice, report a toggle only when
 * the user expands or collapses one: not as the page loads, nor when Ajax renders them again, which
 * the browser answers with a {@code toggle} of each {@code <details open>} it inserts (#148). In
 * Chrome and in Firefox; runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ContactsFacetsBrowserTest extends JettyTestCase
{
	/**
	 * The toggles reported since the page loaded, in the browser's record of what it fetched: the
	 * requests to the behavior of a facet's {@code <details>}, whose path ends in
	 * {@code collapsible-details}.
	 */
	private static final String TOGGLES = "return performance.getEntriesByType('resource')" +
		".filter(function (entry) { return /-collapsible-details(&|$)/.test(entry.name); })" +
		".length;";

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

	private void open(SeleniumBrowser browser) throws InterruptedException
	{
		driver = browser.newDriver(1400, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/address-book");
		wait.until(d -> Boolean.TRUE.equals(script("return document.readyState === 'complete' " +
			"&& typeof Wicket.Ajax === 'object';")));
		// the record keeps 250 entries by default: had the page's own files filled it, the toggles
		// sent as it loaded would be missing
		assertTrue(((Number)script("return performance.getEntriesByType('resource').length;"))
			.intValue() < 200, "the browser's record of what it fetched is full");
		script("performance.setResourceTimingBufferSize(10000);");
		awaitQuiet();
	}

	private Object script(String script, Object... arguments)
	{
		return ((JavascriptExecutor)driver).executeScript(script, arguments);
	}

	private int toggles()
	{
		return ((Number)script(TOGGLES)).intValue();
	}

	private List<WebElement> facets()
	{
		return driver.findElements(By.cssSelector(".contacts-facet details"));
	}

	/**
	 * Waits until the page has no request under way, and still has none a moment later, a toggle
	 * of an inserted {@code <details>} being fired after the response was processed.
	 */
	private void awaitQuiet() throws InterruptedException
	{
		String quiet = "return Object.keys(Wicket.channelManager.channels).length === 0;";
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
		Thread.sleep(700);
		wait.until(d -> Boolean.TRUE.equals(script(quiet)));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFacetsRenderedOpenReportNothingAsThePageLoads(SeleniumBrowser browser)
		throws Exception
	{
		open(browser);

		assertEquals(3, facets().size());
		facets().forEach(details -> assertNotNull(details.getDomAttribute("open")));
		assertEquals(0, toggles(), "toggles reported as the page loaded");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFacetsRenderedAgainByAjaxReportNothing(SeleniumBrowser browser) throws Exception
	{
		open(browser);
		// the elements rendered now, which Ajax replaces
		script("document.querySelectorAll('.contacts-facet details')" +
			".forEach(function (details) { details.antiliaOld = true; });");

		// a value chosen renders every facet again, expanded
		WebElement value = driver.findElement(
			By.cssSelector(".contacts-facet input[type=checkbox]"));
		script("arguments[0].scrollIntoView({block: 'center'});", value);
		value.click();
		wait.until(d -> Boolean.TRUE.equals(script("var all = document.querySelectorAll(" +
			"'.contacts-facet details'); return all.length === 3 && " +
			"Array.prototype.every.call(all, function (details) { return !details.antiliaOld; });")));
		awaitQuiet();

		facets().forEach(details -> assertNotNull(details.getDomAttribute("open")));
		assertEquals(0, toggles(), "toggles reported for the facets rendered again");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theUsersToggleIsReportedOnceAndRemembered(SeleniumBrowser browser) throws Exception
	{
		open(browser);

		WebElement summary = driver.findElement(By.cssSelector(".contacts-facet summary"));
		script("arguments[0].scrollIntoView({block: 'center'});", summary);
		summary.click();
		wait.until(d -> toggles() == 1);
		awaitQuiet();
		assertEquals(1, toggles());

		// the page rendered again keeps the facet collapsed, and reports nothing
		driver.navigate().refresh();
		wait.until(d -> Boolean.TRUE.equals(script("return document.readyState === 'complete' " +
			"&& typeof Wicket.Ajax === 'object';")));
		awaitQuiet();
		assertNull(facets().get(0).getDomAttribute("open"));
		assertNotNull(facets().get(1).getDomAttribute("open"));
		assertEquals(0, toggles(), "toggles reported as the page loaded again");

		summary = driver.findElement(By.cssSelector(".contacts-facet summary"));
		summary.click();
		wait.until(d -> toggles() == 1);
		awaitQuiet();
		assertEquals(1, toggles());
	}
}
