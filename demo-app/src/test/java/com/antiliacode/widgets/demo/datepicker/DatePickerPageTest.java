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
package com.antiliacode.widgets.demo.datepicker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;

/**
 * Renders {@link DatePickerPage}, changes the locale of its calendars and its values via Ajax and
 * sends its form.
 */
class DatePickerPageTest
{
	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.getSession().setLocale(Locale.US);
		tester.startPage(DatePickerPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void rendersFivePickersInTheLocaleOfTheSession()
	{
		String page = tester.getLastResponseAsString();
		assertEquals(5, page.split(Pattern.quote("Wicket.DatePicker.attach("), -1).length - 1);
		tester.assertContains(Pattern.quote("\"locale\":\"en-US\",\"firstDayOfWeek\":0"));
		tester.assertContains(Pattern.quote("\"max\":\"" + LocalDate.now() + "\""));
		tester.assertContains(Pattern.quote("\"minuteStep\":15"));
		tester.assertContains(Pattern.quote("value=\"1990-05-17\""));
		tester.assertContains(Pattern.quote(
			"Born May 17, 1990; contract from nothing; appointment "));
	}

	@Test
	void givesTheAppsTheTextsOfThePickersAndTheValues()
	{
		String config = tester.getLastResponseAsString();
		assertTrue(config.contains("&quot;dateTexts&quot;:{&quot;open&quot;:&quot;Choose date&quot;"),
			config);
		assertTrue(config.contains("&quot;language&quot;:&quot;en-US&quot;"));
		assertTrue(config.contains("&quot;born&quot;:&quot;1990-05-17&quot;"));
	}

	@Test
	void showsTheCalendarsInTheLocaleChosen()
	{
		FormTester form = tester.newFormTester("themed:form", false);
		form.select("calendarLocale", 3);
		tester.executeAjaxEvent("themed:form:calendarLocale", "change");

		tester.assertComponentOnAjaxResponse("themed:form");
		String response = tester.getLastResponseAsString();
		assertEquals(5, response.split(Pattern.quote("\"locale\":\"de-DE\",\"firstDayOfWeek\":1"),
			-1).length - 1, response);
		// the texts stay in the language of the session
		tester.assertContains(Pattern.quote("\"today\":\"Today\""));
	}

	@Test
	void reportsEveryChangeViaAjax()
	{
		tester.newFormTester("themed:form", false).setValue("born", "2000-01-02");
		tester.executeAjaxEvent("themed:form:born", "change");

		tester.assertComponentOnAjaxResponse("themed:choices");
		tester.assertContains(Pattern.quote("Born Jan 2, 2000;"));
	}

	@Test
	void refusesTextThatIsNoDateAndDatesOutOfBounds()
	{
		tester.newFormTester("themed:form", false).setValue("born", "nonsense");
		tester.executeAjaxEvent("themed:form:born", "change");
		tester.assertErrorMessages("The value of 'Born' is not a valid date.");
		tester.clearFeedbackMessages();

		tester.newFormTester("themed:form", false).setValue("start", "2000-01-01");
		tester.executeAjaxEvent("themed:form:start", "change");
		tester.assertErrorMessages("'Contract starts' must not be before " +
			DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US)
				.format(LocalDate.now()) + ".");
	}

	@Test
	void sendsTheForm()
	{
		FormTester form = tester.newFormTester("themed:form");
		form.setValue("departure", "2026-12-24T18:30");
		form.submit();

		tester.assertNoErrorMessage();
		String choices = tester.getComponentFromLastRenderedPage("themed:choices")
			.getDefaultModelObjectAsString();
		tester.assertInfoMessages("Sent: " + choices);
		assertTrue(choices.endsWith("departure Dec 24, 2026, 6:30\u202fPM."), choices);
	}
}
