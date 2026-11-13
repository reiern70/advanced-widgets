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

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.icon.IIcon;

/**
 * A row action shown as a button with a text, for an action better read as words than as an
 * icon: «Open», «Show only this one», «Resend», «Mark as paid». No tooltip is needed to tell what
 * it does, which touch screens do not show.
 *
 * <pre>
 * new TextRowAction&lt;Branch&gt;("only", Model.of("Show only this one"))
 * {
 * 	&#64;Override
 * 	protected void onRowAction(Branch row, AjaxRequestTarget target)
 * 	{
 * 		...
 * 	}
 * };
 * </pre>
 * <p>
 * It is an {@link AbstractRowAction}, like an {@link IconBasedRowAction}, with the same
 * {@link #isVisible(Object)}, {@link #isEnabled(Object)}, {@link #getDisabledReason(Object)} and
 * {@link #setConfirmation(IModel) confirmation}, and goes next to icon actions in a
 * {@link BeanActionsColumn} or a {@link CompoundDynamicColumn}. The text is written escaped, with
 * an optional {@link #setIcon(IIcon) icon} before it. {@link #setLinkStyle(boolean)} draws it as a
 * link rather than as a button. A disabled action is greyed out, with the reason as its tooltip.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public abstract class TextRowAction<T> extends AbstractRowAction<T>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the button of a text action. */
	public static final String CSS_CLASS = "ant-data-table-text-action";

	/** The CSS class of a text action {@link #setLinkStyle(boolean) drawn as a link}. */
	public static final String LINK_CSS_CLASS = "ant-data-table-link-action";

	private IIcon icon;

	private boolean linkStyle;

	/**
	 * @param actionId
	 *            the action the button carries, unique within its table
	 * @param text
	 *            the text of the button, what the action does, written escaped
	 */
	public TextRowAction(String actionId, IModel<String> text)
	{
		super(actionId, text);
	}

	/**
	 * @return the icon before the text, or {@code null}
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon before the text, or {@code null} for none
	 * @return {@code this}
	 */
	public TextRowAction<T> setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @return whether the action is drawn as a link rather than as a button
	 */
	public boolean isLinkStyle()
	{
		return linkStyle;
	}

	/**
	 * @param linkStyle
	 *            whether the action is drawn as a link, underlined and without a border, rather
	 *            than as a button; it stays a button for the keyboard and screen readers
	 * @return {@code this}
	 */
	public TextRowAction<T> setLinkStyle(boolean linkStyle)
	{
		this.linkStyle = linkStyle;
		return this;
	}

	@Override
	public TextRowAction<T> setConfirmation(IModel<String> confirmation)
	{
		super.setConfirmation(confirmation);
		return this;
	}

	/**
	 * Writes the button, with the text escaped.
	 */
	@Override
	public String getTemplate()
	{
		return button(getName().getObject());
	}

	/**
	 * Like {@link #getTemplate()}, with a text that is an
	 * {@link org.apache.wicket.model.IComponentAssignedModel}, such as a
	 * {@link org.apache.wicket.model.ResourceModel}, resolved against the table.
	 */
	@Override
	public String getTemplate(BaseDynamicDataTable<T, ?, ?> table)
	{
		return button(IconBasedRowAction.resolve(getName(), table));
	}

	private String button(String text)
	{
		IIcon before = getIcon();
		return "<button type=\"button\" class=\"ant-data-table-action " + CSS_CLASS +
			(linkStyle ? " " + LINK_CSS_CLASS : "") + "\" data-dt-action=\"" +
			Strings.escapeMarkup(getActionId()) + "\">" +
			(before != null ? before.getMarkup() : "") + "<span>" +
			Strings.escapeMarkup(text != null ? text : "") + "</span></button>";
	}
}
