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
package com.antiliacode.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.icon.IIcon;

/**
 * Shows how many rows are selected, such as "2 selected", in the navigation of the table, among
 * its {@link IDynamicToolbarAction actions}, with a button that deselects every row
 * ({@link BaseDynamicDataTable#clearSelection(org.apache.wicket.ajax.AjaxRequestTarget)}), a waste
 * bin unless {@link #setClearIcon(IIcon) another icon} is given. Nothing is shown while no row is
 * selected or the table selects none. The text is the {@code DynamicDataTable.selectedCount}
 * resource, with {@code ${count}} standing for the number, and the tooltip of the button the
 * {@code DynamicDataTable.clearSelection} resource; the count is re-rendered whenever the user
 * changes the selection.
 * <p>
 * The count can be turned off and on again without changing the actions of the table, with
 * {@link #setShown(boolean)}; the table has to be rendered again after that.
 *
 * <pre>
 * SelectionCountToolbarAction&lt;User, Long&gt; count = new SelectionCountToolbarAction&lt;&gt;();
 * table.addToolbarAction(count);
 * </pre>
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class SelectionCountToolbarAction<T, K extends Serializable>
	implements
		IDynamicToolbarAction<T, K>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the count. */
	public static final String CSS_CLASS = "ant-data-table-selection-count";

	private boolean shown = true;

	private IIcon clearIcon = IconBasedRowAction.Icon.DELETE;

	/**
	 * @return whether the count is shown
	 */
	public boolean isShown()
	{
		return shown;
	}

	/**
	 * @param shown
	 *            whether the count is shown
	 * @return this
	 */
	public SelectionCountToolbarAction<T, K> setShown(boolean shown)
	{
		this.shown = shown;
		return this;
	}

	/**
	 * @return the icon of the button next to the count that deselects every row, a waste bin
	 *         by default
	 */
	public IIcon getClearIcon()
	{
		return clearIcon;
	}

	/**
	 * @param clearIcon
	 *            the icon of the button next to the count that deselects every row
	 * @return this
	 */
	public SelectionCountToolbarAction<T, K> setClearIcon(IIcon clearIcon)
	{
		this.clearIcon = Args.notNull(clearIcon, "clearIcon");
		return this;
	}

	@Override
	public Component newComponent(String id, BaseDynamicDataTable<T, K, ?> table)
	{
		return new SelectionCount(id, table, this);
	}
}
