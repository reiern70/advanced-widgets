/*
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
	 * Whether the action applies to the row, for example "Cancel" only to an item still
	 * scheduled: an action that does not is neither rendered in the row's cell of a
	 * {@link BeanActionsColumn} nor run when the browser asks for it on the row, which ends up in
	 * {@link BaseDynamicDataTable#onNotFoundAction(AjaxRequestTarget, String, java.io.Serializable,
	 * Object)} instead. Every row by default.
	 * <p>
	 * A {@link JsonBasedDynamicDataTable} renders the same template on every row, in the browser:
	 * it sends the actions that do not apply to a row with the row, and the browser gives the
	 * elements of the row carrying them, those whose {@code data-dt-action} attribute is the
	 * action, the CSS class {@value BaseDynamicDataTable#HIDDEN_ACTION_CSS_CLASS}, which hides
	 * them. The template needs no condition. Rows served by a {@link JsonRowsResource} carry the
	 * actions its {@link JsonRowsResource#getHiddenActions(Object)} names.
	 *
	 * @param row
	 *            the row, never {@code null}
	 * @return whether the action applies to the row
	 */
	default boolean isVisible(T row)
	{
		return true;
	}

	/**
	 * Called when the action is clicked on a row that still exists and that the action
	 * {@link #isVisible(Object) applies to}.
	 *
	 * @param row
	 *            the row the action was clicked on, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	void onAction(T row, AjaxRequestTarget target);
}
