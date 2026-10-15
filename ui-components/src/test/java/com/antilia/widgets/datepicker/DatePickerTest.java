/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.datepicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class DatePickerTest extends WicketTestCase
{
	@Test
	void attachesTheScriptToTheInput()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		tester.assertContains("antilia-date-picker.*\\.js");
		tester.assertContains("antilia-date-picker.*\\.css");
		tester.assertContains("Wicket.DatePicker.attach\\(\"" + page.date.getMarkupId() + "\"");
		tester.assertContains("Wicket.DatePicker.attach\\(\"" + page.dateTime.getMarkupId() + "\"");
		assertEquals("2026-10-09", tester.getTagByWicketId("date").getAttribute("value"));
		assertEquals("2026-10-09T14:30", tester.getTagByWicketId("dateTime").getAttribute("value"));
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void passesTheLocaleItsFirstDayOfWeekAndTheTextsOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMANY);
		tester.startPage(new TestPage());

		tester.assertContains("\"locale\":\"de-DE\"");
		tester.assertContains("\"firstDayOfWeek\":1");
		tester.assertContains("\"today\":\"Heute\"");
		tester.assertContains("\"open\":\"Datum wählen\"");
		tester.assertContains("\"open\":\"Datum und Uhrzeit wählen\"");
		tester.assertContains("\"symbols\":\"TT MM JJJJ hh mm\"");
	}

	@Test
	void leavesTheFirstDayOfWeekOfALanguageWithoutACountryToTheScript()
	{
		// Java answers Sunday for German and Spanish without a country; the script, Monday
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage());

		tester.assertContains("\"locale\":\"de\"");
		assertFalse(tester.getLastResponseAsString().contains("firstDayOfWeek"));
	}

	@Test
	void passesALocaleOfItsOwnTheBoundsAndTheMinuteStep()
	{
		TestPage page = new TestPage();
		page.date.setMinimum(LocalDate.of(2026, 1, 1)).setMaximum(LocalDate.of(2026, 12, 31))
			.getDatePickerBehavior()
			.setLocale(Locale.US)
			.setPlaceholder(Model.of("When?"));
		page.dateTime.setMinuteStep(15).setMinimum(LocalDateTime.of(2026, 10, 9, 8, 0, 59));
		tester.startPage(page);

		tester.assertContains("\"locale\":\"en-US\"");
		tester.assertContains("\"firstDayOfWeek\":0");
		tester.assertContains("\"min\":\"2026-01-01\"");
		tester.assertContains("\"max\":\"2026-12-31\"");
		tester.assertContains("\"placeholder\":\"When\\?\"");
		tester.assertContains("\"minuteStep\":15");
		tester.assertContains("\"min\":\"2026-10-09T08:00\"");
	}

	@Test
	void postsTheIsoText()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("date", "2027-02-01");
		form.setValue("dateTime", "2027-02-01T17:45");
		form.submit();

		tester.assertNoErrorMessage();
		assertEquals(LocalDate.of(2027, 2, 1), page.date.getModelObject());
		assertEquals(LocalDateTime.of(2027, 2, 1, 17, 45), page.dateTime.getModelObject());
	}

	@Test
	void takesAnEmptyValueAsNone()
	{
		TestPage page = new TestPage();
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("date", "");
		form.submit();

		assertNull(page.date.getModelObject());
	}

	@Test
	void refusesTextThatIsNoDate()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TestPage page = new TestPage();
		page.date.setLabel(Model.of("Geboren"));
		page.dateTime.setLabel(Model.of("Termin"));
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("date", "gestern");
		form.setValue("dateTime", "2027-02-30T10:00");
		form.submit();

		tester.assertErrorMessages("Der Wert von 'Geboren' ist kein gültiges Datum.",
			"Der Wert von 'Termin' ist kein gültiges Datum mit Uhrzeit.");
		assertEquals(LocalDate.of(2026, 10, 9), page.date.getModelObject());
	}

	@Test
	void refusesDatesOutsideItsBounds()
	{
		TestPage page = new TestPage();
		page.date.setMinimum(LocalDate.of(2026, 1, 1)).setMaximum(LocalDate.of(2026, 12, 31))
			.setLabel(Model.of("Born"));
		page.dateTime.setMaximum(LocalDateTime.of(2026, 10, 9, 18, 0)).setLabel(Model.of("Meeting"));
		tester.startPage(page);

		FormTester form = tester.newFormTester("form");
		form.setValue("date", "2025-12-31");
		form.setValue("dateTime", "2026-10-09T18:01");
		form.submit();

		tester.assertErrorMessages("'Born' must not be before Jan 1, 2026.",
			"'Meeting' must not be after Oct 9, 2026, 6:00 PM.");

		form = tester.newFormTester("form");
		form.setValue("date", "2027-01-01");
		form.setValue("dateTime", "2026-10-09T18:00");
		form.submit();
		tester.assertErrorMessages("'Born' must not be after Dec 31, 2026.");
	}

	@Test
	void notifiesAnAjaxBehaviorOfTheChange()
	{
		TestPage page = new TestPage();
		page.date.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				page.changes++;
			}
		});
		tester.startPage(page);

		tester.newFormTester("form", false).setValue("date", "2026-12-24");
		tester.executeAjaxEvent(page.date, "change");

		assertEquals(LocalDate.of(2026, 12, 24), page.date.getModelObject());
		assertEquals(1, page.changes);
	}

	@Test
	void refusesATagOtherThanAnInput()
	{
		TestPage page = new TestPage();
		page.form.get("text").add(new DatePickerBehavior());

		assertThrows(WicketRuntimeException.class, () -> tester.startPage(page));
	}

	@Test
	void hasDefaults()
	{
		DatePickerBehavior behavior = new DatePickerBehavior();
		assertFalse(behavior.isTime());
		assertTrue(new DatePickerBehavior(true).isTime());
		assertEquals(1, behavior.getMinuteStep());
		assertEquals(59, behavior.setMinuteStep(90).getMinuteStep());
		assertEquals(1, behavior.setMinuteStep(0).getMinuteStep());
		assertNull(behavior.getLocale());
		assertNull(behavior.getMinimum());
		assertNull(behavior.getMaximum());
		assertNull(behavior.getPlaceholder());
		DatePicker picker = new DatePicker("date");
		assertNull(picker.getMinimum());
		assertNull(picker.setMaximum(null).getMaximum());
		assertNull(picker.getDatePickerBehavior().getMaximum());
		assertNull(new DateTimePicker("time").setMinimum(null).getMinimum());
	}

	private static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Form<Void> form = new Form<>("form");

		final DatePicker date = new DatePicker("date", Model.of(LocalDate.of(2026, 10, 9)));

		final DateTimePicker dateTime = new DateTimePicker("dateTime",
			Model.of(LocalDateTime.of(2026, 10, 9, 14, 30)));

		int changes;

		TestPage()
		{
			add(form);
			form.add(date, dateTime);
			form.add(new WebMarkupContainer("text"));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id='form'>" +
				"<input type='text' wicket:id='date'/><input type='text' wicket:id='dateTime'/>" +
				"<span wicket:id='text'></span></form></body></html>");
		}
	}
}
