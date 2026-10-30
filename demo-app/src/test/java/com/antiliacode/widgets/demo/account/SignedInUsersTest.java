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
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.data.Role;

/**
 * At most {@value SignedInUsers#MAXIMUM} accounts are signed in at once, administrators and the
 * accounts always signed in apart; a session stops counting when it signs out, ends or makes no
 * request for the timeout.
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

	/** The accounts the demo always has and one the server is started with. */
	private final Set<String> alwaysSignedIn = Set.of("semedy", "wicket", "client");

	private final SignedInUsers users = new SignedInUsers(clock, Duration.ofMinutes(5),
		Duration.ofMinutes(30), alwaysSignedIn::contains);

	/** Signs user1 to user5 in, a minute apart, as s1 to s5. */
	private void fillAMinuteApart()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			if (i > 1)
			{
				clock.advance(Duration.ofMinutes(1));
			}
			assertTrue(signIn("user" + i, Role.USER, "s" + i));
		}
	}

	/** Keeps every session signed in, as requests every few minutes would. */
	private void allSeen()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			users.seen("s" + i);
		}
	}

	/** Lets time go by while every session makes a request every four minutes. */
	private void keepActive(Duration duration)
	{
		for (Duration gone = Duration.ZERO; gone.compareTo(duration) < 0; gone = gone
			.plusMinutes(4))
		{
			clock.advance(Duration.ofMinutes(4));
			allSeen();
		}
	}

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
	void theAccountsOfTheDemoAndOfTheServerAlwaysSignInAndAreNotCounted()
	{
		fill();

		assertTrue(users.hasRoomFor("client", Role.USER));
		assertTrue(signIn("client", Role.USER, "client-session"));
		assertTrue(signIn("wicket", Role.USER, "wicket-session"));
		assertEquals(5, users.count());
		assertFalse(signIn("user6", Role.USER, "s6"));

		// nor do they take a place when there is room
		users.signedOut("s1");
		assertTrue(signIn("user6", Role.USER, "s6"));
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

	@Test
	void anAccountKeepsItsPlaceForThePlaceTime()
	{
		fill();
		keepActive(Duration.ofMinutes(28));

		assertFalse(signIn("user6", Role.USER, "s6"));
		assertEquals(Duration.ofMinutes(2), users.untilRoom());
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			assertTrue(users.holdsPlace("s" + i));
		}
	}

	@Test
	void beyondItsTimeAnotherAccountTakesThePlaceHeldLongest()
	{
		fillAMinuteApart();
		keepActive(Duration.ofMinutes(28));

		// user1 signed in 32 minutes ago, user2 31, user3 30, user4 29 and user5 28
		assertEquals(Duration.ZERO, users.untilRoom());
		assertTrue(users.hasRoomFor("user6", Role.USER));
		assertTrue(signIn("user6", Role.USER, "s6"));
		assertFalse(users.holdsPlace("s1"));
		assertTrue(users.holdsPlace("s2"));
		assertTrue(users.holdsPlace("s6"));
		assertEquals(5, users.count());

		// the next one takes the place of user2, and user1, signing in again, that of user3
		assertTrue(signIn("user7", Role.USER, "s7"));
		assertFalse(users.holdsPlace("s2"));
		assertTrue(signIn("user1", Role.USER, "s1-again"));
		assertFalse(users.holdsPlace("s3"));

		// user4 and user5, and the newcomers, are within their time
		assertFalse(signIn("user8", Role.USER, "s8"));
		assertEquals(Duration.ofMinutes(1), users.untilRoom());
	}

	@Test
	void anAccountBeyondItsTimeGoesOnWhileNobodyNeedsThePlace()
	{
		fill();
		keepActive(Duration.ofHours(2));

		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			assertTrue(users.holdsPlace("s" + i));
		}
		assertEquals(5, users.count());
	}

	@Test
	void anotherBrowserDoesNotRenewThePlace()
	{
		fillAMinuteApart();
		keepActive(Duration.ofMinutes(24));
		assertTrue(signIn("user1", Role.USER, "s1-another-browser"));
		keepActive(Duration.ofMinutes(4));

		// the place of user1 counts from its first session; both of them lose it
		assertTrue(signIn("user6", Role.USER, "s6"));
		assertFalse(users.holdsPlace("s1"));
		assertFalse(users.holdsPlace("s1-another-browser"));
	}

	@Test
	void theAccountsNotCountedTakeNoPlace()
	{
		fill();
		keepActive(Duration.ofMinutes(40));

		assertTrue(signIn("admin", Role.ADMIN, "admin-session"));
		assertTrue(signIn("client", Role.USER, "client-session"));
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			assertTrue(users.holdsPlace("s" + i));
		}
		assertFalse(users.holdsPlace("admin-session"));
	}
}
