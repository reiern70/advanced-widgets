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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.demo.data.User;

/**
 * All the administration tells of an account, a row each, as the table of the
 * {@link UsersPanel users} has them; or that there is no such account any more.
 */
class UserInfoPanel extends GenericPanel<User>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the account, {@code null} for none
	 */
	UserInfoPanel(String id, IModel<User> model)
	{
		super(id, model);
		WebMarkupContainer table = new WebMarkupContainer("table")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(UserInfoPanel.this.getModelObject() != null);
			}
		};
		add(table);
		table.add(new ListView<>("fields", UserColumns.of(this))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<AdminColumn<User>> item)
			{
				AdminColumn<User> column = item.getModelObject();
				item.add(new Label("name", getString(column.getHeaderKey())));
				item.add(new Label("value", column.getDataModel(UserInfoPanel.this.getModel())));
			}
		});
		add(new WebMarkupContainer("none")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(UserInfoPanel.this.getModelObject() == null);
			}
		});
	}
}
