/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.table;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A table keeping on the server the widths the user resized its columns to, so that they survive
 * the table being rendered again. A {@link ResizableColumnsBehavior} added to such a table hands
 * the widths to the browser and reports every resize back; added to any other table, it keeps
 * the widths in the browser only.
 *
 * @since 11.0.0
 */
public interface IResizableColumnsTable
{
	/**
	 * @return the widths, in pixels, of the columns the table shows, one for each in display
	 *         order, or {@code null} while the user has not resized them
	 */
	double[] getShownColumnWidths();

	/**
	 * Called when the user resized a column.
	 *
	 * @param target
	 *            the handler of the request reporting the resize
	 * @param widths
	 *            the widths, in pixels, of the columns the table shows, one for each in display
	 *            order; each is positive and finite, but sent by the browser
	 */
	void columnsResized(AjaxRequestTarget target, double[] widths);
}
