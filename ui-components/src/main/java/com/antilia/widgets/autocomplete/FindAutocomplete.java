/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

/**
 * An autocomplete as a find bar: a chosen value is not kept as a chip, but handed to
 * {@link #onPick(AjaxRequestTarget, Object)} via Ajax, and the text field is empty again for
 * the next one. The page keeps what was picked where it likes, for example as a list of check
 * boxes or rows below the bar, and its provider leaves out the values already there. See
 * {@link AbstractAutocomplete}.
 * <p>
 * It has no model of its own: the hidden input holds the id of the value picked last only for the
 * Ajax request telling it, and sending the form changes nothing.
 *
 * @param <T>
 *            the type of the values
 * @since 11.0.0
 */
public abstract class FindAutocomplete<T> extends AbstractAutocomplete<T, T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id, attached to an {@code <input type="hidden">}
	 * @param provider
	 *            supplies the suggestions
	 */
	public FindAutocomplete(String id, IAutocompleteProvider<T> provider)
	{
		super(id, null, provider);
		add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				T value = getConvertedInput();
				clearInput();
				if (value != null)
				{
					onPick(target, value);
				}
			}

			@Override
			protected boolean getUpdateModel()
			{
				return false;
			}
		});
	}

	/**
	 * Called when the user picked a suggestion.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param value
	 *            the value picked
	 */
	protected abstract void onPick(AjaxRequestTarget target, T value);

	@Override
	protected boolean isMultiple()
	{
		return false;
	}

	@Override
	protected boolean isPick()
	{
		return true;
	}

	/**
	 * @return none: the values picked are the page's
	 */
	@Override
	protected List<T> getChosen()
	{
		return new ArrayList<>();
	}

	@Override
	protected T toModelObject(List<T> values)
	{
		return values.isEmpty() ? null : values.get(0);
	}

	/**
	 * Keeps nothing: the values picked are handed to {@link #onPick(AjaxRequestTarget, Object)}.
	 */
	@Override
	public void updateModel()
	{
	}
}
