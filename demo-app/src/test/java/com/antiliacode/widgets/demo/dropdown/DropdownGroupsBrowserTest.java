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
package com.antiliacode.widgets.demo.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * The groups of the drop-downs of {@link DropdownPage} in the Wicket, React and Svelte tabs, in
 * Chrome and Firefox: the countries chosen most first, and the dialog choosing a unit of measure
 * from a long list of long options in groups, whose open list reaches over the footer of the
 * dialog, nothing covering it, each option's whole text shown, wrapped onto more lines when long,
 * and the filter hiding the groups none of whose options match. With
 * {@code -Ddropdowns.screenshots=<folder>} the open lists are saved there as pictures. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DropdownGroupsBrowserTest extends FrameworkAppsTestCase
{
	/** The labels of the groups of the units, in their order. */
	private static final List<String> GROUPS = List.of("Used most", "Count", "Mass", "Length",
		"Area", "Volume", "Time", "Energy");

	/** A long unit, in the middle of the list. */
	private static final String ACRE = "Acre (ac): an imperial and US unit of area of 43,560 " +
		"square feet, about 4,047 square metres";

	/**
	 * The lines the text of an option of an open list is drawn on, and where it is cut: the parts
	 * of its lines out of the list, and whether the text scrolls in its own box.
	 */
	private static final String LINES = """
		var option = arguments[0];
		var text = option.querySelector('.antilia-dropdown-text');
		var box = option.closest('.antilia-dropdown-menu').getBoundingClientRect();
		var range = document.createRange();
		range.selectNodeContents(text);
		var lines = {};
		var cut = [];
		Array.from(range.getClientRects()).forEach(function (rect) {
			if (rect.width > 0) {
				lines[Math.round(rect.top)] = true;
			}
			if (rect.left < box.left - 0.5 || rect.right > box.right + 0.5) {
				cut.push(Math.round(rect.left) + '-' + Math.round(rect.right) + ' out of ' +
					Math.round(box.left) + '-' + Math.round(box.right));
			}
		});
		if (text.scrollWidth > text.clientWidth + 1) {
			cut.push('scrolls ' + text.scrollWidth + ' in ' + text.clientWidth);
		}
		return {lines: Object.keys(lines).length, cut: cut};
		""";

	/**
	 * Whether the text of a drop-down's button is too long for it and ends in «…»: an ellipsis
	 * applies to a block, not to a flex box.
	 */
	private static final String ELLIPSIS = """
		var value = arguments[0].querySelector('.antilia-dropdown-value');
		var style = getComputedStyle(value);
		return value.scrollWidth > value.clientWidth && style.textOverflow === 'ellipsis' &&
			style.whiteSpace === 'nowrap' &&
			['block', 'inline-block', 'flow-root'].indexOf(style.display) >= 0;
		""";

	/** Whether the open list is what the browser finds at a point, not what is under it. */
	private static final String LIST_AT = """
		var menu = arguments[0].parentElement.querySelector('.antilia-dropdown-menu');
		var found = document.elementFromPoint(arguments[1], arguments[2]);
		return found !== null && menu.contains(found);
		""";

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void listsTheCountriesChosenMostFirstInAGroupOfTheirOwn(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "dropdown");
		WebElement country = app().findElement(By.name("country"));
		wait.until(d -> Dropdowns.options(country).size() > 200);

		Dropdowns.open(driver, country);

		assertEquals(List.of("Chosen most", "All the others"), Dropdowns.groups(country));
		assertEquals(List.of("France", "Germany", "Spain", "United Kingdom", "United States"),
			Dropdowns.groupOptions(driver, country, "Chosen most"));
		List<String> others = Dropdowns.groupOptions(driver, country, "All the others");
		assertEquals("Afghanistan", others.get(0));
		assertFalse(others.contains("Germany"));
		assertEquals("Germany", toggle(country).getText());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theListOfUnitsOpensOverTheFooterOfTheDialogAndChooses(SeleniumBrowser browser,
		String framework)
	{
		WebElement unit = openDialog(browser, framework);
		Dropdowns.open(driver, unit);
		wait.until(d -> Dropdowns.list(unit).getHeight() > 0);

		// the body of the dialog would cut the list under the footer's buttons
		Rectangle list = Dropdowns.list(unit);
		Rectangle footer = dialog().findElement(By.cssSelector(".modal-dialog-footer")).getRect();
		assertTrue(list.getY() < footer.getY() &&
			list.getY() + list.getHeight() > footer.getY() + footer.getHeight(),
			"the list does not reach over the footer: " + list.getY() + "+" + list.getHeight() +
				" and " + footer.getY() + "+" + footer.getHeight());
		assertEquals(List.of(), Dropdowns.covered(driver, unit), "the list is cut or covered");
		assertEquals(Boolean.TRUE, ((JavascriptExecutor)driver).executeScript(LIST_AT, unit,
			list.getX() + list.getWidth() / 2, footer.getY() + footer.getHeight() / 2),
			"the footer is drawn over the list");
		screenshot("units");

		List<WebElement> options = Dropdowns.options(unit);
		int acre = options.stream().map(WebElement::getText).toList().indexOf(ACRE);
		Dropdowns.chooseInOpenList(driver, unit, acre);
		wait.until(d -> ACRE.equals(toggle(unit).getText()));
		// too long for the button, which ends it in «…» and has the whole text as its title
		assertEquals(Boolean.TRUE, ((JavascriptExecutor)driver).executeScript(ELLIPSIS,
			toggle(unit)), "the choice is cut without «…»");
		assertEquals(ACRE, toggle(unit).getDomAttribute("title"));
		assertFalse(dialogs().isEmpty(), "the dialog stays open");

		dialog().findElement(By.xpath(".//button[normalize-space()='Choose']")).click();
		wait.until(d -> dialogs().isEmpty());
		wait.until(d -> app().findElement(By.cssSelector(".dropdown-demo__unit")).getText()
			.equals("Unit of measure: " + ACRE));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theGroupsOfUnitsAreApartWithTheirLabels(SeleniumBrowser browser, String framework)
	{
		WebElement unit = openDialog(browser, framework);
		Dropdowns.open(driver, unit);

		assertEquals(GROUPS, Dropdowns.groups(unit));
		assertEquals(List.of("Piece (pc)", "Kilogram (kg)", "Metre (m)", "Litre (l)", "Hour (h)"),
			Dropdowns.groupOptions(driver, unit, "Used most").stream()
				.map(option -> option.substring(0, option.indexOf(':'))).toList());
		assertEquals(List.of("Square metre (m²)", "Hectare (ha)", "Square foot (ft²)",
			"Acre (ac)"), Dropdowns.groupOptions(driver, unit, "Area").stream()
				.map(option -> option.substring(0, option.indexOf(':'))).toList());
		// each group is labelled by its label, and drawn under the one before it
		List<WebElement> groups = unit.findElement(By.xpath(".."))
			.findElements(By.cssSelector(".antilia-dropdown-group"));
		int bottom = Integer.MIN_VALUE;
		for (WebElement group : groups)
		{
			assertEquals("group", group.getDomAttribute("role"));
			WebElement label = group.findElement(By.cssSelector(".antilia-dropdown-group-label"));
			assertEquals(label.getDomAttribute("id"), group.getDomAttribute("aria-labelledby"));
			Rectangle box = group.getRect();
			assertTrue(box.getY() >= bottom, "groups overlap: " + label.getText());
			bottom = box.getY() + box.getHeight();
			assertTrue(label.getRect().getY() < group.findElement(
				By.cssSelector(".antilia-dropdown-option")).getRect().getY(),
				"the label comes first: " + label.getText());
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void aLongOptionWrapsRatherThanBeingCut(SeleniumBrowser browser, String framework)
	{
		WebElement unit = openDialog(browser, framework);
		Dropdowns.open(driver, unit);

		List<WebElement> options = Dropdowns.options(unit);
		WebElement acre = options.stream().filter(option -> ACRE.equals(option.getText()))
			.findFirst().orElseThrow();
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'nearest'});", acre);
		Rectangle list = Dropdowns.list(unit);
		assertTrue(acre.getRect().getWidth() < list.getWidth(), "the option fits the list");
		for (WebElement option : options)
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> lines = (Map<String, Object>)((JavascriptExecutor)driver)
				.executeScript(LINES, option);
			assertEquals(List.of(), lines.get("cut"), "cut: " + option.getText());
			if (option.equals(acre))
			{
				assertTrue(((Number)lines.get("lines")).intValue() > 1,
					"not wrapped: " + lines + " " + ACRE);
			}
		}
		screenshot("wrapped");
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theFilterHidesTheGroupsWithoutMatches(SeleniumBrowser browser, String framework)
	{
		WebElement unit = openDialog(browser, framework);
		Dropdowns.open(driver, unit);
		WebElement filter = unit.findElement(By.xpath(".."))
			.findElement(By.cssSelector(".antilia-dropdown-filter"));
		wait.until(d -> filter.equals(d.switchTo().activeElement()));

		filter.sendKeys("imperial");

		assertEquals(List.of("Mass", "Length", "Area"), Dropdowns.groups(unit));
		List<String> shown = Dropdowns.groupOptions(driver, unit, "Length");
		assertEquals(List.of("Inch (in)", "Foot (ft)", "Yard (yd)", "Mile (mi)"), shown.stream()
			.map(option -> option.substring(0, option.indexOf(':'))).toList());
		assertEquals(List.of(), Dropdowns.covered(driver, unit), "the list is cut or covered");
		screenshot("filtered");

		filter.sendKeys(Keys.BACK_SPACE.toString().repeat("imperial".length()),
			"nothing like this");
		wait.until(d -> Dropdowns.groups(unit).isEmpty());
		assertTrue(unit.findElement(By.xpath("..")).findElement(
			By.cssSelector(".antilia-dropdown-empty")).isDisplayed(), "no matches");

		// Escape closes the list, and leaves the dialog open
		filter.sendKeys(Keys.ESCAPE);
		wait.until(d -> !Dropdowns.isOpen(unit));
		assertFalse(dialogs().isEmpty(), "the dialog closed with the list");
		assertEquals(toggle(unit), driver.switchTo().activeElement());
		assertNoErrors();
	}

	/**
	 * Opens the drop-down page in the tab of a framework, and the dialog of the units.
	 *
	 * @return the select of the units, in the dialog
	 */
	private WebElement openDialog(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dropdown");
		WebElement choose = app().findElement(
			By.xpath(".//button[normalize-space()='Choose a unit']"));
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", choose);
		choose.click();
		WebElement dialog = dialog();
		assertEquals("Unit of measure", dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		WebElement unit = dialog.findElement(By.name("unit"));
		wait.until(d -> !Dropdowns.options(unit).isEmpty());
		assertTrue(toggle(unit).getText().startsWith("Kilogram (kg): "), toggle(unit).getText());
		return unit;
	}

	private WebElement toggle(WebElement select)
	{
		return select.findElement(By.xpath("..")).findElement(
			By.cssSelector(".antilia-dropdown-toggle"));
	}

	/**
	 * @return the dialog open, once shown
	 */
	private WebElement dialog()
	{
		return wait.until(d -> dialogs().stream().findFirst().orElse(null));
	}

	private List<WebElement> dialogs()
	{
		return driver.findElements(By.cssSelector(
			".dialog-theme-default .modal-dialog-overlay > .modal-dialog")).stream()
			.filter(WebElement::isDisplayed).toList();
	}

	/**
	 * Takes a screenshot of the window into the folder of {@code dropdowns.screenshots}, if any.
	 */
	private void screenshot(String name)
	{
		String folder = System.getProperty("dropdowns.screenshots");
		if (folder == null)
		{
			return;
		}
		try
		{
			Path path = Path.of(folder, "dropdown-" + name + "-" + framework + "-" +
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
