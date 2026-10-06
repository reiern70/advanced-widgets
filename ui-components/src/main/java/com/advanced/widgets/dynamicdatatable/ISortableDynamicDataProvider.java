/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;

/**
 * A provider of the rows of a {@link BaseDynamicDataTable} that can sort them, the counterpart of
 * {@link org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider} for
 * a {@code DataTable}.
 * <p>
 * The table changes the {@link #getSortState() sort state} when a
 * {@link ISortableDynamicColumn sortable column}'s header is clicked, and then fetches the rows
 * again; {@link #iterator(long, long)} has to return them in the order the sort state describes.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys
 * @param <S>
 *            the type of the sort properties
 * @see SortableDynamicDataProvider
 * @since 11.0.0
 */
public interface ISortableDynamicDataProvider<T, K extends Serializable, S>
	extends
		IDynamicDataProvider<T, K>,
		ISortStateLocator<S>
{
}
