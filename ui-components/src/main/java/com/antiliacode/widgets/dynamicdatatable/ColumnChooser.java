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

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookContainer;
import com.antiliacode.widgets.theme.LookPart;

class ColumnChooser<T, K extends Serializable, C extends IDetachable> extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<T, K, C> table;

	private final ColumnChooserToolbarAction<T, K> action;

	ColumnChooser(String id, BaseDynamicDataTable<T, K, C> table,
		ColumnChooserToolbarAction<T, K> action)
	{
		super(id);
		this.table = table;
		this.action = action;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		IModel<String> label = action.getLabel();
		IModel<String> assigned = label instanceof IComponentAssignedModel<String> resource
			? resource.wrapOnAssignment(this) : label;
		WebMarkupContainer toggle = new WebMarkupContainer("toggle");
		toggle.add(AttributeModifier.replace("title", assigned),
			AttributeModifier.replace("aria-label", assigned));
		toggle.add(new Label("icon", () -> action.getIcon().getMarkup()).setEscapeModelStrings(false)
			.setRenderBodyOnly(true));
		add(new LookContainer("chooser", LookPart.TABLE_COLUMN_CHOOSER));
		add(toggle.add(new LookBehavior(LookPart.TABLE_ACTION)));

		WebMarkupContainer menu = new WebMarkupContainer("menu");
		menu.add(AttributeModifier.replace("aria-label", assigned));
		menu.add(new LookBehavior(LookPart.TABLE_COLUMN_CHOOSER_MENU));
		menu.add(new LookContainer("apply", LookPart.TABLE_BUTTON, LookPart.TABLE_BUTTON_PRIMARY));
		add(menu);
		menu.add(new ListView<>("columns", this::hideableColumns)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<C> item)
			{
				C column = item.getModelObject();
				WebMarkupContainer button = new WebMarkupContainer("column");
				button.add(AttributeModifier.replace("data-dt-column",
					table.getColumns().indexOf(column)));
				button.add(AttributeModifier.replace("aria-pressed",
					String.valueOf(table.isColumnVisible(column))));
				button.add(new Label("label", table.getColumnTitle(column)));
				button.add(new LookBehavior(LookPart.TABLE_COLUMN_CHOOSER_COLUMN));
				button.add(new LookContainer("tick", LookPart.TABLE_COLUMN_CHOOSER_TICK));
				item.add(button);
			}
		});
	}

	private List<C> hideableColumns()
	{
		return table.getColumns().stream().filter(table::isHideable).toList();
	}
}
