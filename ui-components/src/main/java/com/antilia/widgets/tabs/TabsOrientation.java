/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tabs;

import java.util.Locale;

import com.antilia.widgets.theme.LookPart;

/**
 * Where the tabs of {@link Tabs} are.
 *
 * @since 11.0.0
 */
public enum TabsOrientation
{
	/** Along the top of the panels. */
	HORIZONTAL,

	/** Down the left side of the panels. */
	VERTICAL;

	/**
	 * @return the name of the orientation in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look the tabs are in this orientation
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("TABS_" + name());
	}
}
