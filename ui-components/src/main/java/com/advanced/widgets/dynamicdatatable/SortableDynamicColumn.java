/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

/**
 * Makes any column of a {@link JsonBasedDynamicDataTable} sortable: the header, template, CSS
 * class, export value and header items are those of the wrapped column, and the rows are sorted by
 * the given property. The actions of a wrapped {@link CompoundDynamicColumn} are registered with
 * the table as if it were given directly.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort property
 * @since 11.0.0
 */
public class SortableDynamicColumn<T, S>
	implements
		ISortableDynamicColumn<T, S>,
		IHeaderContributor
{
	private static final long serialVersionUID = 1L;

	private final IDynamicColumn<T> column;

	private final S sortProperty;

	/**
	 * @param column
	 *            the column to sort by
	 * @param sortProperty
	 *            the property the rows are sorted by
	 */
	public SortableDynamicColumn(IDynamicColumn<T> column, S sortProperty)
	{
		this.column = Args.notNull(column, "column");
		this.sortProperty = Args.notNull(sortProperty, "sortProperty");
	}

	/**
	 * @return the wrapped column
	 */
	public IDynamicColumn<T> getColumn()
	{
		return column;
	}

	@Override
	public S getSortProperty()
	{
		return sortProperty;
	}

	@Override
	public IModel<String> getHeader()
	{
		return column.getHeader();
	}

	@Override
	public String getTemplate()
	{
		return column.getTemplate();
	}

	@Override
	public String getTemplate(JsonBasedDynamicDataTable<T, ?> table)
	{
		return column.getTemplate(table);
	}

	@Override
	public String getId()
	{
		return column.getId();
	}

	@Override
	public String getCssClass()
	{
		return column.getCssClass();
	}

	@Override
	public Component newHeader(String id)
	{
		return column.newHeader(id);
	}

	@Override
	public boolean isResizable()
	{
		return column.isResizable();
	}

	@Override
	public boolean isExportable()
	{
		return column.isExportable();
	}

	@Override
	public Object getExportValue(T row)
	{
		return column.getExportValue(row);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		if (column instanceof IHeaderContributor contributor)
		{
			contributor.renderHead(response);
		}
	}

	@Override
	public void detach()
	{
		column.detach();
	}
}
