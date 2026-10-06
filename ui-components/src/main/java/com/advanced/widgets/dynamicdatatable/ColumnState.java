/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

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
 * which of them are hidden and their widths. Columns are referred to by their
 * {@link IDynamicColumn#getId() ids}, so a state can be stored, for example
 * by an {@link IColumnStateStore}, and applied to a table built again later.
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

	/**
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
		this.order = List.copyOf(Args.notNull(order, "order"));
		this.hidden = Collections.unmodifiableSet(
			new LinkedHashSet<>(Args.notNull(hidden, "hidden")));
		this.widths = Collections.unmodifiableMap(
			new LinkedHashMap<>(Args.notNull(widths, "widths")));
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

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof ColumnState other && order.equals(other.order) &&
			hidden.equals(other.hidden) && widths.equals(other.widths);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(order, hidden, widths);
	}

	@Override
	public String toString()
	{
		return "ColumnState [order=" + order + ", hidden=" + hidden + ", widths=" + widths + "]";
	}
}
