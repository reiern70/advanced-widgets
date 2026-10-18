/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;

import jakarta.servlet.http.Cookie;

/**
 * Downloads files via Ajax from buttons that are busy meanwhile: an {@link AjaxDownloadButton},
 * and a form's button with a {@link ButtonDownloadBehavior}.
 */
class AjaxDownloadButtonTest extends WicketTestCase
{
	private static final String ICON = "<svg viewBox=\"0 0 1 1\"></svg>";

	@Test
	void rendersAButtonOfTheThemeWithTheScriptMarkingItBusy()
	{
		DownloadsPage page = new DownloadsPage();
		page.report.setIcon(() -> ICON).setVariant(Variant.PRIMARY).setSize(Size.SMALL);
		tester.startPage(page);

		TagTester tag = tester.getTagByWicketId("report");
		assertEquals("button", tag.getAttribute("type"));
		assertEquals("ant-button ant-button--primary ant-button--small", tag.getAttribute("class"));
		assertEquals("<span class=\"ant-button__icon\" aria-hidden=\"true\">" + ICON +
			"</span><span class=\"ant-button__label\">Report</span>", tag.getValue());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("antilia-button.js"), response);
		assertTrue(response.contains("antilia-button.css"), response);
	}

	@Test
	void aClickStartsTheDownloadIntoABlobAndMarksTheButtonBusy()
	{
		DownloadsPage page = new DownloadsPage();
		tester.startPage(page);

		tester.executeAjaxEvent("report", "click");

		String response = tester.getLastResponseAsString();
		int initiate = response.indexOf("Wicket.AjaxDownload.initiate(");
		int busy = response.indexOf("Wicket.Button.busy(\"" + page.report.getMarkupId() +
			"\",true);");
		assertTrue(initiate >= 0 && busy > initiate, response);
		assertTrue(response.contains("\"method\":\"blob\""), response);
	}

	@Test
	void theLocationSaysWhereTheBrowserGetsTheFileAndHowItIsSent()
	{
		DownloadsPage page = new DownloadsPage();
		assertEquals(Location.Blob, page.report.getLocation());
		assertEquals(ContentDisposition.ATTACHMENT, page.report.getContentDisposition());
		page.report.setLocation(Location.NewWindow);
		assertEquals(ContentDisposition.INLINE, page.report.getContentDisposition());
		tester.startPage(page);

		tester.executeAjaxEvent("report", "click");
		assertTrue(tester.getLastResponseAsString().contains("\"method\":\"newwindow\""));

		page.report.setLocation(Location.IFrame);
		assertEquals(ContentDisposition.ATTACHMENT, page.report.getContentDisposition());
		tester.executeAjaxEvent("report", "click");
		assertTrue(tester.getLastResponseAsString().contains("\"method\":\"iframe\""));
	}

	@Test
	void servesTheFileWithTheCookieTellingTheBrowserItCame()
	{
		DownloadsPage page = new DownloadsPage();
		tester.startPage(page);
		tester.executeAjaxEvent("report", "click");

		tester.invokeListener(page.report, resourceBehavior(page.report));

		assertEquals("Q1;Q2", tester.getLastResponseAsString());
		assertTrue(tester.getLastResponse().getCookies().stream()
			.anyMatch(cookie -> cookie.getName().startsWith("wicket-ajaxdownload-" +
				page.report.getMarkupId())), "the cookie of the download");
	}

	@Test
	void anErrorOfTheResourceClearsTheCookieSoTheBrowserTellsTheFailure()
	{
		DownloadsPage page = new DownloadsPage();
		page.fail = true;
		page.report.setLocation(Location.IFrame);
		tester.startPage(page);
		tester.executeAjaxEvent("report", "click");

		tester.invokeListener(page.report, resourceBehavior(page.report));

		assertEquals(500, tester.getLastResponse().getStatus());
		List<Cookie> cookies = tester.getLastResponse().getCookies().stream()
			.filter(cookie -> cookie.getName().startsWith("wicket-ajaxdownload-" +
				page.report.getMarkupId()))
			.toList();
		// the last word on the cookie, which the browser keeps
		Cookie last = cookies.get(cookies.size() - 1);
		assertEquals(0, last.getMaxAge(), "cleared");
		assertEquals("/", last.getPath());
	}

	@Test
	void hearsHowTheDownloadEndedAndMarksTheButtonDone()
	{
		DownloadsPage page = new DownloadsPage();
		tester.startPage(page);
		tester.executeAjaxEvent("report", "click");

		ended(page.report.getDownloadBehavior(), "success");
		assertEquals(List.of("success", "completed"), page.ended);
		assertTrue(tester.getLastResponseAsString().contains("Wicket.Button.busy(\"" +
			page.report.getMarkupId() + "\",false);"));

		page.ended.clear();
		ended(page.report.getDownloadBehavior(), "failed");
		assertEquals(List.of("failed", "completed"), page.ended);
	}

	@Test
	void downloadsTheResourceOfAReferenceWithItsParameters()
	{
		DownloadsPage page = new DownloadsPage();
		tester.startPage(page);

		tester.executeAjaxEvent("mounted", "click");

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.AjaxDownload.initiate("), response);
		assertTrue(response.contains("year=2026"), response);
		assertTrue(response.contains("wicket-ajaxdownload="), response);
		assertTrue(response.contains("Wicket.Button.busy(\"" + page.mounted.getMarkupId() +
			"\",true);"), response);
	}

	@Test
	void aFormButtonDownloadsOnceTheFormIsValid()
	{
		DownloadsPage page = new DownloadsPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.submit("export");
		tester.executeAjaxEvent("form:export", "click");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.AjaxDownload.initiate("),
			"no download of a form not valid");

		form = tester.newFormTester("form");
		form.setValue("name", "sales");
		tester.executeAjaxEvent("form:export", "click");

		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.AjaxDownload.initiate("), response);
		assertTrue(response.contains("Wicket.Button.busy(\"" + page.export.getMarkupId() +
			"\",true);"), response);
	}

	private void ended(AjaxDownloadBehavior download, String result)
	{
		tester.getRequest().getPostParameters().setParameterValue("result", result);
		tester.executeBehavior(download);
	}

	private static Behavior resourceBehavior(AjaxDownloadButton button)
	{
		return button.getBehaviors()
			.stream()
			.filter(behavior -> behavior.getClass().getName().endsWith("$ResourceBehavior"))
			.findFirst()
			.orElseThrow();
	}

	public static class DownloadsPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final AjaxDownloadButton report;

		final AjaxDownloadButton mounted;

		final AjaxButton export;

		final List<String> ended = new ArrayList<>();

		boolean fail;

		DownloadsPage()
		{
			report = new AjaxDownloadButton("report", Model.of("Report"), new ByteArrayResource(
				"text/csv", "Q1;Q2".getBytes(StandardCharsets.UTF_8), "report.csv")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected ResourceResponse newResourceResponse(Attributes attributes)
				{
					if (fail)
					{
						return new ResourceResponse().setError(500, "Broken");
					}
					return super.newResourceResponse(attributes);
				}
			})
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onDownloadSuccess(AjaxRequestTarget target)
				{
					ended.add("success");
				}

				@Override
				protected void onDownloadFailed(AjaxRequestTarget target)
				{
					ended.add("failed");
				}

				@Override
				protected void onDownloadCompleted(AjaxRequestTarget target)
				{
					ended.add("completed");
				}
			};
			add(report);

			mounted = new AjaxDownloadButton("mounted", Model.of("Year"), new YearReference(),
				new PageParameters().set("year", 2026));
			add(mounted);

			Form<Void> form = new Form<>("form");
			add(form);
			form.add(new RequiredTextField<>("name", Model.of("")));
			ButtonDownloadBehavior download = new ButtonDownloadBehavior(new ByteArrayResource(
				"text/plain", new byte[0], "export.txt"));
			export = new AjaxButton("export")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					download.initiate(target);
				}
			};
			form.add(export.add(new ButtonBehavior(Variant.PRIMARY, Size.MEDIUM), download));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><body>" +
				"<button wicket:id=\"report\"></button>" +
				"<button wicket:id=\"mounted\"></button>" +
				"<form wicket:id=\"form\"><input type=\"text\" wicket:id=\"name\"/>" +
				"<button wicket:id=\"export\">Export</button></form>" +
				"</body></html>");
		}
	}

	private static class YearReference extends ResourceReference
	{
		private static final long serialVersionUID = 1L;

		YearReference()
		{
			super(AjaxDownloadButtonTest.class, "year");
		}

		@Override
		public IResource getResource()
		{
			return new AbstractResource()
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected ResourceResponse newResourceResponse(Attributes attributes)
				{
					AjaxDownloadBehavior.markCompleted(attributes);
					ResourceResponse response = new ResourceResponse();
					response.setContentType("text/plain");
					response.setWriteCallback(new WriteCallback()
					{
						@Override
						public void writeData(Attributes attributes)
						{
							attributes.getResponse().write(attributes.getParameters()
								.get("year").toString());
						}
					});
					return response;
				}
			};
		}
	}
}
