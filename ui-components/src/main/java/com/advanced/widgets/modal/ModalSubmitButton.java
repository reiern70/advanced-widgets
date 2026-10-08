/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.modal;

import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * A button of the footer of a {@link Modal} submitting a form of its body in an Ajax request,
 * although the footer is outside the form; see {@link IModalButtonFactory}. Made the
 * {@link #setDefault(boolean) default button} of the form, it is the one Enter in a field clicks.
 *
 * @since 11.0.0
 */
public abstract class ModalSubmitButton extends AjaxButton
{
	private static final long serialVersionUID = 1L;

	private boolean primary;

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
	}

	/**
	 * @param primary
	 *            whether this is the primary button, in the color of the header
	 * @return {@code this}
	 */
	public ModalSubmitButton setPrimary(boolean primary)
	{
		this.primary = primary;
		return this;
	}

	/**
	 * @return whether this is the primary button, in the color of the header
	 */
	public boolean isPrimary()
	{
		return primary;
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
		if (primary)
		{
			tag.append("class", ModalButton.PRIMARY_CLASS, " ");
		}
	}
}
