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
package com.antiliacode.widgets.demo.list;

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

import com.antiliacode.widgets.demo.Dropdowns;
import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link JsonListPage} in their tabs: the cards of the users
 * from the users resource, loading more as the list scrolls, filtered and sorted, and the actions
 * of a card handled in the app. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class JsonListAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void loadsMoreCardsAsTheListScrolls(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "json-infinite-list");
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE);
		assertTrue(cards().get(0).getText().contains("#1 · ID"), cards().get(0).getText());

		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollTop = arguments[0].scrollHeight;", list());

		wait.until(d -> cards().size() == 2 * JsonListPage.USERS_PER_PAGE);
		assertTrue(cards().get(24).getText().contains("#25 · ID"), cards().get(24).getText());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void filtersAndSortsTheUsers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "json-infinite-list");
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE);
		assertTrue(isSorted(names().stream().map(name -> name.split(" ")[0]).toList()),
			"by first name");

		Dropdowns.choose(driver, app().findElement(By.name("sort")), 1);
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE &&
			isSorted(names().stream().map(name -> name.split(" ")[1]).toList()));

		app().findElement(By.cssSelector(".user-filter input")).sendKeys("Rossi");
		wait.until(d -> !cards().isEmpty() &&
			names().stream().allMatch(name -> name.contains("Rossi")));
	}

	private static boolean isSorted(List<String> names)
	{
		return names.size() > 1 && names.equals(names.stream().sorted().toList());
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void showsEditsAndDeletesAUserInTheApp(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "json-infinite-list");
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE);
		String name = names().get(1);
		String lastName = name.substring(name.indexOf(' ') + 1);

		cards().get(1).findElement(By.cssSelector(".user-card__name")).click();
		wait.until(d -> details().contains(lastName));

		cards().get(1).findElement(By.cssSelector("[data-dt-action='edit']")).click();
		WebElement dialog = dialog();
		assertEquals("Edit " + name, dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		WebElement firstName = dialog.findElement(By.name("firstName"));
		// typed over as a user does, so that the framework sees every change
		firstName.sendKeys(Keys.END);
		firstName.sendKeys(Keys.BACK_SPACE.toString().repeat(name.indexOf(' ')) + "Zoe");
		dialog.findElement(By.cssSelector("button[type='submit']")).click();

		wait.until(d -> names().get(1).equals("Zoe " + lastName));
		wait.until(d -> details().contains("Zoe"));
		assertTrue(app().findElements(By.cssSelector(".modal-dialog")).isEmpty());

		cards().get(1).findElement(By.cssSelector("[data-dt-action='delete']")).click();
		assertEquals("Delete Zoe " + lastName + "?", dialog().findElement(By.tagName("p"))
			.getText());
		dialog().findElement(By.cssSelector(".ant-data-table-button-primary")).click();

		wait.until(d -> !names().contains("Zoe " + lastName));
		assertEquals("Deleted Zoe " + lastName + ".", feedback());
		assertEquals("Click a card to see the user here.", app().findElement(
			By.cssSelector(".user-list-demo__none")).getText());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void addsAUserEveryFieldRequired(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "json-infinite-list");
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE);

		app().findElement(By.cssSelector(".user-list-demo__toolbar .ant-data-table-button"))
			.click();
		WebElement dialog = dialog();
		assertEquals("New user", dialog.findElement(By.cssSelector(".modal-dialog-title"))
			.getText());
		dialog.findElement(By.cssSelector("button[type='submit']")).click();
		wait.until(d -> dialog().findElements(By.cssSelector(".feedbackPanelERROR span"))
			.size() == 5);
		assertEquals("'First name' is required.", dialog().findElement(
			By.cssSelector(".feedbackPanelERROR span")).getText());
		assertEquals("true", dialog().findElement(By.name("firstName"))
			.getDomAttribute("aria-invalid"));

		dialog().findElement(By.name("firstName")).sendKeys("Ada");
		dialog().findElement(By.name("lastName")).sendKeys("Lovelace");
		dialog().findElement(By.name("city")).sendKeys("London");
		dialog().findElement(By.name("address")).sendKeys("12 St James's Square");
		WebElement country = dialog().findElement(By.name("country"));
		country.findElement(By.xpath("..")).findElement(By.cssSelector(".antilia-dropdown-toggle"))
			.click();
		WebElement filter = country.findElement(By.xpath(".."))
			.findElement(By.cssSelector(".antilia-dropdown-filter"));
		filter.sendKeys("United Kingdom");
		filter.sendKeys(Keys.ENTER);
		dialog().findElement(By.cssSelector("button[type='submit']")).click();

		wait.until(d -> names().get(0).equals("Ada Lovelace"));
		assertTrue(cards().get(0).getText().contains("London, United Kingdom"),
			cards().get(0).getText());
		assertEquals("Added Ada Lovelace.", feedback());
		assertTrue(details().contains("Lovelace"));
	}

	private WebElement list()
	{
		return app().findElement(By.cssSelector(".json-infinite-list"));
	}

	private List<WebElement> cards()
	{
		return list().findElements(By.cssSelector(".json-infinite-list-item"));
	}

	private List<String> names()
	{
		return list().findElements(By.cssSelector(".user-card__name")).stream()
			.map(WebElement::getText)
			.toList();
	}

	private String details()
	{
		return app().findElement(By.cssSelector(".user-list-demo__details")).getText();
	}

	private WebElement dialog()
	{
		return wait.until(d -> app().findElement(By.cssSelector(".modal-dialog")));
	}

	private String feedback()
	{
		return app().findElement(By.cssSelector(".feedbackPanel")).getText();
	}
}
