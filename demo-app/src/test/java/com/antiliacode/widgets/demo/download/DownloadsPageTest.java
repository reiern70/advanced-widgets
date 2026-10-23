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
package com.antiliacode.widgets.demo.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.button.AjaxDownloadButton;
import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.demo.WicketApplication;

/**
 * Downloads from {@link DownloadsPage}: via Ajax in each location, the server answering at once
 * or failing, by links and after its form; and the files of the {@link DownloadsResource}.
 */
class DownloadsPageTest
{
	private static final String[] LOCATIONS = { "blob", "iframe", "newwindow", "samewindow" };

	private WicketTester tester;

	private DownloadsPage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.getSession().setLocale(Locale.ENGLISH);
		page = tester.startPage(DownloadsPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private static String inCase(String name, String path)
	{
		return "themed:cases:" + DemoCases.pathOf(name) + ":" + path;
	}

	private static String location(int index)
	{
		return inCase("ajax", "locations:locations:" + index + ":download");
	}

	private AjaxDownloadButton button(int index)
	{
		return (AjaxDownloadButton)page.get(location(index));
	}

	private void click(int index)
	{
		tester.executeAjaxEvent(button(index), "click");
	}

	private void choose(String path, String value)
	{
		FormComponent<?> choice = (FormComponent<?>)page.get(path);
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(), value);
		tester.executeAjaxEvent(choice, "change");
	}

	/**
	 * Asks for the file of a component's download, as the browser does once told to.
	 */
	private void fetch(Component component)
	{
		Behavior resource = component.getBehaviors()
			.stream()
			.filter(behavior -> behavior.getClass().getName().endsWith("$ResourceBehavior"))
			.findFirst()
			.orElseThrow();
		tester.invokeListener(component, resource);
	}

	private void ended(Component component, String result)
	{
		tester.getRequest().getPostParameters().setParameterValue("result", result);
		tester.executeBehavior(component.getBehaviors(AjaxDownloadBehavior.class).get(0));
	}

	@Test
	void showsTheCasesWithTheButtonsOfTheTheme()
	{
		String response = tester.getLastResponseAsString();
		for (String title : List.of("Via Ajax", "Links", "After a form"))
		{
			assertTrue(response.contains(">" + title + "<"), title);
		}
		for (int i = 0; i < LOCATIONS.length; i++)
		{
			TagTester button = TagTester.createTagByAttribute(response, "id",
				button(i).getMarkupId());
			assertEquals("button", button.getName());
			assertTrue(button.getAttribute("class").startsWith("ant-button ant-button--"),
				button.getMarkup());
			assertTrue(button.getValue().contains("ant-button__icon"), button.getMarkup());
		}
		assertEquals("ant-button ant-button--primary ant-button--medium", TagTester
			.createTagByAttribute(response, "id", button(0).getMarkupId()).getAttribute("class"));
		assertTrue(response.contains("Into the browser"), response);
		assertTrue(response.contains("Through a hidden iframe"), response);
		tester.assertContains("antilia-button.*\\.js");
		tester.assertContains("DownloadsPage.*\\.css");
		assertTrue(response.contains("<option selected=\"selected\" value=\"CSV\">CSV (.csv)" +
			"</option>"), response);
	}

	@Test
	void eachButtonDownloadsInItsLocationBusyMeanwhile()
	{
		for (int i = 0; i < LOCATIONS.length; i++)
		{
			click(i);

			String response = tester.getLastResponseAsString();
			assertTrue(response.contains("\"method\":\"" + LOCATIONS[i] + "\""), response);
			assertTrue(response.contains("Wicket.Button.busy(\"" + button(i).getMarkupId() +
				"\",true);"), response);
		}
	}

	@Test
	void servesTheFileChosenAsAnAttachmentOrInlineForANewTab()
	{
		click(0);
		fetch(button(0));
		assertEquals("text/csv; charset=UTF-8", tester.getLastResponse().getContentType());
		String disposition = tester.getLastResponse().getHeader("Content-Disposition");
		assertTrue(disposition.startsWith("attachment") &&
			disposition.contains("countries.csv"), disposition);
		assertTrue(tester.getLastResponseAsString().startsWith("\"Code\",\"Name\"\r\n"));

		choose(inCase("ajax", "file"), "PDF");
		click(2);
		fetch(button(2));
		assertEquals("application/pdf", tester.getLastResponse().getContentType());
		disposition = tester.getLastResponse().getHeader("Content-Disposition");
		assertTrue(disposition.startsWith("inline") && disposition.contains("countries.pdf"),
			disposition);
		assertTrue(tester.getLastResponseAsString().startsWith("%PDF-1.4"));
	}

	@Test
	void tellsHowTheDownloadEndedInANotification()
	{
		click(1);

		ended(button(1), "success");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Notifications.show("), response);
		assertTrue(response.contains("\"type\":\"success\",\"title\":\"Downloaded\",\"body\":" +
			"\"countries.csv: Through a hidden iframe.\""), response);
		assertTrue(response.contains("Wicket.Button.busy(\"" + button(1).getMarkupId() +
			"\",false);"), response);

		ended(button(1), "failed");
		assertTrue(tester.getLastResponseAsString().contains("\"type\":\"error\",\"title\":" +
			"\"Download failed\",\"body\":\"The server could not send countries.csv: Through a " +
			"hidden iframe.\""));
	}

	@Test
	void theServerFailsWhenToldToAndTheSameWindowWaitsMeanwhile()
	{
		choose(inCase("ajax", "answer"), "FAILS");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("The button waits while the server fails"), response);
		assertFalse(button(3).isEnabledInHierarchy());

		click(0);
		fetch(button(0));
		assertEquals(500, tester.getLastResponse().getStatus());

		choose(inCase("ajax", "answer"), "AT_ONCE");
		assertTrue(button(3).isEnabledInHierarchy());
		assertFalse(tester.getLastResponseAsString().contains(
			"The button waits while the server fails"));
	}

	@Test
	void theDownloadLinkSendsAFileWrittenOnTheServerAndDeletesIt() throws IOException
	{
		Set<Path> before = temporaryFiles();

		tester.clickLink(inCase("links", "fileLink"));

		assertEquals(before, temporaryFiles(), "the file is gone");

		String disposition = tester.getLastResponse().getHeader("Content-Disposition");
		assertTrue(disposition.startsWith("attachment") &&
			disposition.contains("countries.txt"), disposition);
		assertTrue(tester.getLastResponseAsString().startsWith(
			"Countries of the world\n======================\n\nCode  Name\n"),
			tester.getLastResponseAsString());
	}

	/**
	 * @return the files of the temporary folder the download link writes
	 */
	private static Set<Path> temporaryFiles() throws IOException
	{
		try (Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir"))))
		{
			return files.filter(file -> file.getFileName().toString().startsWith("countries-"))
				.collect(Collectors.toSet());
		}
	}

	@Test
	void theLinksAreButtonsOfTheTheme()
	{
		String response = tester.getLastResponseAsString();
		assertTrue(response.matches("(?s).*<a [^>]*href=\"[^\"]*files/countries\\.json\".*"),
			"the mounted resource");
		assertTrue(TagTester.createTagByAttribute(response, "class", "downloads-demo__url")
			.getValue().matches("http://[^;]+/files/countries\\.json"), "to share, without a session");

		TagTester open = TagTester.createTagByAttribute(response, "target", "_blank");
		assertTrue(open.getAttribute("href").endsWith("files/countries.pdf?open=true"),
			open.getAttribute("href"));
		assertEquals("ant-button ant-button--secondary ant-button--medium",
			open.getAttribute("class"));
		assertTrue(open.getValue().contains("ant-button__icon"), open.getValue());

		TagTester image = TagTester.createTagByAttribute(response, "download", "downloads.png");
		assertEquals("a", image.getName());
		assertTrue(image.getAttribute("href").endsWith(DownloadsPage.IMAGE),
			image.getAttribute("href"));
		assertTrue(image.getValue().contains("<span class=\"ant-button__label\">downloads.png" +
			"</span>"), image.getValue());
	}

	@Test
	void theResourceLinkSendsTheCsvOfThePage()
	{
		tester.clickLink(inCase("links", "resourceLink"));

		assertTrue(tester.getLastResponse().getHeader("Content-Disposition")
			.contains("countries.csv"));
		assertTrue(tester.getLastResponseAsString().startsWith("\"Code\",\"Name\"\r\n"));
	}

	@Test
	void theMountedResourceSendsEachFileAtItsUrlAndNothingElse()
	{
		tester.executeUrl("files/countries.json");
		assertEquals("application/json; charset=UTF-8", tester.getLastResponse().getContentType());
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition")
			.startsWith("attachment"));
		assertTrue(tester.getLastResponseAsString().contains("\"code\": \"AF\""),
			tester.getLastResponseAsString());

		tester.executeUrl("files/countries.pdf?open=true");
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition").startsWith("inline"));

		tester.executeUrl("files/countries.zip");
		assertEquals("application/zip", tester.getLastResponse().getContentType());

		tester.executeUrl("files/secrets.txt");
		assertEquals(404, tester.getLastResponse().getStatus());
		tester.executeUrl("files/countries.exe");
		assertEquals(404, tester.getLastResponse().getStatus());
	}

	@Test
	void theFormDownloadsTheReportItDescribesOnceItIsValid()
	{
		String form = inCase("form", "report");
		FormTester formTester = tester.newFormTester(form);
		formTester.setValue("title", "");
		tester.executeAjaxEvent(form + ":download", "click");
		String response = tester.getLastResponseAsString();
		assertFalse(response.contains("Wicket.AjaxDownload.initiate("), response);
		assertTrue(response.contains("\"type\":\"error\""), response);

		formTester = tester.newFormTester(form);
		formTester.setValue("title", "Países del Sur!");
		formTester.select("format", 1);
		formTester.setValue("rows", "3");
		tester.executeAjaxEvent(form + ":download", "click");
		response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.AjaxDownload.initiate("), response);
		assertTrue(response.contains("\"method\":\"blob\""), response);

		Component button = tester.getComponentFromLastRenderedPage(form + ":download");
		fetch(button);
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition")
			.contains("paises-del-sur.csv"), tester.getLastResponse().getHeader("Content-Disposition"));
		String csv = new String(tester.getLastResponse().getBinaryContent(),
			StandardCharsets.UTF_8);
		assertEquals(4, csv.split("\r\n").length, csv);

		ended(button, "success");
		assertTrue(tester.getLastResponseAsString().contains("\"body\":\"paises-del-sur.csv: " +
			"Into the browser.\""), tester.getLastResponseAsString());
	}

	@Test
	void theFormOpensTheReportInANewTab()
	{
		String form = inCase("form", "report");
		FormTester formTester = tester.newFormTester(form);
		formTester.setValue("rows", "300");
		tester.executeAjaxEvent(form + ":open", "click");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.AjaxDownload.initiate("),
			"no more countries than there are");

		formTester = tester.newFormTester(form);
		formTester.setValue("rows", "10");
		tester.executeAjaxEvent(form + ":open", "click");
		assertTrue(tester.getLastResponseAsString().contains("\"method\":\"newwindow\""));

		fetch(tester.getComponentFromLastRenderedPage(form + ":open"));
		assertTrue(tester.getLastResponse().getHeader("Content-Disposition")
			.startsWith("inline"));
		assertEquals("application/pdf", tester.getLastResponse().getContentType());
	}

	@Test
	void namesTheReportAfterItsTitle()
	{
		assertEquals("countries-of-the-world", page.reportName());
	}
}
