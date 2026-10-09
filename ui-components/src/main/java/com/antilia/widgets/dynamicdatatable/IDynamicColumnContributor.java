/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.model.IDetachable;

/**
 * A piece of a {@link CompoundDynamicColumn}'s cell.
 * <p>
 * The template is trusted markup, inserted into the page as is, and has to be authored by the
 * developer; see {@link IDynamicColumn} for how it is evaluated.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
@FunctionalInterface
public interface IDynamicColumnContributor<T> extends IDetachable
{
	/**
	 * @return this contributor's part of the cell template
	 */
	String getTemplate();

	/**
	 * Returns this contributor's part of the cell template as rendered in the given table, which
	 * may depend on its locale, style or variation. Returns {@link #getTemplate()} by default.
	 *
	 * @param table
	 *            the table the contributor's column is rendered in
	 * @return this contributor's part of the cell template
	 */
	default String getTemplate(BaseDynamicDataTable<T, ?, ?> table)
	{
		return getTemplate();
	}

	@Override
	default void detach()
	{
	}
}
