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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import com.antiliacode.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.dynamicdatatable.IDynamicColumn;
import com.antiliacode.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antiliacode.widgets.dynamicdatatable.RowNumberColumn;
import com.antiliacode.widgets.modal.Modal;
import com.antiliacode.widgets.table.MovableColumnsBehavior;
import com.antiliacode.widgets.table.ResizableColumnsBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import com.antiliacode.widgets.demo.WicketApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Edits and adds users of {@link DynamicDataTablePage}, and checks its columns.
 */
class DynamicDataTablePageTest
{
	private static final String TABLE_PARENT = "themed:tableParent";

	private static final String TABLE = TABLE_PARENT + ":table";

	private static final String DIALOG = "dialog";

	private static final String FORM = DIALOG + ":overlay:dialog:content:body:form";

	/** The second button of the dialog's footer, after Cancel. */
	private static final String CONFIRM = DIALOG + ":overlay:dialog:content:footer:buttons:2";

	private WicketTester tester;

	private JsonBasedDynamicDataTable<User, Long> table;

	private Modal dialog;

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
		dialog = (Modal)tester.getComponentFromLastRenderedPage(DIALOG);
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
		assertEquals(4, page.split("aria-pressed=\"false\"", -1).length - 1, page);
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

		assertTrue(dialog.isOpen());
		assertFalse(table.isOverlayShown());
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
			assertFalse(dialog.isOpen());
		}
		finally
		{
			database.delete(added);
		}
	}

	@Test
	void editShowsTheUserInAFormInTheDialog()
	{
		edit();

		assertTrue(dialog.isOpen());
		assertFalse(table.isOverlayShown(), "the table's own overlay is not used");
		tester.assertComponentOnAjaxResponse(DIALOG);
		tester.assertComponent(FORM.substring(0, FORM.lastIndexOf(':')), UserEditPanel.class);
		tester.assertModelValue(FORM + ":city", city);
		tester.assertModelValue(FORM + ":firstName", user.getFirstName());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Modal.attach("), response);
		assertTrue(response.contains("Edit " + user.getFirstName() + " " + user.getLastName()),
			response);
		assertTrue(response.contains("Discard the changes?"), response);
	}

	@Test
	void theDialogNamesItsFieldsAsItsOwnResourcesSay()
	{
		edit();

		// not "First Name", the header of the page's column, which would win over the panel's
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("First name <input"), response);
		assertTrue(response.contains("Last name <input"), response);
	}

	@Test
	void theCountriesComeInTheOrderOfTheLanguage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		edit();

		@SuppressWarnings("unchecked")
		List<String> countries = (List<String>)((DropDownChoice<String>)tester
			.getComponentFromLastRenderedPage(FORM + ":country")).getChoices();
		// Österreich between Oman and Pakistan, not after Zypern
		assertEquals(countries.indexOf("OM") + 1, countries.indexOf("AT"));
		assertEquals(countries.indexOf("AT") + 1, countries.indexOf("PK"));
	}

	@Test
	void savingUpdatesTheUserAndClosesTheDialog()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Paris");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals("Paris", DatabaseLocator.getLargeDatabase().get(user.getId()).getCity());
		assertFalse(dialog.isOpen());
		assertEquals(user, ((DynamicDataTablePage)tester.getLastRenderedPage()).getSelected());
	}

	@Test
	void anEmptyRequiredFieldKeepsTheDialogOpen()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "");
		tester.executeAjaxEvent(FORM + ":save", "click");

		assertEquals(city, user.getCity());
		assertTrue(dialog.isOpen());
		tester.assertComponentOnAjaxResponse(FORM);
	}

	@Test
	void cancellingClosesTheDialogWithoutSaving()
	{
		edit();

		FormTester form = tester.newFormTester(FORM);
		form.setValue("city", "Paris");
		tester.clickLink(FORM + ":cancel", true);

		assertEquals(city, user.getCity());
		assertFalse(dialog.isOpen());
	}

	@Test
	void theDialogClosesWhenTheBrowserAsks()
	{
		edit();

		tester.executeBehavior(dialog.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));

		assertFalse(dialog.isOpen());
		assertEquals(city, user.getCity());
	}

	@Test
	void deletingAsksInTheDialogFirst()
	{
		UsersDatabase database = DatabaseLocator.getLargeDatabase();
		User added = new User("Zelda", "Zuse");
		database.save(added);
		try
		{
			executeAction("delete", String.valueOf(added.getId()));

			assertTrue(dialog.isOpen());
			assertFalse(table.isOverlayShown());
			tester.assertContains(Pattern.quote("Delete Zelda Zuse?"));
			assertTrue(isSaved(added));

			tester.clickLink(CONFIRM);

			assertFalse(dialog.isOpen());
			assertFalse(isSaved(added));
			tester.assertContains(Pattern.quote("Deleted Zelda Zuse."));
		}
		finally
		{
			if (isSaved(added))
			{
				database.delete(added);
			}
		}
	}

	@Test
	void aUserDeletedWhileTheDialogAskedIsReported()
	{
		UsersDatabase database = DatabaseLocator.getLargeDatabase();
		User added = new User("Zelda", "Zuse");
		database.save(added);
		executeAction("delete", String.valueOf(added.getId()));
		database.delete(added);

		tester.clickLink(CONFIRM);

		assertFalse(dialog.isOpen());
		tester.assertContains(Pattern.quote("That user no longer exists."));
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

		tester.clickLink("actions:resetColumns", true);

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
	void thePlacementOfTheOverlayIsNotOffered()
	{
		tester.assertInvisible("options:overlayOption");
		tester.assertContainsNot("Edit form and confirmation");
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
		assertTrue(parent.getAttribute("class").contains("ant-dataview-sized"));
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

		tester.getRequest().setMethod("GET");
		tester.executeUrl("api/users?first=0&count=3&sort=lastName&ascending=true&filter=jans");
		String json = tester.getLastResponseAsString();
		assertTrue(json.startsWith("{\"rows\":[{\"key\":"), json);
		assertTrue(json.contains("\"lastName\":\"Jansen\""), json);
	}

	@Test
	void everySettingHasATooltipAndIsPostedViaAjax()
	{
		String page = tester.getLastResponseAsString();
		// with "Rows from", which the JSON table has besides infinite scroll, and without the
		// placement of the overlay, which it does not use
		assertEquals(14, page.split("class=\"ant-dataview-tooltip\" role=\"tooltip\"", -1)
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

	private static boolean isSaved(User user)
	{
		return DatabaseLocator.getLargeDatabase().find(0, Integer.MAX_VALUE, null).contains(user);
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
