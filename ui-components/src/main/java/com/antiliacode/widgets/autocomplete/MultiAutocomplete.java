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
