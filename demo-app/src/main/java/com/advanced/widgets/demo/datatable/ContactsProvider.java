/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.util.string.Strings;

import com.advanced.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.advanced.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * The contacts of the large database for the dynamic table examples. Sorts by first or last name
 * with the database's indexes, and by born date in memory, and filters by first and last name in
 * memory. The contacts are their own keys' source, being {@link Contact#getId() identifiable}.
 */
public class ContactsProvider extends SortableDynamicDataProvider<Contact, Long, String>
	implements
		IIdentifiableDynamicDataProvider<Contact, Long>
{
	private static final long serialVersionUID = 1L;

	private static final String BORN_DATE = "bornDate";

	private String filter;

	/**
	 * Constructor.
	 */
	public ContactsProvider()
	{
		super(Long.class);
	}

	/**
	 * @return the text the first or last name of a contact has to contain, or {@code null}
	 */
	public String getFilter()
	{
		return filter;
	}

	/**
	 * @param filter
	 *            the text the first or last name of a contact has to contain, or {@code null}
	 */
	public void setFilter(String filter)
	{
		this.filter = filter;
	}

	@Override
	public Iterator<Contact> iterator(long first, long count)
	{
		List<Contact> contacts = contacts();
		return new ArrayList<>(contacts.subList((int)first,
			(int)Math.min(contacts.size(), first + count))).iterator();
	}

	@Override
	public long size()
	{
		return contacts().size();
	}

	/**
	 * @return the contacts whose first or last name contains the filter, in the order of the sort
	 */
	private List<Contact> contacts()
	{
		SortParam<String> sort = getSort();
		boolean byBornDate = sort != null && BORN_DATE.equals(sort.getProperty());
		List<Contact> index = DatabaseLocator.getLargeDatabase()
			.getIndex(byBornDate ? null : sort);
		List<Contact> contacts;
		synchronized (index)
		{
			contacts = new ArrayList<>(index);
		}
		if (Strings.isEmpty(filter) == false)
		{
			String term = filter.trim().toLowerCase(Locale.ROOT);
			contacts.removeIf(contact -> contains(contact.getFirstName(), term) == false &&
				contains(contact.getLastName(), term) == false);
		}
		if (byBornDate)
		{
			Comparator<Contact> byBorn = Comparator.comparing(Contact::getBornDate,
				Comparator.nullsFirst(Comparator.naturalOrder()));
			contacts.sort(sort.isAscending() ? byBorn : byBorn.reversed());
		}
		return contacts;
	}

	private static boolean contains(String name, String term)
	{
		return name != null && name.toLowerCase(Locale.ROOT).contains(term);
	}

	@Override
	public Contact findByKey(Long key)
	{
		try
		{
			return DatabaseLocator.getLargeDatabase().get(key);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}
}
