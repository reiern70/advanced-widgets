/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import com.advanced.widgets.icon.FontAwesomeIcon;
import com.advanced.widgets.icon.IIcon;
import com.advanced.widgets.icon.SvgIcon;

/**
 * The icons the actions of the dynamic table examples are drawn with.
 */
public enum ActionIcons
{
	/** The icons built into ui-components. */
	BUILT_IN,

	/** The solid icons of Font Awesome as inline SVG. */
	SVG,

	/** The solid icons of Font Awesome drawn by its font. */
	FONT_AWESOME;

	/**
	 * @return the icon of this set
	 */
	public IIcon pick(IIcon builtIn, SvgIcon svg, FontAwesomeIcon fontAwesome)
	{
		return switch (this)
		{
			case BUILT_IN -> builtIn;
			case SVG -> svg;
			case FONT_AWESOME -> fontAwesome;
		};
	}
}
