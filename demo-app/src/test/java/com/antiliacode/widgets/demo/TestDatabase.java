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
package com.antiliacode.widgets.demo;

import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.data.Database;
import com.antiliacode.widgets.demo.data.IAccessRepository;
import com.antiliacode.widgets.demo.data.IOldAccessRepository;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.User;

/**
 * The database the tests share, an H2 database in memory (see the surefire configuration of the
 * pom), and a way to start a test on it with only the accounts the demo always has.
 */
public final class TestDatabase
{
	/** The accounts the demo always has. */
	public static final Set<String> DEFAULT_ACCOUNTS = Set.of("semedy", "wicket", "admin");

	/** The password of the account admin in the tests, which the pom gives like the two below. */
	public static final String ADMIN_PASSWORD = "admin-test-password";

	/** The password of the account semedy in the tests. */
	public static final String SEMEDY_PASSWORD = "semedy-test-password";

	/** The password of the account wicket in the tests. */
	public static final String WICKET_PASSWORD = "wicket-test-password";

	private TestDatabase()
	{
	}

	/**
	 * @return the Spring context of the database of the tests
	 */
	public static ApplicationContext context()
	{
		return Database.context(settings());
	}

	/**
	 * @return the accounts of the database of the tests
	 */
	public static Accounts accounts()
	{
		return context().getBean(Accounts.class);
	}

	/**
	 * Removes the accesses, the old ones too, and the accounts but those the demo always has,
	 * which it adds if missing.
	 */
	public static void clear()
	{
		ApplicationContext context = context();
		context.getBean(IAccessRepository.class).deleteAll();
		context.getBean(IOldAccessRepository.class).deleteAll();
		IUserRepository users = context.getBean(IUserRepository.class);
		users.deleteAll(users.findAll().stream()
			.filter(user -> !DEFAULT_ACCOUNTS.contains(user.getName()))
			.toList());
		accounts().addDefaults();
	}

	/**
	 * @return the users of the database, for checks
	 */
	public static List<User> users()
	{
		return context().getBean(IUserRepository.class).findAll();
	}

	private static Database.Settings settings()
	{
		Database.Settings settings = Database.fromSettings();
		if (!settings.url().startsWith("jdbc:h2:mem:"))
		{
			// never empty a real database: run the tests with Maven, which sets one in memory
			throw new IllegalStateException("The tests need a database in memory, not " + settings);
		}
		return settings;
	}
}
