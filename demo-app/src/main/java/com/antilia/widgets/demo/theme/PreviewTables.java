/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.theme;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

import com.antilia.widgets.demo.datatable.User;
import com.antilia.widgets.demo.datatable.UserFilterPanel;
import com.antilia.widgets.demo.datatable.UsersProvider;
import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antilia.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antilia.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antilia.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antilia.widgets.table.MovableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior;

/**
 * What the two tables of the theme editor's preview share: the navigation on top with a filter
 * by name, the rows per page and the toolbar's actions, columns that are dragged to another place
 * and resized, and the selection of many rows with the bar selecting all of them.
 */
final class PreviewTables
{
	/** The number of rows a page shows at first. */
	static final int ROWS_PER_PAGE = 10;

	private PreviewTables()
	{
	}

	/**
	 * Sets the table up as the previews show it.
	 *
	 * @return the table
	 */
	static <C extends BaseDynamicDataTable<User, Long, ?>> C configure(C table)
	{
		table.setAdjustToParentWidth(true);
		table.setNavigationPosition(ToolbarPosition.TOP);
		table.setSelectionToolbarPosition(ToolbarPosition.TOP);
		table.setItemsPerPageOptions(10L, 25L, 50L);
		table.addToolbarAction(new ColumnChooserToolbarAction<>());
		table.addToolbarAction(new CsvExportToolbarAction<User, Long>().setFileName("users.csv"));
		table.add(new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH));
		table.add(new MovableColumnsBehavior());
		return table;
	}

	/**
	 * @return the navigation of a preview's table, with a filter by name in front of it
	 */
	static AbstractDynamicToolbar newNavigationToolbar(BaseDynamicDataTable<User, Long, ?> table,
		UsersProvider provider, String id, ToolbarPosition position)
	{
		return new DynamicNavigationToolbar<>(id, table, position)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPrefix(String id)
			{
				return new UserFilterPanel(id, new PropertyModel<>(provider, "filter"))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onFilter(AjaxRequestTarget target)
					{
						table.setCurrentPage(0);
						table.refresh(target);
					}
				};
			}
		};
	}
}
