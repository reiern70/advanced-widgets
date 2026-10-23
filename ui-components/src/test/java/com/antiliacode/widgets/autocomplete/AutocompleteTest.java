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
package com.antiliacode.widgets.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
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
		tester.assertContains("\"template\":\"\\\\u003cb>\\{\\{name\\}\\}\\\\u003c\\\\/b> " +
			"\\{\\{@label\\}\\}\"");
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
	void writesNoLabelThatClosesTheScript()
	{
		TestPage page = new TestPage();
		page.single.setModelObject(Fruit.of("cherry"));
		page.single.setPlaceholder(Model.of("</script><!--<script>alert(1)"));
		tester.startPage(page);

		String html = tester.getLastResponseAsString();
		assertFalse(html.contains("<!--<script>alert"), html);
		tester.assertContains("\\\\u003c\\\\/script>\\\\u003c!--\\\\u003cscript>alert\\(1\\)");
		assertEquals("{\"a\":\"\\u003c/b>\"}", AbstractAutocomplete.scriptSafe("{\"a\":\"</b>\"}"));
	}

	@Test
	void writesTheActionsIntoTheOptions()
	{
		TestPage page = new TestPage();
		page.single.addAction(new AutocompleteAction("info", "(i)").setTooltip("About {{name}}")
			.setLabel(Model.of("About {0}")));
		page.single.addAction(new AutocompleteAction("eat", "Eat"));
		tester.startPage(page);

		tester.assertContains("\"actions\":\\[\\{\"name\":\"info\",\"template\":\"\\(i\\)\"," +
			"\"label\":\"About \\{0\\}\",\"tooltip\":\"About \\{\\{name\\}\\}\"\\}," +
			"\\{\"name\":\"eat\",\"template\":\"Eat\"\\}\\]");
	}

	@Test
	void handsAReportedActionToOnAction()
	{
		TestPage page = new TestPage();
		page.single.addAction(new AutocompleteAction("info", "(i)").setTooltip("About"));
		page.single.addAction(new AutocompleteAction("eat", "Eat"));
		tester.startPage(page);
		tester.assertContains("antilia-autocomplete-action");

		act(page, "eat", "kiwi");
		// an action shown as a tooltip, one there is not and a value the provider does not know
		act(page, "info", "kiwi");
		act(page, "drop", "kiwi");
		act(page, "eat", "gone");

		assertEquals(List.of("eat kiwi"), page.actions);
	}

	@Test
	void listensForNoActionWhenEveryActionHasATooltip()
	{
		TestPage page = new TestPage();
		page.single.addAction(new AutocompleteAction("info", "(i)").setTooltip("About"));
		tester.startPage(page);

		tester.assertContainsNot("antilia-autocomplete-action");
	}

	private void act(TestPage page, String action, String id)
	{
		tester.getRequest().setParameter("action", action);
		tester.getRequest().setParameter("id", id);
		tester.executeBehavior(page.single.getBehaviors(AjaxEventBehavior.class).get(0));
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
	static class FruitsProvider implements IAutocompleteProvider<Fruit>
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

		final List<String> actions = new ArrayList<>();

		final Autocomplete<Fruit> single = new Autocomplete<>("single", Model.of(),
			new FruitsProvider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onAction(AjaxRequestTarget target, String action, Fruit value)
			{
				actions.add(action + " " + value.getName());
			}
		};

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
