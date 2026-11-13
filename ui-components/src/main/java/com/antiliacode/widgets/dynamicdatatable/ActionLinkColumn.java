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

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;

/**
 * A column of a {@link JsonBasedDynamicDataTable} showing a value of the row as a link that runs a
 * row action: a name that opens the row, a branch that filters by it, a status that opens its
 * history. It is the counterpart of {@link BeanActionLinkColumn}.
 *
 * <pre>
 * new ActionLinkColumn&lt;Branch&gt;(Model.of("Branch"), "name", "only")
 * 	.setExportValue(Branch::name);
 * </pre>
 * <p>
 * The cell is the value at a path of the row's JSON, written escaped by the template engine, in a
 * link-styled button carrying the action. A click goes through the table's
 * {@link BaseDynamicDataTable#onAction onAction} as for any row action: the row is resolved again
 * from its key, and the action runs only if it is still {@link IAjaxActionColumnContributor#isVisible(Object)
 * visible} and {@link IAjaxActionColumnContributor#isEnabled(Object) enabled} on the row; else
 * the request reaches {@link BaseDynamicDataTable#onNotFoundAction onNotFoundAction}. On a row the
 * action is hidden or disabled on, the browser shows the value as plain text.
 * <p>
 * The action is registered with the table by another column, for example as a
 * {@link TextRowAction} of a {@link CompoundDynamicColumn}, or by this one when it is given the
 * action itself. The column is exported by its {@link #setExportValue export value}, the value
 * rather than the link, and sorts once wrapped in a {@link SortableDynamicColumn}.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public class ActionLinkColumn<T> extends AbstractDynamicColumn<T> implements IRowActionColumn<T>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the link of a cell. */
	public static final String CSS_CLASS = "ant-data-table-action-link";

	/**
	 * The attribute telling the browser to show the link's text as plain text on a row its action
	 * is hidden or disabled on, rather than to remove or disable the link.
	 */
	public static final String UNAVAILABLE_ATTRIBUTE = "data-dt-unavailable";

	private final String path;

	private final String actionId;

	private final IAjaxActionColumnContributor<T> action;

	/**
	 * A column running an action registered with the table otherwise.
	 *
	 * @param header
	 *            the header text, rendered escaped
	 * @param path
	 *            the path of the value in the row's JSON, as the template engine reads it, for
	 *            example {@code name}; authored by the developer
	 * @param actionId
	 *            the id of the row action the link runs
	 */
	public ActionLinkColumn(IModel<String> header, String path, String actionId)
	{
		this(header, path, Args.notEmpty(actionId, "actionId"), null);
	}

	/**
	 * A column running an action the table registers with the column.
	 *
	 * @param header
	 *            the header text, rendered escaped
	 * @param path
	 *            the path of the value in the row's JSON, as the template engine reads it, for
	 *            example {@code name}; authored by the developer
	 * @param action
	 *            the row action the link runs
	 */
	public ActionLinkColumn(IModel<String> header, String path,
		IAjaxActionColumnContributor<T> action)
	{
		this(header, path, Args.notNull(action, "action").getActionId(), action);
	}

	private ActionLinkColumn(IModel<String> header, String path, String actionId,
		IAjaxActionColumnContributor<T> action)
	{
		super(header);
		this.path = Args.notEmpty(path, "path");
		this.actionId = actionId;
		this.action = action;
	}

	/**
	 * @return the id of the row action the link runs
	 */
	public String getActionId()
	{
		return actionId;
	}

	/**
	 * @return the action given to the constructor, which the table registers; none for an action
	 *         registered otherwise
	 */
	@Override
	public List<IAjaxActionColumnContributor<T>> getActionContributors()
	{
		return action != null ? List.of(action) : List.of();
	}

	@Override
	public String getTemplate()
	{
		return link(actionId, "{{" + path + "}}");
	}

	/**
	 * @param actionId
	 *            the action
	 * @param text
	 *            the text of the link, escaped or a placeholder writing it escaped
	 * @return the link-styled button carrying the action
	 */
	static String link(String actionId, String text)
	{
		return "<button type=\"button\" class=\"" + CSS_CLASS + "\" data-dt-action=\"" +
			Strings.escapeMarkup(actionId) + "\" " + UNAVAILABLE_ATTRIBUTE + "=\"text\">" + text +
			"</button>";
	}

	@Override
	public void detach()
	{
		super.detach();
		if (action != null)
		{
			action.detach();
		}
	}
}
