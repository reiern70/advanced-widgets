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
package com.antiliacode.widgets.resizable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ResizableBehaviorTest extends WicketTestCase
{
	@Test
	void makesTheElementResizableByItsCornerLeavingItsSizeToThePage()
	{
		TestPage page = new TestPage(new ResizableBehavior());
		tester.startPage(page);

		TagTester box = tester.getTagByWicketId("box");
		assertEquals("box antilia-resizable antilia-resizable-both", box.getAttribute("class"));
		assertEquals(page.box.getMarkupId(), box.getAttribute("id"));
		tester.assertContains("antilia-resizable.*\\.js");
		tester.assertContains("antilia-resizable.*\\.css");
		tester.assertContains("Wicket.Resizable.attach\\(\"" + page.box.getMarkupId() +
			"\", \\{\"axis\":\"both\",\"enabled\":true,\"label\":\"Drag, or use the arrow " +
			"keys, to resize\"\\}, null\\);");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
		assertEquals(ResizableSize.NONE, page.behavior.getSize());
	}

	@Test
	void startsWithTheSizeOfItsModelAndTheLimits()
	{
		ResizableBehavior behavior = new ResizableBehavior(ResizeAxis.VERTICAL,
			Model.of(ResizableSize.of(300, 120)));
		behavior.setMinHeight(80).setMaxHeight(400).setMinWidth(10).setMaxWidth(900).setStep(5)
			.setLabel(Model.of("Resize the notes"));
		tester.startPage(new TestPage(behavior));

		assertEquals("box antilia-resizable antilia-resizable-vertical",
			tester.getTagByWicketId("box").getAttribute("class"));
		// the width is the page's: the user resizes the height alone
		tester.assertContains("\\{\"axis\":\"vertical\",\"enabled\":true,\"height\":120," +
			"\"minWidth\":10,\"minHeight\":80,\"maxWidth\":900,\"maxHeight\":400,\"step\":5," +
			"\"label\":\"Resize the notes\"\\}");
		assertEquals(ResizableSize.height(120), behavior.getSize());
		assertEquals(Integer.valueOf(80), behavior.getMinHeight());
		assertEquals(Integer.valueOf(400), behavior.getMaxHeight());
		assertEquals(Integer.valueOf(10), behavior.getMinWidth());
		assertEquals(Integer.valueOf(900), behavior.getMaxWidth());
		assertEquals(Integer.valueOf(5), behavior.getStep());
	}

	@Test
	void namesTheHandleInTheSessionsLanguage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage(new ResizableBehavior(ResizeAxis.HORIZONTAL)));

		tester.assertContains("\"label\":\"Ziehen oder die Pfeiltasten benutzen, um die Größe " +
			"anzupassen\"");
	}

	@Test
	void keepsTheSizeTheBrowserSendsAndCallsBack()
	{
		List<ResizableSize> resized = new ArrayList<>();
		ResizableBehavior behavior = new ResizableBehavior(ResizeAxis.VERTICAL)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onResized(AjaxRequestTarget target, ResizableSize size)
			{
				resized.add(size);
			}
		}.setSendToServer(true);
		TestPage page = new TestPage(behavior);
		tester.startPage(page);
		tester.assertContains("Wicket.Resizable.attach\\(.*, function \\(width,height\\)");

		send(behavior, "640", "250.6");
		assertEquals(ResizableSize.height(251), behavior.getSize());
		assertEquals(List.of(ResizableSize.height(251)), resized);
		assertTrue(behavior.isSendToServer());

		// rendered again, the element takes the size again
		tester.startPage(page);
		tester.assertContains("\"axis\":\"vertical\",\"enabled\":true,\"height\":251,");

		send(behavior, "", "");
		assertEquals(ResizableSize.NONE, behavior.getSize());
	}

	@Test
	void remembersTheSizeWhenTheComponentIsTakenAwayAndPutBack()
	{
		ResizableBehavior behavior = new ResizableBehavior(ResizeAxis.BOTH).setSendToServer(true);
		TestPage page = new TestPage(behavior);
		tester.startPage(page);
		send(behavior, "500", "200");

		page.replace(new WebMarkupContainer("box"));
		tester.startPage(page);
		tester.assertContainsNot("Wicket.Resizable.attach");
		page.replace(page.box);
		tester.startPage(page);

		tester.assertContains("\"axis\":\"both\",\"enabled\":true,\"width\":500,\"height\":200");
	}

	@Test
	void keepsTheSizeInAModelOfItsOwn()
	{
		IModel<ResizableSize> stored = Model.of((ResizableSize)null);
		ResizableBehavior first = new ResizableBehavior(ResizeAxis.HORIZONTAL, stored)
			.setSendToServer(true);
		tester.startPage(new TestPage(first));
		send(first, "333", "999");
		assertEquals(ResizableSize.width(333), stored.getObject());

		// another component with the same model starts with the size
		tester.startPage(new TestPage(new ResizableBehavior(ResizeAxis.HORIZONTAL, stored)));
		tester.assertContains("\"axis\":\"horizontal\",\"enabled\":true,\"width\":333,");
	}

	@Test
	void hearsNothingWithoutSendingToTheServer()
	{
		ResizableBehavior behavior = new ResizableBehavior();
		tester.startPage(new TestPage(behavior));
		send(behavior, "100", "100");
		assertEquals(ResizableSize.NONE, behavior.getSize());

		ResizableBehavior fixed = new ResizableBehavior().setSendToServer(true).setResizable(false);
		tester.startPage(new TestPage(fixed));
		tester.assertContains("\"enabled\":false");
		tester.assertContains("\\}, null\\);");
		send(fixed, "100", "100");
		assertEquals(ResizableSize.NONE, fixed.getSize());
		assertFalse(fixed.isResizable());
	}

	@Test
	void sizesComparePrintAndParse()
	{
		assertEquals(ResizableSize.of(1, 2), new ResizableSize(1, 2));
		assertEquals(ResizableSize.of(1, 2).hashCode(), new ResizableSize(1, 2).hashCode());
		assertEquals(ResizableSize.NONE, ResizableSize.of(null, null));
		assertTrue(ResizableSize.NONE.isNone());
		assertEquals("auto × 20px", ResizableSize.height(20).toString());
		assertEquals("20px × auto", ResizableSize.width(20).toString());
		assertEquals(ResizableSize.of(0, null), ResizableSize.parse("-5", "abc"));
		assertEquals(ResizableSize.NONE, ResizableSize.parse(null, "NaN"));
		assertEquals(ResizableSize.of(10, null), ResizableSize.of(10, 20).along(
			ResizeAxis.HORIZONTAL));
		assertNull(ResizableSize.of(10, 20).along(ResizeAxis.VERTICAL).getWidth());
		assertEquals(ResizeAxis.BOTH, ResizeAxis.fromCssName("both"));
		assertNull(ResizeAxis.fromCssName("diagonal"));
	}

	private void send(ResizableBehavior behavior, String width, String height)
	{
		tester.getRequest().setParameter("width", width);
		tester.getRequest().setParameter("height", height);
		tester.executeBehavior(behavior);
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final WebMarkupContainer box;

		final ResizableBehavior behavior;

		TestPage(ResizableBehavior behavior)
		{
			this.behavior = behavior;
			box = new WebMarkupContainer("box");
			box.add(behavior);
			add(box);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"box\" class=\"box\"></div></body></html>");
		}
	}
}
