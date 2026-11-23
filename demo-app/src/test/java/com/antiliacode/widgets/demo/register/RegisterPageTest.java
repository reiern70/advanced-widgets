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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.antiliacode.widgets.demo.Countries;
import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.data.JobRole;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;

/**
 * Registers on {@link RegisterPage}, as a person and as robots.
 */
class RegisterPageTest
{
	/** A strong password. */
	private static final String PASSWORD = "Analytical-Engine-1843";

	private final MutableClock clock = new MutableClock();

	/** The clock of the limits of registering. */
	private final TestClock limitsClock = new TestClock();

	private WicketTester tester;

	private RegisterPage page;

	private Accounts accounts;

	@BeforeEach
	void startPage()
	{
		TestDatabase.clear();
		RegistrationLimits limits = new RegistrationLimits(limitsClock);
		WicketApplication application = new WicketApplication()
		{
			@Override
			public RegistrationLimits getRegistrationLimits()
			{
				return limits;
			}
		};
		tester = new WicketTester(application);
		tester.getSession().setLocale(Locale.ENGLISH);
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
	void registeringIsOpenWithoutAnyMailServer()
	{
		tester.assertInvisible("closed");
		tester.assertEnabled("form:submit");
	}

	@Test
	void theSystemPropertyClosesRegistering()
	{
		System.setProperty(RegisterPage.OPEN_PROPERTY, "false");
		page = tester.startPage(new RegisterPage(clock));

		tester.assertVisible("closed");
		tester.assertContains("Registering is closed for now.");
		tester.assertDisabled("form:submit");
		tester.assertDisabled("form:email");

		// a request sent all the same is rejected, and makes no account
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn();
		assertThrows(ListenerInvocationNotAllowedException.class, form::submit);
		assertFalse(DemoSession.get().isSignedIn());
		assertFalse(accounts.find("ada@example.com").isPresent());
	}

	@Test
	void thePageSaysWhatIsDoneWithTheData()
	{
		tester.assertContains("We keep your account and the pages you visit here for 6 months, " +
			"then delete them. We never use your email or anything else you give us to contact " +
			"you: only to count how many people use the components and which demos interest them.");
		// the captcha is asked of everybody here, not after failed attempts as on the sign-in page
		tester.assertContains("To show you are a person, type the letters of the image.");
		assertFalse(tester.getLastResponseAsString().contains("Too many failed attempts"));
	}

	@Test
	void aPersonIsSignedInAtOnceWithAnAccountThatExpiresInSixMonths()
	{
		Instant before = Instant.now();
		clock.advance(Duration.ofSeconds(20));
		fillIn().submit();

		tester.assertRenderedPage(HomePage.class);
		assertEquals("ada@example.com", DemoSession.get().getAccount());
		User ada = accounts.authenticate("Ada@Example.com", PASSWORD).orElseThrow();
		assertTrue(ada.isValidated());
		assertEquals(Role.USER, ada.getRole());
		assertEquals("Ada", ada.getFirstName());
		assertEquals("Lovelace", ada.getLastName());
		assertEquals("ES", ada.getCountry());
		assertEquals(Language.ENGLISH, ada.getLanguage());
		assertEquals(JobRole.DEVELOPER, ada.getJobRole());
		assertNull(ada.getJobRoleOther());
		assertFalse(ada.isConfigured());
		// six months on
		Instant sixMonths = before.atZone(ZoneOffset.UTC).plusMonths(6).toInstant();
		assertFalse(ada.getExpires().isBefore(sixMonths), ada.getExpires().toString());
		assertTrue(ada.getExpires().isBefore(sixMonths.plus(Duration.ofMinutes(1))));
		// only a salted hash of the password is kept
		assertFalse(ada.getPasswordHash().contains(PASSWORD));
	}

	@Test
	void theAccountTakesTheLanguageOfThePage()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		page = tester.startPage(new RegisterPage(clock));
		clock.advance(Duration.ofSeconds(20));
		fillIn().submit();

		assertEquals(Language.GERMAN, accounts.find("ada@example.com").orElseThrow().getLanguage());
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
		form = fillIn();
		form.select("jobRole", JobRole.OTHER.ordinal());
		form.submit();
		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("Your role is required.");

		clock.advance(Duration.ofSeconds(20));
		form = fillIn();
		form.select("jobRole", JobRole.OTHER.ordinal());
		form.setValue("other:jobRoleOther", " Mathematician ");
		form.submit();

		tester.assertRenderedPage(HomePage.class);
		User ada = accounts.find("ada@example.com").orElseThrow();
		assertEquals(JobRole.OTHER, ada.getJobRole());
		assertEquals("Mathematician", ada.getJobRoleOther());
	}

	@Test
	void theRoleIsRequired()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn();
		form.setValue("jobRole", "");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("Role is required.");
	}

	@Test
	void anEmailRegisteredAlreadyIsToldAndTheAccountStaysAsItWas()
	{
		accounts.register("ada@example.com", "another-password", user -> {});
		clock.advance(Duration.ofSeconds(20));
		fillIn().submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("exists"));
		assertFalse(DemoSession.get().isSignedIn());
		assertTrue(accounts.authenticate("ada@example.com", "another-password").isPresent());
		assertFalse(accounts.authenticate("ada@example.com", PASSWORD).isPresent());
		assertNull(accounts.find("ada@example.com").orElseThrow().getFirstName());
	}

	@Test
	void whenTooManyUsersAreSignedInTheAccountWaitsOnTheSignInPage()
	{
		SignedInUsers users = SignedInUsers.get();
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			String id = "session-" + i;
			assertTrue(users.signIn("user" + i, Role.USER, () -> id));
		}
		clock.advance(Duration.ofSeconds(20));
		fillIn().submit();

		tester.assertRenderedPage(SignInPage.class);
		assertFalse(DemoSession.get().isSignedIn());
		assertTrue(accounts.authenticate("ada@example.com", PASSWORD).isPresent());
		tester.assertContains(page.getString("registeredFull"));
	}

	@Test
	void aFormSentOnceIsNotTakenAgain()
	{
		clock.advance(Duration.ofSeconds(20));
		// the page as the browser has it, which it may send again, captcha and all
		RegisterPage sent = WicketObjects.cloneObject(page);
		String solution = page.getCaptcha().getSolution();
		fillIn().submit();
		tester.assertRenderedPage(HomePage.class);
		DemoSession.get().signOut();

		// the same form with the same captcha, for another email
		tester.startPage(sent);
		FormTester form = fillIn(solution);
		form.setValue("email", "grace@example.com");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(sent.getString("robot"));
		assertFalse(accounts.find("grace@example.com").isPresent());
	}

	@Test
	void eachSubmitAsksANewCaptcha()
	{
		clock.advance(Duration.ofSeconds(20));
		String challenge = page.getChallenge();
		String letters = page.getCaptcha().getSolution();
		FormTester form = fillIn();
		form.setValue("repeatedPassword", "something else");
		form.submit();
		assertNotEquals(challenge, page.getChallenge());
		assertNotEquals(letters, page.getCaptcha().getSolution());

		challenge = page.getChallenge();
		letters = page.getCaptcha().getSolution();
		clock.advance(Duration.ofSeconds(20));
		fillIn("WRONG").submit();
		assertNotEquals(challenge, page.getChallenge());
		assertNotEquals(letters, page.getCaptcha().getSolution());
	}

	@Test
	void anAddressRegistersAFewTimesAnHour()
	{
		for (int i = 0; i < RegistrationLimits.REGISTRATIONS_PER_ADDRESS; i++)
		{
			registerAs("person" + i + "@example.com");
			tester.assertRenderedPage(HomePage.class);
			DemoSession.get().signOut();
		}

		registerAs("one-more@example.com");
		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("tooMany"));
		assertFalse(accounts.find("one-more@example.com").isPresent());

		limitsClock.advance(RegistrationLimits.WINDOW);
		registerAs("one-more@example.com");
		tester.assertRenderedPage(HomePage.class);
	}

	private void registerAs(String email)
	{
		page = tester.startPage(new RegisterPage(clock));
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn();
		form.setValue("email", email);
		form.submit();
	}

	@Test
	void thePasswordsMustBeTheSame()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn();
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
			FormTester form = fillIn();
			form.setValue("password", weak);
			form.setValue("repeatedPassword", weak);
			form.submit();

			tester.assertRenderedPage(RegisterPage.class);
			tester.assertErrorMessages("Password needs 10 to 128 characters, with an upper and " +
				"a lower case letter, a digit and a symbol.");
			tester.clearFeedbackMessages();
		}
		assertFalse(accounts.find("ada@example.com").isPresent());
	}

	@Test
	void thePasswordMustNotContainTheEmail()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn();
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
		FormTester form = fillIn();
		form.setValue("website", "https://spam.example.com");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("robot"));
		assertFalse(accounts.find("ada@example.com").isPresent());
	}

	@Test
	void aRobotSendingTheFormAtOnceIsTurnedAway()
	{
		clock.advance(Duration.ofSeconds(1));
		fillIn().submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("robot"));
		assertFalse(accounts.find("ada@example.com").isPresent());
	}

	@Test
	void aWrongCaptchaIsTurnedAwayAndANewOneAsked()
	{
		clock.advance(Duration.ofSeconds(20));
		fillIn("WRONG").submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getCaptcha().getWrongMessage());
		assertFalse(accounts.find("ada@example.com").isPresent());
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void theCaptchaTakesTheLettersInLowerCaseToo()
	{
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getCaptcha().getSolution().toLowerCase(Locale.ROOT)).submit();

		tester.assertRenderedPage(HomePage.class);
	}

	@Test
	void whoCannotReadTheImageAnswersAQuestionInstead()
	{
		tester.clickLink("form:captcha:other");
		tester.assertRenderedPage(RegisterPage.class);
		String answer = page.getCaptcha().getSolution();
		assertTrue(answer.matches("\\d+"), answer);

		clock.advance(Duration.ofSeconds(20));
		fillIn(answer).submit();

		tester.assertRenderedPage(HomePage.class);
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
			"Country is required.", "Role is required.", "The letters of the image is required."))
		{
			int at = response.indexOf(message);
			assertTrue(at > previous, message + " after the one before it");
			previous = at;
		}
		assertFalse(response.contains("Wicket.Notifications.show"), response);
		// the fields in error say so
		for (String field : List.of("firstName", "email", "password", "country", "captcha:answer"))
		{
			assertEquals("true", TagTester.createTagByAttribute(response, "name", field)
				.getAttribute("aria-invalid"), field);
		}
	}

	private FormTester fillIn()
	{
		return fillIn(page.getCaptcha().getSolution());
	}

	private FormTester fillIn(String captcha)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("firstName", "Ada");
		form.setValue("lastName", "Lovelace");
		form.setValue("email", "ada@example.com");
		form.setValue("password", PASSWORD);
		form.setValue("repeatedPassword", PASSWORD);
		form.select("country", countryIndex("ES"));
		form.select("jobRole", JobRole.DEVELOPER.ordinal());
		form.setValue("captcha:answer", captcha);
		return form;
	}

	private static int countryIndex(String code)
	{
		return Countries.codes().getObject().indexOf(code);
	}

	private static class MutableClock extends Clock implements Serializable
	{
		private static final long serialVersionUID = 1L;

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
