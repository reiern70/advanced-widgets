/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.dynamicdatatable;

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
