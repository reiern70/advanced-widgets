/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.github.openjson.JSONObject;

/**
 * A dynamic data table whose rows are rendered on the server, by Wicket components, the way a
 * {@link org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable} renders them. It
 * looks and behaves like a {@link JsonBasedDynamicDataTable}: the toolbars, paging, sorting,
 * selection, overlay, body height and the resizing, moving and hiding of columns are those of
 * {@link BaseDynamicDataTable}.
 * <p>
 * The columns are Wicket's {@link IColumn}s: each cell is populated by
 * {@link IColumn#populateItem(org.apache.wicket.markup.repeater.Item, String, IModel)} with the
 * row's {@link IDynamicDataProvider#model(Object) model}, the header is
 * {@link IColumn#getHeader(String)}, a column is sortable by its
 * {@link IColumn#getSortProperty() sort property}, styled by {@link IStyledColumn#getCssClass()}
 * and exported to CSV if it is an {@link IExportableColumn}. A column the user must not resize is
 * marked {@link IFixedWidthColumn};
 * {@link com.advanced.widgets.table.IMovableColumn} and
 * {@link IHideableColumn} mark the columns the user can move and hide. {@link BeanSelectionColumn}, {@link BeanRowNumberColumn}
 * and {@link BeanActionsColumn} are the counterparts of {@link SelectionColumn},
 * {@link RowNumberColumn} and a {@link CompoundDynamicColumn} of row actions.
 * <p>
 * Changing the page, sorting or {@link #refresh(IPartialPageRequestHandler) refreshing} renders
 * the rows again via Ajax, not the whole table.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanBasedDynamicDataTable<T, K extends Serializable, S>
	extends
		BaseDynamicDataTable<T, K, IColumn<T, S>>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param columns
	 *            the columns, in display order, all of them shown
	 * @param provider
	 *            supplies the rows and resolves them back from their keys
	 * @param rowsPerPage
	 *            the number of rows shown on a page
	 */
	public BeanBasedDynamicDataTable(String id, List<? extends IColumn<T, S>> columns,
		IDynamicDataProvider<T, K> provider, long rowsPerPage)
	{
		super(id, columns, provider, rowsPerPage);
		for (IColumn<T, S> column : getColumns())
		{
			if (column instanceof BeanActionsColumn<T, S> actions)
			{
				for (IAjaxActionColumnContributor<T> contributor : actions.getContributors())
				{
					addActionContributor(contributor);
				}
			}
		}
	}

	@Override
	protected Component newRowsBody(String id)
	{
		return new BeanRowsBody<>(id, this).setOutputMarkupId(true);
	}

	/**
	 * Renders the rows again and has the browser lay them out.
	 */
	@Override
	protected void refreshRows(IPartialPageRequestHandler target)
	{
		target.add(getRowsBody());
		target.appendJavaScript("if (" + getVarName() + ") { " + getVarName() +
			".rowsReplaced(); }");
	}

	@Override
	protected void configure(JSONObject config)
	{
		super.configure(config);
		config.put("serverRows", true);
	}

	/**
	 * @return {@code null}: the columns are known by their index
	 */
	@Override
	protected String getDeclaredColumnId(IColumn<T, S> column)
	{
		return null;
	}

	@Override
	protected Component newHeader(String id, IColumn<T, S> column)
	{
		return column.getHeader(id);
	}

	/**
	 * @return the display model of an {@link IExportableColumn} or an {@link AbstractColumn},
	 *         else an empty text
	 */
	@Override
	protected IModel<String> getColumnTitle(IColumn<T, S> column)
	{
		if (column instanceof IExportableColumn<T, S> exportable)
		{
			return exportable.getDisplayModel();
		}
		if (column instanceof AbstractColumn<T, S> abstractColumn)
		{
			return abstractColumn.getDisplayModel();
		}
		return Model.of("");
	}

	@Override
	protected String getColumnCssClass(IColumn<T, S> column)
	{
		return column instanceof IStyledColumn<T, S> styled ? styled.getCssClass() : null;
	}

	/**
	 * @return whether the column is not an {@link IFixedWidthColumn}
	 */
	@Override
	protected boolean isColumnResizable(IColumn<T, S> column)
	{
		return column instanceof IFixedWidthColumn == false;
	}

	@Override
	protected Object getSortProperty(IColumn<T, S> column)
	{
		return column.isSortable() ? column.getSortProperty() : null;
	}

	@Override
	protected boolean isColumnExportable(IColumn<T, S> column)
	{
		return column instanceof IExportableColumn;
	}

	@Override
	protected Object getColumnExportValue(IColumn<T, S> column, T row)
	{
		IModel<?> value = ((IExportableColumn<T, S>)column).getDataModel(
			getProvider().model(row));
		try
		{
			return value != null ? value.getObject() : null;
		}
		finally
		{
			if (value != null)
			{
				value.detach();
			}
		}
	}
}
