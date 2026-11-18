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
package com.antiliacode.widgets.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class FormVersionBehaviorTest extends WicketTestCase
{
	@Test
	void writesTheVersionOnTheFormAndWatchesTheSubmits()
	{
		tester.startPage(new TestPage());

		assertEquals("0", tester.getTagByWicketId("form").getAttribute(
			FormVersionBehavior.ATTRIBUTE));
		assertEquals("0", tester.getTagByWicketId("line").getAttribute(
			FormVersionBehavior.ATTRIBUTE));
		String page = tester.getLastResponseAsString();
		assertTrue(page.contains("antilia-form-version"), page);
		assertTrue(page.contains("Wicket.FormVersion.watch();"), page);
	}

	@Test
	void anAjaxSubmitCarryingTheVersionIsTakenAndRaisesIt()
	{
		TestPage page = tester.startPage(new TestPage());

		ajaxSave(page, "0");

		assertEquals(1, page.saves);
		assertEquals(1, page.form.getFormVersionBehavior().getVersion());
		String answer = tester.getLastResponseAsString();
		assertTrue(answer.contains("Wicket.FormVersion.set(\"" + page.form.getMarkupId() +
			"\", 1);"), answer);
		assertTrue(errors(page).isEmpty(), errors(page).toString());
	}

	@Test
	void aSecondSubmitFromTheSameVersionIsRefused()
	{
		TestPage page = tester.startPage(new TestPage());
		ajaxSave(page, "0");

		ajaxSave(page, "0", "8");

		assertEquals(1, page.saves, "processed once");
		assertEquals(5, page.amount, "the second submit changed no model");
		assertEquals(List.of(FormVersionBehavior.ALREADY_SENT), errors(page));
		assertEquals(1, page.form.getFormVersionBehavior().getVersion());
		assertFalse(tester.getLastResponseAsString().contains("Wicket.FormVersion.set("),
			"the version stays");
	}

	@Test
	void theNextSubmitFromTheNewVersionIsTaken()
	{
		TestPage page = tester.startPage(new TestPage());
		ajaxSave(page, "0");

		ajaxSave(page, "1");

		assertEquals(2, page.saves);
		assertEquals(2, page.form.getFormVersionBehavior().getVersion());
	}

	@Test
	void aSubmitFromAVersionAheadIsRefused()
	{
		TestPage page = tester.startPage(new TestPage());

		ajaxSave(page, "3");

		assertEquals(0, page.saves);
		assertEquals(0, page.form.getFormVersionBehavior().getVersion());
	}

	@Test
	void aNormalSubmitIsTakenOnceFromAVersion()
	{
		tester.startPage(new TestPage());
		String version = FormVersionBehavior.PARAMETER_PREFIX +
			((TestPage)tester.getLastRenderedPage()).form.getMarkupId();

		normalSave(version, "0", "5");
		TestPage page = (TestPage)tester.getLastRenderedPage();
		assertEquals(1, page.saves);
		assertEquals("1", tester.getTagByWicketId("form").getAttribute(
			FormVersionBehavior.ATTRIBUTE), "the page shows the new version");

		normalSave(version, "0", "9");
		page = (TestPage)tester.getLastRenderedPage();
		assertEquals(1, page.saves, "processed once");
		assertEquals(5, page.amount);
		assertEquals(List.of(FormVersionBehavior.ALREADY_SENT), errors(page));
	}

	@Test
	void aSubmitWithoutAVersionIsTaken()
	{
		TestPage page = tester.startPage(new TestPage());

		tester.executeAjaxEvent(page.form.get("ajaxSave"), "click");
		tester.executeAjaxEvent(page.form.get("ajaxSave"), "click");

		assertEquals(2, page.saves, "a page whose script did not load is not blocked");
		assertEquals(0, page.form.getFormVersionBehavior().getVersion());
	}

	@Test
	void aNestedFormHasAVersionOfItsOwn()
	{
		TestPage page = tester.startPage(new TestPage());
		FormVersionBehavior line = page.line.getBehaviors(FormVersionBehavior.class).get(0);

		tester.getRequest().setParameter(FormVersionBehavior.PARAMETER_PREFIX +
			page.line.getMarkupId(), "0");
		tester.executeAjaxEvent(page.line.get("add"), "click");
		tester.getRequest().setParameter(FormVersionBehavior.PARAMETER_PREFIX +
			page.line.getMarkupId(), "0");
		tester.executeAjaxEvent(page.line.get("add"), "click");

		assertEquals(1, page.adds);
		assertEquals(1, line.getVersion());
		assertEquals(0, page.form.getFormVersionBehavior().getVersion(),
			"the outer form was not processed");
	}

	@Test
	void theFormCanTellItWasSentTwiceInItsOwnWay()
	{
		TestPage page = tester.startPage(new TestPage());
		ajaxSave(page, "0");

		ajaxSave(page, "0");

		assertEquals(1, page.sentTwice);
	}

	@Test
	void theErrorIsInTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.forLanguageTag("es"));
		TestPage page = tester.startPage(new TestPage());
		ajaxSave(page, "0");

		ajaxSave(page, "0");

		assertEquals(List.of("Este formulario ya se envió, o cambió mientras lo enviaba. " +
			"Revíselo y envíelo de nuevo."), errors(page));
	}

	@Test
	void goesOnFormsOnly()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new Label("label").add(new FormVersionBehavior()));
	}

	/**
	 * Wicket gives a page a new version when a submit changes it and keeps the older one stored: a
	 * second normal submit of the same form, sent before the first answer came, reaches that copy,
	 * where a version kept in the page would still match. The copy here is the page serialized
	 * before the first submit, as Wicket stores it; its form shares the version kept in the session.
	 */
	@Test
	void aCopyOfThePageStoredBeforeTheFirstSubmitRefusesItsVersion()
	{
		TestPage page = tester.startPage(new TestPage());
		TestPage older = WicketObjects.cloneObject(page);
		ajaxSave(page, "0");

		tester.startPage(older);
		assertEquals("1", tester.getTagByWicketId("form").getAttribute(
			FormVersionBehavior.ATTRIBUTE), "the copy shows the version of the session");
		ajaxSave(older, "0", "9");

		assertEquals(0, older.saves);
		assertEquals(List.of(FormVersionBehavior.ALREADY_SENT), errors(older));
	}

	private void ajaxSave(TestPage page, String version)
	{
		ajaxSave(page, version, "5");
	}

	private void ajaxSave(TestPage page, String version, String amount)
	{
		// as the browser posts them: the tester serializes only the fields not posted already
		tester.getRequest().getPostParameters().setParameterValue(
			FormVersionBehavior.PARAMETER_PREFIX + page.form.getMarkupId(), version);
		tester.getRequest().getPostParameters().setParameterValue("amount", amount);
		tester.executeAjaxEvent(page.form.get("ajaxSave"), "click");
	}

	private void normalSave(String version, String sent, String amount)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("amount", amount);
		tester.getRequest().getPostParameters().setParameterValue(version, sent);
		form.submit("save");
	}

	private static List<String> errors(TestPage page)
	{
		return page.form.getFeedbackMessages()
			.messages(message -> message.getLevel() == FeedbackMessage.ERROR)
			.stream()
			.map(message -> String.valueOf(message.getMessage()))
			.toList();
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		Integer amount;

		int saves;

		int adds;

		int sentTwice;

		final DoubleSubmitBulletsProofForm<Void> form;

		final Form<Void> line;

		TestPage()
		{
			form = new DoubleSubmitBulletsProofForm<>("form")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSentTwice()
				{
					sentTwice++;
					super.onSentTwice();
				}
			};
			add(form);
			form.add(new TextField<>("amount", new PropertyModel<Integer>(this, "amount"),
				Integer.class));
			form.add(new AjaxButton("ajaxSave")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					saves++;
				}
			});
			form.add(new Button("save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onSubmit()
				{
					saves++;
				}
			});
			line = new Form<>("line");
			line.add(new FormVersionBehavior());
			form.add(line);
			line.add(new AjaxButton("add")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					adds++;
				}
			});
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<form wicket:id=\"form\"><input type=\"text\" wicket:id=\"amount\"/>" +
				"<button wicket:id=\"ajaxSave\">Save</button>" +
				"<button wicket:id=\"save\">Save</button>" +
				"<form wicket:id=\"line\"><button wicket:id=\"add\">Add</button></form>" +
				"</form></body></html>");
		}
	}
}
