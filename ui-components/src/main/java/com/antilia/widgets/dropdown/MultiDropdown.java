/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dropdown;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

/**
 * A {@link ListMultipleChoice} shown as a {@link DropdownBehavior drop-down} in which any number
 * of choices are toggled in the browser; its button shows the chosen ones. Attach it to a
 * {@code <select>}; the {@code multiple} attribute is added.
 *
 * @param <T>
 *            the type of the choices
 * @since 11.0.0
 */
public class MultiDropdown<T> extends ListMultipleChoice<T>
{
	private static final long serialVersionUID = 1L;

	private final DropdownBehavior dropdown = new DropdownBehavior();

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen values
	 * @param choices
	 *            the choices
	 */
	public MultiDropdown(String id, IModel<? extends Collection<T>> model,
		List<? extends T> choices)
	{
		super(id, model, choices);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen values
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public MultiDropdown(String id, IModel<? extends Collection<T>> model,
		List<? extends T> choices, IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
		add(dropdown);
	}

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen values
	 * @param choices
	 *            the choices
	 * @param renderer
	 *            renders the choices
	 */
	public MultiDropdown(String id, IModel<? extends Collection<T>> model,
		IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer)
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
