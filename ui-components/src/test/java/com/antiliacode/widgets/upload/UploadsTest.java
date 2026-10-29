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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadResourceReference;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.markup.html.form.upload.resource.FolderUploadsFileManager;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequestImpl;
import org.apache.wicket.protocol.http.servlet.UploadInfo;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.upload.FilePickerBehavior.Design;
import com.github.openjson.JSONObject;

class UploadsTest extends WicketTestCase
{
	@BeforeAll
	static void uploadsResource() throws Exception
	{
		org.apache.wicket.util.file.File folder = new org.apache.wicket.util.file.File(
			Files.createTempDirectory("antilia-uploads-test").toFile());
		FileUploadResourceReference.createNewInstance(new FolderUploadsFileManager(folder));
	}

	@Test
	void drawsAFilePickerOverAFileInput()
	{
		FilePickerBehavior picker = new FilePickerBehavior();
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>",
			page -> page.add(new FileUploadField("file").add(picker))));

		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("antilia-upload.js"), page);
		assertTrue(page.contains("antilia-upload.css"), page);
		assertTrue(page.contains("antilia-button.css"), page);
		assertTrue(page.contains("antilia-progress-bar.css"), page);
		JSONObject options = options(page, "Wicket.Upload.FilePicker.attach(\"file1\", ");
		assertEquals("button", options.getString("design"));
		assertFalse(options.has("variant"));
		assertFalse(options.has("size"));
		assertTrue(options.getString("locale").startsWith("en"), options.getString("locale"));
		assertEquals("Choose files…", options.getJSONObject("texts").getString("chooseMany"));
		assertEquals("Not accepted: {0}", options.getJSONObject("texts").getString("rejected"));
		assertEquals("Remove {0}", options.getJSONObject("texts").getString("remove"));
		assertEquals("{0} together, more than the {1} sent at once",
			options.getJSONObject("texts").getString("tooMuch"));
		assertFalse(options.has("maxSize"), "no form, no limits");
		assertFalse(options.has("fileMaxSize"));
		assertFalse(options.has("fileCountMax"));
		assertSame(Design.BUTTON, picker.getDesign());
		assertNull(picker.getVariant());
		assertNull(picker.getSize());
	}

	@Test
	void drawsAZoneToDropFilesOnInTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		FilePickerBehavior picker = new FilePickerBehavior().setDesign(Design.DROP_ZONE)
			.setVariant(Variant.PRIMARY).setSize(Size.LARGE);
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>",
			page -> page.add(new FileUploadField("file").add(picker))));

		JSONObject options = options(tester.getLastResponseAsString(),
			"Wicket.Upload.FilePicker.attach(\"file1\", ");
		assertEquals("drop-zone", options.getString("design"));
		assertEquals("primary", options.getString("variant"));
		assertEquals("large", options.getString("size"));
		assertEquals("de", options.getString("locale"));
		assertEquals("Dateien hierher ziehen oder",
			options.getJSONObject("texts").getString("dropMany"));
		assertSame(Variant.PRIMARY, picker.getVariant());
		assertSame(Size.LARGE, picker.getSize());
		assertThrows(IllegalArgumentException.class, () -> picker.setDesign(null));
	}

	@Test
	void tellsThePickerTheLimitsOfTheRootFormOrOfTheField()
	{
		tester.startPage(new TestPage("<form wicket:id=\"form\"><div wicket:id=\"inner\">" +
			"<input type=\"file\" wicket:id=\"file\"/></div></form>" +
			"<form wicket:id=\"plain\"><input type=\"file\" wicket:id=\"other\"/></form>" +
			"<input type=\"file\" wicket:id=\"resource\"/>", page -> {
				Form<Void> form = new Form<>("form");
				form.setMaxSize(Bytes.megabytes(20));
				form.setFileMaxSize(Bytes.megabytes(10));
				form.setFileCountMax(3);
				Form<Void> inner = new Form<>("inner");
				inner.setFileCountMax(1);
				page.add(form.add(inner.add(new FileUploadField("file")
					.add(new FilePickerBehavior()))));
				page.add(new Form<Void>("plain").add(new FileUploadField("other")
					.add(new FilePickerBehavior())));
				TestField resource = new TestField("resource");
				resource.setMaxSize(Bytes.kilobytes(5));
				resource.setFileCountMax(2);
				page.add(resource.add(new FilePickerBehavior()));
			}));

		String page = tester.getLastResponseAsString();
		JSONObject options = options(page, "Wicket.Upload.FilePicker.attach(" + JSONObject.quote(
			tester.getComponentFromLastRenderedPage("form:inner:file").getMarkupId()) + ", ");
		assertEquals(20 * 1024 * 1024, options.getLong("maxSize"));
		assertEquals(10 * 1024 * 1024, options.getLong("fileMaxSize"));
		assertEquals(3, options.getLong("fileCountMax"), "the root form reads the files");
		JSONObject plain = options(page, "Wicket.Upload.FilePicker.attach(" + JSONObject.quote(
			tester.getComponentFromLastRenderedPage("plain:other").getMarkupId()) + ", ");
		assertFalse(plain.has("maxSize"), plain.toString());
		assertFalse(plain.has("fileMaxSize"));
		assertFalse(plain.has("fileCountMax"));
		JSONObject resource = options(page, "Wicket.Upload.FilePicker.attach(" + JSONObject.quote(
			tester.getComponentFromLastRenderedPage("resource").getMarkupId()) + ", ");
		assertEquals(5 * 1024, resource.getLong("maxSize"));
		assertFalse(resource.has("fileMaxSize"));
		assertEquals(2, resource.getLong("fileCountMax"));
	}

	@Test
	void drawsAFilePickerOnlyOverAFileInput()
	{
		assertThrows(WicketRuntimeException.class, () -> tester.startPage(new TestPage(
			"<input type=\"text\" wicket:id=\"file\"/>",
			page -> page.add(new WebMarkupContainer("file").add(new FilePickerBehavior())))));
		assertThrows(WicketRuntimeException.class, () -> tester.startPage(new TestPage(
			"<span wicket:id=\"file\"></span>",
			page -> page.add(new WebMarkupContainer("file").add(new FilePickerBehavior())))));
	}

	@Test
	void showsTheUploadsOfTheRootFormAskingTheServerForTheProgress()
	{
		tester.getApplication().getApplicationSettings().setUploadProgressUpdatesEnabled(true);
		UploadProgressBar[] bars = new UploadProgressBar[2];
		tester.startPage(new TestPage("<form wicket:id=\"form\"><div wicket:id=\"inner\">" +
			"<input type=\"file\" wicket:id=\"file\"/><div wicket:id=\"bar\"></div>" +
			"<div wicket:id=\"fieldBar\"></div></div></form>", page -> {
				Form<Void> form = new Form<>("form");
				Form<Void> inner = new Form<>("inner");
				FileUploadField file = new FileUploadField("file");
				bars[0] = new UploadProgressBar("bar", inner).setPollMillis(10);
				bars[1] = new UploadProgressBar("fieldBar", file).setFollowingServer(false);
				page.add(form.add(inner.add(file, bars[0], bars[1])));
			}));

		String page = tester.getLastResponseAsString();
		String pageId = tester.getLastRenderedPage().getId();
		String form = tester.getComponentFromLastRenderedPage("form").getMarkupId();
		JSONObject options = options(page, "Wicket.Upload.UploadProgress.attach(" +
			JSONObject.quote(bars[0].getMarkupId()) + ", ");
		assertEquals(form, options.getString("source"));
		assertEquals(100, options.getInt("pollMillis"));
		assertEquals(100, bars[0].getPollMillis());
		assertTrue(options.getString("statusUrl").endsWith(UploadStatusResource.NAME + "?upload=" +
			pageId), options.getString("statusUrl"));
		assertEquals("Starting the upload…", options.getJSONObject("texts").getString("starting"));
		JSONObject field = options(page, "Wicket.Upload.UploadProgress.attach(" +
			JSONObject.quote(bars[1].getMarkupId()) + ", ");
		assertEquals(form, field.getString("source"));
		assertTrue(bars[0].isFollowingServer());
		assertFalse(field.has("statusUrl"), "the field's bar follows the request");
		assertTrue(page.contains("id=\"" + form + "\""), page);
	}

	@Test
	void showsTheUploadsOfAFieldUploadingToAResource()
	{
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>" +
			"<div wicket:id=\"bar\"></div>", page -> {
				TestField file = new TestField("file");
				page.add(file, new UploadProgressBar("bar", file));
			}));

		String page = tester.getLastResponseAsString();
		String id = tester.getComponentFromLastRenderedPage("file").getMarkupId();
		JSONObject options = options(page, "Wicket.Upload.UploadProgress.attach(" +
			JSONObject.quote(tester.getComponentFromLastRenderedPage("bar").getMarkupId()) + ", ");
		assertEquals(id, options.getString("source"));
		assertFalse(options.has("statusUrl"), "Wicket keeps no progress");
	}

	@Test
	void guardsTheUploadsOfAFieldUploadingToAResource()
	{
		TestField[] fields = new TestField[1];
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>",
			page -> page.add(fields[0] = new TestField("file"))));

		TestField field = fields[0];
		String id = JSONObject.quote(field.getMarkupId());
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("Wicket.Upload.guard(" + id +
			", {\"leaving\":true,\"requests\":false});"), page);
		// the hooks of Wicket's field stay its own, which the script wraps once the field is made
		assertTrue(page.contains(field.hooks() + ");"), page);
		assertTrue(page.indexOf("Wicket.Upload.field(" + id + ", {") >
			page.indexOf("new Wicket.FileUploadToResourceField("), page);
		assertTrue(field.isKeepingOnPage());
		assertFalse(field.isBlockingRequests());

		field.setBlockingRequests(true).setKeepingOnPage(false);
		assertEquals("Wicket.Upload.guard(" + id + ", {\"leaving\":false,\"requests\":true});" +
			"Wicket.Upload.upload(" + id + ");", field.getTriggerUploadScript());
		field.setBlockingRequests(false);
		assertTrue(field.getTriggerUploadScript().startsWith("Wicket.Upload.guard(" + id +
			", null);"), field.getTriggerUploadScript());
	}

	@Test
	void clearsThePickerAndTellsTheBarAnUploadFailedViaAjax()
	{
		tester.startPage(new TestPage("<form wicket:id=\"form\"><input type=\"file\" " +
			"wicket:id=\"file\"/><div wicket:id=\"bar\"></div><a wicket:id=\"refuse\"></a>" +
			"<a wicket:id=\"quiet\"></a></form>", page -> {
				Form<Void> form = new Form<>("form");
				FileUploadField file = new FileUploadField("file");
				UploadProgressBar bar = new UploadProgressBar("bar", form);
				page.add(form.add(file.add(new FilePickerBehavior()), bar, new TestLink("refuse")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						FilePickerBehavior.clear(file, target);
						bar.fail(target, "Too \"large\"");
					}
				}, new TestLink("quiet")
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target)
					{
						bar.fail(target, null);
					}
				}));
			}));
		String file = tester.getComponentFromLastRenderedPage("form:file").getMarkupId();
		String bar = tester.getComponentFromLastRenderedPage("form:bar").getMarkupId();

		tester.clickLink("form:refuse", true);
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Upload.FilePicker.of(\"" + file + "\")?.clear();"),
			response);
		assertTrue(response.contains("Wicket.Upload.UploadProgress.of(\"" + bar +
			"\")?.fail(\"Too \\\"large\\\"\");"), response);
		tester.clickLink("form:quiet", true);
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Upload.UploadProgress.of(\"" +
			bar + "\")?.fail();"), tester.getLastResponseAsString());
	}

	@Test
	void wordsTheErrorsOfAFieldUploadingToAResource()
	{
		TestField[] fields = new TestField[1];
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>", page -> {
			fields[0] = new TestField("file");
			fields[0].setMaxSize(Bytes.megabytes(20));
			fields[0].setFileCountMax(10);
			page.add(fields[0]);
		}));

		TestField field = fields[0];
		JSONObject messages = options(tester.getLastResponseAsString(),
			"Wicket.Upload.field(" + JSONObject.quote(field.getMarkupId()) + ", ");
		assertEquals("The files weigh more than 20MB together, the most sent at once.",
			messages.getString("uploadTooLarge"));
		assertEquals("More than 10 files, the most sent at once.",
			messages.getString("uploadTooManyFiles"));
		assertEquals("The upload failed.", messages.getString("uploadFailed"));
		assertEquals("Nothing to upload: no files were chosen.",
			messages.getString("wicket.no.files.selected"));
		assertEquals("The connection to the server broke off during the upload.",
			messages.getString(UploadToResourceField.CONNECTION_LOST_RESOURCE_KEY));
		assertEquals(6, messages.length(), messages.toString());

		// the server words a connection broken off with the bar's words, any other failure as one,
		// in the session's language
		AbstractDefaultAjaxBehavior callback = field.getBehaviors(
			AbstractDefaultAjaxBehavior.class).get(0);
		Map<String, String> said = new LinkedHashMap<>();
		said.put("uploadFailed", "The upload failed.");
		said.put("Internal Server Error", "The upload failed.");
		said.put(UploadToResourceField.CONNECTION_LOST_RESOURCE_KEY,
			"The connection to the server broke off during the upload.");
		said.forEach((error, message) -> {
			assertEquals(message, failure(field, callback, error));
			assertEquals(1, field.failures);
			field.failures = 0;
		});
		tester.getSession().setLocale(Locale.GERMAN);
		assertEquals("Der Upload ist fehlgeschlagen.", failure(field, callback, "uploadFailed"));
		assertEquals("Die Verbindung zum Server ist beim Upload abgebrochen.",
			failure(field, callback, UploadToResourceField.CONNECTION_LOST_RESOURCE_KEY));
	}

	@Test
	void escapesTheMessagesOfAFieldUploadingToAResourceInItsScript()
	{
		String value = "<!--<script>";
		tester.startPage(new TestPage("<input type=\"file\" wicket:id=\"file\"/>",
			page -> page.add(new TestField("file")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected JSONObject newMessages()
				{
					return super.newMessages().put("uploadFailed", value);
				}
			})));

		String page = tester.getLastResponseAsString();
		assertFalse(page.contains(value), page);
		assertTrue(page.contains(ScriptJson.escape(JSONObject.quote(value))), page);
	}

	/**
	 * Tells the field that its upload failed, as its script does.
	 *
	 * @return the message the server words the error with
	 */
	private String failure(TestField field, AbstractDefaultAjaxBehavior callback, String error)
	{
		tester.getRequest().setParameter("error", "true");
		tester.getRequest().setParameter("errorMessage", error);
		tester.executeBehavior(callback);
		List<FeedbackMessage> errors = field.getFeedbackMessages().toList();
		return errors.get(errors.size() - 1).getMessage().toString();
	}

	@Test
	void marksTheRequestsThatGoOnWhileAnUploadStopsTheOthers()
	{
		tester.startPage(new TestPage("<a wicket:id=\"marked\"></a><a wicket:id=\"link\"></a>",
			page -> page.add(new TestLink("marked").add(UploadGuard.bypass()), new TestLink("link")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
				{
					super.updateAjaxAttributes(attributes);
					UploadGuard.bypass(attributes);
				}
			})));

		assertEquals("", tester.getTagByWicketId("marked").getAttribute(
			UploadGuard.BYPASS_ATTRIBUTE));
		assertNull(tester.getTagByWicketId("link").getAttribute(UploadGuard.BYPASS_ATTRIBUTE));
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("\"bh\":[function(attrs){" + UploadGuard.BYPASS_SCRIPT + "}]"),
			page);
	}

	@Test
	void tellsHowFarAnUploadOfTheSessionGot()
	{
		assertNotNull(tester.getApplication().getSharedResources().get(UploadStatusResource.NAME));

		tester.startResourceReference(UploadStatusResource.REFERENCE,
			UploadStatusResource.parametersOf("7"));
		assertEquals("{\"state\":\"none\"}", tester.getLastResponseAsString());
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));

		UploadInfo info = new UploadInfo(400);
		info.setBytesUploaded(100);
		MultipartServletWebRequestImpl.setUploadInfo(tester.getRequest(), "7", info);
		MultipartServletWebRequestImpl.setUploadInfo(tester.getRequest(), "empty",
			new UploadInfo(0));
		tester.startResourceReference(UploadStatusResource.REFERENCE,
			UploadStatusResource.parametersOf("7"));
		JSONObject status = new JSONObject(tester.getLastResponseAsString());
		assertEquals("uploading", status.getString("state"));
		assertEquals(100, status.getLong("bytesUploaded"));
		assertEquals(400, status.getLong("totalBytes"));
		assertEquals(25, status.getInt("percent"));

		tester.startResourceReference(UploadStatusResource.REFERENCE,
			UploadStatusResource.parametersOf("empty"));
		assertEquals("{\"state\":\"none\"}", tester.getLastResponseAsString());
		tester.startResourceReference(UploadStatusResource.REFERENCE);
		assertEquals("{\"state\":\"none\"}", tester.getLastResponseAsString());
	}

	private static JSONObject options(String page, String call)
	{
		int start = page.indexOf(call);
		assertTrue(start >= 0, call + " in " + page);
		start += call.length();
		int end = page.indexOf(");", start);
		return new JSONObject(page.substring(start, end));
	}

	private static class TestField extends UploadToResourceField
	{
		private static final long serialVersionUID = 1L;

		int failures;

		TestField(String id)
		{
			super(id);
		}

		/**
		 * @return the hooks of Wicket's field in the browser, as it renders them
		 */
		String hooks()
		{
			return getClientBeforeSendCallBack() + "," + getClientSideSuccessCallBack() + "," +
				getClientSideCancelCallBack() + "," + getClientSideUploadErrorCallBack();
		}

		@Override
		protected void onUploadSuccess(AjaxRequestTarget target,
			List<FileUploadToResourceField.UploadInfo> fileInfos)
		{
		}

		@Override
		protected void onUploadFailure(AjaxRequestTarget target, String errorInfo)
		{
			failures++;
		}
	}

	private static class TestLink extends AjaxLink<Void>
	{
		private static final long serialVersionUID = 1L;

		TestLink(String id)
		{
			super(id);
		}

		@Override
		public void onClick(AjaxRequestTarget target)
		{
		}
	}

	private interface IContent extends java.io.Serializable
	{
		void addTo(TestPage page);
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		private final String body;

		TestPage(String body, IContent content)
		{
			this.body = body;
			content.addTo(this);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" + body + "</body></html>");
		}
	}
}
