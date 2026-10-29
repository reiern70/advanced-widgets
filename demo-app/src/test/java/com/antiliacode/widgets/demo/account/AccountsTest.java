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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;

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
		assertEquals(Role.USER, accounts.authenticate("semedy", TestDatabase.SEMEDY_PASSWORD)
			.map(User::getRole).orElseThrow());
		assertEquals(Role.USER, accounts.authenticate("wicket", TestDatabase.WICKET_PASSWORD)
			.map(User::getRole).orElseThrow());
		assertEquals(Role.ADMIN, accounts.authenticate("admin", TestDatabase.ADMIN_PASSWORD)
			.map(User::getRole).orElseThrow());
		assertFalse(accounts.authenticate("admin", TestDatabase.WICKET_PASSWORD).isPresent());

		// adding them again changes nothing
		String hash = accounts.find("admin").orElseThrow().getPasswordHash();
		accounts.addDefaults();
		assertEquals(3, TestDatabase.users().size());
		assertEquals(hash, accounts.find("admin").orElseThrow().getPasswordHash());
	}

	@Test
	void theAccountsTheDemoAlwaysHasTakeTheirPasswordsFromTheServer()
	{
		// another password replaces the one before
		accounts.addDefault("wicket", Role.USER, "another-test-password");
		assertTrue(accounts.authenticate("wicket", "another-test-password").isPresent());
		assertFalse(accounts.authenticate("wicket", TestDatabase.WICKET_PASSWORD).isPresent());

		// without a password, a missing account is not added
		accounts.remove(accounts.find("semedy").orElseThrow());
		accounts.addDefault("semedy", Role.USER, "");
		assertFalse(accounts.find("semedy").isPresent());
	}

	@Test
	void anAccountHoldingAPasswordTheSourceOnceGaveIsLocked()
	{
		// the hash V4__wicket_password.sql gives the account wicket
		User wicket = accounts.find("wicket").orElseThrow();
		wicket.setPasswordHash("6Fi23jr/izCqXM4Bn7yKxQ==:sLba9BDFyxARONbZ1H5Vi2KadTleKeMw/qqtJwg728M=");
		TestDatabase.context().getBean(IUserRepository.class).save(wicket);

		accounts.addDefault("wicket", Role.USER, "");
		assertEquals(Accounts.LOCKED, accounts.find("wicket").orElseThrow().getPasswordHash());

		// a password from the server opens it again
		accounts.addDefault("wicket", Role.USER, TestDatabase.WICKET_PASSWORD);
		assertTrue(accounts.authenticate("wicket", TestDatabase.WICKET_PASSWORD).isPresent());
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
			legacyHash("compiler").replace(":", "\\:").replace("=", "\\=") + "\n");

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
		assertFalse(Accounts.matches("other password", once));
		assertFalse(Accounts.matches("same password", "not a hash"));
		assertFalse(Accounts.matches("same password", Accounts.LOCKED));
		assertFalse(Accounts.matches("same password", null));
	}

	@Test
	void hashesNameTheirAlgorithmAndIterations()
	{
		String hash = Accounts.hash("same password");

		String[] parts = hash.split("\\$");
		assertEquals(4, parts.length, hash);
		assertEquals("pbkdf2-sha256", parts[0]);
		assertEquals("600000", parts[1]);
		assertEquals(16, Base64.getDecoder().decode(parts[2]).length);
		assertEquals(32, Base64.getDecoder().decode(parts[3]).length);
		assertFalse(Accounts.isOutdated(hash));
		// the column password_hash holds it
		assertTrue(hash.length() <= 200, hash);

		// a hash naming iterations it cannot have matches nothing
		assertFalse(Accounts.matches("same password", "pbkdf2-sha256$0$" + parts[2] + "$" +
			parts[3]));
		assertFalse(Accounts.matches("same password", "pbkdf2-sha256$x$" + parts[2] + "$" +
			parts[3]));
	}

	@Test
	void aHashOfAnEarlierVersionSignsInAndIsMadeAnew()
	{
		String legacy = legacyHash("analytical-engine");
		assertTrue(Accounts.matches("analytical-engine", legacy));
		assertFalse(Accounts.matches("difference-engine", legacy));
		assertTrue(Accounts.isOutdated(legacy));

		User ada = accounts.create("ada@example.com", "analytical-engine", Role.USER, user -> {})
			.orElseThrow();
		ada.setPasswordHash(legacy);
		TestDatabase.context().getBean(IUserRepository.class).save(ada);

		// a wrong password changes nothing
		assertFalse(accounts.authenticate("ada@example.com", "difference-engine").isPresent());
		assertEquals(legacy, accounts.find("ada@example.com").orElseThrow().getPasswordHash());

		assertTrue(accounts.authenticate("ada@example.com", "analytical-engine").isPresent());
		String renewed = accounts.find("ada@example.com").orElseThrow().getPasswordHash();
		assertTrue(renewed.startsWith("pbkdf2-sha256$600000$"), renewed);
		assertTrue(accounts.authenticate("ada@example.com", "analytical-engine").isPresent());
		assertEquals(renewed, accounts.find("ada@example.com").orElseThrow().getPasswordHash());
	}

	@Test
	void theAccountsTheDemoAlwaysHasGetHashesOfToday()
	{
		User wicket = accounts.find("wicket").orElseThrow();
		wicket.setPasswordHash(legacyHash(TestDatabase.WICKET_PASSWORD));
		TestDatabase.context().getBean(IUserRepository.class).save(wicket);

		accounts.addDefaults();

		String hash = accounts.find("wicket").orElseThrow().getPasswordHash();
		assertTrue(hash.startsWith("pbkdf2-sha256$600000$"), hash);
		assertTrue(accounts.authenticate("wicket", TestDatabase.WICKET_PASSWORD).isPresent());
	}

	/**
	 * @return a hash as earlier versions made it: PBKDF2-HMAC-SHA256 with 210,000 iterations, as
	 *         {@code <salt>:<hash>} in Base64
	 */
	private static String legacyHash(String password)
	{
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
		try
		{
			byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
				.generateSecret(spec)
				.getEncoded();
			Base64.Encoder base64 = Base64.getEncoder();
			return base64.encodeToString(salt) + ":" + base64.encodeToString(hash);
		}
		catch (GeneralSecurityException e)
		{
			throw new AssertionError(e);
		}
	}
}
