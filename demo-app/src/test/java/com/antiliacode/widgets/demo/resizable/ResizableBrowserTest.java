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
package com.antiliacode.widgets.demo.resizable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link ResizablePage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core, in Wicket and in the React and Svelte apps: dragging
 * the handles, the keyboard, a double click, the card taken away and put back in the size it had,
 * and the sizes the server keeps in the session in Wicket. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ResizableBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void resizesTheNoteByItsBottomEdge(SeleniumBrowser browser, String framework)
	{
		openResizable(browser, framework);
		WebElement note = box("note");
		long before = Math.round(rect(note, "height"));
		WebElement handle = handle(note);
		assertEquals("horizontal", handle.getDomAttribute("aria-orientation"));
		assertEquals("Drag, or use the arrow keys, to resize", handle.getDomAttribute("aria-label"));

		drag(handle, 0, 50);
		wait.until(d -> Math.abs(rect(box("note"), "height") - (before + 50)) <= 2);
		wait.until(d -> sizes().contains("the note " + (before + 50) + " px high"));
		handle(box("note")).sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> sizes().contains("the note " + (before + 60) + " px high"));

		// back to the height the page gives it
		new Actions(driver).doubleClick(handle(box("note"))).perform();
		wait.until(d -> sizes().contains("the note as the page makes it high"));
		assertEquals(before, Math.round(rect(box("note"), "height")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void resizesTheSidePanelWithTheKeyboardWithinItsLimits(SeleniumBrowser browser,
		String framework)
	{
		openResizable(browser, framework);
		WebElement handle = handle(box("side"));
		assertEquals("vertical", handle.getDomAttribute("aria-orientation"));

		handle.sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT));
		wait.until(d -> sizes().contains("the side panel 250 px wide"));
		handle(box("side")).sendKeys(Keys.HOME);
		wait.until(d -> sizes().contains("the side panel 140 px wide"));
		assertEquals(140, Math.round(rect(box("side"), "width")));
		handle(box("side")).sendKeys(Keys.END);
		wait.until(d -> sizes().contains("the side panel 520 px wide"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theCardComesBackInTheSizeItHad(SeleniumBrowser browser, String framework)
	{
		openResizable(browser, framework);
		assertEquals(320, Math.round(rect(box("card"), "width")));
		assertEquals(180, Math.round(rect(box("card"), "height")));

		drag(handle(box("card")), 80, 40);
		wait.until(d -> sizes().endsWith("the card 400 px × 220 px."));

		toggle().click();
		wait.until(d -> sizes().endsWith("the card taken away."));
		assertTrue(app().findElements(By.cssSelector(".resizable-demo__card")).isEmpty());
		toggle().click();
		wait.until(d -> sizes().endsWith("the card 400 px × 220 px."));
		wait.until(d -> Math.round(rect(box("card"), "width")) == 400);
		assertEquals(220, Math.round(rect(box("card"), "height")));
		assertNoErrors();
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theServerKeepsTheSizesInTheSession(SeleniumBrowser browser)
	{
		openResizable(browser, "wicket");
		handle(box("note")).sendKeys(Keys.ARROW_DOWN, Keys.ARROW_DOWN);
		handle(box("side")).sendKeys(Keys.chord(Keys.SHIFT, Keys.ARROW_RIGHT));
		wait.until(d -> sizes().contains("the side panel 250 px wide"));
		String sentence = sizes();
		long note = Math.round(rect(box("note"), "height"));

		driver.get("http://localhost:" + localPort + "/resizable");
		wait.until(d -> !d.findElements(By.cssSelector(".resizable-demo__card > " +
			".antilia-resizable-handle")).isEmpty());
		assertEquals(sentence, sizes());
		assertEquals(note, Math.round(rect(box("note"), "height")));
		assertEquals(250, Math.round(rect(box("side"), "width")));
		assertNoErrors();
	}

	private void openResizable(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "resizable");
		wait.until(d -> !app().findElements(By.cssSelector(".resizable-demo__card > " +
			".antilia-resizable-handle")).isEmpty());
	}

	private WebElement box(String name)
	{
		return app().findElement(By.cssSelector(".resizable-demo__" + name));
	}

	private static WebElement handle(WebElement box)
	{
		return box.findElement(By.cssSelector(":scope > .antilia-resizable-handle"));
	}

	private WebElement toggle()
	{
		return app().findElement(By.cssSelector(".resizable-demo__heading a, " +
			".resizable-demo__heading button"));
	}

	private String sizes()
	{
		return app().findElement(By.cssSelector(".resizable-demo__sizes")).getText();
	}

	private double rect(WebElement element, String dimension)
	{
		return ((Number)((JavascriptExecutor)driver).executeScript(
			"return arguments[0].getBoundingClientRect()[arguments[1]];", element, dimension))
			.doubleValue();
	}

	private void drag(WebElement handle, int dx, int dy)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", handle);
		new Actions(driver).moveToElement(handle).clickAndHold().moveByOffset(dx / 2, dy / 2)
			.moveByOffset(dx - dx / 2, dy - dy / 2).release().perform();
	}
}
