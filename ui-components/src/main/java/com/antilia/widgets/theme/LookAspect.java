/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.theme;

import java.util.Locale;

/**
 * What a {@link LookPart} says about its element. A {@link CompoundLook} takes each aspect from a
 * look of its choice: Bootstrap's classes with the colors and the sizes of Antilia's, for example.
 *
 * @since 11.0.0
 */
public enum LookAspect
{
	/** What the element is: a dialog's header, a table, a button... */
	BASE,
	/** The colors of the element: a primary button, a notification of an error... */
	COLOR,
	/** The size of the element: a small button, a large dialog... */
	SIZE,
	/** A state the element is in: the option of a list that is active, a row selected... */
	STATE;

	/**
	 * @return the name of the aspect in the browser, such as {@code color}
	 */
	public String getName()
	{
		return name().toLowerCase(Locale.ROOT);
	}
}
