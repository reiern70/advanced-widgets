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
package com.antiliacode.widgets.demo.breadcrumb;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Drives {@link BreadcrumbsPage} in Chrome and in Firefox, in Wicket, React and Svelte, Wicket in
 * deployment mode so through the minified script built from frontend/core: the trail of the
 * folders collapsed, shown by the button "…" with the mouse and the keys, crumbs going back and
 * folders going deeper, the site's pages, and the Bootstrap look. Runs with {@code -Pselenium},
 * see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class BreadcrumbsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheCrumbsHiddenAndGoesBackAndDeeper(SeleniumBrowser browser, String framework)
	{
		openCase(browser, framework, "Folders");
		wait.until(d -> shown().equals(List.of("My drive", "…", "Projects", "widgets", "src")));
		WebElement more = more();
		assertEquals("Show the whole path (1 more)", more.getDomAttribute("aria-label"));

		more.click();

		wait.until(d -> shown().equals(List.of("My drive", "Documents", "Projects", "widgets",
			"src")));
		wait.until(d -> "Documents".equals(focused().getText().trim()));

		crumb("Projects").click();
		wait.until(d -> shown().equals(List.of("My drive", "Documents", "Projects")));
		assertEquals("Projects", current());

		place("widgets").click();
		wait.until(d -> shown().equals(List.of("My drive", "Documents", "Projects", "widgets")));
		place("src").click();
		place("main").click();
		// deeper than four crumbs: collapsed again
		wait.until(d -> shown().equals(List.of("My drive", "…", "widgets", "src", "main")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsTheCrumbsHiddenWithTheKeys(SeleniumBrowser browser, String framework)
	{
		openCase(browser, framework, "Settings");
		wait.until(d -> shown().equals(List.of("My drive", "…", "src", "main", "java")));

		more().sendKeys(Keys.ENTER);

		wait.until(d -> shown().size() == 7);
		wait.until(d -> "Documents".equals(focused().getText().trim()));
		focused().sendKeys(Keys.TAB);
		wait.until(d -> "Projects".equals(focused().getText().trim()));
		focused().sendKeys(Keys.ENTER);
		wait.until(d -> status().startsWith("You chose Projects"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void leadsToThePagesOfTheSite(SeleniumBrowser browser, String framework)
	{
		openCase(browser, framework, "Site");
		wait.until(d -> shown().equals(List.of("Home")));

		place("Products").click();
		wait.until(d -> shown().equals(List.of("Home", "Products")));
		place("Computers").click();
		wait.until(d -> shown().equals(List.of("Home", "Products", "Computers")));
		assertEquals("Computers", current());
		if ("wicket".equals(framework))
		{
			assertTrue(driver.getCurrentUrl().contains("path=products/computers"),
				driver.getCurrentUrl());
		}

		crumb("Home").click();
		wait.until(d -> shown().equals(List.of("Home")));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void takesBootstrapsLook(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "breadcrumbs");
		chooseLook("Bootstrap");
		selectCase("Folders");
		wait.until(d -> shown().size() == 5);

		WebElement list = trail().findElement(By.cssSelector(".antilia-breadcrumb-list"));
		assertTrue(list.getDomAttribute("class").contains("breadcrumb"), list.getDomAttribute("class"));
		List<WebElement> crumbs = list.findElements(By.xpath("./li"));
		assertTrue(crumbs.get(crumbs.size() - 1).getDomAttribute("class").contains("active"));
		more().click();
		wait.until(d -> shown().size() == 5 && !shown().contains("…"));
		assertNoErrors();
	}

	private void openCase(SeleniumBrowser browser, String framework, String title)
	{
		open(browser, framework, "breadcrumbs");
		selectCase(title);
	}

	/**
	 * @return the breadcrumb of the case shown
	 */
	private WebElement trail()
	{
		return app().findElements(By.cssSelector("nav.antilia-breadcrumb")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElseThrow();
	}

	/**
	 * @return the labels of the crumbs shown, "…" for the button showing those hidden
	 */
	private List<String> shown()
	{
		return trail().findElements(By.cssSelector(".antilia-breadcrumb-list > li")).stream()
			.filter(WebElement::isDisplayed)
			.map(item -> item.getDomAttribute("class").contains("antilia-breadcrumb-more") ? "…" :
				item.findElement(By.cssSelector(".antilia-breadcrumb-label")).getText().trim())
			.toList();
	}

	private WebElement more()
	{
		return trail().findElement(By.cssSelector(".antilia-breadcrumb-more-button"));
	}

	/**
	 * @return the link or the button of a crumb shown, by its label
	 */
	private WebElement crumb(String label)
	{
		return trail().findElements(By.cssSelector(".antilia-breadcrumb-link")).stream()
			.filter(link -> link.isDisplayed() && link.getText().trim().equals(label)).findFirst()
			.orElseThrow();
	}

	/**
	 * @return the label of the last crumb, the page shown
	 */
	private String current()
	{
		return trail().findElement(By.cssSelector("[aria-current='page']")).getText().trim();
	}

	/**
	 * @return the link or the button of a page or a folder below the one shown, by its label
	 */
	private WebElement place(String label)
	{
		return wait.until(d -> app().findElements(By.cssSelector(".breadcrumbs-demo__place"))
			.stream().filter(link -> link.isDisplayed() && link.getText().trim().equals(label))
			.findFirst().orElse(null));
	}

	private WebElement focused()
	{
		return driver.switchTo().activeElement();
	}

	/**
	 * @return what the settings say, below their trail
	 */
	private String status()
	{
		return app().findElements(By.cssSelector(".breadcrumbs-demo__status")).stream()
			.filter(WebElement::isDisplayed).findFirst().map(WebElement::getText).orElse("");
	}
}
