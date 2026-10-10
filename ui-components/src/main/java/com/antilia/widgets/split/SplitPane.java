/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.split;

import java.util.Locale;

/**
 * A pane of a {@link SplitPanel}, the one a collapsible split panel has collapsed.
 *
 * @since 11.0.0
 */
public enum SplitPane
{
	/** The left or top pane. */
	FIRST,

	/** The right or bottom pane. */
	SECOND;

	/**
	 * @return the name of the pane in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param name
	 *            the name of a pane in the script, {@code first} or {@code second}
	 * @return the pane, {@code null} for any other name
	 */
	public static SplitPane fromCssName(String name)
	{
		for (SplitPane pane : values())
		{
			if (pane.cssName().equals(name))
			{
				return pane;
			}
		}
		return null;
	}
}
