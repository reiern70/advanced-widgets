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
package com.antiliacode.widgets.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.IIcon;

/**
 * A button of the bar of a {@link Wizard} submitting the form of its body in an Ajax request:
 * {@link #onSubmit(AjaxRequestTarget)} is called once the fields of the body were valid and their
 * values were put into the object. When one is not valid, the wizard repaints itself, its
 * {@link Wizard#onError(AjaxRequestTarget)}, so the fields show their errors. It looks like an
 * {@link com.antiliacode.widgets.button.AjaxActionButton AjaxActionButton}: a text, an icon and a
 * text, or an icon only, in a variant. See {@link IWizardButtonFactory}.
 *
 * @since 10.11.0
 */
public abstract class WizardSubmitButton extends AjaxButton
{
	private static final long serialVersionUID = 1L;

	private final ButtonBehavior button = new ButtonBehavior(Variant.SECONDARY, Size.MEDIUM);

	private IIcon icon;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 * @param form
	 *            the form submitted, that of the wizard
	 */
	public WizardSubmitButton(String id, IModel<String> label, Form<?> form)
	{
		super(id, label, form);
		add(button);
	}

	@Override
	protected abstract void onSubmit(AjaxRequestTarget target);

	@Override
	protected void onError(AjaxRequestTarget target)
	{
		Wizard<?> wizard = findParent(Wizard.class);
		if (wizard != null)
		{
			wizard.onError(target);
		}
	}

	/**
	 * @param variant
	 *            what the button stands for
	 * @return {@code this}
	 */
	public WizardSubmitButton setVariant(Variant variant)
	{
		button.setVariant(variant);
		return this;
	}

	/**
	 * @param size
	 *            how big the button is
	 * @return {@code this}
	 */
	public WizardSubmitButton setSize(Size size)
	{
		button.setSize(size);
		return this;
	}

	/**
	 * @param icon
	 *            the icon shown before the text, {@code null} for none
	 * @return {@code this}
	 */
	public WizardSubmitButton setIcon(IIcon icon)
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
	public WizardSubmitButton setIconOnly(boolean iconOnly)
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
