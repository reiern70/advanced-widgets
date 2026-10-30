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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxSubmitEvent;
import com.antiliacode.widgets.event.ClientEventsTester;
import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;
import com.antiliacode.widgets.richtext.RichTextCleaning;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;
import com.antiliacode.widgets.richtext.RichTextEditorSelectedTextEvent;
import com.antiliacode.widgets.richtext.RichTextEditorState;
import com.antiliacode.widgets.richtext.RichTextFeature;
import com.antiliacode.widgets.richtext.RichTextFormat;

class CKEditorTest extends WicketTestCase
{
	private static final String HTML = "<h2>Title</h2><p>Hello <strong>world</strong></p>";

	/** CKEditor as an application brings it: stand-ins of its files, next to this test. */
	static final CKEditorLibrary LIBRARY = CKEditorLibrary.of(
		new JavaScriptResourceReference(CKEditorTest.class, "ckeditor5.umd.js"),
		new CssResourceReference(CKEditorTest.class, "ckeditor5.css"), CKEditorLibrary.GPL)
		.withContentCss(new CssResourceReference(CKEditorTest.class, "ckeditor5-content.css"))
		.withTranslation("de", new JavaScriptResourceReference(CKEditorTest.class, "de.umd.js"))
		.withTranslation("ES", new JavaScriptResourceReference(CKEditorTest.class, "es.umd.js"));

	@BeforeEach
	void bringCKEditor()
	{
		CKEditorLibrary.set(tester.getApplication(), LIBRARY);
		tester.getSession().setLocale(Locale.ENGLISH);
	}

	@Test
	void rendersTheTextAreaAndMakesCKEditorOfIt()
	{
		TestPage page = new TestPage(new CKEditor("editor", Model.of(HTML)));
		tester.startPage(page);

		TagTester area = tester.getTagByWicketId("editor");
		assertEquals("textarea", area.getName());
		assertEquals(HTML.replace("<", "&lt;").replace(">", "&gt;"), area.getValue());
		assertEquals(page.editor.getMarkupId(), area.getAttribute("id"));
		assertEquals("antilia-ck-editor-loading", area.getAttribute("class"));
		assertEquals(CKEditor.LOADING_CLASS, page.editor.getLoadingClass());

		// CKEditor of the application, then the glue
		String response = tester.getLastResponseAsString();
		assertTrue(response.indexOf("ckeditor5.umd.js") < response.indexOf("antilia-ckeditor.js"));
		tester.assertContains("ckeditor5.*\\.css");
		tester.assertContains("antilia-ckeditor.*\\.css");
		tester.assertContains("antilia-resizable.*\\.css");
		tester.assertContainsNot("de.umd.js");
		tester.assertContainsNot("antilia-rich-text-editor");

		JSONObject options = options(page.editor);
		assertEquals("GPL", options.getString("licenseKey"));
		assertFalse(options.has("language"), "English");
		assertEquals("html", options.getString("format"));
		assertEquals(CKEditor.FEATURES.size(), options.getJSONArray("features").length());
		assertEquals("[1,2,3,4]", options.getJSONArray("headingLevels").toString());
		assertFalse(options.has("toolbar"), "the tools of the features");
		assertFalse(options.has("plugins"));
		assertFalse(options.has("config"));
		assertFalse(options.has("limit"));
		assertFalse(options.has("bubbleMenu"));
		assertFalse(options.getBoolean("readOnly"));
		assertFalse(options.getBoolean("count"));
		assertFalse(options.has("saved"));
		assertFalse(options.has("placeholder"));
		assertFalse(options.has("spellcheck"));
		assertFalse(options.has("textLanguage"));
		assertEquals("vertical", options.getString("resizable"));
		assertEquals(CKEditor.DEFAULT_MIN_HEIGHT, options.getInt("minHeight"));
		assertEquals("{\"selection\":{\"anchor\":0,\"head\":0},\"scrollTop\":0,\"scrollLeft\":0}",
			options.getJSONObject("state").toString());
		JSONObject texts = options.getJSONObject("texts");
		assertEquals(14, texts.length());
		assertEquals("{0} characters", texts.getString("characters"));
		assertEquals("Part of the paste was left out: {0}.", texts.getString("pasteCleaned"));
		assertFalse(options.has("pasteNotice"));
		assertEquals("{0} word", texts.getString("word"));
		assertEquals("Open in a new tab", texts.getString("linkNewTab"));
		assertEquals("Drag, or use the arrow keys, to resize the editor", texts.getString("resize"));
		assertTrue(options.getString("stateUrl").contains("-form-editor"));
		assertEquals(page.editor.getInputName(), options.getString("inputName"));
		assertSame(CKEditorPolicy.ALL, page.editor.getSanitizer());
		assertSame(LIBRARY, page.editor.getLibrary());
		assertNull(page.editor.getLanguage());
	}

	@Test
	void leavesTheGlueAsTheBuildMinifiedIt()
	{
		assertFalse(((JavaScriptPackageResource)CKEditor.JS.getResource()).getCompress());
	}

	@Test
	void speaksTheLanguageOfThePageWhenTheLibraryHasItsTranslation()
	{
		tester.getSession().setLocale(Locale.GERMANY);
		TestPage page = new TestPage(new CKEditor("editor", Model.of(HTML)));
		tester.startPage(page);

		String response = tester.getLastResponseAsString();
		assertTrue(response.indexOf("ckeditor5.umd.js") < response.indexOf("de.umd.js"));
		assertTrue(response.indexOf("de.umd.js") < response.indexOf("antilia-ckeditor.js"));
		JSONObject options = options(page.editor);
		assertEquals("de", options.getString("language"));
		assertEquals("de", page.editor.getLanguage());
		// the editor's own texts, those of the Tiptap editor
		assertNotEquals("Open in a new tab", options.getJSONObject("texts").getString("linkNewTab"));

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(page);
		assertEquals("es", options(page.editor).getString("language"));
		tester.assertContains("es.umd.js");

		// none for French: English
		tester.getSession().setLocale(Locale.FRENCH);
		tester.startPage(page);
		assertFalse(options(page.editor).has("language"));
		tester.assertContainsNot("de\\.umd\\.js");
		tester.assertContainsNot("es\\.umd\\.js");
	}

	@Test
	void takesTheOptions()
	{
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		editor.setFormat(RichTextFormat.MARKDOWN).setHeadingLevels(2, 3)
			.setToolbar("bold", " ", "|", "link", null).setPlugins("FindAndReplace", " ", null,
				"FindAndReplace")
			.setConfig(new JSONObject().put("codeBlock", new JSONObject().put("indentSequence", "  ")))
			.setReadOnly(true).setCount(true).setPlaceholder(Model.of("Write here"))
			.setResizable(ResizeAxis.BOTH).setMinHeight(100).setMaxHeight(600).setMinWidth(200)
			.setMaxWidth(900).setSavedModel(Model.of("Before")).setSpellcheck(true)
			.setTextLanguage(Locale.GERMAN);
		editor.setLabel(Model.of("Article"));
		tester.startPage(new TestPage(editor));

		JSONObject options = options(editor);
		assertEquals("markdown", options.getString("format"));
		assertTrue(options.getBoolean("spellcheck"));
		assertEquals("de", options.getString("textLanguage"));
		String features = options.getJSONArray("features").toString();
		assertFalse(features.contains("\"underline\""), "Markdown has no syntax for it");
		assertFalse(features.contains("\"superscript\""));
		assertFalse(features.contains("\"subscript\""));
		assertFalse(features.contains("\"textAlign\""));
		assertTrue(features.contains("\"table\""));
		assertEquals("[2,3]", options.getJSONArray("headingLevels").toString());
		assertEquals("[\"bold\",\"|\",\"link\"]", options.getJSONArray("toolbar").toString());
		assertEquals("[\"FindAndReplace\"]", options.getJSONArray("plugins").toString());
		assertEquals("{\"codeBlock\":{\"indentSequence\":\"  \"}}",
			options.getJSONObject("config").toString());
		assertTrue(options.getBoolean("readOnly"));
		assertTrue(options.getBoolean("count"));
		assertEquals("Write here", options.getString("placeholder"));
		assertEquals("Article", options.getString("label"));
		assertEquals("Before", options.getString("saved"));
		assertEquals("both", options.getString("resizable"));
		assertEquals(100, options.getInt("minHeight"));
		assertEquals(600, options.getInt("maxHeight"));
		assertEquals(200, options.getInt("minWidth"));
		assertEquals(900, options.getInt("maxWidth"));
		assertEquals(List.of("bold", "|", "link"), editor.getToolbar());
		assertEquals(List.of("FindAndReplace"), editor.getPlugins());
		assertEquals("  ", editor.getConfig().getJSONObject("codeBlock").getString("indentSequence"));
		assertEquals("Write here", editor.getPlaceholder().getObject());

		// no toolbar, and back to the features'
		editor.setToolbar(List.of()).setPlugins((String[])null).setConfig(null).setResizable(null)
			.setFeatures(EnumSet.of(CKEditorFeature.BOLD));
		tester.startPage(new TestPage(editor));
		options = options(editor);
		assertFalse(options.getBoolean("toolbar"));
		assertFalse(options.has("plugins"));
		assertFalse(options.has("config"));
		assertFalse(options.getBoolean("resizable"));
		assertEquals("[\"bold\"]", options.getJSONArray("features").toString());
		assertEquals(CKEditorPolicy.of(EnumSet.of(CKEditorFeature.BOLD)), editor.getSanitizer());
		editor.setToolbar((List<String>)null);
		assertNull(editor.getToolbar());
		assertNull(editor.getConfig());
	}

	@Test
	void hasNoColorsWhichCKEditorWritesAsStyles()
	{
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		assertNull(CKEditorFeature.of(RichTextFeature.TEXT_COLOR.getName()));
		assertNull(CKEditorFeature.of(RichTextFeature.TEXT_BACKGROUND.getName()));
		assertEquals(CKEditorFeature.BOLD, CKEditorFeature.of(RichTextFeature.BOLD.getName()));
		assertEquals(CKEditor.FEATURES, editor.getFeatures());
		editor.setFeatures(EnumSet.of(CKEditorFeature.BOLD));
		assertEquals(EnumSet.of(CKEditorFeature.BOLD), editor.getFeatures());
		tester.startPage(new TestPage(editor));
		assertEquals("[\"bold\"]", options(editor).getJSONArray("features").toString());
	}

	@Test
	void keepsItsTextAsHtmlOrMarkdownOnly()
	{
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		assertThrows(IllegalArgumentException.class, () -> editor.setFormat(RichTextFormat.JSON));
		assertEquals(RichTextFormat.HTML, editor.getFormat());
		assertEquals(RichTextFormat.MARKDOWN, editor.setFormat(RichTextFormat.MARKDOWN).getFormat());
	}

	@Test
	void tellsHowToBringCKEditorWhenTheApplicationHasNone()
	{
		CKEditorLibrary.set(tester.getApplication(), null);
		assertFalse(CKEditorLibrary.isSet(tester.getApplication()));
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		Exception error = assertThrows(Exception.class, () -> tester.startPage(new TestPage(
			editor)));
		Throwable cause = error;
		while (!(cause instanceof IllegalStateException) && cause.getCause() != null)
		{
			cause = cause.getCause();
		}
		assertTrue(cause instanceof IllegalStateException, error.toString());
		assertTrue(cause.getMessage().contains("CKEditorLibrary.set(application"), cause.getMessage());

		// a library of the editor's own
		CKEditorLibrary own = LIBRARY.withLicenseKey("a-key");
		tester.startPage(new TestPage(new CKEditor("editor", Model.of(HTML)).setLibrary(own)));
		tester.assertContains("\"licenseKey\":\"a-key\"");
	}

	@Test
	void cleansTheHtmlTheFormPosts()
	{
		IModel<String> text = Model.of(HTML);
		TestPage page = new TestPage(new CKEditor("editor", text));
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p onclick=\"steal()\">Hi <a href=\"javascript:steal()\">there" +
			"</a></p><script>steal()</script><figure class=\"image\"><img src=\"/a.png\" alt=\"A\" " +
			"style=\"aspect-ratio:1/2\" width=\"10\" height=\"20\" onerror=\"steal()\"></figure>" +
			"<ul><li data-list-item-id=\"e1\">One</li></ul>");
		form.submit();
		assertEquals("<p>Hi there</p><figure class=\"image\"><img src=\"/a.png\" alt=\"A\" />" +
			"</figure><ul><li>One</li></ul>", text.getObject());

		// nothing left: no text
		form = tester.newFormTester("form");
		form.setValue("editor", "<script>steal()</script>");
		form.submit();
		assertNull(text.getObject());
	}

	@Test
	void cleansWithASanitizerOfThePage()
	{
		IModel<String> text = Model.of(HTML);
		CKEditor editor = new CKEditor("editor", text);
		editor.setSanitizer(html -> html.toUpperCase(Locale.ROOT));
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p>hi</p>");
		form.submit();
		assertEquals("<P>HI</P>", text.getObject());
	}

	@Test
	void leavesMarkdownAsTheUserWroteIt()
	{
		IModel<String> text = Model.of("");
		CKEditor editor = new CKEditor("editor", text).setFormat(RichTextFormat.MARKDOWN);
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "# Title\n\nSome <b>HTML</b> as text");
		form.submit();
		assertEquals("# Title\n\nSome <b>HTML</b> as text", text.getObject());
	}

	@Test
	void keepsTheStateAndTheTextTheBrowserSends()
	{
		List<RichTextEditorState> heard = new ArrayList<>();
		IModel<RichTextEditorState> stateModel = Model.of((RichTextEditorState)null);
		IModel<String> text = Model.of(HTML);
		CKEditor editor = new CKEditor("editor", text, stateModel)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onStateChanged(AjaxRequestTarget target)
			{
				heard.add(getState());
			}
		};
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		sendState(editor, "{\"selection\":{\"anchor\":9,\"head\":3},\"scrollTop\":12," +
			"\"scrollLeft\":3,\"width\":700,\"height\":260}", "<p>Changed</p>");

		RichTextEditorState expected = new RichTextEditorState(9, 3, 12, 3,
			ResizableSize.height(260));
		assertEquals(expected, stateModel.getObject(), "the width is the page's: vertical");
		assertEquals(List.of(expected), heard);
		assertEquals(HTML, text.getObject(), "the model waits for the form");
		tester.startPage(page);
		assertEquals("&lt;p&gt;Changed&lt;/p&gt;", tester.getTagByWicketId("editor").getValue());
		assertEquals("{\"selection\":{\"anchor\":9,\"head\":3},\"scrollTop\":12,\"scrollLeft\":3," +
			"\"height\":260}", options(editor).getJSONObject("state").toString());

		// not sending to the server: nothing heard
		editor.setSendToServer(false);
		tester.startPage(page);
		assertFalse(options(editor).has("stateUrl"));
		sendState(editor, "{\"scrollTop\":40}", null);
		assertEquals(12, editor.getState().getScrollTop());
	}

	@Test
	void tellsTheBrowserTheTextWasSaved()
	{
		IModel<String> saved = Model.of("old");
		CKEditor editor = new CKEditor("editor", Model.of("<p>new</p>")).setSavedModel(saved);
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		page.action = target -> editor.markSaved(target).focus(target);
		tester.clickLink("act");

		assertEquals("<p>new</p>", saved.getObject());
		tester.assertContains("Wicket.CKEditor&&Wicket.CKEditor.of\\(\"" + editor.getMarkupId() +
			"\"\\);if\\(e\\)\\{e.markSaved\\(\"\\\\u003cp\\\\u003enew\\\\u003c\\\\/p\\\\u003e" +
			"\"\\);");
		tester.assertContains("if\\(e\\)\\{e.focus\\(\\);");
	}

	@Test
	void hearsOfTheEventsOfTheEditorWithItsText()
	{
		List<String> saved = new ArrayList<>();
		IModel<String> text = Model.of(HTML);
		CKEditor editor = new CKEditor("editor", text);
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.SAVE, (eventId, values, form,
			target) -> {
			saved.add(editor.getModelObject());
			editor.markSaved(target);
		}));
		tester.startPage(new TestPage(editor));

		tester.assertContains("\\{id:\"antilia-rich-text-editor:save\",index:0,kind:'submit'");
		tester.assertContains("Wicket.CKEditor&&Wicket.CKEditor.of\\(element.id\\)");
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"<p>Saved <img src=x onerror=steal()></p>");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
		assertEquals(List.of("<p>Saved <img src=\"x\" /></p>"), saved);
		assertEquals("<p>Saved <img src=\"x\" /></p>", text.getObject());
	}

	@Test
	void hearsOfTheTextSelectedAsTheTiptapEditorDoes()
	{
		List<String> heard = new ArrayList<>();
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		editor.addEvent(RichTextEditorSelectedTextEvent.of((selectedText, target) -> heard.add(
			selectedText)));
		tester.startPage(new TestPage(editor));

		tester.assertContains("\\{id:\"antilia-rich-text-editor:selected-text\",index:0," +
			"kind:'ajax',delay:-1\\}");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SELECTED_TEXT,
			new JSONObject().put("text", "Hello"));
		assertEquals(List.of("Hello"), heard);
	}

	@Test
	void goesToThePageStoreAndBack()
	{
		CKEditor editor = new CKEditor("editor", Model.of(HTML));
		editor.setFeatures(EnumSet.of(CKEditorFeature.BOLD, CKEditorFeature.LINK))
			.setConfig(new JSONObject().put("a", 1)).setLibrary(LIBRARY.withLicenseKey("k"))
			.setToolbar("bold").setState(RichTextEditorState.selecting(2, 5));
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		TestPage copy = WicketObjects.cloneObject(page);
		assertEquals(RichTextEditorState.selecting(2, 5), copy.editor.getState());
		assertEquals(1, copy.editor.getConfig().getInt("a"));
		assertEquals("k", copy.editor.getLibrary().getLicenseKey());
		assertEquals(List.of("bold"), copy.editor.getToolbar());
		assertEquals(editor.getSanitizer(), copy.editor.getSanitizer());
	}

	@Test
	void librariesAreWhatTheApplicationBrings()
	{
		assertTrue(LIBRARY.isGpl());
		assertEquals("GPL", LIBRARY.getLicenseKey());
		assertEquals(List.of("de", "es"), List.copyOf(LIBRARY.getTranslations().keySet()));
		assertEquals("es", LIBRARY.getLanguage(Locale.of("es", "MX")));
		assertNull(LIBRARY.getLanguage(Locale.ITALIAN));
		assertNull(LIBRARY.getLanguage(null));
		assertNull(LIBRARY.getTranslation(null));
		assertEquals(LIBRARY.getTranslation("de"), LIBRARY.getTranslation(" DE "));
		assertThrows(UnsupportedOperationException.class, () -> LIBRARY.getTranslations().clear());

		CKEditorLibrary brazil = LIBRARY.withTranslation("pt_BR", LIBRARY.getTranslation("de"));
		assertEquals("pt-br", brazil.getLanguage(Locale.of("pt", "BR")));
		assertNull(brazil.getLanguage(Locale.of("pt", "PT")));
		assertEquals(LIBRARY, brazil.withTranslation("pt-br", null));
		assertEquals(LIBRARY.hashCode(), brazil.withTranslation("pt-br", null).hashCode());
		assertNotEquals(LIBRARY, LIBRARY.withLicenseKey("key"));
		assertNotEquals(LIBRARY, LIBRARY.withContentCss(null));
		assertFalse(LIBRARY.withLicenseKey("key").isGpl());
		assertFalse(LIBRARY.withLicenseKey("secret").toString().contains("secret"),
			"the key stays out of the logs");
		assertThrows(IllegalArgumentException.class, () -> LIBRARY.withLicenseKey(" "));
		assertThrows(IllegalArgumentException.class, () -> CKEditorLibrary.of(null,
			LIBRARY.getCss(), "GPL"));

		CKEditorLibrary cdn = CKEditorLibrary.fromUrls(
			"https://cdn.ckeditor.com/ckeditor5/49.0.0/ckeditor5.umd.js",
			"https://cdn.ckeditor.com/ckeditor5/49.0.0/ckeditor5.css", "a-key");
		assertTrue(cdn.getScript() instanceof UrlResourceReference);
		assertEquals("https://cdn.ckeditor.com/ckeditor5/49.0.0/ckeditor5.css",
			((UrlResourceReference)cdn.getCss()).getUrl().toString(Url.StringMode.FULL));
		assertNull(cdn.getContentCss());
		assertEquals(Map.of(), cdn.getTranslations());

		assertSame(LIBRARY, CKEditorLibrary.get());
		assertTrue(CKEditorLibrary.isSet(tester.getApplication()));
		assertFalse(CKEditorLibrary.isSet(null));
	}

	/**
	 * @return the options the editor was attached with in the last response
	 */
	private JSONObject options(CKEditor editor)
	{
		Matcher matcher = Pattern.compile("Wicket\\.CKEditor\\.attach\\(\"" +
			Pattern.quote(editor.getMarkupId()) + "\", (\\{.*\\})\\);").matcher(
				tester.getLastResponseAsString());
		assertTrue(matcher.find(), "the editor is attached");
		return new JSONObject(matcher.group(1));
	}

	private void sendState(CKEditor editor, String state, String text)
	{
		tester.getRequest().setParameter("state", state);
		if (text != null)
		{
			// as the browser posts it
			tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		}
		tester.executeBehavior(editor.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	@Test
	void rejectsWhatItsCleaningWouldRemove()
	{
		IModel<String> text = Model.of("");
		CKEditor editor = new CKEditor("editor", text).setCleaning(RichTextCleaning.REJECT);
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p>Sized <img src=\"/a.png\" width=\"10\" /></p>");
		form.submit();
		assertEquals("", text.getObject());
		assertEquals(List.of("The text holds what the editor does not allow: width."),
			editor.getFeedbackMessages().toList().stream().map(message -> String.valueOf(
				message.getMessage())).toList());
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final CKEditor editor;

		/** What the link does. */
		SerializableConsumer<AjaxRequestTarget> action = target -> {
		};

		TestPage(CKEditor editor)
		{
			this.editor = editor;
			Form<Void> form = new Form<>("form");
			form.add(editor);
			add(form);
			add(new AjaxLink<Void>("act")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					action.accept(target);
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"editor\"></textarea></form><a wicket:id=\"act\">Act</a>" +
				"</body></html>");
		}
	}
}
