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

import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.icon.IIcon;

/**
 * A row action shown as an icon button, its tooltip naming what it does, optionally asking for a
 * confirmation before it acts. For an action better read as words, see {@link TextRowAction}.
 * <p>
 * The button carries the {@link #getActionId() action}, so a click is dispatched by the
 * {@link BaseDynamicDataTable} the action is registered with, through a {@link
 * CompoundDynamicColumn}, a {@link BeanActionsColumn} or {@link
 * BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}. The tooltip is written
 * escaped, as the button's {@code title} and {@code aria-label}.
 * <p>
 * With a {@link #setConfirmation(IModel) confirmation}, a click first shows the question with a
 * confirm and a cancel button, in the table's overlay, titled with the tooltip; see
 * {@link AbstractRowAction}. Override {@link #isVisible(Object)} to offer the action only on some
 * rows, or {@link #isEnabled(Object)} and {@link #getDisabledReason(Object)} to show it disabled on
 * the others, with the reason as its tooltip.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public abstract class IconBasedRowAction<T> extends AbstractRowAction<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The icons shipped with the row actions, drawn in the text color of the button. Any other
	 * {@link IIcon}, such as an {@link com.antiliacode.widgets.icon.SvgIcon},
	 * works as well.
	 */
	public enum Icon implements IIcon
	{
		/** A pencil. */
		EDIT("M4.5 19.5h4l11-11-4-4-11 11z M14 6l4 4"),

		/** A waste bin. */
		DELETE("M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6"),

		/** An arrow pointing down onto a line. */
		DOWNLOAD("M12 3v12 M7 10l5 5 5-5 M5 20h14"),

		/** A plus sign. */
		ADD("M12 5v14 M5 12h14");

		private final String path;

		Icon(String path)
		{
			this.path = path;
		}

		/**
		 * @return the icon as inline SVG markup
		 */
		@Override
		public String getMarkup()
		{
			return "<svg aria-hidden=\"true\" focusable=\"false\" viewBox=\"0 0 24 24\" " +
				"width=\"18\" height=\"18\" fill=\"none\" stroke=\"currentColor\" " +
				"stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"" +
				path + "\"/></svg>";
		}
	}

	private final IIcon icon;

	/**
	 * @param actionId
	 *            the action the button carries, unique within its table
	 * @param icon
	 *            the icon of the button
	 * @param tooltip
	 *            what the action does, shown as the button's tooltip and read by screen readers
	 */
	public IconBasedRowAction(String actionId, IIcon icon, IModel<String> tooltip)
	{
		super(actionId, tooltip);
		this.icon = Args.notNull(icon, "icon");
	}

	/**
	 * Returns the icon of the button, the one given to the constructor by default. Override to
	 * choose it when the template is rendered.
	 *
	 * @return the icon of the button
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	@Override
	public IconBasedRowAction<T> setConfirmation(IModel<String> confirmation)
	{
		super.setConfirmation(confirmation);
		return this;
	}

	/**
	 * Writes the button, with the tooltip escaped.
	 */
	@Override
	public String getTemplate()
	{
		return button(getActionId(), getIcon().getMarkup(), getName().getObject());
	}

	/**
	 * Like {@link #getTemplate()}, with a tooltip that is an {@link IComponentAssignedModel}, such
	 * as a {@link org.apache.wicket.model.ResourceModel}, resolved against the table.
	 */
	@Override
	public String getTemplate(BaseDynamicDataTable<T, ?, ?> table)
	{
		return button(getActionId(), getIcon().getMarkup(), resolve(getName(), table));
	}

	/**
	 * @return the markup of an icon button carrying the action, with the tooltip escaped
	 */
	static String button(String actionId, String iconMarkup, String tooltip)
	{
		String escaped = Strings.escapeMarkup(tooltip != null ? tooltip : "").toString();
		return "<button type=\"button\" class=\"ant-data-table-action " +
			"ant-data-table-icon-action\" data-dt-action=\"" + Strings.escapeMarkup(actionId) +
			"\" title=\"" + escaped + "\" aria-label=\"" + escaped + "\">" + iconMarkup +
			"</button>";
	}

	/**
	 * @return the object of the model, resolved against the table if it is an
	 *         {@link IComponentAssignedModel}
	 */
	static String resolve(IModel<String> model, BaseDynamicDataTable<?, ?, ?> table)
	{
		if (model instanceof IComponentAssignedModel<String> assigned)
		{
			IModel<String> wrapped = assigned.wrapOnAssignment(table);
			try
			{
				return wrapped.getObject();
			}
			finally
			{
				wrapped.detach();
			}
		}
		return model.getObject();
	}
}
