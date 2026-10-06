/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.table;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A table whose columns the user can move to other places, with a {@link MovableColumnsBehavior}
 * added to it. The table decides which columns can move and moves them, rendering itself again.
 *
 * @since 11.0.0
 */
public interface IMovableColumnsTable
{
	/**
	 * @return for each column the table shows, in display order, whether the user can move it;
	 *         columns that cannot move keep their places when others move
	 */
	boolean[] getMovableColumns();

	/**
	 * Called when the user moved a column. The indexes are checked to be within the shown
	 * columns, the moved column to be movable and every column that is not to keep its place.
	 *
	 * @param target
	 *            the handler of the request reporting the move, to render the table again with
	 * @param from
	 *            the index of the moved column among the shown columns
	 * @param to
	 *            the index of the column it goes in front of, or the number of shown columns to
	 *            put it at the end
	 */
	void moveColumn(AjaxRequestTarget target, int from, int to);
}
