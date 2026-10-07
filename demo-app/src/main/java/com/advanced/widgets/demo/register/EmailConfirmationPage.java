/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.register;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.advanced.widgets.demo.BasePage;
import com.advanced.widgets.demo.HomePage;

/**
 * Tells a visitor who registered on the {@link RegisterPage} to confirm the email address. A mock
 * for now: no email is sent.
 */
public class EmailConfirmationPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param email
	 *            the address the confirmation would be sent to
	 */
	public EmailConfirmationPage(String email)
	{
		add(new Label("sent", new StringResourceModel("sent", this).setParameters(Model.of(email))));
		add(new BookmarkablePageLink<>("backHome", HomePage.class));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
