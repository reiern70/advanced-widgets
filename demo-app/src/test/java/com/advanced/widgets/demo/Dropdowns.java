/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import org.openqa.selenium.By;
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
		WebElement arrow = wrapper.findElement(By.cssSelector(".wicket-dropdown-arrow"));
		new Actions(driver).moveToElement(arrow).click().perform();
		WebElement option = wrapper.findElements(By.cssSelector(".wicket-dropdown-option"))
			.get(index);
		new Actions(driver).moveToElement(option).click().perform();
	}
}
