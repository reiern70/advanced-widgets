/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.demo.account;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.FormErrors;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.register.ConfirmationMail;
import com.antiliacode.widgets.demo.register.RegisterPage;
import com.github.openjson.JSONObject;

/**
 * Signs a visitor in with the name of an account and its password, while fewer than
 * {@value SignedInUsers#MAXIMUM} other registered accounts are signed in, the accounts the demo
 * always has and those the server is started with signing in whenever (see
 * {@link SignedInUsers}), or while one of them has been signed in for longer than
 * {@link SignedInUsers#PLACE_TIME}, whose place it then takes; then goes on to the demo the
 * visitor wanted to see, or to the {@link HomePage}. After {@value SignInAttempts#BEFORE_CAPTCHA} failed attempts in the session,
 * or on the account, it asks for a {@link Captcha} too: an attempt
 * without it, from a session that has not seen it yet, is turned away and shown the captcha,
 * without checking the password. While the account or the address waits after too many failures
 * (see {@link SignInAttempts}), attempts are turned away and told how long to wait.
 * A visitor who registered signs in only once the email is confirmed; until then signing in
 * sends a new {@link ConfirmationMail} when the last one is {@link Accounts#RESEND_AFTER} old.
 * <p>
 * The form is sent via Ajax, so that what went wrong shows without drawing the field of the
 * password again: what was typed in it stays, unless the password was wrong or missing. Without
 * scripts the form is sent as any other, and the password has to be typed again.
 */
public class SignInPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	private final Model<String> name = Model.of();

	private final FormErrors errors;

	private final FormComponent<String> nameField;

	private final PasswordTextField passwordField;

	private final Captcha captcha;

	/**
	 * The field of what turned the last attempt away, the password or the answer of the captcha,
	 * which then has the focus; {@code null} if nothing did.
	 */
	private FormComponent<?> wrong;

	public SignInPage()
	{
		Model<String> password = Model.of();

		Form<Void> form = new Form<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				wrong = null;
				SignInAttempts attempts = attempts();
				DemoSession session = DemoSession.get();
				String typed = name.getObject();
				String address = SignInAttempts.address();
				Duration wait = attempts.retryAfter(typed, address);
				if (wait.isPositive())
				{
					// the password is not checked while the account or the address waits
					error(waitMessage(wait));
					return;
				}
				if (isCaptchaNeeded() && !captcha.isVisibleInHierarchy())
				{
					// a session that did not see the captcha yet, a new one say, is asked it now
					error(getString("captchaNeeded"));
					wrong = captcha.getAnswerField();
					return;
				}
				if (captcha.isVisibleInHierarchy() && !captcha.isSolved())
				{
					session.failedSignIn();
					error(captcha.getWrongMessage());
					captcha.renew();
					wrong = captcha.getAnswerField();
					return;
				}
				Accounts accounts = Accounts.get();
				Optional<User> user = accounts.authenticate(typed, password.getObject());
				if (user.isEmpty())
				{
					session.failedSignIn();
					attempts.failed(typed, address, accounts.find(typed).isPresent());
					error(getString("wrong"));
					captcha.renew();
					wrong = passwordField;
					return;
				}
				attempts.succeeded(typed);
				if (!user.get().isValidated())
				{
					notValidated(user.get());
					return;
				}
				if (!session.signIn(user.get().getName(), user.get().getRole()))
				{
					Duration untilRoom = SignedInUsers.get().untilRoom();
					error(new StringResourceModel("full", SignInPage.this)
						.setParameters(SignedInUsers.MAXIMUM,
							Math.max(1, (untilRoom.toSeconds() + 59) / 60))
						.getString());
					return;
				}
				continueToOriginalDestination();
				setResponsePage(HomePage.class);
			}

			/**
			 * Turns away an account whose email is not confirmed, sending a new link if the
			 * last one is old enough.
			 */
			private void notValidated(User user)
			{
				if (Accounts.get().isValidationResent(user))
				{
					ConfirmationMail.resend(user);
				}
				error(new StringResourceModel("notValidated", SignInPage.this)
					.setParameters(user.getName()).getString());
			}
		};
		add(form);
		form.add(new AjaxFormSubmitBehavior("submit")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				// the Ajax request sends the form, the browser does not
				attributes.setPreventDefault(true);
			}

			@Override
			protected void onAfterSubmit(AjaxRequestTarget target)
			{
				show(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target)
			{
				wrong = null;
				show(target);
			}
		});

		errors = new FormErrors("errors");
		form.add(errors.setOutputMarkupPlaceholderTag(true));

		nameField = new RequiredTextField<>("name", name);
		form.add(nameField.setLabel(new ResourceModel("name")).setOutputMarkupId(true));
		passwordField = new PasswordTextField("password", password);
		form.add(passwordField.setLabel(new ResourceModel("password"))
			.add(new ShowPasswordBehavior()));

		captcha = new Captcha("captcha")
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

		add(new BookmarkablePageLink<>("registerInstead", RegisterPage.class));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		if (DemoSession.get().tellPlaceLost())
		{
			error(new StringResourceModel("placeLost", this)
				.setParameters(SignedInUsers.PLACE_TIME.toMinutes(), SignedInUsers.MAXIMUM)
				.getString());
		}
	}

	/**
	 * Shows what an attempt sent via Ajax changed: the errors, the fields marked as wrong, and the
	 * captcha, asked or renewed. The field of the password is drawn again, empty, only when the
	 * password was wrong or missing; otherwise what was typed in it stays. The focus goes to the
	 * first field in error, or to the field of what turned the attempt away.
	 */
	private void show(AjaxRequestTarget target)
	{
		target.add(errors, nameField, captcha);
		if (wrong == passwordField || !passwordField.isValid())
		{
			target.add(passwordField);
		}
		else
		{
			// what was typed stays, without the mark of the error it had while it was empty
			target.appendJavaScript("document.getElementById(" +
				JSONObject.quote(passwordField.getMarkupId()) +
				").removeAttribute('aria-invalid');");
		}
		Stream.<FormComponent<?>> of(nameField, passwordField, captcha.getAnswerField())
			.filter(field -> field.isVisibleInHierarchy() && !field.isValid())
			.findFirst()
			.or(() -> Optional.ofNullable(wrong))
			.ifPresent(target::focusComponent);
	}

	/**
	 * @return whether the visitor has to solve a captcha: after failing to sign in too often, in
	 *         the session or on the account typed
	 */
	boolean isCaptchaNeeded()
	{
		return DemoSession.get().getFailedSignIns() >= SignInAttempts.BEFORE_CAPTCHA ||
			attempts().needsCaptcha(name.getObject());
	}

	/**
	 * @return the message telling how long to wait before trying again, in seconds or, from two
	 *         minutes on, in minutes, rounded up
	 */
	private String waitMessage(Duration wait)
	{
		long seconds = Math.max(1, (wait.toMillis() + 999) / 1000);
		if (seconds < 120)
		{
			return new StringResourceModel("waitSeconds", this).setParameters(seconds).getString();
		}
		return new StringResourceModel("waitMinutes", this).setParameters((seconds + 59) / 60)
			.getString();
	}

	/**
	 * @return what solves the captcha, for tests
	 */
	String getChallenge()
	{
		return captcha.getSolution();
	}

	/**
	 * @return the captcha, for tests
	 */
	Captcha getCaptcha()
	{
		return captcha;
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
