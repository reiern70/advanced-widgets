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
package com.antiliacode.widgets.demo.richtext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.event.ClientEventsBehavior;
import com.antiliacode.widgets.event.IAjaxEvent;
import com.antiliacode.widgets.event.IAjaxSubmitEvent;
import com.antiliacode.widgets.event.IClientEvent;
import com.antiliacode.widgets.richtext.IRichTextSuggester;
import com.antiliacode.widgets.richtext.RichTextEditor;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.antiliacode.widgets.richtext.RichTextImageUploadBehavior;
import com.antiliacode.widgets.richtext.RichTextLibrary;
import com.antiliacode.widgets.richtext.RichTextProofreadBehavior;
import com.antiliacode.widgets.richtext.RichTextSuggestBehavior;
import com.antiliacode.widgets.richtext.RichTextTool;
import com.github.openjson.JSONObject;

/**
 * Checks {@link RichTextEditorPage}: the article whose HTML and state the session keeps, cleaned,
 * the options the form switches, the toolbar chosen tool by tool, saving, the notes in Markdown,
 * the cleaning of HTML from anywhere, the configuration of the React and Svelte apps and the library
 * the editor is based on.
 */
class RichTextEditorPageTest
{
	private static final String EDITOR = inCase("articleTitle", "editorForm:editor");

	private static final String NOTES = inCase("notesTitle", "notesForm:editor");

	private WicketTester tester;

	/**
	 * @return the path of a component of a case
	 */
	private static String inCase(String name, String path)
	{
		return "themed:cases:" + DemoCases.pathOf(name) + ":" + path;
	}

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(RichTextEditorPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private RichTextEditor editor(String path)
	{
		return (RichTextEditor)tester.getComponentFromLastRenderedPage(path);
	}

	/**
	 * Sends the editor's state, and the text, as the browser does.
	 */
	private void sendState(RichTextEditor editor, String state, String text)
	{
		tester.getRequest().setParameter("state", state);
		if (text != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		}
		tester.executeBehavior(editor.getBehaviors(AbstractDefaultAjaxBehavior.class).stream()
			.filter(behavior -> !(behavior instanceof AjaxEventBehavior)).findFirst()
			.orElseThrow());
	}

	/**
	 * Fires an event of the editor with its text, as the browser does.
	 */
	private void fire(RichTextEditor editor, String event, String text)
	{
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		List<IClientEvent> events = editor.getEvents();
		IClientEvent found = events.stream().filter(each -> each.getId().equals(event) &&
			(each instanceof IAjaxEvent || each instanceof IAjaxSubmitEvent)).findFirst()
			.orElseThrow();
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, event);
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER,
			String.valueOf(events.indexOf(found)));
		tester.executeBehavior(editor.getBehaviors(ClientEventsBehavior.class).get(0)
			.getAjaxBehavior(found));
	}

	@Test
	void showsTheArticleInAnEditorAndAsAPageShowsIt()
	{
		RichTextEditor editor = editor(EDITOR);
		assertEquals(RichTextFormat.HTML, editor.getFormat());
		assertEquals(RichTextTool.DEFAULT_TOOLBAR, editor.getToolbar());
		assertTrue(editor.isBubbleMenu());
		assertTrue(editor.isCount());
		assertFalse(editor.isReadOnly());
		tester.assertContains("Wicket.RichTextEditor.attach");
		tester.assertContains("antilia-rich-text-editor.*\\.js");
		tester.assertContains("\"placeholder\":\"Write the article…\"");
		tester.assertLabel(inCase("articleTitle", "known"), "The server knows: " +
			RichTextSamples.ARTICLE.length() + " characters of HTML, " +
			RichTextDocument.words(RichTextSamples.ARTICLE).size() + " words; the cursor at 0; " +
			"the size the page gives it; all saved.");
		// the article as a page shows it, cleaned
		tester.assertContains("antilia-rich-text-view\"[^>]*><h2>Field notes from the coast</h2>");
		assertFalse(Pattern.compile("<[a-z][^>]*\\sstyle=").matcher(
			tester.getLastResponseAsString()).find(), "no inline style");
	}

	@Test
	void keepsTheHtmlCleanedAndTheStateTheBrowserSendsInTheSession()
	{
		RichTextEditor editor = editor(EDITOR);
		String html = "<p onclick=\"steal()\">New <b>text</b></p><script>steal()</script>";
		sendState(editor, "{\"selection\":{\"anchor\":3,\"head\":7},\"scrollTop\":40," +
			"\"height\":420}", html);
		fire(editor, RichTextEditorEvents.INPUT, html);

		RichTextDocument article = RichTextEditorPage.article();
		assertEquals("<p>New <b>text</b></p>", article.getText());
		assertTrue(article.isModified());
		assertEquals(7, article.getState().getHead());
		assertEquals(Integer.valueOf(420), article.getState().getSize().getHeight());
		tester.assertContains("selected from 3 to 7; auto × 420 px; changes not saved");

		// a page made anew shows them again, from the session
		tester.startPage(RichTextEditorPage.class);
		tester.assertContains("\"state\":\\{\"selection\":\\{\"anchor\":3,\"head\":7\\}," +
			"\"scrollTop\":40,\"scrollLeft\":0,\"height\":420\\}");
		tester.assertContains("\"saved\":\"<h2>Field notes from the coast");
	}

	@Test
	void savesWithCtrlSAndTheButton()
	{
		RichTextEditor editor = editor(EDITOR);
		fire(editor, RichTextEditorEvents.SAVE, "<p>Saved <em>now</em></p>");

		RichTextDocument article = RichTextEditorPage.article();
		assertEquals("<p>Saved <em>now</em></p>", article.getSaved());
		assertFalse(article.isModified());
		tester.assertContains("e.markSaved\\(\"<p>Saved <em>now<\\\\/em><\\\\/p>\"\\)");
		tester.assertContains("Saved the article.");
		// the article as a page shows it, saved
		tester.assertContains("antilia-rich-text-view\"[^>]*><p>Saved <em>now</em></p>");

		FormTester form = tester.newFormTester(inCase("articleTitle", "editorForm"));
		form.setValue("editor", "<p>Again</p>");
		tester.executeAjaxEvent(inCase("articleTitle", "editorForm:save"), "click");
		assertEquals("<p>Again</p>", article.getSaved());
	}

	@Test
	void hearsOfTheFocus()
	{
		RichTextEditor editor = editor(EDITOR);
		fire(editor, RichTextEditorEvents.FOCUS, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you entered the article.");
		// the text the server has: unchanged
		fire(editor, RichTextEditorEvents.BLUR, editor.getModelObject());
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you left the article, with its text.");
		// another text, which the model gets
		int before = editor.getModelObject().length();
		fire(editor, RichTextEditorEvents.BLUR, "<p>Short</p>");
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you left the article, with its text changed: " + before +
				" characters before, 12 now.");
		assertEquals("<p>Short</p>", editor.getModelObject());
	}

	@Test
	void hearsOfTheSizeTheUserGaveTheArticle()
	{
		RichTextEditor editor = editor(EDITOR);
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"width\":null,\"height\":320.4}");
		fire(editor, RichTextEditorEvents.RESIZE, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you resized the article to auto × 320 px.");
	}

	@Test
	void hearsOfTheTextSelectedAndOfTheHeadings()
	{
		RichTextEditor editor = editor(EDITOR);
		tester.assertLabel(inCase("articleTitle", "selection"),
			"Select some of the article: the server hears of the text you selected.");
		tester.assertLabel(inCase("articleTitle", "outline"),
			"Change the headings of the article: the server hears of its outline.");
		// the built-in type waits for the user to finish selecting; the page's own has a script
		tester.assertContains("\\{id:\"antilia-rich-text-editor:selected-text\",index:\\d+," +
			"kind:'ajax',delay:500\\}");
		tester.assertContains("\\{id:\"rich-text-demo:outline\",index:\\d+,kind:'ajax'," +
			"delay:-1\\}");
		tester.assertContains("OutlineEvent/outline-event");

		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			new JSONObject().put("text", "Hello\n  world").toString());
		fire(editor, RichTextEditorEvents.SELECTED_TEXT, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "selection"),
			"The server heard you selected “Hello world”.");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			new JSONObject().put("text", "a".repeat(70)).toString());
		fire(editor, RichTextEditorEvents.SELECTED_TEXT, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "selection"),
			"The server heard you selected “" + "a".repeat(60) + "…”.");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER, "{\"text\":\"\"}");
		fire(editor, RichTextEditorEvents.SELECTED_TEXT, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "selection"),
			"The server heard nothing is selected.");

		// the headings the browser sent: texts alone, each cut short
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"headings\":[\"Intro\",7,\" \",\" Usage \",\"" + "b".repeat(300) + "\"]}");
		fire(editor, OutlineEvent.NAME, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "outline"),
			"The outline the server heard: Intro · Usage · " + "b".repeat(OutlineEvent.MAX_LENGTH) +
				".");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"headings\":\"none\"}");
		fire(editor, OutlineEvent.NAME, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "outline"),
			"The server heard the article has no headings.");
	}

	@Test
	void marksTheTitleWithTheChangesNotSavedInTheBrowser()
	{
		editor(EDITOR);
		tester.assertContains("\\{id:\"antilia-rich-text-editor:modified\",index:\\d+," +
			"kind:'client',delay:-1,fn:function\\(event\\)\\{var title=document.title");
	}

	@Test
	void switchesTheOptions()
	{
		FormTester form = tester.newFormTester(inCase("articleTitle", "controls"));
		form.select("toolbar", 1);
		tester.executeAjaxEvent(inCase("articleTitle", "controls:toolbar"), "change");
		assertEquals(RichTextEditorPage.COMPACT, editor(EDITOR).getToolbar());
		tester.assertContains("\"toolbar\":\\[\\[\"heading\"\\],\\[\"bold\",\"italic\",\"link\"\\]");

		form = tester.newFormTester(inCase("articleTitle", "controls"));
		form.select("toolbar", 2);
		tester.executeAjaxEvent(inCase("articleTitle", "controls:toolbar"), "change");
		tester.assertContains("\"toolbar\":false");

		for (String option : List.of("bubbleMenu", "readOnly", "count"))
		{
			form = tester.newFormTester(inCase("articleTitle", "controls"));
			form.setValue(option, "readOnly".equals(option));
			tester.executeAjaxEvent(inCase("articleTitle", "controls:" + option), "change");
		}
		RichTextEditor editor = editor(EDITOR);
		assertFalse(editor.isBubbleMenu());
		assertTrue(editor.isReadOnly());
		assertFalse(editor.isCount());
		tester.assertContains("\"readOnly\":true");
		assertEquals(List.of(), RichTextEditorPage.toolbarOf("none"));
		assertEquals(RichTextTool.DEFAULT_TOOLBAR, RichTextEditorPage.toolbarOf(null));
	}

	@Test
	void keepsTheNotesAsMarkdown()
	{
		RichTextEditor notes = editor(NOTES);
		assertEquals(RichTextFormat.MARKDOWN, notes.getFormat());
		assertTrue(notes.isBubbleMenu());
		tester.assertContains("\"format\":\"markdown\"");
		assertEquals(RichTextSamples.NOTES, tester.getComponentFromLastRenderedPage(
			inCase("notesTitle", "kept:markdown")).getDefaultModelObject());

		fire(notes, RichTextEditorEvents.INPUT, "# Packing\n\nOnly <b>this</b>");
		assertEquals("# Packing\n\nOnly <b>this</b>", RichTextEditorPage.notes().getText(),
			"Markdown is text: the page escapes it");
		tester.assertContains("# Packing\n\nOnly &lt;b&gt;this&lt;/b&gt;");

		fire(notes, RichTextEditorEvents.SAVE, "# Packing\n\nDone");
		assertFalse(RichTextEditorPage.notes().isModified());
		tester.assertLabel(inCase("notesTitle", "notesForm:notesStatus"), "Saved the notes.");

		FormTester form = tester.newFormTester(inCase("notesTitle", "notesForm"));
		form.setValue("editor", "# Again");
		tester.executeAjaxEvent(inCase("notesTitle", "notesForm:save"), "click");
		assertEquals("# Again", RichTextEditorPage.notes().getSaved());
	}

	@Test
	void checksTheWritingAndSuggestsFromTheServer()
	{
		String writing = inCase("writingTitle", "writing");
		RichTextEditor editor = editor(writing + ":writingForm:editor");
		assertTrue(editor.isProofreading());
		assertTrue(editor.isSuggesting());
		assertEquals(List.of("people", "topics", "terms"), editor.getSuggesters().stream()
			.map(IRichTextSuggester::getName).toList());
		assertNull(editor.getSpellcheck());
		assertNull(editor.getTextLanguage());
		tester.assertContains("\"proofreadUrl\"");
		tester.assertContains("\"suggesters\":\\[\\{\"name\":\"people\",\"insert\":\"mention\"," +
			"\"char\":\"@\"}");
		tester.assertLabel(writing + ":writingKept:mentions",
			"Mentions the server reads: @Ada Lovelace (people: ada)");

		// the demo's proofreader, through the server
		tester.getRequest().setMethod("POST");
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextProofreadBehavior.TEXTS_PARAMETER, "[\"Teh word word,  ok\"]");
		tester.executeBehavior(editor.getProofreadBehavior());
		assertEquals("{\"issues\":[[{\"from\":0,\"to\":3,\"kind\":\"spelling\"," +
			"\"message\":\"This word may be spelled wrong.\",\"replacements\":[\"the\"]," +
			"\"rule\":\"DEMO_SPELLING\"},{\"from\":4,\"to\":13,\"kind\":\"grammar\"," +
			"\"message\":\"A word said twice.\",\"replacements\":[\"word\"]," +
			"\"rule\":\"DEMO_REPEATED\"},{\"from\":14,\"to\":16,\"kind\":\"style\"," +
			"\"message\":\"Two blanks between words.\",\"replacements\":[\" \"]," +
			"\"rule\":\"DEMO_BLANKS\"}]]}", tester.getLastResponseAsString());

		// the people, the topics and the terms
		assertEquals("[{\"id\":\"ada\",\"label\":\"Ada Lovelace\",\"detail\":" +
			"\"ada@example.org\"}]", suggest(editor, "people", "lov"));
		assertEquals("[{\"id\":\"tiptap\",\"label\":\"tiptap\"}]", suggest(editor, "topics",
			"ti"));
		assertEquals("[{\"id\":\"9\",\"label\":\"Tiptap\",\"text\":\"Tiptap\"}," +
			"{\"id\":\"10\",\"label\":\"TypeScript\",\"text\":\"TypeScript\"}]",
			suggest(editor, "terms", "t"));

		RichTextWritingPanel panel = (RichTextWritingPanel)tester.getComponentFromLastRenderedPage(
			writing);
		panel.setSpellcheck("off");
		panel.setTextLanguage("de");
		panel.setProofreading("none");
		tester.startPage(tester.getLastRenderedPage());
		assertEquals(Boolean.FALSE, editor.getSpellcheck());
		assertEquals(Locale.GERMAN, editor.getTextLanguage());
		assertFalse(editor.isProofreading());
		tester.assertContains("\"textLanguage\":\"de\"");
	}

	private String suggest(RichTextEditor editor, String name, String query)
	{
		tester.getRequest().setMethod("POST");
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextSuggestBehavior.NAME_PARAMETER, name);
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextSuggestBehavior.QUERY_PARAMETER, query);
		tester.executeBehavior(editor.getSuggestBehavior());
		return new JSONObject(tester.getLastResponseAsString()).getJSONArray("items").toString();
	}

	@Test
	void choosesTheToolsOfTheToolbarOneByOne()
	{
		String controls = inCase("articleTitle", "controls");
		String java = inCase("articleTitle", "toolbarJava:java");
		tester.assertInvisible(controls + ":chooser");
		assertEquals("editor.setToolbar(RichTextTool.DEFAULT_TOOLBAR);",
			tester.getComponentFromLastRenderedPage(java).getDefaultModelObjectAsString());

		FormTester form = tester.newFormTester(controls);
		form.select("toolbar", 1);
		tester.executeAjaxEvent(controls + ":toolbar", "change");
		assertEquals("import static com.antiliacode.widgets.richtext.RichTextTool.*;\n\n" +
			"editor.setToolbar(List.of(\n\tList.of(HEADING),\n\tList.of(BOLD, ITALIC, LINK),\n" +
			"\tList.of(BULLET_LIST, ORDERED_LIST),\n\tList.of(UNDO, REDO)));",
			tester.getComponentFromLastRenderedPage(java).getDefaultModelObjectAsString());

		// choosing starts from the tools of the toolbar shown
		form = tester.newFormTester(controls);
		form.select("toolbar", 3);
		tester.executeAjaxEvent(controls + ":toolbar", "change");
		tester.assertVisible(controls + ":chooser");
		tester.assertComponentOnAjaxResponse(controls + ":chooser");
		RichTextEditorPage page = (RichTextEditorPage)tester.getLastRenderedPage();
		assertEquals(RichTextEditorPage.COMPACT.stream().flatMap(List::stream).toList(),
			page.getTools());
		tester.assertContains("<label><input name=\"[^\"]*\" type=\"checkbox\" " +
			"checked=\"checked\" value=\"bold\"[^>]*> Bold</label>");

		form = tester.newFormTester(controls);
		form.selectMultiple("chooser:tools", new int[] { 1, 6, 7, 23 }, true);
		tester.executeAjaxEvent(controls + ":chooser:tools", "change");
		assertEquals(List.of(List.of(RichTextTool.BOLD), List.of(RichTextTool.TEXT_COLOR,
			RichTextTool.TEXT_BACKGROUND), List.of(RichTextTool.REDO)), editor(EDITOR).getToolbar());
		tester.assertComponentOnAjaxResponse(java.replace(":java", ""));
		tester.assertContains("\"toolbar\":\\[\\[\"bold\"\\],\\[\"textColor\"," +
			"\"textBackground\"\\],\\[\"redo\"\\]\\]");
		assertEquals("editor.setToolbar(List.of());", RichTextEditorPage.javaOf(List.of()));
		assertEquals(List.of(), RichTextEditorPage.toolbarOf(RichTextEditorPage.CHOSEN, List.of()));
	}

	@Test
	void tellsTheLibraryTheEditorIsBasedOn()
	{
		RichTextLibrary library = RichTextLibrary.get();
		tester.assertContains("Based on <a[^>]*href=\"https://tiptap.dev\"[^>]*>Tiptap " +
			Pattern.quote(library.version()) + "</a>, which runs on ProseMirror.");

		tester.getSession().setLocale(Locale.of("de"));
		tester.startPage(RichTextEditorPage.class);
		tester.assertContains("Basiert auf <a[^>]*>Tiptap [^<]*</a>, das auf ProseMirror aufbaut.");
	}

	@Test
	void cleansHtmlFromAnywhere()
	{
		assertTrue(tester.getLastResponseAsString().contains(
			"&lt;script&gt;alert("), "shown as text");
		FormTester form = tester.newFormTester(inCase("cleaningTitle", "cleaningForm"));
		form.setValue("unsafe", "<p onclick=\"x()\">Clean <b>me</b></p><script>x()</script>");
		tester.executeAjaxEvent(inCase("cleaningTitle", "cleaningForm:clean"), "click");

		assertEquals("<p>Clean <b>me</b></p>", tester.getComponentFromLastRenderedPage(
			inCase("cleaningTitle", "cleaned:html")).getDefaultModelObject());
		tester.assertContains("antilia-rich-text-view\"[^>]*><p>Clean <b>me</b></p>");
	}

	@Test
	void givesTheAppsTheirConfiguration()
	{
		RichTextEditorPage page = (RichTextEditorPage)tester.getLastRenderedPage();
		JSONObject config = page.getFrameworkConfig();
		assertEquals(RichTextSamples.ARTICLE, config.getString("article"));
		assertEquals(RichTextSamples.NOTES, config.getString("notes"));
		assertEquals("An article in HTML", config.getJSONObject("texts").getString(
			"appArticleTitle"));
		assertEquals("Compact", config.getJSONObject("texts").getString("toolbar.compact"));
		assertEquals("Bold", config.getJSONObject("editor").getString("bold"));
		assertEquals("[\"full\",\"compact\",\"none\"]", config.getJSONArray("toolbars").toString());
		assertEquals("richTextEditor", page.getFrameworkDemo());
		assertEquals("Highlight the code", config.getJSONObject("texts").getString("highlight"));
		assertTrue(config.getJSONObject("texts").getString("extras").endsWith(", 2 MB at most."));
		// the apps upload to the page
		RichTextImageUploadBehavior uploads = page.getBehaviors(RichTextImageUploadBehavior.class)
			.get(0);
		assertEquals(uploads.getUploadUrl(), config.getString("uploadUrl"));
		assertEquals("[\"image/png\",\"image/jpeg\",\"image/gif\",\"image/webp\"]",
			config.getJSONArray("uploadTypes").toString());
		assertEquals(RichTextImages.MAX_SIZE.bytes(), config.getLong("uploadMaxSize"));
	}

	@Test
	void highlightsTheCodeBlocksUnlessToldNot()
	{
		RichTextEditor editor = editor(EDITOR);
		assertTrue(editor.isHighlighting());
		assertTrue(editor(NOTES).isHighlighting());
		tester.assertContains("antilia-rich-text-highlight.*\\.js");
		tester.assertContains("antilia-code-editor.*\\.css");
		tester.assertContains("\"highlight\":true");
		tester.assertContains("2 MB at most");

		FormTester form = tester.newFormTester(inCase("articleTitle", "controls"));
		form.setValue("highlight", false);
		tester.executeAjaxEvent(inCase("articleTitle", "controls:highlight"), "change");
		assertFalse(editor(EDITOR).isHighlighting());
		assertFalse(tester.getLastResponseAsString().contains("\"highlight\":true"));
	}

	@Test
	void uploadsImagesWhichTheApplicationServes() throws IOException
	{
		RichTextEditor editor = editor(EDITOR);
		assertTrue(editor.isImageUploading());
		assertEquals(RichTextImages.MAX_SIZE, editor.getImageMaxSize());
		assertTrue(editor(NOTES).isImageUploading());
		tester.assertContains("\"uploadUrl\":");

		byte[] png = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 13, 'I', 'H',
				'D', 'R' };
		Path file = Files.createTempFile("bird", ".png");
		try
		{
			Files.write(file, png);
			tester.getRequest().setUseMultiPartContentType(true);
			tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
				new org.apache.wicket.util.file.File(file.toFile()), "image/png");
			tester.executeBehavior(editor.getBehaviors(RichTextImageUploadBehavior.class).get(0));
		}
		finally
		{
			Files.deleteIfExists(file);
		}
		String url = new JSONObject(tester.getLastResponseAsString()).getString("url");
		assertTrue(url.matches(".*/rich-text-images/[0-9a-f]{32}\\.png"), url);

		tester.executeUrl(url.substring(url.indexOf("rich-text-images/")));
		assertEquals("image/png", tester.getLastResponse().getContentType());
		assertEquals("nosniff", tester.getLastResponse().getHeader("X-Content-Type-Options"));
		assertArrayEquals(png, tester.getLastResponse().getBinaryContent());

		tester.executeUrl("rich-text-images/../../etc/passwd");
		assertEquals(404, tester.getLastResponse().getStatus());
		tester.executeUrl("rich-text-images/" + "0".repeat(32) + ".png");
		assertEquals(404, tester.getLastResponse().getStatus());
	}

	@Test
	void speaksTheLanguageOfTheUser()
	{
		tester.getSession().setLocale(Locale.of("de"));
		tester.startPage(RichTextEditorPage.class);
		tester.assertContains("Ein Artikel in HTML, auf dem Server bereinigt");
		tester.assertContains("\"bold\":\"Fett\"");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(RichTextEditorPage.class);
		tester.assertContains("Notas en Markdown");
		tester.assertContains("\"bold\":\"Negrita\"");
	}
}
