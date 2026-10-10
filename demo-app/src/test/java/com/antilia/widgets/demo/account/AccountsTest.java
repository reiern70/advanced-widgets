/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
		assertEquals(Role.USER, accounts.authenticate("semedy", "semady4321").map(User::getRole)
			.orElseThrow());
		assertEquals(Role.USER, accounts.authenticate("wicket", "mellon4321").map(User::getRole)
			.orElseThrow());
		assertEquals(Role.ADMIN, accounts.authenticate("admin", "$Macarena_4321")
			.map(User::getRole).orElseThrow());
		assertFalse(accounts.authenticate("admin", "mellon4321").isPresent());

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
		assertTrue(accounts.register("Ada@Example.com", "analytical-engine"));

		User ada = TestDatabase.users().stream()
			.filter(user -> user.getName().equals("ada@example.com"))
			.findFirst()
			.orElseThrow();
		assertFalse(ada.getPasswordHash().contains("analytical-engine"));
		assertEquals(Role.USER, ada.getRole());
		assertTrue(accounts.authenticate("ada@example.com", "analytical-engine").isPresent());
		assertFalse(accounts.authenticate("ada@example.com", "difference-engine").isPresent());
	}

	@Test
	void anAccountIsRegisteredOnlyOnce()
	{
		accounts.addConfigured("client:secret-one");

		assertTrue(accounts.register("ada@example.com", "analytical-engine"));
		assertFalse(accounts.register(" ADA@example.com", "difference-engine"));
		assertFalse(accounts.register("client", "taken-over"));
		assertFalse(accounts.register("admin", "taken-over"));
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
