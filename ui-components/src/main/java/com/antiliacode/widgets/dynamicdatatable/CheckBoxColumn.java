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

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.Looks;

/**
 * A column of a {@link JsonBasedDynamicDataTable} with a checkbox per row bound to a boolean of
 * the row, changed by Ajax: «Active» on a rule, «Visible» on a product, «Done» on a task. It is
 * the counterpart of {@link BeanCheckBoxColumn}, rendered in the browser from a template.
 *
 * <pre>
 * new CheckBoxColumn&lt;Rule&gt;(Model.of("Active"), "active", Rule::isActive)
 * {
 * 	&#64;Override
 * 	protected void onUpdate(Rule row, boolean value, AjaxRequestTarget target)
 * 	{
 * 		rules.setActive(row.id(), value);
 * 	}
 * };
 * </pre>
 * <p>
 * The checkbox is checked when the value at its path in the row's JSON is {@code true}. A change
 * goes as a row action of the table, under the path as action id: checking it runs that action,
 * unchecking it the one with {@code -unchecked} after it, so
 * {@link #onUpdate(Object, boolean, AjaxRequestTarget)} gets the row looked up again by its key
 * and a change for a row that is gone reaches
 * {@link BaseDynamicDataTable#onNotFoundAction onNotFoundAction}. On a row it is not
 * {@link #isEnabled(Object) enabled} on, the browser disables the checkbox, with the
 * {@link #getDisabledReason(Object) reason} as its tooltip, and the server refuses a change sent
 * for it anyway. With a {@link #setConfirmation(IModel) confirmation}, the change waits for the
 * user to confirm; cancelled, the table fetches its rows again.
 * <p>
 * Each checkbox is named for screen readers after the column's header and, with
 * {@link #setRowLabelPath(String)}, a value of the row's JSON: «Active: Rule 5». A CSV export
 * writes «yes» or «no» in the language of the session; the column sorts when wrapped in a
 * {@link SortableDynamicColumn}.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public abstract class CheckBoxColumn<T> extends AbstractDynamicColumn<T>
	implements
		IRowActionColumn<T>
{
	private static final long serialVersionUID = 1L;

	private final String path;

	private final List<IAjaxActionColumnContributor<T>> actions;

	private String rowLabelPath;

	private IModel<String> confirmation;

	/**
	 * @param header
	 *            the header text, rendered escaped, also the accessible name of the checkboxes
	 * @param path
	 *            the path of the boolean in the row's JSON, as the template engine reads it, for
	 *            example {@code active}; authored by the developer, also the action id of the
	 *            column, unique within its table
	 * @param value
	 *            returns the boolean of a row on the server, such as {@code Rule::isActive}, for
	 *            the CSV export
	 */
	public CheckBoxColumn(IModel<String> header, String path,
		SerializableFunction<? super T, Boolean> value)
	{
		super(header);
		this.path = Args.notEmpty(path, "path");
		Args.notNull(value, "value");
		setExportValue(row -> CheckBoxAction.yesNo(value.apply(row)));
		actions = List.of(new Action(true), new Action(false));
	}

	/**
	 * @return the id of the action checking a checkbox, its path; unchecking it is the action
	 *         with {@code -unchecked} after it
	 */
	public String getActionId()
	{
		return path;
	}

	/**
	 * @param rowLabelPath
	 *            the path of the label of a row in its JSON, such as its name, which with the
	 *            header names its checkbox for screen readers; authored by the developer;
	 *            {@code null} for the header alone
	 * @return {@code this}
	 */
	public CheckBoxColumn<T> setRowLabelPath(String rowLabelPath)
	{
		this.rowLabelPath = rowLabelPath;
		return this;
	}

	/**
	 * Asks the user to confirm a change before it is made.
	 *
	 * @param confirmation
	 *            the question to confirm, written escaped; {@code null} to change without asking
	 * @return {@code this}
	 */
	public CheckBoxColumn<T> setConfirmation(IModel<String> confirmation)
	{
		this.confirmation = confirmation;
		return this;
	}

	/**
	 * Returns the question to confirm before a change, the one set with
	 * {@link #setConfirmation(IModel)} by default. Override to ask about the row and the value.
	 *
	 * @param row
	 *            the row
	 * @param value
	 *            the value the user chose
	 * @return the question, or {@code null} to change without asking
	 */
	protected IModel<String> getConfirmation(T row, boolean value)
	{
		return confirmation;
	}

	/**
	 * @param row
	 *            the row, never {@code null}
	 * @return whether the user can change the row's value; every row by default
	 */
	public boolean isEnabled(T row)
	{
		return true;
	}

	/**
	 * @param row
	 *            a row the checkbox is not {@link #isEnabled(Object) enabled} on
	 * @return why the value cannot be changed, shown as the tooltip of the checkbox; {@code null}
	 *         by default
	 */
	public IModel<String> getDisabledReason(T row)
	{
		return null;
	}

	/**
	 * Called when the user changed the value of a row the checkbox is enabled on, once confirmed
	 * if there is a {@link #setConfirmation(IModel) confirmation}. The checkbox shows the new
	 * value already; push the row or refresh the table to show the row's value again, for example
	 * when the change is refused.
	 *
	 * @param row
	 *            the row, looked up again by its key, never {@code null}
	 * @param value
	 *            the value the user chose
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onUpdate(T row, boolean value, AjaxRequestTarget target);

	/**
	 * @return the actions checking and unchecking the checkboxes, which the table registers
	 */
	@Override
	public List<IAjaxActionColumnContributor<T>> getActionContributors()
	{
		return actions;
	}

	@Override
	public String getTemplate()
	{
		return template(getHeader().getObject(), Looks.current());
	}

	/**
	 * Returns the checkbox in the look of the table, named with a header that is an
	 * {@link org.apache.wicket.model.IComponentAssignedModel}, such as a
	 * {@link org.apache.wicket.model.ResourceModel}, resolved against the table.
	 */
	@Override
	public String getTemplate(JsonBasedDynamicDataTable<T, ?> table)
	{
		return template(IconBasedRowAction.resolve(getHeader(), table), Looks.of(table));
	}

	private String template(String header, ILook look)
	{
		String label = CheckBoxAction.label(header,
			rowLabelPath != null ? "{{" + rowLabelPath + "}}" : null);
		return CheckBoxAction.checkbox(path, look, label,
			" data-dt-checked=\"{{" + path + "}}\"");
	}

	@Override
	public void detach()
	{
		super.detach();
		if (confirmation != null)
		{
			confirmation.detach();
		}
	}

	/**
	 * The action checking or unchecking a checkbox of the column.
	 */
	private class Action extends CheckBoxAction<T>
	{
		private static final long serialVersionUID = 1L;

		Action(boolean value)
		{
			super(path, getHeader(), value);
		}

		@Override
		boolean enabled(T row)
		{
			return CheckBoxColumn.this.isEnabled(row);
		}

		@Override
		IModel<String> reason(T row)
		{
			return CheckBoxColumn.this.getDisabledReason(row);
		}

		@Override
		IModel<String> confirmation(T row, boolean value)
		{
			return CheckBoxColumn.this.getConfirmation(row, value);
		}

		@Override
		void update(T row, boolean value, AjaxRequestTarget target)
		{
			CheckBoxColumn.this.onUpdate(row, value, target);
		}
	}
}
