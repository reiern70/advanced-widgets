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
package com.antiliacode.widgets.demo.resumable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import com.antiliacode.widgets.upload.ResumableUploadResourceReference;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.demo.Demos;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.upload.ResumableUploadField;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Checks {@link ResumableUploadPage}: the uploader it draws and tells where the files go, the
 * switch breaking the chunks off, the files the server received whole, listed with their SHA-256
 * and deleted, and the configuration of the React and Svelte apps.
 */
class ResumableUploadPageTest
{
	/** The body of the next request; none when null. */
	private static byte[] body;

	@TempDir
	Path folder;

	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		body = null;
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public WebRequest newWebRequest(HttpServletRequest request, String filterPath)
			{
				return super.newWebRequest(body == null ? request : new Chunk(request, body),
					filterPath);
			}
		});
		tester.startPage(ResumableUploadPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private static JSONObject argument(String page, String call)
	{
		int start = page.indexOf(call);
		assertTrue(start >= 0, call + " in " + page);
		return (JSONObject)new JSONTokener(page.substring(start + call.length())).nextValue();
	}

	/**
	 * @return the query the uploader of the field sends with its requests, its grant
	 */
	private String fieldQuery()
	{
		ResumableUploadField field = (ResumableUploadField)tester
			.getComponentFromLastRenderedPage("themed:uploads");
		JSONObject settings = argument(tester.getLastResponseAsString(),
			"Wicket.Upload.resumable(\"" + field.getMarkupId() + "\", ");
		return "uploadId=" + encoded(settings.getString("inputName")) + "&fileMaxSize=" +
			settings.getLong("fileMaxSize") + "&uploadToken=" +
			encoded(settings.getString("uploadToken")) + "&grantExpires=" +
			settings.getLong("grantExpires") + "&uploadContext=" +
			encoded(settings.getString("uploadContext"));
	}

	private static String encoded(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Creates an upload by tus, as a browser does.
	 *
	 * @return its address
	 */
	private String create(String query, String name, long length)
	{
		tester.getRequest().setMethod("POST");
		tester.getRequest().setHeader("Tus-Resumable", "1.0.0");
		tester.getRequest().setHeader("Upload-Length", String.valueOf(length));
		tester.getRequest().setHeader("Upload-Metadata", "filename " + Base64.getEncoder()
			.encodeToString(name.getBytes(StandardCharsets.UTF_8)));
		tester.executeUrl(DemoResumableUploads.PATH + "?" + query);
		assertEquals(201, tester.getLastResponse().getStatus());
		return tester.getLastResponse().getHeader("Location");
	}

	/**
	 * Sends a chunk, as a browser does.
	 *
	 * @return the status of the answer
	 */
	private int patch(String query, String url, long offset, byte[] bytes)
	{
		body = bytes;
		tester.getRequest().setMethod("PATCH");
		tester.getRequest().setHeader("Tus-Resumable", "1.0.0");
		tester.getRequest().setHeader("Upload-Offset", String.valueOf(offset));
		tester.executeUrl(url.substring(url.indexOf(DemoResumableUploads.PATH)) + "?" + query);
		body = null;
		return tester.getLastResponse().getStatus();
	}

	private void delete(String query, String url)
	{
		tester.getRequest().setMethod("DELETE");
		tester.getRequest().setHeader("Tus-Resumable", "1.0.0");
		tester.executeUrl(url.substring(url.indexOf(DemoResumableUploads.PATH)) + "?" + query);
	}

	private String storedName()
	{
		return tester.getLastResponse().getHeader(ResumableUploadResourceReference.FILE_NAME_HEADER);
	}

	@Test
	void isADemoOfTheFormsInReactAndSvelteToo()
	{
		assertEquals("forms", Demos.of(ResumableUploadPage.class).orElseThrow().category());
		assertTrue(Demos.of(ResumableUploadPage.class).orElseThrow().frameworkApps());
	}

	@Test
	void drawsTheUploaderAndTellsItsLimits()
	{
		ResumableUploadField field = (ResumableUploadField)tester
			.getComponentFromLastRenderedPage("themed:uploads");
		assertEquals(2, field.getParallelUploads());
		JSONObject settings = argument(tester.getLastResponseAsString(),
			"Wicket.Upload.resumable(\"" + field.getMarkupId() + "\", ");
		assertTrue(settings.getString("resourceUrl").endsWith(DemoResumableUploads.PATH),
			settings.getString("resourceUrl"));
		assertEquals(100L * 1024 * 1024, settings.getLong("fileMaxSize"));
		assertEquals(8L * 1024 * 1024, settings.getLong("chunkSize"));
		tester.assertLabel("themed:limits", "Files of 100 MB at most, sent in chunks of 8 MB, 5 " +
			"at once at most. The server deletes what it got 30 minutes after the last chunk.");
		tester.assertVisible("themed:received:none");
		tester.assertInvisible("themed:received:table");
	}

	@Test
	void listsTheFilesReceivedWholeWithTheirDigestsAndDeletesThem()
	{
		ResumableUploadPage page = (ResumableUploadPage)tester.getLastRenderedPage();
		ResumableUploadField field = (ResumableUploadField)tester
			.getComponentFromLastRenderedPage("themed:uploads");
		String query = fieldQuery();
		String url = create(query, "hello.txt", 11);
		assertEquals(204, patch(query, url, 0, "hello world".getBytes(StandardCharsets.UTF_8)));
		String name = storedName();
		File file = DemoResumableUploads.get().getFileManager().getFile(field.getMarkupId(), name);
		assertTrue(file.isFile());

		// back on the page, whose uploader tells the field of the file
		tester.startPage(page);
		// the field's callback, which takes the names the files were stored under, rather than
		// that of Wicket 10's field
		AbstractDefaultAjaxBehavior callback = tester.getComponentFromLastRenderedPage(
			"themed:uploads").getBehaviors(AbstractDefaultAjaxBehavior.class).stream()
			.filter(behavior -> behavior.getClass().getName().endsWith("$Callback"))
			.findFirst().orElseThrow();
		tester.getRequest().setParameter("error", "false");
		tester.getRequest().setParameter("filesInfo", new JSONArray().put(new JSONObject()
			.put("fileName", name).put("clientFileName", "hello.txt")
			.put("contentType", "text/plain")).toString());
		tester.executeBehavior(callback);

		tester.assertComponentOnAjaxResponse("themed:received");
		tester.assertLabel("themed:received:table:files:0:name", "hello.txt");
		tester.assertLabel("themed:received:table:files:0:size", "11 bytes");
		tester.assertLabel("themed:received:table:files:0:sha256",
			"b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
		assertFalse(file.exists(), "the server keeps no file");
		assertEquals(1, ((ResumableUploadPage)tester.getLastRenderedPage()).getReceived().size());
	}

	@Test
	void breaksTheChunksOffOnlyWhenTheSessionAsks() throws IOException
	{
		String query = fieldQuery();
		assertFalse(DemoResumableUploads.isBreaking());
		FormComponent<?> breaking = (FormComponent<?>)tester.getComponentFromLastRenderedPage(
			"themed:breaking");
		tester.getRequest().getPostParameters().setParameterValue(breaking.getInputName(), "on");
		tester.executeAjaxEvent(breaking, "click");
		assertTrue(DemoResumableUploads.isBreaking());

		// one chunk in three breaks off, after 64 kB: of thirty, some do and some do not
		int broken = 0;
		for (int index = 0; index < 30; index++)
		{
			String url = create(query, "big.bin", 128 * 1024);
			if (patch(query, url, 0, new byte[128 * 1024]) == 500)
			{
				broken++;
			}
			delete(query, url);
		}
		assertTrue(broken > 0 && broken < 25, "broken " + broken);
		DemoResumableUploads.setBreaking(false);
		String url = create(query, "big.bin", 128 * 1024);
		assertEquals(204, patch(query, url, 0, new byte[128 * 1024]));

		InputStream cut = new DemoResumableUploads.BreakingStream(
			new ByteArrayInputStream(new byte[10]), 4);
		assertEquals(4, cut.read(new byte[10], 0, 10));
		assertThrows(IOException.class, cut::read);
	}

	@Test
	void configuresTheReactAndSvelteApps()
	{
		JSONObject json = config();
		assertTrue(json.getString("endpoint").endsWith(DemoResumableUploads.PATH),
			json.getString("endpoint"));
		assertTrue(json.getString("query").startsWith("uploadId=demo-"), json.getString("query"));
		assertTrue(json.getString("receiptsUrl").endsWith(ReceiptsResource.PATH),
			json.getString("receiptsUrl"));
		assertEquals("Pause {0}", json.getJSONObject("uploaderTexts").getString("pause"));
		assertEquals("SHA-256", json.getJSONObject("texts").getString("received.sha256"));
		assertNotNull(json.getJSONObject("pickerTexts").optString("browse", null));
	}

	@Test
	void handsTheFilesOfTheAppsOverToTheReceipts()
	{
		String query = config().getString("query");
		String url = create(query, "app.txt", 11);
		assertEquals(204, patch(query, url, 0, "hello world".getBytes(StandardCharsets.UTF_8)));
		String name = storedName();
		File file = DemoResumableUploads.get().getFileManager()
			.getFile(DemoResumableUploads.existingClientId(), name);
		assertTrue(file.isFile());

		tester.getRequest().setMethod("POST");
		tester.getRequest().getPostParameters().setParameterValue(ReceiptsResource.NAME_PARAMETER,
			name);
		tester.getRequest().getPostParameters()
			.setParameterValue(ReceiptsResource.CLIENT_NAME_PARAMETER, "app.txt");
		tester.executeUrl(ReceiptsResource.PATH);
		assertEquals(200, tester.getLastResponse().getStatus());
		JSONObject receipt = new JSONObject(tester.getLastResponseAsString());
		assertEquals("app.txt", receipt.getString("name"));
		assertEquals(11, receipt.getLong("size"));
		assertFalse(file.exists(), "the server keeps no file");

		tester.getRequest().setMethod("POST");
		tester.getRequest().getPostParameters().setParameterValue(ReceiptsResource.NAME_PARAMETER,
			"../" + name);
		tester.executeUrl(ReceiptsResource.PATH);
		assertEquals(404, tester.getLastResponse().getStatus());
		tester.getRequest().setMethod("GET");
		tester.executeUrl(ReceiptsResource.PATH);
		assertEquals(405, tester.getLastResponse().getStatus());
	}

	/**
	 * @return the configuration of the React and Svelte apps the page rendered
	 */
	private JSONObject config()
	{
		String page = tester.getLastResponseAsString();
		String marker = "data-config=\"";
		int start = page.indexOf(marker);
		assertTrue(start >= 0, page);
		String config = page.substring(start + marker.length(), page.indexOf('"',
			start + marker.length())).replace("&quot;", "\"").replace("&amp;", "&")
			.replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">");
		return new JSONObject(config);
	}

	@Test
	void foldersOfProcessesNoLongerRunningGo() throws IOException
	{
		String tmp = System.getProperty("java.io.tmpdir");
		System.setProperty("java.io.tmpdir", folder.toString());
		try
		{
			Path all = folder.resolve("antilia-widgets-resumable-uploads");
			Path dead = Files.createDirectories(all.resolve("999999999"));
			Files.writeString(dead.resolve("file"), "x");
			Path other = Files.createDirectories(all.resolve("not-a-process"));
			Path own = DemoResumableUploads.defaultFolder();
			assertEquals(all.resolve(String.valueOf(ProcessHandle.current().pid())), own);
			assertFalse(Files.exists(dead));
			assertTrue(Files.exists(other));
		}
		finally
		{
			System.setProperty("java.io.tmpdir", tmp);
		}
	}

	@Test
	void theApplicationMountsTheUploads()
	{
		assertSame(DemoResumableUploads.get(), ResumableUploadResourceReference.getInstance());
		assertEquals(DemoResumableUploads.get().getFolder().toPath().getParent(),
			DemoResumableUploads.defaultFolder());
	}

	/** A chunk of tus: a request with a body of its own. */
	private static final class Chunk extends HttpServletRequestWrapper
	{
		private final byte[] bytes;

		Chunk(HttpServletRequest request, byte[] bytes)
		{
			super(request);
			this.bytes = bytes;
		}

		@Override
		public String getContentType()
		{
			return "application/offset+octet-stream";
		}

		@Override
		public long getContentLengthLong()
		{
			return bytes.length;
		}

		@Override
		public ServletInputStream getInputStream()
		{
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			return new ServletInputStream()
			{
				@Override
				public int read()
				{
					return in.read();
				}

				@Override
				public int read(byte[] buffer, int offset, int length)
				{
					return in.read(buffer, offset, length);
				}

				@Override
				public boolean isFinished()
				{
					return in.available() == 0;
				}

				@Override
				public boolean isReady()
				{
					return true;
				}

				@Override
				public void setReadListener(ReadListener listener)
				{
				}
			};
		}
	}
}
