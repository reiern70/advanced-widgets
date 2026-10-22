/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.theme.Theme;

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
	public void theStyleSheetsUrlChangesWithItsContentsInDeployment()
	{
		tester.destroy();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
		tester.startPage(HomePage.class);

		// a hash of the contents in the name, so that browsers do not keep an old copy
		tester.assertContains("BasePage-ver-[0-9A-F]+\\.css");
		assertFalse(tester.getLastResponseAsString().contains("href=\"style.css\""));
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
	public void homepageListsTheDemosOnly()
	{
		tester.startPage(HomePage.class);

		ListView<?> categories = (ListView<?>) tester.getComponentFromLastRenderedPage(
			"categories");
		assertEquals(Demos.all().stream().map(Demos.Demo::category).distinct().count(),
			categories.size());
		tester.assertLabel("categories:1:category", "Data");
		tester.assertLabel("categories:1:demos:0:link:name", "JSON Grid");
		// the index previews the screenshot of a demo and what it shows
		tester.assertContains("data-image=\"[^\"]*images/demos/jsonTable.png\"");
		tester.assertContains("data-description=\"A data grid whose rows are rendered in the browser");
		assertNull(tester.getLastRenderedPage().get("components"));
		tester.assertContainsNot("components in the catalog");
		tester.assertContains("Ernesto Reinaldo Barreiro");
		tester.assertContains("Licensed under the Apache License, Version 2.0.");
	}

	@Test
	public void everyPageLinksToTheProjectAndItsIssuesOnGitHub()
	{
		tester.startPage(HomePage.class);

		tester.assertContains("<a class=\"topbar__github\" " +
			"href=\"https://github.com/reiern70/advanced-widgets\" title=\"GitHub: the code and the issues\">");
		tester.assertContains("<a class=\"footer__link\" " +
			"href=\"https://github.com/reiern70/advanced-widgets\">Code on GitHub</a>");
		tester.assertContains("<a class=\"footer__link\" " +
			"href=\"https://github.com/reiern70/advanced-widgets/issues\">Issues</a>");
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
	public void showsTheCommitAndInDemoModeTheBranch()
	{
		BuildInfo build = BuildInfo.get();
		assertTrue(build.isKnown(), "the build writes build-info.properties");

		tester.startPage(HomePage.class);
		tester.assertVisible("buildInfo");
		tester.assertContains("Commit " + Pattern.quote(build.commit()));
		tester.assertContainsNot("Branch " + Pattern.quote(build.branch() + " · "));

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

		tester.assertLabel("categories:0:demos:0:link:name", "Agenda de contactos");
		tester.assertContains(Pattern.quote("Bajo la Apache License, versión 2.0."));
		tester.assertContains("Español");
	}

	@Test
	public void homepageIsInGermanWhenGermanIsChosen()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(HomePage.class);

		tester.assertLabel("categories:0:demos:0:link:name", "Adressbuch");
		tester.assertContains(Pattern.quote("Lizenziert unter der Apache License, Version 2.0."));
	}

	@Test
	public void homepageIsStateless()
	{
		tester.startPage(HomePage.class);

		assertTrue(tester.getLastRenderedPage().isPageStateless());
		// nothing to keep, so no session either
		assertTrue(tester.getSession().isTemporary());
		// the websocket of the status goes to the shared resource, not to the page
		tester.assertContains("resourceName: 'live'");
	}

	@Test
	public void choosingAThemeOnTheStatelessHomePageKeepsIt()
	{
		tester.startPage(HomePage.class);

		FormTester form = tester.newFormTester("settings");
		form.select("theme", 1);
		tester.executeAjaxEvent("settings:theme", "change");

		assertEquals(Theme.BLUE, DemoSession.get().getTheme());
		assertFalse(tester.getSession().isTemporary());
	}
}
