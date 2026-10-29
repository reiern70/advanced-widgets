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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.util.lang.Args;

/**
 * Keeps the users of the demos in memory, safe for use by several threads. Saving a user gives it
 * the next free id. The users are shared by every visitor, so there are at most
 * {@value #MAX_USERS} of them; the users in each order are kept until one changes, so that the
 * pages of a list cost no sorting.
 */
public class UsersDatabase
{
	/** The most users the database keeps. */
	public static final int MAX_USERS = 2000;

	private static final Comparator<User> BY_FIRST_NAME = Comparator
		.comparing(User::getFirstName, Comparator.nullsFirst(Comparator.naturalOrder()))
		.thenComparing(User::getId);

	private static final Comparator<User> BY_LAST_NAME = Comparator
		.comparing(User::getLastName, Comparator.nullsFirst(Comparator.naturalOrder()))
		.thenComparing(User::getId);

	private final Map<Long, User> users = new ConcurrentHashMap<>();

	private final AtomicLong lastId = new AtomicLong();

	/** The users in each order asked for so far, by the order; emptied by every change. */
	private final Map<String, List<User>> sorted = new ConcurrentHashMap<>();

	/**
	 * @param count
	 *            how many random users the database starts with
	 */
	public UsersDatabase(int count)
	{
		for (int i = 0; i < count; i++)
		{
			save(UserGenerator.getInstance().generate());
		}
	}

	/**
	 * @param id
	 *            the id of a saved user
	 * @return the user
	 * @throws IllegalArgumentException
	 *             if there is no user with that id
	 */
	public User get(long id)
	{
		User user = users.get(id);
		if (user == null)
		{
			throw new IllegalArgumentException("There is no user with the id " + id);
		}
		return user;
	}

	/**
	 * @return how many users there are
	 */
	public int getCount()
	{
		return users.size();
	}

	/**
	 * @param first
	 *            the index of the first user
	 * @param count
	 *            how many users at most
	 * @param sort
	 *            the order, by {@code firstName} or {@code lastName}; by first name if
	 *            {@code null}
	 * @return the users from {@code first} on, in that order
	 */
	public List<User> find(long first, long count, SortParam<String> sort)
	{
		List<User> sorted = getIndex(sort);
		int from = (int)Math.min(first, sorted.size());
		int to = (int)Math.min(first + count, sorted.size());
		return sorted.subList(from, to);
	}

	/**
	 * @param sort
	 *            the order, by {@code firstName} or {@code lastName}; by first name if
	 *            {@code null}
	 * @return all users in that order, a list of its own
	 */
	public List<User> getIndex(SortParam<String> sort)
	{
		Comparator<User> order = comparator(sort);
		String key = sort == null ? "" : sort.getProperty() + (sort.isAscending() ? "+" : "-");
		List<User> index = sorted.get(key);
		if (index != null)
		{
			return index;
		}
		synchronized (this)
		{
			return sorted.computeIfAbsent(key,
				ignored -> users.values().stream().sorted(order).toList());
		}
	}

	/**
	 * @return whether the database keeps {@value #MAX_USERS} users already, and saves no more
	 */
	public boolean isFull()
	{
		return users.size() >= MAX_USERS;
	}

	private static Comparator<User> comparator(SortParam<String> sort)
	{
		if (sort == null)
		{
			return BY_FIRST_NAME;
		}
		Comparator<User> order = switch (sort.getProperty())
		{
			case "firstName" -> BY_FIRST_NAME;
			case "lastName" -> BY_LAST_NAME;
			default -> throw new IllegalArgumentException(
				"Users can be sorted by firstName or lastName, not by " + sort.getProperty());
		};
		return sort.isAscending() ? order : order.reversed();
	}

	/**
	 * Adds a user not saved yet, giving it an id.
	 *
	 * @param user
	 *            the user, with the id {@code 0}
	 * @throws IllegalStateException
	 *             if the database is {@link #isFull() full}
	 */
	public synchronized void save(User user)
	{
		Args.notNull(user, "user");
		if (user.getId() != 0)
		{
			throw new IllegalArgumentException(user + " is already saved");
		}
		if (isFull())
		{
			throw new IllegalStateException("There are " + MAX_USERS + " users already");
		}
		user.setId(lastId.incrementAndGet());
		users.put(user.getId(), user);
		sorted.clear();
	}

	/**
	 * Called after the fields of a saved user were changed. The users are held in memory, so
	 * there is nothing to write, but the orders are made anew.
	 *
	 * @param user
	 *            the changed user
	 */
	public synchronized void update(User user)
	{
		get(user.getId());
		sorted.clear();
	}

	/**
	 * Removes a user.
	 *
	 * @param user
	 *            the user
	 */
	public synchronized void delete(User user)
	{
		users.remove(user.getId());
		sorted.clear();
	}
}
