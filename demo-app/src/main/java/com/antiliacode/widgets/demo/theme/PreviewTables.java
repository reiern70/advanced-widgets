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
package com.antiliacode.widgets.demo.theme;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

import com.antiliacode.widgets.demo.datatable.User;
import com.antiliacode.widgets.demo.datatable.UserFilterPanel;
import com.antiliacode.widgets.demo.datatable.UsersProvider;
import com.antiliacode.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.ToolbarPosition;
import com.antiliacode.widgets.dynamicdatatable.ColumnChooserToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.CsvExportToolbarAction;
import com.antiliacode.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antiliacode.widgets.table.MovableColumnsBehavior;
import com.antiliacode.widgets.table.ResizableColumnsBehavior;

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
