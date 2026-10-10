/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.Strings;
import org.springframework.dao.DataIntegrityViolationException;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.data.IUserRepository;

/**
 * The accounts that may open the demos, in the table {@code users}: the ones the demo always has
 * (see {@link #addDefaults()}), the ones the server is started with, for the people the demos are
 * shown to (see {@link #ACCOUNTS_ENVIRONMENT_VARIABLE}), and the ones visitors register, once
 * registering is open. Only salted hashes of the passwords are kept.
 */
public class Accounts
{
	/**
	 * The system property holding the accounts the server is started with, as
	 * {@code name:password} separated by commas.
	 */
	public static final String ACCOUNTS_PROPERTY = "antilia-widgets.accounts";

	/** The environment variable holding the accounts the server is started with, like the property. */
	public static final String ACCOUNTS_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ACCOUNTS";

	/**
	 * The system property holding the folder where earlier versions kept the registered accounts,
	 * in a properties file that {@link #importFile(File)} reads into the table.
	 */
	public static final String FOLDER_PROPERTY = "antilia-widgets.accounts-folder";

	/** The environment variable holding the folder of the accounts of earlier versions. */
	public static final String FOLDER_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ACCOUNTS_FOLDER";

	/**
	 * The accounts the demo always has, with the salted hashes of their passwords: name, hash,
	 * role. They are added when missing at every start, and never changed.
	 */
	private static final String[][] DEFAULTS = {
		{ "semedy", "hELprMaIQq0bDQ2M6SVwew==:vf4iUqtFpmVE2d7tdUOD5uNzEZTkSsUXWduC60JFYcs=", "USER" },
		{ "wicket", "zabicgAmDuftSxuCXCT8aQ==:7k9JpQBWu3u+lPkJ59dmmO7NMkhLm81FpIshwiwVrwM=", "USER" },
		{ "admin", "92vMz986hJZpF/zSqJnPkg==:H0C0YwBH63MAfeF6K87wDjiuUmLe7qlehEdEdmWJMxA=", "ADMIN" } };

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

	private static final int ITERATIONS = 210_000;

	private static final int KEY_BITS = 256;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final IUserRepository users;

	/**
	 * @param users
	 *            the table of the accounts
	 */
	public Accounts(IUserRepository users)
	{
		this.users = users;
	}

	/**
	 * @return the accounts of the running application
	 */
	public static Accounts get()
	{
		return ((WicketApplication)Application.get()).getAccounts();
	}

	/**
	 * @param name
	 *            the name of an account or the email a visitor registered with
	 * @param password
	 *            the password
	 * @return the account, if the password is the account's
	 */
	public Optional<User> authenticate(String name, String password)
	{
		if (Strings.isEmpty(name) || Strings.isEmpty(password))
		{
			return Optional.empty();
		}
		return users.findByName(accountName(name))
			.filter(user -> matches(password, user.getPasswordHash()));
	}

	/**
	 * @param name
	 *            the name of an account
	 * @return the account of that name, without checking any password
	 */
	public Optional<User> find(String name)
	{
		return Strings.isEmpty(name) ? Optional.empty() : users.findByName(accountName(name));
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
	public boolean register(String email, String password)
	{
		return add(email, hash(password), Role.USER);
	}

	/**
	 * Adds the accounts the demo always has, those that are missing.
	 */
	public void addDefaults()
	{
		for (String[] account : DEFAULTS)
		{
			add(account[0], account[1], Role.valueOf(account[2]));
		}
	}

	/**
	 * Adds the accounts the server is started with and those of earlier versions, those that are
	 * missing.
	 */
	public void addFromSettings()
	{
		addConfigured(setting(ACCOUNTS_PROPERTY, ACCOUNTS_ENVIRONMENT_VARIABLE, ""));
		String folder = setting(FOLDER_PROPERTY, FOLDER_ENVIRONMENT_VARIABLE, "");
		if (!Strings.isEmpty(folder))
		{
			importFile(new File(folder, "antilia-widgets-accounts.properties"));
		}
	}

	/**
	 * Adds the accounts the server is started with, those that are missing.
	 *
	 * @param accounts
	 *            {@code name:password} separated by commas; may be empty
	 */
	public void addConfigured(String accounts)
	{
		if (Strings.isEmpty(accounts))
		{
			return;
		}
		for (String account : accounts.split(","))
		{
			int colon = account.indexOf(':');
			if (colon > 0 && colon < account.length() - 1)
			{
				add(account.substring(0, colon), hash(account.substring(colon + 1).strip()),
					Role.USER);
			}
		}
	}

	/**
	 * Adds the accounts of the properties file where earlier versions kept the registered ones,
	 * those that are missing; the hashes are the same.
	 *
	 * @param file
	 *            the properties file, which need not exist
	 */
	public void importFile(File file)
	{
		if (!file.exists())
		{
			return;
		}
		Properties accounts = new Properties();
		try (InputStream in = Files.newInputStream(file.toPath()))
		{
			accounts.load(in);
		}
		catch (IOException e)
		{
			throw new WicketRuntimeException("Cannot read " + file, e);
		}
		accounts.stringPropertyNames()
			.forEach(name -> add(name, accounts.getProperty(name), Role.USER));
	}

	/**
	 * @return whether the account was added; {@code false} if there is one by that name already
	 */
	private boolean add(String name, String hash, Role role)
	{
		String key = accountName(name);
		if (users.existsByName(key))
		{
			return false;
		}
		try
		{
			users.save(new User(key, hash, role));
			return true;
		}
		catch (DataIntegrityViolationException e)
		{
			// another request added it in between
			return false;
		}
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
	public static String hash(String password)
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
