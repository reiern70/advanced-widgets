/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

/**
 * A column of a {@link JsonBasedDynamicDataTable} the rows can be sorted by, like a sortable
 * {@link org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn} of a
 * {@code DataTable}.
 * <p>
 * If the table's provider is an {@link ISortableDynamicDataProvider}, the column's header becomes
 * a link showing an arrow for the current order: a click sorts the rows by
 * {@link #getSortProperty()}, ascending first and then alternating, and shows the first page.
 * Any column can be made sortable with {@link SortableDynamicColumn}.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort property
 * @since 11.0.0
 */
public interface ISortableDynamicColumn<T, S> extends IDynamicColumn<T>
{
	/**
	 * @return the property the rows are sorted by when the header is clicked, or {@code null} if
	 *         the column is not sortable
	 */
	S getSortProperty();

	/**
	 * @return whether the header sorts the rows, {@code true} if there is a
	 *         {@link #getSortProperty() sort property}
	 */
	default boolean isSortable()
	{
		return getSortProperty() != null;
	}
}
