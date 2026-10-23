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
