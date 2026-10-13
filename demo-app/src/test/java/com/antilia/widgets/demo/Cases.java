/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shows a case of a demo in the browser, as {@link DemoCases} and the pills of the React and
 * Svelte apps draw them: one case at a time, the others hidden.
 */
public final class Cases
{
	private Cases()
	{
	}

	/**
	 * Clicks the pill of a case and waits until its panel shows.
	 *
	 * @param wait
	 *            the wait of the driver
	 * @param panel
	 *            the panel of the framework the demo is in: the Wicket one, React's or Svelte's
	 * @param title
	 *            the title of the case, as its pill has it
	 */
	public static void select(WebDriverWait wait, SearchContext panel, String title)
	{
		WebElement pill = wait.until(d -> panel.findElements(By.cssSelector("[role='tab']"))
			.stream().filter(tab -> title.equals(tab.getText().trim())).findFirst()
			.orElse(null));
		pill.click();
		wait.until(d -> "true".equals(pill.getDomAttribute("aria-selected")));
	}

	/**
	 * Shows a case of the Wicket demo of a page.
	 *
	 * @see #select(WebDriverWait, SearchContext, String)
	 */
	public static void selectInWicket(WebDriverWait wait, SearchContext driver, String title)
	{
		select(wait, driver.findElement(By.id("framework-panel-wicket")), title);
	}
}
