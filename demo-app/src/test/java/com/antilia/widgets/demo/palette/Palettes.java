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
package com.antilia.widgets.demo.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Finds the parts of a palette in the browser tests, drags its items and checks its filter.
 */
final class Palettes
{
	private Palettes()
	{
	}

	static WebElement list(WebElement palette, String side)
	{
		return palette.findElement(By.cssSelector(".antilia-palette-" + side + " .antilia-palette-list"));
	}

	/**
	 * @return the texts of the items of a side, one per line, without their numbers
	 */
	static String texts(WebElement palette, String side)
	{
		return list(palette, side).findElements(By.cssSelector(".antilia-palette-text")).stream()
			.map(WebElement::getText)
			.collect(Collectors.joining("\n"));
	}

	/**
	 * @return the numbers of the items of a side, one per line
	 */
	static String numbers(WebElement palette, String side)
	{
		return list(palette, side).findElements(By.cssSelector(".antilia-palette-number"))
			.stream()
			.map(WebElement::getText)
			.collect(Collectors.joining("\n"));
	}

	/**
	 * @return the titles of both sides, one per line
	 */
	static String titles(WebElement palette)
	{
		return palette.findElements(By.cssSelector(".antilia-palette-title")).stream()
			.map(WebElement::getText)
			.collect(Collectors.joining("\n"));
	}

	static String available(WebElement palette)
	{
		return texts(palette, "available");
	}

	static String selected(WebElement palette)
	{
		return texts(palette, "selected");
	}

	static WebElement item(WebElement palette, String text)
	{
		return palette.findElements(By.cssSelector(".antilia-palette-item")).stream()
			.filter(item -> item.findElement(By.cssSelector(".antilia-palette-text")).getText()
				.equals(text))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no item " + text));
	}

	/**
	 * Types into the filter of a palette and checks the available ones it lets through, and that
	 * each has the part matched underlined, the text in any case.
	 *
	 * @param included
	 *            a label among those let through
	 * @param excluded
	 *            a label not among them, or {@code null}
	 * @param matches
	 *            whether a label is one the filter lets through
	 */
	static void assertMatches(WebDriverWait wait, WebElement palette, String text, String included,
		String excluded, Predicate<String> matches)
	{
		palette.findElement(By.cssSelector(".antilia-palette-filter")).sendKeys(text);
		wait.until(d -> List.of(available(palette).split("\n")).contains(included));
		List<String> labels = List.of(available(palette).split("\n"));
		labels.forEach(label -> assertTrue(matches.test(label), label + " matches " + text));
		if (excluded != null)
		{
			assertFalse(labels.contains(excluded), excluded + " does not match " + text);
		}
		for (WebElement item : list(palette, "available").findElements(
			By.cssSelector(".antilia-palette-item")))
		{
			List<WebElement> parts = item.findElements(By.cssSelector(".antilia-palette-match"));
			assertEquals(1, parts.size(), "one part underlined");
			assertTrue(parts.get(0).getText().equalsIgnoreCase(text), parts.get(0).getText());
		}
	}

	/**
	 * Types "s ge" into the filter of a palette of the countries matching the starts of words,
	 * which lets South Georgia through with "S" and "Ge" underlined, and South Africa not.
	 */
	static void assertWordsMatch(WebDriverWait wait, WebElement palette)
	{
		palette.findElement(By.cssSelector(".antilia-palette-filter")).sendKeys("s ge");
		wait.until(d -> available(palette).startsWith("South Georgia"));
		assertFalse(available(palette).contains("South Africa"));
		WebElement item = list(palette, "available").findElement(
			By.cssSelector(".antilia-palette-item"));
		assertEquals(List.of("S", "Ge"), item.findElements(By.cssSelector(".antilia-palette-match"))
			.stream().map(WebElement::getText).toList());
	}

	static WebElement button(WebElement palette, String name)
	{
		return palette.findElement(By.cssSelector(".antilia-palette-" + name));
	}

	static void doubleClick(WebDriver driver, WebElement item)
	{
		new Actions(driver).doubleClick(item).perform();
	}

	/**
	 * Drags an item over another one, with the pointer.
	 *
	 * @param fraction
	 *            where to drop it, from the top of the other one (0) to its bottom (1)
	 */
	static void drag(WebDriver driver, WebElement item, WebElement target, double fraction)
	{
		// Firefox does not scroll to the pointer, so the palette has to be in view
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", item);
		int height = target.getSize().getHeight();
		int offset = (int)Math.round(height * fraction) - height / 2;
		new Actions(driver)
			.clickAndHold(item)
			.moveByOffset(0, 8)
			.moveToElement(target, 0, offset)
			.moveToElement(target, 1, offset)
			.release()
			.perform();
	}
}
