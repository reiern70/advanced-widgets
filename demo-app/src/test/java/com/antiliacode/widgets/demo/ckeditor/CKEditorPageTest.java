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
package com.antiliacode.widgets.demo.ckeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.ckeditor.CKEditor;
import com.antiliacode.widgets.ckeditor.CKEditorLibrary;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.richtext.OutlineEvent;
import com.antiliacode.widgets.event.ClientEventsBehavior;
import com.antiliacode.widgets.event.IAjaxEvent;
import com.antiliacode.widgets.event.IAjaxSubmitEvent;
import com.antiliacode.widgets.event.IClientEvent;
import com.antiliacode.widgets.richtext.RichTextEditorEvents;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.github.openjson.JSONObject;

/**
 * Checks {@link CKEditorPage}: CKEditor as the demo brings it, with the license key of the
 * application, the article whose HTML and state the session keeps, cleaned, the options the form
 * switches, saving, the notes in Markdown, the cleaning of HTML from anywhere and the
 * configuration of the React and Svelte apps.
 */
class CKEditorPageTest
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
		tester.startPage(CKEditorPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(CKEditorAssets.LICENSE_KEY_PROPERTY);
	}

	private CKEditor editor(String path)
	{
		return (CKEditor)tester.getComponentFromLastRenderedPage(path);
	}

	/**
	 * Sends the editor's state, and the text, as the browser does.
	 */
	private void sendState(CKEditor editor, String state, String text)
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
	private void fire(CKEditor editor, String event, String text)
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
	void bringsCKEditorWithTheLicenseKeyOfTheApplication()
	{
		CKEditorLibrary library = CKEditorLibrary.get();
		assertTrue(library.isGpl());
		assertNotNull(library.getContentCss());
		assertEquals("de", library.getLanguage(Locale.GERMANY));
		tester.assertContains("ckeditor5/ckeditor5.umd.*\\.js");
		tester.assertContains("ckeditor5/ckeditor5.*\\.css");
		tester.assertContains("antilia-ckeditor.*\\.js");
		tester.assertContains("Wicket.CKEditor.attach");
		tester.assertContains("\"licenseKey\":\"GPL\"");
		// which CKEditor, and under which license
		CKEditorAssets.Release release = CKEditorAssets.release();
		assertEquals("CKEditor 5", release.name());
		assertTrue(release.version().matches("\\d+\\.\\d+\\.\\d+"), release.version());
		tester.assertContains("CKEditor 5 " + release.version() + "</a> runs here under the GPL 2");

		System.setProperty(CKEditorAssets.LICENSE_KEY_PROPERTY, "a-key-of-cksource");
		assertEquals("a-key-of-cksource", CKEditorAssets.library().getLicenseKey());
		assertFalse(CKEditorAssets.library().isGpl());
	}

	@Test
	void showsTheArticleInAnEditorAndAsAPageShowsIt()
	{
		CKEditor editor = editor(EDITOR);
		assertEquals(RichTextFormat.HTML, editor.getFormat());
		assertTrue(editor.isCount());
		assertFalse(editor.isReadOnly());
		tester.assertContains("\"placeholder\":\"Write the article…\"");
		tester.assertContains("\"label\":\"Article\"");
		tester.assertContains("antilia-ck-editor-loading");
		// the article as a page shows it, in CKEditor's content style
		tester.assertContains("antilia-ck-editor-view ck-content");
		tester.assertContains("ckeditor5/ckeditor5-content.*\\.css");
	}

	@Test
	void keepsTheHtmlCleanedAndTheStateTheBrowserSendsInTheSession()
	{
		CKEditor editor = editor(EDITOR);
		String html = "<p onclick=\"steal()\" style=\"color:red\">New <strong>text</strong></p>" +
			"<ul><li data-list-item-id=\"e1\">one</li></ul><script>steal()</script>";
		sendState(editor, "{\"selection\":{\"anchor\":3,\"head\":7},\"scrollTop\":40," +
			"\"height\":420}", html);
		fire(editor, RichTextEditorEvents.INPUT, html);

		CKEditorDocument article = CKEditorPage.article();
		assertEquals("<p>New <strong>text</strong></p><ul><li>one</li></ul>", article.getText());
		assertTrue(article.isModified());
		assertEquals(7, article.getState().getHead());
		assertEquals(Integer.valueOf(420), article.getState().getSize().getHeight());
		tester.assertContains("selected from 3 to 7; auto × 420 px; changes not saved");

		// a page made anew shows them again, from the session
		tester.startPage(CKEditorPage.class);
		tester.assertContains("\"state\":\\{\"selection\":\\{\"anchor\":3,\"head\":7\\}," +
			"\"scrollTop\":40,\"scrollLeft\":0,\"height\":420\\}");
		tester.assertContains("\"saved\":\"\\\\u003ch2\\\\u003eField notes from the coast");
	}

	@Test
	void savesWithCtrlSAndTheButton()
	{
		CKEditor editor = editor(EDITOR);
		fire(editor, RichTextEditorEvents.SAVE, "<p>Saved <i>now</i></p>");

		CKEditorDocument article = CKEditorPage.article();
		assertEquals("<p>Saved <i>now</i></p>", article.getSaved());
		assertFalse(article.isModified());
		tester.assertContains("e.markSaved\\(\"\\\\u003cp\\\\u003eSaved \\\\u003ci\\\\u003enow" +
			"\\\\u003c\\\\/i\\\\u003e\\\\u003c\\\\/p\\\\u003e\"\\)");
		tester.assertContains("Saved the article.");
		tester.assertContains("antilia-ck-editor-view ck-content\"[^>]*><p>Saved <i>now</i></p>");

		FormTester form = tester.newFormTester(inCase("articleTitle", "editorForm"));
		form.setValue("editor", "<p>Again</p>");
		tester.executeAjaxEvent(inCase("articleTitle", "editorForm:save"), "click");
		assertEquals("<p>Again</p>", article.getSaved());
	}

	@Test
	void hearsOfTheFocus()
	{
		CKEditor editor = editor(EDITOR);
		fire(editor, RichTextEditorEvents.FOCUS, CKEditorSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you entered the article.");
		// the text the server has: unchanged
		fire(editor, RichTextEditorEvents.BLUR, editor.getModelObject());
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you left the article, with its text.");
		assertFalse(CKEditorPage.article().isModified(), "the sample is as CKEditor writes it");
		// another text, which the model gets
		int before = editor.getModelObject().length();
		fire(editor, RichTextEditorEvents.BLUR, "<p>Short</p>");
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you left the article, with its text changed: " + before +
				" characters before, 12 now.");
		assertEquals("<p>Short</p>", editor.getModelObject());
	}

	@Test
	void hearsOfTheTextSelectedAndOfTheHeadingsAsTheTiptapEditorDoes()
	{
		CKEditor editor = editor(EDITOR);
		tester.assertContains("OutlineEvent/outline-event");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"text\":\"Hello\"}");
		fire(editor, RichTextEditorEvents.SELECTED_TEXT, editor.getModelObject());
		tester.assertLabel(inCase("articleTitle", "selection"),
			"The server heard you selected “Hello”.");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"headings\":[\"One\",\"Two\"]}");
		fire(editor, OutlineEvent.NAME, editor.getModelObject());
		tester.assertLabel(inCase("articleTitle", "outline"),
			"The outline the server heard: One · Two.");
	}

	@Test
	void hearsOfTheSizeTheUserGaveTheArticle()
	{
		CKEditor editor = editor(EDITOR);
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			"{\"width\":null,\"height\":320.4}");
		fire(editor, RichTextEditorEvents.RESIZE, CKEditorSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you resized the article to auto × 320 px.");
	}

	@Test
	void switchesTheOptions()
	{
		for (String option : new String[] { "readOnly", "count" })
		{
			FormTester form = tester.newFormTester(inCase("articleTitle", "controls"));
			form.setValue(option, "readOnly".equals(option));
			tester.executeAjaxEvent(inCase("articleTitle", "controls:" + option), "change");
		}
		CKEditor editor = editor(EDITOR);
		assertTrue(editor.isReadOnly());
		assertFalse(editor.isCount());
		tester.assertContains("\"readOnly\":true");
	}

	@Test
	void keepsTheNotesAsMarkdown()
	{
		CKEditor notes = editor(NOTES);
		assertEquals(RichTextFormat.MARKDOWN, notes.getFormat());
		tester.assertContains("\"format\":\"markdown\"");
		assertEquals(CKEditorSamples.NOTES, tester.getComponentFromLastRenderedPage(
			inCase("notesTitle", "kept:markdown")).getDefaultModelObject());

		fire(notes, RichTextEditorEvents.INPUT, "# Packing\n\nOnly <b>this</b>");
		assertEquals("# Packing\n\nOnly <b>this</b>", CKEditorPage.notes().getText(),
			"Markdown is text: the page escapes it");
		tester.assertContains("# Packing\n\nOnly &lt;b&gt;this&lt;/b&gt;");

		fire(notes, RichTextEditorEvents.SAVE, "# Packing\n\nDone");
		assertFalse(CKEditorPage.notes().isModified());
		tester.assertLabel(inCase("notesTitle", "notesForm:notesStatus"), "Saved the notes.");
	}

	@Test
	void cleansHtmlFromAnywhere()
	{
		assertTrue(tester.getLastResponseAsString().contains("&lt;script&gt;alert("),
			"shown as text");
		FormTester form = tester.newFormTester(inCase("cleaningTitle", "cleaningForm"));
		form.setValue("unsafe", "<p onclick=\"x()\">Clean <b>me</b></p><figure class=\"image\">" +
			"<img src=\"a.png\" width=\"9\" style=\"x\" alt=\"a\"></figure><script>x()</script>");
		tester.executeAjaxEvent(inCase("cleaningTitle", "cleaningForm:clean"), "click");

		assertEquals("<p>Clean <b>me</b></p><figure class=\"image\"><img src=\"a.png\" alt=\"a\" />" +
			"</figure>", tester.getComponentFromLastRenderedPage(inCase("cleaningTitle",
				"cleaned:html")).getDefaultModelObject());
	}

	@Test
	void givesTheAppsTheirConfiguration()
	{
		CKEditorPage page = (CKEditorPage)tester.getLastRenderedPage();
		JSONObject config = page.getFrameworkConfig();
		assertEquals(CKEditorSamples.ARTICLE, config.getString("article"));
		assertEquals(CKEditorSamples.NOTES, config.getString("notes"));
		assertEquals("GPL", config.getString("licenseKey"));
		assertFalse(config.has("language"), "English: no translation");
		assertEquals("Open in a new tab", config.getJSONObject("editor").getString("linkNewTab"));
		assertEquals("Article", config.getJSONObject("texts").getString("articleLabel"));

		tester.getSession().setLocale(Locale.of("de"));
		tester.startPage(CKEditorPage.class);
		page = (CKEditorPage)tester.getLastRenderedPage();
		assertEquals("de", page.getFrameworkConfig().getString("language"));
	}

	@Test
	void speaksTheLanguageOfTheUser()
	{
		tester.getSession().setLocale(Locale.of("de"));
		tester.startPage(CKEditorPage.class);
		tester.assertContains("Ein Artikel in HTML, auf dem Server bereinigt");
		tester.assertContains("läuft hier unter der GPL 2");
		tester.assertContains("ckeditor5/translations/de.umd.*\\.js");
		tester.assertContains("\"language\":\"de\"");

		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(CKEditorPage.class);
		tester.assertContains("Notas en Markdown");
		tester.assertContains("ckeditor5/translations/es.umd.*\\.js");
	}
}
