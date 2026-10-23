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
package com.antiliacode.widgets.demo.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

/**
 * Drives {@link DropdownPage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core: choosing with the mouse and the keyboard, the filter,
 * the chips of a multiple drop-down and the Ajax reporting each choice. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DropdownBrowserTest extends JettyTestCase
{
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
	void choosesWithTheMouseAndReportsTheChoiceByAjax(SeleniumBrowser browser)
	{
		open(browser);
		assertEquals("M", toggle("size").getText());

		Dropdowns.choose(driver, select("size"), 2);

		wait.until(d -> choices().startsWith("Size L,"));
		assertEquals("L", toggle("size").getText());
		assertEquals("2", select("size").getDomProperty("value"), "Wicket's value of the third size");
		assertFalse(isOpen("size"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void choosesWithTheKeyboard(SeleniumBrowser browser)
	{
		open(browser);

		toggle("size").sendKeys(Keys.ARROW_DOWN);
		assertTrue(isOpen("size"));
		driver.switchTo().activeElement().sendKeys(Keys.ARROW_DOWN);
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);

		wait.until(d -> choices().startsWith("Size L,"));
		assertFalse(isOpen("size"));
		assertEquals(toggle("size"), driver.switchTo().activeElement());

		toggle("size").sendKeys(Keys.ARROW_DOWN);
		driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
		assertFalse(isOpen("size"));
		assertEquals("L", toggle("size").getText());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void filtersTheCountries(SeleniumBrowser browser)
	{
		open(browser);

		toggle("country").click();
		WebElement filter = wrapper("country").findElement(By.cssSelector(".antilia-dropdown-filter"));
		assertEquals(filter, driver.switchTo().activeElement());
		filter.sendKeys("zeal");

		List<String> shown = wrapper("country")
			.findElements(By.cssSelector(".antilia-dropdown-option:not([hidden])")).stream()
			.map(WebElement::getText)
			.toList();
		assertEquals(List.of("New Zealand"), shown);
		filter.sendKeys(Keys.ENTER);

		wait.until(d -> choices().contains("country New Zealand;"));
		assertEquals("New Zealand", toggle("country").getText());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void togglesTheSkillsAsChipsWhileTheListStaysOpen(SeleniumBrowser browser)
	{
		open(browser);
		assertEquals(List.of("Java", "Apache Wicket"), chips("skills"));

		Dropdowns.choose(driver, select("skills"), 2);

		wait.until(d -> choices().contains("skills: Java, Apache Wicket, JavaScript;"));
		assertTrue(isOpen("skills"), "a multiple drop-down stays open");
		assertEquals(List.of("Java", "Apache Wicket", "JavaScript"), chips("skills"));

		WebElement option = wrapper("skills").findElements(By.cssSelector(".antilia-dropdown-option"))
			.get(0);
		option.click();
		wait.until(d -> choices().contains("skills: Apache Wicket, JavaScript;"));
		assertEquals(List.of("Apache Wicket", "JavaScript"), chips("skills"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void doesNotOpenWhenDisabled(SeleniumBrowser browser)
	{
		open(browser);

		assertFalse(toggle("disabled").isEnabled());
		assertTrue(wrapper("disabled").getDomAttribute("class").contains("antilia-dropdown-disabled"));
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/dropdown");
		wait.until(d -> !d.findElements(By.cssSelector(".antilia-dropdown-toggle")).isEmpty());
	}

	private WebElement select(String name)
	{
		return driver.findElement(By.name(name));
	}

	private WebElement wrapper(String name)
	{
		return select(name).findElement(By.xpath(".."));
	}

	private WebElement toggle(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".antilia-dropdown-toggle"));
	}

	private boolean isOpen(String name)
	{
		return wrapper(name).getDomAttribute("class").contains("antilia-dropdown-open");
	}

	private List<String> chips(String name)
	{
		return wrapper(name).findElements(By.cssSelector(".antilia-chip")).stream()
			.map(WebElement::getText)
			.toList();
	}

	private String choices()
	{
		return driver.findElement(By.cssSelector(".dropdown-demo__choices")).getText();
	}
}
