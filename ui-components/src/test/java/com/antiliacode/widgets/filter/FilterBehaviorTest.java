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
package com.antiliacode.widgets.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.veil.LocalVeilBehavior;
import org.apache.wicket.extensions.ajax.veil.PageVeilBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.junit.jupiter.api.Test;

class FilterBehaviorTest extends WicketTestCase
{
	/** The throttling Wicket renders for a behavior: its event, then its delay. */
	private static String throttled(String event, long millis)
	{
		return "\"e\":\"" + event + "\",\"tr\":\\{\"id\":\"[^\"]*\",\"d\":" + millis +
			",\"p\":true\\}";
	}

	@Test
	void waitsForTheUserToPauseTyping()
	{
		List<String> filtered = new ArrayList<>();
		TestPage page = new TestPage();
		FilterBehavior filter = FilterBehavior
			.onFilter(target -> filtered.add(page.field.getModelObject()));
		page.field.add(filter);
		tester.startPage(page);

		// each key within 150 ms puts the request off again
		tester.assertContains(throttled("input", 150));
		assertEquals(Duration.ofMillis(150), filter.getDelay());

		tester.getRequest().getPostParameters().setParameterValue(page.field.getInputName(),
			"ann");
		tester.executeBehavior(filter);
		// the field's model holds the text when the filter is told
		assertEquals(List.of("ann"), filtered);
	}

	@Test
	void choiceFiltersOnItsOwnEventAndDelay()
	{
		TestPage page = new TestPage();
		FilterBehavior filter = FilterBehavior.onFilter("change", target -> {
		}).setDelay(Duration.ofMillis(400));
		page.field.add(filter);
		tester.startPage(page);

		tester.assertContains(throttled("change", 400));
		tester.assertContainsNot("\"d\":150");
	}

	@Test
	void noDelayFiltersAtOnce()
	{
		TestPage page = new TestPage();
		page.field.add(FilterBehavior.onFilter(target -> {
		}).setDelay(Duration.ZERO));
		tester.startPage(page);

		tester.assertContains("\"e\":\"input\"");
		tester.assertContainsNot("\"tr\":");

		page.field.getBehaviors(FilterBehavior.class).get(0).setDelay(null);
		tester.startPage(page);
		tester.assertContainsNot("\"tr\":");
	}

	@Test
	void subclassFilters()
	{
		List<String> filtered = new ArrayList<>();
		TestPage page = new TestPage();
		FilterBehavior filter = new FilterBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onFilter(AjaxRequestTarget target)
			{
				filtered.add(page.field.getModelObject());
			}
		};
		page.field.add(filter);
		tester.startPage(page);
		tester.assertContains(throttled(FilterBehavior.DEFAULT_EVENT, 150));

		tester.getRequest().getPostParameters().setParameterValue(page.field.getInputName(),
			"bo");
		tester.executeBehavior(filter);
		assertEquals(List.of("bo"), filtered);
	}

	@Test
	void leavesThePageVeilAloneUnlessToldOtherwise()
	{
		TestPage page = new TestPage();
		FilterBehavior filter = FilterBehavior.onFilter(target -> {
		});
		page.field.add(filter);
		tester.startPage(page);

		// the extra parameter a page veil leaves a request alone by
		tester.assertContains("\\{\"name\":\"" + PageVeilBehavior.NO_VEIL_PARAMETER +
			"\",\"value\":\"true\"\\}");
		assertFalse(filter.isVeilPage());

		filter.setVeilPage(true);
		tester.startPage(page);
		tester.assertContainsNot(PageVeilBehavior.NO_VEIL_PARAMETER);
	}

	@Test
	void veilsWhatItFillsWhileItsRequestRuns()
	{
		TestPage page = new TestPage();
		FilterBehavior filter = FilterBehavior.onFilter(target -> {
		}).veil(page.rows, page.actions);
		page.field.add(filter);
		page.actions.add(new LocalVeilBehavior());
		tester.startPage(page);

		assertEquals(List.of(page.rows, page.actions), filter.getVeiled());
		// each has a local veil: the one it had, or one the filter gave it
		assertEquals(1, page.rows.getBehaviors(LocalVeilBehavior.class).size());
		assertEquals(1, page.actions.getBehaviors(LocalVeilBehavior.class).size());
		String rows = page.rows.getMarkupId();
		String actions = page.actions.getMarkupId();
		String html = tester.getLastResponseAsString();
		assertTrue(html.contains("Wicket.Veil.local(\"" + rows + "\""), html);
		// raised as the request leaves, lowered once it is done
		assertTrue(html.contains("if (Wicket.Veil) {Wicket.Veil.show(\"" + rows +
			"\");Wicket.Veil.show(\"" + actions + "\");}"), html);
		assertTrue(html.contains("Wicket.Veil.hide(\"" + rows + "\");"), html);
		// the field is not veiled: its own request leaves every veil alone
		tester.assertContains("\\{\"name\":\"" + PageVeilBehavior.NO_VEIL_PARAMETER +
			"\",\"value\":\"true\"\\}");

		// a second render gives no second veil
		tester.startPage(page);
		assertEquals(1, page.rows.getBehaviors(LocalVeilBehavior.class).size());
	}

	@Test
	void doesNotVeilTheFieldItself()
	{
		TestPage page = new TestPage();
		page.field.add(FilterBehavior.onFilter(target -> {
		}).veil(page.get("form")));

		// Wicket wraps what a behavior throws while the page renders
		Throwable refused = assertThrows(RuntimeException.class, () -> tester.startPage(page));
		while (!(refused instanceof IllegalStateException) && refused.getCause() != null)
		{
			refused = refused.getCause();
		}
		assertTrue(refused instanceof IllegalStateException &&
			refused.getMessage().contains("whose veil would cover it"), String.valueOf(refused));
	}

	@Test
	void veilsNothingWithoutComponents()
	{
		TestPage page = new TestPage();
		page.field.add(FilterBehavior.onFilter(target -> {
		}));
		tester.startPage(page);

		tester.assertContainsNot("Wicket.Veil.show");
		assertTrue(page.rows.getBehaviors(LocalVeilBehavior.class).isEmpty());
	}

	@Test
	void needsWhatToDo()
	{
		assertThrows(IllegalArgumentException.class,
			() -> FilterBehavior.onFilter((SerializableConsumer<AjaxRequestTarget>)null));
	}

	/**
	 * A page with a form and the field of a filter.
	 */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final TextField<String> field = new TextField<>("field", Model.of(""));

		final WebMarkupContainer rows = new WebMarkupContainer("rows");

		final WebMarkupContainer actions = new WebMarkupContainer("actions");

		TestPage()
		{
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(field);
			add(rows, actions);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<input type=\"text\" wicket:id=\"field\"/></form><div wicket:id=\"rows\"></div>" +
				"<div wicket:id=\"actions\"></div></body></html>");
		}
	}
}
