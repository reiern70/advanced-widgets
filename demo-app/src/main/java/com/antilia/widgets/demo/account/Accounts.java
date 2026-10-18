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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.Strings;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.data.UserFilter;
import com.antilia.widgets.demo.data.IUserRepository;

/**
 * The accounts that may open the demos, in the table {@code users}: the ones the demo always has
 * (see {@link #addDefaults()}), the ones the server is started with, for the people the demos are
 * shown to (see {@link #ACCOUNTS_ENVIRONMENT_VARIABLE}), and the ones visitors register, once
 * registering is open. Only salted hashes of the passwords are kept.
 * <p>
 * A visitor who registers opens the demos only after following the link of the confirmation
 * email, which works for {@link #VALIDATION_TIME}; only the SHA-256 of its token is kept.
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

	/** The system property holding the password of the account {@code admin}, the administrator. */
	public static final String ADMIN_PASSWORD_PROPERTY = "antilia-widgets.admin.password";

	/** The environment variable holding the password of the account {@code admin}. */
	public static final String ADMIN_PASSWORD_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ADMIN_PASSWORD";

	/** The system property holding the password of the account {@code semedy}. */
	public static final String SEMEDY_PASSWORD_PROPERTY = "antilia-widgets.semedy.password";

	/** The environment variable holding the password of the account {@code semedy}. */
	public static final String SEMEDY_PASSWORD_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_SEMEDY_PASSWORD";

	/** The system property holding the password of the account {@code wicket}. */
	public static final String WICKET_PASSWORD_PROPERTY = "antilia-widgets.wicket.password";

	/** The environment variable holding the password of the account {@code wicket}. */
	public static final String WICKET_PASSWORD_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_WICKET_PASSWORD";

	/**
	 * The accounts the demo always has: name, role, and the system property and the environment
	 * variable holding the password, which is never in the source. See {@link #addDefaults()}.
	 */
	private static final String[][] DEFAULTS = {
		{ "semedy", "USER", SEMEDY_PASSWORD_PROPERTY, SEMEDY_PASSWORD_ENVIRONMENT_VARIABLE },
		{ "wicket", "USER", WICKET_PASSWORD_PROPERTY, WICKET_PASSWORD_ENVIRONMENT_VARIABLE },
		{ "admin", "ADMIN", ADMIN_PASSWORD_PROPERTY, ADMIN_PASSWORD_ENVIRONMENT_VARIABLE } };

	/**
	 * The hashes the source once gave the accounts the demo always has, and V4__wicket_password.sql
	 * still gives the account wicket: whoever read the source knows their passwords, so an account
	 * holding one is locked at start, unless the server gives it a password.
	 */
	private static final Set<String> PUBLISHED_HASHES = Set.of(
		"zzSQ5U6FuRjZX1kcQ7BROA==:enhhgrdSQjvzJbTxhO3DHG7jeLDKE1Y/aQwcU5jqQeo=",
		"6Fi23jr/izCqXM4Bn7yKxQ==:sLba9BDFyxARONbZ1H5Vi2KadTleKeMw/qqtJwg728M=",
		"zabicgAmDuftSxuCXCT8aQ==:7k9JpQBWu3u+lPkJ59dmmO7NMkhLm81FpIshwiwVrwM=",
		"92vMz986hJZpF/zSqJnPkg==:H0C0YwBH63MAfeF6K87wDjiuUmLe7qlehEdEdmWJMxA=");

	/** The hash of a locked account: having no salt, it matches no password. */
	static final String LOCKED = "locked";

	/**
	 * The hashes of the accounts the demo always has that were found to match their passwords, so
	 * that each start of the application in the same JVM, as in the tests, need not hash them again.
	 */
	private static final Map<String, String> CHECKED = new ConcurrentHashMap<>();

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

	private static final int ITERATIONS = 210_000;

	private static final int KEY_BITS = 256;

	/** How long the link of a confirmation email works. */
	public static final Duration VALIDATION_TIME = Duration.ofHours(48);

	/** How long after sending a confirmation email signing in sends another one. */
	public static final Duration RESEND_AFTER = Duration.ofMinutes(10);

	private static final SecureRandom RANDOM = new SecureRandom();

	private final IUserRepository users;

	private final Clock clock;

	/**
	 * @param users
	 *            the table of the accounts
	 */
	public Accounts(IUserRepository users)
	{
		this(users, Clock.systemUTC());
	}

	/**
	 * @param users
	 *            the table of the accounts
	 * @param clock
	 *            the clock of the links of the confirmation emails
	 */
	Accounts(IUserRepository users, Clock clock)
	{
		this.users = users;
		this.clock = clock;
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
	 * @param id
	 *            the id of an account
	 * @return the account, {@code null} if there is none by that id
	 */
	public User find(Long id)
	{
		return id == null ? null : users.findById(id).orElse(null);
	}

	/**
	 * @param filter
	 *            which accounts
	 * @param pageable
	 *            the page and the order
	 * @return a page of the accounts the filter lets through
	 */
	public Page<User> page(UserFilter filter, Pageable pageable)
	{
		return users.findAll(filter.toSpecification(), pageable);
	}

	/**
	 * @param filter
	 *            which accounts
	 * @return how many accounts the filter lets through
	 */
	public long count(UserFilter filter)
	{
		return users.count(filter.toSpecification());
	}

	/**
	 * Registers a visitor, whose email is not validated yet.
	 *
	 * @param email
	 *            the email, the name of the account
	 * @param password
	 *            the password
	 * @param details
	 *            puts the other details of the visitor in the account
	 * @return the token of the link of the confirmation email; empty if there is an account by
	 *         that name already
	 */
	public Optional<String> register(String email, String password, Consumer<User> details)
	{
		String key = accountName(email);
		if (users.existsByName(key))
		{
			return Optional.empty();
		}
		User user = new User(key, hash(password), Role.USER);
		user.setEmail(key);
		details.accept(user);
		String token = newValidation(user);
		try
		{
			users.save(user);
			return Optional.of(token);
		}
		catch (DataIntegrityViolationException e)
		{
			// another request added it in between
			return Optional.empty();
		}
	}

	/**
	 * Adds an account an administrator makes, with no email to confirm.
	 *
	 * @param name
	 *            the name of the account
	 * @param password
	 *            the password
	 * @param role
	 *            what the account may do
	 * @param details
	 *            puts the email and the other details in the account
	 * @return the account; empty if there is one by that name already
	 */
	public Optional<User> create(String name, String password, Role role, Consumer<User> details)
	{
		String key = accountName(name);
		if (users.existsByName(key))
		{
			return Optional.empty();
		}
		User user = new User(key, hash(password), role);
		user.setValidated(true);
		details.accept(user);
		try
		{
			return Optional.of(users.save(user));
		}
		catch (DataIntegrityViolationException e)
		{
			// another request added it in between
			return Optional.empty();
		}
	}

	/**
	 * Removes an account; who signed in with it stays until the session ends, and its accesses
	 * are kept.
	 *
	 * @param user
	 *            the account
	 */
	public void remove(User user)
	{
		users.delete(user);
	}

	/**
	 * Gives the account a new link for its confirmation email; the one before stops working.
	 *
	 * @param user
	 *            an account whose email is not validated
	 * @return the token of the new link
	 */
	public String renewValidation(User user)
	{
		String token = newValidation(user);
		users.save(user);
		return token;
	}

	/**
	 * @param user
	 *            an account whose email is not validated
	 * @return whether signing in should send another confirmation email: the link of the last
	 *         one stopped working, or it was sent {@link #RESEND_AFTER} ago
	 */
	public boolean isValidationResent(User user)
	{
		Instant expires = user.getValidationExpires();
		return expires == null ||
			clock.instant().isAfter(expires.minus(VALIDATION_TIME).plus(RESEND_AFTER));
	}

	/**
	 * Validates the email of the account whose confirmation email has the token in its link,
	 * when the link still works. The link works once.
	 *
	 * @param token
	 *            the token of the link, as the visitor sent it
	 * @return the account validated, empty if the link does not work
	 */
	public Optional<User> validate(String token)
	{
		if (Strings.isEmpty(token) || token.length() > 100)
		{
			return Optional.empty();
		}
		Optional<User> user = users.findByValidationToken(sha256(token))
			.filter(found -> found.getValidationExpires() != null &&
				clock.instant().isBefore(found.getValidationExpires()));
		user.ifPresent(found -> {
			found.setValidated(true);
			found.setValidation(null, null);
			users.save(found);
		});
		return user;
	}

	/**
	 * Gives the account a new token for the link of its confirmation email, kept as its SHA-256.
	 *
	 * @return the token
	 */
	private String newValidation(User user)
	{
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		user.setValidation(sha256(token), clock.instant().plus(VALIDATION_TIME));
		return token;
	}

	private static String sha256(String token)
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	/**
	 * Gives the accounts the demo always has the passwords the server is started with, each in its
	 * system property or environment variable, such as {@link #ADMIN_PASSWORD_ENVIRONMENT_VARIABLE}:
	 * an account is added when missing, and its password replaced when it is another. An account
	 * without a password set is not added, and is locked if it holds a password the source once
	 * gave it.
	 */
	public void addDefaults()
	{
		for (String[] account : DEFAULTS)
		{
			addDefault(account[0], Role.valueOf(account[1]), setting(account[2], account[3], ""));
		}
	}

	/**
	 * Gives an account the demo always has its password, see {@link #addDefaults()}.
	 *
	 * @param name
	 *            the name of the account
	 * @param role
	 *            what it may do, when it is added
	 * @param password
	 *            its password; empty when none is set
	 */
	void addDefault(String name, Role role, String password)
	{
		String key = accountName(name);
		Optional<User> existing = users.findByName(key);
		if (Strings.isEmpty(password))
		{
			existing.filter(user -> PUBLISHED_HASHES.contains(user.getPasswordHash()))
				.ifPresent(user -> setPasswordHash(user, LOCKED));
		}
		else if (existing.isEmpty())
		{
			String hash = hash(password);
			add(key, hash, role);
			CHECKED.put(hash, password);
		}
		else
		{
			String hash = existing.get().getPasswordHash();
			if (!password.equals(CHECKED.get(hash)) && !matches(password, hash))
			{
				hash = hash(password);
				setPasswordHash(existing.get(), hash);
			}
			CHECKED.put(hash, password);
		}
	}

	private void setPasswordHash(User user, String passwordHash)
	{
		user.setPasswordHash(passwordHash);
		users.save(user);
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
		User user = new User(key, hash, role);
		// configured accounts have no email to confirm
		user.setValidated(true);
		try
		{
			users.save(user);
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
