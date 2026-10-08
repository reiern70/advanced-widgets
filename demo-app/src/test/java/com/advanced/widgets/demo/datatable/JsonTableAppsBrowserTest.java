/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.advanced.widgets.demo.Dropdowns;
import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;

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
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void pagesSortsAndFiltersTheUsers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);
		assertEquals("Showing 1 to 25 of 1000", label());
		assertEquals(List.of("", "#", "Actions", "ID", "First Name", "Last Name", "Born",
			"Address", "Phones", "Progress", ""), headers());

		app().findElement(By.cssSelector("thead a.next")).click();
		wait.until(d -> label().equals("Showing 26 to 50 of 1000"));
		assertEquals("26", cell(0, 1));

		header("Last Name").findElement(By.cssSelector("a.dynamic-data-table-sort")).click();
		wait.until(d -> label().equals("Showing 1 to 25 of 1000") &&
			"ascending".equals(header("Last Name").getDomAttribute("aria-sort")));
		wait.until(d -> isSorted(column(5)));

		app().findElement(By.cssSelector(".user-filter input")).sendKeys("Rossi");
		wait.until(d -> !rows().isEmpty() && column(5).stream().allMatch("Rossi"::equals));
		assertTrue(label().matches("Showing 1 to \\d+ of \\d+"), label());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsTheHiddenColumnsWithTheChooserAndCountsTheSelection(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);

		WebElement chooser = app().findElement(By.cssSelector(
			"thead .dynamic-data-table-column-chooser"));
		chooser.findElement(By.tagName("summary")).click();
		chooser.findElement(By.cssSelector("[data-dt-column='city']")).click();
		chooser.findElement(By.cssSelector("[data-dt-apply]")).click();
		wait.until(d -> headers().contains("City") && rows().size() == 25);

		rows().get(0).findElement(By.cssSelector(".dynamic-data-table-select")).click();
		rows().get(2).findElement(By.cssSelector(".dynamic-data-table-select")).click();
		wait.until(d -> selection().equals("Selected: 2 user(s)."));
		app().findElement(By.cssSelector(".dynamic-data-table-select-page")).click();
		wait.until(d -> selection().equals("Selected: 25 user(s)."));
		assertTrue(rows().get(4).getDomAttribute("class").contains("selected"));
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void editsAndDeletesAUserInTheApp(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);
		String lastName = cell(1, 5);
		String id = cell(1, 3);

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
		dialog().findElement(By.cssSelector(".dynamic-data-table-button-primary")).click();

		wait.until(d -> !column(3).contains(id));
		assertEquals("Deleted Zoe " + lastName + ".", app().findElement(
			By.cssSelector(".feedbackPanel")).getText());
		assertEquals("Showing 1 to 24 of 999", label());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void scrollsInfinitely(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "dynamic-data-table");
		wait.until(d -> rows().size() == 25);

		Dropdowns.choose(driver, app().findElement(By.name("infiniteScroll")), 1);
		wait.until(d -> app().findElements(By.cssSelector(".navigator")).isEmpty() &&
			rows().size() == 25);

		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollTop = arguments[0].scrollHeight;",
			app().findElement(By.cssSelector(".dynamic-data-table-body")));
		wait.until(d -> rows().size() == 50);
		wait.until(d -> label().equals("Showing 1 to 50 of 1000"));
		assertEquals("50", cell(49, 1));
	}

	private List<WebElement> rows()
	{
		return app().findElements(By.cssSelector("table.dynamic-data-table tbody tr[data-key]"));
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
		return app().findElements(By.cssSelector("tr.dynamic-data-table-headers th")).stream()
			.map(header -> header.getText().replaceAll("[▲▼]", "").trim())
			.toList();
	}

	private WebElement header(String text)
	{
		return app().findElements(By.cssSelector("tr.dynamic-data-table-headers th")).stream()
			.filter(header -> header.getText().contains(text))
			.findFirst()
			.orElseThrow();
	}

	private String label()
	{
		return app().findElement(By.cssSelector("thead .navigatorLabel")).getText();
	}

	private String selection()
	{
		return app().findElement(By.cssSelector(".dynamic-dataview-selection")).getText();
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
