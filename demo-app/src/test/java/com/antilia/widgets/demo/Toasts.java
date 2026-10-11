/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * Reads the notifications of a demo page in the browser, those its base page shows its feedback
 * messages with.
 */
public final class Toasts
{
	/** The notifications shown in the stack at the top right, the newest first. */
	private static final String SHOWN = ".antilia-notifications-stack-top-right > " +
		".antilia-notification:not(.antilia-notification-leaving)";

	private Toasts()
	{
	}

	/**
	 * @param context
	 *            the driver, or an element holding the notifications
	 * @return the text of the newest notification at the top right, read while it may still be
	 *         fading in; empty when there is none
	 */
	public static String latest(SearchContext context)
	{
		return context.findElements(By.cssSelector(SHOWN)).stream().findFirst()
			.map(notification -> notification.findElement(
				By.cssSelector(".antilia-notification-body")).getDomProperty("textContent"))
			.orElse("");
	}

	/**
	 * @param context
	 *            the driver, or an element holding the notifications
	 * @return the newest notification at the top right, {@code null} when there is none
	 */
	public static WebElement newest(SearchContext context)
	{
		return context.findElements(By.cssSelector(SHOWN)).stream().findFirst().orElse(null);
	}
}
