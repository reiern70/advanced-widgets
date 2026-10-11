/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.notification;

/**
 * Where {@link Notifications} stacks notifications: a corner of the browser window, or the middle
 * of its top or bottom edge.
 *
 * @since 11.0.0
 */
public enum NotificationPosition
{
	/** The top left corner. */
	TOP_LEFT,

	/** The middle of the top edge. */
	TOP_CENTER,

	/** The top right corner, the default. */
	TOP_RIGHT,

	/** The bottom left corner. */
	BOTTOM_LEFT,

	/** The middle of the bottom edge. */
	BOTTOM_CENTER,

	/** The bottom right corner. */
	BOTTOM_RIGHT;

	/**
	 * @return the name the script takes, such as {@code top-right}
	 */
	public String cssName()
	{
		return name().toLowerCase().replace('_', '-');
	}
}
