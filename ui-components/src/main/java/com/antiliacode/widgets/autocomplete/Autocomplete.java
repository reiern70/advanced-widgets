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
package com.antiliacode.widgets.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

/**
 * An autocomplete choosing one value: the chosen value is shown as a chip with an x clearing it,
 * and typing again replaces it. See {@link AbstractAutocomplete}; {@link MultiAutocomplete}
 * chooses many.
 *
 * @param <T>
 *            the type of the value
 * @since 11.0.0
 */
public class Autocomplete<T> extends AbstractAutocomplete<T, T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}
	 * @param model
	 *            the chosen value
	 * @param provider
	 *            supplies the suggestions
	 */
	public Autocomplete(String id, IModel<T> model, IAutocompleteProvider<T> provider)
	{
		super(id, model, provider);
	}

	/**
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}, whose model is
	 *            inherited, for example from a {@code CompoundPropertyModel}
	 * @param provider
	 *            supplies the suggestions
	 */
	public Autocomplete(String id, IAutocompleteProvider<T> provider)
	{
		this(id, null, provider);
	}

	@Override
	protected boolean isMultiple()
	{
		return false;
	}

	@Override
	protected List<T> getChosen()
	{
		List<T> chosen = new ArrayList<>();
		T value = getModelObject();
		if (value != null)
		{
			chosen.add(value);
		}
		return chosen;
	}

	@Override
	protected T toModelObject(List<T> values)
	{
		return values.isEmpty() ? null : values.get(0);
	}
}
