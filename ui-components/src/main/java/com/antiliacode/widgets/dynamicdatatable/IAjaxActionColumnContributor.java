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
import org.apache.wicket.model.IModel;

/**
 * A contributor whose template carries an action, handled on the server when it is clicked.
 * <p>
 * The template has to contain an element with a {@code data-dt-action} attribute whose value is
 * {@link #getActionId()}, for example {@code <a href="#" data-dt-action="select">Select</a>}. A
 * click on it is dispatched by the {@link BaseDynamicDataTable} the contributor is registered with,
 * either through a {@link CompoundDynamicColumn} or with
 * {@link BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}.
 * <p>
 * An action can apply to some rows only. On a row it is not {@link #isVisible(Object) visible}
 * on, it is left out; on a row it is visible but not {@link #isEnabled(Object) enabled} on, it is
 * shown disabled, with the {@link #getDisabledReason(Object) reason} as its tooltip. Either way
 * the table does not run it there: a request for it ends up in
 * {@link BaseDynamicDataTable#onNotFoundAction(AjaxRequestTarget, String, java.io.Serializable,
 * Object)}, as for an action that does not exist.
 * <p>
 * A {@link BeanBasedDynamicDataTable} leaves the action out of the row's cell on the server. A
 * {@link JsonBasedDynamicDataTable} renders the same template on every row, in the browser: it
 * sends the actions left out of a row and those disabled on it with the row, and the browser
 * removes the elements of the row carrying the first ones, those whose {@code data-dt-action}
 * attribute is the action, so that the template needs no condition. Rows served by a
 * {@link JsonRowsResource} carry the actions its {@link JsonRowsResource#getHiddenActions(Object)}
 * and {@link JsonRowsResource#getDisabledActions(Object)} name. In both tables, the browser marks
 * the elements carrying a disabled action with {@code aria-disabled="true"} and the CSS class
 * {@value BaseDynamicDataTable#DISABLED_ACTION_CSS_CLASS}, which greys them out, gives them the
 * reason as their {@code title} and {@code aria-description}, and ignores clicks on them. They
 * stay focusable, so that a keyboard or screen reader user learns why the action cannot be used.
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
	 * scheduled: an action that does not is left out of the row and not run on it. Every row by
	 * default.
	 *
	 * @param row
	 *            the row, never {@code null}
	 * @return whether the action is shown on the row
	 * @see #isEnabled(Object)
	 */
	default boolean isVisible(T row)
	{
		return true;
	}

	/**
	 * Whether the action can be used on a row it is {@link #isVisible(Object) visible} on, for
	 * example "Issue" not on an invoice already issued: an action that cannot is shown disabled,
	 * with the {@link #getDisabledReason(Object) reason}, and not run on the row. Useful where the
	 * action missing from the row would surprise the user. Every row by default.
	 *
	 * @param row
	 *            the row, never {@code null}
	 * @return whether the action can be used on the row
	 */
	default boolean isEnabled(T row)
	{
		return true;
	}

	/**
	 * Returns why the action cannot be used on a row it is not {@link #isEnabled(Object) enabled}
	 * on, for example "Already issued", shown as the tooltip of the disabled action and read by
	 * screen readers. An {@link org.apache.wicket.model.IComponentAssignedModel}, such as a
	 * {@link org.apache.wicket.model.ResourceModel}, is resolved against the table. {@code null}
	 * by default: the action keeps its own tooltip.
	 *
	 * @param row
	 *            the row, never {@code null}
	 * @return the reason, or {@code null}
	 */
	default IModel<String> getDisabledReason(T row)
	{
		return null;
	}

	/**
	 * Called when the action is clicked on a row that still exists and that the action is
	 * {@link #isVisible(Object) visible} and {@link #isEnabled(Object) enabled} on.
	 *
	 * @param row
	 *            the row the action was clicked on, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	void onAction(T row, AjaxRequestTarget target);
}
