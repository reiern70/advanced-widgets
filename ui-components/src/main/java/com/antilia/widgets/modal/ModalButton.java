/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.modal;

import org.apache.wicket.model.IModel;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;

/**
 * A button of the footer of a {@link Modal} doing something in an Ajax request; see
 * {@link IModalButtonFactory}. It is an {@link AjaxActionButton}, small by default: a text, an
 * icon and a text, or an icon only, in a variant.
 *
 * @since 11.0.0
 */
public abstract class ModalButton extends AjaxActionButton
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 */
	public ModalButton(String id, IModel<String> label)
	{
		super(id, label);
		setSize(Size.SMALL);
	}

	/**
	 * @param primary
	 *            whether this is the primary button, in the primary color of the theme
	 * @return {@code this}
	 */
	public ModalButton setPrimary(boolean primary)
	{
		setVariant(primary ? Variant.PRIMARY : Variant.SECONDARY);
		return this;
	}

	/**
	 * @return whether this is the primary button, in the primary color of the theme
	 */
	public boolean isPrimary()
	{
		return getButtonBehavior().getVariant() == Variant.PRIMARY;
	}
}
