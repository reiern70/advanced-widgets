/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.JobRole;
import com.antilia.widgets.demo.data.Language;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.data.UserFilter;
import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.SortableDynamicDataProvider;

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
		List<IColumn<User, String>> columns = List.of(
			new AdminColumn<>("account", "name", User::getName),
			new AdminColumn<>("email", "email", User::getEmail),
			new AdminColumn<>("firstName", "firstName", User::getFirstName),
			new AdminColumn<>("lastName", "lastName", User::getLastName),
			new AdminColumn<User>("country", "country",
				user -> user.getCountry() == null ? ""
					: Countries.name(user.getCountry(), getLocale())),
			new AdminColumn<User>("jobRole", "jobRole", this::jobRole),
			new AdminColumn<User>("userLanguage", "language", this::language),
			new AdminColumn<User>("role", "role", user -> getString("role." + user.getRole())),
			new AdminColumn<User>("validated", "validated",
				user -> getString(user.isValidated() ? "yes" : "no")),
			new AdminColumn<>("created", "created", user -> AdminTables.format(user.getCreated())));

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
		add(AdminTables.configure(table, "users.csv"));
	}

	/**
	 * @return what the user does, in words with Other
	 */
	private String jobRole(User user)
	{
		JobRole role = user.getJobRole();
		if (role == null)
		{
			return "";
		}
		String name = getString("JobRole." + role.name());
		return role == JobRole.OTHER && user.getJobRoleOther() != null
			? name + ": " + user.getJobRoleOther() : name;
	}

	private String language(User user)
	{
		Language language = user.getLanguage();
		return language == null ? "" : language.getLocale().getDisplayLanguage(getLocale());
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
