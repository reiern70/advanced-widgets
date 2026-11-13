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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antiliacode.widgets.floating.FloatingPanel;

/**
 * The base of the row actions shown as a button, {@link IconBasedRowAction} and
 * {@link TextRowAction}: an action id, the name of what the action does, and an optional
 * confirmation asked before the action is carried out.
 * <p>
 * The button carries the {@link #getActionId() action}, so a click is dispatched by the
 * {@link BaseDynamicDataTable} the action is registered with, through a {@link
 * CompoundDynamicColumn}, a {@link BeanActionsColumn} or {@link
 * BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}.
 * <p>
 * With a {@link #setConfirmation(IModel) confirmation}, a click first shows the question with a
 * confirm and a cancel button, in a {@link FloatingPanel} titled with the name of the action, in
 * the table's {@link BaseDynamicDataTable#showOverlay(org.apache.wicket.Component,
 * org.apache.wicket.core.request.handler.IPartialPageRequestHandler) overlay}, which blocks the
 * table until the user answers. {@link #onRowAction(Object, AjaxRequestTarget)} is called only when
 * the user confirms, the row still exists and the action is still {@link #isVisible(Object)
 * visible} and {@link #isEnabled(Object) enabled} on it.
 * <p>
 * Override {@link #isVisible(Object)} to offer the action only on some rows, for example "Cancel"
 * only on a run still scheduled, or {@link #isEnabled(Object)} and
 * {@link #getDisabledReason(Object)} to show it disabled on the others, with the reason as its
 * tooltip.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public abstract class AbstractRowAction<T> implements IAjaxActionColumnContributor<T>
{
	private static final long serialVersionUID = 1L;

	private final String actionId;

	private final IModel<String> name;

	private IModel<String> confirmation;

	private BaseDynamicDataTable<T, ?, ?> table;

	/**
	 * @param actionId
	 *            the action the button carries, unique within its table
	 * @param name
	 *            what the action does: the tooltip of an icon, the text of a text button, and the
	 *            title of the confirmation
	 */
	protected AbstractRowAction(String actionId, IModel<String> name)
	{
		this.actionId = Args.notEmpty(actionId, "actionId");
		this.name = Args.notNull(name, "name");
	}

	@Override
	public String getActionId()
	{
		return actionId;
	}

	/**
	 * @return what the action does
	 */
	public IModel<String> getName()
	{
		return name;
	}

	/**
	 * Asks the user to confirm before the action is carried out, for example before a row is
	 * deleted.
	 *
	 * @param confirmation
	 *            the question to confirm, written escaped; {@code null} to act without asking
	 * @return {@code this}
	 */
	public AbstractRowAction<T> setConfirmation(IModel<String> confirmation)
	{
		this.confirmation = confirmation;
		return this;
	}

	/**
	 * Returns the question to confirm before acting on a row, the one set with
	 * {@link #setConfirmation(IModel)} by default. Override to ask about the row itself.
	 *
	 * @param row
	 *            the row the action was clicked on
	 * @return the question, or {@code null} to act without asking
	 */
	protected IModel<String> getConfirmation(T row)
	{
		return confirmation;
	}

	@Override
	public void bind(BaseDynamicDataTable<T, ?, ?> table)
	{
		this.table = table;
	}

	/**
	 * @return the table the action is registered with, or {@code null} before it is registered
	 */
	protected BaseDynamicDataTable<T, ?, ?> getTable()
	{
		return table;
	}

	/**
	 * Carries out the action, or asks for a confirmation first if there is one.
	 */
	@Override
	public final void onAction(T row, AjaxRequestTarget target)
	{
		IModel<String> question = getConfirmation(row);
		if (question == null)
		{
			onRowAction(row, target);
		}
		else
		{
			String key = table.getRowKey(row);
			table.showOverlay(new FloatingPanel(BaseDynamicDataTable.OVERLAY_CONTENT_ID, name)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected Component newBody(String id)
				{
					return new RowActionConfirmationPanel<>(id, question, key,
						AbstractRowAction.this);
				}

				@Override
				protected void onClose(AjaxRequestTarget target)
				{
					table.closeOverlay(target);
					onCancel(target);
				}
			}, target);
		}
	}

	/**
	 * Carries out the action on a row: right away, or once the user confirmed it.
	 *
	 * @param row
	 *            the row, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onRowAction(T row, AjaxRequestTarget target);

	/**
	 * Called when the user answers the {@link #getConfirmation(Object) confirmation} with cancel,
	 * or closes it: the action is not carried out. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onCancel(AjaxRequestTarget target)
	{
	}

	@Override
	public void detach()
	{
		name.detach();
		if (confirmation != null)
		{
			confirmation.detach();
		}
	}
}
