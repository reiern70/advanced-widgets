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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.data.UserFilter;

/**
 * Validates the email of a registered account with the token of the link of its confirmation
 * email, and filters the accounts as the administration does.
 */
class ValidationTest
{
	private Instant now = Instant.parse("2026-10-10T12:00:00Z");

	private Accounts accounts;

	@BeforeEach
	void clear()
	{
		TestDatabase.clear();
		accounts = new Accounts(TestDatabase.context().getBean(IUserRepository.class),
			new Clock()
			{
				@Override
				public Instant instant()
				{
					return now;
				}

				@Override
				public java.time.ZoneId getZone()
				{
					return ZoneOffset.UTC;
				}

				@Override
				public Clock withZone(java.time.ZoneId zone)
				{
					return this;
				}
			});
	}

	@Test
	void theLinkValidatesOnce()
	{
		String token = register("ada@example.com");
		assertFalse(ada().isValidated());

		assertFalse(accounts.validate(token + "x").isPresent());
		assertFalse(accounts.validate("").isPresent());
		assertFalse(accounts.validate(null).isPresent());

		assertEquals("ada@example.com", accounts.validate(token).orElseThrow().getName());
		assertTrue(ada().isValidated());
		assertFalse(accounts.validate(token).isPresent());
	}

	@Test
	void theLinkStopsWorking()
	{
		String token = register("ada@example.com");
		now = now.plus(Accounts.VALIDATION_TIME).plusSeconds(1);

		assertFalse(accounts.validate(token).isPresent());
		assertFalse(ada().isValidated());
	}

	@Test
	void aNewLinkReplacesTheOldOne()
	{
		String first = register("ada@example.com");
		assertFalse(accounts.isValidationResent(ada()));

		now = now.plus(Accounts.RESEND_AFTER).plusSeconds(1);
		assertTrue(accounts.isValidationResent(ada()));
		String second = accounts.renewValidation(ada());

		assertNotEquals(first, second);
		assertFalse(accounts.validate(first).isPresent());
		assertTrue(accounts.validate(second).isPresent());
	}

	@Test
	void theConfiguredAccountsAreValidated()
	{
		assertTrue(accounts.find("admin").orElseThrow().isValidated());
		accounts.addConfigured("client:secret-one");
		assertTrue(accounts.find("client").orElseThrow().isValidated());
	}

	@Test
	void theAccountsAreFilteredByCountryNameAndEmail()
	{
		register("ada@example.com", "Ada", "Lovelace", "GB");
		register("grace@example.org", "Grace", "Hopper", "US");
		register("alan@example.com", "Alan", "Turing", "GB");

		assertEquals(6, accounts.count(new UserFilter()));
		assertEquals(2, accounts.count(filter("GB", null, null)));
		assertEquals(1, accounts.count(filter("GB", "LOVE", null)));
		// the first name too
		assertEquals(1, accounts.count(filter(null, "grac", null)));
		assertEquals(1, accounts.count(filter(null, null, ".ORG")));
		assertEquals(0, accounts.count(filter("US", null, ".com")));
		// the wildcards of LIKE are taken as they are
		assertEquals(0, accounts.count(filter(null, "%", null)));
		assertEquals(0, accounts.count(filter(null, null, "_")));
		assertEquals(1, accounts.count(filter(null, " Turing ", null)));
	}

	private static UserFilter filter(String country, String name, String email)
	{
		UserFilter filter = new UserFilter();
		filter.setCountry(country);
		filter.setName(name);
		filter.setEmail(email);
		return filter;
	}

	private String register(String email)
	{
		return register(email, "Ada", "Lovelace", "GB");
	}

	private String register(String email, String firstName, String lastName, String country)
	{
		return accounts.register(email, "Analytical-Engine-1843", user -> {
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setCountry(country);
		}).orElseThrow();
	}

	private User ada()
	{
		return accounts.find("ada@example.com").orElseThrow();
	}
}
