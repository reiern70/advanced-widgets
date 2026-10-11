/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;

/**
 * The same table as {@link DynamicDataTablePage}, with its rows rendered on the server by Wicket
 * components: a {@link BeanBasedDynamicDataTable} with Wicket's {@link IColumn}s, see
 * {@link BeanUsersTablePanel}.
 */
public class BeanDynamicDataTablePage extends DynamicTableExamplePage
{
	private static final long serialVersionUID = 1L;

	@Override
	protected UsersTablePanel newUsersTable(String id)
	{
		return new BeanUsersTablePanel(id, this);
	}
}
