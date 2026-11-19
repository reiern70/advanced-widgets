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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

class RichTextFilesTest extends WicketTestCase
{
	/** The first bytes of a PNG, and some more. */
	private static final byte[] PNG = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0,
			13, 'I', 'H', 'D', 'R' };

	private static final byte[] GIF = "GIF89a and the rest".getBytes(StandardCharsets.US_ASCII);

	private static final String PATH = "rich-text-files";

	/** The root of the application of the tests on the server. */
	private static final String ROOT = "/context/servlet/";

	@TempDir
	Path folder;

	@Override
	protected WebApplication newApplication()
	{
		return new MockApplication()
		{
			@Override
			protected void init()
			{
				super.init();
				RichTextFiles.mount(this, PATH, new MemoryStore())
					.setAttachmentStore(new MemoryAttachments());
			}
		};
	}

	@BeforeEach
	void forgetTheFiles()
	{
		MemoryStore.FILES.clear();
		MemoryStore.DELETED.clear();
		MemoryStore.PERMANENT.clear();
		MemoryAttachments.FILES.clear();
		MemoryAttachments.DELETED.clear();
	}

	@Test
	void takesFilesAttachedAndServesThemInlineOnlyWhenABrowserMayOpenThem() throws IOException
	{
		String attach = PATH + "?" + RichTextFiles.KIND_PARAMETER + "=" + RichTextFiles.ATTACHMENTS;
		assertEquals(attach, RichTextFiles.get().getAttachmentUploadUrl().substring(ROOT.length()));
		String text = upload(attach, "table.csv", "a,b\n".getBytes(StandardCharsets.UTF_8),
			"application/vnd.ms-excel").getString("url");
		assertEquals(ROOT + PATH + "/f1.csv", text);
		String pdf = upload(attach, "report.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII),
			"application/pdf").getString("url");
		assertEquals("setup.exe is not a file of a type the editor takes: PDF, DOCX, XLSX, PPTX, " +
			"ODT, ODS, ZIP, TXT, CSV, Markdown.", upload(attach, "setup.exe", new byte[] { 'M', 'Z',
				0, 0 }, "application/octet-stream").getString("error"));

		assertEquals(200, get(PATH + "/f1.csv"));
		assertEquals("text/csv; charset=UTF-8", tester.getLastResponse().getContentType());
		assertEquals("attachment; filename=\"table.csv\"; filename*=UTF-8''table.csv",
			tester.getLastResponse().getHeader("Content-Disposition"));
		assertEquals("sandbox", tester.getLastResponse().getHeader("Content-Security-Policy"));
		assertEquals(200, get(pdf.substring(ROOT.length())));
		assertEquals("application/pdf", tester.getLastResponse().getContentType());
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition").startsWith("inline;"));

		// deleted by the session that took it
		JSONObject answer = post(attach, RichTextImageUploadBehavior.REMOVED_PARAMETER,
			new JSONArray(List.of(text)).toString());
		assertEquals(List.of(text), strings(answer.getJSONArray("deleted")));
		assertEquals(List.of(text), MemoryAttachments.DELETED);
	}

	@Test
	void anEditorAttachesItsFilesWithItsOwnRules() throws IOException
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Hi</p>"));
		editor.setUploadResource(RichTextFiles.get()).setAttachmentFormats(EnumSet.of(
			com.antiliacode.widgets.upload.UploadFileFormat.PDF));
		assertSame(RichTextFiles.get().getAttachmentStore(), editor.getAttachmentStore());
		assertTrue(editor.getBehaviors(RichTextAttachmentUploadBehavior.class).isEmpty());
		tester.startPage(new EditorPage(editor));
		JSONObject options = options(editor);
		String url = options.getString("attachUrl").substring(ROOT.length());
		assertTrue(url.startsWith(PATH + "?" + RichTextFiles.KEY_PARAMETER + "="), url);
		assertTrue(url.endsWith("&" + RichTextFiles.KIND_PARAMETER + "=" +
			RichTextFiles.ATTACHMENTS), url);
		assertEquals("[{\"type\":\"application/pdf\",\"extension\":\"pdf\",\"label\":\"PDF\"}]",
			options.getJSONArray("attachTypes").toString());

		assertEquals("notes.txt is not a file of a type the editor takes: PDF.", upload(url,
			"notes.txt", "notes".getBytes(StandardCharsets.UTF_8), "text/plain")
			.getString("error"));
		String pdf = upload(url, "report.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII),
			"application/pdf").getString("url");
		// saved without it: the editor deletes what it attached
		editor.deleteRemovedImages();
		assertEquals(List.of(pdf), MemoryAttachments.DELETED);
	}

	@Test
	void takesAnImageAndServesItAsTheTypeItsBytesSay() throws IOException
	{
		JSONObject answer = upload(PATH, "bird.png", GIF, "image/png");
		String url = answer.getString("url");
		assertEquals(ROOT + PATH + "/1.gif", url, "the site's own address, from the root");
		assertEquals(RichTextImageType.GIF, MemoryStore.FILES.get("1.gif").type);

		tester.getRequest().setMethod("GET");
		tester.executeUrl(PATH + "/1.gif");
		assertEquals(200, tester.getLastResponse().getStatus());
		assertArrayEquals(GIF, tester.getLastResponse().getBinaryContent());
		assertEquals("image/gif", tester.getLastResponse().getContentType(),
			"the type its bytes said, not the browser's");
		assertEquals("nosniff", tester.getLastResponse().getHeader("X-Content-Type-Options"));
		assertEquals("sandbox", tester.getLastResponse().getHeader("Content-Security-Policy"));
		assertEquals("inline; filename=\"bird.png\"; filename*=UTF-8''bird.png",
			tester.getLastResponse().getHeader("Content-Disposition"));
		assertTrue(tester.getLastResponse().getHeader("Cache-Control").contains("private"));
	}

	@Test
	void refusesFilesAgainstTheRulesOfTheResource() throws IOException
	{
		RichTextFiles.get().setImageTypes(EnumSet.of(RichTextImageType.PNG))
			.setImageMaxSize(Bytes.kilobytes(1));
		assertEquals("bird.gif is not an image of a type the editor takes: PNG.",
			upload(PATH, "bird.gif", GIF, "image/gif").getString("error"));
		byte[] large = new byte[4096];
		System.arraycopy(PNG, 0, large, 0, PNG.length);
		assertEquals("large.png is larger than 1 KB.", upload(PATH, "large.png", large,
			"image/png").getString("error"));

		tester.getSession().setLocale(Locale.of("es"));
		assertEquals("notas.gif no es una imagen de un tipo que el editor acepte: PNG.",
			upload(PATH, "notas.gif", GIF, "image/gif").getString("error"));
		assertTrue(MemoryStore.FILES.isEmpty());
	}

	@Test
	void takesUploadsOfTheSiteOwnPagesAlone() throws IOException
	{
		tester.getRequest().addHeader("sec-fetch-site", "cross-site");
		assertRefused(PATH, "bird.png", PNG);

		tester.getRequest().addHeader("Origin", "https://elsewhere.example");
		assertRefused(PATH, "bird.png", PNG);

		// the browser's word that it comes from the site's own page
		tester.getRequest().addHeader("sec-fetch-site", "same-origin");
		assertTrue(upload(PATH, "bird.png", PNG, "image/png").has("url"));
		tester.getRequest().addHeader("Origin", "http://localhost");
		assertTrue(upload(PATH, "bird.png", PNG, "image/png").has("url"));

		// the application's own check
		RichTextFiles.get().setUploadCheck(request -> false);
		assertRefused(PATH, "bird.png", PNG);
		assertEquals(2, MemoryStore.FILES.size());
	}

	private void assertRefused(String url, String name, byte[] bytes) throws IOException
	{
		Path file = folder.resolve(name);
		Files.write(file, bytes);
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
			new org.apache.wicket.util.file.File(file.toFile()), "image/png");
		tester.executeUrl(url);
		assertEquals(403, tester.getLastResponse().getStatus());
	}

	@Test
	void servesNoFileTheStoreOrTheApplicationDoesNotOpen() throws IOException
	{
		upload(PATH, "bird.png", PNG, "image/png");
		assertEquals(404, get(PATH + "/2.png"));
		assertEquals(404, get(PATH + "/..%5C1.png"));
		assertEquals(405, get(PATH));

		RichTextFiles.get().setReadCheck((name, request) -> !name.equals("1.png"));
		assertEquals(404, get(PATH + "/1.png"), "refused is not found");
		RichTextFiles.get().setReadCheck(null);
		assertEquals(200, get(PATH + "/1.png"));
	}

	private int get(String url)
	{
		tester.getRequest().setMethod("GET");
		tester.executeUrl(url);
		return tester.getLastResponse().getStatus();
	}

	@Test
	void deletesOnlyTheImagesTheSessionTook() throws IOException
	{
		String url = upload(PATH, "bird.png", PNG, "image/png").getString("url");

		JSONObject answer = post(PATH, RichTextImageUploadBehavior.REMOVED_PARAMETER,
			new JSONArray(List.of(url, ROOT + PATH + "/other.png")).toString());
		assertEquals(List.of(url), strings(answer.getJSONArray("deleted")));
		assertEquals(List.of(url), MemoryStore.DELETED);

		// another session took nothing
		String second = upload(PATH, "bird.png", PNG, "image/png").getString("url");
		tester.getSession().invalidateNow();
		answer = post(PATH, RichTextImageUploadBehavior.REMOVED_PARAMETER,
			new JSONArray(List.of(second)).toString());
		assertEquals(0, answer.getJSONArray("deleted").length());
		assertEquals(List.of(url), MemoryStore.DELETED);
	}

	@Test
	void keepsImagesPermanentlyAndFindsThemWhenTheResourceSaysSo() throws IOException
	{
		String url = upload(PATH, "bird.png", PNG, "image/png").getString("url");
		assertEquals(0, post(PATH, RichTextImageUploadBehavior.PERMANENT_PARAMETER,
			new JSONArray(List.of(url)).toString()).getJSONArray("stored").length());

		RichTextFiles.get().setPermanentImages(true);
		assertEquals(List.of(url), strings(post(PATH, RichTextImageUploadBehavior.PERMANENT_PARAMETER,
			new JSONArray(List.of(url)).toString()).getJSONArray("stored")));
		assertEquals("bird.png", MemoryStore.PERMANENT.get(url));
		JSONArray found = post(PATH, RichTextImageUploadBehavior.FIND_PARAMETER, "bir")
			.getJSONArray("images");
		assertEquals(1, found.length());
		assertEquals(url, found.getJSONObject(0).getString("url"));
	}

	@Test
	void anEditorPostsItsImagesWithItsOwnRules() throws IOException
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Hi</p>"));
		editor.setUploadResource(RichTextFiles.get()).setImageTypes(EnumSet.of(RichTextImageType.PNG));
		assertSame(RichTextFiles.get(), editor.getUploadResource());
		assertSame(RichTextFiles.get().getImageStore(), editor.getImageStore());
		assertTrue(editor.getBehaviors(RichTextImageUploadBehavior.class).isEmpty(),
			"no behavior of its own");
		tester.startPage(new EditorPage(editor));

		String uploadUrl = options(editor).getString("uploadUrl");
		assertTrue(uploadUrl.startsWith(ROOT + PATH + "?" + RichTextFiles.KEY_PARAMETER + "="),
			uploadUrl);

		String relative = uploadUrl.substring(ROOT.length());
		assertEquals("bird.gif is not an image of a type the editor takes: PNG.",
			upload(relative, "bird.gif", GIF, "image/gif").getString("error"));
		String url = upload(relative, "bird.png", PNG, "image/png").getString("url");
		assertTrue(RichTextFiles.get().getUploaded(relative.substring(relative.indexOf('=') + 1))
			.contains(url));

		// saved without the image: the editor deletes what it took
		editor.deleteRemovedImages();
		assertEquals(List.of(url), MemoryStore.DELETED);

		// a key of no editor of the session
		assertEquals("The image could not be uploaded.", upload(PATH + "?" +
			RichTextFiles.KEY_PARAMETER + "=made-up", "bird.png", PNG, "image/png")
			.getString("error"));
	}

	@Test
	void anEditorTakingNoUploadsNowRefusesThem() throws IOException
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Hi</p>"));
		editor.setUploadResource(RichTextFiles.get());
		EditorPage page = new EditorPage(editor);
		tester.startPage(page);
		String uploadUrl = options(editor).getString("uploadUrl").substring(ROOT.length());

		editor.setReadOnly(true);
		tester.startPage(page);
		assertFalse(tester.getLastResponseAsString().contains("uploadUrl"));
		assertEquals("The image could not be uploaded.", upload(uploadUrl, "bird.png", PNG,
			"image/png").getString("error"));
		assertTrue(MemoryStore.FILES.isEmpty());
	}

	@Test
	void linksTheFilesToTheBaseAddress()
	{
		tester.startPage(new EditorPage(new RichTextEditor("editor", Model.of(""))));
		assertEquals(ROOT + PATH + "/a%20b.png", RichTextFiles.get().addressOf("a b.png"));
		RichTextFiles.get().setBaseUrl("https://files.example.com/texts/");
		assertEquals("https://files.example.com/texts/a.png", RichTextFiles.get().addressOf(
			"a.png"));
		RichTextFiles.get().setBaseUrl(null);
	}

	@Test
	void anEditorOfTheSiteImagesAloneTakesThoseBelowTheBaseAddressAsTheSiteOwn() throws IOException
	{
		RichTextEditor editor = new RichTextEditor("editor", Model.of("<p>Hi</p>"));
		editor.setUploadResource(RichTextFiles.get()).setImagesFromSiteOnly(true);
		tester.startPage(new EditorPage(editor));
		String base = "https://files.example.com/texts";
		RichTextFiles.get().setBaseUrl(base + "/");
		try
		{
			assertTrue(RichTextFiles.isOwnUrl("/a.png"));
			assertTrue(RichTextFiles.isOwnUrl(base + "/a.png"));
			assertFalse(RichTextFiles.isOwnUrl(base));
			assertFalse(RichTextFiles.isOwnUrl(base + "/"));
			assertFalse(RichTextFiles.isOwnUrl(base + "x/a.png"));
			assertFalse(RichTextFiles.isOwnUrl("https://files.example.com.evil.org/texts/a.png"));
			assertFalse(RichTextFiles.isOwnUrl("https://other.org/a.png"));
			assertFalse(RichTextFiles.isOwnUrl("javascript:alert(1)//" + base + "/a.png"));

			// the address the resource answers for an upload is taken
			String relative = options(editor).getString("uploadUrl").substring(ROOT.length());
			String url = upload(relative, "bird.png", PNG, "image/png").getString("url");
			assertTrue(url.startsWith(base + "/"), url);

			// and the cleaning of each format keeps it, not one of another site
			assertEquals("![a](" + url + ") ![b](#)", editor.getMarkdownSanitizer().sanitize(
				"![a](" + url + ") ![b](https://other.org/b.png)"));
			String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"image\",\"attrs\":" +
				"{\"src\":\"" + url + "\"}}]}";
			assertEquals(json, editor.getJsonSanitizer().sanitize(json));
			String html = editor.getSanitizer().sanitize("<p><img src=\"" + url + "\"><img " +
				"src=\"https://other.org/b.png\"></p>");
			assertTrue(html.contains(url), html);
			assertFalse(html.contains("other.org"), html);
		}
		finally
		{
			RichTextFiles.get().setBaseUrl(null);
		}
		assertFalse(RichTextFiles.isOwnUrl(base + "/a.png"), "no base address");
	}

	@Test
	void mountsOnceAPath()
	{
		assertThrows(IllegalStateException.class, () -> RichTextFiles.mount(
			tester.getApplication(), "/" + PATH + "/", new MemoryStore()));
		assertThrows(IllegalStateException.class, () -> RichTextFiles.get("elsewhere"));
		assertSame(RichTextFiles.get(), RichTextFiles.get("/" + PATH));
	}

	@Test
	void namesTheFileAsRfc6266Says()
	{
		assertEquals("attachment", RichTextFiles.disposition(false, null));
		assertEquals("attachment; filename=\"report.pdf\"; filename*=UTF-8''report.pdf",
			RichTextFiles.disposition(false, "C:\\Users\\me\\report.pdf"));
		assertEquals("inline; filename=\"_t_ _quoted_ _50_.png\"; " +
			"filename*=UTF-8''%C3%A9t%C3%A9%20%22quoted%22%20%2550%25.png",
			RichTextFiles.disposition(true, "été \"quoted\" %50%.png"));
		assertEquals("inline; filename=\"ab\"; filename*=UTF-8''ab",
			RichTextFiles.disposition(true, "a\r\nb"));
		assertFalse(RichTextFiles.isName("a/b"));
		assertFalse(RichTextFiles.isName(".."));
		assertTrue(RichTextFiles.isName("1.png"));
	}

	@Test
	void servesAFileNotForTheBrowserAsAnAttachment() throws IOException
	{
		MemoryStore.FILES.put("notes.bin", new Kept(BINARY, "notes.bin", new byte[] { 1, 2 }));
		assertEquals(200, get(PATH + "/notes.bin"));
		assertEquals("application/octet-stream", tester.getLastResponse().getContentType());
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition")
			.startsWith("attachment;"));
	}

	/** A format no browser opens. */
	private static final IUploadFileFormat BINARY = new IUploadFileFormat()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getMimeType()
		{
			return "application/octet-stream";
		}

		@Override
		public String getExtension()
		{
			return "bin";
		}

		@Override
		public String getLabel()
		{
			return "Binary";
		}

		@Override
		public boolean matches(byte[] head)
		{
			return true;
		}
	};

	/**
	 * Posts a file to the resource, as the editor does, and reads the answer.
	 */
	private JSONObject upload(String url, String name, byte[] bytes, String contentType)
		throws IOException
	{
		Path file = folder.resolve(name);
		Files.write(file, bytes);
		tester.getRequest().setUseMultiPartContentType(true);
		tester.getRequest().addFile(RichTextImageUploadBehavior.FILE_PARAMETER,
			new org.apache.wicket.util.file.File(file.toFile()), contentType);
		tester.executeUrl(url);
		assertEquals(200, tester.getLastResponse().getStatus());
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));
		return new JSONObject(tester.getLastResponseAsString());
	}

	private JSONObject post(String url, String name, String value)
	{
		tester.getRequest().getPostParameters().setParameterValue(name, value);
		tester.executeUrl(url);
		return new JSONObject(tester.getLastResponseAsString());
	}

	/** A file kept. */
	private record Kept(IUploadFileFormat type, String fileName, byte[] bytes)
	{
	}

	/** Keeps the files in memory, each named by a number and the extension of its type. */
	private static final class MemoryStore implements IRichTextImageStore
	{
		private static final long serialVersionUID = 1L;

		static final Map<String, Kept> FILES = new ConcurrentHashMap<>();

		static final List<String> DELETED = new java.util.concurrent.CopyOnWriteArrayList<>();

		static final Map<String, String> PERMANENT = new ConcurrentHashMap<>();

		private static final AtomicInteger COUNT = new AtomicInteger();

		@Override
		public String store(FileUpload image, IRichTextImageType type)
		{
			if (FILES.isEmpty())
			{
				COUNT.set(0);
			}
			String name = COUNT.incrementAndGet() + "." + type.getExtension();
			FILES.put(name, new Kept(type, image.getClientFileName(), image.getBytes()));
			return RichTextFiles.get().addressOf(name);
		}

		@Override
		public void delete(String url)
		{
			DELETED.add(url);
		}

		@Override
		public void storePermanently(String url, String name)
		{
			PERMANENT.put(url, name);
		}

		@Override
		public boolean isPermanent(String url)
		{
			return PERMANENT.containsKey(url);
		}

		@Override
		public List<RichTextStoredImage> findPermanent(String query, int max)
		{
			return PERMANENT.entrySet().stream().filter(each -> each.getValue().contains(query))
				.map(each -> new RichTextStoredImage(each.getKey(), each.getValue())).limit(max)
				.toList();
		}

		@Override
		public RichTextServedFile open(String name)
		{
			Kept kept = FILES.get(name);
			return kept == null ? null : new RichTextServedFile(kept.type, kept.fileName,
				kept.bytes.length, Instant.now(), () -> new ByteArrayInputStream(kept.bytes));
		}
	}

	/** Keeps the files attached in memory, each named f and a number and its extension. */
	private static final class MemoryAttachments implements IRichTextAttachmentStore
	{
		private static final long serialVersionUID = 1L;

		static final Map<String, Kept> FILES = new ConcurrentHashMap<>();

		static final List<String> DELETED = new java.util.concurrent.CopyOnWriteArrayList<>();

		private static final AtomicInteger COUNT = new AtomicInteger();

		@Override
		public String store(FileUpload file, IUploadFileFormat format)
		{
			if (FILES.isEmpty())
			{
				COUNT.set(0);
			}
			String name = "f" + COUNT.incrementAndGet() + "." + format.getExtension();
			FILES.put(name, new Kept(format, file.getClientFileName(), file.getBytes()));
			return RichTextFiles.get().addressOf(name);
		}

		@Override
		public void delete(String url)
		{
			DELETED.add(url);
		}

		@Override
		public RichTextServedFile open(String name)
		{
			Kept kept = FILES.get(name);
			return kept == null ? null : new RichTextServedFile(kept.type, kept.fileName,
				kept.bytes.length, Instant.now(), () -> new ByteArrayInputStream(kept.bytes));
		}
	}

	/** A page with an editor. */
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

	private static List<String> strings(JSONArray array)
	{
		List<String> list = new java.util.ArrayList<>();
		for (int i = 0; i < array.length(); i++)
		{
			list.add(array.getString(i));
		}
		return list;
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
}
