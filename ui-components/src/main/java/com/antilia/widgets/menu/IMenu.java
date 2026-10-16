/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import java.util.List;

import org.apache.wicket.Component;

/**
 * An item of a {@link Menu} that opens a submenu of more items, which may be submenus too, as deep
 * as one likes. Its label, icon and tooltip are those of the item opening it; it has no link. A
 * menu takes an {@code IMenu} as its items too: its label is then the accessible name of the bar.
 * {@link SubMenu} is the one of ui-components.
 *
 * @since 11.0.0
 */
public interface IMenu extends IMenuItem
{
	/**
	 * @return the items of the submenu, in order
	 */
	List<? extends IMenuItem> getItems();

	/**
	 * A submenu has no link: its item opens it.
	 *
	 * @return {@code null}
	 */
	@Override
	default Component getLink(String id)
	{
		return null;
	}

	@Override
	default void detach()
	{
		getItems().forEach(IMenuItem::detach);
	}
}
