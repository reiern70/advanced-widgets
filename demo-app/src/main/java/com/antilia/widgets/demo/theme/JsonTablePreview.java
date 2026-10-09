/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UsersProvider;
import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.DynamicColumn;
import com.antilia.widgets.dynamicdatatable.IDynamicColumn;
import com.antilia.widgets.dynamicdatatable.IHideableColumn;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.SelectionColumn;
import com.antilia.widgets.dynamicdatatable.SortableDynamicColumn;
import com.antilia.widgets.table.IMovableColumn;

/**
 * A table of users rendered in the browser from JSON, for the theme editor's preview: no progress
 * and no updates pushed over websockets, but the navigation on top with a filter and the rows per
 * page, columns that are dragged, resized and hidden, and the selection of many rows.
 */
class JsonTablePreview extends Panel
{
	private static final long serialVersionUID = 1L;

	JsonTablePreview(String id)
	{
		super(id);
		UsersProvider provider = new UsersProvider();
		List<IDynamicColumn<User>> columns = List.of(new SelectionColumn<User>().setId("selection"),
			new SortableDynamicColumn<>(column("firstName", "{{firstName}}"), "firstName"),
			new SortableDynamicColumn<>(column("lastName", "{{lastName}}"), "lastName"),
			column("city", "{{city}}"), column("country", "{{countryName}}"));
		add(PreviewTables.configure(new JsonBasedDynamicDataTable<>("table", columns, provider,
			PreviewTables.ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return PreviewTables.newNavigationToolbar(this, provider, id, position);
			}
		}));
	}

	private static DynamicColumn<User> column(String headerKey, String template)
	{
		DynamicColumn<User> column = new ValueColumn(headerKey, template);
		column.setId(headerKey);
		return column;
	}

	/**
	 * A value of the user, in a column the user can move and hide.
	 */
	private static class ValueColumn extends DynamicColumn<User>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ValueColumn(String headerKey, String template)
		{
			super(new ResourceModel(headerKey), template);
		}
	}
}
