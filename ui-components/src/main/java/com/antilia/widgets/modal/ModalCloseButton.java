/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.modal;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * A button of the footer of a {@link Modal} closing it as its close button does, a Cancel button:
 * the browser asks the function of {@link Modal#setCanClose(String)} first, then the server's
 * {@link Modal#onCloseRequest(org.apache.wicket.ajax.AjaxRequestTarget)}. See
 * {@link IModalButtonFactory#close(IModel)}.
 *
 * @since 11.0.0
 */
public class ModalCloseButton extends Label
{
	private static final long serialVersionUID = 1L;

	/** The attribute marking the elements that close their dialog, in the browser. */
	public static final String CLOSE_ATTRIBUTE = "data-modal-close";

	/**
	 * @param id
	 *            the component id
	 * @param label
	 *            the text of the button
	 */
	public ModalCloseButton(String id, IModel<String> label)
	{
		super(id, label);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.put(CLOSE_ATTRIBUTE, "");
	}
}
