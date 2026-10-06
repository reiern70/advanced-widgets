/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.advanced.widgets.demo.JettyTestCase;
import com.advanced.widgets.demo.SeleniumBrowser;

/**
 * Drives every combination of the settings shaping the table, in {@link DynamicDataTablePage}
 * and {@link BeanDynamicDataTablePage}, and checks after each change that the table is whole: all
 * rows of the page, as many cells in a row as headers, the cells under their headers, no content
 * drawn over the next column, the selection checkboxes and the navigation the settings ask for, a
 * body that is not collapsed, a page that does not scroll sideways when the table fills its
 * width, and no script error. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DynamicDataTableSettingsBrowserTest extends JettyTestCase
{
	private static final String TABLE = "table.dynamic-dataview";

	private static final String ROWS = TABLE + " .dynamic-data-table-rows > tbody > tr, " + TABLE +
		" > tbody > tr:not(.dynamic-data-table-body-row)";

	/** The settings and their values, in the order of their drop-downs. */
	private static final Map<String, List<String>> SETTINGS = new LinkedHashMap<>();

	static
	{
		SETTINGS.put("resize", List.of("NONE", "STRETCH"));
		SETTINGS.put("width", List.of("CONTENT", "PARENT"));
		SETTINGS.put("proportions", List.of("CONTENT", "SET"));
		SETTINGS.put("navigation", List.of("TOP", "BOTTOM", "BOTH"));
		SETTINGS.put("selection", List.of("NONE", "ONE", "MANY"));
		SETTINGS.put("bodyHeightUnits", List.of("PIXELS", "PERCENT", "VIEWPORT_HEIGHT"));
	}

	/** Records the script errors of the page into {@code window.tableErrors}. */
	private static final String RECORD_ERRORS = """
		if (!window.tableErrors) {
			window.tableErrors = [];
			window.addEventListener('error', function (event) {
				window.tableErrors.push(event.message);
			});
			window.addEventListener('unhandledrejection', function (event) {
				window.tableErrors.push('' + event.reason);
			});
		}
		""";

	/**
	 * What is wrong with the table, given the navigation position and selection mode it should
	 * have.
	 */
	private static final String PROBLEMS = """
		var navigation = arguments[0], selection = arguments[1]; // arguments[2]: the width setting
		var problems = [];
		var table = document.querySelector('table.dynamic-dataview');
		var box = function (element) { return element.getBoundingClientRect(); };
		var headers = Array.from(table.querySelector('tr.dynamic-data-table-headers').cells)
			.filter(function (cell) { return !cell.classList.contains('dynamic-data-table-filler'); });
		var row = table.querySelector('.dynamic-data-table-rows > tbody > tr') ||
			table.querySelector(':scope > tbody > tr:not(.dynamic-data-table-body-row)');
		if (!row) {
			problems.push('no row');
		} else {
			var cells = Array.from(row.cells)
				.filter(function (cell) { return !cell.classList.contains('dynamic-data-table-filler'); });
			if (cells.length !== headers.length) {
				problems.push(headers.length + ' headers but ' + cells.length + ' cells');
			}
			var misaligned = [];
			headers.forEach(function (header, i) {
				if (cells[i] && (Math.abs(box(cells[i]).left - box(header).left) > 1 ||
						Math.abs(box(cells[i]).width - box(header).width) > 1)) {
					misaligned.push((header.textContent.trim() || '#' + i) + ' ' +
						Math.round(box(header).left) + '/' + Math.round(box(header).width) + ' vs ' +
						Math.round(box(cells[i]).left) + '/' + Math.round(box(cells[i]).width));
				}
			});
			if (misaligned.length) {
				problems.push('misaligned: ' + misaligned.join(', '));
			}
			var spilling = [];
			document.querySelectorAll('table.dynamic-dataview .dynamic-data-table-rows > tbody > tr > td, ' +
					'table.dynamic-dataview > tbody > tr:not(.dynamic-data-table-body-row) > td')
				.forEach(function (cell) {
					if (cell.scrollWidth > cell.clientWidth + 1 &&
							getComputedStyle(cell).overflowX === 'visible') {
						var header = headers[cell.cellIndex];
						var name = header ? header.textContent.trim() : '#' + cell.cellIndex;
						if (spilling.indexOf(name) < 0) {
							spilling.push(name);
						}
					}
				});
			if (spilling.length) {
				problems.push('content spilling out of: ' + spilling.join(', '));
			}
		}
		if (arguments[2] === 'PARENT' &&
				document.documentElement.scrollWidth > document.documentElement.clientWidth + 1) {
			problems.push('the page scrolls sideways: ' + document.documentElement.scrollWidth +
				' in ' + document.documentElement.clientWidth);
		}
		var rowBoxes = table.querySelectorAll('input.dynamic-data-table-select').length;
		var pageBox = !!table.querySelector('input.dynamic-data-table-select-page');
		if ((selection === 'NONE') !== (rowBoxes === 0)) {
			problems.push('selection ' + selection + ' with ' + rowBoxes + ' row checkboxes');
		}
		if ((selection === 'MANY') !== pageBox) {
			problems.push('selection ' + selection + (pageBox ? ' with' : ' without') +
				' a page checkbox');
		}
		var top = !!table.querySelector('thead .dynamic-data-table-navigation');
		var bottom = !!table.querySelector('tfoot .dynamic-data-table-navigation');
		if (top !== (navigation !== 'BOTTOM') || bottom !== (navigation !== 'TOP')) {
			problems.push('navigation ' + navigation + ' shown at top ' + top + ', bottom ' + bottom);
		}
		var body = table.querySelector('.dynamic-data-table-body');
		if (body && body.clientHeight < 40) {
			problems.push('collapsed body ' + body.clientHeight + 'px');
		}
		if (body && body.scrollWidth > body.clientWidth + 1 &&
				box(table).width <= table.parentElement.clientWidth + 1) {
			problems.push('rows clipped: ' + body.scrollWidth + ' in ' + body.clientWidth);
		}
		(window.tableErrors || []).forEach(function (error) { problems.push('error: ' + error); });
		window.tableErrors = [];
		return problems;
		""";

	private WebDriver driver;

	private WebDriverWait wait;

	private final Map<String, String> current = new LinkedHashMap<>();

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
	@MethodSource("pages")
	void everyCombinationOfTheSettingsGivesAWholeTable(SeleniumBrowser browser, String page)
	{
		driver = browser.newDriver(1400, 1100);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(20));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort + "/" + page);
		wait.until(d -> d.findElements(By.cssSelector(ROWS)).size() == 25);
		js().executeScript(RECORD_ERRORS);
		current.put("resize", "STRETCH");
		current.put("width", "PARENT");
		current.put("proportions", "CONTENT");
		current.put("navigation", "BOTH");
		current.put("selection", "MANY");
		current.put("bodyHeightUnits", "PIXELS");

		List<String> broken = new ArrayList<>();
		for (Map<String, String> combination : combinations())
		{
			List<String> problems = new ArrayList<>();
			combination.forEach((name, value) -> {
				String problem = choose(name, value);
				if (problem != null)
				{
					problems.add(problem);
				}
			});
			problems.addAll(problems());
			if (problems.isEmpty() == false)
			{
				broken.add(combination + " -> " + problems);
			}
		}

		assertTrue(broken.isEmpty(), broken.size() + " broken combinations:\n" +
			String.join("\n", broken));
	}

	/**
	 * @return every combination of the settings, the last setting changing fastest
	 */
	private static List<Map<String, String>> combinations()
	{
		List<Map<String, String>> combinations = new ArrayList<>();
		combinations.add(new LinkedHashMap<>());
		for (Map.Entry<String, List<String>> setting : SETTINGS.entrySet())
		{
			List<Map<String, String>> next = new ArrayList<>();
			for (Map<String, String> combination : combinations)
			{
				for (String value : setting.getValue())
				{
					Map<String, String> extended = new LinkedHashMap<>(combination);
					extended.put(setting.getKey(), value);
					next.add(extended);
				}
			}
			combinations = next;
		}
		return combinations;
	}

	@SuppressWarnings("unchecked")
	private List<String> problems()
	{
		String navigation = current.get("navigation");
		String selection = current.get("selection");
		String width = current.get("width");
		List<String> problems = (List<String>)js().executeScript(PROBLEMS, navigation, selection, width);
		if (problems.isEmpty() == false)
		{
			// the cells line up with the headers a moment after the table is laid out
			try
			{
				wait.until(d -> ((List<?>)js().executeScript(PROBLEMS, navigation, selection, width))
					.isEmpty());
				return List.of();
			}
			catch (TimeoutException e)
			{
				return (List<String>)js().executeScript(PROBLEMS, navigation, selection, width);
			}
		}
		int rows = driver.findElements(By.cssSelector(ROWS)).size();
		return rows == 25 ? problems : List.of(rows + " rows instead of 25");
	}

	/**
	 * Chooses a value of a setting and waits for the table rendered again with its rows.
	 *
	 * @return what went wrong, {@code null} if nothing did
	 */
	private String choose(String name, String value)
	{
		if (value.equals(current.get(name)))
		{
			return null;
		}
		WebElement table = driver.findElement(By.cssSelector(TABLE));
		new Select(driver.findElement(By.name("options:" + name)))
			.selectByIndex(SETTINGS.get(name).indexOf(value));
		wait.until(d -> {
			try
			{
				table.isDisplayed();
				return false;
			}
			catch (StaleElementReferenceException replaced)
			{
				return true;
			}
		});
		current.put(name, value);
		try
		{
			wait.until(d -> d.findElements(By.cssSelector(ROWS)).size() == 25);
			wait.until(d -> d.findElements(By.cssSelector(".wicket-veil")).isEmpty());
			return null;
		}
		catch (TimeoutException e)
		{
			String problem = "after choosing " + name + " " + value + ": " +
				driver.findElements(By.cssSelector(ROWS)).size() + " rows, " +
				js().executeScript("return (window.tableErrors || []).join('; ');");
			// the page keeps its settings on the server: start over from a fresh rendering
			driver.navigate().refresh();
			if (driver.findElements(By.cssSelector(TABLE)).isEmpty())
			{
				throw new AssertionError(problem + "; the page shows no table but: " +
					driver.getTitle() + " " + driver.findElement(By.tagName("body")).getText()
						.replaceAll("\\s+", " "), e);
			}
			js().executeScript(RECORD_ERRORS);
			return problem;
		}
	}

	private JavascriptExecutor js()
	{
		return (JavascriptExecutor)driver;
	}

	/**
	 * @return every browser, or the one named by {@code -Dwicket.selenium.browser}, with the JSON
	 *         based and the bean based table
	 */
	static Stream<Arguments> pages()
	{
		String only = System.getProperty("wicket.selenium.browser");
		return Stream.of(SeleniumBrowser.values())
			.filter(browser -> only == null || browser.name().equalsIgnoreCase(only))
			.flatMap(browser -> Stream.of(Arguments.of(browser, "dynamic-data-table"),
				Arguments.of(browser, "bean-dynamic-data-table")));
	}
}
