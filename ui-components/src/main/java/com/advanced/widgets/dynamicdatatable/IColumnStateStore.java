/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.util.io.IClusterable;

/**
 * Keeps the {@link ColumnState} of {@link BaseDynamicDataTable}s, so the order, visibility and
 * widths of their columns survive the table: a table loads its state when it is initialized and
 * saves it whenever the user moves, shows, hides or resizes a column. An implementation can keep
 * the states anywhere, for example in a database by user, to have them survive the session and
 * restarts of the server.
 * <p>
 * A table has no store by default; {@link SessionColumnStateStore} keeps the states in the
 * session.
 *
 * @see BaseDynamicDataTable#setColumnStateStore(IColumnStateStore)
 * @since 11.0.0
 */
public interface IColumnStateStore extends IClusterable
{
	/**
	 * @param key
	 *            the {@link BaseDynamicDataTable#getColumnStateKey() key} of the table
	 * @return the state saved for the table, or {@code null} if there is none
	 */
	ColumnState load(String key);

	/**
	 * Saves the state of a table, replacing the one saved before.
	 *
	 * @param key
	 *            the {@link BaseDynamicDataTable#getColumnStateKey() key} of the table
	 * @param state
	 *            the state of its columns
	 */
	void save(String key, ColumnState state);
}
