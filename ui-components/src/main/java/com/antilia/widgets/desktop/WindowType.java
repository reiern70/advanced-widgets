/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.desktop;

import java.util.Objects;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;

import com.antilia.widgets.icon.IIcon;

/**
 * A type of window of a {@link Desktop}, which the launcher of its taskbar opens: the desktop
 * asks the type for a new {@link DesktopWindow} each time the user opens one, adds it to the pane
 * shown and renders it by Ajax, and takes it away again when the user closes it. Register the
 * types with {@link Desktop#addWindowType(WindowType)}.
 *
 * @since 11.0.0
 */
public abstract class WindowType implements IClusterable
{
	private static final long serialVersionUID = 1L;

	private final String type;

	private final IModel<String> title;

	private IIcon icon;

	/**
	 * @param type
	 *            the name of the type, unique in the desktop
	 * @param title
	 *            what the launcher says for it
	 */
	protected WindowType(String type, IModel<String> title)
	{
		this.type = Objects.requireNonNull(type);
		this.title = Objects.requireNonNull(title);
	}

	/**
	 * Creates a window of this type; the desktop makes it closable.
	 *
	 * @param id
	 *            the id the window must have
	 * @return the window
	 */
	public abstract DesktopWindow newWindow(String id);

	/**
	 * @return the name of the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @return what the launcher says for it
	 */
	public IModel<String> getTitle()
	{
		return title;
	}

	/**
	 * @return the icon in the launcher, {@code null} for none
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon in the launcher, {@code null} for none
	 * @return {@code this}
	 */
	public WindowType setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}
}
