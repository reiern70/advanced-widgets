/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

/**
 * Marks a column of a {@link BaseDynamicDataTable} the user can hide, for example with a
 * {@link ColumnChooserToolbarAction}. Columns without the mark stay shown, unless the application
 * hides them with {@link BaseDynamicDataTable#setColumnVisible(org.apache.wicket.model.IDetachable,
 * boolean)}. A {@link SortableDynamicColumn} is hideable if the column it wraps is.
 *
 * @see BaseDynamicDataTable#isHideable(org.apache.wicket.model.IDetachable)
 * @since 11.0.0
 */
public interface IHideableColumn
{
}
