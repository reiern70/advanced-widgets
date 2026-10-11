/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.register.ConfirmationMail;
import com.antilia.widgets.demo.register.RegisterPage;

/**
 * Signs a visitor in with the name of an account and its password; then goes on to the demo the
 * visitor wanted to see, or to the {@link HomePage}. After {@value SignInAttempts#BEFORE_CAPTCHA}
 * failed attempts in the session, or on the account, it asks for the letters of a captcha too.
 * A visitor who registered signs in only once the email is confirmed; until then signing in
 * sends a new {@link ConfirmationMail} when the last one is {@link Accounts#RESEND_AFTER} old.
 */
public class SignInPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The letters of the captcha: none that look like another, as 0 and O or 1 and l. */
	private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	/** How many letters the captcha has. */
	static final int CAPTCHA_LENGTH = 6;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final Model<String> name = Model.of();

	private final Model<String> challenge = Model.of(newChallenge());

	private final CaptchaImageResource captchaImage = new CaptchaImageResource(challenge, 40, 20);

	public SignInPage()
	{
		Model<String> password = Model.of();
		Model<String> answer = Model.of();

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				SignInAttempts attempts = attempts();
				DemoSession session = DemoSession.get();
				String typed = name.getObject();
				boolean captchaAsked = get("captcha").isVisibleInHierarchy();
				if (captchaAsked && !challenge.getObject().equalsIgnoreCase(
					answer.getObject() == null ? "" : answer.getObject().strip()))
				{
					fail(typed, "captchaWrong");
					return;
				}
				Optional<User> user = Accounts.get().authenticate(typed, password.getObject());
				if (user.isEmpty())
				{
					fail(typed, "wrong");
					return;
				}
				attempts.succeeded(typed);
				if (!user.get().isValidated())
				{
					notValidated(user.get());
					return;
				}
				session.signIn(user.get().getName(), user.get().getRole());
				continueToOriginalDestination();
				setResponsePage(HomePage.class);
			}

			/**
			 * Turns away an account whose email is not confirmed, sending a new link if the
			 * last one is old enough.
			 */
			private void notValidated(User user)
			{
				Accounts accounts = Accounts.get();
				if (accounts.isValidationResent(user))
				{
					ConfirmationMail.send(user, accounts.renewValidation(user));
				}
				error(new StringResourceModel("notValidated", SignInPage.this)
					.setParameters(user.getName()).getString());
			}

			private void fail(String typed, String key)
			{
				DemoSession.get().failedSignIn();
				attempts().failed(typed);
				error(getString(key));
				renewCaptcha();
				answer.setObject(null);
			}
		};
		add(form);

		form.add(new RequiredTextField<>("name", name).setLabel(new ResourceModel("name")));
		form.add(new PasswordTextField("password", password).setLabel(new ResourceModel("password")));

		WebMarkupContainer captcha = new WebMarkupContainer("captcha")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(isCaptchaNeeded());
			}
		};
		form.add(captcha);
		captcha.add(new NonCachingImage("image", captchaImage));
		captcha.add(new Link<Void>("another")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
				renewCaptcha();
			}
		});
		captcha.add(new RequiredTextField<>("answer", answer)
			.setLabel(new ResourceModel("captcha")));

		add(new BookmarkablePageLink<>("registerInstead", RegisterPage.class));
	}

	/**
	 * @return whether the visitor has to read a captcha: after failing to sign in too often, in
	 *         the session or on the account typed
	 */
	boolean isCaptchaNeeded()
	{
		return DemoSession.get().getFailedSignIns() >= SignInAttempts.BEFORE_CAPTCHA ||
			attempts().needsCaptcha(name.getObject());
	}

	/**
	 * @return the letters the captcha shows, for tests
	 */
	String getChallenge()
	{
		return challenge.getObject();
	}

	private void renewCaptcha()
	{
		challenge.setObject(newChallenge());
		captchaImage.invalidate();
	}

	private static String newChallenge()
	{
		StringBuilder letters = new StringBuilder(CAPTCHA_LENGTH);
		for (int i = 0; i < CAPTCHA_LENGTH; i++)
		{
			letters.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
		}
		return letters.toString().toUpperCase(Locale.ROOT);
	}

	private static SignInAttempts attempts()
	{
		return ((WicketApplication)WicketApplication.get()).getSignInAttempts();
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
