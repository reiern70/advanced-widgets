/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.model.IModel;

/**
 * An autocomplete choosing many values: each chosen value is shown as a chip with an x removing
 * it, and Backspace in the empty text field removes the last one. The model is a collection,
 * which is replaced by a new list holding the chosen values. See {@link AbstractAutocomplete}.
 *
 * @param <T>
 *            the type of the values
 * @since 11.0.0
 */
public class MultiAutocomplete<T> extends AbstractAutocomplete<T, Collection<T>>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}
	 * @param model
	 *            the chosen values
	 * @param provider
	 *            supplies the suggestions
	 */
	public MultiAutocomplete(String id, IModel<? extends Collection<T>> model,
		IAutocompleteProvider<T> provider)
	{
		super(id, cast(model), provider);
	}

	@SuppressWarnings("unchecked")
	private static <T> IModel<Collection<T>> cast(IModel<? extends Collection<T>> model)
	{
		return (IModel<Collection<T>>)model;
	}

	@Override
	protected boolean isMultiple()
	{
		return true;
	}

	@Override
	protected List<T> getChosen()
	{
		return listOf(getModelObject());
	}

	@Override
	protected Collection<T> toModelObject(List<T> values)
	{
		return new ArrayList<>(values);
	}
}
