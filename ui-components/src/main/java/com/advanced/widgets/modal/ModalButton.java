/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.modal;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

/**
 * A button of the footer of a {@link Modal} doing something in an Ajax request; see
 * {@link IModalButtonFactory}.
 *
 * @since 11.0.0
 */
public abstract class ModalButton extends AjaxLink<Void>
{
	private static final long serialVersionUID = 1L;

	/** The class of the primary button of a dialog. */
	public static final String PRIMARY_CLASS = "modal-dialog-button-primary";

	private boolean primary;

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 */
	public ModalButton(String id, IModel<String> label)
	{
		super(id);
		setBody(label);
	}

	/**
	 * @param primary
	 *            whether this is the primary button, in the color of the header
	 * @return {@code this}
	 */
	public ModalButton setPrimary(boolean primary)
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

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (primary)
		{
			tag.append("class", PRIMARY_CLASS, " ");
		}
	}
}
