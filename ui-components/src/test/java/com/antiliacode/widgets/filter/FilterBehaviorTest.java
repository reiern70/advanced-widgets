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
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * Checks {@link FilterBehavior}: the request waits for the user to pause, the timer put off by
 * every event, and the model of the field holds the filter when it is called.
 */
class FilterBehaviorTest extends WicketTestCase
{
	@Test
	void waitsForTheUserToPauseTyping()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		assertEquals(FilterBehavior.DEFAULT_DELAY, page.typing.getDelay());
		assertEquals(Duration.ofMillis(150), FilterBehavior.DEFAULT_DELAY);
		// every key pressed within the delay puts the request off again
		tester.assertContains("\"e\":\"input\",\"tr\":\\{\"id\":\"" + page.text.getMarkupId() +
			"\",\"d\":150,\"p\":true\\}");
		tester.assertContains("\"e\":\"change\",\"tr\":\\{\"id\":\"" +
			page.choice.getMarkupId() + "\",\"d\":150,\"p\":true\\}");
	}

	@Test
	void givesTheFilterToTheModelBeforeItIsCalled()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.getRequest().getPostParameters().setParameterValue(page.text.getInputName(),
			"ana");
		tester.executeBehavior(page.typing);

		assertEquals(List.of("ana"), page.filtered);
		assertEquals("ana", page.text.getModelObject());
	}

	@Test
	void takesAnotherDelayOrNone()
	{
		TestPage page = new TestPage();
		page.typing.setDelay(Duration.ofMillis(400));
		tester.startPage(page);
		tester.assertContains("\"e\":\"input\",\"tr\":\\{\"id\":\"[^\"]*\",\"d\":400,\"p\":true\\}");

		page.typing.setDelay(Duration.ZERO);
		tester.startPage(page);
		tester.assertContainsNot("\"e\":\"input\",\"tr\"");

		page.typing.setDelay(null);
		tester.startPage(page);
		tester.assertContainsNot("\"e\":\"input\",\"tr\"");
	}

	@Test
	void needsWhatToDoOnAFilter()
	{
		assertThrows(IllegalArgumentException.class, () -> FilterBehavior.onFilter(null));
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final List<String> filtered = new ArrayList<>();

		final TextField<String> text = new TextField<>("text", Model.of(""));

		final DropDownChoice<String> choice = new DropDownChoice<>("choice", Model.of("a"),
			List.of("a", "b"));

		final FilterBehavior typing;

		TestPage()
		{
			Form<Void> form = new Form<>("form");
			add(form);
			typing = FilterBehavior.onFilter(target -> filtered.add(text.getModelObject()));
			text.add(typing);
			choice.add(FilterBehavior.onFilter("change", target -> {
			}));
			form.add(text, choice);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<input type='text' wicket:id='text'/><select wicket:id='choice'></select>" +
				"</form></body></html>");
		}
	}
}
