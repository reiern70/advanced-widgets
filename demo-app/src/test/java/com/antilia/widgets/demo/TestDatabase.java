/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.Database;
import com.antilia.widgets.demo.data.IAccessRepository;
import com.antilia.widgets.demo.data.IOldAccessRepository;
import com.antilia.widgets.demo.data.IUserRepository;
import com.antilia.widgets.demo.data.User;

/**
 * The database the tests share, an H2 database in memory (see the surefire configuration of the
 * pom), and a way to start a test on it with only the accounts the demo always has.
 */
public final class TestDatabase
{
	/** The accounts the demo always has. */
	public static final Set<String> DEFAULT_ACCOUNTS = Set.of("semedy", "wicket", "admin");

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
