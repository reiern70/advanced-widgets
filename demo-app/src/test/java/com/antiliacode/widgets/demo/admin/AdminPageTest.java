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
package com.antiliacode.widgets.demo.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.Role;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.stats.StatsResource;
import com.antiliacode.widgets.demo.stats.VisitorCookie;
import com.antiliacode.widgets.demo.stats.Visits;

/**
 * The {@link AdminPage}: how many sessions are signed in as {@value Accounts#USER}, and since when,
 * nothing telling who is behind one; for the administrator only.
 */
class AdminPageTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}

			@Override
			protected Visits newVisits()
			{
				return new Visits(null, Clock.systemUTC());
			}
		});
		tester.startPage(HomePage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void theAdministratorSeesTheSessionsSignedIn()
	{
		SignedInUsers.get().signIn(Role.USER, () -> "session-1");
		SignedInUsers.get().signIn(Role.USER, () -> "session-2");
		DemoSession.get().signIn(Accounts.ADMIN, Role.ADMIN);

		tester.startPage(AdminPage.class);

		tester.assertRenderedPage(AdminPage.class);
		tester.assertContains("2 of 10 sessions are signed in as user.");
		tester.assertVisible("table");
		String since = AdminPage.format(SignedInUsers.get().places().get(0).since());
		tester.assertLabel("table:places:0:since", since);
		tester.assertLabel("table:places:0:sureUntil", AdminPage.format(
			SignedInUsers.get().places().get(0).since().plus(SignedInUsers.PLACE_TIME)));
		// nothing about who is behind a session, not even its id
		assertTrue(!tester.getLastResponseAsString().contains("session-1"));
	}

	@Test
	void withoutSessionsThereIsNoTable()
	{
		DemoSession.get().signIn(Accounts.ADMIN, Role.ADMIN);

		tester.startPage(AdminPage.class);

		tester.assertContains("0 of 10 sessions are signed in as user.");
		tester.assertInvisible("table");
	}

	@Test
	void theAdministratorSeesTheVisits()
	{
		String ann = VisitorCookie.newNumber();
		Visits.get().viewed("/tree", ann);
		Visits.get().viewed("/tree", null);
		Visits.get().signedIn(ann, Role.USER);
		DemoSession.get().signIn(Accounts.ADMIN, Role.ADMIN);

		tester.startPage(AdminPage.class);

		// the home page of start() counted too, to nobody
		tester.assertContains("1 people opened a page or signed in in the last 30 days, 1 of them " +
			"for the first time; 1 signed in, 1 times. 3 pages were shown, 2 of them without the " +
			"cookie");
		tester.assertContains("Counted since " + Visits.get().today());
		tester.assertLabel("pagesTable:pages:0:page", "/tree");
		tester.assertLabel("pagesTable:pages:0:views", "2");
		tester.assertLabel("pagesTable:pages:0:people", "1");
		tester.assertLabel("daysTable:days:0:signIns", "1");
		tester.assertContains(StatsResource.PATH);
	}

	@Test
	void everybodyElseIsTurnedAway()
	{
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(SignInPage.class);

		DemoSession.get().signIn(Accounts.USER, Role.USER);
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(com.antiliacode.widgets.demo.error.AccessDeniedPage.class);
	}

	@Test
	void theTimesAreInUtc()
	{
		assertTrue(AdminPage.format(Instant.parse("2026-11-23T10:15:30Z"))
			.equals("2026-11-23 10:15:30 UTC"));
	}
}
