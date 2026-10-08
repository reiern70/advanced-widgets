/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

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
	public void showsTheBranchOnlyInDemoMode()
	{
		BuildInfo build = BuildInfo.get();
		assertTrue(build.isKnown(), "the build writes build-info.properties");

		tester.startPage(HomePage.class);
		tester.assertInvisible("buildInfo");

		System.setProperty(BuildInfo.DEMO_PROPERTY, "true");
		try
		{
			tester.startPage(HomePage.class);
			tester.assertVisible("buildInfo");
			tester.assertContains("Branch " + Pattern.quote(build.branch() + " · " + build.commit()));
		}
		finally
		{
			System.clearProperty(BuildInfo.DEMO_PROPERTY);
		}
	}

	@Test
	public void homepageIsInSpanishWhenSpanishIsChosen()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(HomePage.class);

		tester.assertLabel("components:0:name", "Cuadrícula de datos");
		tester.assertLabel("demos:0:link:name", "Grid JSON");
		tester.assertContains("Todos los derechos reservados.");
		tester.assertContains("componentes en el catálogo");
		tester.assertContains("Español");
	}

	@Test
	public void homepageIsInGermanWhenGermanIsChosen()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(HomePage.class);

		tester.assertLabel("components:0:name", "Datenraster");
		tester.assertLabel("demos:0:link:name", "JSON-Grid");
		tester.assertContains("Alle Rechte vorbehalten.");
		tester.assertContains("Komponenten im Katalog");
	}
}
