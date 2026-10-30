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

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * A registered account keeps its place for {@link SignedInUsers#PLACE_TIME}; once another account
 * takes it, the session is signed out at its next request and the sign-in page tells why, once.
 */
class PlaceTakenTest
{
	private final TestClock clock = new TestClock();

	private SignedInUsers users;

	private WicketTester tester;

	private String ada;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		users = new SignedInUsers(clock, WicketApplication.SESSION_TIMEOUT,
			SignedInUsers.PLACE_TIME, account -> Accounts.get().isAlwaysSignedIn(account));
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}

			@Override
			public SignedInUsers getSignedInUsers()
			{
				return users;
			}
		});
		Accounts accounts = Accounts.get();
		ada = accounts.validate(accounts
			.register("ada@example.com", "Analytical-Engine-1843", user -> {})
			.orElseThrow()).orElseThrow().getName();

		tester.startPage(HomePage.class);
		assertTrue(DemoSession.get().signIn(ada, Role.USER));
		// the four other places are taken a minute later
		clock.advance(Duration.ofMinutes(1));
		for (int i = 2; i <= SignedInUsers.MAXIMUM; i++)
		{
			String id = "other-" + i;
			assertTrue(users.signIn("user" + i, Role.USER, () -> id));
		}
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/** Lets time go by while every session makes a request every four minutes. */
	private void keepActive(Duration duration)
	{
		for (Duration gone = Duration.ZERO; gone.compareTo(duration) < 0; gone = gone
			.plusMinutes(4))
		{
			clock.advance(Duration.ofMinutes(4));
			tester.startPage(DropdownPage.class);
			tester.assertRenderedPage(DropdownPage.class);
			for (int i = 2; i <= SignedInUsers.MAXIMUM; i++)
			{
				users.seen("other-" + i);
			}
		}
	}

	@Test
	void withinItsTimeNobodyTakesThePlace()
	{
		keepActive(Duration.ofMinutes(28));

		assertFalse(users.signIn("newcomer", Role.USER, () -> "newcomer-session"));
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
		assertEquals(ada, DemoSession.get().getAccount());
	}

	@Test
	void aSessionWhosePlaceIsTakenIsSignedOutAndTold()
	{
		keepActive(Duration.ofMinutes(32));
		// still signed in: nobody needed the place
		assertEquals(ada, DemoSession.get().getAccount());

		assertTrue(users.signIn("newcomer", Role.USER, () -> "newcomer-session"));

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);
		assertFalse(DemoSession.get().isSignedIn());
		tester.assertErrorMessages("Another user took your place: each user keeps it for 30 " +
			"minutes for sure, and at most 5 are signed in at once. Sign in again to go on, " +
			"when there is room.");

		// told once
		tester.startPage(SignInPage.class);
		tester.assertNoErrorMessage();
	}
}
