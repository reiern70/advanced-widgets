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
package com.antiliacode.widgets.demo.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the Wicket demo of {@link JsonListPage}: its filter leaves the page usable while the user
 * types, and veils the cards while it asks for them again (#138). Runs with {@code -Pselenium},
 * see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class JsonListBrowserTest extends FrameworkAppsTestCase
{
	/**
	 * Records where each veil of wicket-extensions goes as it is raised: the markup id of its
	 * host, or {@code page} for one over the whole page.
	 */
	private static final String RECORD_VEILS = "window.veilsRaised = [];" +
		"new MutationObserver(changes => changes.forEach(change => change.addedNodes.forEach(" +
		"node => { if (node.classList && node.classList.contains('wicket-veil')) {" +
		"window.veilsRaised.push(node.parentNode === document.body ? 'page' : " +
		"node.parentNode.id); } }))).observe(document.body, {childList: true, subtree: true});";

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void theFilterVeilsTheCardsAndLeavesTheFieldAlone(SeleniumBrowser browser)
	{
		open(browser, "wicket", "json-infinite-list");
		wait.until(d -> cards().size() == JsonListPage.USERS_PER_PAGE);
		String listId = list().getDomAttribute("id");
		JavascriptExecutor script = (JavascriptExecutor)driver;
		script.executeScript(RECORD_VEILS);

		WebElement filter = app().findElement(By.cssSelector(".user-filter input"));
		filter.sendKeys("Rossi");
		wait.until(d -> !cards().isEmpty() &&
			names().stream().allMatch(name -> name.contains("Rossi")));

		@SuppressWarnings("unchecked")
		List<String> raised = (List<String>)script.executeScript("return window.veilsRaised;");
		assertTrue(raised.contains(listId), "the cards were veiled: " + raised);
		assertFalse(raised.contains("page"), "the page was not: " + raised);
		// the user typed on, in the field, which nothing covered
		assertEquals("Rossi", filter.getDomProperty("value"));
		assertEquals(filter, driver.switchTo().activeElement());
		wait.until(d -> list().findElements(By.cssSelector(".wicket-veil")).isEmpty());
		assertNoErrors();
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
}
