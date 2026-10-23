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
package com.antiliacode.widgets.dynamicdatatable;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * The column of a {@link BeanBasedDynamicDataTable} numbering its rows across pages, the
 * counterpart of {@link RowNumberColumn}.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanRowNumberColumn<T, S> extends AbstractColumn<T, S> implements IFixedWidthColumn
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param header
	 *            the header text, rendered escaped
	 */
	public BeanRowNumberColumn(IModel<String> header)
	{
		super(header);
	}

	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
		IModel<T> rowModel)
	{
		cellItem.add(new Label(componentId, () -> {
			Item<?> row = cellItem.findParent(Item.class);
			BaseDynamicDataTable<?, ?, ?> table = cellItem.findParent(BaseDynamicDataTable.class);
			return table.getFirstRowNumber() + row.getIndex();
		}));
	}
}
