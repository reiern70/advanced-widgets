/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.data.User;

/**
 * Registers and authenticates {@link Accounts}, kept in the table {@code users}.
 */
class AccountsTest
{
	private Accounts accounts;

	@BeforeEach
	void clear()
	{
		TestDatabase.clear();
		accounts = TestDatabase.accounts();
	}

	@Test
	void theDemoAlwaysHasItsAccounts()
	{
		assertEquals(Role.USER, accounts.authenticate("semedy", "semedy4321").map(User::getRole)
			.orElseThrow());
		assertEquals(Role.USER, accounts.authenticate("wicket", "wicket4321").map(User::getRole)
			.orElseThrow());
		assertEquals(Role.ADMIN, accounts.authenticate("admin", "$Macarena_4321")
			.map(User::getRole).orElseThrow());
		assertFalse(accounts.authenticate("admin", "wicket4321").isPresent());

		// adding them again changes nothing
		accounts.addDefaults();
		assertEquals(3, TestDatabase.users().size());
	}

	@Test
	void configuredAccountsSignInWithTheirPasswords()
	{
		accounts.addConfigured("client:secret-one, other:secret-two");

		assertEquals("client", accounts.authenticate("Client ", "secret-one").orElseThrow()
			.getName());
		assertEquals("other", accounts.authenticate("other", "secret-two").orElseThrow()
			.getName());
		assertFalse(accounts.authenticate("client", "secret-two").isPresent());
		assertFalse(accounts.authenticate("nobody", "secret-one").isPresent());
		assertFalse(accounts.authenticate("client", "").isPresent());
		assertFalse(accounts.authenticate(null, null).isPresent());
	}

	@Test
	void registeredAccountsAreKeptHashed()
	{
		assertTrue(accounts.register("Ada@Example.com", "analytical-engine", user -> {}).isPresent());

		User ada = TestDatabase.users().stream()
			.filter(user -> user.getName().equals("ada@example.com"))
			.findFirst()
			.orElseThrow();
		assertFalse(ada.getPasswordHash().contains("analytical-engine"));
		assertEquals(Role.USER, ada.getRole());
		// the email is the name of the account, and kept apart too
		assertEquals("ada@example.com", ada.getEmail());
		assertTrue(accounts.authenticate("ada@example.com", "analytical-engine").isPresent());
		assertFalse(accounts.authenticate("ada@example.com", "difference-engine").isPresent());
	}

	@Test
	void anAccountIsRegisteredOnlyOnce()
	{
		accounts.addConfigured("client:secret-one");

		assertTrue(accounts.register("ada@example.com", "analytical-engine", user -> {}).isPresent());
		assertFalse(accounts.register(" ADA@example.com", "difference-engine", user -> {}).isPresent());
		assertFalse(accounts.register("client", "taken-over", user -> {}).isPresent());
		assertFalse(accounts.register("admin", "taken-over", user -> {}).isPresent());
		assertTrue(accounts.authenticate("client", "secret-one").isPresent());
		assertFalse(accounts.authenticate("client", "taken-over").isPresent());
	}

	@Test
	void theAccountsOfEarlierVersionsAreImported(@TempDir Path folder) throws Exception
	{
		File file = new File(folder.toFile(), "antilia-widgets-accounts.properties");
		Files.writeString(file.toPath(), "grace@example.com=" +
			Accounts.hash("compiler").replace(":", "\\:").replace("=", "\\=") + "\n");

		accounts.importFile(file);
		accounts.importFile(new File(folder.toFile(), "missing.properties"));

		assertTrue(accounts.authenticate("grace@example.com", "compiler").isPresent());
	}

	@Test
	void hashesAreSalted()
	{
		String once = Accounts.hash("same password");
		String twice = Accounts.hash("same password");

		assertFalse(once.equals(twice));
		assertTrue(Accounts.matches("same password", once));
		assertTrue(Accounts.matches("same password", twice));
		assertFalse(Accounts.matches("same password", "not a hash"));
	}
}
