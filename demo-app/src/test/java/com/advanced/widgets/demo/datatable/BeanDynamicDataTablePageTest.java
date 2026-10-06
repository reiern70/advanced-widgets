/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import com.advanced.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import com.advanced.widgets.demo.WicketApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks the table of {@link BeanDynamicDataTablePage}, whose rows are rendered on the server.
 */
class BeanDynamicDataTablePageTest
{
	private static final String TABLE = "themed:tableParent:table";

	private static final String FORM = TABLE + ":overlay:window:content:body:form";

	private WicketTester tester;

	private BeanBasedDynamicDataTable<User, Long, String> table;

	private User user;

	private String city;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(BeanDynamicDataTablePage.class);
		table = (BeanBasedDynamicDataTable<User, Long, String>)tester
			.getComponentFromLastRenderedPage(TABLE);
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
	void theRowsOfThePageAreRenderedOnTheServer()
	{
		String page = tester.getLastResponseAsString();
		TagTester row = TagTester.createTagByAttribute(page, "data-key",
			String.valueOf(user.getId()));

		assertEquals("tr", row.getName());
		assertTrue(page.contains("\"serverRows\":true"), page);
		assertTrue(page.contains(user.getLastName()), page);
		assertTrue(page.contains("data-dt-action=\"edit\""), page);
	}

	@Test
	void selectionNumberActionsAndIdCannotBeResized()
	{
		assertEquals(4, TagTester.createTagsByAttribute(tester.getLastResponseAsString(),
			"data-resizable", "false", false).size());
	}

	@Test
	void theExtraColumnsAreHiddenAtFirst()
	{
		List<IColumn<User, String>> hidden = table.getColumns()
			.stream()
			.filter(column -> table.isColumnVisible(column) == false)
			.toList();

		assertEquals(4, hidden.size());
		assertEquals(10, table.getVisibleColumns().size());
	}

	@Test
	void pagingRendersTheRowsAgain()
	{
		tester.clickLink(TABLE + ":bottomNavigation:span:paging:navigator:next");

		tester.assertComponentOnAjaxResponse(table.get("body"));
		assertTrue(tester.getLastResponseAsString().contains(".rowsReplaced();"));
	}

	@Test
	void editingSavesTheUserAndRendersTheRowsAgain()
	{
		executeAction("edit", String.valueOf(user.getId()));
		assertTrue(table.isOverlayShown());

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Paris");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals("Paris", DatabaseLocator.getLargeDatabase().get(user.getId()).getCity());
		assertFalse(table.isOverlayShown());
		tester.assertComponentOnAjaxResponse(table.get("body"));
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		tester.getRequest().getPostParameters().setParameterValue("key", key);
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class)
			.stream()
			.filter(behavior -> behavior.getClass().getSimpleName().equals("ActionBehavior"))
			.findFirst()
			.orElseThrow());
	}
}
