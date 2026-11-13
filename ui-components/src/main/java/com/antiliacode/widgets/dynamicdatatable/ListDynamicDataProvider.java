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
package com.antiliacode.widgets.dynamicdatatable;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.misc.serializable.SerializableComparator;
import org.danekja.java.util.function.serializable.SerializableBiPredicate;
import org.danekja.java.util.function.serializable.SerializableFunction;

/**
 * A sortable provider of rows that are already in memory: a short list from a service, the lines
 * of a document, a preview computed on the page. It reads the list from a model, sorts a copy of
 * it by the table's sort, pages it, and finds a row again by its key.
 *
 * <pre>
 * var provider = new ListDynamicDataProvider&lt;Line, Long, String&gt;(Long.class, linesModel, Line::id)
 * 	.sortByText("description", Line::description)
 * 	.sortBy("amount", Line::amount)
 * 	.defaultSort("description", SortOrder.ASCENDING);
 * </pre>
 * <p>
 * <b>The rows.</b> The model is read once per request, on the first call that needs the rows, and
 * detached with the provider; a model reading the list again on each request, such as a
 * {@link org.apache.wicket.model.LoadableDetachableModel} asking a service, shows the current
 * rows. {@link #keyOf(Object) Keys} come from a function, or from the rows' own
 * {@link IIdentifiable#getId()} if they are {@link IIdentifiable}. {@link #findByKey(Serializable)}
 * looks the key up in the current list, filtered or not, so a {@link #model(Object) row's model},
 * a row action or a selection resolves to the current version of the row, wherever it moved in
 * the list.
 * <p>
 * <b>Sorting.</b> Each sort property has its order: {@link #sortBy(Object, SerializableFunction)}
 * by a value of the row in its natural order, {@link #sortByText(Object, SerializableFunction)} by
 * a text in the alphabetical order of the session's language, which puts {@code Émile} among the
 * {@code E}s rather than after {@code Z}, and {@link #sortWith(Object, SerializableComparator)}
 * by a comparator of the rows. A row whose value is {@code null} comes last, in either direction.
 * A property without an order is not sortable: sorting by it leaves the rows in the order of the
 * list. Sorting is stable: rows of equal values keep the order of the list.
 * <p>
 * The provider is kept in the page, so its functions are serializable lambdas or method
 * references. {@code Comparator.comparing(Line::description)} is not, as its function is not:
 * write {@code sortBy("description", Line::description)} instead.
 * <p>
 * <b>Filtering.</b> {@link #filterBy(IModel, SerializableBiPredicate)} keeps the rows matching the
 * value of a filter model, such as the text of a search box; {@link #size()} and the rows follow
 * it, a {@code null} value keeps every row.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class ListDynamicDataProvider<T, K extends Serializable, S>
	extends
		SortableDynamicDataProvider<T, K, S>
{
	private static final long serialVersionUID = 1L;

	private final IModel<? extends List<T>> rows;

	private final SerializableFunction<? super T, ? extends K> key;

	private final Map<S, Order<T>> orders = new LinkedHashMap<>();

	private IModel<?> filter;

	private SerializableBiPredicate<? super T, Object> matches;

	/** The rows of the current request, read from the model once. */
	private transient List<T> all;

	/** The rows of the current request the filter lets through. */
	private transient List<T> filtered;

	/** The rows of the current request by their keys, made on the first lookup. */
	private transient Map<K, T> byKey;

	/**
	 * A provider of rows that are {@link IIdentifiable}, whose key is their
	 * {@link IIdentifiable#getId()}.
	 *
	 * @param keyType
	 *            the type of the keys
	 * @param rows
	 *            the model of the rows, read once per request; its list is not changed
	 * @throws ClassCastException
	 *             later, on the first row that is not {@link IIdentifiable}
	 */
	public ListDynamicDataProvider(Class<K> keyType, IModel<? extends List<T>> rows)
	{
		this(keyType, rows, null);
	}

	/**
	 * @param keyType
	 *            the type of the keys
	 * @param rows
	 *            the model of the rows, read once per request; its list is not changed
	 * @param key
	 *            returns the key of a row, such as {@code Line::id}: never {@code null}, unique in
	 *            the list and the same for as long as the row exists; {@code null} to take the
	 *            {@link IIdentifiable#getId()} of rows that are {@link IIdentifiable}
	 */
	public ListDynamicDataProvider(Class<K> keyType, IModel<? extends List<T>> rows,
		SerializableFunction<? super T, ? extends K> key)
	{
		super(keyType);
		this.rows = Args.notNull(rows, "rows");
		this.key = key;
	}

	/**
	 * Makes a property sortable by a value of the rows, in its natural order: numbers by size,
	 * dates by time, enums by declaration, texts by their characters. Rows whose value is
	 * {@code null} come last, in either direction. For texts people read, see
	 * {@link #sortByText(Object, SerializableFunction)}.
	 *
	 * @param <U>
	 *            the type of the value
	 * @param property
	 *            the sort property, that of the columns sorting by it
	 * @param value
	 *            returns the value of a row, such as {@code Line::amount}
	 * @return {@code this}
	 */
	public <U extends Comparable<? super U>> ListDynamicDataProvider<T, K, S> sortBy(S property,
		SerializableFunction<? super T, ? extends U> value)
	{
		Args.notNull(value, "value");
		return order(property,
			ascending -> byValue(value, ascending ? Comparator.<U> naturalOrder()
				: Comparator.<U> reverseOrder()));
	}

	/**
	 * Makes a property sortable by a text of the rows, in the alphabetical order of the session's
	 * language: as a {@link Collator} of its locale compares them, so that accents and case do not
	 * put {@code Émile} after {@code Zoé}. Rows whose text is {@code null} come last, in either
	 * direction.
	 *
	 * @param property
	 *            the sort property, that of the columns sorting by it
	 * @param text
	 *            returns the text of a row, such as {@code Line::description}
	 * @return {@code this}
	 */
	public ListDynamicDataProvider<T, K, S> sortByText(S property,
		SerializableFunction<? super T, String> text)
	{
		Args.notNull(text, "text");
		return order(property, ascending -> {
			Comparator<String> collated = collator(locale())::compare;
			return byValue(text, ascending ? collated : collated.reversed());
		});
	}

	/**
	 * Makes a property sortable by a comparator of the rows: ascending in its order, descending in
	 * the reverse one. The comparator decides where rows with {@code null} values go.
	 *
	 * @param property
	 *            the sort property, that of the columns sorting by it
	 * @param comparator
	 *            compares two rows, a serializable lambda
	 * @return {@code this}
	 */
	public ListDynamicDataProvider<T, K, S> sortWith(S property,
		SerializableComparator<? super T> comparator)
	{
		Args.notNull(comparator, "comparator");
		return order(property, ascending -> {
			Comparator<T> order = comparator::compare;
			return ascending ? order : order.reversed();
		});
	}

	/**
	 * Sorts the rows by a sortable property until the table sorts them otherwise.
	 *
	 * @param property
	 *            a property made sortable with {@link #sortBy(Object, SerializableFunction)},
	 *            {@link #sortByText(Object, SerializableFunction)} or
	 *            {@link #sortWith(Object, SerializableComparator)}
	 * @param order
	 *            the order
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             if the property is not sortable
	 */
	public ListDynamicDataProvider<T, K, S> defaultSort(S property, SortOrder order)
	{
		if (!isSortable(property))
		{
			throw new IllegalArgumentException(
				"'" + property + "' is not sortable: give it an order with sortBy first");
		}
		setSort(property, Args.notNull(order, "order"));
		return this;
	}

	/**
	 * @param property
	 *            a sort property
	 * @return whether the rows can be sorted by the property
	 */
	public boolean isSortable(S property)
	{
		return orders.containsKey(property);
	}

	/**
	 * @return the sortable properties, in the order they were made sortable
	 */
	public Set<S> getSortProperties()
	{
		return Collections.unmodifiableSet(orders.keySet());
	}

	/**
	 * Keeps only the rows matching the value of a filter model, for example the text of a search
	 * box: {@link #size()} and the rows follow the value, read once per request. A {@code null}
	 * value keeps every row. The rows a filter leaves out are still found by their key.
	 *
	 * <pre>
	 * provider.filterBy(search, (line, text) -&gt; line.description().contains(text));
	 * </pre>
	 *
	 * @param <F>
	 *            the type of the filter's value
	 * @param filter
	 *            the model of the filter's value, detached with the provider; {@code null} for no
	 *            filter
	 * @param matches
	 *            whether a row matches a value of the filter, never {@code null}
	 * @return {@code this}
	 */
	@SuppressWarnings("unchecked")
	public <F> ListDynamicDataProvider<T, K, S> filterBy(IModel<F> filter,
		SerializableBiPredicate<? super T, ? super F> matches)
	{
		this.filter = filter;
		this.matches = filter != null
			? (SerializableBiPredicate<? super T, Object>)Args.notNull(matches, "matches") : null;
		filtered = null;
		return this;
	}

	/**
	 * Returns the rows of the current request: the list of the model, read on the first call of
	 * the request.
	 *
	 * @return the rows, unfiltered and unsorted, in the order of the list; empty if the model holds
	 *         {@code null}
	 */
	public List<T> getRows()
	{
		if (all == null)
		{
			List<T> list = rows.getObject();
			all = list != null ? list : List.of();
		}
		return all;
	}

	/**
	 * @return the rows the filter lets through, in the order of the list
	 */
	protected List<T> getFilteredRows()
	{
		if (filtered == null)
		{
			Object value = filter != null ? filter.getObject() : null;
			if (value == null)
			{
				filtered = getRows();
			}
			else
			{
				filtered = new ArrayList<>();
				for (T row : getRows())
				{
					if (matches.test(row, value))
					{
						filtered.add(row);
					}
				}
			}
		}
		return filtered;
	}

	/**
	 * @return the rows the filter lets through, sorted by the current {@link #getSort() sort}, a
	 *         copy the list of the model is left alone by
	 */
	protected List<T> getSortedRows()
	{
		List<T> sorted = new ArrayList<>(getFilteredRows());
		SortParam<S> sort = getSort();
		Order<T> order = sort != null ? orders.get(sort.getProperty()) : null;
		if (order != null)
		{
			sorted.sort(order.comparator(sort.isAscending()));
		}
		return sorted;
	}

	@Override
	public Iterator<? extends T> iterator(long first, long count)
	{
		List<T> sorted = getSortedRows();
		int from = (int)Math.min(Math.max(first, 0), sorted.size());
		int to = (int)Math.min(sorted.size(), from + Math.max(count, 0));
		return sorted.subList(from, to).iterator();
	}

	@Override
	public long size()
	{
		return getFilteredRows().size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public K keyOf(T row)
	{
		K id = key != null ? key.apply(row) : ((IIdentifiable<K>)row).getId();
		return Objects.requireNonNull(id, () -> "the key of " + row);
	}

	/**
	 * Finds the row in the current list, filtered out or not.
	 */
	@Override
	public T findByKey(K key)
	{
		if (key == null)
		{
			return null;
		}
		if (byKey == null)
		{
			byKey = new HashMap<>();
			for (T row : getRows())
			{
				byKey.putIfAbsent(keyOf(row), row);
			}
		}
		return byKey.get(key);
	}

	/**
	 * Detaches the models of the rows and of the filter: the next request reads the rows again.
	 */
	@Override
	public void detach()
	{
		all = null;
		filtered = null;
		byKey = null;
		rows.detach();
		if (filter != null)
		{
			filter.detach();
		}
	}

	/**
	 * @return the locale texts are sorted in: the session's, or the default one outside a request
	 */
	protected Locale locale()
	{
		return Session.exists() ? Session.get().getLocale() : Locale.getDefault();
	}

	/**
	 * @param locale
	 *            the locale texts are sorted in
	 * @return the collator comparing texts by {@link #sortByText(Object, SerializableFunction)};
	 *         by default the {@link Collator} of the locale
	 */
	protected Collator collator(Locale locale)
	{
		return Collator.getInstance(locale);
	}

	private ListDynamicDataProvider<T, K, S> order(S property, Order<T> order)
	{
		orders.put(Args.notNull(property, "property"), order);
		return this;
	}

	/**
	 * @return the rows compared by a value, the rows with a {@code null} value last
	 */
	private static <T, U> Comparator<T> byValue(SerializableFunction<? super T, ? extends U> value,
		Comparator<? super U> order)
	{
		return (one, other) -> {
			U a = value.apply(one);
			U b = value.apply(other);
			if (a == null || b == null)
			{
				return a == null ? (b == null ? 0 : 1) : -1;
			}
			return order.compare(a, b);
		};
	}

	/**
	 * The order of the rows by a sort property, ascending or descending.
	 */
	@FunctionalInterface
	private interface Order<T> extends Serializable
	{
		Comparator<T> comparator(boolean ascending);
	}
}
