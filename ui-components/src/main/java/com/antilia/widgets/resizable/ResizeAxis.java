/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.resizable;

import java.util.Locale;

/**
 * The dimensions the user resizes an element in, with a {@link ResizableBehavior} or in a
 * {@link com.antilia.widgets.codeeditor.CodeEditor}.
 *
 * @since 11.0.0
 */
public enum ResizeAxis
{
	/** The height alone, by the bottom edge. */
	VERTICAL,

	/** The width alone, by the right edge. */
	HORIZONTAL,

	/** Both, by the bottom right corner. */
	BOTH;

	/**
	 * @return the name of the axis in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return whether the user changes the width
	 */
	public boolean isHorizontal()
	{
		return this != VERTICAL;
	}

	/**
	 * @return whether the user changes the height
	 */
	public boolean isVertical()
	{
		return this != HORIZONTAL;
	}

	/**
	 * @param name
	 *            the name of an axis in the script, such as {@code vertical}
	 * @return the axis, {@code null} for none known
	 */
	public static ResizeAxis fromCssName(String name)
	{
		for (ResizeAxis axis : values())
		{
			if (axis.cssName().equals(name))
			{
				return axis;
			}
		}
		return null;
	}
}
