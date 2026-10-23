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
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.tree.TreePage;

/**
 * What runs where is in a dialog of every page, opened in the browser by the link in the footer,
 * so the public pages stay stateless.
 */
class WhatRunsWhereTest
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
	void listsEveryDemoWithTheFrameworksItRunsIn()
	{
		tester.startPage(HomePage.class);

		ListView<?> support = (ListView<?>) tester.getComponentFromLastRenderedPage(
			"whatRunsWhere:overlay:body:support");
		assertEquals(Demos.all().size(), support.size());
		tester.assertLabel("whatRunsWhere:overlay:body:support:0:link:name", "Address Book");
		tester.assertLabel("whatRunsWhere:overlay:body:support:1:react", "Yes");
		tester.assertLabel("whatRunsWhere:overlay:body:support:0:react", "No");
		tester.assertLabel("whatRunsWhere:overlay:title", "What runs where");
		assertTrue(tester.getLastRenderedPage().isPageStateless());
	}

	@Test
	void theFooterOpensItInTheBrowser()
	{
		tester.startPage(HomePage.class);

		tester.assertContains("id=\"what-runs-where\"[^>]*hidden=\"hidden\"");
		tester.assertContains("data-modal-width=\"1400px\"");
		tester.assertContains("<a class=\"footer__link\" href=\"#what-runs-where\" " +
			"data-browser-modal=\"what-runs-where\">What runs where</a>");
		tester.assertContains("browser-modal(-ver-[0-9]+)?\\.js");
		tester.assertContains("antilia-modal(-ver-[0-9]+)?\\.js");
	}

	@Test
	void isOnTheDemoPagesToo()
	{
		tester.startPage(TreePage.class);

		tester.assertComponent("whatRunsWhere", BrowserModal.class);
		tester.assertContains("data-browser-modal=\"what-runs-where\"");
	}

	@Test
	void isInGermanWhenGermanIsChosen()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(HomePage.class);

		tester.assertLabel("whatRunsWhere:overlay:title", "Was wo läuft");
		tester.assertLabel("whatRunsWhere:overlay:body:support:0:react", "Nein");
	}
}
