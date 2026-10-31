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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
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
import org.junit.jupiter.api.io.TempDir;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxEvent;
import com.antiliacode.widgets.event.AjaxSubmitEvent;
import com.antiliacode.widgets.event.ClientEvent;
import com.antiliacode.widgets.event.ClientEventsTester;
import com.antiliacode.widgets.event.IClientEvent;
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
		assertEquals("[\"bold\",\"italic\",\"underline\",\"strike\",\"code\",\"superscript\"," +
			"\"subscript\"]", toolbar.getJSONArray(1).toString());
		assertFalse(options.getBoolean("bubbleMenu"));
		assertFalse(options.getBoolean("readOnly"));
		assertFalse(options.getBoolean("count"));
		assertFalse(options.has("limit"));
		assertFalse(options.has("saved"));
		assertFalse(options.has("placeholder"));
		assertFalse(options.has("label"));
		assertFalse(options.has("spellcheck"), "the browser's default");
		assertFalse(options.has("textLanguage"), "the page's");
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
	void hasTheServerCheckTheTextWithAProofreader()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.setProofreader((texts, language) -> List.of());
		tester.startPage(new TestPage(editor));
		JSONObject options = options(editor);
		assertTrue(options.getString("proofreadUrl").endsWith("-form-editor"));
		assertEquals("Spelling", options.getJSONObject("texts").getString("proofreadSpelling"));

		RichTextEditor readOnly = new RichTextEditor("editor", Model.of(HTML));
		readOnly.setProofreader((texts, language) -> List.of()).setReadOnly(true);
		tester.startPage(new TestPage(readOnly));
		assertFalse(options(readOnly).has("proofreadUrl"));
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
			.setSavedModel(Model.of("Before")).setSpellcheck(false)
			.setTextLanguage(Locale.forLanguageTag("en-GB"));
		editor.setLabel(Model.of("Article"));
		tester.startPage(new TestPage(editor));

		JSONObject options = options(editor);
		assertEquals("markdown", options.getString("format"));
		assertFalse(options.getBoolean("spellcheck"));
		assertEquals("en-GB", options.getString("textLanguage"));
		assertEquals(Boolean.FALSE, editor.getSpellcheck());
		assertEquals(Locale.UK, editor.getTextLanguage());
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
	void sendsTheLimitsOfTheUploadsAndWhereToTellOfTheImagesTakenOut()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML))
			.setImageStore((image, type) -> "/images/a.png");
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		JSONObject options = options(editor);
		assertFalse(options.has("uploadMaxWidth"));
		assertFalse(options.has("uploadScaling"));
		assertFalse(options.has("uploadMaxCount"));
		assertFalse(options.has("uploadDialog"));
		assertTrue(options.getString("imagesUrl").contains("-form-editor"));
		assertFalse(tester.getLastResponseAsString().contains("antilia-modal"));
		assertEquals(RichTextImageDeletion.ON_SAVE, editor.getImageDeletion());
		assertNull(editor.getImageMaxDimensions());
		assertFalse(editor.isImageScaling());
		assertFalse(editor.isImageUploadDialog());
		assertNull(editor.getImageMaxCount());

		editor.setImageMaxDimensions(800, 600).setImageScaling(true).setImageMaxCount(3)
			.setImageUploadDialog(true);
		tester.startPage(page);
		options = options(editor);
		assertEquals(800, options.getInt("uploadMaxWidth"));
		assertEquals(600, options.getInt("uploadMaxHeight"));
		assertTrue(options.getBoolean("uploadScaling"));
		assertEquals(3, options.getInt("uploadMaxCount"));
		assertTrue(options.getBoolean("uploadDialog"));
		tester.assertContains("antilia-modal");
		assertEquals(new RichTextImageDimensions(800, 600), editor.getImageMaxDimensions());
		assertEquals(new RichTextImageDimensions(800, 600), editor.getBehaviors(
			RichTextImageUploadBehavior.class).get(0).getMaxDimensions());

		assertEquals(1, editor.setImageMaxCount(0).getImageMaxCount());
		assertNull(editor.setImageMaxCount(null).getImageMaxCount());
		editor.setImageMaxDimensions(null);
		assertNull(editor.getBehaviors(RichTextImageUploadBehavior.class).get(0)
			.getMaxDimensions());
		assertThrows(IllegalArgumentException.class, () -> editor.setImageDeletion(null));
		assertThrows(IllegalArgumentException.class, () -> editor.setImageTypes(List.of()));
	}

	/** A store keeping nothing, which notes the images it was asked to delete. */
	private static IRichTextImageStore deleting(List<String> deleted)
	{
		return new IRichTextImageStore()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String store(FileUpload image, IRichTextImageType type)
			{
				return "/images/new." + type.getExtension();
			}

			@Override
			public void delete(String url)
			{
				deleted.add(url);
			}
		};
	}

	/** An editor noting what it heard of the images taken out of its text. */
	private static RichTextEditor noting(IModel<String> text, List<Set<String>> heard)
	{
		return new RichTextEditor("editor", text)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onImagesRemoved(AjaxRequestTarget target, Set<String> urls)
			{
				heard.add(urls);
			}
		};
	}

	private static final String IMAGES = "<p>a</p><img src=\"/images/old.png\"><img " +
		"src=\"/images/kept.png\">";

	@Test
	void deletesTheImagesTakenOutOnceTheTextIsSaved(@TempDir Path folder) throws IOException
	{
		List<String> deleted = new ArrayList<>();
		List<Set<String>> heard = new ArrayList<>();
		List<RichTextImagesRemovedEvent> events = new ArrayList<>();
		IModel<String> text = Model.of(IMAGES);
		RichTextEditor editor = noting(text, heard).setImageStore(deleting(deleted));
		TestPage page = new TestPage(editor)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(IEvent<?> event)
			{
				if (event.getPayload() instanceof RichTextImagesRemovedEvent removed)
				{
					events.add(removed);
				}
			}
		};
		tester.startPage(page);
		AbstractDefaultAjaxBehavior removal = removal(editor);

		// one the editor knows, one it does not
		removeImages(removal, "[\"/images/old.png\", \"/images/elsewhere.png\"]");
		assertEquals(List.of(Set.of("/images/old.png")), heard);
		assertEquals(1, events.size());
		assertEquals(Set.of("/images/old.png"), events.get(0).getUrls());
		assertFalse(events.get(0).isDeleted());
		assertSame(editor, events.get(0).getEditor());
		assertTrue(deleted.isEmpty(), "not before the text is saved");

		// an image uploaded, which never went into the text
		uploadImage(editor, folder);
		assertEquals(List.of("/images/new.png"), List.copyOf(editor.getBehaviors(
			RichTextImageUploadBehavior.class).get(0).getUploaded()));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p>a</p><img src=\"/images/kept.png\">");
		form.submit();
		assertEquals("<p>a</p><img src=\"/images/kept.png\" />", text.getObject());
		assertTrue(deleted.isEmpty(), "the model has the text, which is not saved yet");

		page.action = target -> editor.markSaved(target);
		tester.clickLink("act");
		assertEquals(List.of("/images/old.png", "/images/new.png"), deleted);

		// saved again, nothing more to delete
		tester.clickLink("act");
		assertEquals(List.of("/images/old.png", "/images/new.png"), deleted);
	}

	@Test
	void deletesTheImagesTakenOutOnceTheTextIsSavedWithoutAjax()
	{
		List<String> deleted = new ArrayList<>();
		IModel<String> text = Model.of(IMAGES);
		RichTextEditor editor = new RichTextEditor("editor", text).setImageStore(deleting(deleted));
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		removeImages(removal(editor), "[\"/images/kept.png\"]");
		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p>a</p><img src=\"/images/old.png\">");
		form.submit();
		assertSame(editor, editor.deleteRemovedImages());
		assertEquals(List.of("/images/kept.png"), deleted);
		// none without a store
		assertSame(editor, editor.setImageStore(null).deleteRemovedImages());
	}

	@Test
	void keepsAnImageTheTextHoldsAgainOnceSaved()
	{
		List<String> deleted = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(IMAGES))
			.setImageStore(deleting(deleted));
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		removeImages(removal(editor), "[\"/images/old.png\"]");

		// Ctrl+Z brought it back
		FormTester form = tester.newFormTester("form");
		form.setValue("editor", IMAGES);
		form.submit();
		page.action = target -> editor.markSaved(target);
		tester.clickLink("act");
		assertTrue(deleted.isEmpty());
	}

	@Test
	void forgetsTheImagesTakenOutOfAnotherTextThanThePagePutsInTheModel()
	{
		List<String> deleted = new ArrayList<>();
		IModel<String> text = Model.of(IMAGES);
		RichTextEditor editor = new RichTextEditor("editor", text).setImageStore(deleting(deleted));
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		removeImages(removal(editor), "[\"/images/old.png\"]");

		text.setObject("<p>Another text</p>");
		tester.startPage(page);
		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p>Another text, changed</p>");
		form.submit();
		page.action = target -> editor.markSaved(target);
		tester.clickLink("act");
		assertTrue(deleted.isEmpty());

		// put in the model without the editor shown again
		removeImages(removal(editor), "[\"/images/old.png\"]");
		text.setObject("<p>A third text</p>");
		tester.clickLink("act");
		assertTrue(deleted.isEmpty());
	}

	@Test
	void deletesTheImagesTakenOutAtOnceWhenTold()
	{
		List<String> deleted = new ArrayList<>();
		List<Set<String>> heard = new ArrayList<>();
		RichTextEditor editor = noting(Model.of(IMAGES), heard).setImageStore(deleting(deleted))
			.setImageDeletion(RichTextImageDeletion.AT_ONCE);
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		AbstractDefaultAjaxBehavior removal = removal(editor);

		removeImages(removal, "[\"/images/old.png\"]");
		assertEquals(List.of("/images/old.png"), deleted);
		assertEquals(List.of(Set.of("/images/old.png")), heard);
		// deleted already: told again, as Ctrl+Z and Delete do, it is not known any more
		removeImages(removal, "[\"/images/old.png\"]");
		assertEquals(List.of("/images/old.png"), deleted);
		assertEquals(1, heard.size());

		// read only, the editor hears of none
		editor.setReadOnly(true);
		removeImages(removal, "[\"/images/kept.png\"]");
		assertEquals(List.of("/images/old.png"), deleted);
	}

	@Test
	void letsThePageDecideWhichImagesToDelete()
	{
		List<String> deleted = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(IMAGES))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void deleteImages(Set<String> urls)
			{
				urls.stream().filter(url -> !url.contains("kept")).forEach(deleted::add);
			}
		};
		editor.setImageStore(deleting(new ArrayList<>()))
			.setImageDeletion(RichTextImageDeletion.AT_ONCE);
		tester.startPage(new TestPage(editor));
		removeImages(removal(editor), "[\"/images/old.png\", \"/images/kept.png\"]");
		assertEquals(List.of("/images/old.png"), deleted);
	}

	@Test
	void keepsImagesPermanentlyAndNeverDeletesThem()
	{
		List<String> deleted = new ArrayList<>();
		List<Set<String>> heard = new ArrayList<>();
		IRichTextImageStore store = new IRichTextImageStore()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String store(FileUpload image, IRichTextImageType type)
			{
				return "/images/new.png";
			}

			@Override
			public void delete(String url)
			{
				deleted.add(url);
			}

			@Override
			public boolean isPermanent(String url)
			{
				return url.contains("kept");
			}

			@Override
			public List<RichTextStoredImage> findPermanent(String query, int max)
			{
				return List.of(new RichTextStoredImage("/images/kept.png", "Kept"));
			}
		};
		RichTextEditor editor = noting(Model.of(IMAGES), heard).setImageStore(store)
			.setImageDeletion(RichTextImageDeletion.AT_ONCE);
		TestPage page = new TestPage(editor);
		tester.startPage(page);
		assertFalse(editor.isPermanentImages());
		assertFalse(options(editor).has("permanentImages"));
		RichTextImageUploadBehavior behavior = editor.getBehaviors(RichTextImageUploadBehavior.class)
			.get(0);
		assertFalse(behavior.isPermanentImages());

		editor.setPermanentImages(true);
		tester.startPage(page);
		assertTrue(options(editor).getBoolean("permanentImages"));
		assertTrue(behavior.isPermanentImages());
		// the store of the page, through the editor's
		assertEquals(List.of(new RichTextStoredImage("/images/kept.png", "Kept")),
			behavior.findPermanent(""));

		// one kept permanently is neither deleted nor told of
		removeImages(removal(editor), "[\"/images/old.png\", \"/images/kept.png\"]");
		assertEquals(List.of("/images/old.png"), deleted);
		assertEquals(List.of(Set.of("/images/old.png")), heard);

		// read only, nothing found
		editor.setReadOnly(true);
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextImageUploadBehavior.FIND_PARAMETER, "");
		tester.executeBehavior(behavior);
		assertEquals("{\"images\":[]}", tester.getLastResponseAsString());
		assertTrue(RichTextTool.DEFAULT_TOOLBAR.stream().anyMatch(group -> group.contains(
			RichTextTool.STORED_IMAGE)));
	}

	@Test
	void readsTheImagesOfHtmlMarkdownAndJson()
	{
		assertEquals(List.of("/a.png", "https://example.org/b?x=1&y=2", "c.png"),
			List.copyOf(RichTextImageAddresses.of("<p>a<img alt=\"x\" src=\"/a.png\"></p>" +
				"<IMG SRC='https://example.org/b?x=1&amp;y=2'><img src=c.png><img alt=\"none\">" +
				"<p>&lt;img src=\"/text.png\"&gt;</p><img src=\"/a.png\">", RichTextFormat.HTML)));
		assertEquals(List.of("/a.png", "/b c.png", "/c.png"), List.copyOf(RichTextImageAddresses
			.of("Text ![A bird](/a.png \"Title\") and ![B](</b c.png>)\n\n<img src=\"/c.png\">",
				RichTextFormat.MARKDOWN)));
		assertEquals(List.of("/a.png"), List.copyOf(RichTextImageAddresses.of("{\"type\":\"doc\"," +
			"\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":" +
			"\"src\"}]},{\"type\":\"image\",\"attrs\":{\"src\":\"/a.png\"}}]}",
			RichTextFormat.JSON)));
		assertTrue(RichTextImageAddresses.of("not json", RichTextFormat.JSON).isEmpty());
		assertTrue(RichTextImageAddresses.of(null, RichTextFormat.HTML).isEmpty());
		assertEquals("<a & \"b\" 'c' é>", RichTextImageAddresses.decode(
			"&lt;a &amp; &quot;b&quot; &#39;c&apos; &#xe9;&gt;"));
	}

	/**
	 * @return the behavior of an editor hearing of the images taken out of its text
	 */
	private static AbstractDefaultAjaxBehavior removal(RichTextEditor editor)
	{
		return editor.getImagesRemoval();
	}

	/**
	 * Tells an editor of images taken out of its text, as its script does.
	 */
	private void removeImages(AbstractDefaultAjaxBehavior removal, String json)
	{
		tester.getRequest().getPostParameters().setParameterValue("removed", json);
		tester.executeBehavior(removal);
	}

	/**
	 * Uploads an image into an editor, as its script does.
	 */
	private void uploadImage(RichTextEditor editor, Path folder) throws IOException
	{
		Path file = folder.resolve("bird.png");
		Files.write(file, new byte[] { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0,
				13, 'I', 'H', 'D', 'R' });
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
			new org.apache.wicket.util.file.File(file.toFile()), "image/png");
		tester.executeBehavior(editor.getBehaviors(RichTextImageUploadBehavior.class).get(0));
		assertEquals("/images/new.png", new JSONObject(tester.getLastResponseAsString())
			.getString("url"));
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
	void cleansTheAddressesOfTheLinksOfMarkdown()
	{
		IModel<String> text = Model.of("");
		RichTextEditor editor = new RichTextEditor("editor", text)
			.setFormat(RichTextFormat.MARKDOWN);
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "[a](javascript:steal()) and [b](https://example.com)");
		form.submit();
		assertEquals("[a](#) and [b](https://example.com)", text.getObject());
		assertSame(RichTextMarkdownSanitizer.INSTANCE, editor.getMarkdownSanitizer());
	}

	@Test
	void cleansTheLinksOfJsonAndTakesNoTextThatIsNoDocument()
	{
		IModel<String> text = Model.of("");
		RichTextEditor editor = new RichTextEditor("editor", text).setFormat(RichTextFormat.JSON);
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "{\"type\":\"doc\",\"content\":[{\"type\":\"image\"," +
			"\"attrs\":{\"src\":\"javascript:steal()\"}}]}");
		form.submit();
		assertEquals("{\"type\":\"doc\",\"content\":[]}", text.getObject());

		form = tester.newFormTester("form");
		form.setValue("editor", "<p>no JSON</p>");
		form.submit();
		assertNull(text.getObject());
		assertSame(RichTextJsonSanitizer.INSTANCE, editor.getJsonSanitizer());
	}

	@Test
	void keepsTheImagesOfTheSiteAloneWhenAsked()
	{
		IModel<String> text = Model.of("");
		RichTextEditor editor = new RichTextEditor("editor", text);
		assertFalse(editor.isImagesFromSiteOnly());
		assertSame(RichTextMarkdownSanitizer.INSTANCE, editor.getMarkdownSanitizer());
		assertSame(RichTextJsonSanitizer.INSTANCE, editor.getJsonSanitizer());
		assertSame(RichTextPolicy.ALL, editor.getSanitizer());

		editor.setImagesFromSiteOnly(true);
		assertTrue(editor.isImagesFromSiteOnly());
		assertEquals(RichTextPolicy.ALL.withImagesFromSiteOnly(true), editor.getSanitizer());
		assertSame(RichTextMarkdownSanitizer.IMAGES_FROM_SITE_ONLY, editor.getMarkdownSanitizer());
		assertSame(RichTextJsonSanitizer.IMAGES_FROM_SITE_ONLY, editor.getJsonSanitizer());
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "<p><img src=\"https://example.org/a.png\" alt=\"A\" />" +
			"<img src=\"/a.png\" alt=\"B\" /> <a href=\"https://example.org\">a link</a></p>");
		form.submit();
		assertEquals("<p><img src=\"/a.png\" alt=\"B\" /> <a href=\"https://example.org\" " +
			"rel=\"noopener noreferrer nofollow\">a link</a></p>", text.getObject());

		editor.setFormat(RichTextFormat.MARKDOWN);
		form = tester.newFormTester("form");
		form.setValue("editor", "![a](//example.org/a.png) ![b](/b.png) [c](https://example.org)");
		form.submit();
		assertEquals("![a](#) ![b](/b.png) [c](https://example.org)", text.getObject());

		editor.setFormat(RichTextFormat.JSON);
		form = tester.newFormTester("form");
		form.setValue("editor", "{\"type\":\"doc\",\"content\":[{\"type\":\"image\"," +
			"\"attrs\":{\"src\":\"https://example.org/a.png\"}}]}");
		form.submit();
		assertEquals("{\"type\":\"doc\",\"content\":[]}", text.getObject());

		// a sanitizer of the page cleans as it does
		IRichTextSanitizer own = html -> html;
		assertSame(own, editor.setSanitizer(own).getSanitizer());
	}

	@Test
	void cleansMarkdownAndJsonWithTheSanitizersOfThePage()
	{
		IModel<String> text = Model.of("");
		RichTextEditor editor = new RichTextEditor("editor", text)
			.setFormat(RichTextFormat.MARKDOWN)
			.setMarkdownSanitizer(markdown -> markdown.replace("<b>", "").replace("</b>", ""))
			.setJsonSanitizer(json -> "{}");
		tester.startPage(new TestPage(editor));

		FormTester form = tester.newFormTester("form");
		form.setValue("editor", "Some <b>HTML</b>");
		form.submit();
		assertEquals("Some HTML", text.getObject());
		assertEquals("{}", editor.getJsonSanitizer().sanitize("[]"));
		assertSame(RichTextMarkdownSanitizer.INSTANCE,
			editor.setMarkdownSanitizer(null).getMarkdownSanitizer());
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
		tester.assertContains(
			"e.markSaved\\(\"\\\\u003cp\\\\u003enew\\\\u003c\\\\/p\\\\u003e\"\\);");
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
		List<String> sizes = new ArrayList<>();
		IModel<String> text = Model.of(HTML);
		RichTextEditor editor = new RichTextEditor("editor", text);
		editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(String eventId, JSONObject values, Form<?> form,
				AjaxRequestTarget target)
			{
				saved.add(editor.getModelObject());
				editor.markSaved(target);
			}
		});
		editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.INPUT, (eventId, values, form,
			target) -> {
		}).setDelay(RichTextEditorEvents.TYPING_DELAY));
		editor.addEvent(AjaxEvent.of(RichTextEditorEvents.RESIZE, (eventId, values,
			target) -> sizes.add(RichTextEditorEvents.sizeOf(values).toString())));
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		tester.assertContains("\\{id:\"antilia-rich-text-editor:save\",index:0,kind:'submit'," +
			"delay:-1\\}");
		// typing waits until the user paused
		tester.assertContains("\\{id:\"input\",index:1,kind:'submit',delay:500\\}");
		tester.assertContains("\\{id:\"antilia-rich-text-editor:resize\",index:2,kind:'ajax'," +
			"delay:-1\\}");
		// the functions get the editor
		tester.assertContains("function\\(element\\)\\{return Wicket.RichTextEditor&&" +
			"Wicket.RichTextEditor.of\\(element.id\\);\\}\\);");
		assertEquals(3, editor.getEvents().size());

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"<p>Saved <img src=x onerror=steal()></p>");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
		assertEquals(List.of("<p>Saved <img src=\"x\" /></p>"), saved);
		assertEquals("<p>Saved <img src=\"x\" /></p>", text.getObject());

		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.RESIZE, new JSONObject()
			.put("size", new JSONObject().put("width", 640.4).put("height", 320)));
		assertEquals(List.of(ResizableSize.of(640, 320).toString()), sizes);
		assertEquals(ResizableSize.NONE, RichTextEditorEvents.sizeOf(new JSONObject()));
		assertEquals(ResizableSize.height(200), RichTextEditorEvents.sizeOf(new JSONObject()
			.put("size", new JSONObject().put("width", JSONObject.NULL).put("height", "200"))));
	}

	@Test
	void hearsOfTheSizeInPixels()
	{
		List<String> sizes = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -> sizes.add(width +
			"x" + height)));
		tester.startPage(new TestPage(editor));

		// the browser measures the dimension the user does not resize
		tester.assertContains("\\{id:\"antilia-rich-text-editor:resize\",index:0,kind:'ajax'," +
			"delay:-1,fn:function\\(event,editor,send\\)");
		tester.assertContains("getBoundingClientRect");

		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.RESIZE, new JSONObject()
			.put("width", 640.4).put("height", 320));
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.RESIZE, new JSONObject()
			.put("width", -5).put("height", "200"));
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.RESIZE, new JSONObject()
			.put("width", "wide").put("height", JSONObject.NULL));
		assertEquals(List.of("640x320", "-1x200", "-1x-1"), sizes);
		assertEquals(-1, RichTextEditorResizeEvent.UNKNOWN);
	}

	@Test
	void hearsOfTheTextSelected()
	{
		List<String> heard = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.addEvent(RichTextEditorSelectedTextEvent.of((selectedText, target) -> heard.add(
			"[" + selectedText + "]")).setDelay(RichTextEditorEvents.TYPING_DELAY));
		tester.startPage(new TestPage(editor));

		// a built-in type of events of the editors: the browser sends what it tells
		tester.assertContains("\\{id:\"antilia-rich-text-editor:selected-text\",index:0," +
			"kind:'ajax',delay:500\\}");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SELECTED_TEXT,
			new JSONObject().put("text", "Hello\nworld"));
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SELECTED_TEXT,
			new JSONObject().put("text", ""));
		// what is not a text: none
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SELECTED_TEXT,
			new JSONObject().put("text", 42));
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SELECTED_TEXT,
			new JSONObject());
		assertEquals(List.of("[Hello\nworld]", "[]", "[]", "[]"), heard);
	}

	@Test
	void rendersTheScriptOfATypeOfEventsAfterTheEditorsAndBeforeListening()
	{
		List<String> heard = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.addEvent(new HeadingsEvent(heard));
		tester.startPage(new TestPage(editor));

		// Wicket.RichTextEvents is there, and the type before the editor listens to it
		String page = tester.getLastResponseAsString();
		int script = page.indexOf("RichTextEditor/antilia-rich-text-editor.js");
		int registered = page.indexOf("Wicket.RichTextEvents.register('antilia-test:headings'");
		int listening = page.indexOf("Wicket.ClientEvents.attach(");
		assertTrue(script >= 0 && script < registered && registered < listening, page);
		ClientEventsTester.fire(tester, editor, HeadingsEvent.NAME, new JSONObject().put(
			"headings", new JSONArray().put("One").put(2).put("Two")));
		assertEquals(List.of("One", "Two"), heard);
	}

	@Test
	void hearsOfTheTextBeforeAndNow()
	{
		List<String> heard = new ArrayList<>();
		IModel<String> text = Model.of(HTML);
		RichTextEditor editor = new RichTextEditor("editor", text);
		editor.setRequired(true);
		editor.addEvent(new RichTextEditorChangeEvent(editor, RichTextEditorEvents.BLUR)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChange(String previousText, String currentText,
				AjaxRequestTarget target)
			{
				heard.add(previousText + " -> " + currentText);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				heard.add("error");
			}
		});
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		// the text as the text area posts it, cleaned
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"<p>New <img src=x onerror=steal()></p>");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.BLUR);
		assertEquals(List.of(HTML + " -> <p>New <img src=\"x\" /></p>"), heard);
		assertEquals("<p>New <img src=\"x\" /></p>", text.getObject());

		// required, but empty: the model keeps its text
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), "");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.BLUR);
		assertEquals("error", heard.get(1));
		assertEquals("<p>New <img src=\"x\" /></p>", text.getObject());

		// no text with the event, or reading only: nothing
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.BLUR);
		editor.setReadOnly(true);
		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(),
			"<p>Other</p>");
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.BLUR);
		assertEquals(2, heard.size());
		assertEquals("<p>New <img src=\"x\" /></p>", text.getObject());
		assertSame(editor, RichTextEditorChangeEvent.of(editor, RichTextEditorEvents.CHANGE,
			(previousText, currentText, target) -> {
			}).getEditor());
	}

	@Test
	void hasEachTextInEachBundle() throws java.io.IOException
	{
		for (String suffix : new String[] { "", "_de", "_es" })
		{
			java.util.Properties bundle = new java.util.Properties();
			try (java.io.InputStream in = com.antiliacode.widgets.Initializer.class
				.getResourceAsStream("Initializer" + suffix + ".properties.xml"))
			{
				bundle.loadFromXML(in);
			}
			for (String name : RichTextEditor.TEXT_NAMES)
			{
				assertTrue(bundle.containsKey("RichTextEditor." + name), name + " in " + suffix);
			}
		}
		// English without a component, in the language of the component with one
		assertEquals("Redo", RichTextEditor.text(null, "redo"));
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage(editor));
		assertEquals("Wiederholen", RichTextEditor.texts(editor).getString("redo"));
		assertEquals("Redo", RichTextEditor.text(null, "redo"));
		assertEquals("unknown", RichTextEditor.text(editor, "unknown"));
	}

	@Test
	void listensToTheEventsOfItsFeatures()
	{
		List<String> heard = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		editor.addEvent(new ClientEvent(RichTextEditorEvents.MODIFIED,
			"function(event){document.title=event.detail.modified?'*':'';}"));
		editor.addFeature(new EventFeature(heard));
		tester.startPage(new TestPage(editor));

		// the editor's own first, then those of its features
		tester.assertContains("\\{id:\"antilia-rich-text-editor:modified\",index:0," +
			"kind:'client',delay:-1,fn:function\\(event\\)");
		tester.assertContains("\\{id:\"antilia-own:inserted\",index:1,kind:'ajax',delay:-1\\}");
		ClientEventsTester.fire(tester, editor, "antilia-own:inserted", new JSONObject().put(
			"what", "kbd"));
		assertEquals(List.of("antilia-own:inserted kbd"), heard);

		editor.removeFeature(editor.getFeatures().stream().filter(feature -> feature
			.getName().equals("own")).findFirst().orElseThrow());
		assertEquals(1, editor.getEvents().size());
	}

	@Test
	void isATextArea()
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of(HTML));
		// a tag without a body, which TextArea would not check
		assertThrows(MarkupException.class, () -> tester.startPage(new TestPage(editor,
			"<div wicket:id=\"editor\"/>")));
	}

	/** A type of events of a page's own, whose script registers it in the browser. */
	private static final class HeadingsEvent extends AjaxEvent
	{
		private static final long serialVersionUID = 1L;

		static final String NAME = "antilia-test:headings";

		private final List<String> heard;

		HeadingsEvent(List<String> heard)
		{
			super(NAME);
			this.heard = heard;
		}

		@Override
		public void renderHead(Component component, IHeaderResponse response)
		{
			response.render(JavaScriptHeaderItem.forScript("Wicket.RichTextEvents.register('" +
				NAME + "',{from:[Wicket.RichTextEvents.events.text]});", NAME));
		}

		@Override
		public void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
		{
			JSONArray headings = values.optJSONArray("headings");
			for (int index = 0; headings != null && index < headings.length(); index++)
			{
				if (headings.opt(index) instanceof String heading)
				{
					heard.add(heading);
				}
			}
		}
	}

	/** A feature of a page's own, which a script fires an event of. */
	private record EventFeature(List<String> heard) implements IRichTextFeature
	{
		@Override
		public String getName()
		{
			return "own";
		}

		@Override
		public List<? extends IClientEvent> getEvents(org.apache.wicket.Component editor)
		{
			return List.of(AjaxEvent.of("antilia-own:inserted", (eventId, values,
				target) -> heard.add(eventId + " " + values.optString("what"))));
		}
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
		assertEquals("input", RichTextEditorEvents.INPUT);
		assertEquals("change", RichTextEditorEvents.CHANGE);
		assertEquals("antilia-rich-text-editor:blur", RichTextEditorEvents.BLUR);
		assertEquals("antilia-rich-text-editor:focus", RichTextEditorEvents.FOCUS);
		assertEquals("antilia-rich-text-editor:resize", RichTextEditorEvents.RESIZE);
		assertEquals("antilia-rich-text-editor:modified", RichTextEditorEvents.MODIFIED);
		assertEquals("antilia-rich-text-editor:select", RichTextEditorEvents.SELECT);
		assertEquals("antilia-rich-text-editor:save", RichTextEditorEvents.SAVE);
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
		assertEquals("superscript", RichTextTool.SUPERSCRIPT.getName());
		assertSame(RichTextFeature.SUBSCRIPT, RichTextTool.SUBSCRIPT.getFeature());
		// Markdown has no syntax for them
		assertFalse(RichTextFeature.defaults(RichTextFormat.MARKDOWN).contains(
			RichTextFeature.SUPERSCRIPT));
		assertFalse(RichTextFeature.defaults(RichTextFormat.MARKDOWN).contains(
			RichTextFeature.SUBSCRIPT));
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

		/** The tag of the editor. */
		private final String tag;

		TestPage(RichTextEditor editor)
		{
			this(editor, "<textarea wicket:id=\"editor\"></textarea>");
		}

		TestPage(RichTextEditor editor, String tag)
		{
			this.editor = editor;
			this.tag = tag;
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
				tag + "</form><a wicket:id=\"act\">Act</a>" +
				"</body></html>");
		}
	}
}
