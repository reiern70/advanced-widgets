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
package com.antiliacode.widgets.demo.datatable;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.dynamicdatatable.IFooterDataProvider;
import com.antiliacode.widgets.dynamicdatatable.IIdentifiableDynamicDataProvider;
import com.antiliacode.widgets.dynamicdatatable.SortableDynamicDataProvider;

/**
 * The users of the large database for the dynamic table examples. Sorts by first or last name
 * with the database's indexes, and by born date in memory, and filters in memory by first and
 * last name and by country. The users are their own keys' source, being
 * {@link User#getId() identifiable}. The footer of the tables tells how many users there are and
 * their average age.
 */
public class UsersProvider extends SortableDynamicDataProvider<User, Long, String>
	implements
		IIdentifiableDynamicDataProvider<User, Long>,
		IFooterDataProvider
{
	private static final long serialVersionUID = 1L;

	private static final String BORN_DATE = "bornDate";

	private String filter;

	private String country;

	/**
	 * Constructor.
	 */
	public UsersProvider()
	{
		super(Long.class);
	}

	/**
	 * @return the text the first or last name of a user has to contain, or {@code null}
	 */
	public String getFilter()
	{
		return filter;
	}

	/**
	 * @param filter
	 *            the text the first or last name of a user has to contain, or {@code null}
	 */
	public void setFilter(String filter)
	{
		this.filter = filter;
	}

	/**
	 * @return the ISO 3166 code of the country the users live in, or {@code null} for all
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * @param country
	 *            the ISO 3166 code of the country the users live in, or {@code null} for all
	 */
	public void setCountry(String country)
	{
		this.country = country;
	}

	@Override
	public Iterator<User> iterator(long first, long count)
	{
		List<User> users = users();
		return new ArrayList<>(users.subList((int)first,
			(int)Math.min(users.size(), first + count))).iterator();
	}

	@Override
	public long size()
	{
		return users().size();
	}

	/**
	 * @return the users whose first or last name contains the filter, in the order of the sort
	 */
	private List<User> users()
	{
		SortParam<String> sort = getSort();
		boolean byBornDate = sort != null && BORN_DATE.equals(sort.getProperty());
		List<User> index = DatabaseLocator.getLargeDatabase()
			.getIndex(byBornDate ? null : sort);
		List<User> users;
		synchronized (index)
		{
			users = new ArrayList<>(index);
		}
		if (Strings.isEmpty(filter) == false)
		{
			String term = filter.trim().toLowerCase(Locale.ROOT);
			users.removeIf(user -> contains(user.getFirstName(), term) == false &&
				contains(user.getLastName(), term) == false);
		}
		if (country != null)
		{
			users.removeIf(user -> country.equals(user.getCountry()) == false);
		}
		if (byBornDate)
		{
			Comparator<User> byBorn = Comparator.comparing(User::getBornDate,
				Comparator.nullsFirst(Comparator.naturalOrder()));
			users.sort(sort.isAscending() ? byBorn : byBorn.reversed());
		}
		return users;
	}

	private static boolean contains(String name, String term)
	{
		return name != null && name.toLowerCase(Locale.ROOT).contains(term);
	}

	/**
	 * @return the average age of the users the filter lets through, in years, or {@code null} if
	 *         none has a born date
	 */
	public Long getAverageAge()
	{
		return averageAge(users());
	}

	private static Long averageAge(List<User> users)
	{
		LocalDate today = LocalDate.now();
		double average = users.stream()
			.map(UsersTablePanel::bornOn)
			.filter(Objects::nonNull)
			.mapToInt(born -> Period.between(born, today).getYears())
			.average()
			.orElse(Double.NaN);
		return Double.isNaN(average) ? null : Math.round(average);
	}

	/**
	 * @return the number of users the filter lets through, as {@code users}, and their average
	 *         age, as {@code averageAge}, if any has a born date
	 */
	@Override
	public Object footer()
	{
		List<User> users = users();
		Map<String, Object> footer = new LinkedHashMap<>();
		footer.put("users", users.size());
		footer.put("averageAge", averageAge(users));
		return footer;
	}

	@Override
	public User findByKey(Long key)
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
