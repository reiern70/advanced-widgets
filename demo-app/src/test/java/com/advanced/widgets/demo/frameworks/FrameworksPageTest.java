/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.frameworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.advanced.widgets.demo.WicketApplication;
import com.advanced.widgets.demo.datatable.UsersRowsResource;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Renders {@link FrameworksPage} and requests the {@link ChoicesResource} its React and Svelte
 * demos load.
 */
class FrameworksPageTest
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
	void givesEachFrameworkTheUrlOfTheChoicesAndTheTexts()
	{
		tester.startPage(FrameworksPage.class);

		tester.assertContains("react-demo.*\\.js");
		tester.assertContains("svelte-demo.*\\.js");
		tester.assertContains("wicket-dropdown.*\\.css");
		tester.assertContains("wicket-autocomplete.*\\.css");
		tester.assertContains("json-infinite-list.*\\.css");
		for (String framework : new String[] { "react", "svelte" })
		{
			TagTester demo = TagTester.createTagByAttribute(tester.getLastResponseAsString(),
				"data-framework-demo", framework);
			assertTrue(demo.getAttribute("data-choices").endsWith(ChoicesResource.PATH),
				demo.getAttribute("data-choices"));
			assertTrue(demo.getAttribute("data-users").endsWith(UsersRowsResource.PATH),
				demo.getAttribute("data-users"));
			JSONObject texts = new JSONObject(demo.getAttribute("data-texts"));
			assertEquals("Choose a country", texts.getString("countryNone"));
			assertEquals("Country {0}; skills: {1}.", texts.getString("chosen"));
			assertEquals("Filter…", texts.getString("filter"));
			assertEquals("All ${count} users are shown.", texts.getString("listEnd"));
			assertEquals("Picked the user with ID {0}.", texts.getString("picked"));
		}
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(FrameworksPage.class);

		tester.assertContains(Pattern.quote("Bitte ein Land wählen"));
		tester.assertContains(Pattern.quote("Filtern…"));
	}

	@Test
	void speaksSpanishToo()
	{
		tester.getSession().setLocale(Locale.forLanguageTag("es"));
		tester.startPage(FrameworksPage.class);

		tester.assertContains(Pattern.quote("Elija un país"));
		tester.assertContains(Pattern.quote("Quitar {0}"));
	}

	@Test
	void servesTheCountriesInTheLanguageOfTheSessionAndTheSkills()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.executeUrl(ChoicesResource.PATH);

		assertEquals("application/json", tester.getLastResponse().getContentType().split(";")[0]);
		JSONObject choices = new JSONObject(tester.getLastResponseAsString());
		JSONArray countries = choices.getJSONArray("countries");
		assertEquals(Locale.getISOCountries().length, countries.length());
		assertTrue(countries.toString().contains("{\"value\":\"DE\",\"label\":\"Deutschland\"}"),
			countries.toString());
		assertEquals("Ägypten", label(countries, "EG"));
		assertTrue(indexOf(countries, "EG") < indexOf(countries, "FI"),
			"sorted by the German names: Ägypten before Finnland");
		assertEquals(new JSONObject().put("value", "java").put("label", "Java").toString(),
			choices.getJSONArray("skills").getJSONObject(0).toString());
	}

	private static String label(JSONArray choices, String value)
	{
		return choices.getJSONObject(indexOf(choices, value)).getString("label");
	}

	private static int indexOf(JSONArray choices, String value)
	{
		for (int i = 0; i < choices.length(); i++)
		{
			if (value.equals(choices.getJSONObject(i).getString("value")))
			{
				return i;
			}
		}
		return -1;
	}
}
