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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.ckeditor.CKEditor;
import com.antiliacode.widgets.event.AjaxSubmitEvent;
import com.antiliacode.widgets.event.ClientEventsTester;

/**
 * What the editors do when the cleaning removed something, by their mode, and what they tell of
 * a paste the cleaning in the browser left part of out.
 */
class RichTextCleaningTest extends WicketTestCase
{
	private static final String UNSAFE = "<p onclick=\"steal()\">Hi</p><script>steal()</script>";

	private static final String CLEAN = "<p>Hi</p>";

	@Test
	void isSilentByDefault()
	{
		IModel<String> text = Model.of("");
		TestPage page = new TestPage(new RichTextEditor("editor", text));
		tester.startPage(page);
		assertSame(RichTextCleaning.SILENT, page.editor.getCleaning());

		submit(UNSAFE);
		assertEquals(CLEAN, text.getObject());
		assertTrue(page.cleaned.isEmpty());
		assertTrue(page.reports.isEmpty());
		assertTrue(page.errors.isEmpty());
	}

	/**
	 * @return an editor telling how its onCleaned was called: with a target or without
	 */
	private static RichTextEditor reporting(IModel<String> text, List<String> calls)
	{
		return new RichTextEditor("editor", text)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onCleaned(AjaxRequestTarget target, RichTextCleaningReport report)
			{
				calls.add("onCleaned " + report + (target == null ? " without" : " with") +
					" target");
			}
		}.setCleaning(RichTextCleaning.REPORT);
	}

	@Test
	void reportsWhatItRemovedOnceTheModelGotTheText()
	{
		IModel<String> text = Model.of("");
		List<String> calls = new ArrayList<>();
		TestPage page = new TestPage(reporting(text, calls));
		tester.startPage(page);

		submit(UNSAFE);
		assertEquals(CLEAN, text.getObject());
		RichTextCleaningReport report = RichTextCleaningReport.of(Map.of("script", 1),
			Map.of("onclick", 1));
		assertEquals(List.of(report), page.reports);
		assertEquals(1, page.cleaned.size());
		RichTextCleanedEvent event = page.cleaned.get(0);
		assertSame(page.editor, event.getEditor());
		assertEquals(report, event.getReport());
		// no Ajax: no target
		assertFalse(event.getTarget().isPresent());
		assertEquals(List.of("onCleaned script, onclick without target"), calls);

		// clean text: nothing to report
		submit("<p>Clean</p>");
		assertEquals("<p>Clean</p>", text.getObject());
		assertEquals(1, page.cleaned.size());
	}

	@Test
	void reportsWithTheTargetOfAnAjaxRequest()
	{
		IModel<String> text = Model.of("");
		List<String> calls = new ArrayList<>();
		TestPage page = new TestPage(reporting(text, calls));
		tester.startPage(page);

		tester.getRequest().getPostParameters().setParameterValue(page.editor.getInputName(),
			UNSAFE);
		tester.executeAjaxEvent(page.save, "click");
		assertEquals(CLEAN, text.getObject());
		assertEquals(List.of("submitted"), page.ajax);
		assertTrue(page.cleaned.get(0).getTarget().isPresent());
		assertEquals(List.of("onCleaned script, onclick with target"), calls);
	}

	@Test
	void rejectsTheTextAsAValidatorWould()
	{
		IModel<String> text = Model.of("<p>Before</p>");
		TestPage page = new TestPage(new RichTextEditor("editor", text)
			.setCleaning(RichTextCleaning.REJECT));
		tester.startPage(page);

		submit(UNSAFE);
		// the model keeps what it had, the editor what the user typed
		assertEquals("<p>Before</p>", text.getObject());
		assertEquals(UNSAFE, page.editor.getRawInput());
		assertEquals(List.of("The text holds what the editor does not allow: script, onclick."),
			page.errors);
		assertTrue(page.reports.isEmpty());
		assertTrue(page.cleaned.isEmpty());

		// clean text passes
		submit("<p>Clean</p>");
		assertEquals("<p>Clean</p>", text.getObject());
	}

	@Test
	void rejectsInTheLanguageOfTheSession()
	{
		Session.get().setLocale(Locale.GERMAN);
		IModel<String> text = Model.of("");
		TestPage page = new TestPage(new RichTextEditor("editor", text)
			.setCleaning(RichTextCleaning.REJECT));
		tester.startPage(page);

		submit("<p>a</p><iframe src=\"x\"></iframe>");
		assertEquals(List.of("Der Text enthält, was der Editor nicht erlaubt: iframe."),
			page.errors);
	}

	@Test
	void rejectsThroughTheAjaxOfTheButtonAndOfTheEventsOfTheEditor()
	{
		IModel<String> text = Model.of("");
		List<String> events = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", text)
			.setCleaning(RichTextCleaning.REJECT);
		editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(String eventId, JSONObject values, Form<?> form,
				AjaxRequestTarget target)
			{
				events.add("saved");
			}

			@Override
			public void onError(String eventId, JSONObject values, Form<?> form,
				AjaxRequestTarget target)
			{
				events.add("error");
			}
		});
		TestPage page = new TestPage(editor);
		tester.startPage(page);

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), UNSAFE);
		tester.executeAjaxEvent(page.save, "click");
		assertEquals(List.of("error"), page.ajax);
		assertEquals("", text.getObject());

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), UNSAFE);
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
		assertEquals(List.of("error"), events);

		tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), CLEAN);
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
		assertEquals(List.of("error", "saved"), events);
		assertEquals(CLEAN, text.getObject());
	}

	@Test
	void takesTheModeOfTheApplicationUnlessSetOtherwise()
	{
		RichTextCleaning.setDefault(tester.getApplication(), RichTextCleaning.REJECT);
		assertSame(RichTextCleaning.REJECT, RichTextCleaning.getDefault());
		IModel<String> text = Model.of("");
		TestPage page = new TestPage(new RichTextEditor("editor", text));
		tester.startPage(page);
		submit(UNSAFE);
		assertEquals("", text.getObject());
		assertEquals(1, page.errors.size());

		page.editor.setCleaning(RichTextCleaning.SILENT);
		submit(UNSAFE);
		assertEquals(CLEAN, text.getObject());

		RichTextCleaning.setDefault(tester.getApplication(), null);
		assertSame(RichTextCleaning.SILENT, RichTextCleaning.getDefault());
	}

	@Test
	void rejectsMarkdownAndJsonByWhatTheirSanitizersTell()
	{
		IModel<String> text = Model.of("");
		TestPage page = new TestPage(new RichTextEditor("editor", text)
			.setFormat(RichTextFormat.MARKDOWN).setCleaning(RichTextCleaning.REJECT));
		tester.startPage(page);
		submit("[Click](javascript:steal()) and ![image](data:image/png;base64,AA)");
		assertEquals(List.of("The text holds what the editor does not allow: a, img."),
			page.errors);
		// HTML in Markdown is not told of
		submit("Some <b>HTML</b>");
		assertEquals("Some <b>HTML</b>", text.getObject());

		IModel<String> json = Model.of("");
		page = new TestPage(new RichTextEditor("editor", json).setFormat(RichTextFormat.JSON)
			.setCleaning(RichTextCleaning.REJECT));
		tester.startPage(page);
		submit("not json");
		assertEquals(List.of("The text holds what the editor does not allow: json."),
			page.errors);
	}

	@Test
	void tellsOfNothingWithASanitizerOfThePage()
	{
		IModel<String> text = Model.of("");
		TestPage page = new TestPage(new RichTextEditor("editor", text)
			.setSanitizer(html -> html.replace("<script>steal()</script>", ""))
			.setCleaning(RichTextCleaning.REJECT));
		tester.startPage(page);
		submit("<p>a</p><script>steal()</script>");
		assertEquals("<p>a</p>", text.getObject());
		assertTrue(page.errors.isEmpty());
	}

	@Test
	void reportsTheLinksAndImagesOfMarkdownWhoseAddressesGaveWay()
	{
		RichTextSanitized sanitized = RichTextMarkdownSanitizer.INSTANCE.sanitizeAndReport(
			"[a](javascript:x) ![b [nested]](javascript:y) \\![c](vbscript:z) <javascript:w>\n\n" +
				"[r]: data:text/html,x\n\n[safe](https://example.org) ![ok](/a.png)");
		assertEquals("[a](#) ![b [nested]](#) \\![c](#) &lt;javascript:w>\n\n[r]: #\n\n" +
			"[safe](https://example.org) ![ok](/a.png)", sanitized.text());
		assertEquals(Map.of("a", 4, "img", 1), sanitized.report().getElements());
		assertTrue(RichTextMarkdownSanitizer.INSTANCE.sanitizeAndReport("# Clean").report()
			.isEmpty());
		assertNull(RichTextMarkdownSanitizer.INSTANCE.sanitizeAndReport(null).text());
	}

	@Test
	void reportsTheLinksAndImagesOfJsonItTookOut()
	{
		String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[" +
			"{\"type\":\"text\",\"text\":\"a\",\"marks\":[{\"type\":\"link\",\"attrs\":{\"href\":" +
			"\"javascript:x\"}}]},{\"type\":\"image\",\"attrs\":{\"src\":\"data:x\"}}]}]}";
		RichTextSanitized sanitized = RichTextJsonSanitizer.INSTANCE.sanitizeAndReport(json);
		assertEquals(Map.of("a", 1, "img", 1), sanitized.report().getElements());
		assertFalse(sanitized.text().contains("javascript"));
		String clean = "{\"type\":\"doc\",\"content\":[]}";
		assertEquals(new RichTextSanitized(clean, RichTextCleaningReport.EMPTY),
			RichTextJsonSanitizer.INSTANCE.sanitizeAndReport(clean));
		assertEquals(new RichTextSanitized(null, RichTextCleaningReport.of(Map.of("json", 1),
			null)), RichTextJsonSanitizer.INSTANCE.sanitizeAndReport("[]"));
	}

	@Test
	void readsTheReportOfTheBrowserWithoutTrustingIt()
	{
		JSONObject values = new JSONObject()
			.put("elements", new JSONObject().put("script", 1).put("IMG", 2)
				.put("<b onclick=x>", 1).put("iframe", -1).put("table", 1.5).put("svg", "3"))
			.put("attributes", new JSONObject().put("onclick", 2))
			.put("editor", new JSONObject());
		RichTextCleaningReport report = RichTextCleaningReport.fromJson(values);
		Map<String, Integer> elements = new LinkedHashMap<>();
		elements.put("script", 1);
		elements.put("img", 2);
		assertEquals(elements, report.getElements());
		assertEquals(Map.of("onclick", 2), report.getAttributes());
		assertEquals("script, img, onclick", report.toString());
		assertEquals(report, RichTextCleaningReport.fromJson(report.toJson()));

		JSONObject many = new JSONObject();
		for (int i = 0; i < 100; i++)
		{
			many.put("e" + i, 1);
		}
		assertEquals(RichTextCleaningReport.MAX_NAMES, RichTextCleaningReport.fromJson(
			new JSONObject().put("elements", many)).getElements().size());
		assertSame(RichTextCleaningReport.EMPTY, RichTextCleaningReport.fromJson(null));
		assertSame(RichTextCleaningReport.EMPTY, RichTextCleaningReport.fromJson(
			new JSONObject().put("elements", "script")));
	}

	@Test
	void hearsOfWhatAPasteLost()
	{
		List<RichTextCleaningReport> pasted = new ArrayList<>();
		RichTextEditor editor = new RichTextEditor("editor", Model.of(""));
		editor.addEvent(RichTextEditorPasteCleanedEvent.of((report, target) -> {
			assertNotNull(target);
			pasted.add(report);
		}));
		editor.setPasteNotice(true);
		tester.startPage(new TestPage(editor));
		assertTrue(editor.getOptions().getBoolean("pasteNotice"));
		assertFalse(editor.setPasteNotice(false).getOptions().has("pasteNotice"));

		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.PASTE_CLEANED,
			new JSONObject().put("elements", new JSONObject().put("script", 1))
				.put("attributes", new JSONObject().put("onclick", 2)));
		assertEquals(List.of(RichTextCleaningReport.of(Map.of("script", 1), Map.of("onclick", 2))),
			pasted);
		// nothing lost: not called
		ClientEventsTester.fire(tester, editor, RichTextEditorEvents.PASTE_CLEANED,
			new JSONObject());
		assertEquals(1, pasted.size());
	}

	@Test
	void hasTheTextsOfTheNoticeInEachLanguage()
	{
		for (Locale locale : List.of(Locale.ENGLISH, Locale.GERMAN, Locale.of("es")))
		{
			Session.get().setLocale(locale);
			TestPage page = new TestPage(new RichTextEditor("editor", Model.of("")));
			tester.startPage(page);
			JSONObject texts = RichTextEditor.texts(page.editor);
			for (String name : List.of("pasteCleaned", "pasteScripts", "pasteEmbedded",
				"pasteForms", "pasteImages", "pasteTables", "pasteLinks", "pasteFormatting"))
			{
				assertFalse(texts.getString(name).equals(name), locale + " " + name);
				assertTrue(CKEditor.texts(page.editor).has(name));
			}
			assertTrue(texts.getString("pasteCleaned").contains("{0}"));
		}
	}

	private void submit(String html)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("editor", html);
		form.submit();
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final AbstractRichTextEditor<?> editor;

		final AjaxButton save;

		/** The events the editor sent, bubbling. */
		final List<RichTextCleanedEvent> cleaned = new ArrayList<>();

		/** What the events the editor sent told. */
		final List<RichTextCleaningReport> reports = new ArrayList<>();

		/** The errors of the editor once the form failed. */
		final List<String> errors = new ArrayList<>();

		/** What the Ajax button heard. */
		final List<String> ajax = new ArrayList<>();

		TestPage(AbstractRichTextEditor<?> editor)
		{
			this.editor = editor;
			Form<Void> form = new Form<>("form")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onError()
				{
					editor.getFeedbackMessages().toList().forEach(message -> errors.add(
						String.valueOf(message.getMessage())));
				}
			};
			form.add(editor);
			save = new AjaxButton("save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					ajax.add("submitted");
				}

				@Override
				protected void onError(AjaxRequestTarget target)
				{
					ajax.add("error");
				}
			};
			form.add(save);
			add(form);
		}

		@Override
		public void onEvent(IEvent<?> event)
		{
			if (event.getPayload() instanceof RichTextCleanedEvent payload)
			{
				cleaned.add(payload);
				reports.add(payload.getReport());
			}
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<textarea wicket:id=\"editor\"></textarea>" +
				"<button wicket:id=\"save\">Save</button></form></body></html>");
		}
	}
}
