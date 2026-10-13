/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.palette.Palette;
import com.antilia.widgets.palette.PaletteMatcher;

/**
 * Checks {@link PalettePage}: the palettes, the order of the columns while it matters, the check
 * box switching it and the configuration of the React and Svelte apps.
 */
class PalettePageTest
{
	private static final String FORM = "themed:form";

	/** The case of each component of a case, by its id. */
	private static final Map<String, String> CASES = Map.of("skills", "unordered", "filterable",
		"unordered", "columns", "orderedTitle", "ordered", "orderedTitle", "numbered",
		"orderedTitle", "countries", "manyTitle", "countriesChosen", "manyTitle", "disabled",
		"disabledTitle");

	private WicketTester tester;

	private PalettePage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(PalettePage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/**
	 * @return the path of a component of a case, relative to the form
	 */
	private static String inForm(String id)
	{
		return "cases:" + DemoCases.pathOf(CASES.getOrDefault(id, "matchTitle")) + ":" + id;
	}

	private Palette<?> palette(String id)
	{
		return (Palette<?>)tester.getComponentFromLastRenderedPage(FORM + ":" + inForm(id));
	}

	@Test
	void drawsThePalettesWithTheColumnsInTheirOrder()
	{
		tester.assertContains("Wicket.Palette.attach\\(\"" + palette("skills").getMarkupId() +
			"\", \\{\"ordered\":false");
		tester.assertContains("Wicket.Palette.attach\\(\"" + palette("columns").getMarkupId() +
			"\", \\{\"ordered\":true");
		tester.assertContains("\"selected\":\\[\"name\",\"country\",\"email\"\\]");
		tester.assertLabel("themed:choices",
			"Skills: Java, Apache Wicket; columns: Name, Country, Email.");
		assertTrue(!palette("disabled").isEnabledInHierarchy());
	}

	@Test
	void reportsTheColumnsInTheOrderPostedByAjax()
	{
		FormTester form = tester.newFormTester(FORM, false);
		tester.getRequest().getPostParameters().setParameterValues(palette("columns")
			.getInputName(), List.of(StringValue.valueOf("city"), StringValue.valueOf("name")));
		tester.executeAjaxEvent(palette("columns"), "change");

		assertEquals(List.of("city", "name"), page.getColumns());
		tester.assertComponentOnAjaxResponse("themed:choices");
		tester.assertContains("columns: City, Name\\.");
	}

	@Test
	void drawsTheCountriesByNameAPageAtATimeAndReportsThemByAjax()
	{
		tester.assertContains("Wicket.Palette.attach\\(\"" + palette("countries").getMarkupId() +
			"\", \\{[^}]*\"pageSize\":30");
		tester.assertLabel(FORM + ":" + inForm("countriesChosen"),
			"3 chosen: Germany, Spain, Uruguay.");
		List<String> codes = PalettePage.countryCodes(Locale.ENGLISH);
		assertTrue(codes.size() > 200, "many choices");
		assertEquals("AF", codes.get(0), "Afghanistan first");
		assertTrue(codes.indexOf("DE") < codes.indexOf("ES"));

		tester.newFormTester(FORM, false);
		tester.getRequest().getPostParameters().setParameterValues(palette("countries")
			.getInputName(), List.of(StringValue.valueOf("FR"), StringValue.valueOf("UY")));
		tester.executeAjaxEvent(palette("countries"), "change");

		assertEquals(List.of("FR", "UY"), page.getCountries());
		tester.assertComponentOnAjaxResponse(FORM + ":" + inForm("countriesChosen"));
		tester.assertContains("2 chosen: France, Uruguay\\.");
	}

	@Test
	void theCheckBoxSwitchesTheOrderOfTheColumns()
	{
		FormTester form = tester.newFormTester(FORM, false);
		form.setValue(inForm("ordered"), false);
		tester.executeAjaxEvent(FORM + ":" + inForm("ordered"), "change");

		assertTrue(!page.isOrdered());
		assertEquals(List.of("name", "email", "country"), page.getColumns(),
			"in the order of the choices again");
		tester.assertComponentOnAjaxResponse(palette("columns"));
		tester.assertContains("\"ordered\":false");
	}

	@Test
	void numbersTheColumnsUnderTheirOwnTitlesUntilTheCheckBoxIsUnticked()
	{
		tester.assertContains("\"availableTitle\":\"Columns left out\"");
		tester.assertContains("\"selectedTitle\":\"In the report\"");
		tester.assertContains("\"numbered\":true");

		FormTester form = tester.newFormTester(FORM, false);
		form.setValue(inForm("numbered"), false);
		tester.executeAjaxEvent(FORM + ":" + inForm("numbered"), "change");

		assertTrue(!page.isNumbered());
		tester.assertComponentOnAjaxResponse(palette("columns"));
		tester.assertContains("\"numbered\":false");
		tester.assertContains("\"selectedTitle\":\"In the report\"");
	}

	@Test
	void theCheckBoxTakesTheFilterOfTheSkillsAway()
	{
		tester.assertContains("Wicket.Palette.attach\\(\"" + palette("skills").getMarkupId() +
			"\", \\{\"ordered\":false,\"numbered\":false,\"filterable\":true");

		FormTester form = tester.newFormTester(FORM, false);
		form.setValue(inForm("filterable"), false);
		tester.executeAjaxEvent(FORM + ":" + inForm("filterable"), "change");

		assertTrue(!page.isFilterable());
		tester.assertComponentOnAjaxResponse(palette("skills"));
		tester.assertContains("\"filterable\":false");
	}

	@Test
	void theFiltersOfTheFourExamplesMatchInTheirOwnWay()
	{
		assertEquals(PaletteMatcher.START, palette("matchStart").getMatcher());
		assertEquals(PaletteMatcher.ANYWHERE, palette("matchAnywhere").getMatcher());
		assertEquals(PaletteMatcher.END, palette("matchEnd").getMatcher());
		assertEquals(PaletteMatcher.WORDS, palette("matchWords").getMatcher());
		for (String id : List.of("matchStart", "matchAnywhere", "matchEnd", "matchWords"))
		{
			tester.assertContains("Wicket.Palette.attach\\(\"" + palette(id).getMarkupId() +
				"\", \\{[^}]*\"match\":\"" + palette(id).getMatcher().getName() + "\"");
		}
		tester.assertContains("How the filter matches");
	}

	@Test
	void givesTheAppsTheTextsChoicesAndValues()
	{
		String config = page.getFrameworkConfig().toString();

		assertTrue(config.contains("\"availableTitle\":\"Available\""), config);
		assertTrue(config.contains("\"columns\":[\"name\",\"country\",\"email\"]"), config);
		assertTrue(config.contains("{\"value\":\"born\",\"label\":\"Born\"}"), config);
		assertTrue(config.contains("\"orderedTitle\":\"The order matters\""), config);
		assertTrue(config.contains("\"columnsSelected\":\"In the report\""), config);
		assertTrue(config.contains("\"numbered\":true"), config);
		assertTrue(config.contains("{\"value\":\"UY\",\"label\":\"Uruguay\"}"), config);
		assertTrue(config.contains("\"countries\":[\"DE\",\"ES\",\"UY\"]"), config);
		assertTrue(config.contains("\"countriesChosen\":\"{0} chosen: {1}.\""), config);
	}
}
