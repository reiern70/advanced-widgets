/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.desktop;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.modal.Modal;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

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
 * Closing a window with changes not saved asks first, in a dialog of the look of
 * {@link com.antilia.widgets.modal.Modal}: a window has changes when a field in it holds another
 * value than the one it was rendered with, or as {@link DesktopWindow#setModified(Boolean)} says.
 * Closing it then puts its fields back as they were rendered, telling their Ajax behaviors.
 * <p>
 * {@code Wicket.Desktop} from {@link #JS} does all that in the browser; it sets where the windows
 * are and their sizes as CSS custom properties through the CSSOM, without an inline style, so the
 * desktop works under a strict Content Security Policy. The look comes from {@link #CSS}, in the
 * colors of the {@link com.antilia.widgets.theme.Theme theme} the desktop is in; give the desktop
 * a height in the CSS of the page, 30rem by default.
 * <p>
 * The windows are {@link DesktopWindow}s, created with the ids {@link #newWindowId()} gives and
 * added with {@link #addWindow(DesktopWindow)}. A window rendered again by Ajax keeps its state in
 * the browser. With {@link #setSendToServer(boolean)} the browser sends the server, via Ajax, the
 * states of all windows each time the user has changed any, with the side of the taskbar and
 * whether the desktop fills the page; the windows give their models the new states, the desktop
 * keeps the side and whether it fills the page, and calls {@link #onChanged(AjaxRequestTarget)}.
 * <p>
 * The desktop is a private component: without a license key activating it, it renders a
 * placeholder telling so, see {@link Licensing}.
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

	/** The texts of the desktop, by their names in the script; their keys are Desktop.name. */
	private static final String[] TEXTS = { "minimize", "maximize", "restore", "close", "taskbar",
		"grid", "columns", "rows", "cascade", "expand", "collapse", "move", "menu", "openWindows",
		"closedWindows", "closeModifiedTitle", "closeModified", "discard", "keep" };

	private final WebMarkupContainer desktop;

	private final RepeatingView windows;

	private final AbstractDefaultAjaxBehavior changed;

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
					parameters.getParameterValue(EXPANDED_PARAMETER).toOptionalString());
			}
		};
		// on the element of the desktop, which has an id even when the panel's tag is not rendered
		desktop.add(changed);
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
	 * taskbar and whether the desktop fills the page, and calls back.
	 */
	private void onStates(AjaxRequestTarget target, String json, String taskbar, String expand)
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
		if (sendToServer)
		{
			script.append(", ").append(changed.getCallbackFunction(
				CallbackParameter.explicit(STATES_PARAMETER),
				CallbackParameter.explicit(TASKBAR_PARAMETER),
				CallbackParameter.explicit(EXPANDED_PARAMETER)));
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
		return new JSONObject()
			.put("taskbar", taskbarPosition.cssName())
			.put("tiling", tiling)
			.put("expandable", expandable)
			.put("expanded", expanded)
			.put("movableTaskbar", movableTaskbar)
			.put("menu", menu)
			.put("texts", texts)
			.toString();
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		Licensing.configurePrivate(this);
	}

	@Override
	protected void renderPlaceholderTag(ComponentTag tag, Response response)
	{
		if (!Licensing.renderPlaceholder(this, tag, response))
		{
			super.renderPlaceholderTag(tag, response);
		}
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
