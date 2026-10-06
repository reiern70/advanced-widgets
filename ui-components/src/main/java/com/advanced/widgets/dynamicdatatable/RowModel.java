/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * The model of a row of an {@link IDynamicDataProvider}, keeping only its key once detached.
 *
 * @param <T>
 *            the type of the row
 * @param <K>
 *            the type of the key
 */
class RowModel<T, K extends Serializable> extends LoadableDetachableModel<T>
{
	private static final long serialVersionUID = 1L;

	private final IDynamicDataProvider<T, K> provider;

	private final K key;

	RowModel(IDynamicDataProvider<T, K> provider, T row)
	{
		super(row);
		this.provider = provider;
		key = provider.keyOf(row);
	}

	@Override
	protected T load()
	{
		return provider.findByKey(key);
	}
}
