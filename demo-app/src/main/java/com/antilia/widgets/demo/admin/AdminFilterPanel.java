/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * The filter of a table of the administration, shown in front of its navigation (see
 * {@link FilteredAdminTable}): its fields filter while the administrator types or chooses.
 */
abstract class AdminFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	AdminFilterPanel(String id)
	{
		super(id);
	}

	/**
	 * Makes the field filter on the event, a moment after the last one.
	 *
	 * @param field
	 *            a field of the filter
	 * @param event
	 *            {@code input} for a text, {@code change} for a choice
	 * @return the field
	 */
	protected <T> FormComponent<T> filtering(FormComponent<T> field, String event)
	{
		field.add(new AjaxFormComponentUpdatingBehavior(event)
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
		return field;
	}

	/**
	 * Called after the filter changed.
	 */
	protected abstract void onFilter(AjaxRequestTarget target);
}
