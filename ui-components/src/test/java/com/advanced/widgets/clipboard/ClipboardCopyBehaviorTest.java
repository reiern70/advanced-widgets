/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ClipboardCopyBehaviorTest extends WicketTestCase
{
	@Test
	void writesTheEscapedTextAndTheTrigger()
	{
		tester.startPage(new TestPage(new ClipboardCopyBehavior(Model.of("<a \"b\">"))
			.setTrigger(ClipboardCopyBehavior.Trigger.DOUBLE_CLICK)));

		TagTester tag = tester.getTagByWicketId("phone");
		assertEquals("<a \"b\">", tag.getAttribute("data-wicket-copy"));
		assertEquals("dblclick", tag.getAttribute("data-wicket-copy-on"));
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("data-wicket-copy=\"&lt;a &quot;b&quot;&gt;\""), page);
	}

	@Test
	void copiesOnAClickByDefaultAndRendersTheScript()
	{
		tester.startPage(new TestPage(new ClipboardCopyBehavior(Model.of("555"))));

		assertEquals("click", tester.getTagByWicketId("phone").getAttribute("data-wicket-copy-on"));
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("wicket-clipboard"), page);
		assertTrue(page.contains("Wicket.Clipboard.copiedMessage = \"Copied\";"), page);
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(ClipboardCopyBehavior behavior)
		{
			add(new Label("phone", "555").add(behavior));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><head></head><body><span wicket:id=\"phone\"></span></body></html>");
		}
	}
}
