/*
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

/**
 * The layout of the columns of a {@link BaseDynamicDataTable} as the user arranged it: their order,
 * which of them are hidden and their widths; and how the user chose to see the rows: the number
 * of rows per page and the column they sorted them by. Columns are referred to by their
 * {@link IDynamicColumn#getId() ids}, so a state can be stored, for example
 * by an {@link IColumnStateStore}, and applied to a table built again later.
 * <p>
 * The rows per page and the sort are {@code null} until the user chooses them, so the table
 * keeps those its page gives it; a state made without them, as by a store written before they
 * were kept, has them {@code null} too.
 * <p>
 * A state is immutable.
 *
 * @see BaseDynamicDataTable#getColumnState()
 * @see BaseDynamicDataTable#setColumnState(ColumnState)
 * @since 11.0.0
 */
public final class ColumnState implements IClusterable
{
	private static final long serialVersionUID = 1L;

	private final List<String> order;

	private final Set<String> hidden;

	private final Map<String, Double> widths;

	private final Long itemsPerPage;

	private final String sortProperty;

	private final boolean sortAscending;

	/**
	 * A state of the columns alone, without rows per page or sort.
	 *
	 * @param order
	 *            the ids of the columns, in display order
	 * @param hidden
	 *            the ids of the hidden columns
	 * @param widths
	 *            the widths of the columns in pixels, by id; columns without a known width are
	 *            left out
	 */
	public ColumnState(List<String> order, Collection<String> hidden, Map<String, Double> widths)
	{
		this(order, hidden, widths, null, null, false);
	}

	/**
	 * @param order
	 *            the ids of the columns, in display order
	 * @param hidden
	 *            the ids of the hidden columns
	 * @param widths
	 *            the widths of the columns in pixels, by id; columns without a known width are
	 *            left out
	 * @param itemsPerPage
	 *            the number of rows per page the user chose, or {@code null}
	 * @param sortProperty
	 *            the {@link BaseDynamicDataTable#getSortProperty(org.apache.wicket.model.IDetachable)
	 *            sort property} of the column the user sorted the rows by, as text, or
	 *            {@code null}
	 * @param sortAscending
	 *            whether the rows are sorted in ascending order; ignored without a sort property
	 * @since 11.0.0
	 */
	public ColumnState(List<String> order, Collection<String> hidden, Map<String, Double> widths,
		Long itemsPerPage, String sortProperty, boolean sortAscending)
	{
		this.order = List.copyOf(Args.notNull(order, "order"));
		this.hidden = Collections.unmodifiableSet(
			new LinkedHashSet<>(Args.notNull(hidden, "hidden")));
		this.widths = Collections.unmodifiableMap(
			new LinkedHashMap<>(Args.notNull(widths, "widths")));
		this.itemsPerPage = itemsPerPage;
		this.sortProperty = sortProperty;
		this.sortAscending = sortProperty != null && sortAscending;
	}

	/**
	 * @return the ids of the columns, in display order
	 */
	public List<String> getOrder()
	{
		return order;
	}

	/**
	 * @return the ids of the hidden columns
	 */
	public Set<String> getHidden()
	{
		return hidden;
	}

	/**
	 * @return the widths of the columns in pixels, by id
	 */
	public Map<String, Double> getWidths()
	{
		return widths;
	}

	/**
	 * @return the number of rows per page the user chose, or {@code null} if they did not
	 * @since 11.0.0
	 */
	public Long getItemsPerPage()
	{
		return itemsPerPage;
	}

	/**
	 * @return the sort property of the column the user sorted the rows by, as text, or
	 *         {@code null} if they did not sort them
	 * @since 11.0.0
	 */
	public String getSortProperty()
	{
		return sortProperty;
	}

	/**
	 * @return whether the user sorted the rows in ascending order; {@code false} without a
	 *         {@link #getSortProperty() sort property}
	 * @since 11.0.0
	 */
	public boolean isSortAscending()
	{
		return sortAscending;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof ColumnState other && order.equals(other.order) &&
			hidden.equals(other.hidden) && widths.equals(other.widths) &&
			Objects.equals(itemsPerPage, other.itemsPerPage) &&
			Objects.equals(sortProperty, other.sortProperty) &&
			sortAscending == other.sortAscending;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(order, hidden, widths, itemsPerPage, sortProperty, sortAscending);
	}

	@Override
	public String toString()
	{
		return "ColumnState [order=" + order + ", hidden=" + hidden + ", widths=" + widths +
			", itemsPerPage=" + itemsPerPage + ", sortProperty=" + sortProperty +
			", sortAscending=" + sortAscending + "]";
	}
}
