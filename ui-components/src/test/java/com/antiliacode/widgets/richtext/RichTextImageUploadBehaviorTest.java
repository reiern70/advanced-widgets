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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
		IRichTextImageType type)
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
		assertEquals("notes.png is not an image of a type the editor takes: PNG, JPEG, GIF, WebP, " +
			"AVIF.", refused.getString("error"));
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
			"WebP, AVIF.", upload(page, "notas.png", new byte[20], "image/png").getString("error"));

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
	void refusesAnAddressOfAnotherSiteForAnEditorKeepingTheImagesOfTheSite() throws IOException
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Text</p>"));
		editor.setImageStore((image, type) -> "https://images.example.org/a.png");
		tester.startPage(new EditorPage(editor));
		RichTextImageUploadBehavior behavior = editor.getBehaviors(
			RichTextImageUploadBehavior.class).get(0);
		assertEquals("https://images.example.org/a.png", upload(behavior, "bird.png", PNG)
			.getString("url"));

		editor.setImagesFromSiteOnly(true);
		assertEquals("bird.png could not be uploaded.", upload(behavior, "bird.png", PNG)
			.getString("error"));
		editor.setImageStore((image, type) -> "/images/a.png");
		assertEquals("/images/a.png", upload(editor.getBehaviors(
			RichTextImageUploadBehavior.class).get(0), "bird.png", PNG).getString("url"));
	}

	@Test
	void hasLimitsAndTypesOfItsOwn()
	{
		RichTextImageUploadBehavior behavior = new RichTextImageUploadBehavior((image, type) -> "/a");
		assertEquals(RichTextImageUploadBehavior.DEFAULT_MAX_SIZE, behavior.getMaxSize());
		assertEquals(Bytes.megabytes(5), RichTextImageUploadBehavior.DEFAULT_MAX_SIZE);
		assertEquals(List.copyOf(RichTextImageType.ALL), List.copyOf(behavior.getTypes()));
		assertEquals(List.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/avif"),
			behavior.getMimeTypes());
		assertNull(behavior.getMaxDimensions());
		assertFalse(behavior.rendersPage());
		assertThrows(IllegalArgumentException.class, () -> behavior.setTypes(EnumSet.noneOf(
			RichTextImageType.class)));
		assertThrows(IllegalArgumentException.class, () -> behavior.setTypes(
			java.util.Arrays.asList(RichTextImageType.PNG, null)));
		assertThrows(IllegalArgumentException.class, () -> new RichTextImageUploadBehavior(null));
	}

	/** A PNG whose header says its width and height. */
	private static byte[] png(int width, int height)
	{
		byte[] bytes = new byte[33];
		System.arraycopy(PNG, 0, bytes, 0, PNG.length);
		java.nio.ByteBuffer.wrap(bytes).putInt(16, width).putInt(20, height);
		return bytes;
	}

	@Test
	void refusesAPictureWiderOrHigherThanAllowedByItsHeader() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		page.upload.setMaxDimensions(new RichTextImageDimensions(100, 100));
		tester.startPage(page);

		assertEquals("wide.png is 200 × 50 pixels, larger than 100 × 100.", upload(page,
			"wide.png", png(200, 50), "image/png").getString("error"));
		assertEquals("/images/1.png", upload(page, "small.png", png(100, 80), "image/png")
			.getString("url"));
		// a header that does not tell them
		assertEquals("odd.png could not be uploaded.", upload(page, "odd.png", PNG, "image/png")
			.getString("error"));

		tester.getSession().setLocale(Locale.GERMAN);
		assertEquals("wide.png hat 200 × 50 Pixel, mehr als 100 × 100.", upload(page, "wide.png",
			png(200, 50), "image/png").getString("error"));
		assertEquals(1, KEPT.size());

		page.upload.setMaxDimensions(null);
		assertTrue(upload(page, "wide.png", png(200, 50), "image/png").has("url"));
	}

	/** A type of the application's own: BMP, which starts with BM. */
	private static final IRichTextImageType BMP = new IRichTextImageType()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getMimeType()
		{
			return "image/bmp";
		}

		@Override
		public String getExtension()
		{
			return "bmp";
		}

		@Override
		public String getLabel()
		{
			return "BMP";
		}

		@Override
		public boolean matches(byte[] head)
		{
			return head.length >= 2 && head[0] == 'B' && head[1] == 'M';
		}
	};

	@Test
	void takesTypesOfTheApplicationsOwn() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		page.upload.setTypes(List.of(BMP, RichTextImageType.PNG));
		tester.startPage(page);

		assertEquals(List.of("image/bmp", "image/png"), page.upload.getMimeTypes());
		assertEquals("/images/1.bmp", upload(page, "map.bmp", "BM and the rest".getBytes(
			StandardCharsets.US_ASCII), "image/bmp").getString("url"));
		assertEquals(BMP, KEPT.get(0)[1]);
		assertEquals("bird.gif is not an image of a type the editor takes: BMP, PNG.",
			upload(page, "bird.gif", GIF, "image/gif").getString("error"));

		// a limit of pixels refuses a type that cannot tell them
		page.upload.setMaxDimensions(new RichTextImageDimensions(100, 100));
		assertEquals("map.bmp could not be uploaded.", upload(page, "map.bmp", "BM and the rest"
			.getBytes(StandardCharsets.US_ASCII), "image/bmp").getString("error"));
	}

	@Test
	void deletesOnlyTheImagesItTookWhenAsked() throws IOException
	{
		List<String> deleted = new ArrayList<>();
		IRichTextImageStore store = new IRichTextImageStore()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String store(org.apache.wicket.markup.html.form.upload.FileUpload image,
				IRichTextImageType type)
			{
				return keep(image, type);
			}

			@Override
			public void delete(String url)
			{
				deleted.add(url);
			}
		};
		KEPT.clear();
		TestPage page = new TestPage(store);
		tester.startPage(page);
		upload(page, "one.png", PNG, "image/png");
		upload(page, "two.png", PNG, "image/png");
		assertEquals(List.of("/images/1.png", "/images/2.png"), List.copyOf(page.upload
			.getUploaded()));
		assertTrue(page.upload.isUploaded(" /images/1.png "));

		assertEquals("{\"deleted\":[\"/images/1.png\"]}", remove(page, "[\"/images/1.png\", " +
			"\"/elsewhere.png\", 3]").toString());
		assertEquals(List.of("/images/1.png"), deleted);
		assertFalse(page.upload.isUploaded("/images/1.png"));
		// deleted already
		assertEquals("{\"deleted\":[]}", remove(page, "[\"/images/1.png\"]").toString());
		assertEquals("{\"deleted\":[]}", remove(page, "not json").toString());

		// a text saved holds it: the behavior forgets it, and leaves it alone
		page.upload.forget(page.upload.getUploaded());
		assertEquals("{\"deleted\":[]}", remove(page, "[\"/images/2.png\"]").toString());
		assertEquals(List.of("/images/1.png"), deleted);
	}

	/** A store keeping images permanently in memory, by the names of their files. */
	private static final class PermanentStore implements IRichTextImageStore
	{
		private static final long serialVersionUID = 1L;

		final Map<String, String> permanent = new LinkedHashMap<>();

		final List<String> deleted = new ArrayList<>();

		@Override
		public String store(org.apache.wicket.markup.html.form.upload.FileUpload image,
			IRichTextImageType type)
		{
			return keep(image, type);
		}

		@Override
		public void delete(String url)
		{
			deleted.add(url);
		}

		@Override
		public void storePermanently(String url, String name)
		{
			permanent.put(url, name);
		}

		@Override
		public boolean isPermanent(String url)
		{
			return permanent.containsKey(url);
		}

		@Override
		public List<RichTextStoredImage> findPermanent(String query, int max)
		{
			List<RichTextStoredImage> found = new ArrayList<>();
			permanent.forEach((url, name) -> {
				if (name.contains(query))
				{
					found.add(new RichTextStoredImage(url, name));
				}
			});
			// one the editor does not take, and more than asked
			found.add(new RichTextStoredImage("javascript:steal()", "trap.png"));
			return found;
		}
	}

	@Test
	void keepsPermanentlyAndFindsOnlyWhenAllowed() throws IOException
	{
		KEPT.clear();
		PermanentStore store = new PermanentStore();
		TestPage page = new TestPage(store);
		tester.startPage(page);
		upload(page, "one.png", PNG, "image/png");
		upload(page, "two.png", PNG, "image/png");

		// off by default
		assertFalse(page.upload.isPermanentImages());
		assertEquals("{\"stored\":[]}", post(page, RichTextImageUploadBehavior.PERMANENT_PARAMETER,
			"[\"/images/1.png\"]").toString());
		assertEquals("{\"images\":[]}", post(page, RichTextImageUploadBehavior.FIND_PARAMETER,
			"").toString());
		assertTrue(store.permanent.isEmpty());

		page.upload.setPermanentImages(true);
		// those it took alone, with the names of their files
		assertEquals("{\"stored\":[\"/images/1.png\"]}", post(page,
			RichTextImageUploadBehavior.PERMANENT_PARAMETER, "[\"/images/1.png\", " +
				"\"/elsewhere.png\"]").toString());
		assertEquals(Map.of("/images/1.png", "one.png"), store.permanent);
		// kept, it is no longer deleted when asked to
		assertFalse(page.upload.isUploaded("/images/1.png"));
		assertEquals("{\"deleted\":[]}", remove(page, "[\"/images/1.png\"]").toString());
		assertTrue(store.deleted.isEmpty());

		// found by their names, those of an address the editor takes
		assertEquals("{\"images\":[{\"url\":\"/images/1.png\",\"name\":\"one.png\"}]}",
			post(page, RichTextImageUploadBehavior.FIND_PARAMETER, " one ").toString());
		page.upload.setMaxFound(0);
		assertEquals(1, page.upload.getMaxFound());
		store.storePermanently("/images/9.png", "nine.png");
		assertEquals(1, post(page, RichTextImageUploadBehavior.FIND_PARAMETER, "")
			.getJSONArray("images").length());
		assertEquals(RichTextImageUploadBehavior.DEFAULT_MAX_FOUND, new RichTextImageUploadBehavior(
			store).getMaxFound());
	}

	@Test
	void keepsNoneWithAStoreThatKeepsNonePermanently() throws IOException
	{
		KEPT.clear();
		TestPage page = new TestPage(RichTextImageUploadBehaviorTest::keep);
		page.upload.setPermanentImages(true);
		tester.startPage(page);
		upload(page, "one.png", PNG, "image/png");
		assertEquals("{\"stored\":[]}", post(page, RichTextImageUploadBehavior.PERMANENT_PARAMETER,
			"[\"/images/1.png\"]").toString());
		// still deleted when asked to
		assertTrue(page.upload.isUploaded("/images/1.png"));
		assertEquals("{\"images\":[]}", post(page, RichTextImageUploadBehavior.FIND_PARAMETER,
			"one").toString());
		assertThrows(IllegalArgumentException.class, () -> new RichTextStoredImage(" ", "x"));
		assertEquals("/a.png", new RichTextStoredImage(" /a.png ", null).name());
	}

	/**
	 * Posts a parameter to a behavior, as the editor does, and reads the answer.
	 */
	private JSONObject post(TestPage page, String name, String value)
	{
		tester.getRequest().getPostParameters().setParameterValue(name, value);
		tester.executeBehavior(page.upload);
		return new JSONObject(tester.getLastResponseAsString());
	}

	/**
	 * Asks a behavior to delete images, as deleteImagesAt() of the editor does.
	 */
	private JSONObject remove(TestPage page, String json)
	{
		tester.getRequest().getPostParameters().setParameterValue(
			RichTextImageUploadBehavior.REMOVED_PARAMETER, json);
		tester.executeBehavior(page.upload);
		return new JSONObject(tester.getLastResponseAsString());
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

		TestPage(IRichTextImageStore store)
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
