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
package com.antiliacode.widgets.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ProgressBarTest extends WicketTestCase
{
	@Test
	void rendersANativeProgressElementWithItsValue()
	{
		tester.startPage(new BarPage(Model.of(42)));

		TagTester bar = tester.getTagByWicketId("bar");
		assertEquals("progress antilia-progress-bar", bar.getAttribute("class"));
		TagTester progress = bar.getChild("progress");
		assertEquals("42", progress.getAttribute("value"));
		assertEquals("100", progress.getAttribute("max"));
		assertTrue(bar.getValue().contains(">42%</span>"), bar.getValue());
		assertFalse(bar.getValue().contains("style="), "no inline style, for strict CSP");
	}

	@Test
	void clampsTheValue()
	{
		tester.startPage(new BarPage(Model.of(142.6)));
		assertEquals("100", value());

		tester.startPage(new BarPage(Model.of(-3)));
		assertEquals("0", value());
	}

	@Test
	void aNullValueMakesTheBarIndeterminate()
	{
		tester.startPage(new BarPage(Model.of((Integer)null)));

		TagTester bar = tester.getTagByWicketId("bar");
		TagTester progress = bar.getChild("progress");
		assertNull(progress.getAttribute("value"), "no value: indeterminate");
		assertEquals("100", progress.getAttribute("max"));
		assertFalse(bar.getValue().contains("%"), bar.getValue());
		assertTrue(((ProgressBar)tester.getComponentFromLastRenderedPage("bar")).isIndeterminate());
	}

	@Test
	void indeterminateMarkupHasNoValue()
	{
		assertEquals("<div class=\"antilia-progress-bar\">" +
			"<progress class=\"antilia-progress-bar-value\" max=\"100\"></progress></div>",
			ProgressBar.indeterminateMarkup());
	}

	private String value()
	{
		return tester.getTagByWicketId("bar").getChild("progress").getAttribute("value");
	}

	@Test
	void contributesItsStyleSheet()
	{
		tester.startPage(new BarPage(Model.of(1)));

		tester.assertContains("antilia-progress-bar.*\\.css");
	}

	@Test
	void markupWritesTheValueAsIs()
	{
		assertEquals("<div class=\"antilia-progress-bar\"><progress class=\"antilia-progress-bar-value\" " +
			"max=\"100\" value=\"{{p}}\">{{p}}%</progress><span class=\"antilia-progress-bar-label\" " +
			"aria-hidden=\"true\">{{p}}%</span></div>", ProgressBar.markup("{{p}}"));
	}

	@Test
	void aLabelOfItsOwnReplacesThePercentageEscaped()
	{
		tester.startPage(new BarPage(new ProgressBar("bar", Model.of(75))
			.setLabel(Model.of("3 of 4 <done>"))));

		TagTester bar = tester.getTagByWicketId("bar");
		assertTrue(bar.getValue().contains("aria-hidden=\"true\">3 of 4 &lt;done&gt;</span>"),
			bar.getValue());
		assertTrue(bar.getValue().contains(">75%</progress>"), "the element keeps the percentage");

		tester.startPage(new BarPage(new ProgressBar("bar", Model.of(75))
			.setLabel(Model.of((String)null))));
		assertTrue(tester.getTagByWicketId("bar").getValue().contains(">75%</span>"),
			"a label of null shows the percentage");
	}

	@Test
	void anAccessibleNameNamesTheProgressElement()
	{
		tester.startPage(new BarPage(new ProgressBar("bar", Model.of(10))
			.setAccessibleName(Model.of("Collected \"so far\""))));

		assertEquals("Collected \"so far\"", tester.getTagByWicketId("bar").getChild("progress")
			.getAttribute("aria-label"));
		assertTrue(tester.getLastResponseAsString().contains("aria-label=\"Collected &quot;so far&quot;\""),
			"escaped");

		tester.startPage(new BarPage(new ProgressBar("bar", Model.of((Integer)null))
			.setAccessibleName(Model.of("Collected"))));
		assertEquals("Collected", tester.getTagByWicketId("bar").getChild("progress")
			.getAttribute("aria-label"), "an indeterminate bar too");

		tester.startPage(new BarPage(Model.of(10)));
		assertNull(tester.getTagByWicketId("bar").getChild("progress").getAttribute("aria-label"),
			"none by default");
	}

	public static class BarPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		BarPage(IModel<? extends Number> value)
		{
			this(new ProgressBar("bar", value));
		}

		BarPage(ProgressBar bar)
		{
			add(bar);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"bar\" class=\"progress\">[bar]</div></body></html>");
		}
	}
}
