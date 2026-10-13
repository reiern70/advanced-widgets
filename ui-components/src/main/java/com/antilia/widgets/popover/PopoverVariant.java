/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.popover;

import java.util.Locale;

import com.antilia.widgets.theme.LookPart;

/**
 * What a {@link Popover} stands for, which sets the color of its stripe and its title, from the
 * {@link com.antilia.widgets.theme.Theme theme}.
 *
 * @since 11.0.0
 */
public enum PopoverVariant
{
	/** In the primary color of the theme. */
	DEFAULT,

	/** In the color of information. */
	INFO,

	/** In the color of what went well. */
	SUCCESS,

	/** In the color of what asks for attention. */
	WARNING,

	/** In the color of what went wrong. */
	ERROR;

	/**
	 * @return the name of the variant in the script
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look a popover is in this variant
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("POPOVER_" + name());
	}
}
