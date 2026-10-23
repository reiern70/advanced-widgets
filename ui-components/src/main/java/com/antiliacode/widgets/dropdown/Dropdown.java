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
package com.antiliacode.widgets.dropdown;

import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 * A {@link DropDownChoice} shown as a {@link DropdownBehavior drop-down} that is opened, filtered
 * and chosen from in the browser. Attach it to a {@code <select>}.
 *
 * @param <T>
 *            the type of the choices
 * @since 11.0.0
 */
public class Dropdown<T> extends DropDownChoice<T>
{
	private static final long serialVersionUID = 1L;

	private final DropdownBehavior dropdown = new DropdownBehavior();

	/**
	 * A drop-down whose model is inherited, for example from a {@code CompoundPropertyModel}.
	 *
	 * @param id
	 *            component id
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public Dropdown(String id, List<? extends T> choices, IChoiceRenderer<? super T> renderer)
	{
		super(id, choices, renderer);
		add(dropdown);
	}

	/**
	 * A drop-down whose model is inherited, for example from a {@code CompoundPropertyModel}.
	 *
	 * @param id
	 *            component id
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public Dropdown(String id, IModel<? extends List<? extends T>> choices,
		IChoiceRenderer<? super T> renderer)
	{
		super(id, choices, renderer);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen value
	 * @param choices
	 *            the choices
	 */
	public Dropdown(String id, IModel<T> model, IModel<? extends List<? extends T>> choices)
	{
		super(id, model, choices);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen value
	 * @param choices
	 *            the choices
	 */
	public Dropdown(String id, IModel<T> model, List<? extends T> choices)
	{
		super(id, model, choices);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen value
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public Dropdown(String id, IModel<T> model, List<? extends T> choices,
		IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen value
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public Dropdown(String id, IModel<T> model, IModel<? extends List<? extends T>> choices,
		IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
		add(dropdown);
	}

	/**
	 * @return the behavior drawing the drop-down, to set its placeholder and filter
	 */
	public DropdownBehavior getDropdownBehavior()
	{
		return dropdown;
	}
}
