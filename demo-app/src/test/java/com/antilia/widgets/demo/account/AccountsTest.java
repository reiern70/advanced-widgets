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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Registers and authenticates {@link Accounts}.
 */
class AccountsTest
{
	@TempDir
	Path folder;

	private File file()
	{
		return new File(folder.toFile(), "accounts.properties");
	}

	@Test
	void configuredAccountsSignInWithTheirPasswords()
	{
		Accounts accounts = new Accounts(file(), "client:secret-one, other:secret-two");

		assertEquals(Optional.of("client"), accounts.authenticate("Client ", "secret-one"));
		assertEquals(Optional.of("other"), accounts.authenticate("other", "secret-two"));
		assertFalse(accounts.authenticate("client", "secret-two").isPresent());
		assertFalse(accounts.authenticate("nobody", "secret-one").isPresent());
		assertFalse(accounts.authenticate("client", "").isPresent());
		assertFalse(accounts.authenticate(null, null).isPresent());
	}

	@Test
	void registeredAccountsAreKeptHashed() throws Exception
	{
		Accounts accounts = new Accounts(file(), "");

		assertTrue(accounts.register("Ada@Example.com", "analytical-engine"));

		String kept = Files.readString(file().toPath());
		assertTrue(kept.contains("ada@example.com"), kept);
		assertFalse(kept.contains("analytical-engine"), kept);
		// another server reading the same file knows the account
		Accounts restarted = new Accounts(file(), "");
		assertEquals(Optional.of("ada@example.com"),
			restarted.authenticate("ada@example.com", "analytical-engine"));
		assertFalse(restarted.authenticate("ada@example.com", "difference-engine").isPresent());
	}

	@Test
	void anAccountIsRegisteredOnlyOnce()
	{
		Accounts accounts = new Accounts(file(), "client:secret-one");

		assertTrue(accounts.register("ada@example.com", "analytical-engine"));
		assertFalse(accounts.register(" ADA@example.com", "difference-engine"));
		assertFalse(accounts.register("client", "taken-over"));
		assertTrue(accounts.authenticate("client", "secret-one").isPresent());
		assertFalse(accounts.authenticate("client", "taken-over").isPresent());
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
