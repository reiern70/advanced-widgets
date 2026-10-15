/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * The errors of a form, listed in the form in the order of its fields. The notifications of the
 * page leave them to it, so none is lost when a form has more errors than a stack of
 * notifications shows; the other messages, such as a success, are still notifications. Shown only
 * when there is an error.
 */
public class FormErrors extends FeedbackPanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 */
	public FormErrors(String id)
	{
		super(id, new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(anyMessage());
	}
}
