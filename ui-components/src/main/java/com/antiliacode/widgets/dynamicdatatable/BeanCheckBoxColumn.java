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
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antiliacode.widgets.theme.Looks;

/**
 * A column of a {@link BeanBasedDynamicDataTable} with a checkbox per row bound to a boolean of
 * the row, changed by Ajax: «Active» on a rule, «Visible» on a product, «Done» on a task. It is
 * the counterpart of {@link CheckBoxColumn}.
 *
 * <pre>
 * new BeanCheckBoxColumn&lt;Rule, String&gt;(Model.of("Active"), "active")
 * {
 * 	&#64;Override
 * 	protected void onUpdate(Rule row, boolean value, AjaxRequestTarget target)
 * 	{
 * 		rules.setActive(row.id(), value);
 * 	}
 *
 * 	&#64;Override
 * 	public boolean isEnabled(Rule row)
 * 	{
 * 		return row.editable();
 * 	}
 * };
 * </pre>
 * <p>
 * The checkbox is drawn in the look of the table, checked as the row's value says. A change goes
 * as a row action of the table: checking it runs the action {@link #getActionId()}, unchecking it
 * the one with {@code -unchecked} after it. So {@link #onUpdate(Object, boolean, AjaxRequestTarget)}
 * gets the row looked up again by its key, and a change for a row that is gone reaches
 * {@link BaseDynamicDataTable#onNotFoundAction onNotFoundAction}. On a row it is not
 * {@link #isEnabled(Object) enabled} on, the checkbox is disabled, with the
 * {@link #getDisabledReason(Object) reason} as its tooltip, and the server refuses a change sent
 * for it anyway. With a {@link #setConfirmation(IModel) confirmation}, the change waits for the
 * user to confirm; cancelled, the table renders its rows again, the checkbox showing the value of
 * the row again.
 * <p>
 * Each checkbox is named for screen readers after the column's header and, with
 * {@link #setRowLabel(SerializableFunction)}, the row: «Active: Rule 5». A CSV export writes
 * «yes» or «no» in the language of the session; the column sorts once it has a
 * {@link #setSortProperty(Object) sort property}.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public abstract class BeanCheckBoxColumn<T, S> extends AbstractColumn<T, S>
	implements
		IExportableColumn<T, S>,
		IRowActionColumn<T>
{
	private static final long serialVersionUID = 1L;

	private final String actionId;

	private final SerializableFunction<? super T, Boolean> value;

	private final List<IAjaxActionColumnContributor<T>> actions;

	private S sortProperty;

	private SerializableFunction<? super T, String> rowLabel;

	private IModel<String> confirmation;

	/**
	 * @param header
	 *            the header text, rendered escaped, also the accessible name of the checkboxes
	 * @param property
	 *            the property expression of the boolean of a row, read as a
	 *            {@link PropertyResolver} reads it, also the action id of the column, unique
	 *            within its table
	 */
	public BeanCheckBoxColumn(IModel<String> header, String property)
	{
		this(header, Args.notEmpty(property, "property"), valueOf(property));
	}

	/**
	 * @param header
	 *            the header text, rendered escaped, also the accessible name of the checkboxes
	 * @param actionId
	 *            the action id of the column, unique within its table
	 * @param value
	 *            returns the boolean of a row, such as {@code Rule::isActive}
	 */
	public BeanCheckBoxColumn(IModel<String> header, String actionId,
		SerializableFunction<? super T, Boolean> value)
	{
		super(header);
		this.actionId = Args.notEmpty(actionId, "actionId");
		this.value = Args.notNull(value, "value");
		actions = List.of(new Action(true), new Action(false));
	}

	private static <T> SerializableFunction<T, Boolean> valueOf(String property)
	{
		return row -> (Boolean)PropertyResolver.getValue(property, row);
	}

	/**
	 * @return the id of the action checking a checkbox; unchecking it is the action with
	 *         {@code -unchecked} after it
	 */
	public String getActionId()
	{
		return actionId;
	}

	/**
	 * @param sortProperty
	 *            the property the column sorts the rows by, or {@code null} for a column that does
	 *            not sort them
	 * @return {@code this}
	 */
	public BeanCheckBoxColumn<T, S> setSortProperty(S sortProperty)
	{
		this.sortProperty = sortProperty;
		return this;
	}

	@Override
	public S getSortProperty()
	{
		return sortProperty;
	}

	/**
	 * @param rowLabel
	 *            returns the label of a row, such as its name, which with the header names its
	 *            checkbox for screen readers; {@code null} for the header alone
	 * @return {@code this}
	 */
	public BeanCheckBoxColumn<T, S> setRowLabel(SerializableFunction<? super T, String> rowLabel)
	{
		this.rowLabel = rowLabel;
		return this;
	}

	/**
	 * Asks the user to confirm a change before it is made.
	 *
	 * @param confirmation
	 *            the question to confirm, written escaped; {@code null} to change without asking
	 * @return {@code this}
	 */
	public BeanCheckBoxColumn<T, S> setConfirmation(IModel<String> confirmation)
	{
		this.confirmation = confirmation;
		return this;
	}

	/**
	 * Returns the question to confirm before a change, the one set with
	 * {@link #setConfirmation(IModel)} by default. Override to ask about the row and the value,
	 * for example only when a rule is turned off.
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
	 * value already; refresh the table to show the row's value again, for example when the change
	 * is refused.
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

	@SuppressWarnings("unchecked")
	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
		IModel<T> rowModel)
	{
		cellItem.add(new Label(componentId, () -> {
			BaseDynamicDataTable<T, ?, ?> table = cellItem.findParent(BaseDynamicDataTable.class);
			T row = rowModel.getObject();
			if (row == null)
			{
				return "";
			}
			String header = IconBasedRowAction.resolve(getDisplayModel(), table);
			String label = CheckBoxAction.label(header,
				rowLabel != null ? rowLabel.apply(row) : null);
			return CheckBoxAction.checkbox(actionId, Looks.of(table), label,
				Boolean.TRUE.equals(value.apply(row)) ? " checked" : "");
		}).setEscapeModelStrings(false));
	}

	/**
	 * @return «yes» or «no» in the language of the session, for example for a CSV export
	 */
	@Override
	public IModel<String> getDataModel(IModel<T> rowModel)
	{
		return rowModel.map(row -> CheckBoxAction.yesNo(value.apply(row)));
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
			super(actionId, getDisplayModel(), value);
		}

		@Override
		boolean enabled(T row)
		{
			return BeanCheckBoxColumn.this.isEnabled(row);
		}

		@Override
		IModel<String> reason(T row)
		{
			return BeanCheckBoxColumn.this.getDisabledReason(row);
		}

		@Override
		IModel<String> confirmation(T row, boolean value)
		{
			return BeanCheckBoxColumn.this.getConfirmation(row, value);
		}

		@Override
		void update(T row, boolean value, AjaxRequestTarget target)
		{
			BeanCheckBoxColumn.this.onUpdate(row, value, target);
		}
	}
}
