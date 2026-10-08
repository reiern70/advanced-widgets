/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.advanced.widgets.demo.JettyTestCase;
import com.advanced.widgets.demo.SeleniumBrowser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Drives {@link DynamicDataTablePage} in Chrome and in Firefox. Runs with {@code -Pselenium}, see
 * {@link SeleniumBrowser}.
 */
@EnabledIfSystemProperty(named = "wicket.selenium", matches = "true")
class DynamicDataTableWebSocketTest extends JettyTestCase
{
	private static final Pattern ITEM_COUNT = Pattern.compile("of (\\d+)");

	private WebDriver driver;

	private WebDriverWait wait;

	@AfterEach
	void stopBrowser()
	{
		if (driver != null)
		{
			driver.quit();
		}
	}

	@ParameterizedTest
	@EnumSource(SeleniumBrowser.class)
	void pushingTheRowsRepaintsThemInReverseOrderViaWebSockets(SeleniumBrowser browser)
	{
		driver = browser.newDriver(1280, 1024);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.ignoring(StaleElementReferenceException.class);
		driver.get("http://localhost:" + localPort +
			"/wicket/bookmarkable/" + DynamicDataTablePage.class.getName());
		wait.until(d -> !bodyRows().isEmpty());
		wait.until(d -> (Boolean)((JavascriptExecutor)d).executeScript(
			"return !!(Wicket.WebSocket.INSTANCE && Wicket.WebSocket.INSTANCE.ws && " +
				"Wicket.WebSocket.INSTANCE.ws.readyState === 1);"));
		int itemCount = itemCount();
		List<String> ids = ids();

		driver.findElement(By.xpath("//button[normalize-space()=" +
			"'Push the rows of this page in reverse order via web sockets']")).click();

		List<String> reversed = new ArrayList<>(ids);
		Collections.reverse(reversed);
		wait.until(d -> ids().equals(reversed));
		assertEquals(itemCount, itemCount(), "the rows were repainted, not loaded again");
		String feedback = driver.findElement(By.className("feedbackPanel")).getText();
		assertTrue(feedback.contains("Pushed the rows of this page in reverse order"), feedback);
	}

	/**
	 * @return the ids of the users the rows show, in their order
	 */
	private List<String> ids()
	{
		return bodyRows().stream()
			.map(row -> row.findElements(By.tagName("td")).get(3).getText())
			.toList();
	}

	private List<WebElement> bodyRows()
	{
		return driver.findElements(
			By.cssSelector("table.dynamic-dataview .dynamic-data-table-rows > tbody > tr"));
	}

	private int itemCount()
	{
		String label = driver.findElement(By.cssSelector("table.dynamic-dataview .navigatorLabel"))
			.getText();
		Matcher matcher = ITEM_COUNT.matcher(label);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
	}
}
