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
package com.antilia.widgets.demo.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.datatable.DatabaseLocator;
import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UsersDatabase;
import com.antilia.widgets.jsonlist.JsonBasedInfiniteList;
import com.antilia.widgets.modal.Modal;

/**
 * Shows, edits and deletes the users of {@link JsonListPage} through the actions of its list.
 */
class JsonListPageTest
{
	private static final String LIST = "themed:users";

	private static final String FORM = "dialog:overlay:dialog:content:body:form";

	private WicketTester tester;

	private JsonBasedInfiniteList<User, Long> list;

	private JsonListPage page;

	private User user;

	private String city;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(JsonListPage.class);
		list = (JsonBasedInfiniteList<User, Long>)tester.getComponentFromLastRenderedPage(LIST);
		user = DatabaseLocator.getLargeDatabase().find(0, 1, null).get(0);
		city = user.getCity();
	}

	@AfterEach
	void stop()
	{
		user.setCity(city);
		tester.destroy();
	}

	@Test
	void theListIsOneEmptyComponentLoadingItsCardsAsJson()
	{
		TagTester tag = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			list.getMarkupId());
		assertEquals("", tag.getValue());
		assertEquals("feed", tag.getAttribute("role"));
		assertEquals("Users", tag.getAttribute("aria-label"));
		tester.assertContains(Pattern.quote("new Wicket.JsonInfiniteList({"));
		tester.assertContains(Pattern.quote("user-card"));
		tester.assertContains(Pattern.quote("\"height\":{\"value\":70,\"unit\":\"vh\"}"));

		tester.executeBehavior(dataBehavior());
		String json = tester.getLastResponseAsString();
		assertEquals(JsonListPage.USERS_PER_PAGE, json.split("\"initials\"", -1).length - 1);
		assertTrue(json.contains("\"itemCount\":" + list.getItemCount()), json);
	}

	@Test
	void theTextsAndTheCardsAreInSpanishToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(JsonListPage.class);

		tester.assertContains(Pattern.quote("Ejemplo de JsonBasedInfiniteList"));
		tester.assertContains(Pattern.quote("title=\\\"Editar\\\""));
		tester.assertContains(Pattern.quote("\"loading\":\"Cargando…\""));
	}

	@Test
	void aClickOnACardShowsTheUserNextToTheList()
	{
		act("details", user);

		assertSame(user, page.getSelected());
		tester.assertContains(Pattern.quote(user.getLastName()));
	}

	@Test
	void editingRepaintsOnlyTheCardOfTheUser()
	{
		act("edit", user);
		assertTrue(((Modal)tester.getComponentFromLastRenderedPage("dialog")).isOpen());

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Springfield");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals("Springfield", user.getCity());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains(".renderItem(\"" + user.getId() + "\""), response);
		assertFalse(response.contains(".update({"), "the other cards are left alone");
		assertFalse(((Modal)tester.getComponentFromLastRenderedPage("dialog")).isOpen());
	}

	@Test
	void deletingAsksFirstThenRefreshesTheList()
	{
		UsersDatabase database = DatabaseLocator.getLargeDatabase();
		User added = new User("Zelda", "Zuse");
		database.save(added);
		try
		{
			act("delete", added);
			tester.assertContains(Pattern.quote("Delete Zelda Zuse?"));
			assertTrue(database.find(0, Integer.MAX_VALUE, null).contains(added));

			tester.clickLink("dialog:overlay:dialog:content:footer:buttons:2");

			assertFalse(database.find(0, Integer.MAX_VALUE, null).contains(added));
			tester.assertContains(Pattern.quote(".update({\"items\":["));
			tester.assertContains(Pattern.quote("Deleted Zelda Zuse."));
		}
		finally
		{
			if (database.find(0, Integer.MAX_VALUE, null).contains(added))
			{
				database.delete(added);
			}
		}
	}

	@Test
	void aClickOnAGoneUserIsReported()
	{
		tester.getRequest().getPostParameters().setParameterValue("action", "edit");
		tester.getRequest().getPostParameters().setParameterValue("key", "-1");
		tester.executeBehavior(list.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		tester.assertContains(Pattern.quote("That user no longer exists."));
		assertNull(page.getSelected());
	}

	private void act(String action, User target)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key",
			String.valueOf(target.getId()));
		tester.executeBehavior(list.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private AbstractAjaxBehavior dataBehavior()
	{
		return list.getBehaviors(AbstractAjaxBehavior.class)
			.stream()
			.filter(behavior -> !(behavior instanceof AbstractDefaultAjaxBehavior))
			.findFirst()
			.orElseThrow();
	}
}
