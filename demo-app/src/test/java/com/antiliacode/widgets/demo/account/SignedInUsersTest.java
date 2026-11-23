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
 * At most {@value SignedInUsers#MAXIMUM} sessions are signed in as {@value Accounts#USER} at once,
 * the administrator apart; a session stops counting when it signs out, ends or makes no request
 * for the timeout, and one held beyond {@link SignedInUsers#PLACE_TIME} goes to a newcomer.
 */
class SignedInUsersTest
{
	private final TestClock clock = new TestClock();

	private final SignedInUsers users = new SignedInUsers(clock, Duration.ofMinutes(5),
		Duration.ofMinutes(30));

	/** Signs s1 to s10 in, a minute apart. */
	private void fillAMinuteApart()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			if (i > 1)
			{
				clock.advance(Duration.ofMinutes(1));
			}
			String id = "s" + i;
			assertTrue(users.signIn(Role.USER, () -> id), id);
			keepAllSeen();
		}
	}

	/** A request of every session signed in, so that none times out. */
	private void keepAllSeen()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			users.seen("s" + i);
		}
	}

	@Test
	void tenSessionsSignInAndTheEleventhWaits()
	{
		fillAMinuteApart();
		assertEquals(SignedInUsers.MAXIMUM, users.count());

		assertFalse(users.signIn(Role.USER, () -> "s11"));
		assertFalse(users.hasRoom());
		assertFalse(users.holdsPlace("s11"));
		// the first place is sure for 30 minutes from its start, 9 minutes ago
		assertEquals(Duration.ofMinutes(21), users.untilRoom());
	}

	@Test
	void theAdministratorAlwaysSignsInAndTakesNoPlace()
	{
		fillAMinuteApart();

		assertTrue(users.signIn(Role.ADMIN, () -> "admin-session"));
		assertFalse(users.holdsPlace("admin-session"));
		assertEquals(SignedInUsers.MAXIMUM, users.count());
	}

	@Test
	void signingOutGivesThePlaceBack()
	{
		fillAMinuteApart();

		users.signedOut("s4");

		assertEquals(SignedInUsers.MAXIMUM - 1, users.count());
		assertTrue(users.hasRoom());
		assertTrue(users.signIn(Role.USER, () -> "s11"));
		assertTrue(users.holdsPlace("s11"));
	}

	@Test
	void aSessionWithoutRequestsStopsCounting()
	{
		fillAMinuteApart();
		clock.advance(Duration.ofMinutes(4));
		for (int i = 2; i <= SignedInUsers.MAXIMUM; i++)
		{
			users.seen("s" + i);
		}
		clock.advance(Duration.ofMinutes(2));

		// s1 made no request for 6 minutes, more than the timeout of 5
		assertFalse(users.holdsPlace("s1"));
		assertEquals(SignedInUsers.MAXIMUM - 1, users.count());
	}

	@Test
	void aPlaceHeldBeyondItsTimeGoesToANewcomer()
	{
		fillAMinuteApart();
		// s1 signed in 30 minutes ago, the others later
		clock.advance(Duration.ofMinutes(21));
		keepAllSeen();

		assertTrue(users.hasRoom());
		assertTrue(users.signIn(Role.USER, () -> "s11"));

		assertFalse(users.holdsPlace("s1"), "the place held longest goes");
		assertTrue(users.holdsPlace("s2"));
		assertTrue(users.holdsPlace("s11"));
		assertEquals(SignedInUsers.MAXIMUM, users.count());
		// the next newcomer waits for s2's time, a minute later
		assertFalse(users.signIn(Role.USER, () -> "s12"));
		assertEquals(Duration.ofMinutes(1), users.untilRoom());
	}

	@Test
	void thePlacesAreListedTheOneHeldLongestFirst()
	{
		fillAMinuteApart();

		assertEquals(SignedInUsers.MAXIMUM, users.places().size());
		assertTrue(users.places().get(0).since().isBefore(users.places().get(1).since()));
	}
}
