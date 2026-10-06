/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.theme;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.advanced.widgets.theme.Theme;

/**
 * A drop-down choosing the {@link Theme} of the extensions components of an example, applied via
 * Ajax by {@link #onThemeChanged(AjaxRequestTarget)}.
 */
public abstract class ThemeChoice extends DropDownChoice<Theme>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id, attached to a {@code <select>}
	 * @param theme
	 *            the chosen theme
	 */
	public ThemeChoice(String id, IModel<Theme> theme)
	{
		super(id, theme, List.of(Theme.values()));
		setChoiceRenderer(new EnumChoiceRenderer<>(this));
		setRequired(true);
		add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onThemeChanged(target);
			}
		});
	}

	/**
	 * Called after the user chose another theme; re-render the themed components here.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onThemeChanged(AjaxRequestTarget target);
}
