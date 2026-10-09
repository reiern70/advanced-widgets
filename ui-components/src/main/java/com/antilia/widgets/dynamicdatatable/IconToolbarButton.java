/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;

class IconToolbarButton<T, K extends Serializable> extends Panel
{
	private static final long serialVersionUID = 1L;

	IconToolbarButton(String id, BaseDynamicDataTable<T, K, ?> table,
		IconBasedToolbarAction<T, K> action)
	{
		super(id);

		IModel<String> tooltip = action.getTooltip();
		IModel<String> assigned = tooltip instanceof IComponentAssignedModel<String> resource
			? resource.wrapOnAssignment(this) : tooltip;
		AjaxLink<Void> button = new AjaxLink<>("button")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				action.onClick(table, target);
			}
		};
		button.add(AttributeModifier.replace("title", assigned),
			AttributeModifier.replace("aria-label", assigned));
		button.add(new Label("icon", () -> action.getIcon().getMarkup())
			.setEscapeModelStrings(false).setRenderBodyOnly(true));
		add(button);
	}
}
