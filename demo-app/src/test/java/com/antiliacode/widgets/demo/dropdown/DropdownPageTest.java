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
package com.antiliacode.widgets.demo.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.modal.Modal;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The {@link DropdownPage}: the countries chosen most first in a group of their own, and the
 * dialog choosing a unit of measure from a long list of long options in groups, the few used most
 * first; and what the React and Svelte apps get to do the same.
 */
class DropdownPageTest
{
	private static final String FORM = "unitDialog:overlay:dialog:content:body:form";

	/** The second button of the footer, Choose. */
	private static final String CHOOSE = "unitDialog:overlay:dialog:content:footer:buttons:2";

	/** The labels of the groups of the units, in their order. */
	private static final List<String> GROUPS = List.of("Used most", "Count", "Mass", "Length",
		"Area", "Volume", "Time", "Energy");

	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(DropdownPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void listsTheCountriesChosenMostFirstInAGroupOfTheirOwn()
	{
		String select = select("country");
		assertEquals(List.of("Chosen most", "All the others"), groups(select));
		String common = select.substring(0, select.indexOf("All the others"));
		for (String country : List.of("Germany", "Spain", "France", "United Kingdom",
			"United States"))
		{
			assertTrue(common.contains(">" + country + "<"), country);
		}
		assertFalse(common.contains(">Austria<"), common);
	}

	@Test
	void choosesAUnitInADialogFromALongListOfLongOptionsInGroups()
	{
		tester.assertContains("Unit of measure: Kilogram \\(kg\\): the base unit of mass");
		Modal dialog = (Modal)tester.getComponentFromLastRenderedPage("unitDialog");
		assertFalse(dialog.isOpen());

		tester.clickLink("themed:chooseUnit");
		assertTrue(dialog.isOpen());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(">Unit of measure<"), response);
		assertTrue(response.contains("\"size\":\"medium\""), response);
		assertTrue(response.contains("<span class=\"ant-button__label\">Cancel</span>"), response);
		assertTrue(response.contains("<span class=\"ant-button__label\">Choose</span>"), response);

		String select = select("unit");
		assertEquals(GROUPS, groups(select));
		assertEquals(UnitsOfMeasure.codes().size(), count(select, "<option "));
		assertTrue(select.contains("<option selected=\"selected\" value=\"kg\">"), select);
		// the few used most first, apart from the others
		String common = select.substring(0, select.indexOf("label=\"Count\""));
		for (String code : List.of("pc", "kg", "m", "l", "h"))
		{
			assertTrue(common.contains("value=\"" + code + "\""), code);
		}
		assertTrue(select.contains(">Acre (ac): an imperial and US unit of area of 43,560 square " +
			"feet, about 4,047 square metres<"), select);

		FormTester form = tester.newFormTester(FORM);
		form.select("unit", UnitsOfMeasure.codes().indexOf("ac"));
		tester.executeAjaxEvent(CHOOSE, "click");

		assertFalse(dialog.isOpen());
		tester.assertContains("Unit of measure: Acre \\(ac\\)");
		tester.assertNoErrorMessage();
	}

	@Test
	void closesWithoutChoosingWhenTheBrowserAsks()
	{
		tester.clickLink("themed:chooseUnit");
		tester.assertContains("data-modal-close=\"\"");
		Modal dialog = (Modal)tester.getComponentFromLastRenderedPage("unitDialog");

		// Cancel, Escape or the close button
		tester.executeBehavior(dialog.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		assertFalse(dialog.isOpen());
		tester.startPage(tester.getLastRenderedPage());
		tester.assertContains("Unit of measure: Kilogram \\(kg\\)");
	}

	@Test
	void everyUnitHasItsWordsAndItsGroupInEveryLanguage()
	{
		for (Locale locale : List.of(Locale.ENGLISH, Locale.of("es"), Locale.GERMAN))
		{
			tester.getSession().setLocale(locale);
			DropdownPage page = tester.startPage(DropdownPage.class);
			List<String> names = new ArrayList<>();
			for (String code : UnitsOfMeasure.codes())
			{
				String name = UnitsOfMeasure.name(page, code);
				// long enough to wrap in the list of the dialog
				assertTrue(name.length() > 60, locale + " " + code + ": " + name);
				assertTrue(name.contains("("), locale + " " + code + ": " + name);
				names.add(name);
				assertNotEquals(null, UnitsOfMeasure.group(page, code), code);
			}
			assertEquals(names.size(), names.stream().distinct().count(), locale.toString());
		}
	}

	@Test
	void theTextsAreInSpanishAndGermanToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(DropdownPage.class);
		tester.assertContains(">En un diálogo<");
		tester.assertContains("Unidad de medida: Kilogramo \\(kg\\)");
		assertEquals(List.of("Los más elegidos", "Todos los demás"), groups(select("country")));
		tester.clickLink("themed:chooseUnit");
		assertEquals(List.of("Las más usadas", "Cantidad", "Masa", "Longitud", "Superficie",
			"Volumen", "Tiempo", "Energía"), groups(select("unit")));
		tester.assertContains(">Elegir<");

		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(DropdownPage.class);
		tester.assertContains(">In einem Dialog<");
		tester.assertContains("Maßeinheit: Kilogramm \\(kg\\)");
		assertEquals(List.of("Am häufigsten gewählt", "Alle anderen"), groups(select("country")));
		tester.clickLink("themed:chooseUnit");
		assertEquals(List.of("Am häufigsten verwendet", "Anzahl", "Masse", "Länge", "Fläche",
			"Volumen", "Zeit", "Energie"), groups(select("unit")));
		tester.assertContains(">Wählen<");
	}

	@Test
	void givesTheAppsTheUnitsInTheirGroupsAndTheCountriesChosenMost()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals("[\"DE\",\"ES\",\"FR\",\"GB\",\"US\"]",
			config.getJSONArray("commonCountries").toString());
		assertEquals("Chosen most", config.getJSONObject("texts").getString("country.common"));
		assertEquals("All the others", config.getJSONObject("texts").getString("country.others"));

		JSONObject units = config.getJSONObject("units");
		assertEquals("kg", units.getString("value"));
		assertEquals("Choose a unit", units.getJSONObject("texts").getString("units.choose"));
		assertEquals("Unit of measure: {0}", units.getJSONObject("texts")
			.getString("units.chosen"));
		assertTrue(units.getJSONObject("icons").getString("ok").startsWith("<svg"));
		JSONArray options = units.getJSONArray("options");
		assertEquals(UnitsOfMeasure.codes().size(), options.length());
		JSONObject first = options.getJSONObject(0);
		assertEquals("pc", first.getString("value"));
		assertEquals("Used most", first.getString("group"));
		assertTrue(first.getString("label").startsWith("Piece (pc): "), first.toString());
		List<String> groups = new ArrayList<>();
		for (int i = 0; i < options.length(); i++)
		{
			String group = options.getJSONObject(i).getString("group");
			if (groups.isEmpty() || !groups.get(groups.size() - 1).equals(group))
			{
				groups.add(group);
			}
		}
		assertEquals(GROUPS, groups);
	}

	/**
	 * @return the markup of the select of a name in the last response
	 */
	private String select(String name)
	{
		String response = tester.getLastResponseAsString();
		Matcher matcher = Pattern.compile("<select[^>]* name=\"[^\"]*" + name +
			"\"[^>]*>.*?</select>", Pattern.DOTALL).matcher(response);
		assertTrue(matcher.find(), name);
		return matcher.group();
	}

	/**
	 * @return the labels of the optgroups of a select, in their order
	 */
	private static List<String> groups(String select)
	{
		List<String> groups = new ArrayList<>();
		Matcher matcher = Pattern.compile("<optgroup label=\"([^\"]*)\">").matcher(select);
		while (matcher.find())
		{
			groups.add(matcher.group(1));
		}
		return groups;
	}

	private static int count(String text, String part)
	{
		return text.split(Pattern.quote(part), -1).length - 1;
	}
}
