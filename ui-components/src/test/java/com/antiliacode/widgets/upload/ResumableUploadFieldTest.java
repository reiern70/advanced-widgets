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
package com.antiliacode.widgets.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.resumable.ResumableUploadResourceReference;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.resource.FolderUploadsFileManager;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

/**
 * Tests {@link ResumableUploadField}: the uploader of the theme over Wicket's resumable field.
 */
class ResumableUploadFieldTest extends WicketTestCase
{
	@TempDir
	static Path folder;

	@Override
	protected WebApplication newApplication()
	{
		return new MockApplication()
		{
			@Override
			protected void init()
			{
				super.init();
				mountResource("resumable-uploads", new ResumableUploadResourceReference(
					new FolderUploadsFileManager(new File(folder.resolve("stored").toString())),
					folder.resolve("parts").toFile()).setMaxFileSize(Bytes.bytes(100))
					.setMaxChunkSize(Bytes.bytes(40)));
			}
		};
	}

	private static JSONObject settings(String page, String id)
	{
		String call = "Wicket.Upload.resumable(\"" + id + "\", ";
		int start = page.indexOf(call);
		assertTrue(start >= 0, call + " in " + page);
		return (JSONObject)new JSONTokener(page.substring(start + call.length())).nextValue();
	}

	@Test
	void drawsTheUploaderOfTheThemeWithTheSettingsOfWicketsField()
	{
		TestField field = new TestField("files");
		field.setAccept("text/*").setDesign(Design.BUTTON).setMultiple(false).setAutoStart(false)
			.setKeepingOnPage(false).setOverridingPatchMethod(true);
		field.setChunkSize(Bytes.bytes(100)).setParallelUploads(3).setRemembering(false);
		tester.startPage(new TestPage(field));
		String page = tester.getLastResponseAsString();
		assertFalse(page.contains("new Wicket.ResumableUploadToResourceField("),
			"not the script of Wicket's field");
		JSONObject settings = settings(page, field.getMarkupId());
		assertEquals("../resumable-uploads", settings.getString("resourceUrl"));
		assertEquals(field.getMarkupId(), settings.getString("inputName"));
		assertTrue(settings.getString("uploadToken").length() > 10);
		assertTrue(settings.getString("ajaxCallBackUrl").contains("files"));
		assertEquals(100, settings.getLong("fileMaxSize"));
		assertEquals(40, settings.getLong("chunkSize"), "at most what the resource takes");
		assertEquals(3, settings.getInt("parallelUploads"));
		assertFalse(settings.getBoolean("remembering"));
		assertFalse(settings.getBoolean("multiple"));
		assertEquals("text/*", settings.getString("accept"));
		assertFalse(settings.getBoolean("autoStart"));
		assertFalse(settings.getBoolean("keepingOnPage"));
		assertTrue(settings.getBoolean("overridePatchMethod"));
		assertEquals(ResumableUploadField.PROGRESS_POLL_MILLIS, settings.getInt("pollMillis"));
		assertEquals("button", settings.getString("design"));
		assertEquals("Pause {0}", settings.getJSONObject("texts").getString("pause"));
		assertEquals(ResumableUploadField.TEXTS.length, settings.getJSONObject("texts").length());
		assertEquals("Choose a file…", settings.getJSONObject("pickerTexts").getString("choose"));
		assertEquals("Larger than 100 bytes, the most for a file.",
			settings.getJSONObject("errorTexts").getString("resumableUploadTooLarge"));
		assertTrue(page.contains("accept=\"text/*\""), "on the input as well");
		assertFalse(page.contains("multiple=\"multiple\""));
	}

	@Test
	void wordsTheRefusalsInTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TestField field = new TestField("files");
		tester.startPage(new TestPage(field));
		JSONObject errors = settings(tester.getLastResponseAsString(), field.getMarkupId())
			.getJSONObject("errorTexts");
		assertEquals("Höchstens 10 Uploads zugleich: beenden oder brechen Sie zuerst einen ab.",
			errors.getString("resumableUploadTooMany"));
		assertEquals(ResumableUploadField.ERROR_KEYS.length, errors.length());
	}

	@Test
	void handsTheStoredFilesToTheField() throws IOException
	{
		TestField field = new TestField("files");
		tester.startPage(new TestPage(field));
		// as the resource stores a whole file
		DiskFileItem item = DiskFileItem.builder().setFieldName("f").setFileName("a.txt")
			.setContentType("text/plain").get();
		try (var out = item.getOutputStream())
		{
			out.write("hello".getBytes(StandardCharsets.UTF_8));
		}
		String name = ResumableUploadResourceReference.getInstance().getFileManager()
			.store(new FileUpload(item), field.getMarkupId());

		tester.getRequest().setParameter("error", "false");
		tester.getRequest().setParameter("filesInfo", new JSONArray().put(new JSONObject()
			.put("fileName", name).put("clientFileName", "a.txt").put("contentType", "text/plain"))
			.toString());
		tester.executeBehavior(callbackOf(field));
		assertEquals(List.of("a.txt"), field.names);
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), field.bytes.get(0));

		tester.getRequest().setParameter("error", "true");
		tester.getRequest().setParameter("errorMessage", "resumableUploadNoRoom");
		tester.executeBehavior(callbackOf(field));
		assertEquals(List.of("The server has no room for the file now: try again later."),
			field.failures);
	}

	@Test
	void isAnInputOfFiles()
	{
		assertThrows(Exception.class,
			() -> tester.startPage(new TestPage(new TestField("files"), "<div wicket:id=\"files\"></div>")));
	}

	/**
	 * @return the callback of Wicket's field, the first behavior it adds
	 */
	private static AbstractDefaultAjaxBehavior callbackOf(TestField field)
	{
		return field.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0);
	}

	private static class TestField extends ResumableUploadField
	{
		private static final long serialVersionUID = 1L;

		final List<String> names = new ArrayList<>();

		final List<byte[]> bytes = new ArrayList<>();

		final List<String> failures = new ArrayList<>();

		TestField(String id)
		{
			super(id);
		}

		@Override
		protected void onUploadSuccess(AjaxRequestTarget target, List<UploadInfo> infos)
		{
			infos.forEach(info -> {
				names.add(info.getClientFileName());
				bytes.add(info.get());
			});
		}

		@Override
		protected void onUploadFailure(AjaxRequestTarget target, String errorInfo)
		{
			failures.add(errorInfo);
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private final String markup;

		TestPage(ResumableUploadField field)
		{
			this(field, "<input type=\"file\" wicket:id=\"files\"/>");
		}

		TestPage(ResumableUploadField field, String markup)
		{
			this.markup = markup;
			add(field);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" + markup +
				"</body></html>");
		}
	}
}
