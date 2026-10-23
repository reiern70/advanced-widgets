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
package com.antiliacode.widgets.modal;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.button.ButtonBehavior.Size;
import com.antiliacode.widgets.button.ButtonBehavior.Variant;
import com.antiliacode.widgets.icon.IIcon;

/**
 * A button of the footer of a {@link Modal} closing it as its close button does, a Cancel button:
 * the browser asks the function of {@link Modal#setCanClose(String)} first, then the server's
 * {@link Modal#onCloseRequest(org.apache.wicket.ajax.AjaxRequestTarget)}. See
 * {@link IModalButtonFactory#close(IModel)}. It looks like an
 * {@link com.antiliacode.widgets.button.AjaxActionButton}, small by default.
 *
 * @since 11.0.0
 */
public class ModalCloseButton extends WebMarkupContainer
{
	private static final long serialVersionUID = 1L;

	/** The attribute marking the elements that close their dialog, in the browser. */
	public static final String CLOSE_ATTRIBUTE = "data-modal-close";

	private final IModel<String> label;

	private final ButtonBehavior button = new ButtonBehavior(Variant.SECONDARY, Size.SMALL);

	private IIcon icon;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 */
	public ModalCloseButton(String id, IModel<String> label)
	{
		super(id);
		this.label = label instanceof IComponentAssignedModel<String> assigned
			? assigned.wrapOnAssignment(this) : label;
		add(button);
	}

	/**
	 * @param icon
	 *            the icon shown before the text, {@code null} for none
	 * @return {@code this}
	 */
	public ModalCloseButton setIcon(IIcon icon)
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
	 * @param variant
	 *            what the button stands for; secondary by default
	 * @return {@code this}
	 */
	public ModalCloseButton setVariant(Variant variant)
	{
		button.setVariant(variant);
		return this;
	}

	/**
	 * @param size
	 *            how big the button is; small by default
	 * @return {@code this}
	 */
	public ModalCloseButton setSize(Size size)
	{
		button.setSize(size);
		return this;
	}

	/**
	 * @param iconOnly
	 *            whether the button shows its icon only, its text being the tooltip
	 * @return {@code this}
	 */
	public ModalCloseButton setIconOnly(boolean iconOnly)
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
		tag.put(CLOSE_ATTRIBUTE, "");
		if (button.isIconOnly())
		{
			tag.put("title", text());
			tag.put("aria-label", text());
		}
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag,
			ButtonBehavior.body(icon, text(), button.isIconOnly()));
	}

	private String text()
	{
		String text = label != null ? label.getObject() : null;
		return text != null ? text : "";
	}

	@Override
	protected void onDetach()
	{
		if (label != null)
		{
			label.detach();
		}
		super.onDetach();
	}
}
