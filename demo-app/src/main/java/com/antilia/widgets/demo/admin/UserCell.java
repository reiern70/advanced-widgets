/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.button.AjaxActionButton;
import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.icon.SvgIcon;

/**
 * The user of an access and, when it is an account, a button telling all about it.
 */
abstract class UserCell extends Panel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param userName
	 *            the user of the access: an account, or a visitor who did not sign in
	 */
	UserCell(String id, String userName)
	{
		super(id);
		add(new Label("name", userName));
		Long account = Access.UNKNOWN.equals(userName) || Access.ROBOT.equals(userName) ? null
			: Accounts.get().find(userName).map(User::getId).orElse(null);
		add(new AjaxActionButton("info", new ResourceModel("userInfo"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				onInfo(target, this, account);
			}
		}.setIcon(SvgIcon.CIRCLE_INFO)
			.setIconOnly(true)
			.setSize(ButtonBehavior.Size.SMALL)
			.setVariant(ButtonBehavior.Variant.GHOST)
			.setVisible(account != null));
	}

	/**
	 * Called when the button asks about the account.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param trigger
	 *            the button
	 * @param account
	 *            the id of the account
	 */
	protected abstract void onInfo(AjaxRequestTarget target, Component trigger, Long account);
}
