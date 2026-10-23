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
package com.antiliacode.widgets.palette;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

/**
 * A {@link ListMultipleChoice} shown as a {@link PaletteBehavior palette}: the available choices
 * on the left, the chosen ones on the right and arrows between them, all in the browser. When the
 * {@link #setOrdered(boolean) order matters} the chosen ones can be moved up and down, also by
 * dragging them, and the model keeps them in that order; give it a {@code List} then. The rows
 * can {@link #setNumbered(boolean) show their numbers}, and the titles over the lists can be set
 * for each palette. Attach it to a {@code <select>}; the {@code multiple} attribute is added.
 *
 * @param <T>
 *            the type of the choices
 * @since 11.0.0
 */
public class Palette<T> extends ListMultipleChoice<T>
{
	private static final long serialVersionUID = 1L;

	private final PaletteBehavior palette = new PaletteBehavior();

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the chosen values
	 * @param choices
	 *            the choices
	 */
	public Palette(String id, IModel<? extends Collection<T>> model, List<? extends T> choices)
	{
		super(id, model, choices);
		add(palette);
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
	public Palette(String id, IModel<? extends Collection<T>> model, List<? extends T> choices,
		IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
		add(palette);
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
	public Palette(String id, IModel<? extends Collection<T>> model,
		IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer)
	{
		super(id, model, choices, renderer);
		add(palette);
	}

	/**
	 * @param ordered
	 *            whether the order of the chosen ones matters: they keep the order they were chosen
	 *            in and can be moved up and down
	 * @return {@code this}
	 */
	public Palette<T> setOrdered(boolean ordered)
	{
		palette.setOrdered(ordered);
		return this;
	}

	/**
	 * @return whether the order of the chosen ones matters
	 */
	public boolean isOrdered()
	{
		return palette.isOrdered();
	}

	/**
	 * @param numbered
	 *            whether the rows of both lists show their number, from 1, before their text
	 * @return {@code this}
	 */
	public Palette<T> setNumbered(boolean numbered)
	{
		palette.setNumbered(numbered);
		return this;
	}

	/**
	 * @return whether the rows of both lists show their number
	 */
	public boolean isNumbered()
	{
		return palette.isNumbered();
	}

	/**
	 * @param availableTitle
	 *            the title of the list of the available choices, {@code null} for that of the
	 *            bundles, {@code Palette.availableTitle}
	 * @return {@code this}
	 */
	public Palette<T> setAvailableTitle(IModel<String> availableTitle)
	{
		palette.setAvailableTitle(availableTitle);
		return this;
	}

	/**
	 * @param selectedTitle
	 *            the title of the list of the chosen ones, {@code null} for that of the bundles,
	 *            {@code Palette.selectedTitle}
	 * @return {@code this}
	 */
	public Palette<T> setSelectedTitle(IModel<String> selectedTitle)
	{
		palette.setSelectedTitle(selectedTitle);
		return this;
	}

	/**
	 * @param filterable
	 *            whether the available ones can have a filter, shown from the
	 *            {@link PaletteBehavior#setFilterThreshold(int) threshold} on; {@code true} unless
	 *            set otherwise
	 * @return {@code this}
	 */
	public Palette<T> setFilterable(boolean filterable)
	{
		palette.setFilterable(filterable);
		return this;
	}

	/**
	 * @return whether the available ones can have a filter
	 */
	public boolean isFilterable()
	{
		return palette.isFilterable();
	}

	/**
	 * @param matcher
	 *            how the filter matches the choices, underlining what it matched:
	 *            {@link PaletteMatcher#ANYWHERE} unless set otherwise
	 * @return {@code this}
	 * @see PaletteBehavior#setMatcher(IPaletteMatcher)
	 */
	public Palette<T> setMatcher(IPaletteMatcher matcher)
	{
		palette.setMatcher(matcher);
		return this;
	}

	/**
	 * @return how the filter matches the choices
	 */
	public IPaletteMatcher getMatcher()
	{
		return palette.getMatcher();
	}

	/**
	 * @param pageSize
	 *            how many rows a list draws at first, and adds each time it is scrolled to its
	 *            end, {@link PaletteBehavior#DEFAULT_PAGE_SIZE} unless set otherwise; 0 draws
	 *            them all
	 * @return {@code this}
	 * @see PaletteBehavior#setPageSize(int)
	 */
	public Palette<T> setPageSize(int pageSize)
	{
		palette.setPageSize(pageSize);
		return this;
	}

	/**
	 * @return how many rows a list draws at first, and adds each time it is scrolled to its end
	 */
	public int getPageSize()
	{
		return palette.getPageSize();
	}

	/**
	 * @return the behavior drawing the palette, to set its filter
	 */
	public PaletteBehavior getPaletteBehavior()
	{
		return palette;
	}
}
