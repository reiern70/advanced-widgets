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
package com.antiliacode.widgets.demo.data;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.util.string.Strings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The database of the demo: PostgreSQL in production, set with the environment variables
 * {@value #URL_ENVIRONMENT_VARIABLE}, {@value #USER_ENVIRONMENT_VARIABLE} and
 * {@value #PASSWORD_ENVIRONMENT_VARIABLE} or the system properties of the same meaning; without
 * them an H2 database in a file, in PostgreSQL mode, so that the demo runs without installing
 * anything.
 * <p>
 * The Spring context of the {@link DatabaseConfig} is made once for each database and kept until
 * the JVM ends: the tests start many applications on the same database, and Hibernate takes a
 * while to start.
 */
public final class Database
{
	/** The system property holding the JDBC URL of the database. */
	public static final String URL_PROPERTY = "antilia-widgets.database.url";

	/**
	 * The environment variable holding the URL of the database: a JDBC URL, or one like
	 * {@code postgres://user:password@host:5432/widgets}.
	 */
	public static final String URL_ENVIRONMENT_VARIABLE = "DATABASE_URL";

	/** The system property holding the user of the database. */
	public static final String USER_PROPERTY = "antilia-widgets.database.user";

	/** The environment variable holding the user of the database. */
	public static final String USER_ENVIRONMENT_VARIABLE = "DATABASE_USER";

	/** The system property holding the password of the database. */
	public static final String PASSWORD_PROPERTY = "antilia-widgets.database.password";

	/** The environment variable holding the password of the database. */
	public static final String PASSWORD_ENVIRONMENT_VARIABLE = "DATABASE_PASSWORD";

	/** The system property holding the folder of the H2 database, used when none is set. */
	public static final String FOLDER_PROPERTY = "antilia-widgets.data-folder";

	/** The environment variable holding the folder of the H2 database. */
	public static final String FOLDER_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_DATA_FOLDER";

	private static final Map<Settings, AnnotationConfigApplicationContext> CONTEXTS = new HashMap<>();

	private Database()
	{
	}

	/**
	 * Where the database is and who connects to it.
	 *
	 * @param url
	 *            the JDBC URL
	 * @param user
	 *            the user, may be empty
	 * @param password
	 *            the password, may be empty
	 */
	public record Settings(String url, String user, String password)
	{
		@Override
		public String toString()
		{
			// never the password, this ends up in logs
			return url + " as " + user;
		}
	}

	/**
	 * @return the database set by the system properties or the environment, else the H2 database
	 *         in the folder of {@value #FOLDER_PROPERTY} or {@value #FOLDER_ENVIRONMENT_VARIABLE},
	 *         the temporary folder by default
	 */
	public static Settings fromSettings()
	{
		String url = setting(URL_PROPERTY, URL_ENVIRONMENT_VARIABLE, "");
		String user = setting(USER_PROPERTY, USER_ENVIRONMENT_VARIABLE, "");
		String password = setting(PASSWORD_PROPERTY, PASSWORD_ENVIRONMENT_VARIABLE, "");
		if (Strings.isEmpty(url))
		{
			File folder = new File(setting(FOLDER_PROPERTY, FOLDER_ENVIRONMENT_VARIABLE,
				System.getProperty("java.io.tmpdir")), "antilia-widgets-db");
			return new Settings(h2(folder), user, password);
		}
		return settings(url, user, password);
	}

	/**
	 * @return the JDBC URL of an H2 database in a file of the folder, in PostgreSQL mode; the
	 *         servers of several checkouts may share it
	 */
	static String h2(File folder)
	{
		return "jdbc:h2:file:" + new File(folder, "widgets").getAbsolutePath() +
			";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE";
	}

	/**
	 * @param url
	 *            a JDBC URL, or one like {@code postgres://user:password@host:5432/widgets} as
	 *            hosting services give them
	 * @param user
	 *            the user, which the URL may hold instead
	 * @param password
	 *            the password, which the URL may hold instead
	 * @return the settings with a JDBC URL
	 */
	static Settings settings(String url, String user, String password)
	{
		if (!url.startsWith("postgres://") && !url.startsWith("postgresql://"))
		{
			return new Settings(url, user, password);
		}
		URI uri = URI.create(url);
		String userInfo = uri.getRawUserInfo();
		if (userInfo != null)
		{
			int colon = userInfo.indexOf(':');
			if (Strings.isEmpty(user))
			{
				user = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
			}
			if (Strings.isEmpty(password) && colon >= 0)
			{
				password = decode(userInfo.substring(colon + 1));
			}
		}
		String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
		String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
		return new Settings("jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query,
			user, password);
	}

	/**
	 * @param settings
	 *            the database
	 * @return the Spring context with the repositories of the database, made on the first call;
	 *         the tables are made or brought up to date by then
	 */
	public static synchronized ApplicationContext context(Settings settings)
	{
		return CONTEXTS.computeIfAbsent(settings, key -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.registerBean(Settings.class, () -> key);
			context.register(DatabaseConfig.class);
			context.refresh();
			context.registerShutdownHook();
			return context;
		});
	}

	private static String decode(String value)
	{
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
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
