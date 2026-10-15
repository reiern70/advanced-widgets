/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.modal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.modal.Modal;

/**
 * Adds, edits and deletes the members of the team of {@link ModalDialogPage} in its dialog, and
 * through the {@link TeamResource} the React and Svelte apps use.
 */
class ModalDialogPageTest
{
	private static final String FORM = "dialog:overlay:dialog:content:body:form";

	/** The second button of the footer: Save in the form, Delete in the confirmation. */
	private static final String SAVE = "dialog:overlay:dialog:content:footer:buttons:2";

	private WicketTester tester;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(ModalDialogPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void showsTheTeamAndADialogThatClosesOnEscapeAndAClickAndTrapsTheFocus()
	{
		tester.assertContains(">Ada Lovelace<");
		tester.assertContains(">Development<");
		ModalDialog dialog = (ModalDialog)tester.getComponentFromLastRenderedPage("dialog");
		assertTrue(!dialog.isOpen());
		tester.assertContains("data-demo=\"modalDialog\"");

		tester.clickLink("themed:add");
		assertTrue(dialog.isOpen());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("dialog-theme-default"), response);
		assertTrue(response.contains(">New member<"), response);
		assertTrue(response.contains("Wicket.Modal.attach("), response);
		assertTrue(response.contains("\"closeOnClick\":true"), response);
		assertTrue(response.contains("Discard the changes?"), response);
		assertTrue(response.contains("data-modal-close=\"\""), response);
		assertTrue(response.contains("<span class=\"ant-button__label\">Cancel</span>"), response);
		assertTrue(response.contains("<span class=\"ant-button__label\">Save</span>"), response);
		assertTrue(response.contains("\"size\":\"medium\""), response);
	}

	@Test
	void closesWhenTheBrowserAsks()
	{
		tester.clickLink("themed:add");
		Modal dialog = (Modal)tester.getComponentFromLastRenderedPage("dialog");

		tester.executeBehavior(dialog.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		assertFalse(dialog.isOpen());
	}

	@Test
	void addsAMemberValidatedAsTheResourceValidates()
	{
		tester.clickLink("themed:add");
		FormTester form = tester.newFormTester(FORM);
		form.setValue("name", "");
		form.setValue("email", "not an address");
		tester.executeAjaxEvent(SAVE, "click");
		Map<String, String> expected = TeamStore.validate(member("", "not an address"),
			Locale.ENGLISH);
		tester.assertErrorMessages(expected.values().toArray(new Serializable[0]));
		// the form in the dialog shows its errors, which are not notifications
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));

		form = tester.newFormTester(FORM);
		form.setValue("name", "Margaret Hamilton");
		form.setValue("email", "margaret@example.com");
		tester.executeAjaxEvent(SAVE, "click");

		tester.assertInfoMessages("Added Margaret Hamilton.");
		// the page's message is a notification of its base page
		tester.assertContains("Wicket.Notifications.show\\(\"[^\"]+\", \\{\"type\":\"info\"," +
			"\"title\":\"Information\",\"body\":\"Added Margaret Hamilton.\"\\}\\);");
		assertTrue(DemoSession.get().getTeam().list().stream()
			.anyMatch(member -> member.getName().equals("Margaret Hamilton")));
	}

	@Test
	void editsAndDeletesAMember()
	{
		tester.clickLink("themed:team:members:0:edit");
		tester.assertContains(">Edit Ada Lovelace<");
		FormTester form = tester.newFormTester(FORM);
		form.setValue("name", "Ada King");
		tester.executeAjaxEvent(SAVE, "click");
		tester.assertInfoMessages("Saved Ada King.");
		tester.cleanupFeedbackMessages();

		tester.clickLink("themed:team:members:0:delete");
		tester.assertContains(Pattern.quote("Delete Ada King from the team?"));
		tester.clickLink(SAVE);
		tester.assertInfoMessages("Deleted Ada King.");
		assertEquals(3, DemoSession.get().getTeam().list().size());
	}

	@Test
	void opensADialogWithTheOptionsChosenAndNestsDialogsInIt()
	{
		String options = "themed:options:";
		tester.newFormTester("themed:options", false).setValue("closeOnEscape", false);
		tester.executeAjaxEvent(options + "closeOnEscape", "change");
		tester.newFormTester("themed:options", false).setValue("closeButton", false);
		tester.executeAjaxEvent(options + "closeButton", "change");
		tester.newFormTester("themed:options", false).select("size", 3);
		tester.executeAjaxEvent(options + "size", "change");
		tester.newFormTester("themed:options", false).setValue("width", "wide");
		tester.executeAjaxEvent(options + "width", "change");
		tester.assertErrorMessages(
			"Width of its own takes a number and a CSS unit, such as 40em or 600px.");
		tester.clearFeedbackMessages();
		tester.newFormTester("themed:options", false).setValue("width", "45em");
		tester.executeAjaxEvent(options + "width", "change");
		tester.assertNoErrorMessage();

		tester.clickLink(options + "open");
		Modal first = (Modal)tester.getComponentFromLastRenderedPage("optionsDialog");
		assertTrue(first.isOpen());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"closeOnEscape\":false"), response);
		assertTrue(response.contains("\"size\":\"large\""), response);
		assertTrue(response.contains("\"width\":\"45em\""), response);
		assertTrue(response.contains(">Dialog 1<"), response);
		assertFalse(first.isCloseButton());

		String nested = "optionsDialog:overlay:dialog:content:";
		tester.clickLink(nested + "footer:buttons:2");
		Modal second = (Modal)tester.getComponentFromLastRenderedPage(nested + "body:nested");
		assertTrue(second.isOpen());
		assertTrue(tester.getLastResponseAsString().contains(">Dialog 2<"));

		nested += "body:nested:overlay:dialog:content:";
		tester.clickLink(nested + "footer:buttons:2");
		assertTrue(tester.getLastResponseAsString().contains(">Dialog 3<"));
		assertTrue(tester.getLastResponseAsString().contains("as deep as this demo goes"));

		// the deepest has no button opening another, its OK closes it
		nested += "body:nested:overlay:dialog:content:";
		tester.clickLink(nested + "footer:buttons:2");
		assertTrue(second.isOpen());
		assertTrue(first.isOpen());
	}

	@Test
	void theTextsAreInSpanishToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(ModalDialogPage.class);

		tester.assertContains(Pattern.quote("Ejemplo de diálogo modal"));
		tester.assertContains(">Añadir miembro<");
		tester.assertContains(">Desarrollo<");
	}

	@Test
	void theResourceListsAddsUpdatesAndDeletes()
	{
		TeamStore team = DemoSession.get().getTeam();
		assertTrue(TeamResource.members(team, Locale.ENGLISH).toString()
			.contains("\"roleName\":\"Development\""));
		assertEquals(Map.of(), TeamStore.validate(member("Kim", "kim@example.com"),
			Locale.ENGLISH));
		assertEquals(Map.of("name", "'Name' is required."),
			TeamStore.validate(member(" ", "kim@example.com"), Locale.ENGLISH));
		Map<String, String> german = TeamStore.validate(member("Kim", "kim"), Locale.GERMAN);
		assertEquals(1, german.size());
		assertTrue(german.get("email").contains("'E-Mail'"), german.toString());

		TeamResource.Answer added = TeamResource.answer("POST", 0, "{\"name\":\"Kim\"," +
			"\"email\":\"kim@example.com\",\"role\":\"tester\"}", team, Locale.ENGLISH);
		assertEquals(201, added.status());
		assertEquals("Kim", added.json().getJSONObject("member").getString("name"));
		long id = added.json().getJSONObject("member").getLong("id");

		TeamResource.Answer invalid = TeamResource.answer("PUT", id, "{\"name\":\"Kim\"," +
			"\"email\":\"\",\"role\":\"tester\"}", team, Locale.ENGLISH);
		assertEquals(400, invalid.status());
		assertEquals("'Email' is required.", invalid.json().getJSONObject("errors")
			.getString("email"));
		assertEquals(200, TeamResource.answer("PUT", id, "{\"name\":\"Kim Lee\"," +
			"\"email\":\"kim@example.com\",\"role\":\"manager\"}", team, Locale.ENGLISH).status());
		assertEquals("Kim Lee", team.find(id).getName());
		assertEquals(404, TeamResource.answer("PUT", 999, "{\"name\":\"X\"," +
			"\"email\":\"x@example.com\",\"role\":\"tester\"}", team, Locale.ENGLISH).status());

		tester.getRequest().setMethod("GET");
		tester.executeUrl("api/team");
		assertTrue(tester.getLastResponseAsString().startsWith("{\"members\":[{\"id\":"),
			tester.getLastResponseAsString());
		tester.getRequest().setMethod("DELETE");
		tester.executeUrl("api/team?id=1");
		assertEquals(204, tester.getLastResponse().getStatus());
		tester.getRequest().setMethod("DELETE");
		tester.executeUrl("api/team?id=1");
		assertEquals(404, tester.getLastResponse().getStatus());
	}

	private static TeamMember member(String name, String email)
	{
		TeamMember member = new TeamMember();
		member.setName(name);
		member.setEmail(email);
		return member;
	}
}
