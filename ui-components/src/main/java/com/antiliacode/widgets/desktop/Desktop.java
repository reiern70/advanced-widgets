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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.modal.Modal;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A desktop: {@link DesktopWindow floating windows} that are not modal, many at once, as on the
 * desktop of an operating system, and a taskbar. A click in a window brings it to the front, the
 * others staying behind as they were. The user drags a window by its title bar, with the mouse, a
 * pen or a finger, folds it to its title bar with a double click on it and, as the window allows,
 * resizes it by its edges and corners, minimizes it to the taskbar, maximizes it to the whole
 * desktop and closes it. While the focus is in a window, F11 maximizes it and restores it, and
 * Ctrl+F6 and Ctrl+Shift+F6 go round the windows (Ctrl+Tab and Ctrl+Shift+Tab too, where the
 * browser lets the page have them).
 * <p>
 * The taskbar, {@link #setTaskbarPosition(TaskbarPosition) below the windows or on another side},
 * has a button for each window that is not closed, with its icon and title: it brings the window to
 * the front, restoring it when minimized, and minimizes the one already in front. While the window
 * in front is maximized, the one brought to the front is maximized too, taking its place. The
 * buttons at the end of the taskbar tile the windows in a grid, side by side or one above the
 * other, or cascade them; {@link #setTiling(boolean)} leaves them out. The last one lets the
 * desktop {@link #setExpanded(boolean) fill the whole page}, and takes it back into the page;
 * {@link #setExpandable(boolean)} leaves it out. The user moves the taskbar to another side by
 * dragging it, or with the arrow keys on its grip, its first button, unless
 * {@link #setMovableTaskbar(boolean)} turns that off; the windows move and shrink to stay on the
 * desktop. The menu of the taskbar, the button after the grip, lists the windows open, to bring
 * one to the front, and those closed, to open one again; {@link #setMenu(boolean)} leaves it out.
 * <p>
 * The windows are in {@link DesktopPane panes}, as in the workspaces of the desktop of an
 * operating system: the desktop shows the windows of one pane at a time, the
 * {@link #setActivePane(String) pane shown}. The {@link DesktopPane#DEFAULT default pane} is
 * always there; {@link #addPane(String)} adds others. The taskbar shows the panes, unless
 * {@link #setPaneSwitcher(boolean)} turns that off, and lets the user add one, asking for its name,
 * rename one and remove one, its windows going to the default pane, unless
 * {@link #setEditablePanes(boolean)} turns that off. Ctrl+Shift and the arrow keys go from pane to
 * pane, and a button on the title bar of each window moves it to another pane.
 * {@link DesktopWindow#setPane(String)} says which pane a window starts in.
 * <p>
 * The launcher of the taskbar opens windows of the {@link WindowType types}
 * {@link #addWindowType(WindowType) registered}: the desktop asks the type for a new window, adds it
 * to the pane shown and renders it by Ajax, and takes it away again when the user closes it.
 * <p>
 * Closing a window with changes not saved asks first, in a dialog of the look of
 * {@link com.antiliacode.widgets.modal.Modal}: a window has changes when a field in it holds another
 * value than the one it was rendered with, or as {@link DesktopWindow#setModified(Boolean)} says.
 * Closing it then puts its fields back as they were rendered, telling their Ajax behaviors.
 * <p>
 * {@code Wicket.Desktop} from {@link #JS} does all that in the browser; it sets where the windows
 * are and their sizes as CSS custom properties through the CSSOM, without an inline style, so the
 * desktop works under a strict Content Security Policy. The look comes from {@link #CSS}, in the
 * colors of the {@link com.antiliacode.widgets.theme.Theme theme} the desktop is in; give the desktop
 * a height in the CSS of the page, 30rem by default.
 * <p>
 * The windows are {@link DesktopWindow}s, created with the ids {@link #newWindowId()} gives and
 * added with {@link #addWindow(DesktopWindow)}. A window rendered again by Ajax keeps its state in
 * the browser. With {@link #setSendToServer(boolean)} the browser sends the server, via Ajax, the
 * states of all windows each time the user has changed any, with the side of the taskbar and
 * whether the desktop fills the page, and the panes; the windows give their models the new states,
 * the desktop keeps the side, whether it fills the page and the panes, and calls
 * {@link #onChanged(AjaxRequestTarget)}.
 *
 * @since 11.0.0
 */
public class Desktop extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The script of the desktop, {@code Wicket.Desktop}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Desktop.class,
		"antilia-desktop.js");

	/** The style sheet of the desktop. */
	public static final ResourceReference CSS = new CssResourceReference(Desktop.class,
		"antilia-desktop.css");

	private static final String STATES_PARAMETER = "states";

	private static final String TASKBAR_PARAMETER = "taskbar";

	private static final String EXPANDED_PARAMETER = "expanded";

	private static final String PANES_PARAMETER = "panes";

	private static final String PANE_PARAMETER = "pane";

	private static final String ACTION_PARAMETER = "action";

	private static final String TYPE_PARAMETER = "type";

	private static final String WINDOW_PARAMETER = "window";

	/** The texts of the desktop, by their names in the script; their keys are Desktop.name. */
	private static final String[] TEXTS = { "minimize", "maximize", "restore", "close", "taskbar",
		"grid", "columns", "rows", "cascade", "expand", "collapse", "move", "menu", "openWindows",
		"closedWindows", "closeModifiedTitle", "closeModified", "discard", "keep", "panes",
		"defaultPane", "newPane", "newPaneName", "renamePane", "renamePaneTitle", "removePane",
		"paneName", "moveToPane", "moveToNewPane", "newWindow", "ok", "cancel" };

	private final WebMarkupContainer desktop;

	private final RepeatingView windows;

	private final AbstractDefaultAjaxBehavior changed;

	/** Adds the windows of types the user opens, and takes them away when closed. */
	private final AbstractDefaultAjaxBehavior windowsBehavior;

	private final List<DesktopPane> panes = new ArrayList<>();

	private final Map<String, WindowType> windowTypes = new LinkedHashMap<>();

	private String activePane = DesktopPane.DEFAULT;

	private boolean paneSwitcher = true;

	private boolean editablePanes = true;

	private TaskbarPosition taskbarPosition = TaskbarPosition.BOTTOM;

	private boolean tiling = true;

	private boolean expandable = true;

	private boolean expanded;

	private boolean movableTaskbar = true;

	private boolean menu = true;

	private boolean sendToServer;

	/**
	 * @param id
	 *            component id
	 */
	public Desktop(String id)
	{
		super(id);

		desktop = new WebMarkupContainer("desktop");
		desktop.setOutputMarkupId(true);
		desktop.add(new LookBehavior(LookPart.DESKTOP));
		desktop.add(new LookContainer("area", LookPart.DESKTOP_AREA));
		desktop.add(AttributeModifier.append("class",
			() -> "antilia-desktop-taskbar-" + taskbarPosition.cssName()));
		add(desktop);

		windows = new RepeatingView("windows");
		desktop.add(windows);

		WebMarkupContainer taskbar = new WebMarkupContainer("taskbar");
		taskbar.add(AttributeModifier.replace("aria-label", () -> getString("Desktop.taskbar")));
		taskbar.add(AttributeModifier.replace("aria-orientation",
			() -> taskbarPosition == TaskbarPosition.LEFT ||
				taskbarPosition == TaskbarPosition.RIGHT ? "vertical" : "horizontal"));
		taskbar.add(new LookBehavior(LookPart.DESKTOP_TASKBAR));
		desktop.add(taskbar);

		changed = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(Component component)
			{
				return sendToServer && super.isEnabled(component);
			}

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				IRequestParameters parameters = getRequest().getRequestParameters();
				onStates(target,
					parameters.getParameterValue(STATES_PARAMETER).toOptionalString(),
					parameters.getParameterValue(TASKBAR_PARAMETER).toOptionalString(),
					parameters.getParameterValue(EXPANDED_PARAMETER).toOptionalString(),
					parameters.getParameterValue(PANES_PARAMETER).toOptionalString(),
					parameters.getParameterValue(PANE_PARAMETER).toOptionalString());
			}
		};
		// on the element of the desktop, which has an id even when the panel's tag is not rendered
		desktop.add(changed);

		windowsBehavior = new AbstractDefaultAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(Component component)
			{
				return !windowTypes.isEmpty() && super.isEnabled(component);
			}

			@Override
			protected void respond(AjaxRequestTarget target)
			{
				IRequestParameters parameters = getRequest().getRequestParameters();
				String action = parameters.getParameterValue(ACTION_PARAMETER).toOptionalString();
				if ("open".equals(action))
				{
					openWindow(target,
						parameters.getParameterValue(TYPE_PARAMETER).toOptionalString(),
						parameters.getParameterValue(PANE_PARAMETER).toOptionalString());
				}
				else if ("dispose".equals(action))
				{
					disposeWindow(target,
						parameters.getParameterValue(WINDOW_PARAMETER).toOptionalString());
				}
			}
		};
		desktop.add(windowsBehavior);

		panes.add(new DesktopPane(DesktopPane.DEFAULT, null));
	}

	/**
	 * Adds a window of a type the user opened from the launcher, to a pane, and renders it.
	 */
	private void openWindow(AjaxRequestTarget target, String type, String pane)
	{
		WindowType windowType = type == null ? null : windowTypes.get(type);
		if (windowType == null)
		{
			return;
		}
		DesktopWindow window = windowType.newWindow(newWindowId());
		window.setPane(getPane(pane) != null ? pane : activePane).setDisposable(true)
			.setClosable(true);
		addWindow(window);
		String desktopId = JSONObject.quote(desktop.getMarkupId());
		String windowId = JSONObject.quote(window.getMarkupId());
		// an element for Wicket to replace by the window, in the area of the windows
		target.prependJavaScript("(function(){var desktop=Wicket.Desktop.of(" + desktopId +
			");if(desktop){var window=document.createElement('section');window.id=" + windowId +
			";desktop.area.appendChild(window);}})();");
		target.add(window);
		target.appendJavaScript("(function(){var desktop=Wicket.Desktop.of(" + desktopId +
			");if(desktop){desktop.refresh();desktop.activate(" + windowId + ");}})();");
		onWindowOpened(target, window);
	}

	/**
	 * Takes away a window of a type the user closed.
	 */
	private void disposeWindow(AjaxRequestTarget target, String markupId)
	{
		for (DesktopWindow window : getWindows())
		{
			if (window.isDisposable() && window.getMarkupId().equals(markupId))
			{
				windows.remove(window);
				target.appendJavaScript("(function(){var window=document.getElementById(" +
					JSONObject.quote(markupId) + ");if(window){window.remove();}})();");
				onWindowDisposed(target, window);
				return;
			}
		}
	}

	/**
	 * Called when the user opened a window of a {@link WindowType type}, which the desktop added
	 * and renders. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param window
	 *            the window
	 */
	protected void onWindowOpened(AjaxRequestTarget target, DesktopWindow window)
	{
	}

	/**
	 * Called when the user closed a window of a {@link WindowType type}, which the desktop took
	 * away. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param window
	 *            the window, no longer in the desktop
	 */
	protected void onWindowDisposed(AjaxRequestTarget target, DesktopWindow window)
	{
	}

	/**
	 * Registers a type of window, which the launcher of the taskbar opens; one of the same name
	 * goes.
	 *
	 * @param type
	 *            the type
	 * @return {@code this}
	 */
	public Desktop addWindowType(WindowType type)
	{
		windowTypes.put(type.getType(), type);
		return this;
	}

	/**
	 * @return the types of windows the launcher opens, in the order they were registered
	 */
	public List<WindowType> getWindowTypes()
	{
		return Collections.unmodifiableList(new ArrayList<>(windowTypes.values()));
	}

	/**
	 * @return the panes, the default one first; with {@link #isSendToServer()} on, as the user
	 *         left them
	 */
	public List<DesktopPane> getPanes()
	{
		return Collections.unmodifiableList(panes);
	}

	/**
	 * @param id
	 *            the id of a pane
	 * @return the pane, {@code null} when there is no such pane
	 */
	public DesktopPane getPane(String id)
	{
		return panes.stream().filter(pane -> pane.getId().equals(id)).findFirst().orElse(null);
	}

	/**
	 * @return the default pane, which is always there
	 */
	public DesktopPane getDefaultPane()
	{
		return panes.get(0);
	}

	/**
	 * Adds a pane, after the others.
	 *
	 * @param name
	 *            the name of the pane
	 * @return the pane
	 */
	public DesktopPane addPane(String name)
	{
		int last = 0;
		for (DesktopPane pane : panes)
		{
			try
			{
				last = Math.max(last, Integer.parseInt(pane.getId()));
			}
			catch (NumberFormatException e)
			{
				// an id given by the page, not a number
			}
		}
		DesktopPane pane = new DesktopPane(String.valueOf(last + 1), name);
		panes.add(pane);
		return pane;
	}

	/**
	 * Removes a pane, its windows going to the default pane, which is shown when the pane was.
	 * The default pane cannot be removed.
	 *
	 * @param id
	 *            the id of the pane
	 * @return whether the pane was removed
	 */
	public boolean removePane(String id)
	{
		DesktopPane pane = getPane(id);
		if (pane == null || pane.isDefault())
		{
			return false;
		}
		panes.remove(pane);
		for (DesktopWindow window : getWindows())
		{
			if (id.equals(window.getPane()))
			{
				window.setPane(DesktopPane.DEFAULT);
			}
		}
		if (id.equals(activePane))
		{
			activePane = DesktopPane.DEFAULT;
		}
		return true;
	}

	/**
	 * @return the id of the pane shown; with {@link #isSendToServer()} on, as the user left it
	 */
	public String getActivePane()
	{
		return activePane;
	}

	/**
	 * @param activePane
	 *            the id of the pane shown, the next time the desktop is rendered; the default
	 *            pane when there is no such pane
	 * @return {@code this}
	 */
	public Desktop setActivePane(String activePane)
	{
		this.activePane = getPane(activePane) != null ? activePane : DesktopPane.DEFAULT;
		return this;
	}

	/**
	 * @return whether the taskbar shows the panes, {@code true} by default
	 */
	public boolean isPaneSwitcher()
	{
		return paneSwitcher;
	}

	/**
	 * @param paneSwitcher
	 *            whether the taskbar shows the panes, and the title bars the buttons moving the
	 *            windows to another pane
	 * @return {@code this}
	 */
	public Desktop setPaneSwitcher(boolean paneSwitcher)
	{
		this.paneSwitcher = paneSwitcher;
		return this;
	}

	/**
	 * @return whether the user may add, rename and remove panes, {@code true} by default
	 */
	public boolean isEditablePanes()
	{
		return editablePanes;
	}

	/**
	 * @param editablePanes
	 *            whether the user may add, rename and remove panes
	 * @return {@code this}
	 */
	public Desktop setEditablePanes(boolean editablePanes)
	{
		this.editablePanes = editablePanes;
		return this;
	}

	/**
	 * @return a new id for a window of this desktop
	 */
	public String newWindowId()
	{
		return windows.newChildId();
	}

	/**
	 * Adds a window to the desktop; the last one added starts in front.
	 *
	 * @param window
	 *            the window, created with an id {@link #newWindowId()} gave
	 * @return {@code this}
	 */
	public Desktop addWindow(DesktopWindow window)
	{
		windows.add(window);
		return this;
	}

	/**
	 * @return the windows, in the order they were added
	 */
	public List<DesktopWindow> getWindows()
	{
		List<DesktopWindow> list = new ArrayList<>();
		windows.forEach(child -> list.add((DesktopWindow)child));
		return list;
	}

	/**
	 * Takes what the browser sent: gives the windows their new states, keeps the side of the
	 * taskbar, whether the desktop fills the page and the panes, and calls back.
	 */
	private void onStates(AjaxRequestTarget target, String json, String taskbar, String expand,
		String panesJson, String pane)
	{
		boolean any = false;
		TaskbarPosition position = TaskbarPosition.of(taskbar);
		if (position != null && position != taskbarPosition)
		{
			taskbarPosition = position;
			any = true;
		}
		if (!Strings.isEmpty(expand) && "true".equals(expand) != expanded)
		{
			expanded = !expanded;
			any = true;
		}
		List<DesktopPane> sent = parsePanes(panesJson);
		if (!sent.isEmpty() && !sent.equals(panes))
		{
			panes.clear();
			panes.addAll(sent);
			any = true;
		}
		if (!Strings.isEmpty(pane) && getPane(pane) != null && !pane.equals(activePane))
		{
			activePane = pane;
			any = true;
		}
		JSONObject states = parse(json);
		for (DesktopWindow window : getWindows())
		{
			JSONObject state = states.optJSONObject(window.getMarkupId());
			if (state == null)
			{
				continue;
			}
			WindowState after = WindowState.fromJson(state);
			if (!after.equals(window.getState()))
			{
				window.setModelObject(after);
				any = true;
			}
		}
		if (any)
		{
			onChanged(target);
		}
	}

	/**
	 * @return the states the browser sent, by the ids of the windows; none when the JSON is
	 *         missing or wrong
	 */
	private static JSONObject parse(String json)
	{
		if (Strings.isEmpty(json))
		{
			return new JSONObject();
		}
		try
		{
			return new JSONObject(json);
		}
		catch (JSONException e)
		{
			return new JSONObject();
		}
	}

	/**
	 * @return the panes the browser sent, the default one first; none when the JSON is missing,
	 *         wrong, or has no default pane
	 */
	private static List<DesktopPane> parsePanes(String json)
	{
		List<DesktopPane> parsed = new ArrayList<>();
		if (Strings.isEmpty(json))
		{
			return parsed;
		}
		try
		{
			JSONArray array = new JSONArray(json);
			for (int index = 0; index < array.length(); index++)
			{
				JSONObject object = array.optJSONObject(index);
				DesktopPane pane = object == null ? null : DesktopPane.fromJson(object);
				if (pane != null && parsed.stream().noneMatch(other ->
					other.getId().equals(pane.getId())))
				{
					parsed.add(pane);
				}
			}
		}
		catch (JSONException e)
		{
			parsed.clear();
		}
		if (parsed.isEmpty() || !parsed.get(0).isDefault())
		{
			parsed.clear();
		}
		return parsed;
	}

	/**
	 * Called when the user has changed the windows and {@link #isSendToServer()} is on, with
	 * their {@link DesktopWindow#getState() states} as the user left them. Does nothing by
	 * default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @return the side of the desktop the taskbar is on, {@link TaskbarPosition#BOTTOM} by default
	 */
	public TaskbarPosition getTaskbarPosition()
	{
		return taskbarPosition;
	}

	/**
	 * @param taskbarPosition
	 *            the side of the desktop the taskbar is on
	 * @return {@code this}
	 */
	public Desktop setTaskbarPosition(TaskbarPosition taskbarPosition)
	{
		this.taskbarPosition = taskbarPosition == null ? TaskbarPosition.BOTTOM : taskbarPosition;
		return this;
	}

	/**
	 * @return whether the taskbar has the buttons tiling the windows, {@code true} by default
	 */
	public boolean isTiling()
	{
		return tiling;
	}

	/**
	 * @param tiling
	 *            whether the taskbar has the buttons tiling the windows
	 * @return {@code this}
	 */
	public Desktop setTiling(boolean tiling)
	{
		this.tiling = tiling;
		return this;
	}

	/**
	 * @return whether the taskbar has the button letting the desktop fill the whole page,
	 *         {@code true} by default
	 */
	public boolean isExpandable()
	{
		return expandable;
	}

	/**
	 * @param expandable
	 *            whether the taskbar has the button letting the desktop fill the whole page
	 * @return {@code this}
	 */
	public Desktop setExpandable(boolean expandable)
	{
		this.expandable = expandable;
		return this;
	}

	/**
	 * @return whether the desktop fills the whole page, {@code false} by default; with
	 *         {@link #isSendToServer()} on, as the user left it
	 */
	public boolean isExpanded()
	{
		return expanded;
	}

	/**
	 * @param expanded
	 *            whether the desktop fills the whole page, the next time it is rendered
	 * @return {@code this}
	 */
	public Desktop setExpanded(boolean expanded)
	{
		this.expanded = expanded;
		return this;
	}

	/**
	 * @return whether the user may move the taskbar to another side, {@code true} by default
	 */
	public boolean isMovableTaskbar()
	{
		return movableTaskbar;
	}

	/**
	 * @param movableTaskbar
	 *            whether the user may move the taskbar to another side, by dragging it or with the
	 *            arrow keys on its grip
	 * @return {@code this}
	 */
	public Desktop setMovableTaskbar(boolean movableTaskbar)
	{
		this.movableTaskbar = movableTaskbar;
		return this;
	}

	/**
	 * @return whether the taskbar has the menu listing the windows open and closed, {@code true}
	 *         by default
	 */
	public boolean isMenu()
	{
		return menu;
	}

	/**
	 * @param menu
	 *            whether the taskbar has the menu listing the windows open, to bring one to the
	 *            front, and those closed, to open one again
	 * @return {@code this}
	 */
	public Desktop setMenu(boolean menu)
	{
		this.menu = menu;
		return this;
	}

	/**
	 * @return whether the browser sends the server what the user changes, {@code false} by default
	 */
	public boolean isSendToServer()
	{
		return sendToServer;
	}

	/**
	 * @param sendToServer
	 *            whether the browser sends the server via Ajax the states of the windows each time
	 *            the user has changed any, which the windows give their models
	 * @return {@code this}
	 */
	public Desktop setSendToServer(boolean sendToServer)
	{
		this.sendToServer = sendToServer;
		return this;
	}

	/**
	 * @return the markup id of the element of the desktop, which the script works on
	 */
	public String getDesktopMarkupId()
	{
		return desktop.getMarkupId();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		StringBuilder script = new StringBuilder("Wicket.Desktop.attach(")
			.append(JSONObject.quote(desktop.getMarkupId())).append(", ").append(getOptions());
		if (sendToServer || !windowTypes.isEmpty())
		{
			script.append(", ").append(sendToServer ? changed.getCallbackFunction(
				CallbackParameter.explicit(STATES_PARAMETER),
				CallbackParameter.explicit(TASKBAR_PARAMETER),
				CallbackParameter.explicit(EXPANDED_PARAMETER),
				CallbackParameter.explicit(PANES_PARAMETER),
				CallbackParameter.explicit(PANE_PARAMETER)) : "null");
		}
		if (!windowTypes.isEmpty())
		{
			script.append(", ").append(windowsBehavior.getCallbackFunction(
				CallbackParameter.explicit(ACTION_PARAMETER),
				CallbackParameter.explicit(TYPE_PARAMETER),
				CallbackParameter.explicit(PANE_PARAMETER),
				CallbackParameter.explicit(WINDOW_PARAMETER)));
		}
		response.render(OnDomReadyHeaderItem.forScript(script.append(");").toString()));
	}

	/**
	 * @return the options of {@code Wicket.Desktop.attach}, as a JavaScript object
	 */
	private String getOptions()
	{
		JSONObject texts = new JSONObject();
		for (String name : TEXTS)
		{
			String text = getString("Desktop." + name, null, "");
			if (!Strings.isEmpty(text))
			{
				texts.put(name, text);
			}
		}
		JSONArray paneList = new JSONArray();
		panes.forEach(pane -> paneList.put(pane.toJson()));
		JSONArray types = new JSONArray();
		for (WindowType type : windowTypes.values())
		{
			IModel<String> title = type.getTitle();
			if (title instanceof IComponentAssignedModel<String> assigned)
			{
				// a resource model, say, as the desktop finds its texts
				title = assigned.wrapOnAssignment(this);
			}
			JSONObject json = new JSONObject().put("type", type.getType())
				.put("title", Objects.toString(title.getObject(), type.getType()));
			title.detach();
			if (type.getIcon() != null)
			{
				json.put("icon", type.getIcon().getMarkup());
			}
			types.put(json);
		}
		return new JSONObject()
			.put("taskbar", taskbarPosition.cssName())
			.put("tiling", tiling)
			.put("expandable", expandable)
			.put("expanded", expanded)
			.put("movableTaskbar", movableTaskbar)
			.put("menu", menu)
			.put("panes", paneList)
			.put("activePane", activePane)
			.put("paneSwitcher", paneSwitcher)
			.put("editablePanes", editablePanes)
			.put("windowTypes", types)
			.put("texts", texts)
			.toString();
	}

	/**
	 * Renders what the desktop needs in the browser: {@link #JS} and {@link #CSS}, with the style
	 * sheets of the dialog asking before a window with changes closes, {@link Modal#CSS} and
	 * {@link ButtonBehavior#CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		response.render(CssHeaderItem.forReference(Modal.CSS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
