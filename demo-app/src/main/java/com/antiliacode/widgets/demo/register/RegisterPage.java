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
package com.antiliacode.widgets.demo.register;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;

import com.antiliacode.widgets.demo.BasePage;
import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.FormErrors;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.Captcha;
import com.antiliacode.widgets.demo.account.ShowPasswordBehavior;
import com.antiliacode.widgets.demo.account.SignInAttempts;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.data.JobRole;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.dropdown.Dropdown;

/**
 * Lets a visitor register, unless registering is closed (see {@link #isOpen()}): then it says so
 * and the form is disabled. A visitor registers with: first and last name, email, a strong
 * password typed twice (see {@link StrongPasswordValidator}), country and what the visitor does (a
 * {@link JobRole}, in words with Other); the account takes the language of the page. Above the
 * button the page says what is done with the data: the account is kept {@link Accounts#KEPT} and
 * then deleted, and never used to contact anybody, only to count who uses the widgets. On success
 * the account opens the demos at once and the visitor is signed in. No email is sent, so the email
 * is not checked: it is the name of the account. As no email can answer in its place, the page
 * says when an email has an account already.
 * <p>
 * It keeps robots out in four ways, none needing an outside service: a field hidden from people
 * that robots fill in (a honeypot), a minimum time between showing the form and sending it, the
 * {@link Captcha} of the sign-in page, letters drawn on noise, or for those who cannot read them a
 * question in words, and a challenge that answers once, so that the form sent is not taken again,
 * not even from an earlier version of the page. The captcha and the challenge are new after every
 * submit. Registrations are limited too, from one address and from everybody (see
 * {@link RegistrationLimits}).
 */
public class RegisterPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * The system property closing the registration, set to {@code false}; registering is open
	 * otherwise.
	 */
	public static final String OPEN_PROPERTY = "antilia-widgets.open-registration";

	/** People take longer than this to fill in the form; robots sending it at once do not. */
	static final Duration MINIMUM_FILL_TIME = Duration.ofSeconds(3);

	private final Clock clock;

	private Instant shownAt;

	/** The challenge of the form shown, which answers once, see {@link RegistrationLimits}. */
	private String challenge;

	private final Captcha captcha;

	private final Registration registration = new Registration();

	/**
	 * Constructor.
	 */
	public RegisterPage()
	{
		this(Clock.systemUTC());
	}

	/**
	 * @param clock
	 *            the clock timing how long the form took to fill in
	 */
	RegisterPage(Clock clock)
	{
		this.clock = clock;
		captcha = new Captcha("captcha");
		newChallenge();

		Form<Registration> form = new Form<>("form", new CompoundPropertyModel<>(registration))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				if (!isOpen())
				{
					return;
				}
				boolean robot = isRobot();
				boolean solved = captcha.isSolved();
				String wrongCaptcha = captcha.getWrongMessage();
				// a new captcha and challenge after every submit: the form sent is not taken again
				newChallenge();
				if (robot)
				{
					error(getString("robot"));
					return;
				}
				if (!solved)
				{
					error(wrongCaptcha);
					return;
				}
				if (!RegistrationLimits.get().register(SignInAttempts.address()))
				{
					error(getString("tooMany"));
					return;
				}
				registration.setLanguage(Language.of(getLocale()));
				Optional<User> user = Accounts.get().register(
					Accounts.accountName(registration.getEmail()), registration.getPassword(),
					registration::fill);
				registration.setPassword(null);
				if (user.isEmpty())
				{
					error(getString("exists"));
					return;
				}
				if (!DemoSession.get().signIn(user.get().getName(), user.get().getRole()))
				{
					// too many users are signed in: the account waits for the visitor to sign in
					getSession().info(getString("registeredFull"));
					setResponsePage(SignInPage.class);
					return;
				}
				getSession().success(getString("registered"));
				continueToOriginalDestination();
				setResponsePage(HomePage.class);
			}

			@Override
			protected void onError()
			{
				newChallenge();
			}
		};
		form.setEnabled(isOpen());
		add(form);
		form.add(new FormErrors("errors"));
		add(new WebMarkupContainer("closed").setVisible(!isOpen()));

		form.add(new RequiredTextField<String>("firstName")
			.add(StringValidator.maximumLength(60))
			.setLabel(new ResourceModel("firstName")));
		form.add(new RequiredTextField<String>("lastName")
			.add(StringValidator.maximumLength(60))
			.setLabel(new ResourceModel("lastName")));
		EmailTextField email = new EmailTextField("email");
		email.setRequired(true)
			.add(StringValidator.maximumLength(254))
			.setLabel(new ResourceModel("email"));
		form.add(email);
		PasswordTextField password = new PasswordTextField("password");
		password.add(new StrongPasswordValidator())
			.setLabel(new ResourceModel("password"));
		password.add(new ShowPasswordBehavior());
		form.add(password);
		PasswordTextField repeated = new PasswordTextField("repeatedPassword", Model.of());
		repeated.setLabel(new ResourceModel("repeatedPassword"));
		repeated.add(new ShowPasswordBehavior());
		form.add(repeated);
		form.add(new EqualPasswordInputValidator(password, repeated));
		form.add(new PasswordNotEmailValidator(email, password));
		form.add(new Dropdown<>("country", Countries.codes(), Countries.renderer())
			.setRequired(true)
			.setLabel(new ResourceModel("country")));

		// what the visitor does; with Other, in words
		WebMarkupContainer other = new WebMarkupContainer("other")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(registration.getJobRole() == JobRole.OTHER);
			}
		};
		other.setOutputMarkupPlaceholderTag(true);
		form.add(other);
		other.add(new RequiredTextField<String>("jobRoleOther")
			.add(StringValidator.maximumLength(100))
			.setLabel(new ResourceModel("jobRoleOther")));
		Dropdown<JobRole> jobRole = new Dropdown<>("jobRole", Arrays.asList(JobRole.values()),
			new EnumChoiceRenderer<>(this));
		jobRole.setRequired(true).setLabel(new ResourceModel("jobRole"));
		jobRole.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(other);
			}
		});
		form.add(jobRole);

		// the honeypot: hidden from people by the style sheet, filled in by robots
		form.add(new TextField<String>("website"));

		form.add(captcha);
		form.add(new Label("notice", new StringResourceModel("notice", this)
			.setParameters(Accounts.KEPT.toTotalMonths())));
		form.add(new Button("submit"));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}

	/**
	 * @return whether visitors may register: unless the system property {@value #OPEN_PROPERTY}
	 *         is {@code false}
	 */
	public static boolean isOpen()
	{
		String open = System.getProperty(OPEN_PROPERTY);
		return open == null || !"false".equalsIgnoreCase(open.strip());
	}

	/**
	 * @return whether the form was sent by a robot: it filled in the honeypot, sent the form too
	 *         soon or sent a form whose challenge answered already
	 */
	boolean isRobot()
	{
		boolean answeredBefore = !RegistrationLimits.get().answer(challenge);
		challenge = null;
		String website = registration.getWebsite();
		boolean filledHoneypot = website != null && !website.isBlank();
		boolean tooSoon = Duration.between(shownAt, clock.instant())
			.compareTo(MINIMUM_FILL_TIME) < 0;
		return answeredBefore || filledHoneypot || tooSoon;
	}

	/**
	 * Asks a new captcha, with a new challenge, and starts timing anew; the challenge before
	 * answers no more.
	 */
	private void newChallenge()
	{
		RegistrationLimits limits = RegistrationLimits.get();
		limits.answer(challenge);
		challenge = limits.newChallenge();
		captcha.renew();
		shownAt = clock.instant();
	}

	/**
	 * @return the captcha asked, for tests
	 */
	Captcha getCaptcha()
	{
		return captcha;
	}

	/**
	 * @return the challenge of the form shown, for tests
	 */
	String getChallenge()
	{
		return challenge;
	}

	/**
	 * Turns away a password containing the email or the part of it before the {@code @}, when
	 * that part has three characters or more. The message is the one of the key
	 * {@code PasswordNotEmailValidator}.
	 */
	private static class PasswordNotEmailValidator extends AbstractFormValidator
	{
		private static final long serialVersionUID = 1L;

		private final FormComponent<String> email;

		private final FormComponent<String> password;

		PasswordNotEmailValidator(FormComponent<String> email, FormComponent<String> password)
		{
			this.email = email;
			this.password = password;
		}

		@Override
		public FormComponent<?>[] getDependentFormComponents()
		{
			return new FormComponent<?>[] { email, password };
		}

		@Override
		public void validate(Form<?> form)
		{
			String address = email.getConvertedInput();
			String typed = password.getConvertedInput();
			if (address == null || typed == null)
			{
				return;
			}
			String local = address.toLowerCase(Locale.ROOT);
			int at = local.indexOf('@');
			if (at >= 3)
			{
				local = local.substring(0, at);
			}
			if (local.length() >= 3 && typed.toLowerCase(Locale.ROOT).contains(local))
			{
				error(password);
			}
		}
	}

	/**
	 * What the visitor entered.
	 */
	static class Registration implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private String firstName;

		private String lastName;

		private String email;

		private String country;

		private Language language;

		private JobRole jobRole;

		private String jobRoleOther;

		private String password;

		private String website;

		public String getFirstName()
		{
			return firstName;
		}

		public void setFirstName(String firstName)
		{
			this.firstName = firstName;
		}

		public String getLastName()
		{
			return lastName;
		}

		public void setLastName(String lastName)
		{
			this.lastName = lastName;
		}

		public String getEmail()
		{
			return email;
		}

		public void setEmail(String email)
		{
			this.email = email;
		}

		public String getCountry()
		{
			return country;
		}

		public void setCountry(String country)
		{
			this.country = country;
		}

		public Language getLanguage()
		{
			return language;
		}

		public void setLanguage(Language language)
		{
			this.language = language;
		}

		/**
		 * Puts the details entered in an account.
		 *
		 * @param user
		 *            the account
		 */
		void fill(com.antiliacode.widgets.demo.data.User user)
		{
			user.setFirstName(firstName.strip());
			user.setLastName(lastName.strip());
			user.setCountry(country);
			user.setLanguage(language);
			user.setJobRole(jobRole);
			user.setJobRoleOther(jobRole == JobRole.OTHER ? jobRoleOther.strip() : null);
		}

		public JobRole getJobRole()
		{
			return jobRole;
		}

		public void setJobRole(JobRole jobRole)
		{
			this.jobRole = jobRole;
		}

		public String getJobRoleOther()
		{
			return jobRoleOther;
		}

		public void setJobRoleOther(String jobRoleOther)
		{
			this.jobRoleOther = jobRoleOther;
		}

		public String getPassword()
		{
			return password;
		}

		public void setPassword(String password)
		{
			this.password = password;
		}

		public String getWebsite()
		{
			return website;
		}

		public void setWebsite(String website)
		{
			this.website = website;
		}
	}
}
