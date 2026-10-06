/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

import com.advanced.widgets.demo.DemoPage;

/**
 * A demo page working on the contacts: shows the selected contact and the feedback messages above
 * the demo.
 */
public abstract class ContactsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private Contact selected;

	protected ContactsPage()
	{
		add(new Label("selectedLabel", this::getSelectedContactLabel));
		add(new FeedbackPanel("feedback"));
	}

	private String getSelectedContactLabel()
	{
		return selected == null ? getString("noContactSelected")
			: selected.getFirstName() + " " + selected.getLastName();
	}

	/**
	 * @return the selected contact, {@code null} if there is none
	 */
	public Contact getSelected()
	{
		return selected;
	}

	/**
	 * @param selected
	 *            the selected contact, {@code null} for none
	 */
	public void setSelected(Contact selected)
	{
		this.selected = selected;
	}
}
