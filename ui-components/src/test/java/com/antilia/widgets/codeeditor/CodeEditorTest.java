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
package com.antilia.widgets.codeeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.csp.ContentSecurityPolicySettings;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

import com.antilia.widgets.codeeditor.CodeEditorState.Range;
import com.antilia.widgets.resizable.ResizableSize;
import com.antilia.widgets.resizable.ResizeAxis;

class CodeEditorTest extends WicketTestCase
{
	private static final String TEXT = "class A {\n}";

	@Test
	void rendersTheTextAreaAndMakesItAnEditorInTheBrowser()
	{
		TestPage page = new TestPage(new CodeEditor("editor", Model.of(TEXT)));
		tester.startPage(page);

		TagTester area = tester.getTagByWicketId("editor");
		assertEquals("textarea", area.getName());
		assertEquals(TEXT, area.getValue());
		assertEquals(page.editor.getMarkupId(), area.getAttribute("id"));
		tester.assertContains("antilia-code-editor.*\\.js");
		tester.assertContains("antilia-code-editor.*\\.css");
		tester.assertContains("antilia-resizable.*\\.css");

		JSONObject options = options(page.editor);
		assertTrue(options.getBoolean("lineNumbers"));
		assertTrue(options.getBoolean("folding"));
		assertFalse(options.getBoolean("readOnly"));
		assertTrue(options.getBoolean("selectable"));
		assertFalse(options.getBoolean("lineWrapping"));
		assertEquals(4, options.getInt("tabSize"));
		assertEquals("vertical", options.getString("resizable"));
		assertEquals(CodeEditor.DEFAULT_MIN_HEIGHT, options.getInt("minHeight"));
		assertFalse(options.has("language"));
		assertFalse(options.has("saved"));
		assertFalse(options.has("placeholder"));
		assertEquals("{\"scrollTop\":0,\"scrollLeft\":0}",
			options.getJSONObject("state").toString());
		assertEquals("Drag, or use the arrow keys, to resize the editor",
			options.getJSONObject("texts").getString("resize"));
		assertEquals(0, options.getJSONObject("texts").getJSONObject("phrases").length(),
			"CodeMirror's own phrases are in English");
		assertTrue(options.getString("stateUrl").contains("-form-editor"));
		assertEquals(page.editor.getInputName(), options.getString("inputName"));
		assertTrue(page.editor.isSendToServer());
		assertEquals(CodeEditorState.NONE, page.editor.getState());
	}

	@Test
	void leavesTheScriptAsTheBuildMinifiedIt()
	{
		// Wicket's compressor would strip it again in deployment mode, breaking its expressions
		assertFalse(((JavaScriptPackageResource)CodeEditor.JS.getResource()).getCompress());
	}

	@Test
	void givesTheStyleElementOfCodeMirrorTheNonceOfThePage()
	{
		tester.getApplication().getCspSettings().blocking().clear().strict();
		TestPage page = new TestPage(new CodeEditor("editor", Model.of(TEXT)));
		tester.startPage(page);

		// the nonce of the page, which its scripts carry
		assertEquals(page.getMetaData(ContentSecurityPolicySettings.NONCE_KEY),
			options(page.editor).getString("nonce"));
	}

	@Test
	void givesNoNonceWithoutAContentSecurityPolicy()
	{
		tester.getApplication().getCspSettings().blocking().disabled();
		TestPage page = new TestPage(new CodeEditor("editor", Model.of(TEXT)));
		tester.startPage(page);

		assertFalse(options(page.editor).has("nonce"));
	}

	@Test
	void takesTheOptions()
	{
		CodeEditor editor = new CodeEditor("editor", Model.of(TEXT));
		editor.setLanguage(CodeEditorLanguage.JAVA).setLineNumbers(false).setFolding(false)
			.setReadOnly(true).setSelectable(false).setLineWrapping(true).setTabSize(2)
			.setPlaceholder(Model.of("Type some Java")).setResizable(ResizeAxis.BOTH)
			.setMinHeight(100).setMaxHeight(600).setMinWidth(200).setMaxWidth(900)
			.setSavedModel(Model.of("class A {\r\n}\r\n"));
		editor.setLabel(Model.of("Source"));
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		JSONObject options = options(editor);
		assertEquals("java", options.getString("language"));
		assertFalse(options.getBoolean("lineNumbers"));
		assertFalse(options.getBoolean("folding"));
		assertTrue(options.getBoolean("readOnly"));
		assertFalse(options.getBoolean("selectable"));
		assertTrue(options.getBoolean("lineWrapping"));
		assertEquals(2, options.getInt("tabSize"));
		assertEquals("Type some Java", options.getString("placeholder"));
		assertEquals("Source", options.getString("label"));
		assertEquals("both", options.getString("resizable"));
		assertEquals(100, options.getInt("minHeight"));
		assertEquals(600, options.getInt("maxHeight"));
		assertEquals(200, options.getInt("minWidth"));
		assertEquals(900, options.getInt("maxWidth"));
		assertEquals("class A {\n}\n", options.getString("saved"), "as the browser has it");
		assertEquals(CodeEditorLanguage.JAVA, editor.getLanguage());
		assertEquals(2, editor.getTabSize());
		assertEquals(ResizeAxis.BOTH, editor.getResizable());

		editor.setResizable(null).setTabSize(99);
		tester.startPage(page);
		options = options(editor);
		assertFalse(options.getBoolean("resizable"));
		assertFalse(options.has("minHeight"));
		assertEquals(16, options.getInt("tabSize"));
	}

	@Test
	void rendersTheScriptOfALanguageOfThePageAfterItsOwn()
	{
		JavaScriptResourceReference script = new JavaScriptResourceReference(CodeEditorTest.class,
			"log-grammar.js");
		CodeEditor editor = new CodeEditor("editor", Model.of("ERROR"));
		editor.setLanguage(CodeEditorLanguage.of("Log", script));
		tester.startPage(new TestPage(editor));

		String response = tester.getLastResponseAsString();
		assertTrue(response.indexOf("antilia-code-editor.js") < response.indexOf("log-grammar.js"));
		assertEquals("log", options(editor).getString("language"));
	}

	@Test
	void translatesCodeMirrorsPhrases()
	{
		tester.getSession().setLocale(Locale.of("es"));
		TestPage page = new TestPage(new CodeEditor("editor", Model.of(TEXT)));
		tester.startPage(page);

		JSONObject texts = options(page.editor).getJSONObject("texts");
		assertEquals("Arrastre o use las flechas para cambiar el tamaño del editor",
			texts.getString("resize"));
		JSONObject phrases = texts.getJSONObject("phrases");
		assertEquals("Buscar", phrases.getString("Find"));
		assertEquals("mayúsculas y minúsculas", phrases.getString("match case"));
		assertEquals("$ coincidencias reemplazadas", phrases.getString("replaced $ matches"));
	}

	@Test
	void keepsTheStateTheBrowserSendsAndShowsItAgain()
	{
		List<CodeEditorState> heard = new ArrayList<>();
		IModel<CodeEditorState> stateModel = Model.of((CodeEditorState)null);
		CodeEditor editor = new CodeEditor("editor", Model.of(TEXT), stateModel)
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

		sendState(editor, "{\"selection\":{\"ranges\":[{\"anchor\":0,\"head\":5}," +
			"{\"anchor\":9,\"head\":9}],\"main\":1},\"scrollTop\":12.4,\"scrollLeft\":3," +
			"\"width\":700,\"height\":260}", null);

		CodeEditorState expected = new CodeEditorState(List.of(new Range(0, 5), new Range(9, 9)),
			1, 12, 3, ResizableSize.height(260));
		assertEquals(expected, stateModel.getObject(), "the width is the page's: vertical");
		assertEquals(List.of(expected), heard);
		assertEquals(9, editor.getState().getCursor());
		assertEquals(TEXT, editor.getModelObject());

		tester.startPage(page);
		assertEquals("{\"selection\":{\"ranges\":[{\"anchor\":0,\"head\":5},{\"anchor\":9," +
			"\"head\":9}],\"main\":1},\"scrollTop\":12,\"scrollLeft\":3,\"height\":260}", options(editor).getJSONObject("state").toString());
	}

	@Test
	void keepsTheTextTheBrowserSendsAsItsInputUntilTheFormIsSent()
	{
		IModel<String> text = Model.of(TEXT);
		TestPage page = new TestPage(new CodeEditor("editor", text));
		tester.startPage(page);

		sendState(page.editor, "{\"scrollTop\":0}", "class B {\n}");
		assertEquals(TEXT, text.getObject(), "the model waits for the form");
		tester.startPage(page);
		assertEquals("class B {\n}", tester.getTagByWicketId("editor").getValue());

		// a state the browser could not make is left alone
		sendState(page.editor, "not json", null);
		assertEquals(CodeEditorState.NONE, page.editor.getState());
	}

	@Test
	void hearsNothingWithoutSendingToTheServer()
	{
		CodeEditor editor = new CodeEditor("editor", Model.of(TEXT)).setSendToServer(false);
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		assertFalse(options(editor).has("stateUrl"));
		assertFalse(options(editor).has("inputName"));

		sendState(editor, "{\"scrollTop\":40}", "other");
		assertEquals(CodeEditorState.NONE, editor.getState());
		tester.startPage(page);
		assertEquals(TEXT, tester.getTagByWicketId("editor").getValue());
	}

	@Test
	void tellsTheBrowserTheTextWasSaved()
	{
		IModel<String> saved = Model.of("old");
		CodeEditor editor = new CodeEditor("editor", Model.of("new\r\ntext"));
		editor.setSavedModel(saved);
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		page.action = target -> editor.markSaved(target).focus(target);
		tester.clickLink("act");

		assertEquals("new\ntext", saved.getObject());
		tester.assertContains("e.markSaved\\(\"new\\\\ntext\"\\);");
		tester.assertContains("Wicket.CodeEditor.of\\(\"" + editor.getMarkupId() +
			"\"\\);if\\(e\\)\\{e.focus\\(\\);");
	}

	@Test
	void hearsOfTheEventsOfTheEditorWithItsText()
	{
		List<String> saved = new ArrayList<>();
		IModel<String> text = Model.of(TEXT);
		CodeEditor editor = new CodeEditor("editor", text);
		CodeEditorEventBehavior save = new CodeEditorEventBehavior(CodeEditorEvent.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				saved.add(editor.getModelObject());
				editor.markSaved(target);
			}
		};
		editor.add(save);
		CodeEditorEventBehavior typing = new CodeEditorEventBehavior(CodeEditorEvent.INPUT)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
			}
		};
		editor.add(typing);
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		tester.assertContains("\"e\":\"antilia-code-editor:save\"");
		// typing waits until the user paused
		tester.assertContains("\"e\":\"input\",\"tr\":\\{\"id\":\"[^\"]*\",\"d\":500,\"p\":true\\}");
		assertSame(CodeEditorEvent.SAVE, save.getCodeEditorEvent());
		assertEquals(CodeEditorEventBehavior.DEFAULT_DELAY, typing.getDelay());

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"class C {}\n");
		tester.executeBehavior(save);
		// code keeps its white space
		assertEquals(List.of("class C {}\n"), saved);
		assertEquals("class C {}\n", text.getObject());
		tester.assertContains("Wicket.CodeEditor.of\\(\"" + editor.getMarkupId() +
			"\"\\);if\\(e\\)\\{e.markSaved\\(\"class C \\{}\\\\n\"\\);");

		typing.setDelay(Duration.ZERO);
		tester.startPage(page);
		tester.assertContainsNot("\"d\":500");
	}

	@Test
	void eventsHaveTheNamesOfTheBrowser()
	{
		assertEquals("input", CodeEditorEvent.INPUT.getEventName());
		assertEquals("change", CodeEditorEvent.CHANGE.getEventName());
		assertEquals("antilia-code-editor:blur", CodeEditorEvent.BLUR.getEventName());
		assertTrue(CodeEditorEvent.SELECT.isFrequent());
		assertFalse(CodeEditorEvent.FOCUS.isFrequent());
	}

	@Test
	void languagesAreKnownByTheirNames()
	{
		assertSame(CodeEditorLanguage.JSON, CodeEditorLanguage.of(" JSON "));
		assertEquals(List.of("java", "javascript", "typescript", "json", "markdown"),
			CodeEditorLanguage.known().stream().map(CodeEditorLanguage::getName).toList());
		assertNull(CodeEditorLanguage.MARKDOWN.getScript());
		CodeEditorLanguage own = CodeEditorLanguage.of("log", new JavaScriptResourceReference(
			CodeEditorTest.class, "log-grammar.js"));
		assertEquals(CodeEditorLanguage.of("LOG"), own);
		assertEquals(own.hashCode(), CodeEditorLanguage.of("log").hashCode());
		assertEquals("log", own.toString());
	}

	@Test
	void statesParseAndPrint()
	{
		CodeEditorState state = CodeEditorState.parse("{\"selection\":{\"ranges\":[{\"anchor\":7," +
			"\"head\":2},{\"anchor\":1}],\"main\":4},\"scrollTop\":-3,\"height\":\"tall\"}");
		assertEquals(List.of(new Range(7, 2)), state.getRanges());
		assertEquals(0, state.getMain());
		assertEquals(0, state.getScrollTop());
		assertEquals(ResizableSize.NONE, state.getSize());
		assertEquals(new Range(7, 2), state.getMainRange());
		assertEquals(2, state.getMainRange().from());
		assertEquals(7, state.getMainRange().to());
		assertFalse(state.getMainRange().isEmpty());
		assertEquals("ass A", state.getSelectedText("class A {\r\n}"));
		assertEquals("", CodeEditorState.NONE.getSelectedText(null));
		assertEquals(0, CodeEditorState.NONE.getCursor());
		assertTrue(CodeEditorState.NONE.getMainRange().isEmpty());
		assertNull(CodeEditorState.parse(" "));
		assertNull(CodeEditorState.parse("[1, 2]"));

		CodeEditorState selecting = CodeEditorState.selecting(3, 8).withSize(
			ResizableSize.of(400, 200));
		assertEquals(selecting, CodeEditorState.parse(selecting.toJson().toString()));
		assertEquals(selecting.hashCode(), CodeEditorState.parse(selecting.toString()).hashCode());
		assertEquals(List.of(Range.cursor(1)), selecting.withRanges(List.of(Range.cursor(1)), 0)
			.getRanges());
		assertNotEquals(selecting, CodeEditorState.NONE);
	}

	/**
	 * @return the options the editor was attached with in the last response
	 */
	private JSONObject options(CodeEditor editor)
	{
		Matcher matcher = Pattern.compile("Wicket\\.CodeEditor\\.attach\\(\"" +
			Pattern.quote(editor.getMarkupId()) + "\", (\\{.*\\})\\);").matcher(
				tester.getLastResponseAsString());
		assertTrue(matcher.find(), "the editor is attached");
		return new JSONObject(matcher.group(1));
	}

	private void sendState(CodeEditor editor, String state, String text)
	{
		tester.getRequest().setParameter("state", state);
		if (text != null)
		{
			// as the browser posts it
			tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), text);
		}
		tester.executeBehavior(editor.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final CodeEditor editor;

		/** What the link does. */
		SerializableConsumer<AjaxRequestTarget> action = target -> {
		};

		TestPage(CodeEditor editor)
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
