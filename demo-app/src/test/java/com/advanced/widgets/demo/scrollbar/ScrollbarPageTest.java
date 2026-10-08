/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.scrollbar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.advanced.widgets.demo.WicketApplication;

/**
 * Renders {@link ScrollbarPage} and grows its last box via Ajax.
 */
class ScrollbarPageTest
{
	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(ScrollbarPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void everyBoxButTheNativeOneGetsTheScrollBars()
	{
		String page = tester.getLastResponseAsString();
		assertEquals(4, page.split(Pattern.quote("Wicket.Scrollbar.attach("), -1).length - 1,
			page);
		tester.assertContains(Pattern.quote("{\"axis\":\"x\"}"));
	}

	@Test
	void addingLinesRendersTheBoxAndItsScrollBarsAgainScrolledToTheFirstAdded()
	{
		tester.clickLink("themed:addLines");

		tester.assertComponentOnAjaxResponse("themed:growing");
		tester.assertContains("Line 40");
		tester.assertContains(Pattern.quote("data-line=\"40\""));
		tester.assertContains(Pattern.quote("Wicket.Scrollbar.attach("));
		assertScrolls("scrollToElement(\"[data-line=\\\"21\\\"]\", true)");
	}

	@Test
	void theButtonsScrollTheGrowingBox()
	{
		tester.clickLink("themed:toTop");
		assertScrolls("scrollToTop(true)");
		tester.clickLink("themed:toBottom");
		assertScrolls("scrollToBottom(true)");
		tester.clickLink("themed:toPosition");
		assertScrolls("scrollTo({\"top\":100}, true)");
		tester.clickLink("themed:toLine");
		assertScrolls("scrollToElement(\"[data-line=\\\"10\\\"]\", true)");
	}

	private void assertScrolls(String call)
	{
		String id = tester.getComponentFromLastRenderedPage("themed:growing").getMarkupId();
		tester.assertContains(Pattern.quote("Wicket.Scrollbar.of(\"" + id + "\")?." + call + ";"));
	}
}
