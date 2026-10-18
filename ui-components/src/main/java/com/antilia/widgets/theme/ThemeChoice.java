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
package com.antilia.widgets.theme;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.theme.Theme;

/**
 * A drop-down choosing the {@link Theme} of the extensions components of an example, applied via
 * Ajax by {@link #onThemeChanged(AjaxRequestTarget)}. The Ajax is stateless: on a stateless page
 * the choice works on the page made anew from its URL.
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

			@Override
			public boolean getStatelessHint(Component component)
			{
				// keeps a stateless page stateless: the choice works on the page made anew
				return true;
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
