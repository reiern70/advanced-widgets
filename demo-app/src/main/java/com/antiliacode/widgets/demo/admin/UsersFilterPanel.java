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
package com.antiliacode.widgets.demo.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.data.UserFilter;
import com.antiliacode.widgets.dropdown.Dropdown;

/**
 * Filters the accounts while the administrator types or chooses: by country, by the name of the
 * account, first or last name and by email; shown in front of the navigation of the
 * {@link UsersPanel}'s table.
 */
abstract class UsersFilterPanel extends AdminFilterPanel
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
}
