/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.register;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.account.SignInPage;
import com.antilia.widgets.demo.data.User;

/**
 * Where the link of the {@link ConfirmationMail} leads: validates the email of the account, so
 * that it opens the demos once the visitor signs in. It does not sign in by itself, as programs
 * checking the links of emails open them too. The link works once and for
 * {@link Accounts#VALIDATION_TIME}; a link that does not work says so.
 */
public class EmailValidationPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The page parameter holding the token of the link. */
	public static final String TOKEN = "token";

	/**
	 * @param parameters
	 *            the token of the link
	 */
	public EmailValidationPage(PageParameters parameters)
	{
		super(parameters);
		User user = Accounts.get().validate(parameters.get(TOKEN).toOptionalString())
			.orElse(null);

		WebMarkupContainer validated = new WebMarkupContainer("validated");
		validated.setVisible(user != null);
		add(validated);
		validated.add(new Label("email", new StringResourceModel("validated.text", this)
			.setParameters(Model.of(user == null ? "" : user.getName()))));
		validated.add(new BookmarkablePageLink<>("signIn", SignInPage.class));

		WebMarkupContainer invalid = new WebMarkupContainer("invalid");
		invalid.setVisible(user == null);
		add(invalid);
		invalid.add(new BookmarkablePageLink<>("signIn", SignInPage.class));
		invalid.add(new BookmarkablePageLink<>("home", HomePage.class));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
