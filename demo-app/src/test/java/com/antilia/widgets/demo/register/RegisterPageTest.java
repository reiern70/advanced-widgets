/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.register;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.account.Accounts;

/**
 * Registers on {@link RegisterPage}, as a person and as robots.
 */
class RegisterPageTest
{
	private final MutableClock clock = new MutableClock();

	private WicketTester tester;

	private RegisterPage page;

	private Accounts accounts;

	@BeforeEach
	void startPage()
	{
		System.setProperty(RegisterPage.OPEN_PROPERTY, "true");
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication());
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
	void registeringIsNotSupportedYet()
	{
		System.clearProperty(RegisterPage.OPEN_PROPERTY);
		page = tester.startPage(new RegisterPage(clock));

		tester.assertVisible("closed");
		tester.assertContains("Registering is not supported yet.");
		tester.assertDisabled("form:submit");
		tester.assertDisabled("form:email");

		// a request sent all the same is rejected, and makes no account
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		assertThrows(ListenerInvocationNotAllowedException.class, form::submit);
		assertFalse(DemoSession.get().isSignedIn());
		assertFalse(accounts.authenticate("ada@example.com", "analytical-engine").isPresent());
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
	}

	@Test
	void aPersonGetsAnAccountAndIsSignedIn()
	{
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getExpectedAnswer()).submit();

		assertEquals("ada@example.com", DemoSession.get().getAccount());
		assertTrue(accounts.authenticate("Ada@Example.com", "analytical-engine").isPresent());
	}

	@Test
	void anEmailRegisteredAlreadyIsTurnedAway()
	{
		accounts.register("ada@example.com", "another-password");
		clock.advance(Duration.ofSeconds(20));
		fillIn(page.getExpectedAnswer()).submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages(page.getString("taken"));
		assertFalse(DemoSession.get().isSignedIn());
		assertTrue(accounts.authenticate("ada@example.com", "another-password").isPresent());
	}

	@Test
	void thePasswordsMustBeTheSame()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("repeatedPassword", "analytical-engines");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("The passwords are not the same.");
		assertNull(DemoSession.get().getAccount());
	}

	@Test
	void thePasswordMustNotBeShort()
	{
		clock.advance(Duration.ofSeconds(20));
		FormTester form = fillIn(page.getExpectedAnswer());
		form.setValue("password", "short");
		form.setValue("repeatedPassword", "short");
		form.submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertErrorMessages("Password needs at least 8 characters.");
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
	void requiresTheFields()
	{
		clock.advance(Duration.ofSeconds(20));
		tester.newFormTester("form").submit();

		tester.assertRenderedPage(RegisterPage.class);
		tester.assertContains("Please enter First name.");
	}

	private FormTester fillIn(int answer)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("firstName", "Ada");
		form.setValue("lastName", "Lovelace");
		form.setValue("email", "ada@example.com");
		form.setValue("password", "analytical-engine");
		form.setValue("repeatedPassword", "analytical-engine");
		form.select("country", 0);
		form.select("role", 0);
		form.setValue("answer", String.valueOf(answer));
		return form;
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
