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
 * {@value SignedInUsers#MAXIMUM} other users are signed in; then goes on to the demo the
 * visitor wanted to see, or to the {@link HomePage}. After {@value SignInAttempts#BEFORE_CAPTCHA}
 * failed attempts in the session, or on the account, it asks for a {@link Captcha} too.
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
				if (captcha.isVisibleInHierarchy() && !captcha.isSolved())
				{
					fail(typed, captcha.getWrongMessage(), captcha.getAnswerField());
					return;
				}
				Optional<User> user = Accounts.get().authenticate(typed, password.getObject());
				if (user.isEmpty())
				{
					fail(typed, getString("wrong"), passwordField);
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
					error(new StringResourceModel("full", SignInPage.this)
						.setParameters(SignedInUsers.MAXIMUM).getString());
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
				Accounts accounts = Accounts.get();
				if (accounts.isValidationResent(user))
				{
					ConfirmationMail.send(user, accounts.renewValidation(user));
				}
				error(new StringResourceModel("notValidated", SignInPage.this)
					.setParameters(user.getName()).getString());
			}

			private void fail(String typed, String message, FormComponent<?> field)
			{
				DemoSession.get().failedSignIn();
				attempts().failed(typed);
				error(message);
				captcha.renew();
				wrong = field;
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
