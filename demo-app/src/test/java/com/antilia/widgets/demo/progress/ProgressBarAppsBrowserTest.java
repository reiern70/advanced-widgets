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
package com.antilia.widgets.demo.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.antilia.widgets.demo.SeleniumBrowser;
import com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link ProgressBarPage} in their tabs: the first bar advanced
 * by a timer of the app, paused and reset, a finished one and an indeterminate one. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ProgressBarAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.antilia.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
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
			By.cssSelector(".antilia-progress-bar-label")).getText());

		app().findElement(By.linkText("Reset")).click();
		wait.until(d -> "0".equals(value(0)));
		app().findElement(By.linkText("Start"));
		assertNoErrors();
	}

	private List<WebElement> bars()
	{
		return app().findElements(By.cssSelector(".antilia-progress-bar"));
	}

	private String value(int bar)
	{
		return bars().get(bar).findElement(By.tagName("progress")).getDomAttribute("value");
	}
}
