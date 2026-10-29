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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.TestClock;

/**
 * The failed attempts to sign in ask for a captcha, then make the account or the address wait,
 * longer after each failure but never for good, whatever the names tried in between.
 */
class SignInAttemptsTest
{
	private static final String ADDRESS = "203.0.113.7";

	private final TestClock clock = new TestClock();

	private final SignInAttempts attempts = new SignInAttempts(clock);

	private void fail(String name, int times)
	{
		for (int i = 0; i < times; i++)
		{
			attempts.failed(name, ADDRESS, true);
		}
	}

	@Test
	void anAccountAsksForACaptchaThenWaitsLongerAfterEachFailure()
	{
		fail("admin", SignInAttempts.BEFORE_CAPTCHA - 1);
		assertFalse(attempts.needsCaptcha("admin"));
		fail("admin", 1);
		assertTrue(attempts.needsCaptcha("admin"));
		assertTrue(attempts.needsCaptcha(" Admin "));
		assertFalse(attempts.needsCaptcha("wicket"));
		assertEquals(Duration.ZERO, attempts.retryAfter("admin", ADDRESS));

		fail("admin", SignInAttempts.BEFORE_WAITING - SignInAttempts.BEFORE_CAPTCHA);
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("admin", ADDRESS));
		// whatever the address
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("admin", null));
		assertEquals(Duration.ZERO, attempts.retryAfter("wicket", null));

		clock.advance(Duration.ofSeconds(1));
		assertEquals(Duration.ZERO, attempts.retryAfter("admin", ADDRESS));
		fail("admin", 1);
		assertEquals(Duration.ofSeconds(2), attempts.retryAfter("admin", ADDRESS));
		fail("admin", 1);
		assertEquals(Duration.ofSeconds(4), attempts.retryAfter("admin", ADDRESS));
	}

	@Test
	void theWaitHasALimit()
	{
		assertEquals(Duration.ZERO, SignInAttempts.waitAfter(4, 5));
		assertEquals(Duration.ofSeconds(1), SignInAttempts.waitAfter(5, 5));
		assertEquals(Duration.ofSeconds(512), SignInAttempts.waitAfter(14, 5));
		assertEquals(SignInAttempts.LONGEST_WAIT, SignInAttempts.waitAfter(15, 5));
		assertEquals(SignInAttempts.LONGEST_WAIT, SignInAttempts.waitAfter(1_000_000, 5));

		fail("admin", 100);
		assertEquals(SignInAttempts.LONGEST_WAIT, attempts.retryAfter("admin", null));
		// nobody is locked out for good
		clock.advance(SignInAttempts.LONGEST_WAIT);
		assertEquals(Duration.ZERO, attempts.retryAfter("admin", null));
	}

	@Test
	void signingInForgetsTheFailuresOfTheAccount()
	{
		fail("admin", SignInAttempts.BEFORE_WAITING);

		attempts.succeeded("Admin");

		assertFalse(attempts.needsCaptcha("admin"));
		assertEquals(Duration.ZERO, attempts.retryAfter("admin", null));
	}

	@Test
	void theFailuresAreForgottenADayAfterTheLast()
	{
		fail("admin", SignInAttempts.BEFORE_CAPTCHA);
		clock.advance(SignInAttempts.ACCOUNT_MEMORY.minusMinutes(1));
		assertTrue(attempts.needsCaptcha("admin"));
		clock.advance(Duration.ofMinutes(2));
		assertFalse(attempts.needsCaptcha("admin"));
	}

	@Test
	void namesOfNoAccountAskForACaptchaAndWaitAsAccountsDo()
	{
		for (int i = 0; i < SignInAttempts.BEFORE_WAITING; i++)
		{
			attempts.failed("nobody", null, false);
		}

		assertTrue(attempts.needsCaptcha("nobody"));
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("nobody", null));
	}

	@Test
	void aFloodOfNamesOfNoAccountKeepsTheFailuresOfTheAccounts()
	{
		fail("admin", SignInAttempts.BEFORE_WAITING);

		for (int i = 0; i <= SignInAttempts.MAXIMUM_ENTRIES; i++)
		{
			attempts.failed("nobody-" + i, null, false);
		}

		assertTrue(attempts.needsCaptcha("admin"));
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("admin", null));
		// the oldest of the names went first
		assertFalse(attempts.needsCaptcha("nobody-0"));
	}

	@Test
	void anAddressTryingManyAccountsWaits()
	{
		for (int i = 0; i < SignInAttempts.BEFORE_ADDRESS_WAITING - 1; i++)
		{
			attempts.failed("user-" + i, ADDRESS, i % 2 == 0);
		}
		assertEquals(Duration.ZERO, attempts.retryAfter("another", ADDRESS));

		attempts.failed("user-last", ADDRESS, false);

		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("another", ADDRESS));
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter(null, ADDRESS));
		// another address does not
		assertEquals(Duration.ZERO, attempts.retryAfter("another", "198.51.100.1"));
		// signing in to an account does not make the address forget
		attempts.succeeded("user-0");
		assertEquals(Duration.ofSeconds(1), attempts.retryAfter("another", ADDRESS));

		// an hour after its last failure the address is forgotten
		clock.advance(SignInAttempts.ADDRESS_MEMORY.plusSeconds(1));
		assertEquals(Duration.ZERO, attempts.retryAfter("another", ADDRESS));
	}
}
