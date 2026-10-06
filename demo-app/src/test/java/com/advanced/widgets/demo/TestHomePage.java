/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Simple test using the WicketTester
 */
public class TestHomePage
{
	private WicketTester tester;

	@BeforeEach
	public void setUp()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@Test
	public void homepageRendersSuccessfully()
	{
		//start and render the test page
		tester.startPage(HomePage.class);

		//assert rendered page class
		tester.assertRenderedPage(HomePage.class);
	}

	@Test
	public void homepageListsTheComponentCatalog()
	{
		tester.startPage(HomePage.class);

		ListView<?> components = (ListView<?>) tester.getComponentFromLastRenderedPage("components");
		assertEquals(ComponentCatalog.components().size(), components.size());
		tester.assertLabel("components:0:name", ComponentCatalog.components().get(0).name());
		tester.assertContains("Ernesto Reinaldo Barreiro");
		tester.assertContains("All rights reserved.");
	}
}
