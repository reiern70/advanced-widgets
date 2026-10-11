/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.contacts;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import com.antilia.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antilia.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * The contacts the {@link ContactFilter filter} lets pass, sorted by a column of the table: by
 * name, email, day of birth, country or profession, the last two by their names in the language
 * of the session. By name at first.
 */
public class ContactsProvider extends SortableDynamicDataProvider<Contact, Long, String>
	implements
		IIdentifiableDynamicDataProvider<Contact, Long>
{
	private static final long serialVersionUID = 1L;

	/** The sort property of the name, last name first. */
	public static final String NAME = "name";

	/** The sort property of the email. */
	public static final String EMAIL = "email";

	/** The sort property of the day of birth. */
	public static final String BIRTH_DATE = "birthDate";

	/** The sort property of the country, by its name. */
	public static final String COUNTRY = "country";

	/** The sort property of the profession, by its name. */
	public static final String PROFESSION = "profession";

	private final ContactFilter filter;

	/**
	 * @param filter
	 *            the filters the contacts have to pass
	 */
	public ContactsProvider(ContactFilter filter)
	{
		super(Long.class);
		this.filter = filter;
		setSort(NAME, SortOrder.ASCENDING);
	}

	@Override
	public Iterator<Contact> iterator(long first, long count)
	{
		List<Contact> contacts = contacts();
		return contacts.subList((int)Math.min(first, contacts.size()),
			(int)Math.min(contacts.size(), first + count)).iterator();
	}

	@Override
	public long size()
	{
		return ContactsDatabase.all().stream().filter(filter::matches).count();
	}

	@Override
	public Contact findByKey(Long key)
	{
		return key != null ? ContactsDatabase.get(key) : null;
	}

	/**
	 * @return the contacts the filter lets pass, in the order of the sort
	 */
	private List<Contact> contacts()
	{
		List<Contact> contacts = new ArrayList<>(
			ContactsDatabase.all().stream().filter(filter::matches).toList());
		SortParam<String> sort = getSort();
		if (sort != null)
		{
			Comparator<Contact> order = comparator(sort.getProperty());
			contacts.sort(sort.isAscending() ? order : order.reversed());
		}
		return contacts;
	}

	private static Comparator<Contact> comparator(String property)
	{
		Locale locale = Session.exists() ? Session.get().getLocale() : Locale.ENGLISH;
		Collator collator = Collator.getInstance(locale);
		Comparator<Contact> byName = Comparator.comparing(Contact::getLastName, collator)
			.thenComparing(Contact::getFirstName, collator);
		Comparator<Contact> order = switch (property)
		{
			case EMAIL -> Comparator.comparing(Contact::getEmail);
			case BIRTH_DATE -> Comparator.comparing(Contact::getBirthDate);
			case COUNTRY -> Comparator.comparing(Contact::getCountryName, collator);
			case PROFESSION -> Comparator.comparing(Contact::getProfessionName, collator);
			default -> byName;
		};
		return order.thenComparing(byName).thenComparing(Contact::getId);
	}
}
