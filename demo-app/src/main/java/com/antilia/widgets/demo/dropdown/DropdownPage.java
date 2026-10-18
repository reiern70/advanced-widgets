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
