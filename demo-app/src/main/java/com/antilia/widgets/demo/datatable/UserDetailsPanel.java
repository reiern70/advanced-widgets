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
package com.antilia.widgets.demo.datatable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

/**
 * Shows the data of a user, and nothing while there is none.
 */
public class UserDetailsPanel extends GenericPanel<User>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the user to show
	 */
	public UserDetailsPanel(String id, IModel<User> model)
	{
		super(id, model);
		setOutputMarkupPlaceholderTag(true);

		add(new Label("id", model.map(User::getId)));
		add(new Label("firstName", model.map(User::getFirstName)));
		add(new Label("lastName", model.map(User::getLastName)));
		add(new Label("homePhone", model.map(User::getHomePhone)));
		add(new Label("cellPhone", model.map(User::getCellPhone)));
		add(new Label("bornDate", model.map(User::getBornDate)));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(getModelObject() != null);
	}
}
