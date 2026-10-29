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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.data.UserFilter;
import com.antiliacode.widgets.demo.data.IUserRepository;

/**
 * The accounts that may open the demos, in the table {@code users}: the ones the demo always has
 * (see {@link #addDefaults()}), the ones the server is started with, for the people the demos are
 * shown to (see {@link #ACCOUNTS_ENVIRONMENT_VARIABLE}), and the ones visitors register, once
 * registering is open. Only salted hashes of the passwords are kept, PBKDF2 with HMAC-SHA256 (see
 * {@link #hash(String)}); a hash of an earlier version, with fewer iterations, is made anew when
 * the account signs in.
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
		"hELprMaIQq0bDQ2M6SVwew==:vf4iUqtFpmVE2d7tdUOD5uNzEZTkSsUXWduC60JFYcs=",
		"zzSQ5U6FuRjZX1kcQ7BROA==:enhhgrdSQjvzJbTxhO3DHG7jeLDKE1Y/aQwcU5jqQeo=",
		"6Fi23jr/izCqXM4Bn7yKxQ==:sLba9BDFyxARONbZ1H5Vi2KadTleKeMw/qqtJwg728M=",
		"zabicgAmDuftSxuCXCT8aQ==:7k9JpQBWu3u+lPkJ59dmmO7NMkhLm81FpIshwiwVrwM=",
		"92vMz986hJZpF/zSqJnPkg==:H0C0YwBH63MAfeF6K87wDjiuUmLe7qlehEdEdmWJMxA=");

	/** The hash of a locked account: having no salt, it matches no password. */
	static final String LOCKED = "locked";

	/**
	 * The hashes of the accounts the demo always has that were found to match their passwords, so
	 * that each start of the application in the same JVM, as in the tests, need not hash them again.
	 * The password is kept as its {@link #fingerprint(String) fingerprint}, never as it is.
	 */
	private static final Map<String, String> CHECKED = new ConcurrentHashMap<>();

	/** The key of the fingerprints in {@link #CHECKED}, new at every start of the JVM. */
	private static final byte[] FINGERPRINT_KEY = new byte[32];

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

	/**
	 * What a hash starts with: the algorithm, PBKDF2 with HMAC-SHA256. The iterations, the salt and
	 * the hash follow, separated by {@code $}: {@code pbkdf2-sha256$600000$<salt>$<hash>}, the salt
	 * and the hash in Base64.
	 */
	static final String HASH_PREFIX = "pbkdf2-sha256$";

	/** The iterations of the hashes made now, as OWASP suggests for PBKDF2-HMAC-SHA256. */
	static final int ITERATIONS = 600_000;

	/**
	 * The iterations of the hashes of earlier versions, {@code <salt>:<hash>} in Base64, which
	 * still match their passwords and are made anew at the next sign-in.
	 */
	static final int LEGACY_ITERATIONS = 210_000;

	/** The most iterations a hash may name, so that a damaged one cannot hold a request. */
	private static final int MAXIMUM_ITERATIONS = 10_000_000;

	private static final int KEY_BITS = 256;

	/** The salt of the hashing done for an account that is not there, or holds no hash. */
	private static final byte[] DUMMY_SALT = new byte[16];

	/** How long the link of a confirmation email works. */
	public static final Duration VALIDATION_TIME = Duration.ofHours(48);

	/** How long after sending a confirmation email signing in sends another one. */
	public static final Duration RESEND_AFTER = Duration.ofMinutes(10);

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final Logger LOG = LoggerFactory.getLogger(Accounts.class);

	/** How often accounts were removed or locked, see {@link #changes()}. */
	private static final AtomicLong CHANGES = new AtomicLong();

	static
	{
		RANDOM.nextBytes(FINGERPRINT_KEY);
		RANDOM.nextBytes(DUMMY_SALT);
	}

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
	 * Checks the password of an account. A name of no account takes as long, the password being
	 * hashed all the same, so that the time does not tell whether the account exists. A hash of an
	 * earlier version that matches is made anew, in the format of today (see
	 * {@link #hash(String)}).
	 *
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
		Optional<User> found = users.findByName(accountName(name));
		boolean matches = matches(password, found.map(User::getPasswordHash).orElse(null));
		Optional<User> user = found.filter(account -> matches);
		user.filter(account -> isOutdated(account.getPasswordHash()))
			.ifPresent(account -> setPasswordHash(account, hash(password)));
		return user;
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
	 *         that name already, which takes as long, the password being hashed all the same
	 */
	public Optional<String> register(String email, String password, Consumer<User> details)
	{
		String key = accountName(email);
		if (users.existsByName(key))
		{
			hash(password);
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
	 * Removes an account; who signed in with it is signed out at the next request (see
	 * {@link #isStillSignedIn(String, Role)}), and its accesses are kept.
	 *
	 * @param user
	 *            the account
	 */
	public void remove(User user)
	{
		users.delete(user);
		CHANGES.incrementAndGet();
	}

	/**
	 * Locks an account: its hash is replaced by {@link #LOCKED}, which matches no password, and
	 * who signed in with it is signed out at the next request.
	 */
	private void lock(User user)
	{
		setPasswordHash(user, LOCKED);
		CHANGES.incrementAndGet();
	}

	/**
	 * @return how often accounts were removed or locked since the start of the JVM, so that a
	 *         session signed in looks at its account again after such a change
	 */
	public static long changes()
	{
		return CHANGES.get();
	}

	/**
	 * @param name
	 *            the name of the account a session signed in with
	 * @param role
	 *            what the account could do then
	 * @return whether the session may stay signed in: the account still exists, is not locked,
	 *         and has the same role
	 */
	public boolean isStillSignedIn(String name, Role role)
	{
		return !Strings.isEmpty(name) && users.findByName(accountName(name))
			.filter(user -> !LOCKED.equals(user.getPasswordHash()) && user.getRole() == role)
			.isPresent();
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
		Optional<User> user = findValidation(token);
		user.ifPresent(found -> {
			found.setValidated(true);
			found.setValidation(null, null);
			users.save(found);
		});
		return user;
	}

	/**
	 * @param token
	 *            the token of the link of a confirmation email, as the visitor sent it
	 * @return the account whose email the link validates, if the link still works; the link
	 *         keeps working
	 */
	public Optional<User> findValidation(String token)
	{
		if (Strings.isEmpty(token) || token.length() > 100)
		{
			return Optional.empty();
		}
		return users.findByValidationToken(sha256(token))
			.filter(found -> found.getValidationExpires() != null &&
				clock.instant().isBefore(found.getValidationExpires()));
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
				.ifPresent(this::lock);
		}
		else if (existing.isEmpty())
		{
			String hash = hash(password);
			add(key, hash, role);
			CHECKED.put(hash, fingerprint(password));
		}
		else
		{
			String hash = existing.get().getPasswordHash();
			if (!fingerprint(password).equals(CHECKED.get(hash)) &&
				(!matches(password, hash) || isOutdated(hash)))
			{
				hash = hash(password);
				setPasswordHash(existing.get(), hash);
			}
			CHECKED.put(hash, fingerprint(password));
		}
	}

	private void setPasswordHash(User user, String passwordHash)
	{
		user.setPasswordHash(passwordHash);
		users.save(user);
	}

	/**
	 * Keeps the accounts the server is started with in step with the setting (see
	 * {@link #addConfigured(String)}), and adds those of earlier versions that are missing.
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
	 * Keeps the accounts the server is started with in step with the setting: an account it lists
	 * is added when missing, and gets the password the setting gives when it holds another; an
	 * account it listed before and no longer lists is locked, never deleted, and opens again once
	 * listed again. An account it lists that an administrator removed comes back. The accounts the
	 * demo always has are left out, as their passwords have settings of their own (see
	 * {@link #addDefaults()}).
	 *
	 * @param accounts
	 *            {@code name:password} separated by commas; may be empty, which locks every
	 *            account listed before
	 */
	public void addConfigured(String accounts)
	{
		Map<String, String> listed = new LinkedHashMap<>();
		if (!Strings.isEmpty(accounts))
		{
			for (String account : accounts.split(","))
			{
				int colon = account.indexOf(':');
				String key = colon > 0 ? accountName(account.substring(0, colon)) : "";
				String password = colon > 0 ? account.substring(colon + 1).strip() : "";
				if (isDefault(key))
				{
					LOG.warn("{} lists {}, which takes its password from a setting of its own",
						ACCOUNTS_ENVIRONMENT_VARIABLE, key);
				}
				else if (!key.isEmpty() && !password.isEmpty())
				{
					listed.put(key, password);
				}
			}
		}
		listed.forEach(this::configure);
		for (User user : users.findByConfiguredTrue())
		{
			if (!listed.containsKey(user.getName()) && !LOCKED.equals(user.getPasswordHash()))
			{
				LOG.warn("{} no longer lists the account {}: it is locked",
					ACCOUNTS_ENVIRONMENT_VARIABLE, user.getName());
				lock(user);
			}
		}
	}

	/**
	 * Gives an account the server is started with its password, adding it when missing.
	 */
	private void configure(String key, String password)
	{
		User user = users.findByName(key).orElse(null);
		if (user == null)
		{
			user = new User(key, hash(password), Role.USER);
		}
		else if (!matches(password, user.getPasswordHash()) || isOutdated(user.getPasswordHash()))
		{
			user.setPasswordHash(hash(password));
		}
		else if (user.isConfigured() && user.isValidated())
		{
			return;
		}
		user.setConfigured(true);
		// configured accounts have no email to confirm
		user.setValidated(true);
		try
		{
			users.save(user);
		}
		catch (DataIntegrityViolationException e)
		{
			// another start added it in between
			LOG.warn("Cannot add the account {}", key, e);
		}
	}

	/**
	 * @param name
	 *            the name of an account
	 * @return whether it is one of the accounts the demo always has or of those the server is
	 *         started with, which sign in however many registered accounts are signed in, see
	 *         {@link SignedInUsers}
	 */
	public boolean isAlwaysSignedIn(String name)
	{
		if (Strings.isEmpty(name))
		{
			return false;
		}
		String key = accountName(name);
		return isDefault(key) || users.findByName(key).map(User::isConfigured).orElse(false);
	}

	/**
	 * @param key
	 *            the name of an account, as {@link #accountName(String)} makes it
	 * @return whether it is one of the accounts the demo always has, see {@link #addDefaults()}
	 */
	static boolean isDefault(String key)
	{
		for (String[] account : DEFAULTS)
		{
			if (account[0].equals(key))
			{
				return true;
			}
		}
		return false;
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
	 * @return the hash of the password, with a new salt: PBKDF2 with HMAC-SHA256 and
	 *         {@value #ITERATIONS} iterations, as {@code pbkdf2-sha256$600000$<salt>$<hash>}, the
	 *         salt and the hash in Base64
	 */
	public static String hash(String password)
	{
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		Base64.Encoder base64 = Base64.getEncoder();
		return HASH_PREFIX + ITERATIONS + "$" + base64.encodeToString(salt) + "$" +
			base64.encodeToString(derive(password, salt, ITERATIONS));
	}

	/**
	 * Checks a password against a hash, of today or of an earlier version. It takes as long as
	 * hashing with {@value #ITERATIONS} iterations whatever the hash, also for a hash of fewer
	 * iterations, a locked account or no hash at all, so that the time taken does not tell which.
	 *
	 * @param password
	 *            the password
	 * @param hash
	 *            the hash, {@code null} for none
	 * @return whether the password is the one of the hash
	 */
	static boolean matches(String password, String hash)
	{
		Hash parsed = Hash.parse(hash);
		if (parsed == null)
		{
			derive(password, DUMMY_SALT, ITERATIONS);
			return false;
		}
		byte[] derived = derive(password, parsed.salt(), parsed.iterations());
		if (parsed.iterations() < ITERATIONS)
		{
			derive(password, DUMMY_SALT, ITERATIONS - parsed.iterations());
		}
		return MessageDigest.isEqual(parsed.hash(), derived);
	}

	/**
	 * @return whether the hash is of an earlier version, or of fewer iterations than today's, so
	 *         that it is made anew once the password is known
	 */
	static boolean isOutdated(String hash)
	{
		Hash parsed = Hash.parse(hash);
		return parsed != null && (!hash.startsWith(HASH_PREFIX) || parsed.iterations() < ITERATIONS);
	}

	private static byte[] derive(String password, byte[] salt, int iterations)
	{
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
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

	/**
	 * @return a fingerprint of the password, HMAC-SHA256 with a key of this JVM, which tells
	 *         whether a password is the same as before without keeping it
	 */
	private static String fingerprint(String password)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(FINGERPRINT_KEY, "HmacSHA256"));
			return Base64.getEncoder()
				.encodeToString(mac.doFinal(password.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	/**
	 * A hash read from the table: the iterations, the salt and the hash of the password.
	 */
	private record Hash(int iterations, byte[] salt, byte[] hash)
	{
		/**
		 * @return the hash, {@code null} if it is none, as {@link Accounts#LOCKED}
		 */
		static Hash parse(String text)
		{
			if (text == null)
			{
				return null;
			}
			try
			{
				Base64.Decoder base64 = Base64.getDecoder();
				if (text.startsWith(HASH_PREFIX))
				{
					String[] parts = text.substring(HASH_PREFIX.length()).split("\\$");
					int iterations = Integer.parseInt(parts[0]);
					if (parts.length != 3 || iterations < 1 || iterations > MAXIMUM_ITERATIONS)
					{
						return null;
					}
					return new Hash(iterations, base64.decode(parts[1]), base64.decode(parts[2]));
				}
				int colon = text.indexOf(':');
				if (colon < 0)
				{
					return null;
				}
				return new Hash(LEGACY_ITERATIONS, base64.decode(text.substring(0, colon)),
					base64.decode(text.substring(colon + 1)));
			}
			catch (IllegalArgumentException e)
			{
				// not Base64, or not a number
				return null;
			}
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
