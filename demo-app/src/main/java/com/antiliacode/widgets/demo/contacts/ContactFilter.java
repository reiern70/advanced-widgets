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
package com.antiliacode.widgets.demo.contacts;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.string.Strings;

/**
 * The filters of the contacts: for each {@link Facet}, the values chosen, and a text. A contact
 * passes when, for each facet with values chosen, it has one of them, and its name or email
 * contains the text, ignoring case and accents; a facet without values, or no text, lets all
 * pass.
 */
public class ContactFilter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final Map<Facet, LinkedHashSet<String>> chosen = new EnumMap<>(Facet.class);

	private String text;

	/**
	 * @return the text the name or the email of a contact has to contain, {@code null} for any
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @param text
	 *            the text the name or the email of a contact has to contain, {@code null} for any
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * @return the values chosen of the facet, in the order they were chosen; the set cannot be
	 *         changed
	 */
	public Set<String> get(Facet facet)
	{
		Set<String> values = chosen.get(facet);
		return values != null ? Collections.unmodifiableSet(values) : Set.of();
	}

	/**
	 * @return whether the value of the facet is chosen
	 */
	public boolean isChosen(Facet facet, String value)
	{
		Set<String> values = chosen.get(facet);
		return values != null && values.contains(value);
	}

	/**
	 * Chooses the values of the facet, instead of those chosen before.
	 */
	public void set(Facet facet, Collection<String> values)
	{
		if (values.isEmpty())
		{
			chosen.remove(facet);
		}
		else
		{
			chosen.put(facet, new LinkedHashSet<>(values));
		}
	}

	/**
	 * Chooses a value of the facet, or no longer.
	 */
	public void choose(Facet facet, String value, boolean choose)
	{
		LinkedHashSet<String> values = new LinkedHashSet<>(get(facet));
		if (choose)
		{
			values.add(value);
		}
		else
		{
			values.remove(value);
		}
		set(facet, values);
	}

	/**
	 * Lets all values of the facet pass.
	 */
	public void clear(Facet facet)
	{
		chosen.remove(facet);
	}

	/**
	 * Lets the contacts of all values of all facets pass; the text stays.
	 */
	public void clear()
	{
		chosen.clear();
	}

	/**
	 * @return whether no value of any facet is chosen
	 */
	public boolean isEmpty()
	{
		return chosen.isEmpty();
	}

	/**
	 * @return how many values are chosen, of all facets
	 */
	public int size()
	{
		return chosen.values().stream().mapToInt(Set::size).sum();
	}

	/**
	 * @return whether the contact passes the filters
	 */
	public boolean matches(Contact contact)
	{
		return matches(contact, null);
	}

	/**
	 * @param ignored
	 *            a facet whose values are not looked at, {@code null} for none
	 * @return whether the contact passes the filters of the facets but the one ignored
	 */
	public boolean matches(Contact contact, Facet ignored)
	{
		if (Strings.isEmpty(text) == false)
		{
			String typed = plain(text.trim());
			if (plain(contact.getName()).contains(typed) == false &&
				contact.getEmail().contains(typed) == false)
			{
				return false;
			}
		}
		for (Map.Entry<Facet, LinkedHashSet<String>> entry : chosen.entrySet())
		{
			Facet facet = entry.getKey();
			if (facet != ignored && entry.getValue().contains(facet.valueOf(contact)) == false)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the text in lower case without accents
	 */
	static String plain(String text)
	{
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
	}
}
