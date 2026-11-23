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
