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
package com.antiliacode.widgets.demo;

import java.util.List;
import java.util.function.Predicate;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Chooses in the drop-downs of ui-components the way a user does: a click on the button's arrow
 * opens the list, a click on an option chooses it. The native select stays hidden.
 */
public final class Dropdowns
{
	/**
	 * The points of the open list, its corners and its middle, at which the browser finds
	 * something else than the list: a box around the drop-down cutting it, something drawn over
	 * it, or the edge of the window. None when the list is drawn whole, on top of the page.
	 */
	private static final String COVERED = """
		var menu = arguments[0].parentElement.querySelector('.antilia-dropdown-menu');
		var box = menu.getBoundingClientRect();
		var inset = 3;
		var points = [[box.left + inset, box.top + inset], [box.right - inset, box.top + inset],
			[box.left + inset, box.bottom - inset], [box.right - inset, box.bottom - inset],
			[(box.left + box.right) / 2, (box.top + box.bottom) / 2]];
		var covered = [];
		points.forEach(function (point) {
			var found = document.elementFromPoint(point[0], point[1]);
			if (!found || !menu.contains(found)) {
				covered.push(Math.round(point[0]) + ',' + Math.round(point[1]) + ': ' +
					(found ? found.tagName.toLowerCase() + '.' + found.className : 'nothing'));
			}
		});
		return covered;
		""";

	private Dropdowns()
	{
	}

	/**
	 * Chooses an option of the drop-down drawn over a select.
	 *
	 * @param driver
	 *            the driver
	 * @param select
	 *            the hidden select
	 * @param index
	 *            the index of the option
	 */
	public static void choose(WebDriver driver, WebElement select, int index)
	{
		open(driver, select);
		chooseInOpenList(driver, select, index);
	}

	/**
	 * Opens the list of the drop-down drawn over a select, by a click on the button's arrow.
	 *
	 * @param driver
	 *            the driver
	 * @param select
	 *            the hidden select
	 */
	public static void open(WebDriver driver, WebElement select)
	{
		// the arrow: the middle of the button may show the x of a chosen option's chip
		WebElement arrow = wrapper(select).findElement(By.cssSelector(".antilia-dropdown-arrow"));
		scrollTo(driver, arrow);
		new Actions(driver).moveToElement(arrow).click().perform();
	}

	/**
	 * Chooses an option in the list of a drop-down already open, by a click on it.
	 *
	 * @param driver
	 *            the driver
	 * @param select
	 *            the hidden select
	 * @param index
	 *            the index of the option
	 */
	public static void chooseInOpenList(WebDriver driver, WebElement select, int index)
	{
		WebElement option = options(select).get(index);
		// in sight, in the list and in the page: geckodriver does not scroll to it
		scrollTo(driver, option);
		new Actions(driver).moveToElement(option).click().perform();
	}

	/**
	 * @param select
	 *            the hidden select
	 * @return the options of the list of the drop-down drawn over it, in the order of the select
	 */
	public static List<WebElement> options(WebElement select)
	{
		return wrapper(select).findElements(By.cssSelector(".antilia-dropdown-option"));
	}

	/**
	 * @param select
	 *            the hidden select
	 * @return where the open list of the drop-down drawn over it is in the window
	 */
	public static Rectangle list(WebElement select)
	{
		return wrapper(select).findElement(By.cssSelector(".antilia-dropdown-menu")).getRect();
	}

	/**
	 * Tells where the open list of a drop-down is not drawn on top of the page: cut by a box
	 * around the drop-down, under something else, or out of the window.
	 *
	 * @param driver
	 *            the driver
	 * @param select
	 *            the hidden select
	 * @return the points of the list where the browser finds something else, with what it finds
	 *         there; empty when the list is drawn whole, on top
	 */
	@SuppressWarnings("unchecked")
	public static List<String> covered(WebDriver driver, WebElement select)
	{
		return (List<String>)((JavascriptExecutor)driver).executeScript(COVERED, select);
	}

	private static WebElement wrapper(WebElement select)
	{
		return select.findElement(By.xpath(".."));
	}

	/**
	 * Chooses the option of a text in the drop-down drawn over a select.
	 *
	 * @param text
	 *            the text of the option
	 * @see #choose(WebDriver, WebElement, int)
	 */
	public static void chooseByText(WebDriver driver, WebElement select, String text)
	{
		choose(driver, select, indexOf(select, option -> text.equals(
			option.getDomProperty("textContent").trim())));
	}

	/**
	 * Chooses the option of a value in the drop-down drawn over a select.
	 *
	 * @param value
	 *            the value of the option
	 * @see #choose(WebDriver, WebElement, int)
	 */
	public static void chooseByValue(WebDriver driver, WebElement select, String value)
	{
		choose(driver, select, indexOf(select, option -> value.equals(
			option.getDomProperty("value"))));
	}

	private static void scrollTo(WebDriver driver, WebElement element)
	{
		((JavascriptExecutor)driver).executeScript(
			"arguments[0].scrollIntoView({block: 'nearest', inline: 'nearest'});", element);
	}

	/**
	 * @return the index of the first option of a select that matches
	 */
	private static int indexOf(WebElement select, Predicate<WebElement> matches)
	{
		List<WebElement> options = select.findElements(By.tagName("option"));
		for (int i = 0; i < options.size(); i++)
		{
			if (matches.test(options.get(i)))
			{
				return i;
			}
		}
		throw new IllegalArgumentException("no such option");
	}
}
