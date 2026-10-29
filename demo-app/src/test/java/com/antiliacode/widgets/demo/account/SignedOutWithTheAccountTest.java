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

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * A session signed in does not outlive its account: once the account is removed, locked or has
 * another role, the next request of the session is signed out.
 */
class SignedOutWithTheAccountTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
		Accounts.get().addConfigured("client:secret-one, other:secret-two");
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private void signIn(String account, Role role)
	{
		tester.startPage(HomePage.class);
		assertTrue(DemoSession.get().signIn(account, role));
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
	}

	@Test
	void aRemovedAccountIsSignedOutAtTheNextRequest()
	{
		signIn("client", Role.USER);

		Accounts.get().remove(Accounts.get().find("client").orElseThrow());

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);
		assertFalse(DemoSession.get().isSignedIn());
		assertEquals(0, SignedInUsers.get().count());
	}

	@Test
	void aLockedAccountIsSignedOutAtTheNextRequest()
	{
		signIn("client", Role.USER);

		// the server is no longer started with it
		Accounts.get().addConfigured("other:secret-two");

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void anAccountWithAnotherRoleIsSignedOut()
	{
		// signed in as an administrator, which the account is not, or no longer
		signIn("wicket", Role.ADMIN);

		// another account changes, which makes every session look at its account
		Accounts.get().remove(Accounts.get().find("other").orElseThrow());

		tester.startPage(HomePage.class);
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void theSessionsOfOtherAccountsStaySignedIn()
	{
		signIn("client", Role.USER);

		Accounts.get().remove(Accounts.get().find("other").orElseThrow());

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
		assertEquals("client", DemoSession.get().getAccount());
	}
}
