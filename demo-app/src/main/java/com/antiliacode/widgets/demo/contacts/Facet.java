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
package com.antiliacode.widgets.demo.contacts;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.antiliacode.widgets.demo.Countries;

/**
 * A category of the filters of the contacts: the country they live in, the year they were born or
 * their profession. A value of a facet is a string: an ISO 3166 code, a year or the name of a
 * {@link Profession}.
 */
public enum Facet
{
	/** The country a contact lives in. */
	COUNTRY("country")
	{
		@Override
		public String valueOf(Contact contact)
		{
			return contact.getCountry();
		}

		@Override
		public String label(String value, Locale locale)
		{
			return Countries.name(value, locale);
		}
	},

	/** The year a contact was born. */
	BIRTH_YEAR("birthYear")
	{
		@Override
		public String valueOf(Contact contact)
		{
			return String.valueOf(contact.getBirthYear());
		}

		@Override
		public String label(String value, Locale locale)
		{
			return value;
		}
	},

	/** What a contact does. */
	PROFESSION("profession")
	{
		@Override
		public String valueOf(Contact contact)
		{
			return contact.getProfession().name();
		}

		@Override
		public String label(String value, Locale locale)
		{
			return Profession.valueOf(value).getName(locale);
		}
	};

	private final String key;

	Facet(String key)
	{
		this.key = key;
	}

	/**
	 * @return the name of the facet in the keys of the texts, {@code country} for example
	 */
	public String key()
	{
		return key;
	}

	/**
	 * @return the value of the facet the contact has
	 */
	public abstract String valueOf(Contact contact);

	/**
	 * @return how a value of the facet reads in the language
	 */
	public abstract String label(String value, Locale locale);

	/**
	 * Counts the values of the facet among the contacts the filter leaves when this facet is not
	 * applied, as faceted search does: choosing a country leaves the counts of the other
	 * countries as they were.
	 *
	 * @param filter
	 *            the filters
	 * @param locale
	 *            the language of the labels
	 * @return the values with how many contacts have each, the most common first; a value chosen
	 *         in the filter is there even when no contact has it
	 */
	public List<FacetValue> values(ContactFilter filter, Locale locale)
	{
		Map<String, Integer> counts = new HashMap<>();
		for (Contact contact : ContactsDatabase.all())
		{
			if (filter.matches(contact, this))
			{
				counts.merge(valueOf(contact), 1, Integer::sum);
			}
		}
		Set<String> chosen = filter.get(this);
		chosen.forEach(value -> counts.putIfAbsent(value, 0));
		Collator collator = Collator.getInstance(locale);
		return counts.entrySet().stream()
			.map(entry -> new FacetValue(entry.getKey(), label(entry.getKey(), locale),
				entry.getValue()))
			.sorted(Comparator.comparingInt(FacetValue::count).reversed()
				.thenComparing(FacetValue::label, collator))
			.toList();
	}
}
