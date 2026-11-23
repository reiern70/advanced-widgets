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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.data.Access;
import com.antiliacode.widgets.demo.data.Accesses;
import com.antiliacode.widgets.demo.data.FormerAccount;
import com.antiliacode.widgets.demo.data.IAccessRepository;
import com.antiliacode.widgets.demo.data.IFormerAccountRepository;
import com.antiliacode.widgets.demo.data.IOldAccessRepository;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.JobRole;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;

/**
 * The accounts visitors registered are deleted {@link Accounts#KEPT} later, with their accesses,
 * and what is kept of them tells nothing of who they were; see {@link ExpiredAccounts}.
 */
class ExpiredAccountsTest
{
	/** When the accounts of the tests register. */
	private static final Instant REGISTERED = Instant.parse("2026-11-23T10:00:00Z");

	/** Six months later. */
	private static final Instant EXPIRES = Instant.parse("2027-05-23T10:00:00Z");

	private ApplicationContext context;

	private Accounts accounts;

	private IUserRepository users;

	private IAccessRepository accesses;

	private IOldAccessRepository oldAccesses;

	private IFormerAccountRepository formerAccounts;

	private ExpiredAccounts expired;

	@BeforeEach
	void clear()
	{
		TestDatabase.clear();
		context = TestDatabase.context();
		users = context.getBean(IUserRepository.class);
		accesses = context.getBean(IAccessRepository.class);
		oldAccesses = context.getBean(IOldAccessRepository.class);
		formerAccounts = context.getBean(IFormerAccountRepository.class);
		expired = context.getBean(ExpiredAccounts.class);
		accounts = new Accounts(users, Clock.fixed(REGISTERED, ZoneOffset.UTC));
	}

	@Test
	void anAccountRegisteredExpiresSixMonthsLater()
	{
		User ada = register("ada@example.com");

		assertEquals(EXPIRES, ada.getExpires());
		assertTrue(ada.isValidated());
	}

	@Test
	void theAccountsMadeOtherwiseNeverExpire()
	{
		User client = accounts.create("client", "Secret-password-1", Role.USER, user -> {})
			.orElseThrow();

		assertNull(client.getExpires());
		for (String name : TestDatabase.DEFAULT_ACCOUNTS)
		{
			assertNull(users.findByName(name).orElseThrow().getExpires(), name);
		}
	}

	@Test
	void anAccountThatExpiredIsDeletedWithItsAccessesKeepingWhatIsCounted()
	{
		register("ada@example.com");
		// a month later
		accounts = new Accounts(users, Clock.fixed(Instant.parse("2026-12-23T10:00:00Z"),
			ZoneOffset.UTC));
		register("grace@example.com");
		access("ada@example.com", "2026-11-24T10:00:00Z", true);
		access("ada@example.com", "2026-12-01T10:00:00Z", false);
		access("ada@example.com", "2027-05-20T10:00:00Z", true);
		access("grace@example.com", "2026-12-01T10:00:00Z", true);
		access("unknown", "2026-12-01T10:00:00Z", false);
		access("semedy", "2026-12-01T10:00:00Z", true);
		// the older ones are among the old accesses by then
		context.getBean(Accesses.class).archive(Instant.parse("2027-01-01T00:00:00Z"));
		assertEquals(5, oldAccesses.count());
		long changes = Accounts.changes();

		// not before it expires
		assertEquals(0, expired.delete(EXPIRES.minusSeconds(1)));
		assertTrue(users.findByName("ada@example.com").isPresent());

		assertEquals(1, expired.delete(EXPIRES.plusMillis(1)));

		assertFalse(users.findByName("ada@example.com").isPresent());
		assertEquals(0, accesses.countByUserName("ada@example.com"));
		assertEquals(0, oldAccesses.countByUserName("ada@example.com"));
		assertTrue(Accounts.changes() > changes, "who is signed in with it is signed out");
		// the others and their accesses stay
		assertTrue(users.findByName("grace@example.com").isPresent());
		assertEquals(1, oldAccesses.countByUserName("grace@example.com"));
		assertEquals(1, oldAccesses.countByUserName("unknown"));
		assertEquals(1, oldAccesses.countByUserName("semedy"));
		for (String name : TestDatabase.DEFAULT_ACCOUNTS)
		{
			assertTrue(users.findByName(name).isPresent(), name);
		}

		List<FormerAccount> former = formerAccounts.findAll();
		assertEquals(1, former.size());
		FormerAccount kept = former.get(0);
		assertEquals(LocalDate.parse("2026-11-01"), kept.getRegistered());
		assertEquals(LocalDate.parse("2027-05-23"), kept.getDeleted());
		assertEquals("ES", kept.getCountry());
		assertEquals(Language.GERMAN, kept.getLanguage());
		assertEquals(JobRole.OTHER, kept.getJobRole());
		assertEquals(3, kept.getAccesses());
		assertEquals(2, kept.getDemoAccesses());
	}

	@Test
	void administratorsAndConfiguredAccountsAreNeverDeleted()
	{
		accounts.addConfigured("client:Secret-password-1");
		try
		{
			for (String name : List.of("admin", "client"))
			{
				User user = users.findByName(name).orElseThrow();
				user.setExpires(REGISTERED);
				users.save(user);
			}

			assertEquals(0, expired.delete(EXPIRES));

			assertTrue(users.findByName("admin").isPresent());
			assertTrue(users.findByName("client").isPresent());
			assertTrue(formerAccounts.findAll().isEmpty());
		}
		finally
		{
			// the accounts the demo always has stay from one test to the next
			User admin = users.findByName("admin").orElseThrow();
			admin.setExpires(null);
			users.save(admin);
		}
	}

	private User register(String email)
	{
		return accounts.register(email, "Analytical-Engine-1843", user -> {
			user.setFirstName("Ada");
			user.setLastName("Lovelace");
			user.setCountry("ES");
			user.setLanguage(Language.GERMAN);
			user.setJobRole(JobRole.OTHER);
			user.setJobRoleOther("Mathematician");
		}).orElseThrow();
	}

	private void access(String userName, String accessed, boolean demos)
	{
		Access access = new Access(null, "203.0.113.7", "Mozilla/5.0", Instant.parse(accessed));
		access.setUserName(userName);
		access.setPrivatePart(demos);
		accesses.save(access);
	}
}
