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
package com.antilia.widgets.demo.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link PalettePage} in their tabs: the same form as the
 * Wicket one, the sentence below it following every change in the browser. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PaletteAppsBrowserTest extends FrameworkAppsTestCase
{
	private static final String START = "Skills: Java, Apache Wicket; columns: Name, Country, Email.";

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void startsWithTheChoicesOfThePage(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "palette");

		wait.until(d -> choices().equals(START));
		assertEquals("Java\nApache Wicket", Palettes.selected(palette(0)));
		assertEquals("Available", palette(0).findElement(By.cssSelector(".antilia-palette-title"))
			.getText());
		assertEquals("", Palettes.numbers(palette(0), "selected"));
		selectCase("The order matters");
		assertEquals("Name\nCountry\nEmail", Palettes.selected(palette(1)));
		assertEquals("Columns left out\nIn the report", Palettes.titles(palette(1)));
		assertEquals("1.\n2.\n3.", Palettes.numbers(palette(1), "selected"));
		selectCase("Many choices");
		assertEquals("Germany\nSpain\nUruguay", Palettes.selected(palette(2)));
		assertEquals(30, palette(2).findElements(By.cssSelector(
			".antilia-palette-available .antilia-palette-item")).size(), "a page of countries");
		assertEquals("3 chosen: Germany, Spain, Uruguay.", app().findElement(
			By.cssSelector(".palette-demo__count")).getText());
		selectCase("Disabled");
		assertEquals("SQL", Palettes.selected(palette(7)));
		assertFalse(Palettes.button(palette(7), "add-all").isEnabled());
		assertFalse(driver.findElement(By.id("framework-panel-wicket")).isDisplayed());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theSentenceFollowsEveryChange(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "palette");
		wait.until(d -> choices().equals(START));

		Palettes.doubleClick(driver, Palettes.item(palette(0), "Docker"));
		wait.until(d -> choices().startsWith("Skills: Java, Apache Wicket, Docker;"));

		selectCase("The order matters");
		Palettes.drag(driver, Palettes.item(palette(1), "Email"),
			Palettes.item(palette(1), "Name"), 0.1);
		wait.until(d -> choices().endsWith("columns: Email, Name, Country."));

		Palettes.item(palette(1), "Name").click();
		Palettes.button(palette(1), "down").click();
		wait.until(d -> choices().endsWith("columns: Email, Country, Name."));

		app().findElements(By.cssSelector(".palette-demo__check input")).get(2).click();
		wait.until(d -> Palettes.numbers(palette(1), "selected").isEmpty());
		assertEquals("Email\nCountry\nName", Palettes.selected(palette(1)));

		app().findElements(By.cssSelector(".palette-demo__check input")).get(1).click();
		wait.until(d -> choices().endsWith("columns: Name, Email, Country."));

		selectCase("Any order");
		assertEquals(1, palette(0).findElements(By.cssSelector(".antilia-palette-filter")).size());
		app().findElements(By.cssSelector(".palette-demo__check input")).get(0).click();
		wait.until(d -> palette(0).findElements(By.cssSelector(".antilia-palette-filter"))
			.isEmpty());
		selectCase("The order matters");
		assertEquals("Name\nEmail\nCountry", Palettes.selected(palette(1)));
		assertFalse(palette(1).findElement(By.cssSelector(".antilia-palette-order")).isDisplayed());

		app().findElement(By.cssSelector(".palette-demo__send")).click();
		wait.until(d -> app().findElement(By.cssSelector(".feedbackPanel")).getText()
			.equals("Sent: Skills: Java, Apache Wicket, Docker; columns: Name, Email, Country."));
		List<String> posted = app().findElements(By.cssSelector("input[name='columns']")).stream()
			.map(input -> input.getDomProperty("value"))
			.toList();
		assertEquals(List.of("name", "email", "country"), posted);
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theFiltersMatchFromTheStartAnywhereAndAtTheEnd(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "palette");
		wait.until(d -> choices().equals(START));
		selectCase("How the filter matches");

		Palettes.assertMatches(wait, palette(3), "gu", "Guatemala", "Uruguay",
			label -> label.startsWith("Gu"));
		Palettes.assertMatches(wait, palette(4), "gu", "Uruguay", null,
			label -> label.toLowerCase().contains("gu"));
		Palettes.assertMatches(wait, palette(5), "land", "Finland", null,
			label -> label.endsWith("land"));
		Palettes.assertWordsMatch(wait, palette(6));
		assertNoErrors();
	}

	private WebElement palette(int index)
	{
		return app().findElements(By.cssSelector(".antilia-palette")).get(index);
	}

	private String choices()
	{
		return app().findElement(By.cssSelector(".palette-demo__choices")).getText();
	}
}
