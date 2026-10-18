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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antilia.widgets.clipboard.ClipboardCopyBehavior;
import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BeanActionsColumn;
import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.BeanRowNumberColumn;
import com.antilia.widgets.dynamicdatatable.BeanSelectionColumn;
import com.antilia.widgets.dynamicdatatable.IFixedWidthColumn;
import com.antilia.widgets.dynamicdatatable.IHideableColumn;
import com.antilia.widgets.table.IMovableColumn;

/**
 * The table of the users with its rows rendered on the server by Wicket components: a
 * {@link BeanBasedDynamicDataTable} with Wicket's {@link IColumn}s, looking and behaving like the
 * {@link JsonUsersTablePanel JSON one}.
 */
public class BeanUsersTablePanel extends UsersTablePanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param host
	 *            the page the table is on
	 */
	public BeanUsersTablePanel(String id, IUsersTableHost host)
	{
		super(id, host);
	}

	/**
	 * The proportions of the columns: selection, number, actions, id, first and last name, born, address, phones and the hidden
	 * phones, city and country.
	 */
	@Override
	public double[] getColumnProportions()
	{
		return new double[] { 0.4, 0.4, 1.1, 0.5, 1.1, 1.1, 0.9, 2.0, 2.4,
			1.1, 1.1, 1.0, 1.0 };
	}

	@Override
	protected BaseDynamicDataTable<User, Long, ?> newTable(String id, UsersProvider provider)
	{
		List<IColumn<User, String>> columns = new ArrayList<>();
		columns.add(new BeanSelectionColumn<>());
		columns.add(new BeanRowNumberColumn<>(Model.of("#")));
		columns.add(new BeanActionsColumn<>(new ResourceModel("actions"), newRowActions()));
		columns.add(new IdColumn());
		columns.add(new UserColumn("firstName", "firstName", User::getFirstName));
		columns.add(new UserColumn("lastName", "lastName", User::getLastName));
		columns.add(new UserColumn("bornDate", "bornDate",
			user -> Objects.toString(bornOn(user), "")));
		columns.add(new UserColumn("address", null,
			user -> user.getAddress() + ", " + user.getCity() + ", " +
				user.getCountryName()));
		columns.add(new PhonesColumn());
		List<IColumn<User, String>> hidden = List.of(
			new UserColumn("homePhone", null, User::getHomePhone),
			new UserColumn("cellPhone", null, User::getCellPhone),
			new UserColumn("city", null, User::getCity),
			new UserColumn("country", null, User::getCountryName));
		columns.addAll(hidden);

		BeanBasedDynamicDataTable<User, Long, String> table = new BeanBasedDynamicDataTable<>(
			id, columns, provider, ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return BeanUsersTablePanel.this.newNavigationToolbar(this, id, position);
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				User user)
			{
				onUserNotFound(target);
			}
		};
		hidden.forEach(column -> table.setColumnVisible(column, false));
		return table;
	}

	/**
	 * The id of the user, a column the user cannot resize.
	 */
	private static class IdColumn extends LambdaColumn<User, String> implements IFixedWidthColumn
	{
		private static final long serialVersionUID = 1L;

		IdColumn()
		{
			super(new ResourceModel("id"), User::getId);
		}
	}

	/**
	 * A value of the user, written escaped, in a column the user can hide and move.
	 */
	private static class UserColumn extends LambdaColumn<User, String>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		UserColumn(String headerKey, String sortProperty,
			SerializableFunction<User, ?> value)
		{
			super(new ResourceModel(headerKey), sortProperty, value);
		}
	}

	/**
	 * The phone numbers of the user, each copied to the clipboard on a double click.
	 */
	private static class PhonesColumn extends LambdaColumn<User, String>
		implements
			IHeaderContributor,
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		PhonesColumn()
		{
			super(new ResourceModel("phones"),
				user -> user.getHomePhone() + " / " + user.getCellPhone());
		}

		@Override
		public void populateItem(Item<ICellPopulator<User>> item, String componentId,
			IModel<User> rowModel)
		{
			item.add(new Label(componentId, rowModel.map(user -> phones(user, item)))
				.setEscapeModelStrings(false));
		}

		private static String phones(User user, Component component)
		{
			return "<span class=\"user-phones\"><b>" + component.getString("phones.home") +
				"</b> " + phone(user.getHomePhone()) + "<b>" +
				component.getString("phones.cell") + "</b> " + phone(user.getCellPhone()) +
				"</span>";
		}

		private static String phone(String number)
		{
			String escaped = Strings.escapeMarkup(number != null ? number : "").toString();
			return "<span data-antilia-copy=\"" + escaped + "\" data-antilia-copy-on=\"dblclick\">" +
				escaped + "</span>";
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			ClipboardCopyBehavior.renderHeadItems(response);
		}
	}
}
