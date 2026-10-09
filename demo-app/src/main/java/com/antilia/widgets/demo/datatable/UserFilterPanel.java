/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A text field filtering users while the user types, shown in front of the navigation of the
 * dynamic table examples.
 */
public abstract class UserFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	public UserFilterPanel(String id, IModel<String> filter)
	{
		super(id);

		TextField<String> field = new TextField<>("filter", filter);
		field.add(new AjaxFormComponentUpdatingBehavior("input")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				attributes.setThrottlingSettings(
					new ThrottlingSettings(Duration.ofMillis(300), true));
			}

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onFilter(target);
			}
		});
		add(field);
	}

	/**
	 * Called after the filter changed.
	 */
	protected abstract void onFilter(AjaxRequestTarget target);
}
