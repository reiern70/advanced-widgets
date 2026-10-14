/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;

/**
 * A line between the items of a submenu, or upright between those of a bar along the top. It has
 * neither label nor link.
 *
 * @since 11.0.0
 */
public enum MenuSeparator implements IMenuItem
{
	/** The separator. */
	INSTANCE;

	/**
	 * @return {@code null}: a separator has no label
	 */
	@Override
	public IModel<String> getLabel()
	{
		return null;
	}

	/**
	 * @return {@code null}: a separator has no link
	 */
	@Override
	public AbstractLink getLink(String id)
	{
		return null;
	}
}
