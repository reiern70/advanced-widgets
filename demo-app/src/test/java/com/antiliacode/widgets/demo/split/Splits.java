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
package com.antiliacode.widgets.demo.split;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * What the browser tests of the split panels read and do in a split panel, the element with the
 * class {@code antilia-split}, the same in Wicket, React and Svelte.
 */
final class Splits
{
	private Splits()
	{
	}

	/**
	 * @return the divider of the split panel, not that of one in its panes
	 */
	static WebElement divider(WebElement split)
	{
		return split.findElement(By.xpath("./*[contains(@class, 'antilia-split-divider')]"));
	}

	private static WebElement first(WebElement split)
	{
		return split.findElement(By.xpath("./*[contains(@class, 'antilia-split-first')]"));
	}

	private static boolean horizontal(WebElement split)
	{
		return split.getDomAttribute("class").contains("antilia-split-horizontal");
	}

	/**
	 * @return the pixels the panes share along the orientation, without the divider
	 */
	static double space(WebDriver driver, WebElement split)
	{
		boolean horizontal = horizontal(split);
		String side = horizontal ? "width" : "height";
		String[] borders = horizontal ? new String[] { "left", "right" } :
			new String[] { "top", "bottom" };
		return ((Number)((JavascriptExecutor)driver).executeScript(
			"var split = arguments[0], divider = arguments[1], style = getComputedStyle(split);" +
				"return split.getBoundingClientRect()." + side +
				" - parseFloat(style.getPropertyValue('border-" + borders[0] + "-width'))" +
				" - parseFloat(style.getPropertyValue('border-" + borders[1] + "-width'))" +
				" - divider.getBoundingClientRect()." + side + ";",
			split, divider(split))).doubleValue();
	}

	/**
	 * @return the share of the first pane, in percent, as the browser lays it out
	 */
	static double share(WebDriver driver, WebElement split)
	{
		return size(driver, split) / space(driver, split) * 100;
	}

	/**
	 * @return the size of the first pane along the orientation, in pixels
	 */
	static double size(WebDriver driver, WebElement split)
	{
		String side = horizontal(split) ? "width" : "height";
		return ((Number)((JavascriptExecutor)driver).executeScript(
			"return arguments[0].getBoundingClientRect()." + side + ";", first(split)))
			.doubleValue();
	}

	/**
	 * Scrolls the body of the first pane of a split panel of the demo to its end.
	 *
	 * @return how far the title of the pane moved, in pixels: 0 when only the body scrolls
	 */
	static double scrollBody(WebDriver driver, WebElement split)
	{
		return ((Number)((JavascriptExecutor)driver).executeScript(
			"var pane = arguments[0], title = pane.querySelector('.split-demo__title');" +
				"var body = pane.querySelector('.antilia-scrollbar > .split-demo__body');" +
				"var top = title.getBoundingClientRect().top;" +
				"body.scrollTop = body.scrollHeight; pane.scrollTop = pane.scrollHeight;" +
				"return body.scrollTop > 0 ? title.getBoundingClientRect().top - top : -1;",
			first(split))).doubleValue();
	}

	/**
	 * Clicks a collapse button of the split panel's divider.
	 *
	 * @param pane
	 *            {@code first} for the button towards the first pane, {@code second} for the other
	 */
	static void collapse(WebDriver driver, WebElement split, String pane)
	{
		WebElement button = divider(split).findElement(By.xpath(
			"./*[contains(@class, 'antilia-split-collapse-" + pane + "')]"));
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", button);
		new Actions(driver).click(button).perform();
	}

	/**
	 * @return whether the split panel has collapsed the pane, {@code first} or {@code second}
	 */
	static boolean collapsed(WebElement split, String pane)
	{
		return split.getDomAttribute("class").contains("antilia-split-collapsed-" + pane);
	}

	/**
	 * Drags an element by a distance with the mouse.
	 */
	static void drag(WebDriver driver, WebElement element, int dx, int dy)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'center'});", element);
		new Actions(driver).moveToElement(element)
			.clickAndHold()
			.moveByOffset(dx / 2, dy / 2)
			.moveByOffset(dx - dx / 2, dy - dy / 2)
			.release()
			.perform();
	}

	static void doubleClick(WebDriver driver, WebElement element)
	{
		new Actions(driver).doubleClick(element).perform();
	}
}
