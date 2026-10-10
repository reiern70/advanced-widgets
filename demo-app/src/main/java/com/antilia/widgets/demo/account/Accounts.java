/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.demo.WicketApplication;

/**
 * The accounts that may open the demos: the ones visitors register, kept in a properties file so
 * they survive restarts, and the ones the server is started with, for the people the demos are
 * shown to before they register (see {@link #ACCOUNTS_ENVIRONMENT_VARIABLE}). Only salted hashes of
 * the passwords are kept.
 */
public class Accounts
{
	/** The system property holding the folder of the registered accounts. */
	public static final String FOLDER_PROPERTY = "antilia-widgets.accounts-folder";

	/** The environment variable holding the folder of the registered accounts. */
	public static final String FOLDER_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ACCOUNTS_FOLDER";

	/**
	 * The system property holding the accounts the server is started with, as
	 * {@code name:password} separated by commas.
	 */
	public static final String ACCOUNTS_PROPERTY = "antilia-widgets.accounts";

	/** The environment variable holding the accounts the server is started with, like the property. */
	public static final String ACCOUNTS_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ACCOUNTS";

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

	private static final int ITERATIONS = 210_000;

	private static final int KEY_BITS = 256;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final File file;

	/** The hashes of the configured accounts, by name. */
	private final Map<String, String> configured = new LinkedHashMap<>();

	/**
	 * @param file
	 *            the properties file of the registered accounts
	 * @param accounts
	 *            the configured accounts, {@code name:password} separated by commas; may be empty
	 */
	public Accounts(File file, String accounts)
	{
		this.file = file;
		if (!Strings.isEmpty(accounts))
		{
			for (String account : accounts.split(","))
			{
				int colon = account.indexOf(':');
				if (colon > 0 && colon < account.length() - 1)
				{
					configured.put(accountName(account.substring(0, colon)),
						hash(account.substring(colon + 1).strip()));
				}
			}
		}
	}

	/**
	 * @return the accounts of the running application
	 */
	public static Accounts get()
	{
		return ((WicketApplication)Application.get()).getAccounts();
	}

	/**
	 * @return the accounts set by the system properties or the environment: the registered ones in
	 *         the folder of {@value #FOLDER_PROPERTY} or {@value #FOLDER_ENVIRONMENT_VARIABLE}, the
	 *         temporary folder by default, and the configured ones of {@value #ACCOUNTS_PROPERTY}
	 *         or {@value #ACCOUNTS_ENVIRONMENT_VARIABLE}
	 */
	public static Accounts fromSettings()
	{
		return new Accounts(new File(setting(FOLDER_PROPERTY, FOLDER_ENVIRONMENT_VARIABLE,
			System.getProperty("java.io.tmpdir")), "antilia-widgets-accounts.properties"),
			setting(ACCOUNTS_PROPERTY, ACCOUNTS_ENVIRONMENT_VARIABLE, ""));
	}

	/**
	 * @param name
	 *            the name of a configured account or the email a visitor registered with
	 * @param password
	 *            the password
	 * @return the name of the account, as kept, if the password is the account's
	 */
	public synchronized Optional<String> authenticate(String name, String password)
	{
		if (Strings.isEmpty(name) || Strings.isEmpty(password))
		{
			return Optional.empty();
		}
		String key = accountName(name);
		String hash = configured.containsKey(key) ? configured.get(key) : read().getProperty(key);
		return hash != null && matches(password, hash) ? Optional.of(key) : Optional.empty();
	}

	/**
	 * Registers a visitor.
	 *
	 * @param email
	 *            the email, the name of the account
	 * @param password
	 *            the password
	 * @return whether the account was made; {@code false} if there is one by that name already
	 */
	public synchronized boolean register(String email, String password)
	{
		String key = accountName(email);
		Properties accounts = read();
		if (configured.containsKey(key) || accounts.containsKey(key))
		{
			return false;
		}
		accounts.setProperty(key, hash(password));
		file.getParentFile().mkdirs();
		try (OutputStream out = Files.newOutputStream(file.toPath()))
		{
			accounts.store(out, "Accounts registered to the Advanced Widgets demos");
		}
		catch (IOException e)
		{
			throw new WicketRuntimeException("Cannot write " + file, e);
		}
		return true;
	}

	private Properties read()
	{
		Properties properties = new Properties();
		if (file.exists())
		{
			try (InputStream in = Files.newInputStream(file.toPath()))
			{
				properties.load(in);
			}
			catch (IOException e)
			{
				throw new WicketRuntimeException("Cannot read " + file, e);
			}
		}
		return properties;
	}

	/**
	 * @return the name of the account of an email or name as typed: without spaces around and in
	 *         lower case
	 */
	public static String accountName(String name)
	{
		return name.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the salt and the hash of the password, in Base64 separated by a colon
	 */
	static String hash(String password)
	{
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		Base64.Encoder base64 = Base64.getEncoder();
		return base64.encodeToString(salt) + ":" + base64.encodeToString(derive(password, salt));
	}

	static boolean matches(String password, String hash)
	{
		int colon = hash.indexOf(':');
		if (colon < 0)
		{
			return false;
		}
		Base64.Decoder base64 = Base64.getDecoder();
		byte[] salt = base64.decode(hash.substring(0, colon));
		byte[] expected = base64.decode(hash.substring(colon + 1));
		return MessageDigest.isEqual(expected, derive(password, salt));
	}

	private static byte[] derive(String password, byte[] salt)
	{
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
		try
		{
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		}
		catch (GeneralSecurityException e)
		{
			throw new WicketRuntimeException(e);
		}
		finally
		{
			spec.clearPassword();
		}
	}

	private static String setting(String property, String environmentVariable, String fallback)
	{
		String value = System.getProperty(property);
		if (Strings.isEmpty(value))
		{
			value = System.getenv(environmentVariable);
		}
		return Strings.isEmpty(value) ? fallback : value;
	}
}
