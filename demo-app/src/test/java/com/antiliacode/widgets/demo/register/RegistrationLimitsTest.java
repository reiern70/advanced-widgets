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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.TestClock;

/**
 * Registrations and emails are limited by address, by recipient and for everybody, in a window
 * that moves on; a challenge answers once.
 */
class RegistrationLimitsTest
{
	private final TestClock clock = new TestClock();

	private final RegistrationLimits limits = new RegistrationLimits(clock);

	@Test
	void anAddressRegistersAFewTimesAnHour()
	{
		for (int i = 0; i < RegistrationLimits.REGISTRATIONS_PER_ADDRESS; i++)
		{
			assertTrue(limits.register("203.0.113.7"));
			clock.advance(Duration.ofMinutes(10));
		}
		assertFalse(limits.register("203.0.113.7"));
		assertTrue(limits.register("198.51.100.1"));

		// the window moves on: the first registration is an hour old
		clock.advance(RegistrationLimits.WINDOW.minusMinutes(30));
		assertTrue(limits.register("203.0.113.7"));
		assertFalse(limits.register("203.0.113.7"));
	}

	@Test
	void everybodyRegistersSomeTimesAnHour()
	{
		for (int i = 0; i < RegistrationLimits.REGISTRATIONS; i++)
		{
			assertTrue(limits.register("203.0.113." + i));
		}
		assertFalse(limits.register("198.51.100.1"));
		assertFalse(limits.register(null));

		clock.advance(RegistrationLimits.WINDOW);
		assertTrue(limits.register("198.51.100.1"));
	}

	@Test
	void aRecipientGetsAFewEmailsAnHour()
	{
		for (int i = 0; i < RegistrationLimits.MAILS_PER_RECIPIENT; i++)
		{
			assertTrue(limits.mail("203.0.113." + i, "ada@example.com"));
		}
		assertFalse(limits.mail("198.51.100.1", "Ada@Example.com"));
		assertTrue(limits.mail("198.51.100.1", "grace@example.com"));
	}

	@Test
	void anAddressAsksForAFewEmailsAnHour()
	{
		for (int i = 0; i < RegistrationLimits.MAILS_PER_ADDRESS; i++)
		{
			assertTrue(limits.mail("203.0.113.7", "person" + i + "@example.com"));
		}
		assertFalse(limits.mail("203.0.113.7", "another@example.com"));
		assertTrue(limits.mail("198.51.100.1", "another@example.com"));
	}

	@Test
	void everybodyGetsSomeEmailsAnHour()
	{
		for (int i = 0; i < RegistrationLimits.MAILS; i++)
		{
			assertTrue(limits.mail(null, "person" + i + "@example.com"));
		}
		assertFalse(limits.mail("198.51.100.1", "another@example.com"));

		clock.advance(RegistrationLimits.WINDOW);
		assertTrue(limits.mail("198.51.100.1", "another@example.com"));
	}

	@Test
	void aChallengeAnswersOnce()
	{
		String challenge = limits.newChallenge();
		assertNotEquals(challenge, limits.newChallenge());

		assertTrue(limits.answer(challenge));
		assertFalse(limits.answer(challenge));
		assertFalse(limits.answer("made-up"));
		assertFalse(limits.answer(null));
	}

	@Test
	void anOldChallengeAnswersNoMore()
	{
		String challenge = limits.newChallenge();
		clock.advance(RegistrationLimits.CHALLENGE_TIME.plusSeconds(1));

		assertFalse(limits.answer(challenge));
	}
}
