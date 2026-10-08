/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import com.advanced.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.OverlayPlacement;
import com.advanced.widgets.dynamicdatatable.IDynamicColumn;
import com.advanced.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.RowNumberColumn;
import com.advanced.widgets.table.MovableColumnsBehavior;
import com.advanced.widgets.table.ResizableColumnsBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import com.advanced.widgets.demo.WicketApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Edits and adds users of {@link DynamicDataTablePage}, and checks its columns.
 */
class DynamicDataTablePageTest
{
	private static final String TABLE_PARENT = "themed:tableParent";

	private static final String TABLE = TABLE_PARENT + ":table";

	private static final String FORM = TABLE + ":overlay:window:content:body:form";

	private WicketTester tester;

	private JsonBasedDynamicDataTable<User, Long> table;

	private User user;

	private String city;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		tester.startPage(DynamicDataTablePage.class);
		table = (JsonBasedDynamicDataTable<User, Long>)tester
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
	void actionsFollowTheRowNumberWithEditBeforeDeleteAndDownload()
	{
		List<IDynamicColumn<User>> columns = table.getColumns();
		assertTrue(columns.get(1) instanceof RowNumberColumn);
		IDynamicColumn<User> actions = columns.get(2);
		assertTrue(actions instanceof CompoundDynamicColumn);
		String template = actions.getTemplate(table);
		int edit = template.indexOf("data-dt-action=\"edit\"");
		int delete = template.indexOf("data-dt-action=\"delete\"");
		int download = template.indexOf("data-dt-action=\"download\"");
		assertTrue(edit >= 0 && edit < delete && delete < download, template);
		assertEquals(1, columns.stream()
			.filter(column -> column.getTemplate(table).contains("data-dt-action=\"edit\""))
			.count());
	}

	@Test
	void selectionNumberActionsAndIdCannotBeResized()
	{
		List<Boolean> resizable = table.getColumns()
			.stream()
			.map(IDynamicColumn::isResizable)
			.toList();

		assertEquals(List.of(false, false, false, false), resizable.subList(0, 4));
		assertTrue(resizable.subList(4, resizable.size()).stream().allMatch(Boolean::booleanValue));
		assertEquals(4, TagTester.createTagsByAttribute(tester.getLastResponseAsString(),
			"data-resizable", "false", false).size());
	}

	@Test
	void theExtraColumnsAreHiddenAtFirstAndOfferedByTheColumnChooser()
	{
		List<String> hidden = table.getColumns()
			.stream()
			.filter(column -> table.isColumnVisible(column) == false)
			.map(column -> column.getTemplate(table))
			.toList();

		assertEquals(List.of("{{homePhone}}", "{{cellPhone}}", "{{city}}", "{{countryName}}"),
			hidden);
		String page = tester.getLastResponseAsString();
		assertEquals(2 * 4, page.split("aria-pressed=\"false\"", -1).length - 1, page);
	}

	@Test
	void theSelectionRowNumberIdAndActionsAreFixedAndTheOthersMovable()
	{
		List<IDynamicColumn<User>> columns = table.getColumns();
		for (int i = 0; i < columns.size(); i++)
		{
			assertEquals(i > 3, table.isMovable(columns.get(i)), String.valueOf(i));
		}
	}

	@Test
	void anAddedUserIsSavedAndSelected()
	{
		UsersDatabase database = DatabaseLocator.getLargeDatabase();
		int count = database.getCount();
		Component add = ((MarkupContainer)table.get("topNavigation")).visitChildren(Component.class,
			(child, visit) -> {
				if ("button".equals(child.getId()))
				{
					visit.stop(child);
				}
			});
		tester.clickLink(add);

		assertTrue(table.isOverlayShown());
		FormTester form = tester.newFormTester(FORM);
		form.setValue("firstName", "Zoe");
		form.setValue("lastName", "Quintana");
		form.setValue("country", "PA");
		form.setValue("city", "Panama");
		form.setValue("address", "1 Via Espana");
		tester.executeAjaxEvent(FORM + ":save", "click");

		User added = ((DynamicDataTablePage)tester.getLastRenderedPage()).getSelected();
		try
		{
			assertEquals(count + 1, database.getCount());
			assertEquals("Quintana", added.getLastName());
			assertEquals(added, database.get(added.getId()));
			assertFalse(table.isOverlayShown());
		}
		finally
		{
			database.delete(added);
		}
	}

	@Test
	void editShowsTheUserInAFormInTheOverlay()
	{
		edit();

		assertTrue(table.isOverlayShown());
		tester.assertComponentOnAjaxResponse(TABLE + ":overlay");
		tester.assertComponent(FORM.substring(0, FORM.lastIndexOf(':')), UserEditPanel.class);
		tester.assertModelValue(FORM + ":city", city);
		tester.assertModelValue(FORM + ":firstName", user.getFirstName());
	}

	@Test
	void savingUpdatesTheUserAndClosesTheOverlay()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Paris");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals("Paris", DatabaseLocator.getLargeDatabase().get(user.getId()).getCity());
		assertFalse(table.isOverlayShown());
		tester.assertComponentOnAjaxResponse(TABLE + ":overlay");
	}

	@Test
	void anEmptyRequiredFieldKeepsTheOverlayOpen()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals(city, user.getCity());
		assertTrue(table.isOverlayShown());
		tester.assertComponentOnAjaxResponse(FORM);
		assertTrue(tester.getLastResponseAsString().contains("data-dt-changed=\"true\""));
	}

	@Test
	void cancellingClosesTheOverlayWithoutSaving()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Paris");
		tester.clickLink(FORM + ":cancel", true);

		assertEquals(city, user.getCity());
		assertFalse(table.isOverlayShown());
	}

	@Test
	void escapeClosesTheOverlay()
	{
		edit();

		executeAction(BaseDynamicDataTable.CLOSE_OVERLAY_ACTION, null);

		assertFalse(table.isOverlayShown());
		assertEquals(city, user.getCity());
	}

	@Test
	@SuppressWarnings("unchecked")
	void theColumnsAreRememberedAndCanBeReset()
	{
		List<String> initial = table.getColumnState().getOrder();
		int shown = table.getVisibleColumns().size();
		tester.getRequest().getPostParameters().setParameterValue("from", "5");
		tester.getRequest().getPostParameters().setParameterValue("to", String.valueOf(shown));
		tester.executeBehavior(table.getBehaviors(MovableColumnsBehavior.class).get(0));
		tester.getRequest().getPostParameters().setParameterValue("widths",
			String.join(",", Collections.nCopies(shown, "50")));
		tester.executeBehavior(table.getBehaviors(ResizableColumnsBehavior.class).get(0));
		List<String> moved = table.getColumnState().getOrder();

		tester.startPage(DynamicDataTablePage.class);
		table = (JsonBasedDynamicDataTable<User, Long>)tester
			.getComponentFromLastRenderedPage(TABLE);
		assertEquals(moved, table.getColumnState().getOrder(), "a new page keeps the order");
		assertEquals(50d, table.getColumnWidths()[0]);

		tester.clickLink("options:resetColumns", true);

		assertEquals(initial, table.getColumnState().getOrder());
		assertEquals(shown, table.getVisibleColumns().size());
		assertNull(table.getColumnWidths());
	}

	@Test
	void theSelectionBarOptionIsTheBulkSelectionToolbarPosition()
	{
		tester.assertContains("Bulk selection toolbar position");
	}

	@Test
	void theEditFormIsShownOverThePage()
	{
		assertEquals(OverlayPlacement.BODY, table.getOverlayPlacement());

		edit();

		TagTester overlay = tester.getTagById(table.get("overlay").getMarkupId());
		assertTrue(overlay.getAttribute("class").contains("dynamic-data-table-overlay-body"));
	}

	@Test
	void theBodyIs800PixelsHighAndTheOptionsChangeIt()
	{
		assertEquals(800, table.getBodyHeight());
		assertEquals(BodyHeightUnits.PIXELS, table.getBodyHeightUnits());
		tester.assertContains(Pattern.quote("\"bodyHeight\":{\"value\":800,\"unit\":\"px\"}"));

		tester.getRequest().getPostParameters().setParameterValue("options:bodyHeightUnits",
			String.valueOf(BodyHeightUnits.VIEWPORT_HEIGHT.ordinal()));
		tester.executeAjaxEvent("options:bodyHeightUnits", "change");

		assertEquals(100, table.getBodyHeight(), "a unit starts with its default height");
		tester.assertComponentOnAjaxResponse("options:bodyHeight");
		tester.getRequest().getPostParameters().setParameterValue("options:bodyHeight", "60");
		tester.executeAjaxEvent("options:bodyHeight", "change");

		assertEquals(60, table.getBodyHeight());
		assertEquals(BodyHeightUnits.VIEWPORT_HEIGHT, table.getBodyHeightUnits());
		tester.assertComponentOnAjaxResponse(TABLE_PARENT);

		tester.getRequest().getPostParameters().setParameterValue("options:bodyHeight", "0");
		tester.executeAjaxEvent("options:bodyHeight", "change");

		assertEquals(-1, table.getBodyHeight(), "0 lets the body take the height of its rows");
		tester.assertComponentOnAjaxResponse(TABLE_PARENT);
		tester.assertContainsNot(Pattern.quote("\"bodyHeight\":{"));
	}

	@Test
	void thePercentOfTheParentGivesTheParentAHeight()
	{
		tester.getRequest().getPostParameters().setParameterValue("options:bodyHeightUnits",
			String.valueOf(BodyHeightUnits.PERCENT.ordinal()));
		tester.executeAjaxEvent("options:bodyHeightUnits", "change");

		assertEquals(100, table.getBodyHeight());
		TagTester parent = tester.getTagById(
			tester.getComponentFromLastRenderedPage(TABLE_PARENT).getMarkupId());
		assertTrue(parent.getAttribute("class").contains("dynamic-dataview-sized"));
	}

	@Test
	void infiniteScrollIsSwitchedOnViaAjax()
	{
		tester.getRequest().getPostParameters().setParameterValue(
			"options:infiniteScrollOption:infiniteScroll", "1");
		tester.executeAjaxEvent("options:infiniteScrollOption:infiniteScroll", "change");

		assertTrue(table.isInfiniteScroll());
		tester.assertComponentOnAjaxResponse(TABLE_PARENT);
		tester.assertContains(Pattern.quote("\"infinite\":true"));
		tester.assertContainsNot("Rows per page");
	}

	@Test
	void theRowsComeFromTheMountedResourceWhenChosen()
	{
		DynamicDataTablePage page = (DynamicDataTablePage)tester.getLastRenderedPage();
		JsonBasedDynamicDataTable<?, ?> table = (JsonBasedDynamicDataTable<?, ?>)page.getTable();
		table.setRowsSource(JsonBasedDynamicDataTable.RowsSource.RESOURCE);
		tester.startPage(page);
		tester.assertContains("\"dataUrl\":\"[^\"]*/api/users\\?first=0&count=25");

		tester.executeUrl("api/users?first=0&count=3&sort=lastName&ascending=true&filter=jans");
		String json = tester.getLastResponseAsString();
		assertTrue(json.startsWith("{\"rows\":[{\"key\":"), json);
		assertTrue(json.contains("\"lastName\":\"Jansen\""), json);
	}

	@Test
	void everySettingHasATooltipAndIsPostedViaAjax()
	{
		String page = tester.getLastResponseAsString();
		// with "Rows from", which the JSON table has besides infinite scroll
		assertEquals(13, page.split("class=\"dynamic-dataview-tooltip\" role=\"tooltip\"", -1)
			.length - 1);
		assertTrue(page.contains("Whether the user can resize the columns"), page);
		tester.assertContains("Resizable");
		tester.assertContains("stretching the table");
	}

	@Test
	void theLanguageIsSwitchedViaAjax()
	{
		FormTester form = tester.newFormTester("settings");
		form.select("language", 1);
		tester.executeAjaxEvent("settings:language", "change");

		assertEquals("de", tester.getSession().getLocale().getLanguage());
	}

	private void edit()
	{
		executeAction("edit", String.valueOf(user.getId()));
	}

	private void executeAction(String action, String key)
	{
		tester.getRequest().getPostParameters().setParameterValue("action", action);
		if (key != null)
		{
			tester.getRequest().getPostParameters().setParameterValue("key", key);
		}
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class)
			.stream()
			.filter(behavior -> behavior.getClass().getSimpleName().equals("ActionBehavior"))
			.findFirst()
			.orElseThrow());
	}
}
