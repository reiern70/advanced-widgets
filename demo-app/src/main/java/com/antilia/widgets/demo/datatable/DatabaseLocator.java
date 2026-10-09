/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

/**
 * Holds the users of the demos, shared by all sessions.
 */
public final class DatabaseLocator
{
	private static final UsersDatabase LARGE = new UsersDatabase(1000);

	private DatabaseLocator()
	{
	}

	/**
	 * @return the database of a thousand users
	 */
	public static UsersDatabase getLargeDatabase()
	{
		return LARGE;
	}
}
