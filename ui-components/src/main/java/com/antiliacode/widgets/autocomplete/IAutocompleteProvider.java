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
package com.antiliacode.widgets.autocomplete;

import java.util.List;

import org.apache.wicket.model.IDetachable;

/**
 * Supplies the suggestions of an {@link Autocomplete} and resolves the chosen values back from
 * their ids.
 *
 * @param <T>
 *            the type of the values
 * @since 11.0.0
 */
public interface IAutocompleteProvider<T> extends IDetachable
{
	/**
	 * @param term
	 *            what the user typed, trimmed
	 * @param max
	 *            the most values to return
	 * @return the values matching the term, best first
	 */
	List<T> search(String term, int max);

	/**
	 * @return the id of the value, which the browser sends back when it is chosen
	 */
	String getId(T value);

	/**
	 * @return the value of an id, {@code null} if there is none any more
	 */
	T fromId(String id);

	/**
	 * @return the text of the chip showing the chosen value
	 */
	String getLabel(T value);

	@Override
	default void detach()
	{
	}
}
