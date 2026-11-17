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
package com.antiliacode.widgets.demo.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * Drives the drop-downs of {@link ThemeEditorPage} in Chrome and in Firefox: the color scheme of
 * the theme, chosen in its list and previewed, and the theme the editor starts from, whose color
 * scheme the drop-down then shows. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ThemeEditorBrowserTest extends JettyTestCase
{
	private static final String PROPERTY = "--antilia-theme-color-scheme";

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
	void choosesTheColorSchemeInItsListAndPreviewsIt(SeleniumBrowser browser)
	{
		open(browser);
		assertEquals("light", toggle().getText());

		Dropdowns.open(driver, scheme());
		wait.until(d -> Dropdowns.list(scheme()).getHeight() > 0);
		assertEquals(List.of(), Dropdowns.covered(driver, scheme()), "the list is cut or covered");
		Dropdowns.chooseInOpenList(driver, scheme(), 1);

		wait.until(d -> "dark".equals(previewed()));
		assertEquals("dark", toggle().getText());
		assertTrue(css().contains(PROPERTY + ": dark;"), css());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void showsTheColorSchemeOfTheThemeStartedFrom(SeleniumBrowser browser)
	{
		open(browser);

		Dropdowns.chooseByText(driver, driver.findElement(By.cssSelector(
			".theme-editor-start select")), "Dark");

		wait.until(d -> "dark".equals(toggle().getText()));
		assertEquals("dark", scheme().getDomProperty("value"));
		assertTrue(css().contains(PROPERTY + ": dark;"), css());
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1300, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/theme-editor");
		wait.until(d -> !scheme().findElement(By.xpath("..")).findElements(By.cssSelector(
			".antilia-dropdown-toggle")).isEmpty());
	}

	/**
	 * @return the hidden select of the color scheme
	 */
	private WebElement scheme()
	{
		return driver.findElement(By.cssSelector("select[data-theme-property='" + PROPERTY + "']"));
	}

	private WebElement toggle()
	{
		return scheme().findElement(By.xpath("..")).findElement(By.cssSelector(
			".antilia-dropdown-toggle"));
	}

	/**
	 * @return the color scheme the preview is shown in
	 */
	private String previewed()
	{
		return (String)((JavascriptExecutor)driver).executeScript(
			"return document.querySelector('.theme-editor-preview').style.getPropertyValue(" +
				"arguments[0]);", PROPERTY);
	}

	/**
	 * @return the CSS of the theme
	 */
	private String css()
	{
		return driver.findElement(By.cssSelector(".theme-editor-css")).getDomProperty("value");
	}
}
