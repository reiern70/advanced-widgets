/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import com.antilia.widgets.demo.DemoPage;

/**
 * A demo page working on the users: keeps the selected user.
 */
public abstract class UsersPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private User selected;

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
