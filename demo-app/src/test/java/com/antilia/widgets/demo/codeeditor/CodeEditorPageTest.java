/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.codeeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.codeeditor.CodeEditor;
import com.antilia.widgets.codeeditor.CodeEditorEvent;
import com.antilia.widgets.codeeditor.CodeEditorEventBehavior;
import com.antilia.widgets.codeeditor.CodeEditorLanguage;
import com.antilia.widgets.codeeditor.CodeEditorState;
import com.antilia.widgets.codeeditor.MarkdownPreview;
import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.resizable.ResizableBehavior;
import com.antilia.widgets.resizable.ResizeAxis;
import com.antilia.widgets.split.SplitPane;
import com.antilia.widgets.split.SplitPanel;
import com.antilia.widgets.split.SplitSize;
import com.antilia.widgets.tabs.Tab;
import com.antilia.widgets.tabs.Tabs;
import com.github.openjson.JSONObject;

/**
 * Checks {@link CodeEditorPage}: the samples whose texts and states the session keeps, the options
 * the form switches, saving, the files in tabs, and the configuration of the React and Svelte apps.
 */
class CodeEditorPageTest
{
	private static final String EDITOR = inCase("editorTitle", "editorForm:editor");

	private static final String FILES = inCase("filesTitle", "files");

	private static final String NOTES = inCase("notesTitle", "notesForm");

	private static final String SPLIT = NOTES + ":notesSplit";

	private static final String TOGGLE = NOTES + ":togglePreview";

	private WicketTester tester;

	private CodeEditorPage page;

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
		page = tester.startPage(CodeEditorPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private CodeEditor editor()
	{
		return (CodeEditor)tester.getComponentFromLastRenderedPage(EDITOR);
	}

	/**
	 * Sends the editor's state, and the text, as the browser does.
	 */
	private void sendState(CodeEditor editor, String state, String text)
	{
		tester.getRequest().setParameter("state", state);
		if (text != null)
		{
			tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		}
		tester.executeBehavior(editor.getBehaviors(AbstractDefaultAjaxBehavior.class).stream()
			.filter(behavior -> !(behavior instanceof CodeEditorEventBehavior)).findFirst()
			.orElseThrow());
	}

	private CodeEditor notesEditor()
	{
		return (CodeEditor)tester.getComponentFromLastRenderedPage(SPLIT +
			":split:firstPane:first:editor");
	}

	private MarkdownPreview preview()
	{
		return (MarkdownPreview)tester.getComponentFromLastRenderedPage(SPLIT +
			":split:second:preview");
	}

	/**
	 * @return the button hiding and showing the preview, as rendered
	 */
	private TagTester toggle()
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			tester.getComponentFromLastRenderedPage(TOGGLE).getMarkupId());
	}

	/**
	 * Sends where the user put the divider of the notes and which pane they collapsed, as the
	 * browser does.
	 */
	private void sendSplit(String position, String collapsed)
	{
		Component split = tester.getComponentFromLastRenderedPage(SPLIT + ":split");
		tester.getRequest().setParameter("position", position);
		tester.getRequest().setParameter("collapsed", collapsed);
		tester.executeBehavior(split.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private CodeEditorEventBehavior behavior(CodeEditor editor, CodeEditorEvent event)
	{
		for (Behavior behavior : editor.getBehaviors())
		{
			if (behavior instanceof CodeEditorEventBehavior heard &&
				heard.getCodeEditorEvent() == event)
			{
				return heard;
			}
		}
		throw new AssertionError("no behavior for " + event);
	}

	@Test
	void showsTheJavaSampleInAnEditorKeepingItsState()
	{
		CodeEditor editor = editor();
		assertEquals(CodeEditorLanguage.JAVA, editor.getLanguage());
		assertEquals(ResizeAxis.VERTICAL, editor.getResizable());
		assertTrue(editor.isLineNumbers());
		assertTrue(editor.isSendToServer());
		tester.assertContains("Wicket.CodeEditor.attach");
		tester.assertContains("public record Order");
		tester.assertContains("antilia-code-editor.*\\.js");
		tester.assertLabel(inCase("editorTitle", "known"), "The server knows: Order.java, 31 " +
			"lines; the cursor on line 1, column 1; 0 characters selected; the size the page " +
			"gives it; all saved.");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void keepsTheTextAndTheStateTheBrowserSendsInTheSession()
	{
		CodeEditor editor = editor();
		String text = CodeEditorPage.samples().get("java").getText().replace("record Order",
			"record Purchase");
		sendState(editor, "{\"selection\":{\"ranges\":[{\"anchor\":120,\"head\":124}]," +
			"\"main\":0},\"scrollTop\":40,\"height\":420}", text);
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		tester.executeBehavior(behavior(editor, CodeEditorEvent.INPUT));

		CodeDocument java = CodeEditorPage.samples().get("java");
		assertEquals(text, java.getText());
		assertTrue(java.isModified());
		assertEquals(124, java.getState().getCursor());
		assertEquals(Integer.valueOf(420), java.getState().getSize().getHeight());
		tester.assertContains("the cursor on line 7, column 41; 4 characters selected; auto × " +
			"420 px; changes not saved");

		// a page made anew shows them again, from the session
		tester.startPage(CodeEditorPage.class);
		tester.assertContains("public record Purchase");
		tester.assertContains("\"state\":\\{\"selection\":\\{\"ranges\":\\[\\{\"anchor\":120," +
			"\"head\":124\\}\\],\"main\":0\\},\"scrollTop\":40,\"scrollLeft\":0,\"height\":420\\}");
		tester.assertContains("\"saved\":\"package com.example.shop;");
	}

	@Test
	void savesWithCtrlSAndTheButton()
	{
		CodeEditor editor = editor();
		String text = "class Saved {}\n";
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		tester.executeBehavior(behavior(editor, CodeEditorEvent.SAVE));

		CodeDocument java = CodeEditorPage.samples().get("java");
		assertEquals(text, java.getText());
		assertFalse(java.isModified());
		tester.assertContains("e.markSaved\\(\"class Saved \\{}\\\\n\"\\)");
		tester.assertContains("Saved Order.java.");

		FormTester form = tester.newFormTester(inCase("editorTitle", "editorForm"));
		form.setValue("editor", "class Again {}\n");
		tester.executeAjaxEvent(inCase("editorTitle", "editorForm:save"), "click");
		assertEquals("class Again {}\n", java.getSaved());
	}

	@Test
	void hearsOfTheFocus()
	{
		CodeEditor editor = editor();
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			CodeEditorPage.samples().get("java").getText());
		tester.executeBehavior(behavior(editor, CodeEditorEvent.FOCUS));
		tester.assertLabel(inCase("editorTitle", "editorForm:heard"),
			"The server heard you entered Order.java.");

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			CodeEditorPage.samples().get("java").getText());
		tester.executeBehavior(behavior(editor, CodeEditorEvent.BLUR));
		tester.assertLabel(inCase("editorTitle", "editorForm:heard"),
			"The server heard you left Order.java, with its text.");
	}

	@Test
	void switchesTheLanguageAndTheOptions()
	{
		FormTester form = tester.newFormTester(inCase("editorTitle", "controls"));
		form.select("language", 4);
		tester.executeAjaxEvent(inCase("editorTitle", "controls:language"), "change");
		assertEquals(CodeEditorLanguage.MARKDOWN, page.getLanguage());
		assertEquals(CodeEditorLanguage.MARKDOWN, editor().getLanguage());
		tester.assertContains("# Shop front");
		tester.assertContains("README.md");

		form = tester.newFormTester(inCase("editorTitle", "controls"));
		form.setValue("lineNumbers", false);
		tester.executeAjaxEvent(inCase("editorTitle", "controls:lineNumbers"), "change");
		form = tester.newFormTester(inCase("editorTitle", "controls"));
		form.setValue("readOnly", true);
		tester.executeAjaxEvent(inCase("editorTitle", "controls:readOnly"), "change");
		form = tester.newFormTester(inCase("editorTitle", "controls"));
		form.select("resize", 3);
		tester.executeAjaxEvent(inCase("editorTitle", "controls:resize"), "change");
		assertFalse(editor().isLineNumbers());
		assertTrue(editor().isReadOnly());
		assertEquals(null, editor().getResizable());
		tester.assertContains("\"lineNumbers\":false");
		tester.assertContains("\"readOnly\":true");
		tester.assertContains("\"resizable\":false");

		// the grammar of the log is the page's own, a script after the editor's
		form = tester.newFormTester(inCase("editorTitle", "controls"));
		form.select("language", 5);
		tester.executeAjaxEvent(inCase("editorTitle", "controls:language"), "change");
		tester.assertContains("log-grammar.*\\.js");
		tester.assertContains("\"language\":\"log\"");
	}

	@Test
	void opensTheFilesInTabsEachInAnEditor()
	{
		Tabs files = (Tabs)tester.getComponentFromLastRenderedPage(FILES);
		assertEquals(List.of("Order.java", "words.js", "package.json", "README.md"),
			page.getFiles().stream().map(CodeDocument::getName).toList());
		Tab first = files.getTabs().get(0);
		assertTrue(first.isClosable());
		assertEquals(null, first.getModified(), "the editor tells the tabs");

		CodeEditor json = (CodeEditor)tester.getComponentFromLastRenderedPage(FILES +
			":element:panels:items:" + files.getTabs().get(2).getName() + ":panel:form:editor");
		assertEquals(CodeEditorLanguage.JSON, json.getLanguage());
		tester.getRequest().getPostParameters().setParameterValue(json.getInputName(),
			"{ \"name\": \"shop\" }\n");
		tester.executeBehavior(behavior(json, CodeEditorEvent.SAVE));
		assertEquals("{ \"name\": \"shop\" }\n", page.getFiles().get(2).getSaved());
		tester.assertContains("Saved package.json.");
	}

	@Test
	void showsTheNotesBesideTheirPreview()
	{
		CodeEditor editor = notesEditor();
		assertEquals(CodeEditorLanguage.MARKDOWN, editor.getLanguage());
		assertEquals(null, editor.getResizable(), "the editor fills its pane");
		assertTrue(editor.isLineWrapping());
		assertEquals("NOTES.md", editor.getLabel().getObject());
		MarkdownPreview preview = preview();
		assertSame(editor, preview.getEditor());
		tester.assertContains("Wicket.MarkdownPreview.attach\\(\"" + preview.getMarkupId() +
			"\", \\{\"value\":\"# Release notes");
		tester.assertContains("\"editor\":\"" + editor.getMarkupId() + "\"");

		SplitPanel split = (SplitPanel)tester.getComponentFromLastRenderedPage(SPLIT);
		assertTrue(split.isCollapsible());
		assertTrue(split.isSendToServer());
		assertTrue(page.isPreviewShown());
		TagTester toggle = toggle();
		assertEquals("Hide the preview", toggle.getValue());
		assertEquals(preview.getMarkupId(), toggle.getAttribute("aria-controls"));
		tester.assertLabel(inCase("notesTitle", "notesStatus"), "Type on the left, and the " +
			"preview follows; the server keeps the notes and how they show.");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void hidesThePreviewAndShowsItAgainWithoutRenderingThePanes()
	{
		tester.clickLink(TOGGLE);
		assertFalse(page.isPreviewShown());
		assertEquals(SplitPane.SECOND, CodeEditorPage.notes().split.getCollapsed());
		tester.assertContains("split.setCollapsed\\(\"second\"\\)");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.CodeEditor.attach"),
			"the editor stays as it is in the browser");
		tester.assertContains("Show the preview");
		tester.assertContains("The preview is hidden: the editor has all the room.");

		tester.clickLink(TOGGLE);
		assertTrue(page.isPreviewShown());
		assertEquals(null, CodeEditorPage.notes().split.getCollapsed());
		tester.assertContains("split.setCollapsed\\(null\\)");
		tester.assertContains("Hide the preview");
		tester.assertContains("The editor and the preview share the room.");
	}

	@Test
	void keepsHowTheNotesShowInTheSession()
	{
		sendSplit("35", "first");
		tester.assertContains("The editor is hidden: the preview has all the room.");
		tester.assertContains("Hide the preview");
		sendSplit("35", "second");
		tester.assertContains("The preview is hidden: the editor has all the room.");
		tester.assertContains("Show the preview");
		Component split = tester.getComponentFromLastRenderedPage(SPLIT);
		tester.getRequest().setParameter("width", "");
		tester.getRequest().setParameter("height", "420");
		tester.executeBehavior(split.getBehaviors(ResizableBehavior.class).get(0));
		CodeEditor editor = notesEditor();
		sendState(editor, "{\"selection\":{\"ranges\":[{\"anchor\":2,\"head\":9}]," +
			"\"main\":0}}", "# Notes, changed\n");
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"# Notes, changed\n");
		tester.executeBehavior(behavior(editor, CodeEditorEvent.INPUT));
		CodeEditorPage.Notes notes = CodeEditorPage.notes();
		assertEquals(SplitSize.percent(35), notes.split.getPosition());
		assertEquals(SplitPane.SECOND, notes.split.getCollapsed());
		assertEquals(Integer.valueOf(420), notes.size.getHeight());
		assertEquals("# Notes, changed\n", notes.document.getText());

		// a page made anew shows them as the user left them
		page = tester.startPage(CodeEditorPage.class);
		assertFalse(page.isPreviewShown());
		tester.assertContains("\"position\":35,");
		tester.assertContains("\"collapsed\":\"second\"");
		tester.assertContains("\"axis\":\"vertical\",\"enabled\":true,\"height\":420");
		tester.assertContains("# Notes, changed");
		assertEquals("Show the preview", toggle().getValue());
	}

	@Test
	void savesTheNotesWithCtrlSAndTheButton()
	{
		CodeEditor editor = notesEditor();
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), "# Saved\n");
		tester.executeBehavior(behavior(editor, CodeEditorEvent.SAVE));
		assertEquals("# Saved\n", CodeEditorPage.notes().document.getSaved());
		tester.assertContains("Saved NOTES.md.");

		FormTester form = tester.newFormTester(NOTES);
		form.setValue("notesSplit:split:firstPane:first:editor", "# Again\n");
		tester.executeAjaxEvent(NOTES + ":save", "click");
		assertEquals("# Again\n", CodeEditorPage.notes().document.getSaved());
		assertFalse(CodeEditorPage.notes().document.isModified());
	}

	@Test
	void describesTheStateInWords()
	{
		String text = "one\ntwo\nthree";
		assertTrue(CodeEditorPage.describe(page, "f.txt", text, CodeEditorState.selecting(6, 9),
			false).startsWith("The server knows: f.txt, 3 lines; the cursor on line 3, column 2; " +
				"3 characters selected"));
		assertNotNull(CodeEditorPage.axisOf("both"));
		assertEquals(null, CodeEditorPage.axisOf("none"));
	}

	@Test
	void givesTheAppsTheSamplesTheFilesAndTheTexts()
	{
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "react")
			.getAttribute("data-config"));

		assertEquals(6, config.getJSONArray("languages").length());
		assertEquals("log", config.getJSONArray("languages").getJSONObject(5).getString("name"));
		assertEquals("shop.log", config.getJSONArray("languages").getJSONObject(5)
			.getString("file"));
		assertEquals("[\"vertical\",\"horizontal\",\"both\",\"none\"]",
			config.getJSONArray("resizes").toString());
		assertEquals("markdown", config.getJSONArray("files").getJSONObject(3)
			.getString("language"));
		JSONObject texts = config.getJSONObject("texts");
		assertTrue(texts.getString("known").startsWith("The app knows"));
		assertEquals("Log (the page's own grammar)", texts.getString("language.log"));
		assertEquals("Both", texts.getString("resize.both"));
		assertEquals("Drag, or use the arrow keys, to resize the editor",
			config.getJSONObject("editor").getString("resize"));
		assertEquals("Close {0}?", config.getJSONObject("tabs").getString("closeModifiedTitle"));
		assertTrue(config.getJSONObject("icons").getString("file").contains("<svg"));
		assertEquals("NOTES.md", config.getJSONObject("notes").getString("file"));
		assertTrue(config.getJSONObject("notes").getString("text").startsWith("# Release notes"));
		assertEquals("Preview", config.getJSONObject("preview").getString("label"));
		assertEquals("Nothing to preview", config.getJSONObject("preview").getString("placeholder"));
		assertEquals("Hide the right pane", config.getJSONObject("split").getString("collapseRight"));
		assertTrue(texts.getString("notesStart").contains("the app keeps the notes"));
		assertEquals("Show the preview", texts.getString("showPreview"));
	}

	@Test
	void speaksGermanAndSpanish()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(CodeEditorPage.class);
		tester.assertContains("Jede Sprache, auf dem Server behalten");
		JSONObject config = new JSONObject(TagTester.createTagByAttribute(
			tester.getLastResponseAsString(), "data-framework-app", "svelte")
			.getAttribute("data-config"));
		assertEquals("Suchen", config.getJSONObject("editor").getJSONObject("phrases")
			.getString("Find"));

		tester.getSession().setLocale(new Locale("es"));
		tester.startPage(CodeEditorPage.class);
		tester.assertContains("Cualquier lenguaje, guardado en el servidor");
		tester.assertContains("\"Find\":\"Buscar\"");
	}
}
