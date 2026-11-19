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
package com.antiliacode.widgets.demo.looks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Shows the {@link LooksPage} of daisyUI in Chrome and in Firefox: the page has daisyUI's style
 * sheet alone, from the application, under its content security policy, and the components of the
 * column of the daisyUI look are drawn by it in the colors of the theme: a button, a drop-down
 * and its list, a dialog, whose modal-box daisyUI shows only in an open modal. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class LooksBrowserTest extends FrameworkAppsTestCase
{
	/** The primary color of the default theme, #c2410c. */
	private static final String PRIMARY = "rgb(194, 65, 12)";

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void drawsTheComponentsWithTheStyleSheetOfDaisyUi(SeleniumBrowser browser)
	{
		open(browser, "looks?framework=daisyui");

		List<String> sheets = sheets();
		assertTrue(sheets.stream().anyMatch(href -> href.endsWith("/daisyui.css")), sheets.toString());
		assertTrue(sheets.stream().noneMatch(href -> href.contains("bootstrap")), sheets.toString());
		// daisyUI's theme is on the page: its corners of fields
		assertEquals(".25rem", script("return getComputedStyle(document.documentElement)" +
			".getPropertyValue('--radius-field').trim();"));

		WebElement column = column();
		// a primary button, daisyUI's btn in the primary color of the theme, as tall as its fields
		WebElement save = column.findElement(By.cssSelector("button.btn.btn-primary"));
		assertEquals(PRIMARY, backgroundColor(save));
		assertEquals("40px", save.getCssValue("height"));

		// a drop-down: daisyUI's select, its list a box of daisyUI with its corners
		WebElement toggle = column.findElement(By.cssSelector(".antilia-dropdown-toggle"));
		assertTrue(List.of(toggle.getDomAttribute("class").split(" ")).contains("select"));
		assertEquals("40px", toggle.getCssValue("height"));
		toggle.click();
		WebElement menu = column.findElement(By.cssSelector(".antilia-dropdown-menu"));
		wait.until(d -> menu.isDisplayed());
		assertEquals("8px", menu.getCssValue("border-top-left-radius"));
		screenshot("dropdown");
		toggle.click();
		wait.until(d -> !menu.isDisplayed());

		// a dialog: its content daisyUI's modal-box, shown, its header no band of color
		column.findElements(By.cssSelector("button")).stream()
			.filter(button -> button.getText().contains("Open a dialog")).findFirst().orElseThrow()
			.click();
		By content = By.cssSelector(".modal-dialog-content.modal-box");
		wait.until(d -> !d.findElements(content).isEmpty());
		WebElement box = driver.findElement(content);
		wait.until(d -> box.isDisplayed() && "1".equals(box.getCssValue("opacity")));
		assertTrue(List.of(driver.findElement(By.cssSelector(".modal-box .modal-dialog-footer"))
			.getDomAttribute("class").split(" ")).contains("modal-action"));
		assertEquals("rgba(0, 0, 0, 0)", backgroundColor(driver.findElement(By.cssSelector(
			".modal-box .modal-dialog-header"))));
		screenshot("dialog");
		driver.findElement(By.cssSelector(".modal-box .modal-dialog-close")).click();
		wait.until(d -> d.findElements(content).isEmpty() || !d.findElement(content).isDisplayed());

		// daisyUI's own markup in the colors of the theme too
		WebElement own = driver.findElement(By.cssSelector(".looks__native .btn.btn-primary"));
		assertEquals(PRIMARY, backgroundColor(own));
		screenshot("page");
		assertNoErrors();
	}

	private void open(SeleniumBrowser browser, String path)
	{
		this.browser = browser;
		framework = "wicket";
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + path);
		wait.until(SeleniumBrowser::isReady);
	}

	/**
	 * @return the column of the daisyUI look
	 */
	private WebElement column()
	{
		return driver.findElement(By.cssSelector(".looks__column[data-antilia-look='daisyui']"));
	}

	@SuppressWarnings("unchecked")
	private List<String> sheets()
	{
		return (List<String>)script("return Array.from(document.styleSheets, s => s.href)" +
			".filter(h => h);");
	}

	/**
	 * @return the background color of an element, as the browser computes it, alike in every
	 *         browser
	 */
	private String backgroundColor(WebElement element)
	{
		return (String)((JavascriptExecutor)driver).executeScript(
			"return getComputedStyle(arguments[0]).backgroundColor;", element);
	}

	private Object script(String script)
	{
		return ((JavascriptExecutor)driver).executeScript(script);
	}

	/**
	 * Takes a screenshot of the page into the folder of the system property looks.screenshots, if
	 * set.
	 */
	private void screenshot(String name)
	{
		String folder = System.getProperty("looks.screenshots");
		if (folder == null)
		{
			return;
		}
		try
		{
			Path path = Path.of(folder, "looks-daisyui-" + name + "-" +
				browser.name().toLowerCase(Locale.ROOT) + ".png");
			Files.createDirectories(path.getParent());
			Files.write(path, ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
