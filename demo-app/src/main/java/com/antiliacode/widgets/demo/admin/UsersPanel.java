/*
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

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.data.UserFilter;
import com.antiliacode.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antiliacode.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * The accounts: those the demo always has, the configured ones and the ones visitors registered,
 * the latest first, filtered by country, by the name of the account, first or last name and by
 * email (see {@link UsersFilterPanel}).
 */
class UsersPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final UserFilter filter = new UserFilter();

	UsersPanel(String id)
	{
		super(id);
		List<IColumn<User, String>> columns = List.copyOf(UserColumns.of(this));

		UsersProvider provider = new UsersProvider(filter);
		add(new Label("count", () -> provider.size()).setOutputMarkupId(true));
		FilteredAdminTable<User> table = new FilteredAdminTable<>("table", columns, provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AdminFilterPanel newFilter(String id)
			{
				return new UsersFilterPanel(id, filter)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onFilter(AjaxRequestTarget target)
					{
						filtered(target);
						target.add(UsersPanel.this.get("count"));
					}
				};
			}
		};
		table.setColumnProportions(1.5, 1.9, 1, 1, 1.1, 1, 0.9, 1, 1.2, 1.5);
		// on a phone, the table scrolls sideways rather than squeezing the columns to a letter
		table.setColumnMinWidths(100, 170, 90, 90, 100, 100, 90, 80, 90, 150);
		add(AdminTables.configure(table, "users.csv"));
	}

	/**
	 * The accounts the filter lets through, a page at a time from the database, sorted there.
	 */
	private static class UsersProvider extends SortableDynamicDataProvider<User, Long, String>
		implements
			IIdentifiableDynamicDataProvider<User, Long>
	{
		private static final long serialVersionUID = 1L;

		private final UserFilter filter;

		UsersProvider(UserFilter filter)
		{
			super(Long.class);
			this.filter = filter;
			setSort("created", SortOrder.DESCENDING);
		}

		@Override
		public Iterator<? extends User> iterator(long first, long count)
		{
			return Accounts.get()
				.page(filter, AdminTables.pageable(first, count, getSort()))
				.iterator();
		}

		@Override
		public long size()
		{
			return Accounts.get().count(filter);
		}

		@Override
		public User findByKey(Long key)
		{
			return Accounts.get().find(key);
		}
	}
}
