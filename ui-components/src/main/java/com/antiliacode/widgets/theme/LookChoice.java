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
package com.antiliacode.widgets.theme;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 * A drop-down choosing the {@link ILook look} of the components, applied via Ajax by
 * {@link #onLookChanged(AjaxRequestTarget)}, as {@link ThemeChoice} chooses their theme. The Ajax
 * is stateless: on a stateless page the choice works on the page made anew from its URL.
 * <p>
 * A look shows as the string {@code look.<name>}, such as {@code look.bootstrap}, or as its name
 * if there is none: {@code look.bootstrap color:antilia} for a look made of others.
 *
 * @since 11.0.0
 */
public abstract class LookChoice extends DropDownChoice<ILook>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Chooses one of the {@link Look looks of ui-components}.
	 *
	 * @param id
	 *            component id, attached to a {@code <select>}
	 * @param look
	 *            the chosen look
	 */
	public LookChoice(String id, IModel<ILook> look)
	{
		this(id, look, List.of(Look.values()));
	}

	/**
	 * @param id
	 *            component id, attached to a {@code <select>}
	 * @param look
	 *            the chosen look
	 * @param looks
	 *            the looks to choose from, such as looks made of others
	 */
	public LookChoice(String id, IModel<ILook> look, List<? extends ILook> looks)
	{
		super(id, look, looks);
		setChoiceRenderer(new IChoiceRenderer<>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Object getDisplayValue(ILook object)
			{
				return getString("look." + object.getName(), null, object.getName());
			}

			@Override
			public String getIdValue(ILook object, int index)
			{
				return object.getName();
			}
		});
		setRequired(true);
		add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onLookChanged(target);
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
	 * Called after the user chose another look; re-render the components in the look here, or
	 * the page, as a look may bring a style sheet the page lacks.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onLookChanged(AjaxRequestTarget target);
}
