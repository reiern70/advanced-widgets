/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.desktop;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookContainer;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * A window of a {@link Desktop}: a title bar, with an icon and the title, and a body, the component
 * {@link #newContent(String)} returns. It floats on the desktop without being modal: the user
 * drags it by its title bar, brings it to the front with a click, folds it with a double click on
 * the title bar and, as the window allows, resizes it by its edges and corners, minimizes it to the
 * taskbar, maximizes it to the whole desktop and closes it. Create it with the id
 * {@link Desktop#newWindowId()} gives and {@link Desktop#addWindow(DesktopWindow) add it} to the
 * desktop; its tag is the window, so it keeps its markup id.
 * <p>
 * Where the window is, its size and what the user made of it, the {@link WindowState}, is the
 * window's model. By default the model is kept in the window, and so in the page, and starts
 * empty: the window shows the state it was given with {@link #setPosition(Integer, Integer)},
 * {@link #setSize(Integer, Integer)}, {@link #setMinimized(boolean)} and the like. With
 * {@link Desktop#setSendToServer(boolean)} the browser sends the server what the user changes, and
 * the window gives its model the new state, which it renders again when it or its page is rendered
 * again. To keep the state elsewhere, in the session for example, give the window a model that
 * stores what it is given. The browser keeps the state of a window rendered again by Ajax anyway.
 * <p>
 * A window is in a {@link DesktopPane pane} of the desktop, the one {@link #setPane(String)} says,
 * or the one shown when it is rendered; the user moves it to another pane with the first button
 * on the right of its title bar.
 *
 * @since 11.0.0
 */
public abstract class DesktopWindow extends Panel
{
	private static final long serialVersionUID = 1L;

	private final WebMarkupContainer body;

	private WindowState initial = new WindowState();

	private IIcon icon;

	private Integer minWidth;

	private Integer minHeight;

	private boolean resizable = true;

	private boolean maximizable = true;

	private boolean minimizable = true;

	private boolean foldable = true;

	private boolean closable;

	private Boolean modified;

	/** Whether the desktop added the window of a type, and takes it away when it is closed. */
	private boolean disposable;

	/**
	 * A window keeping its state in the page.
	 *
	 * @param id
	 *            the id {@link Desktop#newWindowId()} gives
	 * @param title
	 *            the title, in the title bar and on the button of the taskbar
	 */
	public DesktopWindow(String id, IModel<String> title)
	{
		this(id, title, null);
	}

	/**
	 * @param id
	 *            the id {@link Desktop#newWindowId()} gives
	 * @param title
	 *            the title, in the title bar and on the button of the taskbar
	 * @param model
	 *            where the window keeps its state; empty until the user changes it, with
	 *            {@link Desktop#setSendToServer(boolean)}; in the page when {@code null}
	 */
	public DesktopWindow(String id, IModel<String> title, IModel<WindowState> model)
	{
		super(id, model == null ? Model.of((WindowState)null) : model);
		setOutputMarkupId(true);
		add(new LookContainer("header", LookPart.WINDOW_HEADER));
		add(new Label("title", title).setOutputMarkupId(true)
			.add(new LookBehavior(LookPart.WINDOW_TITLE)));
		add(new Label("icon", () -> icon != null ? icon.getMarkup() : null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(icon != null);
			}
		}.setEscapeModelStrings(false).add(new LookBehavior(LookPart.WINDOW_ICON)));
		body = new WebMarkupContainer("body");
		body.add(new LookBehavior(LookPart.WINDOW_BODY));
		add(body);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		body.add(newContent("content"));
	}

	/**
	 * Creates what the body of the window shows.
	 *
	 * @param id
	 *            the id the component must have
	 * @return the component
	 */
	protected abstract Component newContent(String id);

	/**
	 * @return the container of the body, below the title bar, holding the content
	 */
	public WebMarkupContainer getBody()
	{
		return body;
	}

	/**
	 * @return the model keeping the state of the window
	 */
	@SuppressWarnings("unchecked")
	public IModel<WindowState> getModel()
	{
		return (IModel<WindowState>)getDefaultModel();
	}

	/**
	 * @return the state the model has, {@code null} until the user has changed it
	 */
	public WindowState getModelObject()
	{
		return getModel().getObject();
	}

	/**
	 * @param state
	 *            the state the model gets, {@code null} for the one the window was given
	 * @return {@code this}
	 */
	public DesktopWindow setModelObject(WindowState state)
	{
		setDefaultModelObject(state);
		return this;
	}

	/**
	 * @return the state of the window: as the model has it, or as the window was given it while
	 *         the model is empty
	 */
	public WindowState getState()
	{
		WindowState state = getModelObject();
		return state != null ? state : initial;
	}

	/**
	 * @param x
	 *            where the window starts, from the left of the desktop, in pixels; {@code null}
	 *            for the browser to cascade the windows
	 * @param y
	 *            where the window starts, from the top of the desktop, in pixels
	 * @return {@code this}
	 */
	public DesktopWindow setPosition(Integer x, Integer y)
	{
		initial = initial.withPosition(x, y);
		return this;
	}

	/**
	 * @param width
	 *            the width the window starts with, in pixels; {@code null} for 420
	 * @param height
	 *            the height the window starts with, in pixels; {@code null} for 280
	 * @return {@code this}
	 */
	public DesktopWindow setSize(Integer width, Integer height)
	{
		initial = initial.withSize(width, height);
		return this;
	}

	/**
	 * @param minimized
	 *            whether the window starts minimized to the taskbar
	 * @return {@code this}
	 */
	public DesktopWindow setMinimized(boolean minimized)
	{
		initial = initial.withMinimized(minimized);
		return this;
	}

	/**
	 * @param maximized
	 *            whether the window starts maximized to the whole desktop
	 * @return {@code this}
	 */
	public DesktopWindow setMaximized(boolean maximized)
	{
		initial = initial.withMaximized(maximized);
		return this;
	}

	/**
	 * @param folded
	 *            whether the window starts folded to its title bar
	 * @return {@code this}
	 */
	public DesktopWindow setFolded(boolean folded)
	{
		initial = initial.withFolded(folded);
		return this;
	}

	/**
	 * @param closed
	 *            whether the window starts closed
	 * @return {@code this}
	 */
	public DesktopWindow setClosed(boolean closed)
	{
		initial = initial.withClosed(closed);
		return this;
	}

	/**
	 * Puts the window in a pane, as it starts and as its model has it.
	 *
	 * @param pane
	 *            the id of a {@link DesktopPane pane} of the desktop; {@code null} for the one
	 *            shown when the window is rendered
	 * @return {@code this}
	 */
	public DesktopWindow setPane(String pane)
	{
		initial = initial.withPane(pane);
		WindowState state = getModelObject();
		if (state != null)
		{
			setModelObject(state.withPane(pane));
		}
		return this;
	}

	/**
	 * @return the id of the pane the window is in, {@code null} for the one shown when it is
	 *         rendered
	 */
	public String getPane()
	{
		return getState().getPane();
	}

	/**
	 * @return whether the desktop added the window, of a {@link WindowType type}, and takes it
	 *         away when the user closes it
	 */
	public boolean isDisposable()
	{
		return disposable;
	}

	/**
	 * @param disposable
	 *            whether the desktop takes the window away when the user closes it
	 * @return {@code this}
	 */
	DesktopWindow setDisposable(boolean disposable)
	{
		this.disposable = disposable;
		return this;
	}

	/**
	 * @return the icon before the title, which the taskbar shows too; {@code null} for none
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon before the title, which the taskbar shows too; {@code null} for none
	 * @return {@code this}
	 */
	public DesktopWindow setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @param minWidth
	 *            the smallest width of the window, in pixels; {@code null} for 180
	 * @param minHeight
	 *            the smallest height of the window, in pixels; {@code null} for 100
	 * @return {@code this}
	 */
	public DesktopWindow setMinSize(Integer minWidth, Integer minHeight)
	{
		this.minWidth = minWidth;
		this.minHeight = minHeight;
		return this;
	}

	/**
	 * @return whether the edges and corners of the window resize it, {@code true} by default
	 */
	public boolean isResizable()
	{
		return resizable;
	}

	/**
	 * @param resizable
	 *            whether the edges and corners of the window resize it
	 * @return {@code this}
	 */
	public DesktopWindow setResizable(boolean resizable)
	{
		this.resizable = resizable;
		return this;
	}

	/**
	 * @return whether the window can be maximized, with its button or F11, {@code true} by default
	 */
	public boolean isMaximizable()
	{
		return maximizable;
	}

	/**
	 * @param maximizable
	 *            whether the window can be maximized, with its button or F11
	 * @return {@code this}
	 */
	public DesktopWindow setMaximizable(boolean maximizable)
	{
		this.maximizable = maximizable;
		return this;
	}

	/**
	 * @return whether the window can be minimized to the taskbar, {@code true} by default
	 */
	public boolean isMinimizable()
	{
		return minimizable;
	}

	/**
	 * @param minimizable
	 *            whether the window can be minimized to the taskbar
	 * @return {@code this}
	 */
	public DesktopWindow setMinimizable(boolean minimizable)
	{
		this.minimizable = minimizable;
		return this;
	}

	/**
	 * @return whether a double click on the title bar folds the window, {@code true} by default
	 */
	public boolean isFoldable()
	{
		return foldable;
	}

	/**
	 * @param foldable
	 *            whether a double click on the title bar folds the window
	 * @return {@code this}
	 */
	public DesktopWindow setFoldable(boolean foldable)
	{
		this.foldable = foldable;
		return this;
	}

	/**
	 * @return whether the window has a button closing it, {@code false} by default
	 */
	public boolean isClosable()
	{
		return closable;
	}

	/**
	 * @param closable
	 *            whether the window has a button closing it
	 * @return {@code this}
	 */
	public DesktopWindow setClosable(boolean closable)
	{
		this.closable = closable;
		return this;
	}

	/**
	 * @return whether the window has changes not saved, which the user is asked about before it
	 *         closes; {@code null}, as by default, for the browser to tell by its fields
	 */
	public Boolean getModified()
	{
		return modified;
	}

	/**
	 * Says whether the window has changes not saved, which the user is asked about before it closes,
	 * the next time it is rendered. Left {@code null}, the browser tells by the fields of the window:
	 * whether one holds another value than the one it was rendered with. {@code false} suits a
	 * window whose fields only search or choose, never losing anything.
	 *
	 * @param modified
	 *            whether the window has changes not saved; {@code null} for the browser to tell
	 * @return {@code this}
	 */
	public DesktopWindow setModified(Boolean modified)
	{
		this.modified = modified;
		return this;
	}

	/**
	 * Renders the window's tag as the script takes it: its state and what the user may do with
	 * it in data attributes, hidden when minimized or closed so that it does not show before the
	 * script runs.
	 */
	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.WINDOW);
		tag.put("role", "region");
		tag.put("tabindex", "-1");
		tag.put("aria-labelledby", get("title").getMarkupId());
		WindowState state = getState();
		putNumber(tag, "data-x", state.getX());
		putNumber(tag, "data-y", state.getY());
		putNumber(tag, "data-width", state.getWidth());
		putNumber(tag, "data-height", state.getHeight());
		putNumber(tag, "data-min-width", minWidth);
		putNumber(tag, "data-min-height", minHeight);
		if (state.getOrder() > 0)
		{
			tag.put("data-order", state.getOrder());
		}
		putFlag(tag, "data-minimized", state.isMinimized() && minimizable);
		putFlag(tag, "data-maximized", state.isMaximized() && maximizable);
		putFlag(tag, "data-folded", state.isFolded() && foldable);
		putFlag(tag, "data-closed", state.isClosed());
		if ((state.isMinimized() && minimizable) || state.isClosed())
		{
			tag.put("hidden", "hidden");
		}
		if (!resizable)
		{
			tag.put("data-resizable", "false");
		}
		if (!maximizable)
		{
			tag.put("data-maximizable", "false");
		}
		if (!minimizable)
		{
			tag.put("data-minimizable", "false");
		}
		if (!foldable)
		{
			tag.put("data-foldable", "false");
		}
		if (closable)
		{
			tag.put("data-closable", "true");
		}
		if (modified != null)
		{
			tag.put("data-modified", modified.toString());
		}
		if (state.getPane() != null)
		{
			tag.put("data-pane", state.getPane());
		}
		if (disposable)
		{
			tag.put("data-disposable", "");
		}
	}

	private static void putNumber(ComponentTag tag, String name, Integer value)
	{
		if (value != null)
		{
			tag.put(name, value);
		}
	}

	private static void putFlag(ComponentTag tag, String name, boolean value)
	{
		if (value)
		{
			tag.put(name, "");
		}
	}
}
