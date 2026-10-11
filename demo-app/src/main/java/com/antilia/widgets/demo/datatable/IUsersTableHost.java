/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.io.IClusterable;

import com.antilia.widgets.modal.Modal;

/**
 * What a {@link UsersTablePanel} needs of the page it is on: the selected user, which the page may
 * show, the icons of the actions, and the dialog the edit form shows in, if any.
 */
public interface IUsersTableHost extends IClusterable
{
	/**
	 * @return the selected user, {@code null} if there is none
	 */
	User getSelected();

	/**
	 * Selects a user, or none, showing it where the page shows it.
	 *
	 * @param user
	 *            the user, {@code null} for none
	 * @param target
	 *            the Ajax request target, which the page adds what shows the user to
	 */
	void select(User user, AjaxRequestTarget target);

	/**
	 * @return the icons of the actions, the built-in ones by default
	 */
	default ActionIcons getIcons()
	{
		return ActionIcons.BUILT_IN;
	}

	/**
	 * @return the dialog the edit form and the confirmation of a deletion show in; {@code null},
	 *         as by default, to show them in the table's overlay
	 */
	default Modal getDialog()
	{
		return null;
	}
}
