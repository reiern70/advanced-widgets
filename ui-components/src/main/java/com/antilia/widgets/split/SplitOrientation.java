/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.split;

import java.util.Locale;

/**
 * How a {@link SplitPanel} lays out its panes.
 *
 * @since 11.0.0
 */
public enum SplitOrientation
{
	/** The panes side by side, the divider standing between them. */
	HORIZONTAL,

	/** The panes one above the other, the divider lying between them. */
	VERTICAL;

	/**
	 * @return the name of the orientation in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}
}
