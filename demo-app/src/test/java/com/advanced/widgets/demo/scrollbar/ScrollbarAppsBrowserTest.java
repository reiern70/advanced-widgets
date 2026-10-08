/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.scrollbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.advanced.widgets.demo.SeleniumBrowser;
import com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase;

/**
 * Drives the React and Svelte apps of {@link ScrollbarPage} in their tabs: the boxes with the slim
 * bars of the Wicket ones, the growing one's bar following its content. Runs with
 * {@code -Pselenium}, see {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class ScrollbarAppsBrowserTest extends FrameworkAppsTestCase
{
	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void drawsTheBarsOfEveryBoxButTheNativeOne(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "scrollbar");

		List<WebElement> boxes = app().findElements(By.cssSelector(".scrollbar-demo__box"));
		assertEquals(5, boxes.size());
		List<String> wicket = driver.findElements(By.cssSelector(
			"#framework-panel-wicket .scrollbar-demo__box")).stream().map(this::axes).toList();
		assertEquals(List.of("", "xy", "x", "xy", "xy"), wicket, "the tracks Wicket draws");
		assertEquals(wicket, boxes.stream().map(this::axes).toList());
		assertNoErrors();
	}

	@ParameterizedTest
	@MethodSource("com.advanced.widgets.demo.frameworks.FrameworkAppsTestCase#frameworks")
	void theBarOfTheGrowingBoxFollowsItsLines(SeleniumBrowser browser, String framework)
	{
		open(browser, framework, "scrollbar");
		WebElement thumb = growing().findElement(By.xpath(".."))
			.findElement(By.cssSelector(".wicket-scrollbar-track--y .wicket-scrollbar-thumb"));
		wait.until(d -> thumb.getSize().getHeight() > 0);
		int before = thumb.getSize().getHeight();

		app().findElement(By.linkText("Add 20 lines")).click();

		wait.until(d -> growing().findElements(By.tagName("p")).size() == 40);
		wait.until(d -> thumb.getSize().getHeight() < before);
		assertTrue(growing().getText().endsWith("Line 40"));
	}

	private WebElement growing()
	{
		return app().findElement(By.cssSelector(".scrollbar-demo__box--short"));
	}

	/**
	 * @return the axes a box has tracks of slim bars along, shown when it scrolls along them: x, y,
	 *         both or none
	 */
	private String axes(WebElement box)
	{
		WebElement wrapper = box.findElement(By.xpath(".."));
		String wrapperClass = wrapper.getDomAttribute("class");
		if (wrapperClass == null || !wrapperClass.contains("wicket-scrollbar"))
		{
			return "";
		}
		return (wrapper.findElements(By.cssSelector(":scope > .wicket-scrollbar-track--x"))
			.isEmpty() ? "" : "x") + (wrapper.findElements(By.cssSelector(
				":scope > .wicket-scrollbar-track--y")).isEmpty() ? "" : "y");
	}
}
