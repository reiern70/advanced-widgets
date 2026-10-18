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
package com.antilia.widgets.palette;

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
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class PaletteTest extends WicketTestCase
{
	@Test
	void attachesTheScriptToTheSelect()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.assertContains("antilia-palette.*\\.js");
		tester.assertContains("antilia-palette.*\\.css");
		tester.assertContains("antilia-scrollbar.*\\.js");
		tester.assertContains("Wicket.Palette.attach\\(\"" + page.palette.getMarkupId() + "\"");
		tester.assertContains("\"ordered\":false");
		tester.assertContains("\"availableTitle\":\"Available\"");
		tester.assertContains("\"filterThreshold\":8");
		tester.assertContains("\"pageSize\":30");
		tester.assertContains("\"numbered\":false");
		tester.assertContains("\"filterable\":true");
		assertEquals("multiple", tester.getTagByWicketId("palette").getAttribute("multiple"));
		assertFalse(tester.getLastResponseAsString().contains("\"selected\":["),
			"the select gives the choice while the order does not matter");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void passesTheTextsInTheLocaleOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage());

		tester.assertContains("\"availableTitle\":\"Verfügbar\"");
		tester.assertContains("\"selectedTitle\":\"Ausgewählt\"");
		tester.assertContains("\"addAll\":\"Alle hinzufügen\"");
	}

	@Test
	void passesTheChosenOnesInTheirOrderWhileTheOrderMatters()
	{
		TestPage page = new TestPage();
		page.palette.setModelObject(new ArrayList<>(List.of("Cherry", "Apple")));
		page.palette.setOrdered(true).setPageSize(5).getPaletteBehavior().setFilterThreshold(0);
		tester.startPage(page);

		tester.assertContains("\"ordered\":true");
		tester.assertContains("\"filterThreshold\":0");
		tester.assertContains("\"pageSize\":5");
		tester.assertContains("\"selected\":\\[\"2\",\"0\"\\]");
	}

	@Test
	void numbersTheRowsAndTakesItsOwnTitles()
	{
		TestPage page = new TestPage();
		page.palette.setNumbered(true)
			.setAvailableTitle(Model.of("Fruit"))
			.setSelectedTitle(Model.of("Basket"));
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(page);

		assertTrue(page.palette.isNumbered());
		tester.assertContains("\"numbered\":true");
		tester.assertContains("\"availableTitle\":\"Fruit\"");
		tester.assertContains("\"selectedTitle\":\"Basket\"");
		tester.assertContains("\"addAll\":\"Alle hinzufügen\"");

		// an empty title gives that of the bundles again
		page.palette.setAvailableTitle(Model.of("")).setSelectedTitle(null);
		tester.startPage(page);
		tester.assertContains("\"availableTitle\":\"Verfügbar\"");
		tester.assertContains("\"selectedTitle\":\"Ausgewählt\"");
	}

	@Test
	void keepsThePostedOrderInTheModel()
	{
		TestPage page = new TestPage();
		page.palette.setOrdered(true);
		tester.startPage(page);

		// FormTester posts the options in the order of the choices, the browser in their order in
		// the select, which the palette sets
		FormTester form = tester.newFormTester("form", false);
		tester.getRequest().getPostParameters().setParameterValues(page.palette.getInputName(),
			List.of(StringValue.valueOf("2"), StringValue.valueOf("0")));
		form.submit();

		assertEquals(List.of("Cherry", "Apple"), page.palette.getModelObject());
	}

	@Test
	void notifiesAnAjaxBehaviorOfTheChange()
	{
		TestPage page = new TestPage();
		page.palette.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				page.changes++;
			}
		});
		tester.startPage(page);

		tester.newFormTester("form", false).selectMultiple("palette", new int[] { 1 });
		tester.executeAjaxEvent(page.palette, "change");

		assertEquals(List.of("Banana"), page.palette.getModelObject());
		assertEquals(1, page.changes);
	}

	@Test
	void matchesAnywhereUnlessSetOtherwise()
	{
		TestPage page = new TestPage();
		tester.startPage(page);
		tester.assertContains("\"match\":\"anywhere\"");
		assertEquals(PaletteMatcher.ANYWHERE, page.palette.getMatcher());

		page = new TestPage();
		page.palette.setMatcher(PaletteMatcher.START);
		tester.startPage(page);
		tester.assertContains("\"match\":\"start\"");

		page = new TestPage();
		page.palette.setMatcher(PaletteMatcher.END);
		tester.startPage(page);
		tester.assertContains("\"match\":\"end\"");

		page = new TestPage();
		page.palette.setMatcher(PaletteMatcher.WORDS);
		tester.startPage(page);
		tester.assertContains("\"match\":\"words\"");

		page = new TestPage();
		page.palette.setMatcher(null);
		assertEquals(PaletteMatcher.ANYWHERE, page.palette.getMatcher());
	}

	@Test
	void rendersTheScriptOfAMatcherOfItsOwn()
	{
		TestPage page = new TestPage();
		page.palette.setMatcher(new IPaletteMatcher()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getName()
			{
				return "words";
			}

			@Override
			public void renderHead(IHeaderResponse response)
			{
				response.render(JavaScriptHeaderItem.forScript(
					"Wicket.Palette.matchers.words = function () { return null; };", "words"));
			}
		});
		tester.startPage(page);

		tester.assertContains("\"match\":\"words\"");
		tester.assertContains("Wicket.Palette.matchers.words = ");
	}

	@Test
	void refusesATagOtherThanASelect()
	{
		TestPage page = new TestPage();
		page.form.get("text").add(new PaletteBehavior());

		assertThrows(WicketRuntimeException.class, () -> tester.startPage(page));
	}

	@Test
	void defaultsToAnUnorderedPaletteWithAFilterFromEightChoices()
	{
		PaletteBehavior behavior = new PaletteBehavior();
		assertEquals(8, behavior.getFilterThreshold());
		assertFalse(behavior.isOrdered());
		assertFalse(behavior.isNumbered());
		assertTrue(behavior.isFilterable());
		assertFalse(behavior.setFilterable(false).isFilterable());
		assertFalse(new Palette<>("p", Model.ofList(new ArrayList<String>()), List.of("A"))
			.setFilterable(false).isFilterable());
		assertEquals(null, behavior.getAvailableTitle());
		assertEquals(null, behavior.getSelectedTitle());
		assertEquals(0, behavior.setFilterThreshold(-3).getFilterThreshold());
		assertEquals(30, behavior.getPageSize());
		assertEquals(0, behavior.setPageSize(-1).getPageSize());
		assertEquals(50, new Palette<>("p", Model.ofList(new ArrayList<String>()), List.of("A"))
			.setPageSize(50).getPageSize());
		assertTrue(behavior.setOrdered(true).isOrdered());
		assertFalse(new Palette<>("p", Model.ofList(new ArrayList<String>()), List.of("A"))
			.isOrdered());
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private static final List<String> FRUITS = List.of("Apple", "Banana", "Cherry");

		final Form<Void> form = new Form<>("form");

		final Palette<String> palette = new Palette<>("palette", Model.ofList(new ArrayList<>()),
			FRUITS);

		int changes;

		TestPage()
		{
			add(form);
			form.add(palette);
			form.add(new WebMarkupContainer("text"));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<select wicket:id='palette'></select><span wicket:id='text'></span></form>" +
				"</body></html>");
		}
	}
}
