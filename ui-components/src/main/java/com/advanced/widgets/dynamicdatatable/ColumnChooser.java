/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

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
		add(toggle);

		WebMarkupContainer menu = new WebMarkupContainer("menu");
		menu.add(AttributeModifier.replace("aria-label", assigned));
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
				item.add(button);
			}
		});
	}

	private List<C> hideableColumns()
	{
		return table.getColumns().stream().filter(table::isHideable).toList();
	}
}
