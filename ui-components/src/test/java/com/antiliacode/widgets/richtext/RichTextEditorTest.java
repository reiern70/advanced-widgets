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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.resizable.ResizableSize;
import com.antiliacode.widgets.resizable.ResizeAxis;

class RichTextEditorTest extends WicketTestCase
{
	private static final String HTML = "<h2>Title</h2><p>Hello <strong>world</strong></p>";

	@Test
	void rendersTheTextAreaAndMakesItAnEditorInTheBrowser()
	{
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		tester.startPage(page);

		TagTester area = tester.getTagByWicketId("editor");
		assertEquals("textarea", area.getName());
		// escaped in the text area
		assertEquals(HTML.replace("<", "&lt;").replace(">", "&gt;"), area.getValue());
		assertEquals(page.editor.getMarkupId(), area.getAttribute("id"));
		// hidden by the style sheet until the script makes the editor of it
		assertEquals(RichTextEditor.LOADING_CLASS, area.getAttribute("class"));
		assertEquals("antilia-rich-text-editor-loading", RichTextEditor.LOADING_CLASS);
		tester.assertContains("antilia-rich-text-editor.*\\.js");
		tester.assertContains("antilia-rich-text-editor.*\\.css");
		tester.assertContains("antilia-resizable.*\\.css");
		tester.assertContains("antilia-button.*\\.css");
		tester.assertContains("antilia-popover.*\\.css");

		JSONObject options = options(page.editor);
		assertEquals("html", options.getString("format"));
		assertEquals(RichTextFeature.values().length, options.getJSONArray("features").length());
		assertEquals("heading", options.getJSONArray("features").getString(0));
		assertEquals("[1,2,3,4]", options.getJSONArray("headingLevels").toString());
		JSONArray toolbar = options.getJSONArray("toolbar");
		assertEquals(RichTextTool.DEFAULT_TOOLBAR.size(), toolbar.length());
		assertEquals("[\"bold\",\"italic\",\"underline\",\"strike\",\"code\"]",
			toolbar.getJSONArray(1).toString());
		assertFalse(options.getBoolean("bubbleMenu"));
		assertFalse(options.getBoolean("readOnly"));
		assertFalse(options.getBoolean("count"));
		assertFalse(options.has("limit"));
		assertFalse(options.has("saved"));
		assertFalse(options.has("placeholder"));
		assertFalse(options.has("label"));
		assertFalse(options.has("extensions"));
		assertEquals("vertical", options.getString("resizable"));
		assertEquals(RichTextEditor.DEFAULT_MIN_HEIGHT, options.getInt("minHeight"));
		assertEquals("{\"selection\":{\"anchor\":0,\"head\":0},\"scrollTop\":0,\"scrollLeft\":0}",
			options.getJSONObject("state").toString());
		JSONObject texts = options.getJSONObject("texts");
		assertEquals("Bold", texts.getString("bold"));
		assertEquals("Heading {0}", texts.getString("headingLevel"));
		assertEquals("Drag, or use the arrow keys, to resize the editor", texts.getString("resize"));
		assertTrue(options.getString("stateUrl").contains("-form-editor"));
		assertEquals(page.editor.getInputName(), options.getString("inputName"));
		assertTrue(page.editor.isSendToServer());
		assertEquals(RichTextEditorState.NONE, page.editor.getState());
		assertSame(RichTextPolicy.ALL, page.editor.getSanitizer());
	}

	@Test
	void leavesTheScriptAsTheBuildMinifiedIt()
	{
		assertFalse(((JavaScriptPackageResource)RichTextEditor.JS.getResource()).getCompress());
	}

	@Test
	void takesTheOptions()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.setFormat(RichTextFormat.MARKDOWN).setHeadingLevels(3, 1, 9, 1)
			.setToolbar(List.of(List.of(RichTextTool.BOLD, RichTextTool.LINK), List.of()))
			.setBubbleMenu(true).setReadOnly(true).setLimit(500).setCount(true)
			.setPlaceholder(Model.of("Write here")).setResizable(ResizeAxis.BOTH)
			.setMinHeight(100).setMaxHeight(600).setMinWidth(200).setMaxWidth(900)
			.setSavedModel(Model.of("Before"));
		editor.setLabel(Model.of("Article"));
		tester.startPage(new TestPage(editor));

		JSONObject options = options(editor);
		assertEquals("markdown", options.getString("format"));
		String features = options.getJSONArray("features").toString();
		assertFalse(features.contains("\"underline\""), "Markdown has no syntax for it");
		assertFalse(features.contains("\"textAlign\""));
		assertFalse(features.contains("\"textColor\""));
		assertFalse(features.contains("\"textBackground\""));
		assertTrue(features.contains("\"table\""));
		assertEquals("[1,3]", options.getJSONArray("headingLevels").toString());
		assertEquals("[[\"bold\",\"link\"]]", options.getJSONArray("toolbar").toString());
		assertTrue(options.getBoolean("bubbleMenu"));
		assertTrue(options.getBoolean("readOnly"));
		assertTrue(options.getBoolean("count"));
		assertEquals(500, options.getInt("limit"));
		assertEquals("Write here", options.getString("placeholder"));
		assertEquals("Article", options.getString("label"));
		assertEquals("Before", options.getString("saved"));
		assertEquals("both", options.getString("resizable"));
		assertEquals(100, options.getInt("minHeight"));
		assertEquals(600, options.getInt("maxHeight"));
		assertEquals(200, options.getInt("minWidth"));
		assertEquals(900, options.getInt("maxWidth"));
		assertEquals(RichTextFormat.MARKDOWN, editor.getFormat());
		assertEquals(List.of(1, 3), editor.getHeadingLevels());
		assertEquals(500, editor.getLimit());
		assertTrue(editor.isCount());
		assertTrue(editor.isBubbleMenu());
		assertTrue(editor.isReadOnly());
		assertEquals(ResizeAxis.BOTH, editor.getResizable());
		assertEquals(1, editor.getToolbar().size());

		editor.setResizable(null).setToolbar(null).setLimit(-4).setSavedModel(Model.of((String)null))
			.setFeatures(EnumSet.of(RichTextFeature.BOLD));
		tester.startPage(new TestPage(editor));
		options = options(editor);
		assertFalse(options.getBoolean("resizable"));
		assertFalse(options.has("minHeight"));
		assertFalse(options.getBoolean("toolbar"), "no toolbar");
		assertEquals(0, options.getInt("limit"));
		assertEquals("", options.getString("saved"));
		assertEquals("[\"bold\"]", options.getJSONArray("features").toString());
		assertEquals(Set.of(RichTextFeature.BOLD), editor.getFeatures());

		editor.setFeatures(Set.of());
		assertTrue(editor.getFeatures().isEmpty());
		editor.setFeatures(null).setLimit(null);
		assertEquals(RichTextFeature.defaults(RichTextFormat.MARKDOWN), editor.getFeatures());
		assertNull(editor.getLimit());
	}

	@Test
	void rendersTheScriptsOfTheExtensionsOfThePageAfterItsOwn()
	{
		JavaScriptResourceReference script = new JavaScriptResourceReference(
			RichTextEditorTest.class, "highlight-extension.js");
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.setExtensions(RichTextExtension.of("Highlight", script),
			RichTextExtension.of("own", null));
		tester.startPage(new TestPage(editor));

		String response = tester.getLastResponseAsString();
		assertTrue(response.indexOf("antilia-rich-text-editor.js") <
			response.indexOf("highlight-extension.js"));
		assertEquals("[\"highlight\",\"own\"]", options(editor).getJSONArray("extensions").toString());
		assertEquals(2, editor.getExtensions().size());
	}

	@Test
	void highlightsWithTheScriptOfTheGrammarsOnlyWhenTold()
	{
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		tester.startPage(page);
		tester.assertContainsNot("antilia-rich-text-highlight");
		tester.assertContainsNot("antilia-code-editor");
		JSONObject options = options(page.editor);
		assertFalse(options.has("highlight"));
		assertFalse(options.has("codeLanguages"));
		assertFalse(page.editor.isHighlighting());

		page.editor.setHighlighting(true);
		tester.startPage(page);
		String response = tester.getLastResponseAsString();
		assertTrue(response.indexOf("antilia-rich-text-editor.js") <
			response.indexOf("antilia-rich-text-highlight.js"));
		tester.assertContains("antilia-code-editor.*\\.css");
		assertTrue(options(page.editor).getBoolean("highlight"));
		assertFalse(((JavaScriptPackageResource)RichTextEditor.HIGHLIGHT_JS.getResource())
			.getCompress());
	}

	@Test
	void offersTheLanguagesOfCodeBlocksItIsGiven()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		assertNull(editor.getCodeLanguages());
		editor.setCodeLanguages("java", " sql ", "not a name", null, "java");
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		assertEquals("[\"java\",\"sql\"]", options(editor).getJSONArray("codeLanguages")
			.toString());
		assertEquals(List.of("java", "sql"), editor.getCodeLanguages());

		editor.setCodeLanguages();
		tester.startPage(page);
		assertFalse(options(editor).getBoolean("codeLanguages"));
		editor.setCodeLanguages((String[])null);
		tester.startPage(page);
		assertFalse(options(editor).has("codeLanguages"));
	}

	@Test
	void uploadsImagesWhileItHasAStore()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		assertFalse(options(editor).has("uploadUrl"));
		assertTrue(editor.getBehaviors(RichTextImageUploadBehavior.class).isEmpty());
		assertFalse(editor.isImageUploading());

		editor.setImageMaxSize(Bytes.kilobytes(512)).setImageTypes(EnumSet.of(
			RichTextImageType.PNG, RichTextImageType.JPEG))
			.setImageStore((image, type) -> "/images/a." + type.getExtension());
		tester.startPage(page);
		JSONObject options = options(editor);
		assertTrue(options.getString("uploadUrl").contains("-form-editor"));
		assertEquals("[\"image/png\",\"image/jpeg\"]", options.getJSONArray("uploadTypes")
			.toString());
		assertEquals(512 * 1024, options.getLong("uploadMaxSize"));
		List<RichTextImageUploadBehavior> behaviors = editor.getBehaviors(
			RichTextImageUploadBehavior.class);
		assertEquals(1, behaviors.size());
		assertEquals(Bytes.kilobytes(512), behaviors.get(0).getMaxSize());
		assertEquals(Bytes.kilobytes(512), editor.getImageMaxSize());
		assertEquals(Set.of(RichTextImageType.PNG, RichTextImageType.JPEG), editor.getImageTypes());
		assertTrue(editor.isImageUploading());

		// changed after: the behavior follows
		editor.setImageMaxSize(Bytes.megabytes(1)).setImageTypes(EnumSet.of(RichTextImageType.GIF));
		assertEquals(Bytes.megabytes(1), behaviors.get(0).getMaxSize());
		assertEquals(Set.of(RichTextImageType.GIF), behaviors.get(0).getTypes());

		// not while read only, without images or disabled
		editor.setReadOnly(true);
		tester.startPage(page);
		assertFalse(options(editor).has("uploadUrl"));
		editor.setReadOnly(false).setFeatures(EnumSet.of(RichTextFeature.BOLD));
		tester.startPage(page);
		assertFalse(options(editor).has("uploadUrl"));
		editor.setFeatures(null);
		editor.setEnabled(false);
		tester.startPage(page);
		assertFalse(options(editor).has("uploadUrl"));
		editor.setEnabled(true);
		editor.setImageStore(null);
		tester.startPage(page);
		assertFalse(options(editor).has("uploadUrl"));
		assertEquals(1, editor.getBehaviors(RichTextImageUploadBehavior.class).size());
	}

	@Test
	void translatesItsTexts()
	{
		tester.getSession().setLocale(Locale.of("es"));
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		tester.startPage(page);

		JSONObject texts = options(page.editor).getJSONObject("texts");
		assertEquals("Negrita", texts.getString("bold"));
		assertEquals("Título {0}", texts.getString("headingLevel"));
		assertEquals("Arrastre o use las flechas para cambiar el tamaño del editor",
			texts.getString("resize"));
		assertEquals("Texto sin formato", texts.getString("plainText"));
		assertEquals("{0} pesa más de {1}.", texts.getString("uploadTooLarge"));
		assertEquals("Color del texto", texts.getString("textColor"));
		assertEquals("Rojo", texts.getString("colorRed"));

		tester.getSession().setLocale(Locale.of("de"));
		tester.startPage(page);
		assertEquals("Fett", options(page.editor).getJSONObject("texts").getString("bold"));
		assertEquals("Hintergrundfarbe", options(page.editor).getJSONObject("texts")
			.getString("textBackground"));
	}

	@Test
	void sendsItsPalettesNamedInTheLanguageOfThePage()
	{
		tester.getSession().setLocale(Locale.of("es"));
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		tester.startPage(page);

		JSONArray colors = options(page.editor).getJSONArray("colors");
		assertEquals(RichTextColor.values().length, colors.length());
		assertEquals("{\"name\":\"red\",\"colorClass\":\"antilia-color-red\",\"label\":\"Rojo\"}",
			colors.getJSONObject(2).toString());
		JSONArray backgrounds = options(page.editor).getJSONArray("backgroundColors");
		assertEquals(RichTextBackgroundColor.values().length, backgrounds.length());
		assertEquals("{\"name\":\"red\",\"backgroundClass\":\"antilia-background-red\"," +
			"\"label\":\"Rojo\"}", backgrounds.getJSONObject(2).toString());
		assertSame(RichTextColor.PALETTE, page.editor.getColors());
		assertSame(RichTextBackgroundColor.PALETTE, page.editor.getBackgroundColors());

		page.editor.setColors(List.of(new RichTextViewTest.Brand(), RichTextColor.BLUE));
		page.editor.setBackgroundColors(List.of(RichTextBackgroundColor.GREEN));
		tester.getSession().setLocale(Locale.ENGLISH);
		tester.startPage(page);
		assertEquals("[{\"name\":\"brand\",\"colorClass\":\"brand-text\",\"label\":\"brand\"}," +
			"{\"name\":\"blue\",\"colorClass\":\"antilia-color-blue\",\"label\":\"Blue\"}]",
			options(page.editor).getJSONArray("colors").toString());
		assertEquals("[{\"name\":\"green\",\"backgroundClass\":\"antilia-background-green\"," +
			"\"label\":\"Green\"}]", options(page.editor).getJSONArray("backgroundColors")
				.toString());

		page.editor.setFeatures(EnumSet.of(RichTextFeature.TEXT_BACKGROUND));
		tester.startPage(page);
		assertFalse(options(page.editor).has("colors"));
		assertTrue(options(page.editor).has("backgroundColors"));

		page.editor.setFeatures(EnumSet.of(RichTextFeature.TEXT_COLOR));
		tester.startPage(page);
		assertTrue(options(page.editor).has("colors"));
		assertFalse(options(page.editor).has("backgroundColors"));

		page.editor.setFeatures(EnumSet.of(RichTextFeature.BOLD));
		tester.startPage(page);
		assertFalse(options(page.editor).has("colors"));
		assertFalse(options(page.editor).has("backgroundColors"));
	}

	@Test
	void cleansWithTheClassesOfItsPalettes()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		String html = "<p><span class=\"brand-text\">a</span><mark class=\"brand-mark\">b</mark>" +
			"<span class=\"antilia-color-red\">c</span><mark class=\"antilia-background-red\">d" +
			"</mark></p>";
		assertSame(RichTextPolicy.ALL, editor.getSanitizer());

		editor.setColors(List.of(new RichTextViewTest.Brand()));
		assertEquals("<p><span class=\"brand-text\">a</span><mark>b</mark><span>c</span>" +
			"<mark class=\"antilia-background-red\">d</mark></p>",
			editor.getSanitizer().sanitize(html));
		assertEquals(List.of(new RichTextViewTest.Brand()), List.copyOf(editor.getColors()));

		editor.setBackgroundColors(List.of(new RichTextViewTest.Brand()));
		assertEquals("<p><span class=\"brand-text\">a</span><mark class=\"brand-mark\">b</mark>" +
			"<span>c</span><mark>d</mark></p>", editor.getSanitizer().sanitize(html));
		assertEquals(List.of(new RichTextViewTest.Brand()),
			List.copyOf(editor.getBackgroundColors()));

		editor.setColors(null).setBackgroundColors(List.of());
		assertEquals(Set.of(), editor.getColors(), "null for no colors");
		assertEquals(Set.of(), editor.getBackgroundColors());
		assertEquals("<p><span>a</span><mark>b</mark><span>c</span><mark>d</mark></p>",
			editor.getSanitizer().sanitize(html));

		editor.setColors(RichTextColor.PALETTE).setBackgroundColors(RichTextBackgroundColor.PALETTE);
		assertSame(RichTextPolicy.ALL, editor.getSanitizer());
		assertThrows(IllegalArgumentException.class, () -> editor.setColors(List.of(
			RichTextColor.RED, new RichTextViewTest.Brand(), RichTextColor.RED)));
		assertThrows(IllegalArgumentException.class, () -> editor.setBackgroundColors(List.of(
			RichTextBackgroundColor.RED, RichTextBackgroundColor.RED)));
		assertEquals(RichTextColor.PALETTE, editor.getColors(), "a palette refused changes nothing");
		assertEquals(RichTextBackgroundColor.PALETTE, editor.getBackgroundColors());
	}

	@Test
	void sendsNoColorsWithoutAPalette()
	{
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		page.editor.setColors(null);
		tester.startPage(page);
		// the editor in the browser leaves the tool out
		assertEquals("[]", options(page.editor).getJSONArray("colors").toString());
		assertEquals(RichTextBackgroundColor.values().length,
			options(page.editor).getJSONArray("backgroundColors").length());

		page.editor.setColors(RichTextColor.PALETTE).setBackgroundColors(null);
		tester.startPage(page);
		assertEquals(RichTextColor.values().length,
			options(page.editor).getJSONArray("colors").length());
		assertEquals("[]", options(page.editor).getJSONArray("backgroundColors").toString());

		page.editor.addBackgroundColor(RichTextBackgroundColor.YELLOW);
		tester.startPage(page);
		assertEquals("[{\"name\":\"yellow\",\"backgroundClass\":\"antilia-background-yellow\"," +
			"\"label\":\"Yellow\"}]", options(page.editor).getJSONArray("backgroundColors")
				.toString());
	}

	@Test
	void addsColorsAtTheEndOfItsPalettes()
	{
		TestPage page = new TestPage(new RichTextEditor("editor", Model.of(HTML)));
		RichTextViewTest.Brand brand = new RichTextViewTest.Brand();
		IRichTextBackgroundColor glow = RichTextPolicyTest.background("glow", "glow");
		assertSame(page.editor, page.editor.addColor(brand));
		assertSame(page.editor, page.editor.addBackgroundColor(glow));
		tester.startPage(page);

		List<IRichTextColor> palette = new ArrayList<>(RichTextColor.PALETTE);
		palette.add(brand);
		assertEquals(palette, List.copyOf(page.editor.getColors()));
		List<IRichTextBackgroundColor> backgrounds = new ArrayList<>(
			RichTextBackgroundColor.PALETTE);
		backgrounds.add(glow);
		assertEquals(backgrounds, List.copyOf(page.editor.getBackgroundColors()));

		JSONArray colors = options(page.editor).getJSONArray("colors");
		assertEquals(palette.size(), colors.length());
		assertEquals("brand", colors.getJSONObject(palette.size() - 1).getString("name"));
		JSONArray sent = options(page.editor).getJSONArray("backgroundColors");
		assertEquals(backgrounds.size(), sent.length());
		assertEquals("glow", sent.getJSONObject(backgrounds.size() - 1).getString("name"));
		assertEquals("<p><span class=\"brand-text\">a</span><span class=\"antilia-color-red\">b" +
			"</span><mark class=\"glow\">c</mark><mark>d</mark></p>", page.editor.getSanitizer()
				.sanitize("<p><span class=\"brand-text\">a</span><span class=\"antilia-color-red\">b" +
					"</span><mark class=\"glow\">c</mark><mark class=\"brand-mark\">d</mark></p>"));

		assertThrows(UnsupportedOperationException.class,
			() -> page.editor.getColors().add(RichTextColor.RED));
		assertThrows(UnsupportedOperationException.class,
			() -> page.editor.getBackgroundColors().add(RichTextBackgroundColor.RED));
		assertThrows(IllegalArgumentException.class, () -> page.editor.addColor(brand));
		assertThrows(IllegalArgumentException.class, () -> page.editor.addColor(null));
		assertThrows(IllegalArgumentException.class, () -> page.editor.addBackgroundColor(glow));
		assertThrows(IllegalArgumentException.class, () -> page.editor.addBackgroundColor(
			RichTextPolicyTest.background("other", "glow")));
		assertEquals(palette, List.copyOf(page.editor.getColors()),
			"a color refused changes nothing");
		assertEquals(backgrounds, List.copyOf(page.editor.getBackgroundColors()));
	}

	@Test
	void cleansTheHtmlTheFormPosts()
	{
		IModel<String> text = Model.of(HTML);
		TestPage page = new TestPage(new RichTextEditor("editor", text));
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p onclick=\"steal()\">Hi <a href=\"javascript:steal()\">there" +
			"</a></p><script>steal()</script><p style=\"position: fixed\">Styled</p>");
		form.submit();
		assertEquals("<p>Hi there</p><p>Styled</p>", text.getObject());

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
		RichTextEditor editor = new RichTextEditor("editor", text);
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
		RichTextEditor editor = new RichTextEditor("editor", text)
			.setFormat(RichTextFormat.MARKDOWN);
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "# Title\n\nSome <b>HTML</b> as text");
		form.submit();
		assertEquals("# Title\n\nSome <b>HTML</b> as text", text.getObject());
	}

	@Test
	void keepsTheStateTheBrowserSendsAndShowsItAgain()
	{
		List<RichTextEditorState> heard = new ArrayList<>();
		IModel<RichTextEditorState> stateModel = Model.of((RichTextEditorState)null);
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML), stateModel)
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

		sendState(editor, "{\"selection\":{\"anchor\":9,\"head\":3},\"scrollTop\":12.4," +
			"\"scrollLeft\":3,\"width\":700,\"height\":260}", null);

		RichTextEditorState expected = new RichTextEditorState(9, 3, 12, 3,
			ResizableSize.height(260));
		assertEquals(expected, stateModel.getObject(), "the width is the page's: vertical");
		assertEquals(List.of(expected), heard);
		assertEquals(HTML, editor.getModelObject());

		// the same again: the model keeps it
		sendState(editor, expected.toString(), null);
		assertEquals(2, heard.size());

		tester.startPage(page);
		assertEquals("{\"selection\":{\"anchor\":9,\"head\":3},\"scrollTop\":12,\"scrollLeft\":3," +
			"\"height\":260}", options(editor).getJSONObject("state").toString());

		editor.setResizable(null);
		sendState(editor, "{\"width\":700,\"height\":260}", null);
		assertEquals(ResizableSize.NONE, editor.getState().getSize());
		editor.setState(RichTextEditorState.selecting(1, 2));
		assertEquals(1, editor.getState().getAnchor());
		assertSame(stateModel, editor.getStateModel());
		editor.setStateModel(null);
		assertEquals(RichTextEditorState.NONE, editor.getState());
	}

	@Test
	void keepsTheTextTheBrowserSendsAsItsInputUntilTheFormIsSent()
	{
		IModel<String> text = Model.of(HTML);
		TestPage page = new TestPage(new RichTextEditor("editor", text));
		tester.startPage(page);

		sendState(page.editor, "{\"scrollTop\":0}", "<p>Changed</p>");
		assertEquals(HTML, text.getObject(), "the model waits for the form");
		tester.startPage(page);
		assertEquals("&lt;p&gt;Changed&lt;/p&gt;", tester.getTagByWicketId("editor").getValue());

		// a state the browser could not make is left alone
		sendState(page.editor, "not json", null);
		assertEquals(RichTextEditorState.NONE, page.editor.getState());

		// read only, the text the browser sends is not taken
		page.editor.setReadOnly(true);
		sendState(page.editor, "{\"scrollTop\":5}", "<p>Other</p>");
		assertEquals(5, page.editor.getState().getScrollTop());
	}

	@Test
	void hearsNothingWithoutSendingToTheServer()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML)).setSendToServer(false);
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		assertFalse(options(editor).has("stateUrl"));
		assertFalse(options(editor).has("inputName"));

		sendState(editor, "{\"scrollTop\":40}", "other");
		assertEquals(RichTextEditorState.NONE, editor.getState());
	}

	@Test
	void tellsTheBrowserTheTextWasSaved()
	{
		IModel<String> saved = Model.of("old");
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>new</p>"));
		editor.setSavedModel(saved);
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		page.action = target -> editor.markSaved(target).focus(target);
		tester.clickLink("act");

		assertEquals("<p>new</p>", saved.getObject());
		assertSame(saved, editor.getSavedModel());
		tester.assertContains("e.markSaved\\(\"<p>new<\\\\/p>\"\\);");
		tester.assertContains("Wicket.RichTextEditor.of\\(\"" + editor.getMarkupId() +
			"\"\\);if\\(e\\)\\{e.focus\\(\\);");

		// nothing to save, without a model of the text saved
		RichTextEditor empty = new RichTextEditor("editor", Model.of((String)null));
		TestPage other = new TestPage(empty);
		tester.startPage(other);
		other.action = target -> empty.markSaved(target);
		tester.clickLink("act");
		tester.assertContains("e.markSaved\\(\"\"\\);");
	}

	@Test
	void hearsOfTheEventsOfTheEditorWithItsText()
	{
		List<String> saved = new ArrayList<>();
		IModel<String> text = Model.of(HTML);
		RichTextEditor editor = new RichTextEditor("editor", text);
		RichTextEditorEventBehavior save = new RichTextEditorEventBehavior(RichTextEditorEvent.SAVE)
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
		RichTextEditorEventBehavior typing = new RichTextEditorEventBehavior(
			RichTextEditorEvent.INPUT)
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

		tester.assertContains("\"e\":\"antilia-rich-text-editor:save\"");
		// typing waits until the user paused
		tester.assertContains("\"e\":\"input\",\"tr\":\\{\"id\":\"[^\"]*\",\"d\":500,\"p\":true\\}");
		assertSame(RichTextEditorEvent.SAVE, save.getRichTextEditorEvent());
		assertEquals(RichTextEditorEventBehavior.DEFAULT_DELAY, typing.getDelay());

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"<p>Saved <img src=x onerror=steal()></p>");
		tester.executeBehavior(save);
		assertEquals(List.of("<p>Saved <img src=\"x\" /></p>"), saved);
		assertEquals("<p>Saved <img src=\"x\" /></p>", text.getObject());

		typing.setDelay(Duration.ZERO);
		tester.startPage(page);
		tester.assertContainsNot("\"d\":500");
	}

	@Test
	void goesToThePageStoreAndBack()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.setFeatures(EnumSet.of(RichTextFeature.BOLD, RichTextFeature.LINK))
			.setExtensions(RichTextExtension.of("own", null)).setSanitizer(html -> html.trim())
			.setPlaceholder(Model.of("Write")).setSavedModel(Model.of(HTML))
			.setState(RichTextEditorState.selecting(2, 5));
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		TestPage copy = org.apache.wicket.core.util.lang.WicketObjects.cloneObject(page);
		assertEquals(RichTextEditorState.selecting(2, 5), copy.editor.getState());
		assertEquals(" <p>x</p> ".trim(), copy.editor.getSanitizer().sanitize(" <p>x</p> "));
		assertEquals(Set.of(RichTextFeature.BOLD, RichTextFeature.LINK),
			copy.editor.getFeatures());
	}

	@Test
	void eventsHaveTheNamesOfTheBrowser()
	{
		assertEquals("input", RichTextEditorEvent.INPUT.getEventName());
		assertEquals("change", RichTextEditorEvent.CHANGE.getEventName());
		assertEquals("antilia-rich-text-editor:blur", RichTextEditorEvent.BLUR.getEventName());
		assertEquals("antilia-rich-text-editor:focus", RichTextEditorEvent.FOCUS.getEventName());
		assertEquals("antilia-rich-text-editor:resize", RichTextEditorEvent.RESIZE.getEventName());
		assertEquals("antilia-rich-text-editor:modified",
			RichTextEditorEvent.MODIFIED.getEventName());
		assertTrue(RichTextEditorEvent.SELECT.isFrequent());
		assertFalse(RichTextEditorEvent.FOCUS.isFrequent());
	}

	@Test
	void featuresToolsAndFormatsHaveTheNamesOfTheBrowser()
	{
		assertEquals("bulletList", RichTextFeature.BULLET_LIST.getName());
		assertEquals("textAlign", RichTextFeature.TEXT_ALIGN.getName());
		assertEquals("alignJustify", RichTextTool.ALIGN_JUSTIFY.getName());
		assertEquals("textBackground", RichTextFeature.TEXT_BACKGROUND.getName());
		assertSame(RichTextFeature.TEXT_COLOR, RichTextTool.TEXT_COLOR.getFeature());
		assertEquals("pink", RichTextColor.PINK.getName());
		assertSame(RichTextFeature.TEXT_ALIGN, RichTextTool.ALIGN_CENTER.getFeature());
		assertNull(RichTextTool.UNDO.getFeature());
		assertEquals("json", RichTextFormat.JSON.getName());
		assertEquals(EnumSet.allOf(RichTextFeature.class), RichTextFeature.defaults(
			RichTextFormat.HTML));
		assertEquals(EnumSet.allOf(RichTextFeature.class), RichTextFeature.defaults(
			RichTextFormat.JSON));
	}

	@Test
	void tellsTheLibraryItIsBasedOn()
	{
		RichTextLibrary library = RichTextLibrary.get();
		assertEquals("Tiptap", library.name());
		assertTrue(library.version().matches("[0-9]+\\.[0-9]+\\.[0-9]+.*"), library.version());
		assertEquals("https://tiptap.dev", library.url());
		assertEquals("Tiptap " + library.version(), library.toString());
	}

	@Test
	void extensionsAreKnownByTheirNames()
	{
		RichTextExtension extension = RichTextExtension.of(" Highlight ", null);
		assertEquals("highlight", extension.getName());
		assertNull(extension.getScript());
		assertEquals(RichTextExtension.of("HIGHLIGHT", null), extension);
		assertEquals(extension.hashCode(), RichTextExtension.of("highlight", null).hashCode());
		assertEquals("highlight", extension.toString());
		assertFalse(extension.equals("highlight"));
	}

	/**
	 * @return the options the editor was attached with in the last response
	 */
	private JSONObject options(RichTextEditor editor)
	{
		Matcher matcher = Pattern.compile("Wicket\\.RichTextEditor\\.attach\\(\"" +
			Pattern.quote(editor.getMarkupId()) + "\", (\\{.*\\})\\);").matcher(
				tester.getLastResponseAsString());
		assertTrue(matcher.find(), "the editor is attached");
		return new JSONObject(matcher.group(1));
	}

	private void sendState(RichTextEditor editor, String state, String text)
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

		final RichTextEditor editor;

		/** What the link does. */
		SerializableConsumer<AjaxRequestTarget> action = target -> {
		};

		TestPage(RichTextEditor editor)
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
