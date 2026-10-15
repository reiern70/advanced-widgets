/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.contacts.ContactsPage;
import com.antilia.widgets.demo.tree.TreePage;

/**
 * The home page has a dialog about each demo, with the explanation of the demo page, which the
 * buttons of the index open in the browser.
 */
class DemoDetailsTest
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
	void theHomePageHasADialogAboutEachDemo()
	{
		tester.startPage(HomePage.class);

		ListView<?> details = (ListView<?>) tester.getComponentFromLastRenderedPage("demoDetails");
		assertEquals(Demos.all().size(), details.size());
		tester.assertLabel("demoDetails:0:details:overlay:title", "Address Book");
		tester.assertBookmarkablePageLink("demoDetails:0:details:overlay:footer:open",
			ContactsPage.class, new PageParameters());
		// the explanation from the bundle of the demo page, its markup as is
		tester.assertContains("A realistic page made of the widgets of the other demos");
		tester.assertContains("<li>A SplitPanel holds the filters on the left");
		assertTrue(tester.getLastRenderedPage().isPageStateless());
		assertTrue(tester.getSession().isTemporary());
	}

	@Test
	void theIndexOpensIt()
	{
		tester.startPage(HomePage.class);

		String opener = "data-browser-modal=\"" +
			DemoDetails.markupIdOf(Demos.of(TreePage.class).orElseThrow()) + "\"";
		assertEquals(1, tester.getLastResponseAsString().split(opener).length - 1);
		tester.assertContains("id=\"demo-details-tree\"");
	}

	@Test
	void isInSpanishWhenSpanishIsChosen()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(HomePage.class);

		tester.assertLabel("demoDetails:0:details:overlay:title", "Agenda de contactos");
		tester.assertContains("Una página realista hecha con los componentes de las otras demos");
		tester.assertContains("Abrir la demo");
	}

	@Test
	void aDemoPageOpensItsExplanationInADialog()
	{
		tester.startPage(TreePage.class);

		tester.assertComponent("explanation", BrowserModal.class);
		String id = tester.getComponentFromLastRenderedPage("explanation").getMarkupId();
		tester.assertContains("data-browser-modal=\"" + id + "\"");
		tester.assertContains("About this demo");
		tester.assertContains("This is a demo of the two trees of ui-components");
	}
}
