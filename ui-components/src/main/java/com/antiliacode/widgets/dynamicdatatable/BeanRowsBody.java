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

import java.io.Serializable;
import java.util.Iterator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The rows of a {@link BeanBasedDynamicDataTable}, in its {@code <tbody>}: one {@code <tr>} per row
 * of the current page, carrying the attributes the table's script reads, and one {@code <td>} per
 * shown column, populated by the column.
 */
class BeanRowsBody<T, K extends Serializable, S> extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BeanBasedDynamicDataTable<T, K, S> table;

	BeanRowsBody(String id, BeanBasedDynamicDataTable<T, K, S> table)
	{
		super(id);
		this.table = table;

		add(new RefreshingView<T>("rows")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Iterator<IModel<T>> getItemModels()
			{
				return table.getCurrentPageRowList()
					.stream()
					.map(row -> table.getProvider().model(row))
					.iterator();
			}

			@Override
			protected void populateItem(Item<T> item)
			{
				populateRow(item);
			}
		});
	}

	private void populateRow(Item<T> item)
	{
		T row = item.getModelObject();
		K key = table.getProvider().keyOf(row);
		String formatted = table.getProvider().formatKey(key);
		long number = table.getFirstRowNumber() + item.getIndex();
		item.add(AttributeModifier.replace("id", table.getMarkupId() + "-row-" + formatted));
		item.add(AttributeModifier.replace("data-key", formatted));
		item.add(AttributeModifier.replace("data-number", number));
		item.add(new LookBehavior(
			number % 2 == 1 ? LookPart.TABLE_ROW_EVEN : LookPart.TABLE_ROW_ODD,
			table.isSelected(key) ? LookPart.TABLE_ROW_SELECTED : null));

		RepeatingView cells = new RepeatingView("cells");
		item.add(cells);
		int index = 0;
		for (IColumn<T, S> column : table.getVisibleColumns())
		{
			Item<ICellPopulator<T>> cell = new Item<>(cells.newChildId(), index++,
				Model.of(column));
			String cssClass = table.getColumnCssClass(column);
			if (cssClass != null)
			{
				cell.add(AttributeModifier.replace("class", cssClass));
			}
			cells.add(cell);
			column.populateItem(cell, "cell", item.getModel());
		}
		item.add(new WebMarkupContainer("filler").setVisible(table.hasFiller())
			.add(new LookBehavior(LookPart.TABLE_FILLER)));
	}
}
