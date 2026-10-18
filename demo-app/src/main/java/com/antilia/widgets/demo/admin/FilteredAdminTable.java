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
package com.antilia.widgets.demo.admin;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.DynamicNavigationToolbar;
import com.antilia.widgets.dynamicdatatable.IDynamicDataProvider;

/**
 * A table of the administration with an {@link AdminFilterPanel} in front of its navigation; as
 * the filter changes, the table goes back to its first page and shows the rows let through.
 *
 * @param <T>
 *            the type of the rows
 */
abstract class FilteredAdminTable<T> extends BeanBasedDynamicDataTable<T, Long, String>
{
	private static final long serialVersionUID = 1L;

	FilteredAdminTable(String id, List<? extends IColumn<T, String>> columns,
		IDynamicDataProvider<T, Long> provider)
	{
		super(id, columns, provider, AdminPage.ROWS_PER_PAGE);
	}

	@Override
	protected AbstractDynamicToolbar newNavigationToolbar(String id, ToolbarPosition position)
	{
		return new DynamicNavigationToolbar<>(id, this, position)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newPrefix(String id)
			{
				return newFilter(id);
			}
		};
	}

	/**
	 * @param id
	 *            the id of the filter
	 * @return the filter, which calls {@link #filtered(AjaxRequestTarget)} as it changes
	 */
	protected abstract AdminFilterPanel newFilter(String id);

	/**
	 * Shows the first page of the rows the filter lets through.
	 */
	protected void filtered(AjaxRequestTarget target)
	{
		setCurrentPage(0);
		refresh(target);
	}
}
