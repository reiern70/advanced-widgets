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
package com.antiliacode.widgets.demo.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FilesSelectedBehavior;
import org.apache.wicket.markup.html.form.upload.resource.AbstractFileUploadResource;
import org.apache.wicket.markup.html.form.upload.resource.FileUploadToResourceField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.Demos;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.throttle.AjaxDelay;
import com.antiliacode.widgets.throttle.Throttle;
import com.antiliacode.widgets.throttle.UploadSpeed;
import com.antiliacode.widgets.upload.UploadGuard;
import com.antiliacode.widgets.upload.UploadToResourceField;
import com.github.openjson.JSONObject;

/**
 * Checks {@link UploadPage}: the pickers, the bars and the guard it draws, the files a form posts
 * or sends via Ajax, what the browser tells of the files chosen, the uploads to the resource and
 * their end, the buttons the guard stops or lets go, the hooks of the field, and slowing the
 * session down.
 */
class UploadPageTest
{
	private static final String CASES = "themed:cases:";

	@TempDir
	Path folder;

	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(UploadPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private static String inCase(String name, String path)
	{
		return CASES + DemoCases.pathOf(name) + ":" + path;
	}

	private Component component(String name, String path)
	{
		return tester.getComponentFromLastRenderedPage(inCase(name, path));
	}

	private File file(String name, int size) throws IOException
	{
		Path path = folder.resolve(name);
		try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw"))
		{
			file.setLength(size);
		}
		return new File(path.toFile());
	}

	private static int count(String text, String part)
	{
		Matcher matcher = Pattern.compile(Pattern.quote(part)).matcher(text);
		int count = 0;
		while (matcher.find())
		{
			count++;
		}
		return count;
	}

	@Test
	void isADemoOfTheForms()
	{
		assertEquals("forms", Demos.of(UploadPage.class).orElseThrow().category());
		assertFalse(Demos.of(UploadPage.class).orElseThrow().frameworkApps());
	}

	@Test
	void drawsThePickersTheBarsAndTheGuardOfTheTheme()
	{
		String page = tester.getLastResponseAsString();
		assertEquals(4, count(page, "Wicket.Upload.FilePicker.attach("), page);
		assertEquals(2, count(page, "\"design\":\"drop-zone\""), page);
		assertEquals(4, count(page, "Wicket.Upload.UploadProgress.attach("), page);
		// the guards of the two fields, and the script of the zone uploading at once
		assertEquals(3, count(page, "Wicket.Upload.guard("), page);
		assertTrue(page.contains("UploadStatusResource"), "the bars ask the server");
		assertTrue(page.contains("Up to 10 files of 10 MB each, 20 MB in all."), page);

		for (String button : new String[] { "buttons:cancel", "bypassing" })
		{
			assertEquals("", tester.getTagById(component("resource", button).getMarkupId())
				.getAttribute(UploadGuard.BYPASS_ATTRIBUTE), button);
		}
		assertEquals(null, tester.getTagById(component("resource", "blocked").getMarkupId())
			.getAttribute(UploadGuard.BYPASS_ATTRIBUTE));
		assertTrue(component("resource", "buttons:upload").isEnabled());
		assertFalse(component("resource", "buttons:cancel").isEnabled());

		// the zone sending its files at once
		String dropped = component("drop", "dropFiles").getMarkupId();
		assertTrue(page.contains("if (event.target.files && event.target.files.length) { " +
			"Wicket.Upload.guard(\"" + dropped + "\""), page);

		tester.assertVisible("themed:received:none");
		tester.assertInvisible("themed:received:table");
	}

	@Test
	void postsTheFilesOfTheForm() throws IOException
	{
		FormTester form = tester.newFormTester(inCase("form", "form"));
		form.setValue("description", "Holiday");
		form.setFile("files", file("notes.txt", 5), "text/plain");
		form.submit("submit");

		tester.assertInfoMessages("Files received: 1.");
		tester.assertVisible("themed:received:table");
		tester.assertLabel("themed:received:table:files:0:name", "notes.txt");
		tester.assertLabel("themed:received:table:files:0:size", "5 byte");
		tester.assertLabel("themed:received:table:files:0:type", "text/plain");
		tester.assertLabel("themed:received:table:files:0:way", "Form, posted");
		assertEquals("Holiday", ((FormComponent<?>)component("form", "form:description"))
			.getModelObject());
	}

	@Test
	void tellsWhenTheFormSentNoFile()
	{
		tester.newFormTester(inCase("form", "form")).submit("submit");

		tester.assertInfoMessages("No file was sent.");
		tester.assertInvisible("themed:received:table");
	}

	@Test
	void sendsTheFilesOfTheFormViaAjaxAndClearsThePicker() throws IOException
	{
		FormTester form = tester.newFormTester(inCase("form", "form"), false);
		form.setFile("files", file("photo.png", 1536), "image/png");
		tester.getRequest().setUseMultiPartContentType(true);
		tester.executeAjaxEvent(inCase("form", "form:ajaxSubmit"), "click");

		tester.assertInfoMessages("Files received: 1.");
		tester.assertComponentOnAjaxResponse("themed:received");
		tester.assertLabel("themed:received:table:files:0:size", "1.5 kB");
		tester.assertLabel("themed:received:table:files:0:way", "Form, via Ajax");
		String files = component("form", "form:files").getMarkupId();
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Upload.FilePicker.of(\"" +
			files + "\")?.clear();"), tester.getLastResponseAsString());
	}

	@Test
	void tellsTheBarWhenTheServerRefusesTheFiles() throws IOException
	{
		FormTester form = tester.newFormTester(inCase("form", "form"), false);
		form.setFile("files", file("huge.bin", (int)UploadPage.FILE_MAX_SIZE.bytes() + 1),
			"application/octet-stream");
		tester.getRequest().setUseMultiPartContentType(true);
		tester.executeAjaxEvent(inCase("form", "form:ajaxSubmit"), "click");

		// the bar tells why, as the server does
		String bar = component("form", "form:progress").getMarkupId();
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Upload.UploadProgress.of(\"" +
			bar + "\")?.fail(\"Each of files must be less than 10MB.\");"),
			tester.getLastResponseAsString());
		tester.assertErrorMessages("Each of files must be less than 10MB.");
		tester.assertInvisible("themed:received:table");
	}

	@Test
	void tellsWhatTheBrowserToldOfTheFilesChosen()
	{
		Component files = component("form", "form:files");
		FilesSelectedBehavior selected = files.getBehaviors(FilesSelectedBehavior.class).get(0);

		tester.getRequest().setParameter("fileInfos", "[{\"fileName\":\"a.png\"," +
			"\"fileSize\":2048,\"lastModified\":0,\"mimeType\":\"image/png\"}]");
		tester.executeBehavior(selected);
		tester.assertComponentOnAjaxResponse(inCase("form", "form:selection"));
		tester.assertLabel(inCase("form", "form:selection"),
			"Chosen: 1 · 2 kB, as the browser told the server before sending anything.");

		tester.getRequest().setParameter("fileInfos", "[{\"fileName\":\"a.iso\"," +
			"\"fileSize\":31457280,\"lastModified\":0,\"mimeType\":\"\"}]");
		tester.executeBehavior(selected);
		tester.assertLabel(inCase("form", "form:selection"), "Chosen: 1 · 30 MB, as the " +
			"browser told the server before sending anything. That is more than the form " +
			"takes: the page does not send it.");
	}

	/**
	 * Calls the Ajax callback of a field uploading to the resource, as Wicket's script does once
	 * the upload ended.
	 */
	private void uploaded(String name, String path, String... parameters)
	{
		Component field = component(name, path);
		AbstractDefaultAjaxBehavior callback = field
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0);
		for (int i = 0; i < parameters.length; i += 2)
		{
			tester.getRequest().setParameter(parameters[i], parameters[i + 1]);
		}
		tester.executeBehavior(callback);
	}

	@Test
	void uploadsToTheResourceAndListsWhatArrived() throws IOException
	{
		UploadToResourceField field = (UploadToResourceField)component("resource",
			"resourceFiles");
		Path stored = DemoUploadsFileManager.defaultFolder().toPath()
			.resolve(field.getMarkupId()).resolve("a.txt");
		Files.createDirectories(stored.getParent());
		Files.writeString(stored, "hello");

		tester.executeAjaxEvent(inCase("resource", "buttons:upload"), "click");
		assertTrue(tester.getLastResponseAsString().contains(field.getTriggerUploadScript()),
			tester.getLastResponseAsString());
		tester.assertComponentOnAjaxResponse(inCase("resource", "buttons"));
		assertFalse(component("resource", "buttons:upload").isEnabled());
		assertTrue(component("resource", "buttons:cancel").isEnabled());

		uploaded("resource", "resourceFiles", "error", "false", "filesInfo",
			"[{\"clientFileName\":\"a.txt\",\"size\":5,\"contentType\":\"text/plain\"}]");
		tester.assertInfoMessages("Files received: 1.");
		tester.assertLabel("themed:received:table:files:0:name", "a.txt");
		tester.assertLabel("themed:received:table:files:0:way", "Resource");
		assertFalse(Files.exists(stored), "the page keeps no file");
		assertFalse(Files.exists(stored.getParent()), "nor the folder of the field");
		assertTrue(component("resource", "buttons:upload").isEnabled());
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Upload.FilePicker.of(\"" +
			field.getMarkupId() + "\")?.clear();"));
	}

	@Test
	void cancelsTheUploadToTheResource()
	{
		// every hook of the field tells when it is called, in the browser
		String log = JSONObject.quote(component("resource", "hookLog").getMarkupId());
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("function () { UploadDemo.hook(" + log + ", 'browser', " +
			"\"Browser\", \"getClientSideCancelCallBack()\", \"the upload was canceled, before the " +
			"server heard of it\"); }"), page);
		assertTrue(page.contains("function (xhr, settings) { UploadDemo.hook(" + log), page);
		assertTrue(page.contains("function () { UploadDemo.hook(" + log + ", 'browser', " +
			"\"Browser\", \"getClientSideSuccessCallBack()\""), page);
		assertTrue(page.contains("\"the upload failed: {0}\".replace('{0}', res.errorMessage)"),
			page);
		assertTrue(Pattern.compile("UploadPage(-ver-\\d+)?\\.js").matcher(page).find(), page);

		tester.executeAjaxEvent(inCase("resource", "buttons:upload"), "click");
		assertTrue(tester.getLastResponseAsString().contains("UploadDemo.clear(" + log + ");"),
			"each upload logs its own hooks");
		UploadToResourceField field = (UploadToResourceField)component("resource",
			"resourceFiles");

		tester.executeAjaxEvent(inCase("resource", "buttons:cancel"), "click");
		assertTrue(tester.getLastResponseAsString().contains(field.getTriggerCancelUploadScript()),
			tester.getLastResponseAsString());

		uploaded("resource", "resourceFiles", "error", "true", "errorMessage",
			FileUploadToResourceField.UPLOAD_CANCELED);
		tester.assertInfoMessages("The upload was canceled.");
		// and on the server
		assertTrue(tester.getLastResponseAsString().contains("UploadDemo.hook(" + log +
			", 'server', \"Server\", \"onUploadCanceled(target)\", \"the upload was canceled\");"),
			tester.getLastResponseAsString());
		assertTrue(component("resource", "buttons:upload").isEnabled());
		assertFalse(component("resource", "buttons:cancel").isEnabled());
	}

	@Test
	void tellsWhyAnUploadToTheResourceFailed()
	{
		tester.executeAjaxEvent(inCase("resource", "buttons:upload"), "click");

		uploaded("resource", "resourceFiles", "error", "true", "errorMessage",
			AbstractFileUploadResource.NO_FILE_SELECTED);
		tester.assertErrorMessages("Nothing to upload: no files were chosen.");
		assertTrue(component("resource", "buttons:upload").isEnabled());

		// whatever failed: Wicket's own words of it expect an exception the field does not have
		tester.clearFeedbackMessages();
		tester.executeAjaxEvent(inCase("resource", "buttons:upload"), "click");
		uploaded("resource", "resourceFiles", "error", "true", "errorMessage", "uploadFailed");
		tester.assertErrorMessages("The upload failed.");
		assertTrue(component("resource", "buttons:upload").isEnabled());
		tester.clearFeedbackMessages();
		tester.executeAjaxEvent(inCase("resource", "buttons:upload"), "click");
		uploaded("resource", "resourceFiles", "error", "true", "errorMessage",
			"uploadTooManyFiles");
		tester.assertErrorMessages("More than 10 files, the most sent at once.");
	}

	@Test
	void listsTheFilesDroppedOnTheZoneOfTheResource() throws IOException
	{
		// the file the resource stored, which Wicket takes the details from, not the browser
		UploadToResourceField field = (UploadToResourceField)component("drop", "dropFiles");
		Path stored = DemoUploadsFileManager.defaultFolder().toPath()
			.resolve(field.getMarkupId()).resolve("b.png");
		Files.createDirectories(stored.getParent());
		Files.write(stored, new byte[2048]);

		uploaded("drop", "dropFiles", "error", "false", "filesInfo",
			"[{\"clientFileName\":\"b.png\",\"size\":2048,\"contentType\":\"image/png\"}]");

		tester.assertLabel("themed:received:table:files:0:way", "Dropped, to a resource");
		assertFalse(Files.exists(stored), "the page keeps no file");
	}

	@Test
	void sendsTheImagesDroppedIntoTheFormViaAjax() throws IOException
	{
		FormTester form = tester.newFormTester(inCase("drop", "dropForm"), false);
		form.setFile("images", file("c.png", 10), "image/png");
		tester.getRequest().setUseMultiPartContentType(true);
		tester.executeAjaxEvent(inCase("drop", "dropForm:send"), "click");

		tester.assertLabel("themed:received:table:files:0:way", "Dropped into a form");
	}

	@Test
	void setsWhatGuardsTheUploadsOfTheField()
	{
		UploadToResourceField field = (UploadToResourceField)component("resource",
			"resourceFiles");
		assertTrue(field.isKeepingOnPage());
		assertFalse(field.isBlockingRequests());

		tester.executeAjaxEvent(inCase("resource", "keepOnPage"), "click");
		FormComponent<?> block = (FormComponent<?>)component("resource", "blockRequests");
		tester.getRequest().getPostParameters().setParameterValue(block.getInputName(), "on");
		tester.executeAjaxEvent(block, "click");

		assertFalse(field.isKeepingOnPage());
		assertTrue(field.isBlockingRequests());
		assertTrue(field.getTriggerUploadScript().contains(
			"{\"leaving\":false,\"requests\":true}"), field.getTriggerUploadScript());
	}

	@Test
	void countsTheClicksOfTheButtonsTheGuardStopsOrLetsGo()
	{
		tester.executeAjaxEvent(inCase("resource", "blocked"), "click");
		tester.executeAjaxEvent(inCase("resource", "blocked"), "click");
		tester.assertLabel(inCase("resource", "blockedCount"), "Clicks: 2");
		tester.executeAjaxEvent(inCase("resource", "bypassing"), "click");
		tester.assertLabel(inCase("resource", "bypassingCount"), "Clicks: 1");
	}

	@Test
	void takesOneFileOrSeveral()
	{
		String[][] places = { { "form", "form:files", "form:several" },
			{ "resource", "resourceFiles", "several" }, { "drop", "dropFiles", "severalDropped" },
			{ "drop", "dropForm:images", "severalImages" } };
		for (String[] place : places)
		{
			String input = component(place[0], place[1]).getMarkupId();
			assertEquals("multiple", tester.getTagById(input).getAttribute("multiple"), place[1]);
		}
		for (String[] place : places)
		{
			// unchecked: one file, the input rendered anew
			String input = component(place[0], place[1]).getMarkupId();
			tester.executeAjaxEvent(inCase(place[0], place[2]), "click");
			tester.assertComponentOnAjaxResponse(inCase(place[0], place[1]));
			Matcher tag = Pattern.compile("<input[^>]*id=\"" + input + "\"[^>]*>")
				.matcher(tester.getLastResponseAsString());
			assertTrue(tag.find(), place[1]);
			assertFalse(tag.group().contains("multiple"), tag.group());
		}
	}

	@Test
	void signingOutUndoesTheSlowingDown()
	{
		DemoSession.get().signIn("wicket");
		Throttle.setAjaxDelay(tester.getRequest(), AjaxDelay.FIVE_SECONDS);
		Throttle.setUploadSpeed(tester.getRequest(), UploadSpeed.SLOW);

		DemoSession.get().signOut();

		assertFalse(Throttle.isSlowingDown(tester.getRequest()));
	}

	@Test
	void slowsTheSessionDownFromThePageOrTheTopBar()
	{
		assertTrue(Throttle.isInstalled(tester.getApplication()));
		tester.assertInvisible("themed:speed:notInstalled");
		assertSame(UploadSpeed.FULL, Throttle.getUploadSpeed(tester.getRequest()));
		FormComponent<?> speed = (FormComponent<?>)tester.getComponentFromLastRenderedPage(
			"themed:speed:uploadSpeed");
		tester.getRequest().getPostParameters().setParameterValue(speed.getInputName(), "SLOW");
		tester.executeAjaxEvent(speed, "change");

		assertSame(UploadSpeed.SLOW, Throttle.getUploadSpeed(tester.getRequest()));
		// the gauge of the top bar says so
		tester.assertComponentOnAjaxResponse("throttle:state");
		tester.assertLabel("throttle:state", "Uploads 64 kB/s");
		FormComponent<?> delay = (FormComponent<?>)tester.getComponentFromLastRenderedPage(
			"throttle:settings:ajaxDelay");
		tester.getRequest().getPostParameters().setParameterValue(delay.getInputName(),
			"HALF_SECOND");
		tester.executeAjaxEvent(delay, "change");
		assertSame(AjaxDelay.HALF_SECOND, Throttle.getAjaxDelay(tester.getRequest()));
		tester.assertComponentOnAjaxResponse("themed:speed");
		tester.assertLabel("throttle:state", "Ajax +0.5 s · Uploads 64 kB/s");

		// its own title, though the page has one
		tester.clickLink("throttle:button", true);
		assertTrue(tester.getLastResponseAsString().contains("\"title\":\"Slow down\""),
			tester.getLastResponseAsString());
	}

	@Test
	void writesSizesAsTheBrowserDoes()
	{
		UploadPage page = (UploadPage)tester.getLastRenderedPage();
		assertEquals(List.of("0 byte", "1,023 byte", "1 kB", "781.3 kB", "10 MB", "2 GB",
			"2,048 GB"), List.of(page.sizeOf(-1), page.sizeOf(1023), page.sizeOf(1024),
			page.sizeOf(800_000), page.sizeOf(10L << 20), page.sizeOf(2L << 30),
			page.sizeOf(2L << 40)));
	}
}
