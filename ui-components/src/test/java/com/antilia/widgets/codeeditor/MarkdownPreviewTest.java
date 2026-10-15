/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.codeeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

class MarkdownPreviewTest extends WicketTestCase
{
	@Test
	void rendersTheTextOfItsModelInTheBrowser()
	{
		MarkdownPreview preview = new MarkdownPreview("preview", Model.of("# Notes"));
		tester.startPage(new TestPage(new CodeEditor("editor", Model.of("")), preview));

		TagTester tag = tester.getTagByWicketId("preview");
		assertEquals(preview.getMarkupId(), tag.getAttribute("id"));
		assertEquals("antilia-markdown-preview", tag.getAttribute("class"));
		assertEquals("0", tag.getAttribute("tabindex"));
		assertEquals("", tag.getValue(), "the script renders the text");
		tester.assertContains("antilia-code-editor.*\\.js");
		tester.assertContains("antilia-code-editor.*\\.css");

		JSONObject options = options(preview);
		assertEquals("# Notes", options.getString("value"));
		assertEquals("Preview", options.getString("label"));
		assertEquals("Nothing to preview", options.getString("placeholder"));
		assertFalse(options.has("editor"));
		assertNull(preview.getEditor());
		assertEquals("# Notes", preview.getText());
	}

	@Test
	void followsACodeEditor()
	{
		CodeEditor editor = new CodeEditor("editor", Model.of("*text* <b>"));
		MarkdownPreview preview = new MarkdownPreview("preview", editor);
		tester.startPage(new TestPage(editor, preview));

		JSONObject options = options(preview);
		assertEquals(editor.getMarkupId(), options.getString("editor"));
		assertEquals("*text* <b>", options.getString("value"));
		assertSame(editor, preview.getEditor());

		// what the user typed, which the editor keeps as its input
		tester.getRequest().setParameter("state", "{}");
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"typed <i>");
		tester.executeBehavior(editor.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
		assertEquals("typed <i>", preview.getText());

		assertThrows(IllegalArgumentException.class, () -> new MarkdownPreview("preview",
			(CodeEditor)null));
	}

	@Test
	void takesItsLabelAndPlaceholder()
	{
		MarkdownPreview preview = new MarkdownPreview("preview", Model.of((String)null));
		preview.setLabel(Model.of("Notes, as shown")).setPlaceholder(Model.of((String)null));
		tester.startPage(new TestPage(new CodeEditor("editor", Model.of("")), preview));

		JSONObject options = options(preview);
		assertEquals("", options.getString("value"));
		assertEquals("Notes, as shown", options.getString("label"));
		assertEquals("", options.getString("placeholder"));
		assertEquals("Notes, as shown", preview.getLabel().getObject());
		assertNull(preview.getPlaceholder().getObject());
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		MarkdownPreview preview = new MarkdownPreview("preview", Model.of(""));
		tester.startPage(new TestPage(new CodeEditor("editor", Model.of("")), preview));
		assertEquals("Vorschau", options(preview).getString("label"));
		assertEquals("Nichts zur Vorschau", options(preview).getString("placeholder"));

		tester.getSession().setLocale(new Locale("es"));
		preview = new MarkdownPreview("preview", Model.of(""));
		tester.startPage(new TestPage(new CodeEditor("editor", Model.of("")), preview));
		assertEquals("Vista previa", options(preview).getString("label"));
	}

	private JSONObject options(MarkdownPreview preview)
	{
		Matcher matcher = Pattern.compile("Wicket\\.MarkdownPreview\\.attach\\(\"" +
			Pattern.quote(preview.getMarkupId()) + "\", (\\{.*\\})\\);").matcher(
				tester.getLastResponseAsString());
		assertTrue(matcher.find(), "the preview is attached");
		return new JSONObject(matcher.group(1));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(CodeEditor editor, MarkdownPreview preview)
		{
			Form<Void> form = new Form<>("form");
			add(form);
			form.add(editor);
			add(preview);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"editor\"></textarea></form>" +
				"<div wicket:id=\"preview\">[preview]</div></body></html>");
		}
	}
}
