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
package com.antilia.widgets.demo.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.github.openjson.JSONObject;

/**
 * Shows the notifications {@link NotificationsPage} chooses, via Ajax, and those of its feedback
 * messages; and gives the React and Svelte apps the same texts and form.
 */
class NotificationsPageTest
{
	private WicketTester tester;

	private NotificationsPage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(NotificationsPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private String id()
	{
		return page.getNotifications().getMarkupId();
	}

	@Test
	void rendersTheNotificationsAndTheFormsWithTheirDefaults()
	{
		tester.assertContains("Wicket.Notifications.attach\\(\"" + id() + "\"");
		tester.assertContains("data-demo=\"notifications\"");
		tester.assertContains("antilia-notifications.*\\.css");
		tester.assertContains("NotificationsPage.*\\.css");
		tester.assertModelValue("themed:showForm:title", "Saved");
		tester.assertModelValue("themed:showForm:message", "The changes were saved.");
		tester.assertContains("<option selected=\"selected\" value=\"success\">Success</option>");
		tester.assertContains("<option selected=\"selected\" value=\"top-right\">Top right</option>");
		tester.assertContains("<option selected=\"selected\" value=\"-1\">Default: 5 seconds, " +
			"errors until closed</option>");
		tester.assertModelValue("themed:showForm:html", false);
		tester.assertContains("<option value=\"0\">Until closed</option>");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
	}

	@Test
	void showsTheNotificationTheFormChooses()
	{
		FormTester form = tester.newFormTester("themed:showForm");
		form.select("type", 1);
		form.setValue("title", "Oops");
		form.setValue("message", "Something broke.");
		form.select("position", 4);
		form.select("timeout", 4);
		form.submit("show");
		tester.executeAjaxEvent("themed:showForm:show", "click");

		tester.assertContains("Wicket.Notifications.show\\(\"" + id() + "\", \\{\"type\":\"error\"," +
			"\"title\":\"Oops\",\"body\":\"Something broke.\",\"timeout\":0,\"position\":" +
			"\"bottom-center\"\\}\\);");
	}

	@Test
	void showsAMessageOfHtmlWithTheTimeoutOfTheNotifications()
	{
		FormTester form = tester.newFormTester("themed:showForm");
		form.setValue("message", "The user <strong>Ada</strong> was saved.");
		form.setValue("html", true);
		form.submit();
		tester.executeAjaxEvent("themed:showForm:show", "click");

		tester.assertContains("\\{\"type\":\"success\",\"title\":\"Saved\",\"body\":\"The user " +
			"<strong>Ada<\\\\/strong> was saved.\",\"html\":true,\"position\":\"top-right\"\\}");
	}

	@Test
	void showsTheNoteWithItsLightBulb()
	{
		FormTester form = tester.newFormTester("themed:showForm");
		form.select("type", 4);
		form.submit();
		tester.executeAjaxEvent("themed:showForm:show", "click");

		tester.assertContains("\\{\"type\":\"note\",\"title\":\"Saved\"");
		tester.assertContains("\"icon\":\"" + NotificationsPage.NOTE_ICON + "\"");
	}

	@Test
	void showsTheNotificationOnANewPageAfterARedirect()
	{
		FormTester form = tester.newFormTester("themed:showForm");
		form.setValue("title", "Welcome back");
		form.submit("showAfterRedirect");

		NotificationsPage next = (NotificationsPage)tester.getLastRenderedPage();
		assertTrue(next != page, "a new page");
		tester.assertContains("Wicket.Notifications.show\\(\"" +
			next.getNotifications().getMarkupId() + "\", \\{\"type\":\"success\"," +
			"\"title\":\"Welcome back\",\"body\":\"The changes were saved.\"");
	}

	@Test
	void closesAllNotifications()
	{
		tester.clickLink("themed:showForm:closeAll");

		tester.assertContains("Wicket.Notifications.closeAll\\(\"" + id() + "\"\\);");
	}

	@Test
	void showsTheErrorOfTheRequiredNameAsANotification()
	{
		tester.executeAjaxEvent("themed:saveForm:save", "click");

		tester.assertContains("Wicket.Notifications.show\\(\"" + id() + "\", \\{\"type\":\"error\"," +
			"\"title\":\"Error\",\"body\":\"'Name' is required.\"\\}\\);");
	}

	@Test
	void showsTheSuccessOfSavingAsANotification()
	{
		FormTester form = tester.newFormTester("themed:saveForm", false);
		form.setValue("name", " Ada ");
		tester.executeAjaxEvent("themed:saveForm:save", "click");

		tester.assertContains("\\{\"type\":\"success\",\"title\":\"Success\",\"body\":" +
			"\"Hello, Ada! Your name was saved.\"\\}");
	}

	@Test
	void givesTheAppsTheTextsInTheLanguageOfTheSessionAndTheForm()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		page = tester.startPage(NotificationsPage.class);

		JSONObject config = page.getFrameworkConfig();
		JSONObject texts = config.getJSONObject("texts");
		assertEquals("Titel", texts.getString("title"));
		assertEquals("Schließen", texts.getString("close"));
		assertEquals("Notiz", texts.getJSONObject("types").getString("note"));
		assertEquals("Unten mittig", texts.getJSONObject("positions").getString("bottom-center"));
		assertEquals("Bis geschlossen", texts.getJSONObject("timeouts").getString("0"));
		assertEquals("'{0}' ist ein Pflichtfeld.", texts.getString("required"));
		assertEquals("success", config.getString("type"));
		assertEquals("Gespeichert", config.getString("title"));
		assertEquals("top-right", config.getString("position"));
		assertEquals(-1, config.getInt("timeout"));
		assertFalse(config.getBoolean("html"));
		assertEquals("Die Nachricht ist HTML", texts.getString("html"));
		assertEquals("Standard: 5 Sekunden, Fehler bis geschlossen",
			texts.getJSONObject("timeouts").getString("-1"));
		assertTrue(tester.getLastResponseAsString().contains("\"label\":\"Mitteilungen\""));
	}
}
