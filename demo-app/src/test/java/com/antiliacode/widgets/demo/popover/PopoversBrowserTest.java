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
package com.antiliacode.widgets.demo.popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Drives {@link PopoversPage} in Chrome and in Firefox, in Wicket, React and Svelte: popovers on
 * each side of the target, from either edge or centered, their arrow pointing at the button; the
 * gallery of designs; the facts rendered into a popover and the project renamed in a form in one;
 * the help on the fields and more on the rows of the table. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}; {@code -Dpopovers.screenshots=<folder>} keeps screenshots of them.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class PopoversBrowserTest extends FrameworkAppsTestCase
{
	/** The placements checked, and the side each opens on with room. */
	private static final Map<String, String> SIDES = Map.of("top-left", "top", "right-bottom",
		"right", "bottom-center", "bottom", "left-top", "left", "bottom-right", "bottom", "top-center",
		"top", "left-center", "left");

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void opensOnEachSideAlongItsButton(SeleniumBrowser browser, String framework)
	{
		openPopovers(browser, framework);
		scrollToCenter(app().findElement(By.cssSelector(".popovers-demo__compass")));

		for (Map.Entry<String, String> each : SIDES.entrySet())
		{
			WebElement button = place(each.getKey());
			button.click();
			WebElement popover = live();
			String side = each.getValue();
			assertEquals(side, popover.getDomAttribute("data-side"), each.getKey());
			assertEquals("true", button.getDomAttribute("aria-expanded"));
			Rectangle at = popover.getRect();
			Rectangle from = button.getRect();
			boolean vertical = side.equals("top") || side.equals("bottom");
			// on its side, the arrow pointing at the middle of the button
			assertTrue(switch (side)
			{
				case "top" -> at.getY() + at.getHeight() <= from.getY();
				case "bottom" -> at.getY() >= from.getY() + from.getHeight();
				case "left" -> at.getX() + at.getWidth() <= from.getX();
				default -> at.getX() >= from.getX() + from.getWidth();
			}, each.getKey() + " on its side");
			assertEquals(0, arrowOffset(popover, button, vertical), 2, each.getKey() + " arrow");
			// along the button from the edge its placement says
			if (each.getKey().equals("top-left"))
			{
				assertEquals(from.getX(), at.getX(), 1, "from the left edge");
			}
			else if (each.getKey().equals("bottom-right"))
			{
				assertEquals(from.getX() + from.getWidth(), at.getX() + at.getWidth(), 1,
					"up to the right edge");
			}
			else if (each.getKey().equals("left-top"))
			{
				assertTrue(at.getY() <= from.getY() + 1, "from the top edge");
			}
			screenshot("placement-" + each.getKey());
			driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
			wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());
			assertEquals(button, driver.switchTo().activeElement(), "the button gets the focus back");
		}

		// in the design and the variant chosen
		Dropdowns.chooseByText(driver, app().findElement(By.cssSelector(
			".popovers-demo__controls select")), "Neon");
		Dropdowns.chooseByText(driver, app().findElements(By.cssSelector(
			".popovers-demo__controls select")).get(1), "Success");
		wait.until(d -> app().findElement(By.cssSelector(".popovers-demo__example .antilia-popover"))
			.getDomAttribute("class").contains("antilia-popover-success"));
		place("auto").click();
		WebElement auto = live();
		assertTrue(auto.getDomAttribute("class").contains("antilia-popover-neon"));
		assertTrue(auto.getDomAttribute("class").contains("antilia-popover-success"));
		screenshot("neon");
		// the button again closes it
		place("auto").click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsEveryDesign(SeleniumBrowser browser, String framework)
	{
		openPopovers(browser, framework);
		selectCase("Nine designs");
		List<WebElement> examples = app().findElements(By.cssSelector(".popovers-demo__example"));
		assertEquals(9, examples.size());
		WebElement gallery = gallery();
		scrollToCenter(gallery);
		screenshot("gallery", gallery);

		WebElement glass = examples.get(3).findElement(By.tagName("button"));
		glass.click();
		WebElement popover = live();
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-glass"));
		assertEquals("Glass", popover.findElement(By.cssSelector(".antilia-popover-title")).getText());
		// where the gallery draws it, below the button with room
		assertEquals("bottom", popover.getDomAttribute("data-side"));
		assertEquals("bottom", examples.get(3).findElement(By.cssSelector(".antilia-popover"))
			.getDomAttribute("data-side"));
		glass.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());

		// the gallery and what it opens where the drop-down says
		Dropdowns.chooseByText(driver, gallery().findElement(By.tagName("select")), "Right top");
		wait.until(d -> app().findElements(By.cssSelector(".popovers-demo__example .antilia-popover"))
			.stream().allMatch(each -> "right".equals(each.getDomAttribute("data-side")) &&
				"start".equals(each.getDomAttribute("data-align"))));
		WebElement card = app().findElements(By.cssSelector(".popovers-demo__example")).get(0)
			.findElement(By.tagName("button"));
		card.click();
		popover = live();
		assertEquals("right", popover.getDomAttribute("data-side"));
		assertEquals("start", popover.getDomAttribute("data-align"));
		screenshot("gallery-right", gallery());
		card.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());

		// in the colors of the theme: the dark one
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === 'Dark'); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select")));
		// the page rendered again in it
		wait.until(d -> !((String)((JavascriptExecutor)d).executeScript("return getComputedStyle(" +
			"arguments[0]).backgroundColor;", app().findElement(By.cssSelector(
				".popovers-demo__example .antilia-popover")))).equals("rgb(255, 255, 255)"));
		selectCase("Nine designs");
		screenshot("gallery-dark", gallery());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void drawsThePopoversInTheBootstrapLook(SeleniumBrowser browser, String framework)
	{
		openPopovers(browser, framework);
		chooseLook("Bootstrap");
		wait.until(d -> app().findElements(By.cssSelector(".popovers-demo__place")).size() == 13);
		scrollToCenter(app().findElement(By.cssSelector(".popovers-demo__compass")));

		WebElement button = place("bottom-center");
		button.click();
		WebElement popover = live();
		// the look of its trigger, carried into the body: the header and the body Bootstrap's
		assertTrue(popover.getDomAttribute("class").contains("antilia-look-bootstrap"));
		assertEquals("bootstrap", popover.getDomAttribute("data-antilia-look"));
		WebElement header = popover.findElement(By.cssSelector(".antilia-popover-header"));
		assertTrue(header.getDomAttribute("class").contains("popover-header"));
		assertTrue(popover.findElement(By.cssSelector(".antilia-popover-close"))
			.getDomAttribute("class").contains("btn-close"));
		// the card in the shape of Bootstrap's popover: its room and the line under its header
		assertEquals("16px", ((JavascriptExecutor)driver).executeScript("return getComputedStyle(" +
			"arguments[0]).paddingLeft;", popover.findElement(By.cssSelector(
				".antilia-popover-body"))));
		assertEquals("1px", ((JavascriptExecutor)driver).executeScript("return getComputedStyle(" +
			"arguments[0]).borderBottomWidth;", header));
		assertEquals(0, arrowOffset(popover, button, true), 2, "arrow");
		screenshot("bootstrap");
		button.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());

		// the gallery as they open
		selectCase("Nine designs");
		WebElement gallery = gallery();
		scrollToCenter(gallery);
		WebElement preview = gallery.findElement(By.cssSelector(".popovers-demo__example " +
			".antilia-popover"));
		assertTrue(preview.getDomAttribute("class").contains("antilia-look-bootstrap"));
		screenshot("bootstrap-gallery", gallery);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void showsContentAndAFormInAPopover(SeleniumBrowser browser, String framework)
	{
		openPopovers(browser, framework);
		selectCase("Components in a popover");
		WebElement status = app().findElement(By.cssSelector(".popovers-demo__rich > button"));
		scrollToCenter(status);
		status.click();
		WebElement popover = live();
		assertEquals("How it is now", popover.findElement(By.cssSelector(".antilia-popover-title"))
			.getText());
		WebElement facts = popover.findElement(By.cssSelector(".popovers-demo__facts"));
		assertTrue(facts.isDisplayed());
		assertEquals("Time", facts.findElement(By.tagName("dt")).getText());
		assertTrue(facts.getText().contains("wicket".equals(framework) ? "Java" : "Language"),
			facts.getText());
		assertEquals("bottom", popover.getDomAttribute("data-side"));
		screenshot("status");
		status.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());
		// back in the page, hidden
		if (!"react".equals(framework))
		{
			assertTrue(!app().findElement(By.cssSelector(".popovers-demo__facts")).isDisplayed());
		}

		WebElement rename = app().findElement(By.cssSelector(".popovers-demo__project button"));
		rename.click();
		popover = live();
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-aurora"));
		WebElement field = popover.findElement(By.cssSelector(".popovers-demo__rename input"));
		wait.until(d -> field.equals(d.switchTo().activeElement()));
		assertEquals("Apollo", field.getDomProperty("value"));
		field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
		field.sendKeys(Keys.ENTER);
		wait.until(d -> live().findElement(By.cssSelector(".popovers-demo__error")).isDisplayed());
		assertEquals("Give the project a name.", live().findElement(By.cssSelector(
			".popovers-demo__error")).getText());
		screenshot("rename-error");

		WebElement again = live().findElement(By.cssSelector(".popovers-demo__rename input"));
		again.sendKeys("Artemis");
		live().findElement(By.cssSelector("button[type='submit']")).click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());
		wait.until(d -> app().findElement(By.cssSelector(".popovers-demo__project strong"))
			.getText().equals("Artemis"));
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void helpsOnFieldsAndTellsMoreOfRows(SeleniumBrowser browser, String framework)
	{
		openPopovers(browser, framework);
		selectCase("Help on the fields of a form");
		WebElement password = app().findElements(By.cssSelector(".popovers-demo__label button"))
			.get(1);
		assertEquals("Help on Password", password.getDomAttribute("aria-label"));
		scrollToCenter(password);
		password.click();
		WebElement popover = live();
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-inverse"));
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-default"));
		assertEquals("right", popover.getDomAttribute("data-side"));
		assertTrue(popover.getText().contains("At least 12 characters."));
		assertEquals(0, arrowOffset(popover, password, false), 2);
		screenshot("help");
		String light = background(popover);
		password.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());

		// in the colors of the theme the page is in: the dark one
		((JavascriptExecutor)driver).executeScript("var s = arguments[0]; s.selectedIndex = " +
			"Array.from(s.options).findIndex(o => o.text === 'Dark'); s.dispatchEvent(" +
			"new Event('change', {bubbles: true}));",
			driver.findElement(By.cssSelector(".topbar__setting select")));
		// the page in it first
		wait.until(d -> !((String)((JavascriptExecutor)d).executeScript("return getComputedStyle(" +
			"arguments[0]).backgroundColor;", app().findElement(By.cssSelector(
				".popovers-demo__example .antilia-popover")))).equals("rgb(255, 255, 255)"));
		selectCase("Help on the fields of a form");
		WebElement help = app().findElements(By.cssSelector(".popovers-demo__label button")).get(1);
		scrollToCenter(help);
		help.click();
		String dark = background(live());
		assertTrue(!dark.equals(light), "the help in the dark theme: " + dark + ", " + light);
		screenshot("help-dark");

		help.click();
		wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).isEmpty());
		selectCase("More on the rows of a table");
		WebElement japan = app().findElements(By.cssSelector(".popovers-demo__action button")).get(2);
		scrollToCenter(japan);
		japan.click();
		popover = wait.until(d -> {
			WebElement shown = live();
			return shown.getText().contains("Japan") ? shown : null;
		});
		assertEquals("left", popover.getDomAttribute("data-side"));
		assertTrue(popover.getDomAttribute("class").contains("antilia-popover-callout"));
		assertTrue(popover.getText().contains("JP / JPN"), popover.getText());
		screenshot("country");
		assertNoErrors();
	}

	/**
	 * @return how far the middle of the arrow of a popover is from the middle of its button, along
	 *         the side of the popover
	 */
	private double arrowOffset(WebElement popover, WebElement button, boolean vertical)
	{
		return ((Number)((JavascriptExecutor)driver).executeScript("var a = arguments[0]" +
			".querySelector('.antilia-popover-arrow').getBoundingClientRect(), b = arguments[1]" +
			".getBoundingClientRect(); return arguments[2] ? (a.left + a.width / 2) - " +
			"(b.left + b.width / 2) : (a.top + a.height / 2) - (b.top + b.height / 2);",
			popover, button, vertical)).doubleValue();
	}

	/**
	 * @return the color of the background of a popover
	 */
	private String background(WebElement popover)
	{
		return (String)((JavascriptExecutor)driver).executeScript(
			"return getComputedStyle(arguments[0]).backgroundColor;", popover);
	}

	private void scrollToCenter(WebElement element)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
	}

	/**
	 * @return the popover open, out of the page in its body, once it shows
	 */
	private WebElement live()
	{
		return wait.until(d -> d.findElements(By.cssSelector("body > .antilia-popover")).stream()
			.filter(WebElement::isDisplayed).findFirst().orElse(null));
	}

	/**
	 * @return the case of the gallery of designs
	 */
	private WebElement gallery()
	{
		return app().findElement(By.cssSelector(".popovers-demo__gallery"))
			.findElement(By.xpath("ancestor::section[1]"));
	}

	private WebElement place(String placement)
	{
		return app().findElement(By.cssSelector(".popovers-demo__place--" + placement));
	}

	private void openPopovers(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "popovers");
		wait.until(d -> app().findElements(By.cssSelector(".popovers-demo__place")).size() == 13);
	}

	private void screenshot(String name)
	{
		screenshot(name, null);
	}

	/**
	 * Takes a screenshot of an element, or of the window when null, into the folder of
	 * {@code popovers.screenshots}, if any.
	 */
	private void screenshot(String name, WebElement element)
	{
		String folder = System.getProperty("popovers.screenshots");
		if (folder == null)
		{
			return;
		}
		// once the popover faded in
		wait.until(d -> (Boolean)((JavascriptExecutor)d).executeScript("return document" +
			".getAnimations().every(a => a.playState !== 'running');"));
		try
		{
			Path path = Path.of(folder, "popovers-" + name + "-" + framework + "-" +
				browser.name().toLowerCase(Locale.ROOT) + ".png");
			Files.createDirectories(path.getParent());
			Files.write(path, (element != null ? element : (TakesScreenshot)driver)
				.getScreenshotAs(OutputType.BYTES));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
