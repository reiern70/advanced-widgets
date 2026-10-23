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
package com.antiliacode.widgets.demo;

import java.util.List;
import java.util.function.Predicate;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Chooses in the drop-downs of ui-components the way a user does: a click on the button's arrow
 * opens the list, a click on an option chooses it. The native select stays hidden.
 */
public final class Dropdowns
{
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
		WebElement wrapper = select.findElement(By.xpath(".."));
		// the arrow: the middle of the button may show the x of a chosen option's chip
		WebElement arrow = wrapper.findElement(By.cssSelector(".antilia-dropdown-arrow"));
		scrollTo(driver, arrow);
		new Actions(driver).moveToElement(arrow).click().perform();
		WebElement option = wrapper.findElements(By.cssSelector(".antilia-dropdown-option"))
			.get(index);
		// in sight, in the list and in the page: geckodriver does not scroll to it
		scrollTo(driver, option);
		new Actions(driver).moveToElement(option).click().perform();
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
