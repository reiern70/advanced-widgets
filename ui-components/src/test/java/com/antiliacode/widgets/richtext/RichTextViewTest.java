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
package com.antiliacode.widgets.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.resizable.ResizableSize;

class RichTextViewTest extends WicketTestCase
{
	@Test
	void showsTheHtmlCleaned()
	{
		RichTextView view = new RichTextView("view", Model.of("<h2 onclick=\"steal()\">Title" +
			"</h2><script>steal()</script><p class=\"antilia-text-center\">Text</p>"));
		tester.startPage(new TestPage(view, "<div wicket:id=\"view\">[the text]</div>"));

		TagTester tag = tester.getTagByWicketId("view");
		assertEquals("<h2>Title</h2><p class=\"antilia-text-center\">Text</p>", tag.getValue());
		assertEquals("antilia-rich-text-view", tag.getAttribute("class"));
		tester.assertContains("antilia-rich-text-editor.*\\.css");
		assertSame(RichTextPolicy.ALL, view.getSanitizer());
	}

	@Test
	void showsNothingForNoText()
	{
		RichTextView view = new RichTextView("view", Model.of((String)null));
		tester.startPage(new TestPage(view, "<div wicket:id=\"view\"/>"));
		assertEquals("", tester.getTagByWicketId("view").getValue());
		assertEquals("", view.getHtml());
	}

	@Test
	void cleansWithTheSanitizerGiven()
	{
		RichTextView view = new RichTextView("view", Model.of("<p>a</p>"));
		view.setSanitizer(html -> null);
		assertEquals("", view.getHtml());
		view.setSanitizer(RichTextPolicy.of(java.util.Set.of()));
		assertEquals("<p>a</p>", view.getHtml());
		view.setSanitizer(null);
		assertSame(RichTextPolicy.ALL, view.getSanitizer());
	}

	@Test
	void keepsTheClassesOfItsPalettes()
	{
		String html = "<p><span class=\"brand-text\">brand</span> <mark class=\"brand-mark\">" +
			"marked</mark> <span class=\"antilia-color-red\">red</span> " +
			"<mark class=\"antilia-background-red\">on red</mark></p>";
		RichTextView view = new RichTextView("view", Model.of(html));
		assertEquals("<p><span>brand</span> <mark>marked</mark> " +
			"<span class=\"antilia-color-red\">red</span> " +
			"<mark class=\"antilia-background-red\">on red</mark></p>", view.getHtml());

		view.setColors(java.util.List.of(new Brand()));
		assertEquals("<p><span class=\"brand-text\">brand</span> <mark>marked</mark> " +
			"<span>red</span> <mark class=\"antilia-background-red\">on red</mark></p>",
			view.getHtml(), "the colors of the text apart from those of the background");
		assertEquals(java.util.Set.of(new Brand()), view.getColors());
		assertSame(RichTextBackgroundColor.PALETTE, view.getBackgroundColors());

		view.setBackgroundColors(java.util.List.of(new Brand()));
		assertEquals("<p><span class=\"brand-text\">brand</span> <mark class=\"brand-mark\">" +
			"marked</mark> <span>red</span> <mark>on red</mark></p>", view.getHtml());
		assertEquals(java.util.Set.of(new Brand()), view.getBackgroundColors());

		view.setColors(null).setBackgroundColors(null);
		assertEquals(java.util.Set.of(), view.getColors(), "null for no colors");
		assertEquals(java.util.Set.of(), view.getBackgroundColors());
		assertEquals("<p><span>brand</span> <mark>marked</mark> <span>red</span> <mark>on red" +
			"</mark></p>", view.getHtml());

		view.setColors(RichTextColor.PALETTE).setBackgroundColors(RichTextBackgroundColor.PALETTE);
		assertSame(RichTextPolicy.ALL, view.getSanitizer());
		assertThrows(IllegalArgumentException.class, () -> view.setColors(java.util.List.of(
			new Brand(), new Brand())));
		assertThrows(IllegalArgumentException.class, () -> view.setBackgroundColors(
			java.util.List.of(new Brand(), new Brand())));
		assertThrows(UnsupportedOperationException.class,
			() -> view.getColors().add(new Brand()));
		assertThrows(UnsupportedOperationException.class,
			() -> view.getBackgroundColors().add(new Brand()));

		assertSame(view, view.addColor(new Brand()));
		assertEquals("<p><span class=\"brand-text\">brand</span> <mark>marked</mark> " +
			"<span class=\"antilia-color-red\">red</span> " +
			"<mark class=\"antilia-background-red\">on red</mark></p>", view.getHtml(),
			"a color added keeps those of the palette");
		assertSame(view, view.addBackgroundColor(new Brand()));
		assertEquals("<p><span class=\"brand-text\">brand</span> <mark class=\"brand-mark\">" +
			"marked</mark> <span class=\"antilia-color-red\">red</span> " +
			"<mark class=\"antilia-background-red\">on red</mark></p>", view.getHtml());
		assertEquals(RichTextColor.values().length + 1, view.getColors().size());
		assertEquals(RichTextBackgroundColor.values().length + 1,
			view.getBackgroundColors().size());
		assertThrows(IllegalArgumentException.class, () -> view.addColor(new Brand()));
		assertThrows(IllegalArgumentException.class, () -> view.addBackgroundColor(new Brand()));
	}

	/** A color of the page's own. */
	record Brand() implements IRichTextColor, IRichTextBackgroundColor
	{
		@Override
		public String getName()
		{
			return "brand";
		}

		@Override
		public String getColorClass()
		{
			return "brand-text";
		}

		@Override
		public String getBackgroundClass()
		{
			return "brand-mark";
		}
	}

	@Test
	void statesParseAndPrint()
	{
		RichTextEditorState state = RichTextEditorState.parse("{\"selection\":{\"anchor\":7," +
			"\"head\":2},\"scrollTop\":-3,\"scrollLeft\":1e12,\"height\":\"tall\"}");
		assertEquals(7, state.getAnchor());
		assertEquals(2, state.getHead());
		assertFalse(state.isCursor());
		assertEquals(0, state.getScrollTop());
		assertEquals(Integer.MAX_VALUE, state.getScrollLeft());
		assertEquals(ResizableSize.NONE, state.getSize());
		assertTrue(RichTextEditorState.NONE.isCursor());
		assertNull(RichTextEditorState.parse(" "));
		assertNull(RichTextEditorState.parse(null));
		assertNull(RichTextEditorState.parse("[1, 2]"));
		assertEquals(RichTextEditorState.NONE, RichTextEditorState.parse("{}"));

		RichTextEditorState selecting = RichTextEditorState.selecting(3, 8).withSize(
			ResizableSize.of(400, 200));
		assertEquals(selecting, RichTextEditorState.parse(selecting.toJson().toString()));
		assertEquals(selecting.hashCode(), RichTextEditorState.parse(selecting.toString())
			.hashCode());
		assertNotEquals(selecting, RichTextEditorState.NONE);
		assertNotEquals(selecting, "state");
		assertEquals(new RichTextEditorState(0, 0, 0, 0, null), RichTextEditorState.selecting(-1,
			-2));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private final String markup;

		TestPage(RichTextView view, String markup)
		{
			this.markup = markup;
			add(view);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" + markup + "</body></html>");
		}
	}
}
