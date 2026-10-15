/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.frameworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.Page;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.autocomplete.AutocompletePage;
import com.antilia.widgets.demo.datatable.DynamicDataTablePage;
import com.antilia.widgets.demo.datatable.UsersRowsResource;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.list.JsonListPage;
import com.antilia.widgets.demo.progress.ProgressBarPage;
import com.antilia.widgets.demo.scrollbar.ScrollbarPage;
import com.antilia.widgets.demo.theme.ThemeEditorPage;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Renders the demos with {@link FrameworkTabs} and checks the configuration each page gives its
 * React and Svelte apps.
 */
class FrameworkTabsTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void leavesTheBundlesOfTheAppsAsTheBuildMinifiedThem()
	{
		// Wicket's compressor would strip them again in deployment mode, breaking their template
		// literals
		assertFalse(((JavaScriptPackageResource)FrameworkTabs.REACT.getResource()).getCompress());
		assertFalse(((JavaScriptPackageResource)FrameworkTabs.SVELTE.getResource()).getCompress());
	}

	@Test
	void showsTheTabsBelowTheExplanationAndThePageInTheWicketOne()
	{
		tester.startPage(DropdownPage.class);

		tester.assertContains("framework-tabs.*\\.js");
		tester.assertContains("framework-tabs.*\\.css");
		tester.assertContains("react-demo.*\\.js");
		tester.assertContains("svelte-demo.*\\.js");
		String response = tester.getLastResponseAsString();
		assertEquals(List.of("wicket", "react", "svelte"), TagTester.createTagsByAttribute(
			response, "role", "tab", false).stream()
			.map(tab -> tab.getAttribute("data-framework"))
			.toList());
		TagTester wicket = TagTester.createTagByAttribute(response, "id",
			"framework-panel-wicket");
		assertNull(wicket.getAttribute("hidden"), "the Wicket tab is shown first");
		assertTrue(wicket.getValue().contains("dropdown-demo__form"), "the page's components");
		// the paths of the page's components stay as they were
		tester.assertComponent("themed:examples:form:size", com.antilia.widgets.dropdown.Dropdown.class);
		for (String framework : FrameworkTabs.FRAMEWORKS)
		{
			TagTester app = app(framework);
			assertEquals("dropdown", app.getAttribute("data-demo"));
			assertEquals("hidden", app.getAttribute("hidden"));
			assertNull(app.getAttribute("data-mount"), "mounted when its tab is first shown");
		}
	}

	@Test
	void stylesTheDialogsOfTheAppsOnPagesWithoutAModal()
	{
		tester.startPage(DynamicDataTablePage.class);

		tester.assertContains("antilia-modal.*\\.css");
		tester.assertContains("antilia-scrollbar.*\\.css");
	}

	@Test
	void givesTheDropdownAppsTheTextsOptionsAndChoicesOfThePage()
	{
		tester.startPage(DropdownPage.class);

		JSONObject config = config(DropdownPage.class, "react");
		assertEquals(config.toString(), config(DropdownPage.class, "svelte").toString());
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Choose a country", texts.getString("country.none"));
		assertEquals("Size {0}, country {1}; skills: {2}; languages: {3}.",
			texts.getString("choices"));
		assertEquals("Filter…", texts.getString("filter"));
		assertEquals(new JSONArray(List.of("S", "M", "L", "XL")).toString(),
			config.getJSONArray("sizes").toString());
		assertEquals("{\"value\":\"wicket\",\"label\":\"Apache Wicket\"}",
			config.getJSONArray("skills").getJSONObject(1).toString());
		assertEquals("{\"value\":\"de\",\"label\":\"Deutsch\"}",
			config.getJSONArray("languages").getJSONObject(1).toString());
		JSONObject values = config.getJSONObject("values");
		assertEquals("M", values.getString("size"));
		assertEquals("DE", values.getString("country"));
		assertEquals("[\"java\",\"wicket\"]", values.getJSONArray("skills").toString());
		assertTrue(config.getString("choicesUrl").endsWith(ChoicesResource.PATH));
	}

	@Test
	void givesTheAutocompleteAppsTheTemplatesAndTheUrlsOfTheUsersAndCountries()
	{
		JSONObject config = config(AutocompletePage.class, "react");

		assertEquals("autocomplete", app("svelte").getAttribute("data-demo"));
		assertTrue(config.getString("userTemplate").contains("{{firstName}} {{lastName}}"));
		assertTrue(config.getString("countryTemplate").contains("{{flag}}"));
		assertTrue(config.getString("usersUrl").endsWith(UsersRowsResource.PATH));
		assertTrue(config.getString("choicesUrl").endsWith(ChoicesResource.PATH));
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Owner: {0}; team: {1}; countries: {2}.", texts.getString("choices"));
		assertEquals("'Owner (required)' is required.", texts.getString("required"));
		assertEquals("Searching…", texts.getString("searching"));
		assertEquals("[]", config.getJSONObject("values").getJSONArray("owner").toString());
	}

	@Test
	void givesTheJsonListAppsTheCardOfTheLanguageOfTheSession()
	{
		JSONObject config = config(JsonListPage.class, "svelte");

		assertEquals("jsonList", app("react").getAttribute("data-demo"));
		assertTrue(config.getString("template").contains("<dt>Cell phone</dt>"),
			config.getString("template"));
		assertEquals(24, config.getInt("perPage"));
		assertEquals("firstName", config.getString("sort"));
		assertEquals("[\"firstName\",\"lastName\",\"bornDate\"]",
			config.getJSONArray("sorts").toString());
		assertEquals("{\"value\":70,\"unit\":\"vh\"}", config.getJSONObject("height").toString());
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Filter", texts.getString("filterLabel"));
		assertEquals("Filter…", texts.getString("filter"), "the drop-downs' filter");
		assertEquals("Edit ${firstName} ${lastName}", texts.getString("editUser"));
		assertEquals("Save", texts.getString("save"), "from the bundle of the Wicket form");
		assertEquals("'{0}' is required.", texts.getString("required"));

		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(JsonListPage.class);
		JSONObject german = config(JsonListPage.class, "svelte");
		assertTrue(german.getString("template").contains("<dt>Mobiltelefon</dt>"),
			"the card in German, UserCard_de.html");
		assertEquals("Speichern", german.getJSONObject("texts").getString("save"));
	}

	@Test
	void givesTheScrollbarAndProgressAppsTheirTextsAndState()
	{
		JSONObject scrollbar = config(ScrollbarPage.class, "react");
		assertEquals(20, scrollbar.getInt("lines"));
		assertEquals("Line {0}", scrollbar.getJSONObject("texts").getString("line"));

		JSONObject progress = config(ProgressBarPage.class, "react");
		assertEquals(0, progress.getInt("progress"));
		assertEquals("Start", progress.getJSONObject("texts").getString("start"));
	}

	@Test
	void leavesTheTabsOutOfDemosWithoutApps()
	{
		tester.startPage(ThemeEditorPage.class);

		tester.assertContainsNot("framework-tabs.*\\.js");
		tester.assertContainsNot("role=\"tablist\"");
		tester.assertContainsNot("react-demo");
		assertNull(TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"data-framework-app", "react"));
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);

		JSONObject texts = config(DropdownPage.class, "react").getJSONObject("texts");
		assertEquals("Land wählen", texts.getString("country.none"));
		assertEquals("Filtern…", texts.getString("filter"));
		assertEquals("Demo zeigen in", TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "role", "tablist").getAttribute("aria-label"));
	}

	/**
	 * @return the configuration a page gives the app of a framework
	 */
	private JSONObject config(Class<? extends Page> page, String framework)
	{
		if (!page.isInstance(tester.getLastRenderedPage()))
		{
			tester.startPage(page);
		}
		return new JSONObject(app(framework).getAttribute("data-config"));
	}

	private TagTester app(String framework)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"data-framework-app", framework);
	}
}
