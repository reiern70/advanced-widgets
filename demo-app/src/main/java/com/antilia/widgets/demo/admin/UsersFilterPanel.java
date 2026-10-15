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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.data.UserFilter;
import com.antilia.widgets.dropdown.Dropdown;

/**
 * Filters the accounts while the administrator types or chooses: by country, by the name of the
 * account, first or last name and by email; shown in front of the navigation of the
 * {@link UsersPanel}'s table.
 */
abstract class UsersFilterPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	UsersFilterPanel(String id, UserFilter filter)
	{
		super(id);
		add(filtering(new TextField<>("name", new PropertyModel<String>(filter, "name")), "input"));
		add(filtering(new TextField<>("email", new PropertyModel<String>(filter, "email")),
			"input"));
		Dropdown<String> country = new Dropdown<>("country",
			new PropertyModel<>(filter, "country"), Countries.codes(), Countries.renderer());
		country.setNullValid(true);
		add(filtering(country, "change"));
	}

	private FormComponent<String> filtering(FormComponent<String> field, String event)
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
