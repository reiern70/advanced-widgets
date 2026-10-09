/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.table;

/**
 * Marks a column the user can move, in a table with a {@link MovableColumnsBehavior}: its header
 * gets a handle to drag it to another place, or to move it with the arrow keys. Columns without
 * the mark keep their places. In a
 * {@link
 * com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable} a
 * {@link
 * com.antilia.widgets.dynamicdatatable.SortableDynamicColumn}
 * is movable if the column it wraps is.
 *
 * @since 11.0.0
 */
public interface IMovableColumn
{
}
