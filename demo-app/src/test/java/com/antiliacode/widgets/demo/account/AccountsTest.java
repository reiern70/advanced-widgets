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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Two accounts, in memory: {@value Accounts#USER}, whose password is in the source, and the
 * administrator, whose password the server is given; see {@link Accounts}.
 */
class AccountsTest
{
	@Test
	void everybodySignsInAsUserWithThePasswordOfTheSource()
	{
		Accounts accounts = new Accounts("admin-secret");

		assertEquals(Optional.of(Role.USER),
			accounts.authenticate("user", "$Amigo_MEllon_Friend_4321"));
		assertEquals(Optional.of(Role.USER),
			accounts.authenticate(" User ", Accounts.USER_PASSWORD));
		assertTrue(accounts.authenticate("user", "$amigo_mellon_friend_4321").isEmpty());
		assertTrue(accounts.authenticate("user", "admin-secret").isEmpty());
	}

	@Test
	void theAdministratorSignsInWithThePasswordOfTheServer()
	{
		Accounts accounts = new Accounts("admin-secret");

		assertEquals(Optional.of(Role.ADMIN), accounts.authenticate("admin", "admin-secret"));
		assertEquals(Optional.of(Role.ADMIN), accounts.authenticate("ADMIN", "admin-secret"));
		assertTrue(accounts.authenticate("admin", Accounts.USER_PASSWORD).isEmpty());
		assertEquals(Optional.of(Role.ADMIN), accounts.find("admin"));
	}

	@Test
	void withoutAPasswordThereIsNoAdministrator()
	{
		for (String none : new String[] { null, "" })
		{
			Accounts accounts = new Accounts(none);

			assertTrue(accounts.authenticate("admin", "").isEmpty());
			assertTrue(accounts.authenticate("admin", "anything").isEmpty());
			assertTrue(accounts.find("admin").isEmpty());
			assertEquals(Optional.of(Role.USER), accounts.find("user"));
		}
	}

	@Test
	void thereAreNoOtherAccounts()
	{
		Accounts accounts = new Accounts("admin-secret");

		for (String name : new String[] { "semedy", "wicket", "client", "", null })
		{
			assertTrue(accounts.authenticate(name, Accounts.USER_PASSWORD).isEmpty(), name);
			assertTrue(accounts.authenticate(name, "admin-secret").isEmpty(), name);
			assertTrue(accounts.find(name).isEmpty(), name);
		}
		assertTrue(accounts.authenticate("user", null).isEmpty());
	}

	@Test
	void theAdministratorsPasswordComesFromTheSystemProperty()
	{
		System.setProperty(Accounts.ADMIN_PASSWORD_PROPERTY, "from-the-property");
		try
		{
			assertEquals(Optional.of(Role.ADMIN),
				new Accounts().authenticate("admin", "from-the-property"));
		}
		finally
		{
			System.setProperty(Accounts.ADMIN_PASSWORD_PROPERTY, "admin-test-password");
		}
	}
}
