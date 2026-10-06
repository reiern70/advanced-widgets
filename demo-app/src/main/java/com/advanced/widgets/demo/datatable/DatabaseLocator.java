/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

/**
 * Holds the contacts of the demos, shared by all sessions.
 */
public final class DatabaseLocator
{
	private static final ContactsDatabase LARGE = new ContactsDatabase(1000);

	private DatabaseLocator()
	{
	}

	/**
	 * @return the database of a thousand contacts
	 */
	public static ContactsDatabase getLargeDatabase()
	{
		return LARGE;
	}
}
