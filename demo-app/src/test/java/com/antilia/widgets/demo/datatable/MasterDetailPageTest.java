/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.table.ResizableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;

/**
 * Checks {@link MasterDetailPage}: the users of the country selected in the master, and the
 * settings of both tables.
 */
class MasterDetailPageTest
{
	private static final String TABLES = "themed:tables:";

	private WicketTester tester;

	private JsonBasedDynamicDataTable<?, ?> countries;

	private BeanBasedDynamicDataTable<?, ?, ?> users;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(MasterDetailPage.class);
		countries = (JsonBasedDynamicDataTable<?, ?>)tester
			.getComponentFromLastRenderedPage(TABLES + "countries");
		users = (BeanBasedDynamicDataTable<?, ?, ?>)tester
			.getComponentFromLastRenderedPage(TABLES + "users");
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void theDetailShowsTheUsersOfTheCountrySelectedInTheMaster()
	{
		User user = DatabaseLocator.getLargeDatabase().find(0, 1, null).get(0);
		long all = users.getItemCount();
		tester.assertLabel(TABLES + "usersTitle", "All users");

		select(BaseDynamicDataTable.SELECT_ACTION, user.getCountry());

		assertTrue(users.getItemCount() < all);
		users.getProvider().iterator(0, users.getItemCount()).forEachRemaining(
			row -> assertEquals(user.getCountry(), ((User)row).getCountry()));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Users in " + user.getCountryName()), response);
		assertTrue(response.contains(user.getLastName()), response);

		select(BaseDynamicDataTable.DESELECT_ACTION, user.getCountry());

		assertEquals(all, users.getItemCount());
	}

	@Test
	void theSettingsApplyToBothTables()
	{
		assertEquals(Mode.STRETCH, mode(countries));
		assertEquals(Mode.STRETCH, mode(users));
		assertTrue(countries.isAdjustToParentWidth());

		change("resize", "fit");
		assertEquals(Mode.FIT, mode(countries));
		assertEquals(Mode.FIT, mode(users));
		tester.assertComponentOnAjaxResponse(TABLES.substring(0, TABLES.length() - 1));

		change("resize", "none");
		assertTrue(countries.getBehaviors(ResizableColumnsBehavior.class).isEmpty());
		assertTrue(users.getBehaviors(ResizableColumnsBehavior.class).isEmpty());

		change("width", "content");
		assertFalse(countries.isAdjustToParentWidth());
		assertFalse(users.isAdjustToParentWidth());

		change("layout", "sideBySide");
		tester.assertContains("master-detail--sideBySide");
	}

	private void select(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(countries.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	private void change(String id, String value)
	{
		FormComponent<?> choice = (FormComponent<?>)tester
			.getComponentFromLastRenderedPage("themed:" + id);
		tester.getRequest().getPostParameters().setParameterValue(choice.getInputName(), value);
		tester.executeAjaxEvent(choice, "change");
	}

	private static Mode mode(BaseDynamicDataTable<?, ?, ?> table)
	{
		return table.getBehaviors(ResizableColumnsBehavior.class).get(0).getMode();
	}
}
