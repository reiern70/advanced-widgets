/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.desktop;

import java.util.Locale;

/**
 * The side of a {@link Desktop} its taskbar is on.
 *
 * @since 11.0.0
 */
public enum TaskbarPosition
{
	/** Below the windows, as by default. */
	BOTTOM,

	/** Above the windows. */
	TOP,

	/** On the left of the windows. */
	LEFT,

	/** On the right of the windows. */
	RIGHT;

	/**
	 * @return the name of the position in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param cssName
	 *            the name of a position in the script and in the CSS classes
	 * @return the position of that name, {@code null} when there is none
	 */
	public static TaskbarPosition of(String cssName)
	{
		for (TaskbarPosition position : values())
		{
			if (position.cssName().equals(cssName))
			{
				return position;
			}
		}
		return null;
	}
}
