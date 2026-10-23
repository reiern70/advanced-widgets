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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.JettyTestCase;
import com.antiliacode.widgets.demo.SeleniumBrowser;

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
	private static final String TABLE = "table.ant-dataview";

	private static final String ROWS = TABLE + " .ant-data-table-rows > tbody > tr, " + TABLE +
		" > tbody > tr:not(.ant-data-table-body-row)";

	private static final String PUSH_ROWS = "Push the rows of this page in reverse order via web sockets";

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

	/** Settings set by the scenarios only, not combined with the others. */
	private static final Map<String, List<String>> OTHER_SETTINGS = Map.of("columnState",
		List.of("SESSION", "FILE"));

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
		var table = document.querySelector('table.ant-dataview');
		var box = function (element) { return element.getBoundingClientRect(); };
		var headers = Array.from(table.querySelector('tr.ant-data-table-headers').cells)
			.filter(function (cell) { return !cell.classList.contains('ant-data-table-filler'); });
		var row = table.querySelector('.ant-data-table-rows > tbody > tr') ||
			table.querySelector(':scope > tbody > tr:not(.ant-data-table-body-row)');
		if (!row) {
			problems.push('no row');
		} else {
			var cells = Array.from(row.cells)
				.filter(function (cell) { return !cell.classList.contains('ant-data-table-filler'); });
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
			document.querySelectorAll('table.ant-dataview .ant-data-table-rows > tbody > tr > td, ' +
					'table.ant-dataview > tbody > tr:not(.ant-data-table-body-row) > td')
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
		var rowBoxes = table.querySelectorAll('input.ant-data-table-select').length;
		var pageBox = !!table.querySelector('input.ant-data-table-select-page');
		if ((selection === 'NONE') !== (rowBoxes === 0)) {
			problems.push('selection ' + selection + ' with ' + rowBoxes + ' row checkboxes');
		}
		if ((selection === 'MANY') !== pageBox) {
			problems.push('selection ' + selection + (pageBox ? ' with' : ' without') +
				' a page checkbox');
		}
		var top = !!table.querySelector('thead .ant-data-table-navigation');
		var bottom = !!table.querySelector('tfoot .ant-data-table-navigation');
		if (top !== (navigation !== 'BOTTOM') || bottom !== (navigation !== 'TOP')) {
			problems.push('navigation ' + navigation + ' shown at top ' + top + ', bottom ' + bottom);
		}
		var body = table.querySelector('.ant-data-table-body');
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

	/**
	 * The key of each of the first rows and the text of each of its cells, by the header of the
	 * cell's column.
	 */
	private static final String CONTENT = """
		var table = document.querySelector('table.ant-dataview');
		var headers = Array.from(table.querySelector('tr.ant-data-table-headers').cells)
			.filter(function (cell) { return !cell.classList.contains('ant-data-table-filler'); })
			.map(function (cell) { return cell.textContent.replace(/[\u25b2\u25bc]/g, '').trim(); });
		var rows = Array.from(table.querySelectorAll('.ant-data-table-rows > tbody > tr[data-key], ' +
			':scope > tbody > tr[data-key]')).slice(0, 5);
		return rows.map(function (tr) {
			var cells = {};
			Array.from(tr.cells).forEach(function (cell, i) {
				if (headers[i] !== undefined) {
					cells[headers[i]] = cell.textContent.replace(/\s+/g, ' ').trim();
				}
			});
			return { key: tr.getAttribute('data-key'), cells: cells };
		});
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
		everyCombination(browser, page, false);
	}

	/**
	 * The same with columns moved, hidden and shown first, so that the order and the set of the
	 * columns differ from the ones the table was created with.
	 */
	@ParameterizedTest
	@MethodSource("pages")
	void everyCombinationOfTheSettingsGivesAWholeTableWithRearrangedColumns(
		SeleniumBrowser browser, String page)
	{
		everyCombination(browser, page, true);
	}

	private void everyCombination(SeleniumBrowser browser, String page, boolean rearrange)
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
		current.put("navigation", "TOP");
		current.put("selection", "MANY");
		current.put("bodyHeightUnits", "PIXELS");
		current.put("columnState", "SESSION");
		if (rearrange)
		{
			rearrangeColumns();
			assertTrue(mixedCells().isEmpty(), "after rearranging: " + mixedCells());
			keepColumnStateInFileAndBack();
			if (driver.findElements(pushRowsButton()).isEmpty() == false)
			{
				pushRows();
			}
		}

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
		return rows == 25 ? mixedCells() : List.of(rows + " rows instead of 25");
	}

	/**
	 * @return the cells of the first rows that do not show their user's value for the header of
	 *         their column
	 */
	@SuppressWarnings("unchecked")
	private List<String> mixedCells()
	{
		List<String> mixed = new ArrayList<>();
		List<Map<String, Object>> rows = (List<Map<String, Object>>)js().executeScript(CONTENT);
		if (rows.size() < 5 || ((Map<String, String>)rows.get(0).get("cells"))
			.containsKey("Last Name") == false)
		{
			return List.of("the cells of the rows were not found: " + rows);
		}
		for (Map<String, Object> row : rows)
		{
			User user = DatabaseLocator.getLargeDatabase()
				.get(Long.parseLong((String)row.get("key")));
			((Map<String, String>)row.get("cells")).forEach((header, text) -> {
				String expected = expected(user, header);
				boolean contains = "Address".equals(header) || "Phones".equals(header);
				if (expected != null && (contains ? text.contains(expected)
					: text.equals(expected)) == false)
				{
					mixed.add("user " + user.getId() + ": '" + header + "' shows '" + text +
						"' instead of '" + expected + "'");
				}
			});
		}
		return mixed;
	}

	/**
	 * @return what the cell of the user in the column of the header shows, all of it or, for the
	 *         address and the phones, a part of it; {@code null} if it is not checked
	 */
	private static String expected(User user, String header)
	{
		return switch (header)
		{
			case "ID" -> String.valueOf(user.getId());
			case "First Name" -> user.getFirstName();
			case "Last Name" -> user.getLastName();
			case "Born" -> Objects.toString(user.getBorn(), "");
			case "Address" -> user.getAddress();
			case "Phones" -> user.getCellPhone();
			case "Home phone" -> user.getHomePhone();
			case "Cell phone" -> user.getCellPhone();
			case "City" -> user.getCity();
			case "Country" -> user.getCountryName();
			default -> null;
		};
	}

	/**
	 * Moves First Name two columns to the right and Phones three to the left with the keyboard,
	 * then hides Born and shows City with the column chooser.
	 */
	private void rearrangeColumns()
	{
		move("First Name", Keys.ARROW_RIGHT, 2);
		move("Phones", Keys.ARROW_LEFT, 3);

		WebElement chooser = driver.findElement(By.cssSelector(
			TABLE + " details.ant-data-table-column-chooser"));
		chooser.findElement(By.tagName("summary")).click();
		for (String label : List.of("Born", "City"))
		{
			chooser.findElements(By.cssSelector(".ant-data-table-column-chooser-column"))
				.stream()
				.filter(button -> button.getText().contains(label))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no " + label + " in the column chooser"))
				.click();
		}
		WebElement table = driver.findElement(By.cssSelector(TABLE));
		chooser.findElement(By.cssSelector("[data-dt-apply]")).click();
		awaitRenderedAgain(table);
		List<String> headers = headerTexts();
		assertTrue(headers.contains("City") && headers.contains("Born") == false,
			"the chooser did not apply: " + headers);
	}

	/**
	 * Keeps the column state in the file and in the session again, each switch taking the state
	 * of the other store, and reloads the page.
	 */
	private void keepColumnStateInFileAndBack()
	{
		List<String> headers = headerTexts();
		choose("columnState", "FILE");
		choose("columnState", "SESSION");
		driver.navigate().refresh();
		wait.until(d -> d.findElements(By.cssSelector(ROWS)).size() == 25);
		js().executeScript(RECORD_ERRORS);
		assertTrue(headerTexts().equals(headers), "the columns changed: " + headers + " -> " +
			headerTexts());
		assertTrue(mixedCells().isEmpty(), "after the column state round trip: " + mixedCells());
	}

	/**
	 * Has the rows of the page pushed over the web socket, in reverse order, and waits for a push
	 * of the changed progress too.
	 */
	/**
	 * @return the button pushing the rows of the page, which only the JSON grid has
	 */
	private static By pushRowsButton()
	{
		return By.xpath("//button[normalize-space()='" + PUSH_ROWS + "']");
	}

	private void pushRows()
	{
		String first = (String)js().executeScript(
			"return document.querySelector('table.ant-dataview tr[data-key]').getAttribute('data-key');");
		driver.findElement(pushRowsButton()).click();
		wait.until(d -> first.equals(js().executeScript("return document.querySelector(" +
			"'table.ant-dataview tr[data-key]').getAttribute('data-key');")) == false);
		try
		{
			Thread.sleep(3500);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		assertTrue(mixedCells().isEmpty(), "after pushing the rows: " + mixedCells());
	}

	private void move(String header, Keys key, int times)
	{
		for (int i = 0; i < times; i++)
		{
			List<String> before = headerTexts();
			WebElement mover = driver.findElements(By.cssSelector(TABLE +
				" tr.ant-data-table-headers > th"))
				.stream()
				.filter(th -> th.getText().replaceAll("[\u25b2\u25bc]", "").trim().equals(header))
				.findFirst()
				.orElseThrow()
				.findElement(By.className("antilia-col-mover"));
			js().executeScript("arguments[0].focus();", mover);
			new Actions(driver).sendKeys(key).perform();
			wait.until(d -> headerTexts().equals(before) == false);
			wait.until(d -> d.findElements(By.cssSelector(".wicket-veil")).isEmpty());
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> headerTexts()
	{
		return (List<String>)js().executeScript("return Array.from(document.querySelectorAll(" +
			"'table.ant-dataview tr.ant-data-table-headers > th')).map(function (th) {" +
			" return th.textContent.replace(/[\\u25b2\\u25bc]/g, '').trim(); });");
	}

	private void awaitRenderedAgain(WebElement table)
	{
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
		wait.until(d -> d.findElements(By.cssSelector(ROWS)).size() == 25);
		wait.until(d -> d.findElements(By.cssSelector(".wicket-veil")).isEmpty());
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
		Dropdowns.choose(driver, driver.findElement(By.name("options:" + name)),
			SETTINGS.getOrDefault(name, OTHER_SETTINGS.get(name)).indexOf(value));
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
