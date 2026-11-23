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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.TestClock;

/**
 * Ten failures from an address block it for 30 minutes, whatever the account; see
 * {@link SignInAttempts}.
 */
class SignInAttemptsTest
{
	private final TestClock clock = new TestClock();

	private final SignInAttempts attempts = new SignInAttempts(clock);

	@Test
	void tenFailuresBlockTheAddressForThirtyMinutes()
	{
		for (int i = 1; i < SignInAttempts.MAXIMUM_FAILURES; i++)
		{
			attempts.failed("203.0.113.7");
			assertEquals(Duration.ZERO, attempts.retryAfter("203.0.113.7"), "after " + i);
		}
		attempts.failed("203.0.113.7");

		assertEquals(Duration.ofMinutes(30), attempts.retryAfter("203.0.113.7"));
		// another address is not blocked
		assertEquals(Duration.ZERO, attempts.retryAfter("198.51.100.1"));

		clock.advance(Duration.ofMinutes(29));
		assertEquals(Duration.ofMinutes(1), attempts.retryAfter("203.0.113.7"));
		clock.advance(Duration.ofMinutes(1));
		assertEquals(Duration.ZERO, attempts.retryAfter("203.0.113.7"));
		// and forgotten
		assertEquals(0, attempts.size());
	}

	@Test
	void theFailuresAreForgottenThirtyMinutesAfterTheLast()
	{
		for (int i = 1; i < SignInAttempts.MAXIMUM_FAILURES; i++)
		{
			attempts.failed("203.0.113.7");
		}
		clock.advance(SignInAttempts.BLOCKED);

		attempts.failed("203.0.113.7");

		assertEquals(Duration.ZERO, attempts.retryAfter("203.0.113.7"));
	}

	@Test
	void signingInForgetsTheFailuresOfTheAddress()
	{
		for (int i = 1; i < SignInAttempts.MAXIMUM_FAILURES; i++)
		{
			attempts.failed("203.0.113.7");
		}
		attempts.succeeded("203.0.113.7");
		attempts.failed("203.0.113.7");

		assertEquals(Duration.ZERO, attempts.retryAfter("203.0.113.7"));
	}

	@Test
	void anUnknownAddressIsNeverBlocked()
	{
		for (int i = 0; i < 20; i++)
		{
			attempts.failed(null);
		}

		assertEquals(Duration.ZERO, attempts.retryAfter(null));
		assertEquals(0, attempts.size());
	}
}
