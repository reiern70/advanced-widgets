/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

import com.advanced.widgets.demo.DemoPage;

/**
 * A demo page working on the users: shows the selected user and the feedback messages above
 * the demo.
 */
public abstract class UsersPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private User selected;

	protected UsersPage()
	{
		add(new Label("selectedLabel", this::getSelectedUserLabel));
		add(new FeedbackPanel("feedback"));
	}

	private String getSelectedUserLabel()
	{
		return selected == null ? getString("noUserSelected")
			: selected.getFirstName() + " " + selected.getLastName();
	}

	/**
	 * @return the selected user, {@code null} if there is none
	 */
	public User getSelected()
	{
		return selected;
	}

	/**
	 * @param selected
	 *            the selected user, {@code null} for none
	 */
	public void setSelected(User selected)
	{
		this.selected = selected;
	}
}
