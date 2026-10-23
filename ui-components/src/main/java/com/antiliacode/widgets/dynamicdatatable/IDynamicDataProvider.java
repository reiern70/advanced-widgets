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
import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;

/**
 * Supplies the rows of a {@link BaseDynamicDataTable} and resolves a row back from its key. It is
 * an {@link IDataProvider}: {@link #iterator(long, long)} and {@link #size()} give the rows of a
 * page and their count, and {@link #model(Object)} wraps a row in a model reloading it by its key.
 * <p>
 * The key is what ties a row in the browser to a row on the server: a click on one of its
 * actions, its selection, a repaint pushed for it. It has to stay stable for as long as the row
 * exists. In the browser the key is a string, {@link #formatKey(Serializable)}; what the browser
 * sends back is turned into a key again by {@link #parseKey(String)}, and has to be treated as
 * untrusted input: it arrives as a request parameter.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys
 * @since 11.0.0
 */
public interface IDynamicDataProvider<T, K extends Serializable> extends IDataProvider<T>
{

	/**
	 * Returns the key of a row. See {@link IIdentifiableDynamicDataProvider} for rows that know
	 * their key.
	 *
	 * @param row
	 *            a row returned by {@link #iterator(long, long)}
	 * @return the key identifying the row, never {@code null}; unique within the table and the
	 *         same for as long as the row exists
	 */
	K keyOf(T row);

	/**
	 * Resolves a row from its key.
	 *
	 * @param key
	 *            the key, possibly one of a row that no longer exists
	 * @return the row, or {@code null} if there is none for the key
	 */
	T findByKey(K key);

	/**
	 * @return the type of the keys, which {@link #parseKey(String)} converts to by default
	 */
	Class<K> getKeyType();

	/**
	 * Writes a key the way the browser shows it: as the row element's {@code data-key} and in its
	 * id {@code <tableId>-row-<key>}. Returns {@link String#valueOf(Object)} by default.
	 *
	 * @param key
	 *            the key
	 * @return the key as a string, which {@link #parseKey(String)} turns into the same key again
	 */
	default String formatKey(K key)
	{
		return String.valueOf(key);
	}

	/**
	 * Turns a key sent by the browser into a key. By default the application's converter for
	 * {@link #getKeyType()} converts it, regardless of the user's locale.
	 *
	 * @param key
	 *            the key as sent by the browser, possibly not a key at all
	 * @return the key, or {@code null} if the string is none
	 */
	default K parseKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		Class<K> type = getKeyType();
		if (type == String.class)
		{
			return type.cast(key);
		}
		try
		{
			return Application.get()
				.getConverterLocator()
				.getConverter(type)
				.convertToObject(key, Locale.ROOT);
		}
		catch (ConversionException e)
		{
			return null;
		}
	}

	/**
	 * Wraps a row in a model that keeps only its key and reloads the row with
	 * {@link #findByKey(Serializable)} once it was detached; the row is {@code null} then if it
	 * no longer exists.
	 */
	@Override
	default IModel<T> model(T row)
	{
		return new RowModel<>(this, row);
	}

	@Override
	default void detach()
	{
	}
}
