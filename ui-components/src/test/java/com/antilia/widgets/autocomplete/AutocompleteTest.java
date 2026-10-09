/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;

class AutocompleteTest extends WicketTestCase
{
	@Test
	void rendersAHiddenInputWithTheIdsAndTheScript()
	{
		TestPage page = new TestPage();
		page.single.setModelObject(Fruit.of("cherry"));
		page.multiple.setModelObject(new ArrayList<>(List.of(Fruit.of("apple"), Fruit.of("kiwi"))));
		page.single.setTemplate("<b>{{name}}</b> {{@label}}");
		tester.startPage(page);

		TagTester single = tester.getTagByWicketId("single");
		assertEquals("hidden", single.getAttribute("type"));
		assertEquals("[\"cherry\"]", single.getAttribute("value"));
		assertEquals("[\"apple\",\"kiwi\"]",
			tester.getTagByWicketId("multiple").getAttribute("value"));
		tester.assertContains("Wicket\\.Autocomplete\\.attach\\(\"" + page.single.getMarkupId());
		tester.assertContains("\"template\":\"<b>\\{\\{name\\}\\}<\\\\/b> \\{\\{@label\\}\\}\"");
		tester.assertContains("\"chosen\":\\[\\{\"id\":\"cherry\",\"label\":\"Cherry\"\\}\\]");
		tester.assertContains("\"multiple\":true");
		tester.assertContains("antilia-autocomplete.*\\.js");
		tester.assertContains("antilia-templates.*\\.js");
		tester.assertContains("antilia-chips.*\\.css");
	}

	@Test
	void suggestsTheMatchingValuesAsJson()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		suggest(page.single, "an");

		JSONArray suggestions = new JSONArray(tester.getLastResponseAsString());
		assertEquals(2, suggestions.length(), tester.getLastResponseAsString());
		assertEquals("banana", suggestions.getJSONObject(0).getString("id"));
		assertEquals("Banana", suggestions.getJSONObject(0).getString("label"));
		assertEquals("banana",
			suggestions.getJSONObject(0).getJSONObject("data").getString("name"));
		assertEquals("mango", suggestions.getJSONObject(1).getString("id"));
	}

	@Test
	void suggestsNothingForATermTooShort()
	{
		TestPage page = new TestPage();
		page.single.setMinLength(3);
		tester.startPage(page);

		suggest(page.single, "an");

		assertEquals("[]", tester.getLastResponseAsString());
	}

	@Test
	void postsTheChosenValues()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("single", "[\"kiwi\"]");
		form.setValue("multiple", "[\"apple\",\"gone\",\"mango\",\"apple\"]");
		form.submit();

		assertEquals(Fruit.of("kiwi"), page.single.getModelObject());
		assertEquals(List.of(Fruit.of("apple"), Fruit.of("mango")),
			new ArrayList<>(page.multiple.getModelObject()));
	}

	@Test
	void clearsTheValueWhenNothingIsChosen()
	{
		TestPage page = new TestPage();
		page.single.setModelObject(Fruit.of("kiwi"));
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("single", "");
		form.setValue("multiple", "not json");
		form.submit();

		assertNull(page.single.getModelObject());
		assertTrue(page.multiple.getModelObject().isEmpty());
	}

	@Test
	void aRequiredAutocompleteNeedsAValue()
	{
		TestPage page = new TestPage();
		page.single.setRequired(true);
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("single", "");
		form.submit();

		assertTrue(page.single.hasErrorMessage());
	}

	@Test
	void notifiesAnAjaxBehaviorOfTheChange()
	{
		TestPage page = new TestPage();
		page.multiple.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				page.changes++;
			}
		});
		tester.startPage(page);

		tester.newFormTester("form", false).setValue("multiple", "[\"pear\"]");
		tester.executeAjaxEvent(page.multiple, "change");

		assertEquals(List.of(Fruit.of("pear")), new ArrayList<>(page.multiple.getModelObject()));
		assertEquals(1, page.changes);
	}

	@Test
	void refusesATagOtherThanAnInput()
	{
		assertThrows(WicketRuntimeException.class,
			() -> tester.startPage(new TestPage("<span wicket:id='single'></span>")));
	}

	private void suggest(AbstractAutocomplete<?, ?> autocomplete, String term)
	{
		AbstractAjaxBehavior behavior = autocomplete.getBehaviors(AbstractAjaxBehavior.class)
			.get(0);
		tester.getRequest().setParameter("term", term);
		tester.executeBehavior(behavior);
	}

	/** A fruit, whose JSON has its name. */
	public static class Fruit implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final String name;

		private Fruit(String name)
		{
			this.name = name;
		}

		static Fruit of(String name)
		{
			return new Fruit(name);
		}

		public String getName()
		{
			return name;
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof Fruit fruit && fruit.name.equals(name);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name);
		}
	}

	/**
	 * Fruits by their names.
	 */
	private static class FruitsProvider implements IAutocompleteProvider<Fruit>
	{
		private static final long serialVersionUID = 1L;

		private static final List<String> FRUITS = List.of("apple", "banana", "cherry", "kiwi",
			"mango", "pear");

		@Override
		public List<Fruit> search(String term, int max)
		{
			return FRUITS.stream().filter(fruit -> fruit.contains(term.toLowerCase())).limit(max)
				.map(Fruit::of).toList();
		}

		@Override
		public String getId(Fruit value)
		{
			return value.getName();
		}

		@Override
		public Fruit fromId(String id)
		{
			return FRUITS.contains(id) ? Fruit.of(id) : null;
		}

		@Override
		public String getLabel(Fruit value)
		{
			return Character.toUpperCase(value.getName().charAt(0)) + value.getName().substring(1);
		}
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private final String singleMarkup;

		final Autocomplete<Fruit> single = new Autocomplete<>("single", Model.of(),
			new FruitsProvider());

		final MultiAutocomplete<Fruit> multiple = new MultiAutocomplete<Fruit>("multiple",
			new ListModel<>(new ArrayList<>()), new FruitsProvider());

		int changes;

		TestPage()
		{
			this("<input type='hidden' wicket:id='single'/>");
		}

		TestPage(String singleMarkup)
		{
			this.singleMarkup = singleMarkup;
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(single, multiple);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				singleMarkup + "<input type='hidden' wicket:id='multiple'/></form></body></html>");
		}
	}
}
