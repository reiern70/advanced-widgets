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
package com.antilia.widgets.modal;

import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.icon.IIcon;

/**
 * A button of the footer of a {@link Modal} submitting a form of its body in an Ajax request,
 * although the footer is outside the form; see {@link IModalButtonFactory}. Made the
 * {@link #setDefault(boolean) default button} of the form, it is the one Enter in a field clicks.
 * It looks like an {@link com.antilia.widgets.button.AjaxActionButton}, small by default: a text,
 * an icon and a text, or an icon only, in a variant.
 *
 * @since 11.0.0
 */
public abstract class ModalSubmitButton extends AjaxButton
{
	private static final long serialVersionUID = 1L;

	private final ButtonBehavior button = new ButtonBehavior(Variant.SECONDARY, Size.SMALL);

	private IIcon icon;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 * @param form
	 *            the form submitted
	 */
	public ModalSubmitButton(String id, IModel<String> label, Form<?> form)
	{
		super(id, label, form);
		add(button);
	}

	/**
	 * @param primary
	 *            whether this is the primary button, in the primary color of the theme
	 * @return {@code this}
	 */
	public ModalSubmitButton setPrimary(boolean primary)
	{
		return setVariant(primary ? Variant.PRIMARY : Variant.SECONDARY);
	}

	/**
	 * @return whether this is the primary button, in the primary color of the theme
	 */
	public boolean isPrimary()
	{
		return button.getVariant() == Variant.PRIMARY;
	}

	/**
	 * @param variant
	 *            what the button stands for
	 * @return {@code this}
	 */
	public ModalSubmitButton setVariant(Variant variant)
	{
		button.setVariant(variant);
		return this;
	}

	/**
	 * @param size
	 *            how big the button is; small by default
	 * @return {@code this}
	 */
	public ModalSubmitButton setSize(Size size)
	{
		button.setSize(size);
		return this;
	}

	/**
	 * @param icon
	 *            the icon shown before the text, {@code null} for none
	 * @return {@code this}
	 */
	public ModalSubmitButton setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @return the icon shown before the text, {@code null} if there is none
	 */
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param iconOnly
	 *            whether the button shows its icon only, its text being the tooltip
	 * @return {@code this}
	 */
	public ModalSubmitButton setIconOnly(boolean iconOnly)
	{
		button.setIconOnly(iconOnly);
		return this;
	}

	/**
	 * @return the behavior drawing the button, holding its variant and size
	 */
	public ButtonBehavior getButtonBehavior()
	{
		return button;
	}

	/**
	 * @param asDefault
	 *            whether Enter in a field of the form clicks this button
	 * @return {@code this}
	 */
	public ModalSubmitButton setDefault(boolean asDefault)
	{
		getForm().setDefaultButton(asDefault ? this : null);
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (button.isIconOnly())
		{
			tag.put("title", getDefaultModelObjectAsString());
			tag.put("aria-label", getDefaultModelObjectAsString());
		}
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag,
			ButtonBehavior.body(icon, getModelObject(), button.isIconOnly()));
	}
}
