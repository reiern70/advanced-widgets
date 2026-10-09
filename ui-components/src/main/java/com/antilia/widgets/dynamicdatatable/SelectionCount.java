/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * The count of a {@link SelectionCountToolbarAction}, "2 selected" and a button deselecting every
 * row, which the table re-renders when the selection changes.
 */
class SelectionCount extends Panel implements ISelectionView
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<?, ?, ?> table;

	private final SelectionCountToolbarAction<?, ?> action;

	SelectionCount(String id, BaseDynamicDataTable<?, ?, ?> table,
		SelectionCountToolbarAction<?, ?> action)
	{
		super(id);
		this.table = table;
		this.action = action;
		setOutputMarkupPlaceholderTag(true);

		add(new Label("count", () -> getString("DynamicDataTable.selectedCount",
			Model.ofMap(Map.of("count", table.getSelection().size())), "${count} selected")));
		IModel<String> tooltip = new ResourceModel("DynamicDataTable.clearSelection",
			"Clear selection");
		AjaxLink<Void> clear = new AjaxLink<>("clear")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				table.clearSelection(target);
			}
		};
		clear.add(AttributeModifier.replace("title", tooltip),
			AttributeModifier.replace("aria-label", tooltip));
		clear.add(new Label("icon", () -> action.getClearIcon().getMarkup())
			.setEscapeModelStrings(false).setRenderBodyOnly(true));
		add(clear);
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(action.isShown() && table.getSelectionMode() != SelectionMode.NONE &&
			table.getSelection().isEmpty() == false);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.append("class", SelectionCountToolbarAction.CSS_CLASS, " ");
	}
}
