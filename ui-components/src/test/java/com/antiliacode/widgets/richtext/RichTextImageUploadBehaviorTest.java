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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.openjson.JSONObject;

class RichTextImageUploadBehaviorTest extends WicketTestCase
{
	/** The first bytes of a PNG, and some more. */
	private static final byte[] PNG = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0,
			13, 'I', 'H', 'D', 'R' };

	private static final byte[] GIF = "GIF89a and the rest".getBytes(StandardCharsets.US_ASCII);

	@TempDir
	Path folder;

	/** What the store of the tests kept: the name, the type and the bytes of each image. */
	private static final List<Object[]> KEPT = new ArrayList<>();

	private static String keep(org.apache.wicket.markup.html.form.upload.FileUpload image,
		RichTextImageType type)
	{
		KEPT.add(new Object[] { image.getClientFileName(), type, image.getBytes() });
		return "/images/" + KEPT.size() + "." + type.getExtension();
	}

	@Test
	void keepsAnImageAndAnswersItsAddress() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		tester.startPage(page);

		JSONObject answer = upload(page, "bird.png", PNG, "image/png");
		assertEquals("{\"url\":\"/images/1.png\"}", answer.toString());
		assertEquals(1, KEPT.size());
		assertEquals("bird.png", KEPT.get(0)[0]);
		assertEquals(RichTextImageType.PNG, KEPT.get(0)[1]);
		assertArrayEquals(PNG, (byte[])KEPT.get(0)[2]);
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));
	}

	@Test
	void tellsTheTypeByTheBytesNotByTheBrowser() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		tester.startPage(page);

		// a GIF the browser calls a PNG is a GIF
		assertEquals("/images/1.gif", upload(page, "bird.png", GIF, "image/png").getString("url"));
		assertEquals(RichTextImageType.GIF, KEPT.get(0)[1]);

		JSONObject refused = upload(page, "notes.png", "<svg onload=\"steal()\"/>".getBytes(
			StandardCharsets.UTF_8), "image/png");
		assertEquals("notes.png is not an image of a type the editor takes: PNG, JPEG, GIF, WebP.",
			refused.getString("error"));
		assertFalse(refused.has("url"));

		page.upload.setTypes(EnumSet.of(RichTextImageType.PNG));
		assertEquals("bird.gif is not an image of a type the editor takes: PNG.",
			upload(page, "bird.gif", GIF, "image/gif").getString("error"));
		assertEquals(1, KEPT.size());
	}

	@Test
	void refusesAnImageTooLarge() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		page.upload.setMaxSize(Bytes.kilobytes(1));
		tester.startPage(page);

		byte[] large = new byte[4096];
		System.arraycopy(PNG, 0, large, 0, PNG.length);
		assertEquals("large.png is larger than 1 KB.", upload(page, "large.png", large,
			"image/png").getString("error"));

		// the whole request beyond the limit, cut before its file is read
		page.upload.setMaxSize(Bytes.bytes(10));
		assertEquals("The image is larger than 10 B.", upload(page, "huge.png", new byte[64 * 1024],
			"image/png").getString("error"));
		assertTrue(KEPT.isEmpty());
	}

	@Test
	void refusesAnAddressTheEditorDoesNotTake() throws IOException
	{
		TestPage page = new TestPage((image, type) -> "javascript:steal()");
		tester.startPage(page);
		assertEquals("bird.png could not be uploaded.", upload(page, "bird.png", PNG, "image/png")
			.getString("error"));

		TestPage failing = new TestPage((image, type) -> {
			throw new IOException("The disk is full");
		});
		tester.startPage(failing);
		assertEquals("bird.png could not be uploaded.", upload(failing, "bird.png", PNG,
			"image/png").getString("error"));
	}

	@Test
	void refusesARequestWithoutAnImage()
	{
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		tester.startPage(page);
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().getPostParameters().setParameterValue("other", "value");
		tester.executeBehavior(page.upload);
		assertEquals("The image could not be uploaded.",
			new JSONObject(tester.getLastResponseAsString()).getString("error"));
	}

	@Test
	void saysWhyInTheLanguageOfThePage() throws IOException
	{
		tester.getSession().setLocale(Locale.of("es"));
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		tester.startPage(page);
		assertEquals("notas.png no es una imagen de un tipo que el editor acepte: PNG, JPEG, GIF, " +
			"WebP.", upload(page, "notas.png", new byte[20], "image/png").getString("error"));

		tester.getSession().setLocale(Locale.of("de"));
		page.upload.setMaxSize(Bytes.bytes(10));
		// the request is cut before its file is read: the image has no name yet
		assertEquals("Das Bild ist größer als 10 B.", upload(page, "huge.png", new byte[64 * 1024],
			"image/png").getString("error"));
	}

	@Test
	void takesImagesForTheEditorItIsOf() throws IOException
	{
		KEPT.clear();
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Text</p>"));
		editor.setImageStore(RichTextImageUploadBehaviorTest::keep);
		EditorPage page = new EditorPage(editor);
		tester.startPage(page);
		RichTextImageUploadBehavior behavior = editor.getBehaviors(
			RichTextImageUploadBehavior.class).get(0);

		assertEquals("/images/1.png", upload(behavior, "bird.png", PNG).getString("url"));

		// read only, the editor takes no images
		editor.setReadOnly(true);
		assertEquals("The image could not be uploaded.", upload(behavior, "bird.png", PNG)
			.getString("error"));
		editor.setReadOnly(false);
		editor.setFeatures(EnumSet.of(RichTextFeature.BOLD));
		assertTrue(upload(behavior, "bird.png", PNG).has("error"));
		editor.setFeatures(null);
		editor.setImageStore(null);
		assertTrue(upload(behavior, "bird.png", PNG).has("error"));
		assertEquals(1, KEPT.size());
	}

	@Test
	void hasLimitsAndTypesOfItsOwn()
	{
		RichTextImageUploadBehavior behavior = new RichTextImageUploadBehavior((image, type) -> "/a");
		assertEquals(RichTextImageUploadBehavior.DEFAULT_MAX_SIZE, behavior.getMaxSize());
		assertEquals(Bytes.megabytes(5), RichTextImageUploadBehavior.DEFAULT_MAX_SIZE);
		assertEquals(EnumSet.allOf(RichTextImageType.class), behavior.getTypes());
		assertEquals(List.of("image/png", "image/jpeg", "image/gif", "image/webp"),
			behavior.getMimeTypes());
		assertFalse(behavior.rendersPage());
		assertThrows(IllegalArgumentException.class, () -> behavior.setTypes(EnumSet.noneOf(
			RichTextImageType.class)));
		assertThrows(IllegalArgumentException.class, () -> new RichTextImageUploadBehavior(null));
	}

	@Test
	void saysSizesAsTheBrowserDoes()
	{
		assertEquals("512 B", RichTextImageUploadBehavior.sizeText(512));
		assertEquals("2 KB", RichTextImageUploadBehavior.sizeText(2048));
		assertEquals("1.5 MB", RichTextImageUploadBehavior.sizeText(1536 * 1024));
		assertEquals("5 MB", RichTextImageUploadBehavior.sizeText(Bytes.megabytes(5).bytes()));
		assertEquals("2 GB", RichTextImageUploadBehavior.sizeText(2L * 1024 * 1024 * 1024));
	}

	private JSONObject upload(TestPage page, String name, byte[] bytes, String contentType)
		throws IOException
	{
		return upload(page.upload, name, bytes, contentType);
	}

	private JSONObject upload(RichTextImageUploadBehavior behavior, String name, byte[] bytes)
		throws IOException
	{
		return upload(behavior, name, bytes, "image/png");
	}

	/**
	 * Posts a file to a behavior, as the editor does, and reads the answer.
	 */
	private JSONObject upload(RichTextImageUploadBehavior behavior, String name, byte[] bytes,
		String contentType) throws IOException
	{
		Path file = folder.resolve(name);
		Files.write(file, bytes);
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
			new org.apache.wicket.util.file.File(file.toFile()), contentType);
		tester.executeBehavior(behavior);
		return new JSONObject(tester.getLastResponseAsString());
	}

	/** A page taking images for the editors of React and Svelte in it. */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final RichTextImageUploadBehavior upload;

		TestPage(RichTextImageStore store)
		{
			upload = new RichTextImageUploadBehavior(store);
			add(upload);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body></body></html>");
		}
	}

	/** A page with an editor taking images. */
	public static class EditorPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		EditorPage(RichTextEditor editor)
		{
			Form<Void> form = new Form<>("form");
			form.add(editor);
			add(form);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"editor\"></textarea></form></body></html>");
		}
	}
}
