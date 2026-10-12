/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.datatable.DynamicDataTablePage;

/**
 * The carousel shows a slide per demo, which links to it, and starts its script.
 */
class DemoCarouselTest
{
	private WicketTester tester;

	@BeforeEach
	void setUp()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void tearDown()
	{
		tester.destroy();
	}

	@Test
	void showsASlidePerDemoLinkingToIt()
	{
		tester.startPage(CarouselTestPage.class);

		assertEquals(Demos.all().size(),
			((ListView<?>) tester.getComponentFromLastRenderedPage("carousel:slides")).size());
		tester.assertLabel("carousel:slides:1:name", "JSON Grid");
		tester.assertBookmarkablePageLink("carousel:slides:1:open", DynamicDataTablePage.class,
			new PageParameters());
		tester.assertContains("aria-label=\"2 of " + Demos.all().size() + "\"");
		tester.assertContains("AntiliaDemoCarousel.init\\(");
		tester.assertContains("demo-carousel(-ver-[0-9]+)?\\.js");
	}

	@Test
	void isInSpanishWhenSpanishIsChosen()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(CarouselTestPage.class);

		tester.assertContains("Abrir la demo");
		tester.assertContains("data-label-play=\"Pasar las demos\"");
	}
}
