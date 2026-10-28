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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
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

		TestPage()
		{
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(field);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<input type=\"text\" wicket:id=\"field\"/></form></body></html>");
		}
	}
}
