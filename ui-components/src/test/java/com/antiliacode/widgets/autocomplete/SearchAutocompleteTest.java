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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.autocomplete.AutocompleteTest.Fruit;
import com.antiliacode.widgets.autocomplete.AutocompleteTest.FruitsProvider;

class SearchAutocompleteTest extends WicketTestCase
{
	@Test
	void rendersTheTextAsTheValueOfAFreeTextAutocomplete()
	{
		TestPage page = new TestPage();
		page.search.setModelObject("kiwi & <pear>");
		tester.startPage(page);

		assertEquals("kiwi & <pear>", tester.getTagByWicketId("search").getAttribute("value"));
		// escaped once, as an attribute
		tester.assertContains("value=\"kiwi &amp; &lt;pear&gt;\"");
		tester.assertContains("Wicket\\.Autocomplete\\.attach\\(\"" + page.search.getMarkupId() +
			"\", \\{[^}]*\"freeText\":true");
		tester.assertContains("\"chosen\":\\[\\]");
		// the others are no search boxes
		tester.assertContains("Wicket\\.Autocomplete\\.attach\\(\"" + page.single.getMarkupId() +
			"\", \\{[^}]*\"freeText\":false");
	}

	@Test
	void postsTheTextTyped()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("search", "  red apples ");
		form.submit();

		assertEquals("red apples", page.search.getModelObject());

		form = tester.newFormTester("form");
		form.setValue("search", "");
		form.submit();

		assertNull(page.search.getModelObject());
	}

	@Test
	void aRequiredSearchBoxNeedsAText()
	{
		TestPage page = new TestPage();
		page.search.setRequired(true);
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("search", " ");
		form.submit();

		assertTrue(page.search.hasErrorMessage());
	}

	@Test
	void suggestsTheValuesMatchingTheText()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.getRequest().setParameter("term", "an");
		tester.executeBehavior(page.search.getBehaviors(AbstractAjaxBehavior.class).get(0));

		tester.assertContains("\"label\":\"Banana\"");
		tester.assertContains("\"label\":\"Mango\"");
	}

	@Test
	void anAjaxChooseBehaviorUpdatesTheModelAndGetsTheValueChosen()
	{
		TestPage page = new TestPage();
		List<String> chosen = new ArrayList<>();
		page.search.add(new AjaxChooseBehavior<Fruit>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChoose(AjaxRequestTarget target, Fruit value)
			{
				chosen.add(value.getName() + " " + page.search.getModelObject());
			}
		});
		page.single.add(new AjaxChooseBehavior<Fruit>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChoose(AjaxRequestTarget target, Fruit value)
			{
				chosen.add(value.getName() + " " + page.single.getModelObject().getName());
			}
		});
		tester.startPage(page);
		tester.assertContains(AbstractAutocomplete.CHOOSE_EVENT);

		choose(page, page.search, "Kiwi", "kiwi");
		choose(page, page.single, "[\"pear\"]", "pear");
		// a value the provider does not know
		choose(page, page.search, "Plum", "plum");

		assertEquals(List.of("kiwi Kiwi", "pear pear"), chosen);
		assertEquals("Plum", page.search.getModelObject());
	}

	private void choose(TestPage page, AbstractAutocomplete<?, ?> autocomplete, String value,
		String id)
	{
		tester.newFormTester("form", false).setValue(autocomplete.getId(), value);
		tester.getRequest().setParameter("chosen", id);
		tester.executeAjaxEvent(autocomplete, AbstractAutocomplete.CHOOSE_EVENT);
	}

	@Test
	void submitOnChooseHasTheBrowserSendTheForm()
	{
		TestPage page = new TestPage();
		page.search.add(new SubmitOnChooseBehavior());
		tester.startPage(page);

		tester.assertContains("Wicket\\.Autocomplete\\.submitOnChoose\\(\"" +
			page.search.getMarkupId() + "\"\\);");
	}

	@Test
	void theBehaviorsOfAChoiceGoWithAnAutocompleteOnly()
	{
		TextField<String> field = new TextField<>("field", Model.of(""));
		assertThrows(WicketRuntimeException.class, () -> field.add(new SubmitOnChooseBehavior()));
		assertThrows(WicketRuntimeException.class, () -> field.add(new AjaxChooseBehavior<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChoose(AjaxRequestTarget target, String value)
			{
			}
		}));
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final SearchAutocomplete<Fruit> search = new SearchAutocomplete<>("search", Model.of(),
			new FruitsProvider());

		final Autocomplete<Fruit> single = new Autocomplete<>("single", Model.of(),
			new FruitsProvider());

		TestPage()
		{
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(search, single);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<input type='hidden' wicket:id='search'/><input type='hidden' wicket:id='single'/>" +
				"</form></body></html>");
		}
	}
}
