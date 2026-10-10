/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import java.util.Optional;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.register.RegisterPage;

/**
 * Signs a visitor in with the email registered on the {@link RegisterPage}, or the name of an
 * account the server was started with, and the password; then goes on to the demo the visitor
 * wanted to see, or to the {@link HomePage}.
 */
public class SignInPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	public SignInPage()
	{
		Model<String> name = Model.of();
		Model<String> password = Model.of();

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				Optional<String> account = Accounts.get().authenticate(name.getObject(),
					password.getObject());
				if (account.isEmpty())
				{
					error(getString("wrong"));
					return;
				}
				DemoSession.get().signIn(account.get());
				continueToOriginalDestination();
				setResponsePage(HomePage.class);
			}
		};
		add(form);

		form.add(new RequiredTextField<>("name", name).setLabel(new ResourceModel("name")));
		form.add(new PasswordTextField("password", password).setLabel(new ResourceModel("password")));

		add(new FeedbackPanel("feedback"));
		add(new BookmarkablePageLink<>("registerInstead", RegisterPage.class));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
