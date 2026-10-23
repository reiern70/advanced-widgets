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
package com.antiliacode.widgets.demo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * Upgrading the database keeps its data: a database made by the first migration, with users and
 * accesses in it, keeps every one of them through all the migrations after it; and no migration
 * drops a table or a column, or deletes or truncates rows.
 */
class MigrationsTest
{
	/** What a migration may never do: drop a table or a column, delete rows or truncate. */
	private static final Pattern DESTRUCTIVE = Pattern.compile(
		"\\b(DROP\\s+(TABLE|COLUMN|SCHEMA|DATABASE)|ALTER\\s+TABLE\\s+\\S+\\s+DROP\\b(?!\\s+(DEFAULT|NOT\\s+NULL|CONSTRAINT))|DELETE\\s+FROM|TRUNCATE)\\b");

	@Test
	void upgradingKeepsTheUsersAndTheAccesses() throws SQLException
	{
		String url = "jdbc:h2:mem:migrations-" + UUID.randomUUID() +
			";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
		Flyway.configure().dataSource(url, "", "").target("1").load().migrate();
		try (Connection connection = DriverManager.getConnection(url);
			Statement statement = connection.createStatement())
		{
			statement.executeUpdate("INSERT INTO users (name, password_hash, role, created) VALUES " +
				"('ada@example.com', 'salt:hash-of-ada', 'USER', TIMESTAMP WITH TIME ZONE '2026-10-01 10:00:00+00')," +
				"('client', 'salt:hash-of-client', 'USER', TIMESTAMP WITH TIME ZONE '2026-10-02 10:00:00+00')");
			statement.executeUpdate("INSERT INTO accesses (session_id, user_name, private_part, " +
				"address, user_agent, accessed, time_zone, local_time) VALUES " +
				"('s1', 'unknown', FALSE, '66.249.66.5', 'Googlebot', TIMESTAMP WITH TIME ZONE '2026-10-01 11:00:00+00', 'UTC', TIMESTAMP '2026-10-01 11:00:00')," +
				"('s2', 'ada@example.com', TRUE, '200.46.55.1', 'Firefox', TIMESTAMP WITH TIME ZONE '2026-10-01 12:00:00+00', 'Europe/Madrid', TIMESTAMP '2026-10-01 14:00:00')");
		}

		Flyway.configure().dataSource(url, "", "").load().migrate();

		try (Connection connection = DriverManager.getConnection(url);
			Statement statement = connection.createStatement())
		{
			assertEquals(List.of(
				"ada@example.com|salt:hash-of-ada|USER|true|ada@example.com",
				"client|salt:hash-of-client|USER|true|null"),
				rows(statement, "SELECT name, password_hash, role, validated, email FROM users ORDER BY name"));
			assertEquals(List.of(
				"s1|unknown|false|66.249.66.5|Googlebot|UTC|2026-10-01 11:00:00",
				"s2|ada@example.com|true|200.46.55.1|Firefox|Europe/Madrid|2026-10-01 14:00:00"),
				rows(statement, "SELECT session_id, user_name, private_part, address, user_agent, " +
					"time_zone, FORMATDATETIME(local_time, 'yyyy-MM-dd HH:mm:ss') FROM accesses ORDER BY session_id"));
		}
	}

	@Test
	void noMigrationDestroysData() throws IOException, URISyntaxException
	{
		Path folder = Path.of(MigrationsTest.class.getResource("/db/migration").toURI());
		List<Path> migrations;
		try (Stream<Path> files = Files.list(folder))
		{
			migrations = files.filter(file -> file.toString().endsWith(".sql")).sorted().toList();
		}
		assertFalse(migrations.isEmpty());
		for (Path migration : migrations)
		{
			String sql = Files.readString(migration)
				// without the comments, which may tell what is not done
				.replaceAll("--[^\n]*", "")
				.toUpperCase(Locale.ROOT);
			assertFalse(DESTRUCTIVE.matcher(sql).find(), migration.getFileName() +
				" destroys data: migrations only add, see deploy/README.md");
		}
	}

	@Test
	void theGuardKnowsWhatDestroysData()
	{
		assertTrue(DESTRUCTIVE.matcher("DROP TABLE ACCESSES;").find());
		assertTrue(DESTRUCTIVE.matcher("ALTER TABLE USERS DROP COLUMN EMAIL;").find());
		assertTrue(DESTRUCTIVE.matcher("ALTER TABLE USERS DROP EMAIL;").find());
		assertTrue(DESTRUCTIVE.matcher("DELETE FROM ACCESSES WHERE 1 = 1;").find());
		assertTrue(DESTRUCTIVE.matcher("TRUNCATE ACCESSES;").find());
		assertFalse(DESTRUCTIVE.matcher("ALTER TABLE USERS ALTER COLUMN EMAIL DROP NOT NULL;").find());
		assertFalse(DESTRUCTIVE.matcher("DROP INDEX USERS_EMAIL;").find());
	}

	private static List<String> rows(Statement statement, String query) throws SQLException
	{
		List<String> rows = new ArrayList<>();
		try (ResultSet result = statement.executeQuery(query))
		{
			int columns = result.getMetaData().getColumnCount();
			while (result.next())
			{
				List<String> values = new ArrayList<>();
				for (int i = 1; i <= columns; i++)
				{
					values.add(String.valueOf(result.getObject(i)));
				}
				rows.add(String.join("|", values));
			}
		}
		return rows;
	}
}
