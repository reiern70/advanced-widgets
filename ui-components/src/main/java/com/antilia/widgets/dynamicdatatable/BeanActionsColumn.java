/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.dynamicdatatable;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

/**
 * The column of a {@link BeanBasedDynamicDataTable} with row actions, the counterpart of a
 * {@link CompoundDynamicColumn} of {@link IAjaxActionColumnContributor}s such as
 * {@link IconBasedRowAction}s: every cell shows the markup of each contributor, one after the
 * other. The table registers the contributors and dispatches their clicks to them, by the row's
 * key.
 * <p>
 * The markup of a contributor is written into the cell as it is, without the row's data: a
 * contributor's {@link IDynamicColumnContributor#getTemplate(BaseDynamicDataTable) template} has to
 * be trusted markup, authored by the developer, that refers to no property of the row.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanActionsColumn<T, S> extends AbstractColumn<T, S> implements IFixedWidthColumn
{
	private static final long serialVersionUID = 1L;

	private final List<IAjaxActionColumnContributor<T>> contributors;

	/**
	 * @param header
	 *            the header text, rendered escaped
	 * @param contributors
	 *            the actions, in display order
	 */
	public BeanActionsColumn(IModel<String> header,
		List<? extends IAjaxActionColumnContributor<T>> contributors)
	{
		super(header);
		this.contributors = List.copyOf(Args.notEmpty(contributors, "contributors"));
	}

	/**
	 * @return the actions, in display order
	 */
	public List<IAjaxActionColumnContributor<T>> getContributors()
	{
		return contributors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
		IModel<T> rowModel)
	{
		cellItem.add(new Label(componentId, () -> {
			BaseDynamicDataTable<T, ?, ?> table = cellItem.findParent(BaseDynamicDataTable.class);
			return contributors.stream()
				.map(contributor -> contributor.getTemplate(table))
				.collect(Collectors.joining());
		}).setEscapeModelStrings(false));
	}

	@Override
	public void detach()
	{
		super.detach();
		contributors.forEach(IAjaxActionColumnContributor::detach);
	}
}
