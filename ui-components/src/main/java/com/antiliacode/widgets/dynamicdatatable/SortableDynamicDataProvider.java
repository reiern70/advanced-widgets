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

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.util.lang.Args;

/**
 * Base class for a {@link ISortableDynamicDataProvider} sorting by one property at a time, like
 * {@link org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider} does for a
 * {@code DataTable}. Subclasses read the current {@link #getSort() sort} in
 * {@link #iterator(long, long)}.
 * <p>
 * Subclasses give the keys of the rows with {@link #keyOf(Object)}, or take them from the rows by
 * implementing {@link IIdentifiableDynamicDataProvider}.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public abstract class SortableDynamicDataProvider<T, K extends Serializable, S>
	implements
		ISortableDynamicDataProvider<T, K, S>
{
	private static final long serialVersionUID = 1L;

	private final SingleSortState<S> state = new SingleSortState<>();

	private final Class<K> keyType;

	/**
	 * @param keyType
	 *            the type of the keys
	 */
	protected SortableDynamicDataProvider(Class<K> keyType)
	{
		this.keyType = Args.notNull(keyType, "keyType");
	}

	@Override
	public Class<K> getKeyType()
	{
		return keyType;
	}

	@Override
	public final ISortState<S> getSortState()
	{
		return state;
	}

	/**
	 * @return the current sort, or {@code null} if the rows are not sorted
	 */
	public SortParam<S> getSort()
	{
		return state.getSort();
	}

	/**
	 * @param param
	 *            the sort, or {@code null} not to sort
	 */
	public void setSort(SortParam<S> param)
	{
		state.setSort(param);
	}

	/**
	 * @param property
	 *            the property to sort by
	 * @param order
	 *            the order
	 */
	public void setSort(S property, SortOrder order)
	{
		state.setPropertySortOrder(property, order);
	}
}
