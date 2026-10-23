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
package com.antiliacode.widgets.demo.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.Cases;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.Toasts;

/**
 * Drives {@link PalettePage} in Chrome and in Firefox, in deployment mode, so through the
 * minified script built from frontend/core: the arrows, a double click, the keyboard, dragging,
 * the order of the columns and the Ajax reporting each change. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PaletteBrowserTest extends JettyTestCase
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
	void movesTheSkillsWithTheArrowsAndADoubleClick(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Any order");
		assertEquals("Java\nApache Wicket", Palettes.selected(palette("skills")));

		Palettes.item(palette("skills"), "CSS").click();
		Palettes.button(palette("skills"), "add").click();
		wait.until(d -> choices().startsWith("Skills: Java, Apache Wicket, CSS;"));

		Palettes.doubleClick(driver, Palettes.item(palette("skills"), "Java"));
		wait.until(d -> choices().startsWith("Skills: Apache Wicket, CSS;"));

		Palettes.button(palette("skills"), "add-all").click();
		wait.until(d -> choices().startsWith("Skills: Java, Apache Wicket, JavaScript, CSS, SQL, " +
			"React, Svelte, Docker;"));
		assertFalse(Palettes.button(palette("skills"), "add-all").isEnabled());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void filtersTheSkillsAndAddsWithTheKeyboard(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Any order");

		WebElement filter = palette("skills").findElement(By.cssSelector(".antilia-palette-filter"));
		filter.sendKeys("script");
		assertEquals("JavaScript", Palettes.available(palette("skills")));

		WebElement list = Palettes.list(palette("skills"), "available");
		list.sendKeys(Keys.ARROW_DOWN);
		list.sendKeys(Keys.ENTER);
		wait.until(d -> choices().startsWith("Skills: Java, Apache Wicket, JavaScript;"));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void ordersTheColumnsWithTheButtonsAndByDragging(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "The order matters");
		assertEquals("Name\nCountry\nEmail", Palettes.selected(palette("columns")));

		Palettes.item(palette("columns"), "Email").click();
		Palettes.button(palette("columns"), "up").click();
		wait.until(d -> choices().endsWith("columns: Name, Email, Country."));

		// Name dragged below Country
		Palettes.drag(driver, Palettes.item(palette("columns"), "Name"),
			Palettes.item(palette("columns"), "Country"), 0.9);
		wait.until(d -> choices().endsWith("columns: Email, Country, Name."));

		// City dragged from the available ones to the top
		Palettes.drag(driver, Palettes.item(palette("columns"), "City"),
			Palettes.item(palette("columns"), "Email"), 0.1);
		wait.until(d -> choices().endsWith("columns: City, Email, Country, Name."));

		driver.findElement(By.cssSelector(".palette-demo__send")).click();
		wait.until(d -> Toasts.latest(d).equals(
			"Sent: Skills: Java, Apache Wicket; columns: City, Email, Country, Name."));
		assertEquals("City\nEmail\nCountry\nName", Palettes.selected(palette("columns")),
			"the order the form posted");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theCheckBoxSwitchesTheOrderOff(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "The order matters");
		assertTrue(palette("columns").findElement(By.cssSelector(".antilia-palette-order"))
			.isDisplayed());

		field("ordered").click();

		wait.until(d -> choices().endsWith("columns: Name, Email, Country."));
		wait.until(d -> !palette("columns").findElement(By.cssSelector(".antilia-palette-order"))
			.isDisplayed());
		assertEquals("Name\nEmail\nCountry", Palettes.selected(palette("columns")));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void numbersTheRowsOfTheColumnsUnderTheirOwnTitles(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Any order");
		assertEquals("Available\nSelected", Palettes.titles(palette("skills")));
		assertEquals("", Palettes.numbers(palette("skills"), "selected"));
		Cases.selectInWicket(wait, driver, "The order matters");
		WebElement columns = palette("columns");
		assertEquals("Columns left out\nIn the report", Palettes.titles(columns));
		assertEquals("1.\n2.\n3.", Palettes.numbers(columns, "selected"));
		assertEquals("1.\n2.\n3.\n4.", Palettes.numbers(columns, "available"));

		Palettes.doubleClick(driver, Palettes.item(columns, "Born"));
		wait.until(d -> Palettes.numbers(palette("columns"), "selected").equals("1.\n2.\n3.\n4."));
		assertEquals("Name\nCountry\nEmail\nBorn", Palettes.selected(palette("columns")));

		field("numbered").click();
		wait.until(d -> Palettes.numbers(palette("columns"), "selected").isEmpty());
		assertEquals("Name\nCountry\nEmail\nBorn", Palettes.selected(palette("columns")));
		assertEquals("Columns left out\nIn the report", Palettes.titles(palette("columns")));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theCheckBoxTakesTheFilterAway(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Any order");
		WebElement filter = palette("skills").findElement(By.cssSelector(".antilia-palette-filter"));
		filter.sendKeys("sv");
		assertEquals("Svelte", Palettes.available(palette("skills")));

		field("filterable").click();

		wait.until(d -> palette("skills").findElements(By.cssSelector(".antilia-palette-filter"))
			.isEmpty());
		assertEquals("JavaScript\nCSS\nSQL\nReact\nSvelte\nDocker",
			Palettes.available(palette("skills")));
		assertEquals("Skills: Java, Apache Wicket; columns: Name, Country, Email.", choices(),
			"filtering changes nothing");
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void doesNothingWhenDisabled(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Disabled");

		WebElement disabled = palette("disabled");
		assertTrue(disabled.getDomAttribute("class").contains("antilia-palette-disabled"));
		assertFalse(Palettes.button(disabled, "add-all").isEnabled());
		assertFalse(Palettes.button(disabled, "remove-all").isEnabled());
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void drawsTheCountriesAPageAtATimeAndFiltersThemAll(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "Many choices");
		WebElement countries = palette("countries");
		WebElement list = Palettes.list(countries, "available");
		assertEquals(30, rows(list));

		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollTop = arguments[0].scrollHeight;", list);
		wait.until(d -> rows(list) == 60);

		WebElement filter = countries.findElement(By.cssSelector(".antilia-palette-filter"));
		filter.sendKeys("urug");
		assertEquals("", Palettes.available(countries), "Uruguay is chosen already");
		filter.clear();
		filter.sendKeys("zimb");
		wait.until(d -> Palettes.available(countries).equals("Zimbabwe"));
		Palettes.doubleClick(driver, Palettes.item(countries, "Zimbabwe"));

		wait.until(d -> driver.findElement(By.cssSelector(".palette-demo__count")).getText()
			.equals("4 chosen: Germany, Spain, Uruguay, Zimbabwe."));
		assertEquals("Germany\nSpain\nUruguay\nZimbabwe", Palettes.selected(countries));
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFiltersMatchFromTheStartAnywhereAndAtTheEnd(SeleniumBrowser browser)
	{
		open(browser);
		Cases.selectInWicket(wait, driver, "How the filter matches");

		Palettes.assertMatches(wait, palette("matchStart"), "gu", "Guatemala", "Uruguay",
			label -> label.startsWith("Gu"));
		Palettes.assertMatches(wait, palette("matchAnywhere"), "gu", "Uruguay", null,
			label -> label.toLowerCase().contains("gu"));
		Palettes.assertMatches(wait, palette("matchEnd"), "land", "Finland", null,
			label -> label.endsWith("land"));
		Palettes.assertWordsMatch(wait, palette("matchWords"));
		assertEquals(30, rows(Palettes.list(palette("countries"), "available")),
			"the others are not filtered");
	}

	private static int rows(WebElement list)
	{
		return list.findElements(By.cssSelector(".antilia-palette-item")).size();
	}

	private void open(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1200, 1000);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/palette");
		wait.until(d -> d.findElements(By.cssSelector(".antilia-palette-side")).size() == 16);
	}

	/**
	 * @return the field of a name, in the form its name ends the path of
	 */
	private WebElement field(String name)
	{
		return driver.findElement(By.cssSelector("[name$=':" + name + "']"));
	}

	private WebElement palette(String name)
	{
		return field(name).findElement(By.xpath(".."));
	}

	private String choices()
	{
		return driver.findElement(By.cssSelector(".palette-demo__choices")).getText();
	}
}
