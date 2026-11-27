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
import java.util.regex.Pattern;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.FormErrors;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.stats.VisitorCookie;
import com.antiliacode.widgets.demo.stats.Visits;

/**
 * Signs a visitor in, as {@value Accounts#USER}, whose password the page shows, or as the
 * administrator. Stateless: a visitor who does not sign in gets no session. Every attempt asks the
 * {@link Captcha}. After {@value SignInAttempts#MAXIMUM_FAILURES} failures, of the captcha or of
 * the password, the address is blocked for {@link SignInAttempts#BLOCKED} and told how long is
 * left (see {@link SignInAttempts}). At most {@value SignedInUsers#MAXIMUM} sessions are signed in
 * as {@value Accounts#USER} at once, and the page says how many are now; the administrator always
 * signs in (see {@link SignedInUsers}). Then it goes on to the demo the visitor wanted to see, the
 * page parameter {@value #TO}, or to the {@link HomePage}.
 * <p>
 * It says what the site counts of its visitors, see {@link com.antiliacode.widgets.demo.stats.Visits}.
 */
public class SignInPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** The page parameter naming the page to go on to, as a path of the site. */
	public static final String TO = "to";

	/** A path of the site to go on to: relative, of the site's own pages, nothing else. */
	private static final Pattern LOCAL_PATH = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._~/-]{0,199}");

	private final Captcha captcha;

	/**
	 * @param parameters
	 *            the page parameters: {@value #TO}, the path of the page to go on to
	 */
	public SignInPage(PageParameters parameters)
	{
		super(parameters);
		Model<String> name = Model.of();
		Model<String> password = Model.of();
		captcha = new Captcha("captcha");

		StatelessForm<Void> form = new StatelessForm<>("form")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				SignInAttempts attempts = SignInAttempts.get();
				String address = SignInAttempts.address();
				Duration wait = attempts.retryAfter(address);
				if (wait.isPositive())
				{
					// neither the captcha nor the password is checked while the address is blocked
					error(waitMessage(wait));
					captcha.renew();
					return;
				}
				boolean solved = captcha.isSolved();
				String wrongCaptcha = captcha.getWrongMessage();
				captcha.renew();
				if (!solved)
				{
					attempts.failed(address);
					error(wrongCaptcha);
					return;
				}
				Optional<Role> role = Accounts.get().authenticate(name.getObject(),
					password.getObject());
				if (role.isEmpty())
				{
					attempts.failed(address);
					error(getString("wrong"));
					return;
				}
				attempts.succeeded(address);
				String account = role.get() == Role.ADMIN ? Accounts.ADMIN : Accounts.USER;
				if (!DemoSession.get().signIn(account, role.get()))
				{
					Duration untilRoom = SignedInUsers.get().untilRoom();
					error(new StringResourceModel("full", SignInPage.this)
						.setParameters(SignedInUsers.MAXIMUM,
							Math.max(1, (untilRoom.toSeconds() + 59) / 60))
						.getString());
					return;
				}
				Visits.get().signedIn(VisitorCookie.read(), role.get());
				goOn();
			}

			@Override
			protected void onError()
			{
				captcha.renew();
			}
		};
		add(form);
		form.add(new FormErrors("errors"));
		form.add(new RequiredTextField<>("name", name).setLabel(new ResourceModel("name")));
		form.add(new PasswordTextField("password", password).setLabel(new ResourceModel("password"))
			.add(new ShowPasswordBehavior()));
		form.add(captcha);

		add(new Label("user", Accounts.USER));
		add(new Label("userPassword", Accounts.USER_PASSWORD));
		add(new Label("inUse", () -> new StringResourceModel("inUse", this)
			.setParameters(SignedInUsers.get().count(), SignedInUsers.MAXIMUM).getString()));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		if (!getSession().isTemporary() && DemoSession.get().tellPlaceLost())
		{
			error(new StringResourceModel("placeLost", this)
				.setParameters(SignedInUsers.PLACE_TIME.toMinutes(), SignedInUsers.MAXIMUM)
				.getString());
		}
	}

	/**
	 * Goes on to the page the visitor wanted to see, if it is a path of the site, or else to the
	 * home page.
	 */
	private void goOn()
	{
		String to = getPageParameters().get(TO).toOptionalString();
		if (to != null && LOCAL_PATH.matcher(to).matches() && !to.contains(".."))
		{
			throw new RedirectToUrlException(getRequestCycle().getUrlRenderer()
				.renderContextRelativeUrl(to));
		}
		setResponsePage(HomePage.class);
	}

	/**
	 * @return the message telling how long the address is still blocked, in seconds or, from two
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
	 * @return the captcha, for tests
	 */
	Captcha getCaptcha()
	{
		return captcha;
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}
}
