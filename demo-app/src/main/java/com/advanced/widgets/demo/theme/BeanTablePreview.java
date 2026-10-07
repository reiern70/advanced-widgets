/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.advanced.widgets.demo.datatable.User;
import com.advanced.widgets.demo.datatable.UsersProvider;
import com.advanced.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.advanced.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BeanSelectionColumn;
import com.advanced.widgets.dynamicdatatable.IHideableColumn;
import com.advanced.widgets.table.IMovableColumn;

/**
 * A table of users rendered on the server by Wicket components, for the theme editor's preview: no
 * progress and no updates pushed over websockets, but the navigation on top with a filter and the
 * rows per page, columns that are dragged, resized and hidden, and the selection of many rows.
 */
class BeanTablePreview extends Panel
{
	private static final long serialVersionUID = 1L;

	BeanTablePreview(String id)
	{
		super(id);
		UsersProvider provider = new UsersProvider();
		List<IColumn<User, String>> columns = List.of(new BeanSelectionColumn<>(),
			new UserColumn("firstName", "firstName", User::getFirstName),
			new UserColumn("lastName", "lastName", User::getLastName),
			new UserColumn("city", null, User::getCity),
			new UserColumn("country", null, User::getCountryName));
		add(PreviewTables.configure(new BeanBasedDynamicDataTable<>("table", columns, provider,
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

	/**
	 * A value of the user, in a column the user can move and hide.
	 */
	private static class UserColumn extends LambdaColumn<User, String>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		UserColumn(String headerKey, String sortProperty, SerializableFunction<User, ?> value)
		{
			super(new ResourceModel(headerKey), sortProperty, value);
		}
	}
}
