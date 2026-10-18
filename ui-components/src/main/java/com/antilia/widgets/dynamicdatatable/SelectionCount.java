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

import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

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
		add(clear.add(new LookBehavior(LookPart.TABLE_SELECTION_CLEAR)));
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
		Looks.addClasses(tag, Looks.of(this), LookPart.TABLE_SELECTION_COUNT);
	}
}
