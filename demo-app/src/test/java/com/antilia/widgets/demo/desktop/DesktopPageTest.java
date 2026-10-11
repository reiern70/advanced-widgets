/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.datatable.BeanUsersTablePanel;
import com.antilia.widgets.demo.datatable.JsonUsersTablePanel;
import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UsersTablePanel;
import com.antilia.widgets.demo.dropdown.DropdownExamplesPanel;
import com.antilia.widgets.desktop.Desktop;
import com.antilia.widgets.desktop.DesktopWindow;
import com.antilia.widgets.desktop.TaskbarPosition;
import com.antilia.widgets.desktop.WindowState;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.table.MovableColumnsBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Checks {@link DesktopPage}: the windows of the desktop, the JSON and bean grids of the grid demos
 * and the elements the React and Svelte grids mount into, the states the browser sends, the side of
 * the taskbar and whether the desktop fills the page, the button opening the keyboard window again
 * and the configuration of the React and Svelte apps.
 */
class DesktopPageTest
{
	private WicketTester tester;

	private DesktopPage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(DesktopPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private List<DesktopWindow> windows()
	{
		return page.getDesktop().getWindows();
	}

	/**
	 * @return the element the grid of a framework mounts into, in a window
	 */
	private static TagTester grid(String response, String framework)
	{
		return app(response, framework, "jsonTable");
	}

	/**
	 * @return the element an app of a framework mounts into, in a window
	 */
	private static TagTester app(String response, String framework, String demo)
	{
		List<TagTester> tags = TagTester.createTagsByAttribute(response, "data-demo", demo,
			false);
		return tags.stream()
			.filter(tag -> framework.equals(tag.getAttribute("data-framework-app")))
			.findFirst().orElseThrow();
	}

	private void send(JSONObject states)
	{
		send(states, "", "");
	}

	private void send(JSONObject states, String taskbar, String expanded)
	{
		Desktop desktop = page.getDesktop();
		tester.getRequest().setParameter("states", states.toString());
		tester.getRequest().setParameter("taskbar", taskbar);
		tester.getRequest().setParameter("expanded", expanded);
		tester.executeBehavior(desktop.get("desktop")
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0));
	}

	/**
	 * @return the components of a class in the windows of the desktop
	 */
	private <T extends Component> List<T> inWindows(Class<T> type)
	{
		List<T> found = new ArrayList<>();
		page.getDesktop().visitChildren(type, (T component, IVisit<Void> visit) -> {
			found.add(component);
			visit.dontGoDeeper();
		});
		return found;
	}

	@Test
	void showsTheGridsOfTheGridDemosAndTheKeyboardMinimized()
	{
		Desktop desktop = page.getDesktop();
		tester.assertContains("Wicket.Desktop.attach\\(\"" + desktop.getDesktopMarkupId() +
			"\", \\{\"taskbar\":\"bottom\",\"tiling\":true,\"expandable\":true," +
			"\"expanded\":false,\"movableTaskbar\":true,\"menu\":true,");
		tester.assertContains("JSON grid in Wicket");
		tester.assertContains("Bean grid in Wicket");
		tester.assertContains("JSON grid in React");
		tester.assertContains("JSON grid in Svelte");
		assertEquals(8, windows().size());
		assertEquals(1, inWindows(JsonUsersTablePanel.class).size());
		assertEquals(1, inWindows(BeanUsersTablePanel.class).size());
		for (UsersTablePanel users : inWindows(UsersTablePanel.class))
		{
			// the columns move by drag and drop, as in the grid demos
			assertEquals(1, users.getTable().getBehaviors(MovableColumnsBehavior.class).size());
			assertEquals(DesktopPage.BODY_HEIGHT, users.getTable().getBodyHeight());
		}

		String response = tester.getLastResponseAsString();
		TagTester react = grid(response, "react");
		assertEquals("jsonTable", react.getAttribute("data-demo"));
		assertEquals("true", react.getAttribute("data-mount"));
		JSONObject config = new JSONObject(react.getAttribute("data-config"));
		assertTrue(config.getBoolean("compact"));
		assertEquals(DesktopPage.BODY_HEIGHT, config.getJSONObject("bodyHeight").getInt("value"));
		JSONArray columns = config.getJSONArray("columns");
		assertEquals("number", columns.getJSONObject(0).getString("id"));
		assertEquals("actions", columns.getJSONObject(1).getString("id"));
		JSONObject firstName = columns.getJSONObject(3);
		assertEquals("First Name", firstName.getString("header"));
		assertEquals("firstName", firstName.getString("sort"));
		assertTrue(firstName.getBoolean("movable"));
		assertEquals("New user", config.getJSONObject("texts").getString("newUser"));
		assertTrue(config.has("licenseKey"), "the key of the demo");
		assertEquals("true", grid(response, "svelte").getAttribute("data-mount"));

		DesktopWindow keyboard = page.getKeyboardWindow();
		TagTester window = TagTester.createTagByAttribute(response, "id", keyboard.getMarkupId());
		assertEquals("", window.getAttribute("data-minimized"));
		assertEquals("true", window.getAttribute("data-closable"));
		assertEquals("hidden", window.getAttribute("hidden"));
		tester.assertContains("Ctrl</kbd>\\+<kbd>F6");
		tester.assertContains("Dragging the taskbar");
		tester.assertLabel("themed:states", "The server knows: taskbar below; JSON grid in " +
			"Wicket at 16, 16, " +
			"900 × 460; Bean grid in Wicket at 56, 56, 900 × 460; JSON grid in React at 96, 96, " +
			"900 × 460; JSON grid in Svelte at 136, 136, 900 × 460; Dropdowns in Wicket at 240, " +
			"60, 700 × 460; Dropdowns in React at 280, 100, 700 × 460; Dropdowns in " +
			"Svelte at 320, 140, 700 × 460; Keyboard at 560, 40, 600 × 480, minimized.");
	}

	@Test
	void showsTheDropdownsAndLetsEveryWindowCloseAskingWhenItHasChanges()
	{
		String response = tester.getLastResponseAsString();
		assertEquals(1, inWindows(DropdownExamplesPanel.class).size());
		tester.assertContains("Dropdowns in Wicket");
		for (String framework : new String[] { "react", "svelte" })
		{
			TagTester app = app(response, framework, "dropdown");
			assertEquals("true", app.getAttribute("data-mount"));
			JSONObject config = new JSONObject(app.getAttribute("data-config"));
			// the choices of the drop-downs in Wicket
			assertEquals("M", config.getJSONObject("values").getString("size"));
			assertEquals("One choice", config.getJSONObject("texts").getString("single"));
		}

		List<DesktopWindow> windows = windows();
		for (DesktopWindow window : windows)
		{
			TagTester tag = TagTester.createTagByAttribute(response, "id", window.getMarkupId());
			assertEquals("true", tag.getAttribute("data-closable"), window.getId());
		}
		// the Wicket grids and drop-downs tell by their fields, the apps' grids never have changes
		assertEquals(null, attribute(response, windows.get(0), "data-modified"));
		assertEquals("false", attribute(response, windows.get(2), "data-modified"));
		assertEquals("false", attribute(response, windows.get(3), "data-modified"));
		assertEquals(null, attribute(response, windows.get(4), "data-modified"));
		// all open
		for (DesktopWindow window : windows)
		{
			assertEquals(null, attribute(response, window, "data-closed"), window.getId());
		}
		// in the grids only the form of a user counts
		for (UsersTablePanel users : inWindows(UsersTablePanel.class))
		{
			assertEquals("false", TagTester.createTagByAttribute(response, "id",
				users.getTable().getMarkupId()).getAttribute("data-modified"));
		}
		tester.assertContains("The menu of the taskbar");
		tester.assertContains("\"menu\":\"All windows\"");
		tester.assertContains("\"discard\":\"Close and lose them\"");
	}

	private static String attribute(String response, DesktopWindow window, String name)
	{
		return TagTester.createTagByAttribute(response, "id", window.getMarkupId())
			.getAttribute(name);
	}

	@Test
	void keepsWhatTheBrowserSends()
	{
		DesktopWindow wicket = windows().get(0);
		DesktopWindow keyboard = page.getKeyboardWindow();
		send(new JSONObject()
			.put(wicket.getMarkupId(), new WindowState(30, 40, 500, 300, false, true, false, false,
				4).toJson())
			.put(keyboard.getMarkupId(), new WindowState(680, 24, 380, 260, false, false, false,
				true, 1).toJson()));

		assertEquals(new WindowState(30, 40, 500, 300, false, true, false, false, 4),
			wicket.getModelObject());
		assertTrue(keyboard.getState().isClosed());
		tester.assertComponentOnAjaxResponse("themed:states");
		assertTrue(tester.getLastResponseAsString().contains(
			"JSON grid in Wicket at 30, 40, 500 × 300, maximized"));
		assertTrue(tester.getLastResponseAsString().contains("Keyboard at 680, 24, 380 × 260, " +
			"closed"));
	}

	@Test
	void keepsTheSideTheUserMovedTheTaskbarToAndWhetherTheDesktopFillsThePage()
	{
		send(new JSONObject(), "right", "true");

		Desktop desktop = page.getDesktop();
		assertEquals(TaskbarPosition.RIGHT, desktop.getTaskbarPosition());
		assertTrue(desktop.isExpanded());
		// the choice above the desktop follows
		tester.assertComponentOnAjaxResponse("themed:form");
		TagTester selected = TagTester.createTagByAttribute(tester.getLastResponseAsString(),
			"selected", "selected");
		assertEquals("right", selected.getAttribute("value"));
		assertTrue(tester.getLastResponseAsString().contains(
			"The server knows: taskbar on the right, filling the page; "));

		tester.startPage(page);
		tester.assertContains("\"taskbar\":\"right\"");
		tester.assertContains("\"expanded\":true");
	}

	@Test
	void editsAUserInTheOverlayOfItsWindow()
	{
		JsonUsersTablePanel users = inWindows(JsonUsersTablePanel.class).get(0);
		JsonBasedDynamicDataTable<User, Long> table = users.getTable();
		User user = table.getProvider().iterator(0, 1).next();
		tester.getRequest().getPostParameters().setParameterValue("action", "edit");
		tester.getRequest().getPostParameters().setParameterValue("key",
			String.valueOf(user.getId()));
		tester.executeBehavior(table.getBehaviors(AbstractDefaultAjaxBehavior.class).stream()
			.filter(behavior -> behavior.getClass().getSimpleName().equals("ActionBehavior"))
			.findFirst().orElseThrow());

		assertEquals(user, page.getSelected());
		assertTrue(table.isOverlayShown(), "the form shows in the overlay of the grid");
		assertTrue(tester.getLastResponseAsString().contains("Edit " + user.getFirstName() + " " +
			user.getLastName()));
		// its fields count, closing the window, unlike the others of the grid
		tester.assertContains("data-modified=\"auto\"");
	}

	@Test
	void rendersTheDesktopAgainWithTheTaskbarElsewhereInTheStatesItKeeps()
	{
		DesktopWindow wicket = windows().get(0);
		send(new JSONObject().put(wicket.getMarkupId(), new WindowState(30, 40, 500, 300, false,
			false, true, false, 2).toJson()));

		FormTester form = tester.newFormTester("themed:form");
		form.select("taskbar", 2);
		tester.executeAjaxEvent("themed:form:taskbar", "change");

		assertEquals(TaskbarPosition.LEFT, page.getDesktop().getTaskbarPosition());
		tester.assertComponentOnAjaxResponse("themed:desktop");
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("\"taskbar\":\"left\""));
		TagTester window = TagTester.createTagByAttribute(response, "id", wicket.getMarkupId());
		assertEquals("30", window.getAttribute("data-x"));
		assertEquals("", window.getAttribute("data-folded"));
		assertEquals("2", window.getAttribute("data-order"));
	}

	@Test
	void opensTheKeyboardWindowAgain()
	{
		tester.executeAjaxEvent("themed:form:openKeyboard", "click");

		tester.assertContains("Wicket.Desktop.of\\(\"" + page.getDesktop().getDesktopMarkupId() +
			"\"\\).activate\\(\"" + page.getKeyboardWindow().getMarkupId() + "\", true\\);");
	}

	@Test
	void givesTheAppsTheTextsOfTheirDesktop()
	{
		TagTester react = TagTester.createTagsByAttribute(tester.getLastResponseAsString(),
			"data-demo", "desktop", false).get(0);
		JSONObject config = new JSONObject(react.getAttribute("data-config"));

		assertEquals("Notes", config.getJSONObject("texts").getString("notes"));
		assertEquals("On the left", config.getJSONObject("texts").getString("taskbar.left"));
		assertEquals("Tile the windows", config.getJSONObject("desktop").getString("grid"));
		assertTrue(config.getJSONObject("jsonTable").getBoolean("compact"));
		assertTrue(config.getJSONObject("jsonTable").getJSONArray("localUsers").length() > 0);
		assertEquals("Dropdowns in React", config.getJSONObject("texts").getString("reactDropdowns"));
		assertEquals("Keep it open", config.getJSONObject("desktop").getString("keep"));
		assertEquals("The menu of the taskbar", config.getJSONObject("texts").getString("key.menu"));
		assertEquals("One choice", config.getJSONObject("dropdown").getJSONObject("texts")
			.getString("single"));
		// six more demos, configured as their pages configure them
		JSONObject demos = config.getJSONObject("demos");
		assertEquals(List.of("autocomplete", "buttons", "datePicker", "jsonList", "modalDialog",
			"progressBar"), demos.keySet().stream().sorted().toList());
		assertEquals("Autocomplete", demos.getJSONObject("autocomplete").getString("title"));
		assertEquals("Date and time pickers", demos.getJSONObject("datePicker")
			.getString("title"));
		assertTrue(demos.getJSONObject("autocomplete").getJSONObject("config").getString(
			"usersUrl").contains("api/users"));
		assertTrue(demos.getJSONObject("modalDialog").getJSONObject("config").has("roles"));
		assertTrue(demos.getJSONObject("progressBar").getJSONObject("config").getJSONObject(
			"texts").has("start"));
	}

	@Test
	void speaksSpanish()
	{
		tester.getSession().setLocale(new Locale("es"));
		page = tester.startPage(DesktopPage.class);

		tester.assertContains("Tabla JSON en Wicket");
		tester.assertContains("Desplegables en Wicket");
		tester.assertContains("\"menu\":\"Todas las ventanas\"");
		tester.assertContains("\"minimize\":\"Minimizar\"");
		TagTester react = grid(tester.getLastResponseAsString(), "react");
		JSONObject texts = new JSONObject(react.getAttribute("data-config"))
			.getJSONObject("texts");
		assertEquals("Nombre", texts.getString("firstName"));
		assertEquals("Usuario nuevo", texts.getString("newUser"));
	}
}
