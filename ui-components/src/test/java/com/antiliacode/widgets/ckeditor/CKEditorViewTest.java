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
package com.antiliacode.widgets.ckeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class CKEditorViewTest extends WicketTestCase
{
	@Test
	void showsTheHtmlCleanedAsCKEditorShowsIt()
	{
		CKEditorLibrary.set(tester.getApplication(), CKEditorTest.LIBRARY);
		CKEditorView view = new CKEditorView("view", Model.of("<h2 onclick=\"steal()\">Title" +
			"</h2><script>steal()</script><figure class=\"table\"><table><tbody><tr><td>c</td>" +
			"</tr></tbody></table></figure>"));
		tester.startPage(new TestPage(view, "<div wicket:id=\"view\">[the text]</div>"));

		TagTester tag = tester.getTagByWicketId("view");
		assertEquals("<h2>Title</h2><figure class=\"table\"><table><tbody><tr><td>c</td></tr>" +
			"</tbody></table></figure>", tag.getValue());
		assertEquals("antilia-ck-editor-view ck-content", tag.getAttribute("class"));
		tester.assertContains("ckeditor5-content.*\\.css");
		tester.assertContains("antilia-ckeditor.*\\.css");
		tester.assertContainsNot("ckeditor5\\.umd");
		assertSame(CKEditorPolicy.ALL, view.getSanitizer());
		assertSame(CKEditorTest.LIBRARY, view.getLibrary());
	}

	@Test
	void showsTheTextWithoutALibraryToo()
	{
		CKEditorView view = new CKEditorView("view", Model.of((String)null));
		tester.startPage(new TestPage(view, "<div wicket:id=\"view\"/>"));
		assertEquals("", tester.getTagByWicketId("view").getValue());
		tester.assertContainsNot("ckeditor5-content");
		tester.assertContains("antilia-ckeditor.*\\.css");
		assertNull(view.getLibrary());

		// one of its own
		view.setLibrary(CKEditorTest.LIBRARY);
		tester.startPage(new TestPage(view, "<div wicket:id=\"view\"/>"));
		tester.assertContains("ckeditor5-content.*\\.css");
	}

	@Test
	void cleansWithTheSanitizerGiven()
	{
		CKEditorView view = new CKEditorView("view", Model.of("<p><b>a</b></p>"));
		view.setSanitizer(html -> null);
		assertEquals("", view.getHtml());
		view.setSanitizer(CKEditorPolicy.of(Set.of()));
		assertEquals("<p>a</p>", view.getHtml());
		assertThrows(IllegalArgumentException.class, () -> view.setSanitizer(null));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private final String markup;

		TestPage(Component view, String markup)
		{
			this.markup = markup;
			add(view);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" + markup +
				"</body></html>");
		}
	}
}
