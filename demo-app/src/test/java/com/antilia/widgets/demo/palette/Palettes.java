/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.palette;

import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Finds the parts of a palette in the browser tests and drags its items.
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
