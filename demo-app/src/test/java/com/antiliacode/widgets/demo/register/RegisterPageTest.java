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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.RecordingMailer;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.data.JobRole;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.mail.LogMailer;
import com.antiliacode.widgets.demo.mail.Mail;

/**
 * Registers on {@link RegisterPage}, as a person and as robots.
 */
class RegisterPageTest
{
	/** A strong password. */
	private static final String PASSWORD = "Analytical-Engine-1843";

	private final MutableClock clock = new MutableClock();

	private WicketTester tester;

	private RegisterPage page;

	private Accounts accounts;

	private RecordingMailer mailer;

	@BeforeEach
	void startPage()
	{
		System.setProperty(RegisterPage.OPEN_PROPERTY, "true");
		TestDatabase.clear();
		WicketApplication application = new WicketApplication();
		tester = new WicketTester(application);
		mailer = RecordingMailer.install(application);
		accounts = Accounts.get();
		page = tester.startPage(new RegisterPage(clock));
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(RegisterPage.OPEN_PROPERTY);
	}

	@Test
	void registeringIsClosedWhenNoEmailIsDelivered()
	{
		System.clearProperty(RegisterPage.OPEN_PROPERTY);
		((WicketApplication)tester.getApplication()).setMailer(new LogMailer());
		page = tester.startPage(new RegisterPage(clock));

		tester.assertVisible("closed");
		tester.assertContains("Registering is closed for now.");
		tester.assertDisabled("form:submit");
		tester.assertDisabled("form:email");

		// a request sent all the same is rejected, and makes no account
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		assertThrows(ListenerInvocationNotAllowedException.class, form::submit);
		assertFalse(DemoSession.get().isSignedIn());
		assertFalse(accounts.authenticate("ada@example.com", PASSWORD).isPresent());
	}

	@Test
	void registeringIsOpenWhenTheEmailsAreDelivered()
	{
		System.clearProperty(RegisterPage.OPEN_PROPERTY);
		page = tester.startPage(new RegisterPage(clock));

		tester.assertInvisible("closed");
		tester.assertEnabled("form:submit");
	}

	@Test
	void theSystemPropertyClosesRegistering()
	{
		System.setProperty(RegisterPage.OPEN_PROPERTY, "false");
		page = tester.startPage(new RegisterPage(clock));

		tester.assertVisible("closed");
	}

	@Test
	void anOpenRegistrationSaysNothingOfIt()
	{
		tester.assertInvisible("closed");
		tester.assertEnabled("form:submit");
	}

	@Test
	void aPersonIsSentToTheEmailConfirmation()
	{
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getExpectedAnswer()).submit();

		tester.assertRenderedPage(EmailConfirmationPage.class);
		tester.assertContains("ada@example.com");

		Mail mail = mailer.last();
		assertEquals("ada@example.com", mail.to());
		assertEquals("Confirm your email for Antilia Widgets", mail.subject());
		assertTrue(mail.text().contains("Hello Ada,"), mail.text());
		assertTrue(mail.text().contains("validate-email?token=" + mailer.lastToken()));
		assertTrue(mail.html().contains("href=\"http://localhost/context/servlet/validate-email?token=" +
			mailer.lastToken() + "\""), mail.html());
	}

	@Test
	void aPersonGetsAnAccountWhoseEmailIsNotConfirmedYet()
	{
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getExpectedAnswer()).submit();

		assertFalse(DemoSession.get().isSignedIn());
		User ada = accounts.authenticate("Ada@Example.com", PASSWORD).orElseThrow();
		assertFalse(ada.isValidated());
		assertEquals("Ada", ada.getFirstName());
		assertEquals("Lovelace", ada.getLastName());
		assertEquals("ES", ada.getCountry());
		assertEquals(Language.ENGLISH, ada.getLanguage());
		assertEquals(JobRole.DEVELOPER, ada.getJobRole());
		assertNull(ada.getJobRoleOther());
		// only salted hashes are kept: of the password and of the token of the link
		assertFalse(ada.getPasswordHash().contains(PASSWORD));
		assertFalse(mailer.lastToken().equals(ada.getValidationToken()));
	}

	@Test
	void otherAsksTheRoleInWords()
	{
		tester.assertInvisible("form:other");
		FormTester form = tester.newFormTester("form");
		form.select("jobRole", JobRole.OTHER.ordinal());
		tester.executeAjaxEvent("form:jobRole", "change");
		tester.assertVisible("form:other");

		// required then
		clock.advance(Duration.ofSeconds(20));
		form = fillIn(page.getExpectedAnswer());
		form.select("jobRole", JobRole.OTHER.ordinal());
		form.submit();
		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("Your role is required.");

		clock.advance(Duration.ofSeconds(20));
		form = fillIn(page.getExpectedAnswer());
		form.select("jobRole", JobRole.OTHER.ordinal());
		form.setValue("other:jobRoleOther", " Mathematician ");
		form.submit();

		tester.assertRenderedPage(EmailConfirmationPage.class);
		User ada = accounts.find("ada@example.com").orElseThrow();
		assertEquals(JobRole.OTHER, ada.getJobRole());
		assertEquals("Mathematician", ada.getJobRoleOther());
	}

	@Test
	void theRoleIsRequired()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("jobRole", "");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("Role is required.");
	}

	@Test
	void theEmailIsInTheLanguageChosen()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.select("language", Language.GERMAN.ordinal());
		form.submit();

		assertEquals(Language.GERMAN, accounts.find("ada@example.com").orElseThrow().getLanguage());
		Mail mail = mailer.last();
		assertEquals("Bestätigen Sie Ihre E-Mail-Adresse für Antilia Widgets", mail.subject());
		assertTrue(mail.text().contains("Hallo Ada,"), mail.text());
	}

	@Test
	void anEmailRegisteredAlreadyIsTurnedAway()
	{
		accounts.register("ada@example.com", "another-password", user -> {});
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getExpectedAnswer()).submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("taken"));
		assertFalse(DemoSession.get().isSignedIn());
		assertTrue(accounts.authenticate("ada@example.com", "another-password").isPresent());
		assertTrue(mailer.getMails().isEmpty());
	}

	@Test
	void thePasswordsMustBeTheSame()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("repeatedPassword", PASSWORD + "s");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("The passwords are not the same.");
		assertNull(DemoSession.get().getAccount());
	}

	@Test
	void thePasswordMustBeStrong()
	{
		for (String weak : new String[] { "Sh0rt!", "analytical-engine-1843",
			"ANALYTICAL-ENGINE-1843", "Analytical-Engine", "AnalyticalEngine1843" })
		{
			clock.advance(Duration.ofSeconds(20));
			FormTester form = fillIn(page.getExpectedAnswer());
			form.setValue("password", weak);
			form.setValue("repeatedPassword", weak);
			form.submit();

			tester.assertRenderedPage(RegisterPage.class);
			tester.assertErrorMessages("Password needs 10 to 128 characters, with an upper and " +
				"a lower case letter, a digit and a symbol.");
			tester.clearFeedbackMessages();
		}
		assertTrue(mailer.getMails().isEmpty());
	}

	@Test
	void thePasswordMustNotContainTheEmail()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("password", "Ada@example.com-1");
		form.setValue("repeatedPassword", "Ada@example.com-1");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("The password must not contain your email.");
	}

	@Test
	void aRobotFillingInTheHoneypotIsTurnedAway()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("website", "https://spam.example.com");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("robot"));
	}

	@Test
	void aRobotSendingTheFormAtOnceIsTurnedAway()
	{
		clock.advance(Duration.ofSeconds(1));
		fillIn(page.getExpectedAnswer()).submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("robot"));
	}

	@Test
	void aWrongSumIsTurnedAwayAndANewOneAsked()
	{
		clock.advance(Duration.ofSeconds(20));
		int expected = page.getExpectedAnswer();
		fillIn(expected + 1).submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("robot"));
	}

	@Test
	void requiresTheFieldsListingEveryErrorInTheFormInTheOrderOfTheFields()
	{
		clock.advance(Duration.ofSeconds(20));
		tester.newFormTester("form").submit();

		tester.assertRenderedPage(RegisterPage.class);
		String response = tester.getLastResponseAsString();
		// more errors than a stack of notifications shows: all in the form, none a notification
		int previous = -1;
		for (String message : List.of("First name is required.", "Last name is required.",
			"Email is required.", "Password is required.", "Password again is required.",
			"Country is required.", "Role is required.", "The sum is required."))
		{
			int at = response.indexOf(message);
			assertTrue(at > previous, message + " after the one before it");
			previous = at;
		}
		assertFalse(response.contains("Wicket.Notifications.show"), response);
		// the fields in error say so; the language, chosen already, is right
		for (String field : List.of("firstName", "email", "password", "country", "answer"))
		{
			assertEquals("true", TagTester.createTagByAttribute(response, "name", field)
				.getAttribute("aria-invalid"), field);
		}
		assertNull(TagTester.createTagByAttribute(response, "name", "language")
			.getAttribute("aria-invalid"));
	}

	private FormTester fillIn(int answer)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("firstName", "Ada");
		form.setValue("lastName", "Lovelace");
		form.setValue("email", "ada@example.com");
		form.setValue("password", PASSWORD);
		form.setValue("repeatedPassword", PASSWORD);
		form.select("country", countryIndex("ES"));
		form.select("language", Language.ENGLISH.ordinal());
		form.select("jobRole", JobRole.DEVELOPER.ordinal());
		form.setValue("answer", String.valueOf(answer));
		return form;
	}

	private static int countryIndex(String code)
	{
		return Countries.codes().getObject().indexOf(code);
	}

	private static class MutableClock extends Clock
	{
		private Instant now = Instant.parse("2026-10-07T12:00:00Z");

		void advance(Duration duration)
		{
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone)
		{
			return this;
		}

		@Override
		public Instant instant()
		{
			return now;
		}
	}
}
