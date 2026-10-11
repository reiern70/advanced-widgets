/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.register;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.StringValidator;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.JobRole;
import com.antilia.widgets.demo.data.Language;
import com.antilia.widgets.dropdown.Dropdown;

/**
 * Lets a visitor register, once registering is open (see {@link #isOpen()}); until then it says
 * that registering is closed and the form is disabled. A visitor registers with: first and last
 * name, email, a strong password typed twice (see {@link StrongPasswordValidator}), country, what
 * the visitor does (a {@link JobRole}, in words with Other) and the language of the emails. On success the account is made, but opens the demos only once the
 * visitor followed the link of the {@link ConfirmationMail} to the {@link EmailValidationPage};
 * the visitor is sent to the {@link EmailConfirmationPage}, which says so.
 * <p>
 * It keeps robots out in three ways, none needing an outside service: a field hidden from people
 * that robots fill in (a honeypot), a minimum time between showing the form and sending it, and
 * a small sum to work out.
 */
public class RegisterPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * The system property opening ({@code true}) or closing ({@code false}) the registration;
	 * when it is not set registering is open if the confirmation emails are delivered, see
	 * {@link com.antilia.widgets.demo.mail.SmtpMailer}.
	 */
	public static final String OPEN_PROPERTY = "antilia-widgets.open-registration";

	/** People take longer than this to fill in the form; robots sending it at once do not. */
	static final Duration MINIMUM_FILL_TIME = Duration.ofSeconds(3);

	private final Clock clock;

	private Instant shownAt;

	private int first;

	private int second;

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
		newChallenge();
		registration.setLanguage(Language.of(getLocale()));

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
				if (isRobot())
				{
					error(getString("robot"));
					newChallenge();
					registration.setAnswer(null);
					return;
				}
				String email = Accounts.accountName(registration.getEmail());
				Optional<String> token = Accounts.get().register(email,
					registration.getPassword(), registration::fill);
				registration.setPassword(null);
				if (token.isEmpty())
				{
					error(getString("taken"));
					newChallenge();
					registration.setAnswer(null);
					return;
				}
				Accounts.get().find(email)
					.ifPresent(user -> ConfirmationMail.send(user, token.get()));
				setResponsePage(new EmailConfirmationPage(email));
			}

			@Override
			protected void onError()
			{
				newChallenge();
			}
		};
		form.setEnabled(isOpen());
		add(form);
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
		form.add(password);
		PasswordTextField repeated = new PasswordTextField("repeatedPassword", Model.of());
		repeated.setLabel(new ResourceModel("repeatedPassword"));
		form.add(repeated);
		form.add(new EqualPasswordInputValidator(password, repeated));
		form.add(new PasswordNotEmailValidator(email, password));
		form.add(new Dropdown<>("country", Countries.codes(), Countries.renderer())
			.setRequired(true)
			.setLabel(new ResourceModel("country")));
		form.add(new Dropdown<>("language", Arrays.asList(Language.values()),
			new LanguageRenderer())
			.setRequired(true)
			.setLabel(new ResourceModel("emailLanguage")));

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

		form.add(new Label("question", new StringResourceModel("question", this)
			.setParameters((IModel<Integer>)() -> first, (IModel<Integer>)() -> second)));
		form.add(new NumberTextField<Integer>("answer", Integer.class)
			.setRequired(true)
			.setLabel(new ResourceModel("answer")));
		form.add(new Button("submit"));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}

	/**
	 * @return whether visitors may register: as the system property {@value #OPEN_PROPERTY}
	 *         says, or else if the confirmation emails reach them
	 */
	public static boolean isOpen()
	{
		String open = System.getProperty(OPEN_PROPERTY);
		if (!Strings.isEmpty(open))
		{
			return Boolean.parseBoolean(open);
		}
		return ((WicketApplication)Application.get()).getMailer().isDelivering();
	}

	/**
	 * @return whether the form was sent by a robot: it filled in the honeypot, sent the form too
	 *         soon or got the sum wrong
	 */
	boolean isRobot()
	{
		String website = registration.getWebsite();
		boolean filledHoneypot = website != null && !website.isBlank();
		boolean tooSoon = Duration.between(shownAt, clock.instant())
			.compareTo(MINIMUM_FILL_TIME) < 0;
		Integer answer = registration.getAnswer();
		boolean wrongSum = answer == null || answer != first + second;
		return filledHoneypot || tooSoon || wrongSum;
	}

	/**
	 * Asks a new sum and starts timing anew.
	 */
	private void newChallenge()
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		first = random.nextInt(1, 10);
		second = random.nextInt(1, 10);
		shownAt = clock.instant();
	}

	/**
	 * @return the sum asked, for tests
	 */
	int getExpectedAnswer()
	{
		return first + second;
	}

	/**
	 * Shows a language by its name in that language: English, Español, Deutsch.
	 */
	private static class LanguageRenderer extends ChoiceRenderer<Language>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(Language language)
		{
			Locale locale = language.getLocale();
			String name = locale.getDisplayLanguage(locale);
			return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
		}

		@Override
		public String getIdValue(Language language, int index)
		{
			return language.getCode();
		}
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

		private Integer answer;

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
		void fill(com.antilia.widgets.demo.data.User user)
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

		public Integer getAnswer()
		{
			return answer;
		}

		public void setAnswer(Integer answer)
		{
			this.answer = answer;
		}
	}
}
