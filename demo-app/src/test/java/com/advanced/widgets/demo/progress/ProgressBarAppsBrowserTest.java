/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link ProgressBarPage} in their tabs: the first bar advanced
 * by a timer of the app, paused and reset, a finished one and an indeterminate one. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ProgressBarAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void advancesPausesAndResetsTheFirstBar(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "progress-bar");
		List<WebElement> bars = bars();
		assertEquals(3, bars.size());
		assertEquals("0", value(0));
		assertEquals("100", value(1));
		assertNull(value(2), "indeterminate");

		app().findElement(By.linkText("Start")).click();
		wait.until(d -> Integer.parseInt(value(0)) >= 9);
		app().findElement(By.linkText("Pause")).click();
		String paused = value(0);
		assertEquals(paused + "%", bars().get(0).findElement(
			By.cssSelector(".wicket-progress-bar-label")).getText());

		app().findElement(By.linkText("Reset")).click();
		wait.until(d -> "0".equals(value(0)));
		app().findElement(By.linkText("Start"));
		assertNoErrors();
	}

	private List<WebElement> bars()
	{
		return app().findElements(By.cssSelector(".wicket-progress-bar"));
	}

	private String value(int bar)
	{
		return bars().get(bar).findElement(By.tagName("progress")).getDomAttribute("value");
	}
}
