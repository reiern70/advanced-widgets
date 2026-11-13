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

import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;

import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.LookPart;

/**
 * One of the two row actions of a checkbox of {@link BeanCheckBoxColumn} and
 * {@link CheckBoxColumn}: checking it, under the column's action id, or unchecking it, under the
 * id with {@link #UNCHECKED_SUFFIX}. The column decides whether it is enabled, the confirmation
 * and what a change does; a confirmation cancelled renders the rows again, so that the checkbox
 * shows the value of the row again.
 *
 * @param <T>
 *            the type of the rows
 */
abstract class CheckBoxAction<T> extends AbstractRowAction<T>
{
	private static final long serialVersionUID = 1L;

	/** What the id of the action unchecking a checkbox ends with. */
	static final String UNCHECKED_SUFFIX = "-unchecked";

	private final boolean value;

	/**
	 * @param actionId
	 *            the action id of the column
	 * @param name
	 *            the header of the column, the title of a confirmation
	 * @param value
	 *            whether the action checks the checkbox
	 */
	CheckBoxAction(String actionId, IModel<String> name, boolean value)
	{
		super(value ? actionId : actionId + UNCHECKED_SUFFIX, name);
		this.value = value;
	}

	/**
	 * Writes nothing: the column writes the checkbox carrying both actions.
	 */
	@Override
	public String getTemplate()
	{
		return "";
	}

	@Override
	public boolean isEnabled(T row)
	{
		return enabled(row);
	}

	@Override
	public IModel<String> getDisabledReason(T row)
	{
		return reason(row);
	}

	@Override
	protected IModel<String> getConfirmation(T row)
	{
		return confirmation(row, value);
	}

	@Override
	protected void onRowAction(T row, AjaxRequestTarget target)
	{
		update(row, value, target);
	}

	/**
	 * Renders the rows again, the checkbox with the value of its row.
	 */
	@Override
	protected void onCancel(AjaxRequestTarget target)
	{
		getTable().refresh(target);
	}

	abstract boolean enabled(T row);

	abstract IModel<String> reason(T row);

	abstract IModel<String> confirmation(T row, boolean value);

	abstract void update(T row, boolean value, AjaxRequestTarget target);

	/**
	 * @param actionId
	 *            the action id of the column
	 * @param look
	 *            the look of the table
	 * @param label
	 *            the accessible name of the checkbox, escaped, or a template writing it
	 * @param state
	 *            {@code " checked"}, nothing, or the attribute telling the browser the state
	 * @return the checkbox carrying the actions checking and unchecking it
	 */
	static String checkbox(String actionId, ILook look, String label, String state)
	{
		String id = Strings.escapeMarkup(actionId).toString();
		return "<input type=\"checkbox\" class=\"" + look.getPartClasses(LookPart.TABLE_CHECK) +
			"\" data-dt-action=\"" + id + "\" data-dt-action-unchecked=\"" + id +
			UNCHECKED_SUFFIX + "\" aria-label=\"" + label + "\"" + state + ">";
	}

	/**
	 * @param column
	 *            the header of the column
	 * @param row
	 *            the label of the row, or {@code null}
	 * @return the accessible name of a checkbox, escaped: the header, and the label of the row
	 */
	static String label(String column, String row)
	{
		String text = column != null ? column : "";
		if (row != null)
		{
			String pattern = Application.get().getResourceSettings().getLocalizer()
				.getString("DynamicDataTable.checkLabel", null, "${column}: ${row}");
			text = new MapVariableInterpolator(pattern, Map.of("column", text, "row", row))
				.toString();
		}
		return Strings.escapeMarkup(text).toString();
	}

	/**
	 * @param value
	 *            a value of a row
	 * @return the value as exported: «yes» or «no» in the language of the session, nothing for
	 *         {@code null}
	 */
	static String yesNo(Boolean value)
	{
		if (value == null)
		{
			return null;
		}
		return Application.get().getResourceSettings().getLocalizer().getString(
			value ? "DynamicDataTable.yes" : "DynamicDataTable.no", null, value ? "Yes" : "No");
	}
}
