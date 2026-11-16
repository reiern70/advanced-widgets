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
import java.util.Objects;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;
import org.danekja.java.util.function.serializable.SerializableFunction;

/**
 * A {@link DropDownChoice} shown as a {@link DropdownBehavior drop-down} that is opened, filtered
 * and chosen from in the browser. Attach it to a {@code <select>}.
 * <p>
 * Its choices may be put in {@link #setGroup(SerializableFunction) groups}, such as the few common
 * ones first and the rest of a long catalog under them: each group is an {@code <optgroup>}, drawn
 * with its label in the open list.
 *
 * @param <T>
 *            the type of the choices
 * @since 11.0.0
 */
public class Dropdown<T> extends DropDownChoice<T>
{
	private static final long serialVersionUID = 1L;

	private final DropdownBehavior dropdown = new DropdownBehavior();

	private SerializableFunction<? super T, String> group;

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

	/**
	 * Puts the choices in groups: the choices next to each other with the same group are drawn in
	 * one {@code <optgroup>} labelled with it, so the choices of a group have to come together, in
	 * the order of the groups. A choice whose group is {@code null} is out of any.
	 *
	 * @param group
	 *            the label of the group of a choice, written escaped, or {@code null}; {@code null}
	 *            for no groups
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public Dropdown<T> setGroup(SerializableFunction<? super T, String> group)
	{
		this.group = group;
		return this;
	}

	/**
	 * @param choice
	 *            a choice
	 * @return the label of the group of the choice, {@code null} when it is in none
	 */
	protected String getGroup(T choice)
	{
		return group == null ? null : group.apply(choice);
	}

	@Override
	protected void appendOptionHtml(AppendingStringBuffer buffer, T choice, int index,
		String selected)
	{
		List<? extends T> choices = getChoices();
		String current = getGroup(choice);
		String previous = index > 0 ? getGroup(choices.get(index - 1)) : null;
		boolean same = index > 0 && Objects.equals(previous, current);
		if (previous != null && !same)
		{
			buffer.append("\n</optgroup>");
		}
		if (current != null && !same)
		{
			buffer.append("\n<optgroup label=\"").append(Strings.escapeMarkup(current)).append("\">");
		}
		super.appendOptionHtml(buffer, choice, index, selected);
		if (current != null && index == choices.size() - 1)
		{
			buffer.append("\n</optgroup>");
		}
	}
}
