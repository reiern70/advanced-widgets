/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;

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
		tester.assertLabel("components:0:name", "Data Grid");
		tester.assertContains("Ernesto Reinaldo Barreiro");
		tester.assertContains("All rights reserved.");
	}

	@Test
	public void wicketsAjaxRunsOnPlainJavaScriptByDefault()
	{
		tester.startPage(HomePage.class);

		tester.assertContains("res/js/wicket-ajax-ver-");
		String page = tester.getLastResponseAsString();
		assertFalse(page.contains("jquery/jquery-") || page.contains("wicket-ajax-jquery"), page);
		// the setup of the websocket needs no jQuery either
		tester.assertContains("Object\\.assign\\(Wicket\\.WebSocket");
	}

	@Test
	public void wicketsAjaxRunsOnJQueryWhenChosen()
	{
		tester.startPage(HomePage.class);
		DemoSession.get().setAjaxEngine(AjaxEngine.JQUERY);
		tester.startPage(HomePage.class);

		tester.assertContains("jquery/jquery-4");
		tester.assertContains("res/js/wicket-ajax-jquery-ver-");
	}

	@Test
	public void homepageIsInGermanWhenGermanIsChosen()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(HomePage.class);

		tester.assertLabel("components:0:name", "Datenraster");
		tester.assertLabel("demos:0:link:name", "JSON-Datentabelle");
		tester.assertContains("Alle Rechte vorbehalten.");
		tester.assertContains("Komponenten im Katalog");
	}
}
