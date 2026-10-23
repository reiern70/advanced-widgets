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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.data.Role;

/**
 * At most {@value SignedInUsers#MAXIMUM} accounts are signed in at once, administrators apart; a
 * session stops counting when it signs out, ends or makes no request for the timeout.
 */
class SignedInUsersTest
{
	/** A clock the test moves on. */
	private static class TestClock extends Clock
	{
		private Instant now = Instant.parse("2026-10-15T10:00:00Z");

		void advance(Duration duration)
		{
			now = now.plus(duration);
		}

		@Override
		public Instant instant()
		{
			return now;
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
	}

	private final TestClock clock = new TestClock();

	private final SignedInUsers users = new SignedInUsers(clock, Duration.ofMinutes(5));

	private boolean signIn(String account, Role role, String sessionId)
	{
		return users.signIn(account, role, () -> sessionId);
	}

	private void fill()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			assertTrue(signIn("user" + i, Role.USER, "s" + i));
		}
	}

	@Test
	void theSixthAccountWaits()
	{
		fill();

		assertEquals(5, users.count());
		assertFalse(signIn("user6", Role.USER, "s6"));
		assertFalse(users.hasRoomFor("user6", Role.USER));
		assertEquals(5, users.count());
	}

	@Test
	void anAccountSignedInAlreadySignsInAgainAndCountsOnce()
	{
		fill();

		assertTrue(signIn("user1", Role.USER, "another-browser"));
		assertEquals(5, users.count());
	}

	@Test
	void administratorsAlwaysSignInAndAreNotCounted()
	{
		fill();

		assertTrue(signIn("admin", Role.ADMIN, "admin-session"));
		assertEquals(5, users.count());
	}

	@Test
	void signingOutOrEndingMakesRoom()
	{
		fill();

		users.signedOut("s1");
		assertEquals(4, users.count());
		assertTrue(signIn("user6", Role.USER, "s6"));
		assertFalse(signIn("user7", Role.USER, "s7"));
	}

	@Test
	void aSessionWithoutRequestsForTheTimeoutStopsCounting()
	{
		fill();
		clock.advance(Duration.ofMinutes(4));
		for (int i = 2; i <= SignedInUsers.MAXIMUM; i++)
		{
			users.seen("s" + i);
		}

		clock.advance(Duration.ofMinutes(2));
		// s1 went 6 minutes without a request, the others 2
		assertEquals(4, users.count());
		assertTrue(signIn("user6", Role.USER, "s6"));

		// a request of a session signed out does not count it again
		users.seen("s1");
		assertEquals(5, users.count());
	}
}
