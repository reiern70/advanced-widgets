/*
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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.contacts.ContactsPage;
import com.antiliacode.widgets.demo.tree.TreePage;

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
