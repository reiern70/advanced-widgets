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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.veil.PageLoadingVeilBehavior;

/**
 * The veil over a page while it loads ({@link PageLoadingVeilBehavior}), in Chrome and in Firefox,
 * on the pages of the demo, whose scripts are made slow by a deferred script the server answers
 * late ({@link SlowScript}): the browser shows the page, but runs the domready handlers that
 * initialize its components only after that script. A click on the page meanwhile does nothing,
 * the spinner shows after its delay, and the veil is gone once the scripts have run; a script that
 * never ends leaves the page veiled only until the timeout of the style sheet. All of it under the
 * demo's strict Content Security Policy. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PageLoadingVeilBrowserTest extends JettyTestCase
{
	/** How long the scripts of the pages take, for the test of a page that gets ready. */
	private static final Duration SLOW = Duration.ofSeconds(8);

	/** How long the scripts of the pages take, for the test of the timeout: much longer than it. */
	private static final Duration NEVER = Duration.ofSeconds(30);

	/** The timeout of the veil's style sheet. */
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final Duration slowScript;

	private SeleniumBrowser browser;

	private WebDriver driver;

	/**
	 * @param test
	 *            the test about to run, whose web application gets scripts as slow as it needs
	 */
	PageLoadingVeilBrowserTest(TestInfo test)
	{
		slowScript = test.getTestMethod()
			.map(Method::getName)
			.filter("aScriptThatNeverEndsLeavesThePageVeiledUntilTheTimeoutOnly"::equals)
			.isPresent() ? NEVER : SLOW;
	}

	@Override
	protected void configure(WebAppContext web)
	{
		web.setInitParameter(SlowScript.PARAMETER, String.valueOf(slowScript.toMillis()));
	}

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
	void aClickBeforeThePageIsReadyDoesNothing(SeleniumBrowser browser)
	{
		WebElement home = openVeiledPage(browser);
		assertEquals("0.3s, 0s, 10s", veil("::after", "animationDelay"),
			"the spinner waits for its delay, the timeout for its own");
		new WebDriverWait(driver, Duration.ofSeconds(3))
			.until(d -> "1".equals(veil("::after", "opacity")));
		assertEquals("wait", veil("::before", "cursor"));

		// a click on the link to the home page reaches the veil, not the link
		assertTrue(isCovered(home), "the veil is over the link");
		new Actions(driver).moveToElement(home).click().perform();
		pause(Duration.ofMillis(1500));
		assertTrue(driver.getCurrentUrl().contains("/dropdown"), driver.getCurrentUrl());

		// once the scripts have run, the veil is gone, and the link takes the click
		new WebDriverWait(driver, SLOW.plusSeconds(10)).until(d -> !isVeiled());
		assertFalse(isCovered(home), "the veil is gone");
		home.click();
		new WebDriverWait(driver, Duration.ofSeconds(10))
			.until(d -> !d.getCurrentUrl().contains("/dropdown"));
		assertNoPolicyViolations();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void aScriptThatNeverEndsLeavesThePageVeiledUntilTheTimeoutOnly(SeleniumBrowser browser)
	{
		WebElement home = openVeiledPage(browser);
		long start = System.nanoTime();

		new WebDriverWait(driver, TIMEOUT.plusSeconds(10))
			.withMessage(() -> "the veil still shows: " + animations())
			.until(d -> "hidden".equals(veil("::before", "visibility")));
		Duration veiled = Duration.ofNanos(System.nanoTime() - start);
		assertTrue(veiled.compareTo(TIMEOUT.minusSeconds(2)) > 0, "veiled for " + veiled);
		assertEquals("hidden", veil("::after", "visibility"));
		assertTrue(isVeiled(), "the scripts have not run yet");

		// the page takes the click again, its scripts still loading
		assertFalse(isCovered(home), "the veil is lifted");
		new Actions(driver).moveToElement(home).click().perform();
		new WebDriverWait(driver, Duration.ofSeconds(10))
			.until(d -> !d.getCurrentUrl().contains("/dropdown"));
		assertNoPolicyViolations();
	}

	/**
	 * Opens the drop-down demo in the browser, without waiting for it to load, and waits for its
	 * link to the home page to show, the page being veiled, and for the veil to be drawn, its
	 * style sheet loaded.
	 *
	 * @return the link to the home page in the top bar
	 */
	private WebElement openVeiledPage(SeleniumBrowser browser)
	{
		this.browser = browser;
		driver = browser.newDriver(1200, 900, PageLoadStrategy.NONE);
		driver.get("http://localhost:" + localPort + "/dropdown");
		WebElement home = new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
			List<WebElement> found = d.findElements(By.cssSelector("a.brand"));
			return !found.isEmpty() && found.get(0).isDisplayed() ? found.get(0) : null;
		});
		assertTrue(isVeiled(), "veiled from the first paint");
		new WebDriverWait(driver, Duration.ofSeconds(15))
			.until(d -> "wait".equals(veil("::before", "cursor")));
		return home;
	}

	/** @return the animations of the page, for a test that failed */
	private String animations()
	{
		return String.valueOf(script().executeScript("return document.getAnimations()" +
			".map(a => a.animationName + ' ' + a.playState + ' ' + Math.round(a.currentTime))" +
			".join(', ');"));
	}

	/** @return whether the body has the class of the veil */
	private boolean isVeiled()
	{
		return (Boolean)script().executeScript("return document.body.classList.contains(" +
			"arguments[0]);", PageLoadingVeilBehavior.CSS_CLASS);
	}

	/**
	 * @param pseudo
	 *            the pseudo-element of the body, {@code ::before} the veil, {@code ::after} the
	 *            spinner
	 * @param property
	 *            the property of its computed style
	 * @return the value
	 */
	private String veil(String pseudo, String property)
	{
		return (String)script().executeScript("return getComputedStyle(document.body, " +
			"arguments[0])[arguments[1]];", pseudo, property);
	}

	/** @return whether a click in the middle of the element reaches something else */
	private boolean isCovered(WebElement element)
	{
		return (Boolean)script().executeScript("const box = arguments[0].getBoundingClientRect();" +
			"const hit = document.elementFromPoint(box.left + box.width / 2," +
			" box.top + box.height / 2);" +
			"return !arguments[0].contains(hit);", element);
	}

	private JavascriptExecutor script()
	{
		return (JavascriptExecutor)driver;
	}

	private static void pause(Duration duration)
	{
		try
		{
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Fails on a violation of the Content Security Policy in Chrome's console; Firefox has none
	 * to read.
	 */
	private void assertNoPolicyViolations()
	{
		if (browser != SeleniumBrowser.CHROME)
		{
			return;
		}
		List<String> violations = driver.manage().logs().get("browser").getAll().stream()
			.map(entry -> entry.getMessage())
			.filter(message -> message.contains("Content Security Policy") ||
				message.contains("Content-Security-Policy"))
			.toList();
		assertEquals(List.of(), violations, "violations of the policy in the console");
	}
}
