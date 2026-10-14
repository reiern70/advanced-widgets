/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import java.util.Locale;

import com.antilia.widgets.theme.Look;
import com.antilia.widgets.theme.LookPart;

/**
 * The designs of a {@link Menu}, in the colors of the {@link com.antilia.widgets.theme.Theme theme}
 * it is in. Every design works in both {@link MenuOrientation orientations}. In Bootstrap's look the
 * bar is Bootstrap's nav: pills and underline are its nav-pills and nav-underline.
 *
 * @since 11.0.0
 */
public enum MenuDesign
{
	/** A bar of the toolbar's color, as the menu of an application. */
	BAR,

	/** Rounded items, the current one filled with the theme's primary color. */
	PILLS,

	/** Plain items, the current one underlined in the theme's primary color. */
	UNDERLINE,

	/** The side bar of an application, an accent on the current item. */
	SIDEBAR;

	/**
	 * @return the name of the design in the script
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the CSS class putting the design on the menu, that of its part in
	 *         {@link Look#ANTILIA}
	 */
	public String getCssClass()
	{
		return Look.ANTILIA.getCssClass(getPart());
	}

	/**
	 * @return the part of a look the menu is in this design
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("MENU_" + name());
	}

	/**
	 * @return the part of a look the bar is in this design, such as Bootstrap's
	 *         {@code nav-pills}
	 */
	public LookPart getListPart()
	{
		return LookPart.valueOf("MENU_LIST_" + name());
	}
}
