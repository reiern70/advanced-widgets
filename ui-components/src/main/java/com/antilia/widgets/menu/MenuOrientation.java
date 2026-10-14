/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import java.util.Locale;

import com.antilia.widgets.theme.LookPart;

/**
 * Where the bar of a {@link Menu} is.
 *
 * @since 11.0.0
 */
public enum MenuOrientation
{
	/** Along the top: its items side by side, their submenus popping up below them. */
	HORIZONTAL,

	/** Down the side: its items one above the other, their submenus beside them or in place. */
	VERTICAL;

	/**
	 * @return the name of the orientation in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look the menu is in this orientation
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("MENU_" + name());
	}
}
