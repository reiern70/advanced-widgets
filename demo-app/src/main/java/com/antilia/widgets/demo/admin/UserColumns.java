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
package com.antilia.widgets.demo.admin;

import java.util.List;

import org.apache.wicket.Component;

import com.antilia.widgets.demo.data.JobRole;
import com.antilia.widgets.demo.data.Language;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.Countries;

/**
 * What the administration tells of an account, a column each: in the table of the
 * {@link UsersPanel users}, and in the {@link UserInfoPanel popover} of a user of the accesses.
 */
final class UserColumns
{
	private UserColumns()
	{
	}

	/**
	 * @param context
	 *            the component whose bundles and locale the values are written in
	 * @return the columns
	 */
	static List<AdminColumn<User>> of(Component context)
	{
		return List.of(new AdminColumn<>("account", "name", User::getName),
			new AdminColumn<>("email", "email", User::getEmail),
			new AdminColumn<>("firstName", "firstName", User::getFirstName),
			new AdminColumn<>("lastName", "lastName", User::getLastName),
			new AdminColumn<User>("country", "country",
				user -> user.getCountry() == null ? ""
					: Countries.name(user.getCountry(), context.getLocale())),
			new AdminColumn<User>("jobRole", "jobRole", user -> jobRole(context, user)),
			new AdminColumn<User>("userLanguage", "language", user -> language(context, user)),
			new AdminColumn<User>("role", "role",
				user -> context.getString("role." + user.getRole())),
			new AdminColumn<User>("validated", "validated",
				user -> context.getString(user.isValidated() ? "yes" : "no")),
			new AdminColumn<>("created", "created", user -> AdminTables.format(user.getCreated())));
	}

	/**
	 * @return what the user does, in words with Other
	 */
	private static String jobRole(Component context, User user)
	{
		JobRole role = user.getJobRole();
		if (role == null)
		{
			return "";
		}
		String name = context.getString("JobRole." + role.name());
		return role == JobRole.OTHER && user.getJobRoleOther() != null
			? name + ": " + user.getJobRoleOther() : name;
	}

	private static String language(Component context, User user)
	{
		Language language = user.getLanguage();
		return language == null ? "" : language.getLocale().getDisplayLanguage(context.getLocale());
	}
}
