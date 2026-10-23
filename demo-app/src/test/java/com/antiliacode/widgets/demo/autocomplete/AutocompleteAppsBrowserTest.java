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
package com.antiliacode.widgets.demo.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link AutocompletePage} in their tabs: the users searched
 * through the users resource, the countries filtered in the browser, the owner required; and the
 * find bar, also in Wicket. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class AutocompleteAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void choosesAnOwnerAndATeamFromTheUsers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "autocomplete");
		assertEquals("Owner: nobody; team: nobody; countries: nobody.", choices());

		String owner = choose("owner", "an", 0);
		wait.until(d -> choices().startsWith("Owner: " + owner + ";"));

		String first = choose("team", "al", 0);
		String second = choose("team", "ro", 1);
		wait.until(d -> choices().contains("team: " + first + ", " + second + ";"));
		assertEquals(List.of(first, second), chips("team"));

		field("team").findElement(By.cssSelector(".antilia-chip-remove")).click();
		wait.until(d -> choices().contains("team: " + second + ";"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void suggestsCountriesWithTheirFlagsFromTheDownArrow(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "autocomplete");

		text("countries").sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> suggestions("countries").size() == 10);
		WebElement first = suggestions("countries").get(0);
		assertEquals("🇦🇫", first.findElement(By.cssSelector(".ac-flag")).getText());
		assertEquals("Afghanistan", first.getText().replace("🇦🇫", "").trim());

		text("countries").sendKeys("zeal");
		wait.until(d -> suggestions("countries").size() == 1);
		suggestions("countries").get(0).click();

		wait.until(d -> choices().endsWith("countries: New Zealand."));
		assertEquals(List.of("New Zealand"), chips("countries"));
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void findsCountriesIntoAListAndKeepsTheBarEmpty(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "autocomplete");
		assertTrue(visits().isEmpty());

		text("visitFind").sendKeys("zeal");
		wait.until(d -> suggestions("visitFind").size() == 1);
		suggestions("visitFind").get(0).click();
		wait.until(d -> visits().equals(List.of("New Zealand")));
		assertTrue(chips("visitFind").isEmpty());
		assertEquals("", text("visitFind").getDomProperty("value"));

		// the countries listed are not suggested again
		text("visitFind").sendKeys("zeal");
		wait.until(d -> app().findElements(By.cssSelector(".antilia-autocomplete-status"))
			.stream().anyMatch(WebElement::isDisplayed));
		assertTrue(suggestions("visitFind").isEmpty());
		text("visitFind").clear();

		text("visitFind").sendKeys("chil");
		wait.until(d -> suggestions("visitFind").size() == 1);
		text("visitFind").sendKeys(Keys.ENTER);
		wait.until(d -> visits().equals(List.of("New Zealand", "Chile")));

		app().findElement(By.cssSelector(".autocomplete-demo__remove")).click();
		wait.until(d -> visits().equals(List.of("Chile")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheDetailsOfAUserWithF1AndCallsThemWithoutChoosing(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "autocomplete");
		text("owner").sendKeys("an");
		wait.until(d -> suggestions("owner").size() > 2 && suggestions("owner").stream()
			.allMatch(option -> option.getText().toLowerCase().contains("an")));
		WebElement tooltip = field("owner").findElement(By.cssSelector(
			".antilia-autocomplete-tooltip"));

		// the tooltip of the active row, which follows the arrows
		text("owner").sendKeys(Keys.F1);
		wait.until(d -> tooltip.isDisplayed());
		assertTrue(tooltip.getText().startsWith(name(suggestions("owner").get(0))));
		text("owner").sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> tooltip.getText().startsWith(name(suggestions("owner").get(1))));
		text("owner").sendKeys(Keys.F1);
		wait.until(d -> !tooltip.isDisplayed());

		WebElement second = suggestions("owner").get(1);
		String user = name(second);
		second.findElement(By.cssSelector(".antilia-autocomplete-action[data-name='call']"))
			.click();
		wait.until(d -> app().findElement(By.cssSelector(".autocomplete-demo__calling"))
			.getText().startsWith("Calling " + user + " at "));
		assertTrue(chips("owner").isEmpty());

		text("owner").sendKeys(Keys.ENTER);
		wait.until(d -> chips("owner").equals(List.of(user)));
		assertNoErrors();
	}

	private static String name(WebElement suggestion)
	{
		return suggestion.findElement(By.tagName("b")).getText();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheValuesOfADisabledAutocompleteWithoutChangingThem(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "autocomplete");

		assertEquals(1, chips("reviewer").size());
		assertEquals(List.of("Spain", "Japan", "Brazil"), chips("markets"));
		for (String name : List.of("reviewer", "markets"))
		{
			assertTrue(field(name).getDomAttribute("class").contains("antilia-autocomplete-disabled"));
			assertTrue(field(name).findElements(By.cssSelector(".antilia-chip-remove")).isEmpty());
			assertFalse(text(name).isEnabled());
		}
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void sendsOnlyWithAnOwner(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "autocomplete");

		send();
		wait.until(d -> feedback().equals("'Owner (required)' is required."));
		assertTrue(app().findElement(By.cssSelector(".feedbackPanel li"))
			.getDomAttribute("class").contains("feedbackPanelERROR"));

		String owner = choose("owner", "an", 0);
		send();
		wait.until(d -> feedback().equals("Sent: Owner: " + owner +
			"; team: nobody; countries: nobody."));
	}

	/**
	 * Types into an autocomplete and chooses a suggestion.
	 *
	 * @return the name of the user chosen
	 */
	private String choose(String name, String typed, int index)
	{
		text(name).sendKeys(typed);
		// the suggestions for the text, not those of a former one
		wait.until(d -> suggestions(name).size() > index && suggestions(name).stream()
			.allMatch(option -> option.isDisplayed() &&
				option.getText().toLowerCase().contains(typed)));
		WebElement suggestion = suggestions(name).get(index);
		String label = suggestion.findElement(By.tagName("b")).getText();
		suggestion.click();
		wait.until(d -> chips(name).contains(label));
		return label;
	}

	private void send()
	{
		app().findElement(By.cssSelector(".autocomplete-demo__send")).click();
	}

	private String feedback()
	{
		return app().findElement(By.cssSelector(".feedbackPanel")).getText();
	}

	private String choices()
	{
		return app().findElement(By.cssSelector(".autocomplete-demo__choices")).getText();
	}

	/**
	 * @return the names of the countries listed below the find bar
	 */
	private List<String> visits()
	{
		return app().findElements(By.cssSelector(".autocomplete-demo__list li")).stream()
			.map(item -> item.findElement(By.cssSelector(".ac-country")).getText()
				.replace(item.findElement(By.cssSelector(".ac-flag")).getText(), "").trim())
			.toList();
	}

	private WebElement field(String name)
	{
		return app().findElement(By.name(name)).findElement(By.xpath(".."));
	}

	private WebElement text(String name)
	{
		return field(name).findElement(By.cssSelector(".antilia-autocomplete-text"));
	}

	private List<String> chips(String name)
	{
		return field(name).findElements(By.cssSelector(".antilia-chip-text")).stream()
			.map(WebElement::getText)
			.toList();
	}

	private List<WebElement> suggestions(String name)
	{
		return field(name).findElements(By.cssSelector(".antilia-autocomplete-option"));
	}
}
