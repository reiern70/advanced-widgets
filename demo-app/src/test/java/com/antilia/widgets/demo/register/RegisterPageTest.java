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

import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;

/**
 * Registers on {@link RegisterPage}, as a person and as robots.
 */
class RegisterPageTest
{
	private final MutableClock clock = new MutableClock();

	private WicketTester tester;

	private RegisterPage page;

	@BeforeEach
	void startPage()
	{
		tester = new WicketTester(new WicketApplication());
		page = tester.startPage(new RegisterPage(clock));
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
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
