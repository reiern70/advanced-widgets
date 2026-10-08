/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.autocomplete;

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
