/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.license;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;

/**
 * Reads license keys signed by a key pair of the test, and keys tampered with.
 */
class LicenseKeyTest
{
	private static KeyPair pair;

	private static String publicKey;

	@BeforeAll
	static void newKeyPair() throws Exception
	{
		pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
	}

	@Test
	void readsASignedKey() throws Exception
	{
		LicenseKey key = LicenseKey.parse(
			LicenseKeyTool.sign(pair.getPrivate(), "ACME", "2027-06-30", "*"), publicKey);

		assertEquals("ACME", key.getLicensee());
		assertEquals(LocalDate.of(2027, 6, 30), key.getExpires());
		assertTrue(key.isValidOn(LocalDate.of(2027, 6, 30)));
		assertFalse(key.isValidOn(LocalDate.of(2027, 7, 1)));
		assertTrue(key.activates(JsonBasedDynamicDataTable.class));
	}

	@Test
	void aKeyWithoutExpiryIsAlwaysValid() throws Exception
	{
		LicenseKey key = LicenseKey.parse(
			LicenseKeyTool.sign(pair.getPrivate(), "ACME", null, "*"), publicKey);

		assertNull(key.getExpires());
		assertTrue(key.isValidOn(LocalDate.of(2100, 1, 1)));
	}

	@Test
	void activatesTheComponentsItNamesAndTheirSubclasses() throws Exception
	{
		LicenseKey key = LicenseKey.parse(LicenseKeyTool.sign(pair.getPrivate(), "ACME", null,
			"JsonBasedDynamicDataTable"), publicKey);

		assertTrue(key.activates(JsonBasedDynamicDataTable.class));
		assertTrue(key.activates(UsersTable.class));
		assertFalse(key.activates(BeanBasedDynamicDataTable.class));
	}

	@Test
	void refusesAKeyTamperedWith() throws Exception
	{
		String key = LicenseKeyTool.sign(pair.getPrivate(), "ACME", "2027-06-30", "*");
		String[] parts = key.substring(LicenseKey.PREFIX.length()).split("\\.");
		String forged = LicenseKey.PREFIX +
			Base64.getUrlEncoder().withoutPadding().encodeToString(
				"{\"licensee\":\"ACME\",\"components\":\"*\"}".getBytes()) +
			"." + parts[1];

		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse(forged, publicKey));
	}

	@Test
	void refusesAKeySignedByAnotherKeyPair() throws Exception
	{
		KeyPair other = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		String key = LicenseKeyTool.sign(other.getPrivate(), "ACME", null, "*");

		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse(key, publicKey));
		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse(key));
	}

	@Test
	void refusesTextThatIsNoKey()
	{
		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse(null));
		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse("hello"));
		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse("AW1.abc"));
		assertThrows(InvalidLicenseKeyException.class, () -> LicenseKey.parse("AW1.!!.??"));
	}

	/** A table of an application, extending a private component. */
	private abstract static class UsersTable
		extends
			JsonBasedDynamicDataTable<String, String>
	{
		private static final long serialVersionUID = 1L;

		UsersTable()
		{
			super(null, null, null, 0);
		}
	}
}
