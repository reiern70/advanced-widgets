/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.dropdown;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.demo.frameworks.ChoicesResource;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.dropdown.MultiDropdown;
import com.github.openjson.JSONObject;

/**
 * {@link Dropdown} and {@link MultiDropdown}: drop-downs opened, filtered and chosen from in the
 * browser, which post their choice like native selects, in the form of the
 * {@link DropdownExamplesPanel}. The React and Svelte tabs show the same form in those frameworks,
 * the countries loaded from the {@link ChoicesResource}.
 */
public class DropdownPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private final DropdownExamplesPanel examples;

	/**
	 * Constructor.
	 */
	public DropdownPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		examples = new DropdownExamplesPanel("examples");
		themed.add(examples);
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "dropdown";
	}

	/**
	 * @return the texts, the options but the countries, which come from the
	 *         {@link ChoicesResource}, and the choices as the examples have them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		return examples.getFrameworkConfig();
	}
}
