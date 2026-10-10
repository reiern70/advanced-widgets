/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.register;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;

import com.antilia.widgets.demo.BasePage;
import com.antilia.widgets.demo.Countries;
import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.dropdown.Dropdown;

/**
 * Lets a visitor register: first and last name, email, country, role and a password. On success
 * the account is made, the visitor signed in and sent on to the demo they wanted to see, or else
 * to the {@link EmailConfirmationPage}.
 * <p>
 * Only the account is kept, the other details are not, and no email is sent yet. It keeps robots out in three ways,
 * none needing an outside service: a field hidden from people that robots fill in (a honeypot),
 * a minimum time between showing the form and sending it, and a small sum to work out.
 */
public class RegisterPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** People take longer than this to fill in the form; robots sending it at once do not. */
	static final Duration MINIMUM_FILL_TIME = Duration.ofSeconds(3);

	/** The fewest characters of a password. */
	static final int MINIMUM_PASSWORD_LENGTH = 8;

	/** The roles offered, keys of the page's bundle. */
	static final List<String> ROLES = List.of("developer", "architect", "designer", "manager",
		"student", "other");

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

		Form<Registration> form = new Form<>("form", new CompoundPropertyModel<>(registration))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit()
			{
				if (isRobot())
				{
					error(getString("robot"));
					newChallenge();
					registration.setAnswer(null);
					return;
				}
				String email = registration.getEmail();
				boolean made = Accounts.get().register(email, registration.getPassword());
				registration.setPassword(null);
				if (!made)
				{
					error(getString("taken"));
					newChallenge();
					registration.setAnswer(null);
					return;
				}
				DemoSession.get().signIn(Accounts.accountName(email));
				continueToOriginalDestination();
				setResponsePage(new EmailConfirmationPage(email));
			}

			@Override
			protected void onError()
			{
				newChallenge();
			}
		};
		add(form);

		form.add(new RequiredTextField<String>("firstName")
			.add(StringValidator.maximumLength(60))
			.setLabel(new ResourceModel("firstName")));
		form.add(new RequiredTextField<String>("lastName")
			.add(StringValidator.maximumLength(60))
			.setLabel(new ResourceModel("lastName")));
		form.add(new EmailTextField("email")
			.setRequired(true)
			.setLabel(new ResourceModel("email")));
		PasswordTextField password = new PasswordTextField("password");
		password.add(StringValidator.minimumLength(MINIMUM_PASSWORD_LENGTH))
			.setLabel(new ResourceModel("password"));
		form.add(password);
		PasswordTextField repeated = new PasswordTextField("repeatedPassword", Model.of());
		repeated.setLabel(new ResourceModel("repeatedPassword"));
		form.add(repeated);
		form.add(new EqualPasswordInputValidator(password, repeated));
		form.add(new Dropdown<>("country", Countries.codes(), Countries.renderer())
			.setRequired(true)
			.setLabel(new ResourceModel("country")));
		Dropdown<String> role = new Dropdown<>("role", ROLES, new RoleRenderer());
		role.setRequired(true).setLabel(new ResourceModel("role"));
		form.add(role);

		// the honeypot: hidden from people by the style sheet, filled in by robots
		form.add(new TextField<String>("website"));

		form.add(new Label("question", new StringResourceModel("question", this)
			.setParameters((IModel<Integer>)() -> first, (IModel<Integer>)() -> second)));
		form.add(new NumberTextField<Integer>("answer", Integer.class)
			.setRequired(true)
			.setLabel(new ResourceModel("answer")));

		add(new FeedbackPanel("feedback"));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
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
	 * Shows a role by its name from the page's bundle.
	 */
	private class RoleRenderer extends ChoiceRenderer<String>
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Object getDisplayValue(String role)
		{
			return getString("role." + role);
		}

		@Override
		public String getIdValue(String role, int index)
		{
			return role;
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

		private String role;

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

		public String getRole()
		{
			return role;
		}

		public void setRole(String role)
		{
			this.role = role;
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
