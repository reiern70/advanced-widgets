/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.autocomplete.MultiAutocomplete;
import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;

class ContactsPageTest
{
	private WicketTester tester;

	private ContactsPage page;

	private JsonBasedDynamicDataTable<?, ?> table;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.getSession().setLocale(Locale.ENGLISH);
		page = tester.startPage(ContactsPage.class);
		table = find(JsonBasedDynamicDataTable.class, component -> true);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void showsAllContactsAndTheFacetsExpanded()
	{
		assertEquals(ContactsDatabase.SIZE, table.getItemCount());
		tester.assertContains("2,500 of 2,500 contacts");
		assertEquals(3, tester.getLastResponseAsString().split("class=\"contacts-facet\"").length - 1);
		tester.assertContains("<details [^>]*open=\"open\"");
		tester.assertContains("Year of birth");
	}

	@Test
	void aCheckBoxFiltersTheContactsAndTheOtherFacetsCountThem()
	{
		long spaniards = count(contact -> "ES".equals(contact.getCountry()));
		List<FacetValue> countries = Facet.COUNTRY.values(page.getFilter(), Locale.ENGLISH);

		check(Facet.COUNTRY, "Spain", true);

		assertEquals(Set.of("ES"), page.getFilter().get(Facet.COUNTRY));
		assertEquals(spaniards, table.getItemCount());
		tester.assertContains(spaniards + " of 2,500 contacts");
		tester.assertContains("Country \\(1\\)");
		// faceted: the countries are counted as before, the professions in Spain only
		assertEquals(countries, Facet.COUNTRY.values(page.getFilter(), Locale.ENGLISH));
		assertEquals(spaniards, Facet.PROFESSION.values(page.getFilter(), Locale.ENGLISH)
			.stream().mapToInt(FacetValue::count).sum());

		check(Facet.COUNTRY, "Spain", false);

		assertTrue(page.getFilter().isEmpty());
		assertEquals(ContactsDatabase.SIZE, table.getItemCount());
	}

	@Test
	void theAutocompleteChoosesAnyValueAndItsSuggestionsCountThem()
	{
		MultiAutocomplete<?> years = find(MultiAutocomplete.class,
			search -> facetOf(search) == Facet.BIRTH_YEAR);
		@SuppressWarnings("unchecked")
		List<FacetValue> found = ((MultiAutocomplete<FacetValue>)years).getProvider()
			.search("198", 20);
		assertEquals(10, found.size());
		assertTrue(found.stream().allMatch(value -> value.label().startsWith("198")));

		tester.getRequest().getPostParameters().setParameterValue(years.getInputName(),
			"[\"1980\",\"1981\"]");
		tester.executeAjaxEvent(years, "change");

		assertEquals(Set.of("1980", "1981"), page.getFilter().get(Facet.BIRTH_YEAR));
		assertEquals(count(contact -> contact.getBirthYear() == 1980 ||
			contact.getBirthYear() == 1981), table.getItemCount());
		// the autocomplete keeps its chips; the other facets are counted again
		tester.assertComponentOnAjaxResponse(find(Component.class,
			component -> "summary".equals(component.getId())));
		assertFalse(tester.getLastResponseAsString().contains(years.getMarkupId() + "\""),
			"the autocomplete is not rendered again while the user is in it");
	}

	@Test
	void theEraserOfAFacetAndTheChipsAndClearAllDeselect()
	{
		check(Facet.COUNTRY, "Spain", true);
		check(Facet.PROFESSION, null, true);
		check(Facet.PROFESSION, null, true);
		assertEquals(2, page.getFilter().get(Facet.PROFESSION).size());

		AjaxActionButton eraser = find(AjaxActionButton.class,
			button -> facetOf(button) == Facet.PROFESSION && button.isVisibleInHierarchy());
		tester.clickLink(eraser);

		assertTrue(page.getFilter().get(Facet.PROFESSION).isEmpty());
		assertEquals(Set.of("ES"), page.getFilter().get(Facet.COUNTRY));

		@SuppressWarnings("rawtypes")
		AjaxLink remove = find(AjaxLink.class, link -> "remove".equals(link.getId()));
		tester.clickLink(remove);

		assertTrue(page.getFilter().isEmpty());

		check(Facet.COUNTRY, "Spain", true);
		check(Facet.BIRTH_YEAR, null, true);
		tester.clickLink(find(AjaxActionButton.class, button -> "clearAll".equals(button.getId())));

		assertTrue(page.getFilter().isEmpty());
		assertEquals(ContactsDatabase.SIZE, table.getItemCount());
	}

	@Test
	void theSearchOfTheNavigationFindsNamesAndEmails()
	{
		@SuppressWarnings("rawtypes")
		TextField text = find(TextField.class, field -> "text".equals(field.getId()));
		tester.getRequest().getPostParameters().setParameterValue(text.getInputName(), "lopez");
		tester.executeAjaxEvent(text, "input");

		long lopez = count(contact -> contact.getLastName().equals("López"));
		assertTrue(lopez > 0);
		assertEquals(lopez, table.getItemCount());
	}

	@Test
	void writesAContactAsAVCard()
	{
		Contact contact = ContactsDatabase.get(1);

		String card = VCard.of(contact);

		assertTrue(card.startsWith("BEGIN:VCARD\r\nVERSION:4.0\r\n"), card);
		assertTrue(card.contains("FN:" + contact.getName() + "\r\n"), card);
		assertTrue(card.contains("EMAIL:" + contact.getEmail() + "\r\n"), card);
		assertTrue(card.contains("BDAY:" + contact.getBirthDate().toString().replace("-", "")),
			card);
		assertTrue(card.endsWith("END:VCARD\r\n"), card);
	}

	/**
	 * Checks or unchecks the check box of a value of the facet, the first one not checked if no
	 * label is given.
	 */
	private void check(Facet facet, String label, boolean checked)
	{
		AjaxCheckBox box = find(AjaxCheckBox.class, candidate -> facetOf(candidate) == facet &&
			(label != null ? label.equals(candidate.getParent().get("label")
				.getDefaultModelObjectAsString()) : Boolean.FALSE.equals(candidate.getModelObject())));
		tester.getRequest().getPostParameters().setParameterValue(box.getInputName(),
			checked ? "true" : "false");
		tester.executeAjaxEvent(box, "click");
	}

	private static Facet facetOf(Component component)
	{
		FacetPanel panel = component.findParent(FacetPanel.class);
		return panel != null ? panel.getFacet() : null;
	}

	private static long count(Predicate<Contact> predicate)
	{
		return ContactsDatabase.all().stream().filter(predicate).count();
	}

	@SuppressWarnings("unchecked")
	private <C extends Component> C find(Class<? super C> type,
		Predicate<C> predicate)
	{
		Component found = tester.getLastRenderedPage().visitChildren(Component.class,
			(component, visit) -> {
				if (type.isInstance(component) && predicate.test((C)component))
				{
					visit.stop(component);
				}
			});
		if (found == null)
		{
			throw new AssertionError("no " + type.getSimpleName() + " found");
		}
		return (C)found;
	}
}
