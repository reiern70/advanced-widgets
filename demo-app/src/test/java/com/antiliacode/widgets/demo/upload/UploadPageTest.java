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
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FilesSelectedBehavior;
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

/**
 * Checks {@link UploadPage}: the pickers and the bars it draws, the files a form posts or sends via
 * Ajax, what the browser tells of the files chosen, the images dropped into a form, and slowing the
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
	void drawsThePickersAndTheBarsOfTheTheme()
	{
		String page = tester.getLastResponseAsString();
		assertEquals(2, count(page, "Wicket.Upload.FilePicker.attach("), page);
		assertEquals(1, count(page, "\"design\":\"drop-zone\""), page);
		assertEquals(2, count(page, "Wicket.Upload.UploadProgress.attach("), page);
		// only the uploads to a resource, which need Wicket 11, are guarded
		assertEquals(0, count(page, "Wicket.Upload.guard("), page);
		assertTrue(page.contains("UploadStatusResource"), "the bars ask the server");
		assertTrue(page.contains("Up to 10 files of 10 MB each, 20 MB in all."), page);

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
	void takesOneFileOrSeveral()
	{
		String[][] places = { { "form", "form:files", "form:several" },
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
