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
package com.antilia.widgets.demo.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link MenusPage} in Chrome and in Firefox, in Wicket, React and Svelte, Wicket in
 * deployment mode so through the minified script built from frontend/core: submenus opened by the
 * mouse and by the keys, items chosen, the navigation of the site and its current item, the side
 * bar opening in place, where the submenus pop up, and the Bootstrap look. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class MenusBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void opensSubmenusWithTheMouseAndChoosesAnItem(SeleniumBrowser browser, String framework)
	{
		openMenus(browser, framework);
		WebElement menu = application();
		WebElement file = item(menu, "File");
		assertEquals("menubar", menu.findElement(By.cssSelector(".antilia-menu-list"))
			.getDomAttribute("role"));
		assertFalse(submenuOf(file).isDisplayed());

		file.click();
		wait.until(d -> submenuOf(file).isDisplayed());
		assertEquals("true", file.getDomAttribute("aria-expanded"));
		// the pointer resting on Export opens its submenu
		WebElement export = item(menu, "Export");
		new Actions(driver).moveToElement(export).perform();
		wait.until(d -> submenuOf(export).isDisplayed());
		screenshot("export");
		WebElement pdf = item(menu, "PDF");
		new Actions(driver).moveToElement(export).moveToElement(pdf).click().perform();

		wait.until(d -> status().startsWith("You chose File › Export › PDF"));
		wait.until(d -> !submenuOf(file).isDisplayed());
		assertFalse(submenuOf(export).isDisplayed());
		assertEquals("false", file.getDomAttribute("aria-expanded"));

		// a disabled item does nothing
		item(menu, "Edit").click();
		WebElement paste = item(menu, "Paste");
		wait.until(d -> paste.isDisplayed());
		assertEquals("true", paste.getDomAttribute("aria-disabled"));
		paste.click();
		assertTrue(submenuOf(item(menu, "Edit")).isDisplayed(), "still open");
		assertTrue(status().startsWith("You chose File › Export › PDF"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void drivesTheMenuWithTheKeys(SeleniumBrowser browser, String framework)
	{
		openMenus(browser, framework);
		WebElement menu = application();
		WebElement file = item(menu, "File");
		assertEquals("0", file.getDomAttribute("tabindex"), "the bar takes the focus with Tab");

		file.sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> focused().equals(item(menu, "New")));
		focused().sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> focused().equals(item(menu, "Open recent")));
		focused().sendKeys(Keys.ARROW_RIGHT);
		wait.until(d -> focused().equals(item(menu, "notes.txt")));
		focused().sendKeys(Keys.ARROW_LEFT);
		wait.until(d -> focused().equals(item(menu, "Open recent")));
		assertFalse(submenuOf(item(menu, "Open recent")).isDisplayed());
		focused().sendKeys(Keys.ESCAPE);
		wait.until(d -> focused().equals(file));
		assertFalse(submenuOf(file).isDisplayed());

		file.sendKeys(Keys.ARROW_RIGHT);
		WebElement edit = item(menu, "Edit");
		wait.until(d -> focused().equals(edit));
		edit.sendKeys(Keys.ENTER);
		wait.until(d -> focused().equals(item(menu, "Undo")));
		// a letter: the next item starting with it
		focused().sendKeys("c");
		wait.until(d -> focused().equals(item(menu, "Cut")));
		focused().sendKeys("c");
		wait.until(d -> focused().equals(item(menu, "Copy")));
		focused().sendKeys(Keys.ENTER);
		wait.until(d -> status().startsWith("You chose Edit › Copy"));
		wait.until(d -> !submenuOf(edit).isDisplayed());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void popsUpBelowTheBarAndBesideTheItemsOfSubmenus(SeleniumBrowser browser, String framework)
	{
		openMenus(browser, framework);
		WebElement menu = application();
		WebElement file = item(menu, "File");
		file.click();
		WebElement fileMenu = submenuOf(file);
		wait.until(d -> fileMenu.isDisplayed());
		Rectangle bar = file.getRect();
		Rectangle below = fileMenu.getRect();
		assertTrue(Math.abs(below.getY() - (bar.getY() + bar.getHeight())) <= 2,
			"below its item: " + below + " " + bar);
		assertTrue(Math.abs(below.getX() - bar.getX()) <= 2, "from its left edge");

		WebElement recent = item(menu, "Open recent");
		recent.click();
		WebElement recentMenu = submenuOf(recent);
		wait.until(d -> recentMenu.isDisplayed());
		Rectangle item = recent.getRect();
		Rectangle beside = recentMenu.getRect();
		assertTrue(beside.getX() >= item.getX() + item.getWidth() - 2, "beside its item");
		assertEquals("beside", recentMenu.getDomAttribute("data-placement"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void leadsToTheSectionsOfTheSiteMarkingTheCurrentOne(SeleniumBrowser browser, String framework)
	{
		openMenus(browser, framework);
		selectCase("Site navigation");
		WebElement menu = menuIn(".menus-demo__navigation");
		assertEquals("page", item(menu, "Welcome").getDomAttribute("aria-current"));

		// the submenus of the bar open on hover
		WebElement products = item(menu, "Products");
		new Actions(driver).moveToElement(products).perform();
		wait.until(d -> submenuOf(products).isDisplayed());
		WebElement laptops = item(menu, "Laptops");
		new Actions(driver).moveToElement(products).moveToElement(laptops).click().perform();

		if ("wicket".equals(framework))
		{
			// a bookmarkable page, the section in its address
			wait.until(d -> d.getCurrentUrl().contains("section=laptops"));
			wait.until(d -> app().findElements(By.cssSelector(".menus-demo__where")).stream()
				.anyMatch(where -> where.getText().equals("You are at Products › Laptops.")));
		}
		else
		{
			wait.until(d -> where().equals("You are at Products › Laptops."));
		}
		WebElement shown = menuIn(".menus-demo__navigation");
		wait.until(d -> "page".equals(item(shown, "Laptops").getDomAttribute("aria-current")));
		assertTrue(item(shown, "Products").getDomAttribute("class")
			.contains("antilia-menu-current-parent"));
		assertEquals(null, item(shown, "Welcome").getDomAttribute("aria-current"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void opensTheSideBarInPlaceMarkingTheViewChosen(SeleniumBrowser browser, String framework)
	{
		openMenus(browser, framework);
		selectCase("Side bar");
		WebElement menu = menuIn(".menus-demo__sidebar");
		// the submenu holding the current view, open from the start
		assertTrue(submenuOf(item(menu, "Reports")).isDisplayed());
		assertEquals("page", item(menu, "Sales").getDomAttribute("aria-current"));

		WebElement people = item(menu, "People");
		people.click();
		wait.until(d -> submenuOf(item(menuIn(".menus-demo__sidebar"), "People")).isDisplayed());
		assertTrue(submenuOf(item(menu, "Reports")).isDisplayed(), "both open");
		// in place: under its item, not beside it
		Rectangle item = people.getRect();
		Rectangle list = submenuOf(people).getRect();
		assertTrue(list.getY() >= item.getY() + item.getHeight() - 1, "under its item");

		// with the keys, so the focus comes back after Ajax renders the side bar again
		people.sendKeys(Keys.ARROW_DOWN);
		wait.until(d -> focused().getDomProperty("textContent").trim().equals("Members"));
		String id = focused().getDomAttribute("id");
		focused().sendKeys(Keys.ENTER);
		wait.until(d -> app().findElements(By.cssSelector(".menus-demo__content .menus-demo__where"))
			.stream().anyMatch(where -> where.getText().equals("People › Members")));
		wait.until(d -> "page".equals(item(menuIn(".menus-demo__sidebar"), "Members")
			.getDomAttribute("aria-current")));
		assertEquals(id, focused().getDomAttribute("id"), "the focus kept");
		screenshot("sidebar");
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void drawsItsPartsWithBootstrapsClassesInTheBootstrapLook(SeleniumBrowser browser,
		String framework)
	{
		openMenus(browser, framework);
		chooseLook("Bootstrap");
		WebElement menu = application();
		assertTrue(menu.findElement(By.cssSelector(".antilia-menu-list")).getDomAttribute("class")
			.contains("nav"));
		WebElement file = item(menu, "File");
		assertTrue(file.getDomAttribute("class").contains("nav-link"));
		file.click();
		WebElement list = submenuOf(file);
		wait.until(d -> list.isDisplayed());
		assertTrue(list.getDomAttribute("class").contains("dropdown-menu"));
		assertTrue(item(menu, "New").getDomAttribute("class").contains("dropdown-item"));
		assertEquals("fixed", list.getCssValue("position"));
		screenshot("bootstrap");
		item(menu, "New").click();
		wait.until(d -> status().startsWith("You chose File › New"));
		assertNoErrors();
	}

	private void openMenus(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "menus");
		wait.until(d -> !app().findElements(By.cssSelector(".menus-demo__application " +
			".antilia-menu-list[role='menubar']")).isEmpty());
	}

	/**
	 * @return the menu of the application
	 */
	private WebElement application()
	{
		return menuIn(".menus-demo__application");
	}

	/**
	 * @return the element of the menu in an element of the demo, of the case shown
	 */
	private WebElement menuIn(String css)
	{
		return app().findElements(By.cssSelector(css + " .antilia-menu")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElseThrow();
	}

	/**
	 * @return the link or the button of the item of a label
	 */
	private static WebElement item(WebElement menu, String label)
	{
		return menu.findElements(By.cssSelector(".antilia-menu-link, .antilia-menu-submenu-link"))
			.stream().filter(link -> link.findElement(By.cssSelector(".antilia-menu-label"))
				.getDomProperty("textContent").trim().equals(label)).findFirst().orElseThrow();
	}

	/**
	 * @return the submenu an item opens
	 */
	private WebElement submenuOf(WebElement item)
	{
		return driver.findElement(By.id(item.getDomAttribute("aria-controls")));
	}

	private WebElement focused()
	{
		return driver.switchTo().activeElement();
	}

	/**
	 * @return what the application says, below its menu
	 */
	private String status()
	{
		return app().findElements(By.cssSelector(".menus-demo__status")).get(0).getText();
	}

	/**
	 * @return where the navigation is
	 */
	private String where()
	{
		return app().findElements(By.cssSelector(".menus-demo__page .menus-demo__where")).get(0)
			.getText();
	}

	/**
	 * Takes a screenshot of the page into the folder of the system property menus.screenshots, if
	 * set.
	 */
	private void screenshot(String name)
	{
		String folder = System.getProperty("menus.screenshots");
		if (folder == null)
		{
			return;
		}
		try
		{
			Path path = Path.of(folder, "menus-" + name + "-" + framework + "-" +
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
