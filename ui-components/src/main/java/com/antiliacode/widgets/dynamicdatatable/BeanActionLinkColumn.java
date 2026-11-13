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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableFunction;

/**
 * A column of a {@link BeanBasedDynamicDataTable} showing a value of the row as a link that runs a
 * row action: a name that opens the row, a branch that filters by it, a status that opens its
 * history. It is the counterpart of {@link ActionLinkColumn}.
 *
 * <pre>
 * new BeanActionLinkColumn&lt;Branch, String&gt;(Model.of("Branch"), Branch::name, "only")
 * 	.setSortProperty("name");
 * </pre>
 * <p>
 * The cell is the value, converted by the table's converters and written escaped, in a
 * link-styled button carrying the action. A click goes through the table's
 * {@link BaseDynamicDataTable#onAction onAction} as for any row action: the row is resolved again
 * from its key, and the action runs only if it is still
 * {@link IAjaxActionColumnContributor#isVisible(Object) visible} and
 * {@link IAjaxActionColumnContributor#isEnabled(Object) enabled} on the row; else the request
 * reaches {@link BaseDynamicDataTable#onNotFoundAction onNotFoundAction}. On a row the action is
 * hidden or disabled on, the cell shows the value as plain text.
 * <p>
 * The action is registered with the table by another column, for example as a
 * {@link TextRowAction} of a {@link BeanActionsColumn}, or by this one when it is given the action
 * itself. A CSV export writes the value, not the link; the column sorts once it has a
 * {@link #setSortProperty(Object) sort property}.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanActionLinkColumn<T, S> extends AbstractColumn<T, S>
	implements
		IExportableColumn<T, S>,
		IRowActionColumn<T>
{
	private static final long serialVersionUID = 1L;

	private final SerializableFunction<? super T, ?> value;

	private final String actionId;

	private final IAjaxActionColumnContributor<T> action;

	private S sortProperty;

	/**
	 * A column running an action registered with the table otherwise.
	 *
	 * @param header
	 *            the header text, rendered escaped
	 * @param value
	 *            returns the value of a row shown as the link, such as {@code Branch::name}
	 * @param actionId
	 *            the id of the row action the link runs
	 */
	public BeanActionLinkColumn(IModel<String> header, SerializableFunction<? super T, ?> value,
		String actionId)
	{
		this(header, value, Args.notEmpty(actionId, "actionId"), null);
	}

	/**
	 * A column running an action the table registers with the column.
	 *
	 * @param header
	 *            the header text, rendered escaped
	 * @param value
	 *            returns the value of a row shown as the link, such as {@code Branch::name}
	 * @param action
	 *            the row action the link runs
	 */
	public BeanActionLinkColumn(IModel<String> header, SerializableFunction<? super T, ?> value,
		IAjaxActionColumnContributor<T> action)
	{
		this(header, value, Args.notNull(action, "action").getActionId(), action);
	}

	private BeanActionLinkColumn(IModel<String> header, SerializableFunction<? super T, ?> value,
		String actionId, IAjaxActionColumnContributor<T> action)
	{
		super(header);
		this.value = Args.notNull(value, "value");
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
	 * @param sortProperty
	 *            the property the column sorts the rows by, or {@code null} for a column that does
	 *            not sort them
	 * @return {@code this}
	 */
	public BeanActionLinkColumn<T, S> setSortProperty(S sortProperty)
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
	 * @return the action given to the constructor, which the table registers; none for an action
	 *         registered otherwise
	 */
	@Override
	public List<IAjaxActionColumnContributor<T>> getActionContributors()
	{
		return action != null ? List.of(action) : List.of();
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
			String text = Strings.escapeMarkup(format(table, value.apply(row))).toString();
			IAjaxActionColumnContributor<T> contributor = table.getActionContributor(actionId);
			return contributor != null && BaseDynamicDataTable.isAvailable(contributor, row)
				? ActionLinkColumn.link(actionId, text) : text;
		}).setEscapeModelStrings(false));
	}

	@SuppressWarnings("unchecked")
	private static String format(BaseDynamicDataTable<?, ?, ?> table, Object value)
	{
		if (value == null)
		{
			return "";
		}
		IConverter<Object> converter = (IConverter<Object>)table.getConverter(value.getClass());
		return converter.convertToString(value, table.getLocale());
	}

	/**
	 * @return the value of the row, for example for a CSV export
	 */
	@Override
	public IModel<?> getDataModel(IModel<T> rowModel)
	{
		return rowModel.map(value);
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
