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

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Reads the settings of the {@link Database}.
 */
class DatabaseTest
{
	@Test
	void aJdbcUrlIsTakenAsItIs()
	{
		Database.Settings settings = Database.settings("jdbc:postgresql://db:5432/widgets",
			"widgets", "secret");

		assertEquals("jdbc:postgresql://db:5432/widgets", settings.url());
		assertEquals("widgets", settings.user());
		assertEquals("secret", settings.password());
	}

	@Test
	void aPostgresUrlBecomesAJdbcOne()
	{
		Database.Settings settings = Database.settings(
			"postgres://widgets:p%40ss@db.example.com:5432/widgets?sslmode=require", "", "");

		assertEquals("jdbc:postgresql://db.example.com:5432/widgets?sslmode=require",
			settings.url());
		assertEquals("widgets", settings.user());
		assertEquals("p@ss", settings.password());
	}

	@Test
	void theUserAndPasswordSetApartWin()
	{
		Database.Settings settings = Database.settings("postgresql://someone:else@db/widgets",
			"widgets", "secret");

		assertEquals("jdbc:postgresql://db/widgets", settings.url());
		assertEquals("widgets", settings.user());
		assertEquals("secret", settings.password());
	}

	@Test
	void withoutADatabaseH2KeepsItInAFile()
	{
		String url = Database.h2(new File("/var/lib/widgets"));

		assertTrue(url.startsWith("jdbc:h2:file:/var/lib/widgets/widgets;MODE=PostgreSQL"), url);
	}

	@Test
	void thePasswordStaysOutOfTheLogs()
	{
		Database.Settings settings = new Database.Settings("jdbc:h2:mem:x", "sa", "secret");

		assertFalse(settings.toString().contains("secret"));
	}
}
