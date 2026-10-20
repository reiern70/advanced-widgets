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
package com.antilia.widgets.demo.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.richtext.RichTextEditor;
import com.antilia.widgets.richtext.RichTextEditorEvent;
import com.antilia.widgets.richtext.RichTextEditorEventBehavior;
import com.antilia.widgets.richtext.RichTextFormat;
import com.antilia.widgets.richtext.RichTextTool;
import com.github.openjson.JSONObject;

/**
 * Checks {@link RichTextEditorPage}: the article whose HTML and state the session keeps, cleaned,
 * the options the form switches, saving, the notes in Markdown, the cleaning of HTML from anywhere
 * and the configuration of the React and Svelte apps.
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
			.filter(behavior -> !(behavior instanceof RichTextEditorEventBehavior)).findFirst()
			.orElseThrow());
	}

	/**
	 * Fires an event of the editor with its text, as the browser does.
	 */
	private void fire(RichTextEditor editor, RichTextEditorEvent event, String text)
	{
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		tester.executeBehavior(editor.getBehaviors(RichTextEditorEventBehavior.class).stream()
			.filter(each -> each.getRichTextEditorEvent() == event).findFirst().orElseThrow());
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
		fire(editor, RichTextEditorEvent.INPUT, html);

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
		fire(editor, RichTextEditorEvent.SAVE, "<p>Saved <em>now</em></p>");

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
		fire(editor, RichTextEditorEvent.FOCUS, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you entered the article.");
		fire(editor, RichTextEditorEvent.BLUR, RichTextSamples.ARTICLE);
		tester.assertLabel(inCase("articleTitle", "editorForm:heard"),
			"The server heard you left the article, with its text.");
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

		fire(notes, RichTextEditorEvent.INPUT, "# Packing\n\nOnly <b>this</b>");
		assertEquals("# Packing\n\nOnly <b>this</b>", RichTextEditorPage.notes().getText(),
			"Markdown is text: the page escapes it");
		tester.assertContains("# Packing\n\nOnly &lt;b&gt;this&lt;/b&gt;");

		fire(notes, RichTextEditorEvent.SAVE, "# Packing\n\nDone");
		assertFalse(RichTextEditorPage.notes().isModified());
		tester.assertLabel(inCase("notesTitle", "notesForm:notesStatus"), "Saved the notes.");

		FormTester form = tester.newFormTester(inCase("notesTitle", "notesForm"));
		form.setValue("editor", "# Again");
		tester.executeAjaxEvent(inCase("notesTitle", "notesForm:save"), "click");
		assertEquals("# Again", RichTextEditorPage.notes().getSaved());
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
