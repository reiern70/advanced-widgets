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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.WicketApplication;

/**
 * The two accounts that open the demos, kept in memory: nothing about them, nor about anybody
 * signing in with them, is stored.
 * <ul>
 * <li>{@value #USER}, shared by everybody who wants to try the demos: its password is in the
 * source, {@link #USER_PASSWORD}, and the sign-in page shows both;</li>
 * <li>{@value #ADMIN}, the administrator, whose password the server is given in the system
 * property {@value #ADMIN_PASSWORD_PROPERTY} or the environment variable
 * {@value #ADMIN_PASSWORD_ENVIRONMENT_VARIABLE}, never in the source; without it there is no
 * administrator.</li>
 * </ul>
 * The passwords are compared by their SHA-256, in a time that does not tell how much of a password
 * was right.
 */
public class Accounts
{
	/** The account everybody shares. */
	public static final String USER = "user";

	/** The password of {@value #USER}, which the sign-in page shows. */
	public static final String USER_PASSWORD = "$Amigo_Mellon_Friend_4_3_2_1";

	/** The administrator's account. */
	public static final String ADMIN = "admin";

	/** The system property holding the administrator's password. */
	public static final String ADMIN_PASSWORD_PROPERTY = "antilia-widgets.admin.password";

	/** The environment variable holding the administrator's password, like the property. */
	public static final String ADMIN_PASSWORD_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_ADMIN_PASSWORD";

	private final byte[] userPassword = sha256(USER_PASSWORD);

	/** The SHA-256 of the administrator's password, {@code null} without an administrator. */
	private final byte[] adminPassword;

	/**
	 * The accounts, with the administrator's password from the system property or the
	 * environment variable.
	 */
	public Accounts()
	{
		this(setting(ADMIN_PASSWORD_PROPERTY, ADMIN_PASSWORD_ENVIRONMENT_VARIABLE));
	}

	/**
	 * @param adminPassword
	 *            the administrator's password, {@code null} or empty for no administrator
	 */
	public Accounts(String adminPassword)
	{
		this.adminPassword = Strings.isEmpty(adminPassword) ? null : sha256(adminPassword);
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
	 *            the name of an account, in any case, with spaces around it or not
	 * @param password
	 *            its password
	 * @return what the account may do, if the password is its own
	 */
	public Optional<Role> authenticate(String name, String password)
	{
		if (Strings.isEmpty(name) || Strings.isEmpty(password))
		{
			return Optional.empty();
		}
		byte[] typed = sha256(password);
		return switch (name.strip().toLowerCase(Locale.ROOT))
		{
			case USER -> MessageDigest.isEqual(typed, userPassword) ? Optional.of(Role.USER)
				: Optional.empty();
			case ADMIN -> adminPassword != null && MessageDigest.isEqual(typed, adminPassword)
				? Optional.of(Role.ADMIN) : Optional.empty();
			default -> Optional.empty();
		};
	}

	/**
	 * @param name
	 *            the name of an account
	 * @return what the account may do, if there is one by that name; there is no administrator
	 *         without a password
	 */
	public Optional<Role> find(String name)
	{
		if (USER.equals(name))
		{
			return Optional.of(Role.USER);
		}
		return ADMIN.equals(name) && adminPassword != null ? Optional.of(Role.ADMIN)
			: Optional.empty();
	}

	/**
	 * @param property
	 *            a system property
	 * @param environmentVariable
	 *            the environment variable read when the property is not set
	 * @return the value of the property, or of the variable
	 */
	static String setting(String property, String environmentVariable)
	{
		String value = System.getProperty(property);
		return Strings.isEmpty(value) ? System.getenv(environmentVariable) : value;
	}

	private static byte[] sha256(String text)
	{
		try
		{
			return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new WicketRuntimeException(e);
		}
	}
}
