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
package com.antiliacode.widgets.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.icon.SvgIcon;
import com.github.openjson.JSONObject;

class DesktopTest extends WicketTestCase
{
	@Test
	void rendersTheWindowsInTheAreaAndAnEmptyTaskbar()
	{
		TestDesktop desktop = new TestDesktop();
		TestWindow first = desktop.window("One", "first");
		first.setPosition(10, 20).setSize(300, 200).setIcon(SvgIcon.ARROW_DOWN_UP_ACROSS_LINE);
		TestWindow second = desktop.window("Two", "second");
		tester.startPage(new TestPage(desktop));

		TagTester element = tester.getTagByWicketId("desktop");
		assertEquals("antilia-desktop antilia-desktop-taskbar-bottom", element.getAttribute("class"));
		TagTester taskbar = tester.getTagByWicketId("taskbar");
		assertEquals("toolbar", taskbar.getAttribute("role"));
		assertEquals("Windows", taskbar.getAttribute("aria-label"));
		assertEquals("horizontal", taskbar.getAttribute("aria-orientation"));

		List<TagTester> windows = TagTester.createTagsByAttribute(tester.getLastResponseAsString(),
			"class", "antilia-window", false);
		assertEquals(2, windows.size());
		TagTester one = windows.get(0);
		assertEquals("section", one.getName());
		assertEquals(first.getMarkupId(), one.getAttribute("id"));
		assertEquals("region", one.getAttribute("role"));
		assertEquals("-1", one.getAttribute("tabindex"));
		assertEquals(first.get("title").getMarkupId(), one.getAttribute("aria-labelledby"));
		assertEquals("10", one.getAttribute("data-x"));
		assertEquals("20", one.getAttribute("data-y"));
		assertEquals("300", one.getAttribute("data-width"));
		assertEquals("200", one.getAttribute("data-height"));
		assertNull(one.getAttribute("data-order"));
		assertNull(one.getAttribute("data-closable"));
		assertNull(one.getAttribute("data-modified"));
		assertNull(one.getAttribute("hidden"));
		assertTrue(one.getValue().contains("antilia-window-title"));
		assertTrue(one.getValue().contains("<svg"), "the icon");
		assertTrue(one.getValue().contains("first"), "the content");
		assertNull(windows.get(1).getAttribute("data-x"));
		assertFalse(windows.get(1).getValue().contains("antilia-window-icon"), "no icon");
		assertEquals(second.getMarkupId(), windows.get(1).getAttribute("id"));

		tester.assertContains("antilia-desktop.*\\.js");
		tester.assertContains("antilia-desktop.*\\.css");
		// the dialog asking before a window with changes closes
		tester.assertContains("antilia-modal.*\\.css");
		tester.assertContains("antilia-button.*\\.css");
		tester.assertContains("Wicket.Desktop.attach\\(\"" + desktop.getDesktopMarkupId() +
			"\", \\{\"taskbar\":\"bottom\",\"tiling\":true,\"expandable\":true,\"expanded\":false," +
			"\"movableTaskbar\":true,\"menu\":true,\"panes\":\\[\\{\"id\":\"0\",\"name\":\"\"\\}\\]," +
			"\"activePane\":\"0\",\"paneSwitcher\":true,\"editablePanes\":true," +
			"\"windowTypes\":\\[\\],\"texts\":\\{\"minimize\":\"Minimize\"," +
			"\"maximize\":\"Maximize\",\"restore\":\"Restore\",\"close\":\"Close\"," +
			"\"taskbar\":\"Windows\",\"grid\":\"Tile the windows\"");
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
		assertEquals(List.of(first, second), desktop.getWindows());
	}

	@Test
	void rendersWhatTheUserMayDoWithAWindowAndTheStateItStartsIn()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.setTaskbarPosition(TaskbarPosition.LEFT).setTiling(false);
		desktop.window("One", "first").setResizable(false).setMaximizable(false)
			.setMinimizable(false).setFoldable(false).setClosable(true).setMinSize(200, 150)
			.setMinimized(true).setMaximized(true).setFolded(true).setModified(true);
		desktop.window("Two", "second").setMinimized(true).setFolded(true).setModified(false);
		desktop.window("Three", "third").setClosed(true).setMaximized(true);
		tester.startPage(new TestPage(desktop));

		assertEquals("antilia-desktop antilia-desktop-taskbar-left",
			tester.getTagByWicketId("desktop").getAttribute("class"));
		assertEquals("vertical", tester.getTagByWicketId("taskbar").getAttribute("aria-orientation"));
		List<TagTester> windows = TagTester.createTagsByAttribute(tester.getLastResponseAsString(),
			"class", "antilia-window", false);
		TagTester one = windows.get(0);
		assertEquals("false", one.getAttribute("data-resizable"));
		assertEquals("false", one.getAttribute("data-maximizable"));
		assertEquals("false", one.getAttribute("data-minimizable"));
		assertEquals("false", one.getAttribute("data-foldable"));
		assertEquals("true", one.getAttribute("data-closable"));
		assertEquals("true", one.getAttribute("data-modified"));
		assertEquals("200", one.getAttribute("data-min-width"));
		assertEquals("150", one.getAttribute("data-min-height"));
		// what it may not be, it is not
		assertNull(one.getAttribute("data-minimized"));
		assertNull(one.getAttribute("data-maximized"));
		assertNull(one.getAttribute("data-folded"));
		assertNull(one.getAttribute("hidden"));

		TagTester two = windows.get(1);
		assertEquals("", two.getAttribute("data-minimized"));
		assertEquals("", two.getAttribute("data-folded"));
		assertEquals("hidden", two.getAttribute("hidden"));
		assertEquals("false", two.getAttribute("data-modified"));
		TagTester three = windows.get(2);
		assertEquals("", three.getAttribute("data-closed"));
		assertEquals("", three.getAttribute("data-maximized"));
		assertEquals("hidden", three.getAttribute("hidden"));
		tester.assertContains("\\{\"taskbar\":\"left\",\"tiling\":false,");
	}

	@Test
	void fillsThePageAndMovesTheTaskbarAsTold()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.setExpandable(false).setExpanded(true).setMovableTaskbar(false).setMenu(false);
		desktop.window("One", "first");
		tester.startPage(new TestPage(desktop));

		assertFalse(desktop.isExpandable());
		assertTrue(desktop.isExpanded());
		assertFalse(desktop.isMovableTaskbar());
		assertFalse(desktop.isMenu());
		tester.assertContains("\"expandable\":false,\"expanded\":true,\"movableTaskbar\":false," +
			"\"menu\":false,");
		tester.assertContains("\"expand\":\"Fill the page\",\"collapse\":\"Back into the page\"");
		assertFalse(tester.getLastResponseAsString().contains("data-minimizable=\"true\""));
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		TestDesktop desktop = new TestDesktop();
		desktop.window("Eins", "erste");
		tester.startPage(new TestPage(desktop));

		assertEquals("Fenster", tester.getTagByWicketId("taskbar").getAttribute("aria-label"));
		tester.assertContains("\"minimize\":\"Minimieren\"");
		tester.assertContains("\"grid\":\"Fenster kacheln\"");
		tester.assertContains("\"menu\":\"Alle Fenster\"");
		tester.assertContains("\"closeModified\":\"Die Änderungen in \\{0\\} sind nicht gespeichert.");
	}

	@Test
	void sendsNothingToTheServerByDefault()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.window("One", "first");
		tester.startPage(new TestPage(desktop));

		assertFalse(tester.getLastResponseAsString().contains("Wicket.Ajax"), "no callback");
		assertFalse(desktop.get("desktop").getBehaviors(AbstractDefaultAjaxBehavior.class).get(0)
			.isEnabled(desktop.get("desktop")));
	}

	@Test
	void givesTheWindowsTheStatesTheBrowserSends()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.setSendToServer(true);
		TestWindow first = desktop.window("One", "first");
		first.setPosition(10, 20);
		TestWindow second = desktop.window("Two", "second");
		tester.startPage(new TestPage(desktop));
		tester.assertContains("Wicket.Desktop.attach\\(\"" + desktop.getDesktopMarkupId() +
			"\", \\{.*\\}, function \\(states,taskbar,expanded,panes,pane\\)");

		JSONObject states = new JSONObject()
			.put(first.getMarkupId(), new JSONObject().put("x", 40).put("y", 50.4)
				.put("width", 320).put("height", 240).put("minimized", false)
				.put("maximized", true).put("folded", false).put("closed", false).put("order", 2))
			.put(second.getMarkupId(), second.getState().toJson())
			.put("unknown", new JSONObject().put("x", 1));
		send(desktop, states.toString());

		assertEquals(1, desktop.changes);
		assertEquals(new WindowState(40, 50, 320, 240, false, true, false, false, 2),
			first.getModelObject());
		assertNull(second.getModelObject(), "unchanged");

		// the same again: nothing changed
		send(desktop, states.toString());
		assertEquals(1, desktop.changes);
		send(desktop, "not json");
		send(desktop, "");
		assertEquals(1, desktop.changes);

		// rendered again, the window shows the state the user left it in
		tester.startComponentInPage(first);
		TagTester window = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			first.getMarkupId());
		assertEquals("40", window.getAttribute("data-x"));
		assertEquals("2", window.getAttribute("data-order"));
		assertEquals("", window.getAttribute("data-maximized"));
	}

	@Test
	void keepsTheSideOfTheTaskbarAndWhetherItFillsThePageAsTheBrowserSends()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.setSendToServer(true);
		desktop.window("One", "first");
		tester.startPage(new TestPage(desktop));

		send(desktop, "{}", "top", "true");
		assertEquals(1, desktop.changes);
		assertEquals(TaskbarPosition.TOP, desktop.getTaskbarPosition());
		assertTrue(desktop.isExpanded());

		send(desktop, "{}", "top", "true");
		send(desktop, "{}", "nowhere", "");
		assertEquals(1, desktop.changes, "nothing changed");
		assertEquals(TaskbarPosition.TOP, desktop.getTaskbarPosition());
		assertTrue(desktop.isExpanded());

		send(desktop, "{}", "bottom", "false");
		assertEquals(2, desktop.changes);
		assertEquals(TaskbarPosition.BOTTOM, desktop.getTaskbarPosition());
		assertFalse(desktop.isExpanded());
		assertEquals(TaskbarPosition.RIGHT, TaskbarPosition.of("right"));
		assertNull(TaskbarPosition.of(null));
	}

	@Test
	void keepsTheStateInAModelOfItsOwn()
	{
		IModel<WindowState> stored = Model.of(new WindowState(5, 6, 400, 300, false, false, true,
			false, 1));
		TestDesktop desktop = new TestDesktop();
		TestWindow window = new TestWindow(desktop.newWindowId(), "One", "first", stored);
		desktop.addWindow(window);
		window.setPosition(99, 99);
		tester.startPage(new TestPage(desktop));

		assertEquals(stored.getObject(), window.getState());
		TagTester tag = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			window.getMarkupId());
		assertEquals("5", tag.getAttribute("data-x"));
		assertEquals("", tag.getAttribute("data-folded"));

		window.setModelObject(null);
		assertEquals(new WindowState(99, 99, null, null, false, false, false, false, 0),
			window.getState());
	}

	@Test
	void windowStatesComeAndGoAsJson()
	{
		WindowState state = new WindowState(1, 2, 3, 4, true, false, true, false, -5);
		assertEquals(0, state.getOrder());
		assertEquals(state, WindowState.fromJson(state.toJson()));
		assertEquals(state.hashCode(), WindowState.fromJson(state.toJson()).hashCode());
		assertEquals(new WindowState(), WindowState.fromJson(new JSONObject()));
		assertEquals("{\"minimized\":false,\"maximized\":false,\"folded\":false,\"closed\":false," +
			"\"order\":0}", new WindowState().toString());
		WindowState changed = state.withClosed(true).withOrder(3).withMaximized(true)
			.withFolded(false).withMinimized(false);
		assertTrue(changed.isClosed());
		assertTrue(changed.isMaximized());
		assertFalse(changed.isFolded());
		assertFalse(changed.isMinimized());
		assertEquals(3, changed.getOrder());
		assertEquals(Integer.valueOf(1), changed.getX());
		assertEquals(Integer.valueOf(2), changed.getY());
		assertEquals(Integer.valueOf(3), changed.getWidth());
		assertEquals(Integer.valueOf(4), changed.getHeight());
		assertFalse(state.equals(changed));
	}

	@Test
	void rendersThePanesAndThePaneOfEachWindow()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.getDefaultPane().setName("Grids");
		DesktopPane dropdowns = desktop.addPane("Dropdowns");
		assertEquals("1", dropdowns.getId());
		TestWindow first = desktop.window("One", "first");
		TestWindow second = desktop.window("Two", "second");
		second.setPane(dropdowns.getId());
		desktop.setActivePane("1").setEditablePanes(false);
		tester.startPage(new TestPage(desktop));

		tester.assertContains("\"panes\":\\[\\{\"id\":\"0\",\"name\":\"Grids\"\\}," +
			"\\{\"id\":\"1\",\"name\":\"Dropdowns\"\\}\\],\"activePane\":\"1\"," +
			"\"paneSwitcher\":true,\"editablePanes\":false");
		tester.assertContains("\"moveToPane\":\"Move to another pane\"");
		assertNull(TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			first.getMarkupId()).getAttribute("data-pane"), "in the pane shown");
		assertEquals("1", TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			second.getMarkupId()).getAttribute("data-pane"));

		desktop.setActivePane("nowhere");
		assertEquals(DesktopPane.DEFAULT, desktop.getActivePane());
	}

	@Test
	void removesAPaneMovingItsWindowsToTheDefaultOne()
	{
		TestDesktop desktop = new TestDesktop();
		DesktopPane other = desktop.addPane("Other");
		TestWindow first = desktop.window("One", "first");
		first.setModelObject(new WindowState().withPosition(1, 2));
		first.setPane(other.getId());
		assertEquals("1", first.getModelObject().getPane());
		desktop.setActivePane(other.getId());

		assertFalse(desktop.removePane(DesktopPane.DEFAULT));
		assertTrue(desktop.removePane(other.getId()));

		assertEquals(List.of(desktop.getDefaultPane()), desktop.getPanes());
		assertEquals(DesktopPane.DEFAULT, first.getPane());
		assertEquals(DesktopPane.DEFAULT, desktop.getActivePane());
		assertFalse(desktop.removePane(other.getId()));
	}

	@Test
	void keepsThePanesTheBrowserSends()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.setSendToServer(true);
		TestWindow first = desktop.window("One", "first");
		tester.startPage(new TestPage(desktop));

		send(desktop, new JSONObject().put(first.getMarkupId(), new WindowState()
			.withPosition(4, 5).withPane("2").toJson()).toString(), null, null,
			"[{\"id\":\"0\",\"name\":\"Main\"},{\"id\":\"2\",\"name\":\"Charts\"}]", "2");

		assertEquals(1, desktop.changes);
		assertEquals(List.of(new DesktopPane("0", "Main"), new DesktopPane("2", "Charts")),
			desktop.getPanes());
		assertEquals("2", desktop.getActivePane());
		assertEquals("2", first.getPane());

		// without the default pane, or not JSON: kept as they were
		send(desktop, "{}", null, null, "[{\"id\":\"2\",\"name\":\"Charts\"}]", "9");
		send(desktop, "{}", null, null, "[", "");
		assertEquals(1, desktop.changes);
		assertEquals(2, desktop.getPanes().size());

		send(desktop, "{}", null, null, "[{\"id\":\"0\",\"name\":\"\"}]", "0");
		assertEquals(2, desktop.changes);
		assertEquals(List.of(new DesktopPane("0", null)), desktop.getPanes());
		assertEquals("0", desktop.getActivePane());
	}

	@Test
	void opensWindowsOfATypeAndTakesThemAwayClosed()
	{
		TestDesktop desktop = new TestDesktop();
		desktop.addPane("Other");
		desktop.window("One", "first");
		desktop.addWindowType(new WindowType("notes", Model.of("Notes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public DesktopWindow newWindow(String id)
			{
				return new TestWindow(id, "Notes", "notes", null);
			}
		}.setIcon(SvgIcon.KEYBOARD));
		tester.startPage(new TestPage(desktop));
		tester.assertContains("\"windowTypes\":\\[\\{\"type\":\"notes\",\"title\":\"Notes\"," +
			"\"icon\":\"\\\\u003csvg");
		// no states sent, but the windows of types
		tester.assertContains("\\}, null, function \\(action,type,pane,window\\)");

		AbstractDefaultAjaxBehavior behavior = desktop.get("desktop")
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(1);
		tester.getRequest().setParameter("action", "open");
		tester.getRequest().setParameter("type", "notes");
		tester.getRequest().setParameter("pane", "1");
		tester.executeBehavior(behavior);

		assertEquals(2, desktop.getWindows().size());
		DesktopWindow opened = desktop.getWindows().get(1);
		assertEquals(List.of(opened), desktop.opened);
		assertTrue(opened.isDisposable());
		assertTrue(opened.isClosable());
		assertEquals("1", opened.getPane());
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("desktop.area.appendChild(window)"), response);
		assertTrue(response.contains("data-disposable"), response);
		assertTrue(response.contains("desktop.activate(\"" + opened.getMarkupId() + "\")"), response);

		// a type there is not: nothing
		tester.getRequest().setParameter("action", "open");
		tester.getRequest().setParameter("type", "none");
		tester.executeBehavior(behavior);
		assertEquals(2, desktop.getWindows().size());

		tester.getRequest().setParameter("action", "dispose");
		tester.getRequest().setParameter("window", desktop.getWindows().get(0).getMarkupId());
		tester.executeBehavior(behavior);
		assertEquals(2, desktop.getWindows().size(), "not of a type: kept");

		tester.getRequest().setParameter("action", "dispose");
		tester.getRequest().setParameter("window", opened.getMarkupId());
		tester.executeBehavior(behavior);
		assertEquals(1, desktop.getWindows().size());
		assertEquals(List.of(opened), desktop.disposed);
		assertEquals("notes", desktop.getWindowTypes().get(0).getType());
	}

	@Test
	void panesComeAndGoAsJson()
	{
		DesktopPane pane = new DesktopPane("3", "Charts");
		assertEquals(pane, DesktopPane.fromJson(pane.toJson()));
		assertEquals(pane.hashCode(), DesktopPane.fromJson(pane.toJson()).hashCode());
		assertNull(DesktopPane.fromJson(new JSONObject().put("name", "x")));
		assertEquals(new DesktopPane("0", null), DesktopPane.fromJson(new DesktopPane("0", null)
			.toJson()));
		assertTrue(new DesktopPane("0", null).isDefault());
		assertEquals("{\"id\":\"3\",\"name\":\"Charts\"}", pane.toString());
		WindowState state = new WindowState().withPane("3");
		assertEquals(state, WindowState.fromJson(state.toJson()));
		assertNull(new WindowState().withPane("").getPane());
	}

	private void send(TestDesktop desktop, String states)
	{
		send(desktop, states, null, null);
	}

	private void send(TestDesktop desktop, String states, String taskbar, String expanded)
	{
		send(desktop, states, taskbar, expanded, null, null);
	}

	private void send(TestDesktop desktop, String states, String taskbar, String expanded,
		String panes, String pane)
	{
		AbstractDefaultAjaxBehavior behavior = desktop.get("desktop")
			.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0);
		tester.getRequest().setParameter("states", states);
		if (taskbar != null)
		{
			tester.getRequest().setParameter("taskbar", taskbar);
			tester.getRequest().setParameter("expanded", expanded);
		}
		if (panes != null)
		{
			tester.getRequest().setParameter("panes", panes);
			tester.getRequest().setParameter("pane", pane);
		}
		tester.executeBehavior(behavior);
	}

	static class TestDesktop extends Desktop
	{
		private static final long serialVersionUID = 1L;

		int changes;

		final List<DesktopWindow> opened = new ArrayList<>();

		final List<DesktopWindow> disposed = new ArrayList<>();

		TestDesktop()
		{
			super("panel");
		}

		TestWindow window(String title, String content)
		{
			TestWindow window = new TestWindow(newWindowId(), title, content, null);
			addWindow(window);
			return window;
		}

		@Override
		protected void onChanged(AjaxRequestTarget target)
		{
			changes++;
		}

		@Override
		protected void onWindowOpened(AjaxRequestTarget target, DesktopWindow window)
		{
			opened.add(window);
		}

		@Override
		protected void onWindowDisposed(AjaxRequestTarget target, DesktopWindow window)
		{
			disposed.add(window);
		}
	}

	static class TestWindow extends DesktopWindow
	{
		private static final long serialVersionUID = 1L;

		private final String content;

		TestWindow(String id, String title, String content, IModel<WindowState> model)
		{
			super(id, Model.of(title), model);
			this.content = content;
		}

		@Override
		protected Component newContent(String id)
		{
			return new Label(id, content);
		}
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		TestPage(Desktop desktop)
		{
			add(desktop);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<div wicket:id=\"panel\"></div></body></html>");
		}
	}
}
