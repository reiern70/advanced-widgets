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
