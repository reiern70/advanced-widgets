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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte tables of {@link DynamicDataTablePage} in their tabs: the skeleton
 * the framework renders, the rows the core renders from the users resource, paging, sorting,
 * filtering, the column chooser, the selection, and the actions of a row handled in the app. Runs
 * with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class JsonTableAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void pagesSortsAndFiltersTheUsers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);
		// the users resource saves and deletes them: other tests may have deleted some
		assertTrue(label().matches("Showing 1 to 25 of \\d+"), label());
		String users = label().replaceAll(".* of ", "");
		assertEquals(List.of("", "#", "Actions", "ID", "First Name", "Last Name", "Born",
			"Address", "Phones", "Progress", ""), headers());

		app().findElement(By.cssSelector("thead a.next")).click();
		wait.until(d -> label().equals("Showing 26 to 50 of " + users));
		assertEquals("26", cell(0, 1));

		header("Last Name").findElement(By.cssSelector("a.ant-data-table-sort")).click();
		wait.until(d -> label().equals("Showing 1 to 25 of " + users) &&
			"ascending".equals(header("Last Name").getDomAttribute("aria-sort")));
		wait.until(d -> isSorted(column(5)));

		app().findElement(By.cssSelector(".user-filter input")).sendKeys("Rossi");
		wait.until(d -> !rows().isEmpty() && column(5).stream().allMatch("Rossi"::equals));
		assertTrue(label().matches("Showing 1 to \\d+ of \\d+"), label());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsTheHiddenColumnsWithTheChooserAndCountsTheSelection(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);

		WebElement chooser = app().findElement(By.cssSelector(
			"thead .ant-data-table-column-chooser"));
		chooser.findElement(By.tagName("summary")).click();
		chooser.findElement(By.cssSelector("[data-dt-column='city']")).click();
		chooser.findElement(By.cssSelector("[data-dt-apply]")).click();
		wait.until(d -> headers().contains("City") && rows().size() == 25);

		rows().get(0).findElement(By.cssSelector(".ant-data-table-select")).click();
		rows().get(2).findElement(By.cssSelector(".ant-data-table-select")).click();
		wait.until(d -> selection().equals("2 selected"));
		app().findElement(By.cssSelector(".ant-data-table-select-page")).click();
		wait.until(d -> selection().equals("25 selected"));
		assertTrue(rows().get(4).getDomAttribute("class").contains("selected"));

		app().findElement(By.cssSelector("thead .ant-data-table-selection-clear")).click();
		wait.until(d -> selection().isEmpty());
		assertFalse(rows().get(4).getDomAttribute("class").contains("selected"));
		assertFalse(app().findElement(By.cssSelector(".ant-data-table-select-page")).isSelected());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsTheNavigationOnTopAndClearsTheSelectionOnPagingWhenChosen(
		SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);
		assertTrue(app().findElements(By.cssSelector("tfoot .ant-data-table-navigation")).isEmpty(),
			"only the top navigation by default");

		rows().get(0).findElement(By.cssSelector(".ant-data-table-select")).click();
		wait.until(d -> selection().equals("1 selected"));
		app().findElement(By.cssSelector("thead a.next")).click();
		wait.until(d -> label().startsWith("Showing 26 to 50"));
		assertEquals("1 selected", selection(), "paging keeps the selection by default");

		Dropdowns.choose(driver, app().findElement(By.name("clearSelectionOnNavigation")), 1);
		app().findElement(By.cssSelector("thead a.prev")).click();
		wait.until(d -> label().startsWith("Showing 1 to 25"));
		wait.until(d -> selection().isEmpty());
		assertFalse(rows().get(0).findElement(By.cssSelector(".ant-data-table-select"))
			.isSelected());

		Dropdowns.choose(driver, app().findElement(By.name("navigation")), 2);
		wait.until(d -> app().findElements(By.cssSelector("tfoot .ant-data-table-navigation"))
			.size() == 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void editsAndDeletesAUserInTheApp(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);
		String lastName = cell(1, 5);
		String id = cell(1, 3);
		// the users resource saves and deletes them: other tests may have deleted some
		int users = Integer.parseInt(label().replaceAll(".* of ", ""));

		rows().get(1).findElement(By.cssSelector("[data-dt-action='edit']")).click();
		WebElement dialog = dialog();
		WebElement firstName = dialog.findElement(By.name("firstName"));
		firstName.sendKeys(Keys.END);
		firstName.sendKeys(Keys.BACK_SPACE.toString().repeat(
			firstName.getDomProperty("value").length()) + "Zoe");
		dialog.findElement(By.cssSelector("button[type='submit']")).click();
		wait.until(d -> cell(1, 4).equals("Zoe"));
		assertEquals(id, cell(1, 3));

		rows().get(1).findElement(By.cssSelector("[data-dt-action='delete']")).click();
		assertEquals("Delete Zoe " + lastName + "?", dialog().findElement(By.tagName("p"))
			.getText());
		dialog().findElement(By.cssSelector(".ant-button--danger")).click();

		wait.until(d -> !column(3).contains(id));
		assertEquals("Deleted Zoe " + lastName + ".", app().findElement(
			By.cssSelector(".feedbackPanel")).getText());
		wait.until(d -> label().equals("Showing 1 to 25 of " + (users - 1)));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsAndChangesALocalListOfUsers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);

		Dropdowns.choose(driver, app().findElement(By.name("rowsFrom")), 1);
		wait.until(d -> label().equals("Showing 1 to 25 of 200"));
		String lastName = cell(0, 5);

		rows().get(0).findElement(By.cssSelector("[data-dt-action='edit']")).click();
		WebElement firstName = dialog().findElement(By.name("firstName"));
		firstName.sendKeys(Keys.END);
		firstName.sendKeys(Keys.BACK_SPACE.toString().repeat(
			firstName.getDomProperty("value").length()) + "Local");
		dialog().findElement(By.cssSelector("button[type='submit']")).click();
		wait.until(d -> cell(0, 4).equals("Local"));

		app().findElement(By.cssSelector(".user-filter input")).sendKeys(lastName);
		wait.until(d -> !rows().isEmpty() && column(5).stream().allMatch(lastName::equals));

		Dropdowns.choose(driver, app().findElement(By.name("selectionCount")), 1);
		rows().get(0).findElement(By.cssSelector(".ant-data-table-select")).click();
		assertEquals("", selection(), "the count is hidden");
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void scrollsInfinitely(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);

		Dropdowns.choose(driver, app().findElement(By.name("infinite")), 1);
		wait.until(d -> app().findElements(By.cssSelector(".navigator")).isEmpty() &&
			rows().size() == 25);

		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollTop = arguments[0].scrollHeight;",
			app().findElement(By.cssSelector(".ant-data-table-body")));
		wait.until(d -> rows().size() == 50);
		wait.until(d -> label().matches("Showing 1 to 50 of \\d+"));
		assertEquals("50", cell(49, 1));
	}

	private List<WebElement> rows()
	{
		return app().findElements(By.cssSelector("table.ant-data-table tbody tr[data-key]"));
	}

	private String cell(int row, int column)
	{
		return rows().get(row).findElements(By.tagName("td")).get(column).getText();
	}

	private List<String> column(int column)
	{
		return rows().stream()
			.map(row -> row.findElements(By.tagName("td")).get(column).getText())
			.toList();
	}

	private List<String> headers()
	{
		return app().findElements(By.cssSelector("tr.ant-data-table-headers th")).stream()
			.map(header -> header.getText().replaceAll("[▲▼]", "").trim())
			.toList();
	}

	private WebElement header(String text)
	{
		return app().findElements(By.cssSelector("tr.ant-data-table-headers th")).stream()
			.filter(header -> header.getText().contains(text))
			.findFirst()
			.orElseThrow();
	}

	private String label()
	{
		return app().findElement(By.cssSelector("thead .navigatorLabel")).getText();
	}

	/**
	 * @return how many users are selected, as the top navigation tells, or nothing
	 */
	private String selection()
	{
		List<WebElement> count = app().findElements(By.cssSelector(
			"thead .ant-data-table-selection-count"));
		return count.isEmpty() ? "" : count.get(0).getText();
	}

	private WebElement dialog()
	{
		return wait.until(d -> app().findElement(By.cssSelector(".modal-dialog")));
	}

	private static boolean isSorted(List<String> names)
	{
		return names.size() > 1 && names.equals(names.stream().sorted().toList());
	}
}
