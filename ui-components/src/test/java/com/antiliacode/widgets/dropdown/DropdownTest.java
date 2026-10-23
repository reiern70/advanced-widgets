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
package com.antiliacode.widgets.dropdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class DropdownTest extends WicketTestCase
{
	@Test
	void attachesTheScriptToTheSelect()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.assertContains("antilia-dropdown.*\\.js");
		tester.assertContains("antilia-dropdown.*\\.css");
		tester.assertContains("Wicket.Dropdown.attach\\(\"" + page.single.getMarkupId() + "\"");
		tester.assertContains("Wicket.Dropdown.attach\\(\"" + page.multiple.getMarkupId() + "\"");
		assertEquals("multiple", tester.getTagByWicketId("multiple").getAttribute("multiple"));
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void passesTheTextsInTheLocaleOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage());

		tester.assertContains("\"placeholder\":\"Bitte wählen…\"");
		tester.assertContains("\"filter\":\"Filtern…\"");
		tester.assertContains("\"noMatch\":\"Keine Treffer\"");
	}

	@Test
	void passesItsOwnPlaceholderAndFilterThreshold()
	{
		TestPage page = new TestPage();
		page.single.getDropdownBehavior()
			.setPlaceholder(Model.of("Pick a fruit"))
			.setFilterThreshold(0);
		tester.startPage(page);

		tester.assertContains("\"placeholder\":\"Pick a fruit\"");
		tester.assertContains("\"filterThreshold\":0");
	}

	@Test
	void postsTheChoiceLikeANativeSelect()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.select("single", 2);
		form.selectMultiple("multiple", new int[] { 0, 2 });
		form.submit();

		assertEquals("Cherry", page.single.getModelObject());
		assertEquals(List.of("Apple", "Cherry"), page.multiple.getModelObject());
	}

	@Test
	void notifiesAnAjaxBehaviorOfTheChange()
	{
		TestPage page = new TestPage();
		page.single.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				page.changes++;
			}
		});
		tester.startPage(page);

		tester.newFormTester("form", false).select("single", 1);
		tester.executeAjaxEvent(page.single, "change");

		assertEquals("Banana", page.single.getModelObject());
		assertEquals(1, page.changes);
	}

	@Test
	void refusesATagOtherThanASelect()
	{
		TestPage page = new TestPage();
		page.form.get("text").add(new DropdownBehavior());

		assertThrows(WicketRuntimeException.class, () -> tester.startPage(page));
	}

	@Test
	void defaultsToAFilterFromEightOptions()
	{
		assertEquals(8, new DropdownBehavior().getFilterThreshold());
		assertEquals(0, new DropdownBehavior().setFilterThreshold(-3).getFilterThreshold());
		assertTrue(new DropdownBehavior().getPlaceholder() == null);
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private static final List<String> FRUITS = List.of("Apple", "Banana", "Cherry");

		final Form<Void> form = new Form<>("form");

		final Dropdown<String> single = new Dropdown<>("single", Model.of("Apple"), FRUITS);

		final MultiDropdown<String> multiple = new MultiDropdown<>("multiple",
			Model.ofList(new ArrayList<>()), FRUITS);

		int changes;

		TestPage()
		{
			add(form);
			form.add(single, multiple);
			form.add(new org.apache.wicket.markup.html.WebMarkupContainer("text"));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<select wicket:id='single'></select><select wicket:id='multiple'></select>" +
				"<span wicket:id='text'></span></form></body></html>");
		}
	}
}
