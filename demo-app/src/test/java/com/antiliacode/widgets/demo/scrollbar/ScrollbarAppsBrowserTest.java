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
package com.antiliacode.widgets.demo.scrollbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.antiliacode.widgets.demo.SeleniumBrowser;
import com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link ScrollbarPage} in their tabs: the boxes with the slim
 * bars of the Wicket ones; and in those and the Wicket tab the growing one's bar following its
 * content, the box scrolled by its buttons, and the strips of the bars hiding the content below
 * them. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ScrollbarAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void drawsTheBarsOfEveryBoxButTheNativeOne(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "scrollbar");

		List<WebElement> boxes = app().findElements(By.cssSelector(".scrollbar-demo__box"));
		assertEquals(5, boxes.size());
		List<String> wicket = driver.findElements(By.cssSelector(
			"#framework-panel-wicket .scrollbar-demo__box")).stream().map(this::axes).toList();
		assertEquals(List.of("", "xy", "x", "xy", "xy"), wicket, "the tracks Wicket draws");
		assertEquals(wicket, boxes.stream().map(this::axes).toList());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void thePlusAddsLinesTheBarFollowsAndScrollsToTheFirstAdded(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "scrollbar");
		wait.until(d -> thumb().getSize().getHeight() > 0);
		int before = thumb().getSize().getHeight();

		WebElement add = app().findElement(By.cssSelector(".scrollbar-demo__add"));
		assertEquals("+", add.getText());
		assertEquals("Add 20 lines", add.getDomAttribute("title"));
		add.click();

		wait.until(d -> growing().findElements(By.tagName("p")).size() == 40);
		wait.until(d -> thumb().getSize().getHeight() < before);
		assertTrue(growing().getText().contains("Line 40"));
		wait.until(d -> Math.abs(offsetOfLine(21)) <= 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theButtonsScrollToTheBottomTheTopAPositionAndALine(SeleniumBrowser browser,
		String framework)
	{
		open(browser, framework, "scrollbar");

		button("Bottom").click();
		wait.until(d -> scrollTop() > 0 && scrollTop() == script(
			"return arguments[0].scrollHeight - arguments[0].clientHeight;"));
		button("Top").click();
		wait.until(d -> scrollTop() == 0);
		button("To 100 px").click();
		wait.until(d -> scrollTop() == 100);
		button("To line 10").click();
		wait.until(d -> Math.abs(offsetOfLine(10)) <= 1);
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antiliacode.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void theStripsOfTheBarsHideTheContentBelowThem(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "scrollbar");
		WebElement box = app().findElement(By.cssSelector(".scrollbar-demo__box--both"));
		WebElement wrapper = box.findElement(By.xpath(".."));

		for (String axis : List.of("x", "y"))
		{
			WebElement track = wrapper.findElement(By.cssSelector(
				":scope > .antilia-scrollbar-track--" + axis));
			wait.until(d -> track.isDisplayed());
			assertEquals(box.getCssValue("background-color"), track.getCssValue(
				"background-color"), "the strip of the " + axis + " bar");
			int thickness = "y".equals(axis) ? track.getSize().getWidth()
				: track.getSize().getHeight();
			assertEquals(10, thickness, "the strip of the " + axis + " bar");
		}
		WebElement track = wrapper.findElement(By.cssSelector(":scope > .antilia-scrollbar-track--y"));
		// the strip lies inside the border of the box, along its right edge
		Number gap = (Number)((JavascriptExecutor)driver).executeScript("var box = arguments[0]; " +
			"return box.getBoundingClientRect().left + box.clientLeft + box.clientWidth - " +
			"arguments[1].getBoundingClientRect().right;", box, track);
		assertTrue(Math.abs(gap.doubleValue()) < 1, "the strip ends " + gap + "px off the border");
	}

	private WebElement growing()
	{
		return app().findElement(By.cssSelector(".scrollbar-demo__box--short"));
	}

	private WebElement thumb()
	{
		return growing().findElement(By.xpath("..")).findElement(
			By.cssSelector(".antilia-scrollbar-track--y .antilia-scrollbar-thumb"));
	}

	private WebElement button(String text)
	{
		return app().findElements(By.cssSelector(".scrollbar-demo__to")).stream()
			.filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
	}

	private long scrollTop()
	{
		return script("return Math.round(arguments[0].scrollTop);");
	}

	/**
	 * @return the pixels between the top of a line of the growing box and the top of its inside
	 */
	private long offsetOfLine(int line)
	{
		return script("var box = arguments[0], line = box.querySelector('[data-line=\"" + line +
			"\"]'); return Math.round(line.getBoundingClientRect().top - " +
			"box.getBoundingClientRect().top - box.clientTop);");
	}

	private long script(String script)
	{
		return ((Number)((JavascriptExecutor)driver).executeScript(script, growing())).longValue();
	}

	/**
	 * @return the axes a box has tracks of slim bars along, shown when it scrolls along them: x, y,
	 *         both or none
	 */
	private String axes(WebElement box)
	{
		WebElement wrapper = box.findElement(By.xpath(".."));
		String wrapperClass = wrapper.getDomAttribute("class");
		if (wrapperClass == null || !wrapperClass.contains("antilia-scrollbar"))
		{
			return "";
		}
		return (wrapper.findElements(By.cssSelector(":scope > .antilia-scrollbar-track--x"))
			.isEmpty() ? "" : "x") + (wrapper.findElements(By.cssSelector(
				":scope > .antilia-scrollbar-track--y")).isEmpty() ? "" : "y");
	}
}
