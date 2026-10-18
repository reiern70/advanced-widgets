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
package com.antilia.widgets.autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.autocomplete.AutocompleteTest.Fruit;
import com.antilia.widgets.autocomplete.AutocompleteTest.FruitsProvider;

class FindAutocompleteTest extends WicketTestCase
{
	@Test
	void rendersAnEmptyFindBar()
	{
		tester.startPage(new TestPage());

		assertEquals("", tester.getTagByWicketId("find").getAttribute("value"));
		tester.assertContains("\"pick\":true");
		tester.assertContains("\"chosen\":\\[\\]");
	}

	@Test
	void handsEachPickToThePageAndKeepsNone()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		pick(page, "pear");
		pick(page, "kiwi");

		assertEquals(List.of(Fruit.of("pear"), Fruit.of("kiwi")), page.picked);
		assertFalse(page.find.hasRawInput());
		tester.assertContains("\"chosen\":\\[\\]");
	}

	@Test
	void sendingTheFormPicksNothing()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.newFormTester("form").setValue("find", "[\"pear\"]").submit();

		assertEquals(List.of(), page.picked);
	}

	private void pick(TestPage page, String id)
	{
		tester.getRequest().getPostParameters().setParameterValue(page.find.getInputName(),
			"[\"" + id + "\"]");
		tester.executeAjaxEvent(page.find, "change");
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final List<Fruit> picked = new ArrayList<>();

		final FindAutocomplete<Fruit> find = new FindAutocomplete<>("find", new FruitsProvider())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPick(AjaxRequestTarget target, Fruit value)
			{
				picked.add(value);
				target.add(this);
			}
		};

		TestPage()
		{
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(find);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<input type='hidden' wicket:id='find'/></form></body></html>");
		}
	}
}
