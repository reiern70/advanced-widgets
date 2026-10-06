/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;

/**
 * An {@link IDynamicDataProvider} of rows that know their key: {@link #keyOf(IIdentifiable)} is
 * the row's {@link IIdentifiable#getId()}.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys
 * @since 11.0.0
 */
public interface IIdentifiableDynamicDataProvider<T extends IIdentifiable<K>,
	K extends Serializable>
	extends
		IDynamicDataProvider<T, K>
{
	/**
	 * @return the row's {@link IIdentifiable#getId()}
	 */
	@Override
	default K keyOf(T row)
	{
		return row.getId();
	}
}
