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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * The rows the pages show, which {@link ProgressSimulator} advances, are kept for the last pages
 * of a session, and forgotten once the session is quiet, unless a simulator of it runs.
 */
class ProgressSimulatorTest
{
	@Test
	void keepsTheRowsOfTheLastPagesOfASession()
	{
		for (int page = 1; page <= ProgressSimulator.PAGES_PER_SESSION + 5; page++)
		{
			ProgressSimulator.show("many-pages", page, List.of((long)page));
		}

		assertEquals(List.of(), ProgressSimulator.shown("many-pages", 1), "the first pages went");
		assertEquals(List.of(15L), ProgressSimulator.shown("many-pages", 15));
		assertEquals(List.of(6L), ProgressSimulator.shown("many-pages", 6));

		ProgressSimulator.stop("many-pages", 15);
		assertEquals(List.of(), ProgressSimulator.shown("many-pages", 15));
	}

	@Test
	void forgetsTheRowsOfAQuietSessionWhoseWebSocketNeverConnected()
	{
		ProgressSimulator.show("no-socket", 1, List.of(1L, 2L));
		long later = System.currentTimeMillis() + ProgressSimulator.STALE.toMillis();

		ProgressSimulator.forget(later - 1000);
		assertEquals(List.of(1L, 2L), ProgressSimulator.shown("no-socket", 1));
		ProgressSimulator.forget(later + 60_000);
		assertEquals(List.of(), ProgressSimulator.shown("no-socket", 1));
	}

	@Test
	void keepsThoseOfASessionWithASimulatorRunning()
	{
		ProgressSimulator.show("socket", 1, List.of(1L));
		ProgressSimulator.start(null, "socket", 1, null, "table", new UsersDatabase(3),
			Locale.ENGLISH);
		try
		{
			ProgressSimulator.forget(System.currentTimeMillis() +
				ProgressSimulator.STALE.toMillis() + 60_000);
			assertEquals(List.of(1L), ProgressSimulator.shown("socket", 1));
		}
		finally
		{
			ProgressSimulator.stop("socket", 1);
		}
		assertEquals(List.of(), ProgressSimulator.shown("socket", 1));
	}
}
