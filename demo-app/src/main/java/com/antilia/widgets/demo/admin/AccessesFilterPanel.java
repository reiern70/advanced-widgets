/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

import com.antilia.widgets.demo.data.AccessFilter;
import com.antilia.widgets.dropdown.Dropdown;

/**
 * Filters the accesses while the administrator types or chooses: by user, by the part opened,
 * public or private, and by the address they came from; shown in front of the navigation of the
 * {@link AccessesPanel}'s table.
 */
abstract class AccessesFilterPanel extends AdminFilterPanel
{
	private static final long serialVersionUID = 1L;

	AccessesFilterPanel(String id, AccessFilter filter)
	{
		super(id);
		add(filtering(new TextField<>("user", new PropertyModel<String>(filter, "user")), "input"));
		Dropdown<Boolean> part = new Dropdown<>("part", new PropertyModel<>(filter, "privatePart"),
			List.of(Boolean.FALSE, Boolean.TRUE), new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(Boolean privatePart)
				{
					return getString(privatePart ? "part.private" : "part.public");
				}

				@Override
				public String getIdValue(Boolean privatePart, int index)
				{
					return privatePart.toString();
				}
			});
		part.setNullValid(true);
		add(filtering(part, "change"));
		add(filtering(new TextField<>("address", new PropertyModel<String>(filter, "address")),
			"input"));
	}
}
