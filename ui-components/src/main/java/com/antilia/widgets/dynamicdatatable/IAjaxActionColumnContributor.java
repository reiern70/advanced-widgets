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
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A contributor whose template carries an action, handled on the server when it is clicked.
 * <p>
 * The template has to contain an element with a {@code data-dt-action} attribute whose value is
 * {@link #getActionId()}, for example {@code <a href="#" data-dt-action="select">Select</a>}. A
 * click on it is dispatched by the {@link BaseDynamicDataTable} the contributor is registered with,
 * either through a {@link CompoundDynamicColumn} or with
 * {@link BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public interface IAjaxActionColumnContributor<T> extends IDynamicColumnContributor<T>
{
	/**
	 * @return the value of the {@code data-dt-action} attribute this contributor handles, unique
	 *         within its table
	 */
	String getActionId();

	/**
	 * Called once when the contributor is registered with a table, for example to add the
	 * behaviors it needs to the table. Does nothing by default.
	 *
	 * @param table
	 *            the table dispatching the contributor's action
	 */
	default void bind(BaseDynamicDataTable<T, ?, ?> table)
	{
	}

	/**
	 * Called when the action is clicked on a row that still exists.
	 *
	 * @param row
	 *            the row the action was clicked on, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	void onAction(T row, AjaxRequestTarget target);
}
