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
package com.antilia.widgets.demo.datepicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.antilia.widgets.demo.Dropdowns;
import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.Toasts;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives {@link DatePickerPage} in its Wicket, React and Svelte tabs, in Chrome and in Firefox:
 * the calendar with the mouse and the keyboard, typing in the format of the locale, the locale
 * changed, and text that is no date. Runs with {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DatePickerBrowserTest extends FrameworkAppsTestCase
{
	private static final String START = "Born May 17, 1990; contract from nothing; appointment ";

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void startsWithTheValuesOfThePageInItsLocale(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		assertEquals("05/17/1990", field("born").getDomProperty("value"));
		assertEquals("mm/dd/yyyy", field("start").getDomAttribute("placeholder"));
		assertEquals("Born", field("born").getDomAttribute("aria-label"));
		assertFalse(field("disabled").isEnabled());
		assertTrue(choices().startsWith(START), choices());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void choosesADayFromTheCalendar(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		toggle("born").click();
		assertEquals("May 1990", popup("born").findElement(
			By.cssSelector(".antilia-datepicker-title")).getText());
		popup("born").findElement(By.cssSelector("button[data-value='1990-05-20']")).click();

		wait.until(d -> choices().startsWith("Born May 20, 1990;"));
		assertEquals("05/20/1990", field("born").getDomProperty("value"));
		assertEquals("1990-05-20", input("born").getDomProperty("value"));
		assertFalse(popup("born").isDisplayed());
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void choosesWithTheKeyboard(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		field("start").sendKeys(Keys.chord(Keys.ALT, Keys.ARROW_DOWN));
		assertTrue(popup("start").isDisplayed());
		driver.switchTo().activeElement().sendKeys(Keys.ARROW_RIGHT);
		driver.switchTo().activeElement().sendKeys(Keys.ENTER);

		String tomorrow = LocalDate.now().plusDays(1).toString();
		wait.until(d -> tomorrow.equals(input("start").getDomProperty("value")));
		wait.until(d -> !choices().contains("contract from nothing"));
		assertFalse(popup("start").isDisplayed());
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void typesTheWayTheLocaleChosenWrites(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		Dropdowns.choose(driver, app().findElement(By.name("calendarLocale")), 3);
		wait.until(d -> "17.05.1990".equals(field("born").getDomProperty("value")));
		assertEquals("dd.mm.yyyy", field("born").getDomAttribute("placeholder"));

		field("departure").sendKeys("24.12.2026 18:30", Keys.TAB);

		wait.until(d -> "2026-12-24T18:30".equals(input("departure").getDomProperty("value")));
		wait.until(d -> choices().endsWith("departure Dec 24, 2026, 6:30 PM."));
		assertEquals("24.12.2026, 18:30", field("departure").getDomProperty("value"));
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void choosesATime(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		toggle("departure").click();
		popup("departure").findElement(By.cssSelector("button[data-value='" + LocalDate.now() +
			"']")).click();
		// the clock of the United States has 12 hours: the morning is taken
		WebElement period = popup("departure").findElement(
			By.cssSelector(".antilia-datepicker-period"));
		if (period.getText().equals("PM"))
		{
			period.click();
		}
		WebElement hour = popup("departure").findElement(By.cssSelector(".antilia-datepicker-hour"));
		hour.clear();
		hour.sendKeys("11");
		WebElement minute = popup("departure").findElement(
			By.cssSelector(".antilia-datepicker-minute"));
		minute.clear();
		minute.sendKeys("45", Keys.ENTER);

		String expected = LocalDate.now() + "T11:45";
		wait.until(d -> expected.equals(input("departure").getDomProperty("value")));
		wait.until(d -> choices().endsWith(", 11:45 AM."));
		assertFalse(popup("departure").isDisplayed());
	}

	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#wicketAndFrameworks")
	void marksTextThatIsNoDate(SeleniumBrowser browser, String framework)
	{
		openDemo(browser, framework);

		field("born").clear();
		field("born").sendKeys("nonsense", Keys.TAB);

		wait.until(d -> "true".equals(field("born").getDomAttribute("aria-invalid")));
		assertEquals("nonsense", input("born").getDomProperty("value"));
		if ("wicket".equals(framework))
		{
			wait.until(d -> Toasts.latest(d).equals("The value of 'Born' is not a valid date."));
			assertTrue(Toasts.newest(driver).getDomAttribute("class").contains(
				"antilia-notification-error"));
		}
	}

	private void openDemo(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "date-picker");
		wait.until(d -> !app().findElements(By.cssSelector(".antilia-datepicker-text")).isEmpty());
	}

	private String choices()
	{
		return app().findElement(By.cssSelector(".datepicker-demo__choices")).getText()
			.replaceAll("[\\u00a0\\u202f]", " ");
	}

	private WebElement input(String name)
	{
		return app().findElement(By.name(name));
	}

	private WebElement wrapper(String name)
	{
		return input(name).findElement(By.xpath(".."));
	}

	private WebElement field(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".antilia-datepicker-text"));
	}

	private WebElement toggle(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".antilia-datepicker-toggle"));
	}

	private WebElement popup(String name)
	{
		return wrapper(name).findElement(By.cssSelector(".antilia-datepicker-popup"));
	}
}
