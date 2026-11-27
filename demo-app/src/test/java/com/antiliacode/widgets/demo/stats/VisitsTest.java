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
package com.antiliacode.widgets.demo.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.account.Role;
import com.antiliacode.widgets.demo.stats.Visits.DayCount;
import com.antiliacode.widgets.demo.stats.Visits.PageCount;
import com.antiliacode.widgets.demo.stats.Visits.Report;
import com.antiliacode.widgets.demo.stats.Visits.VisitorCount;

/**
 * The {@link Visits}: people told apart by the number of their cookie, the pages they open, the
 * sign-ins, the administrator left out, a file a day read again at the next start and deleted
 * after 13 months.
 */
class VisitsTest
{
	private static final String ANN = VisitorCookie.newNumber();

	private static final String BOB = VisitorCookie.newNumber();

	private final TestClock clock = new TestClock();

	@TempDir
	Path folder;

	@Test
	void peopleAreToldApartByTheirCookie()
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", null);
		visits.viewed("/", ANN);
		visits.viewed("/tree", ANN);
		visits.viewed("/", BOB);
		visits.signedIn(ANN, Role.USER);

		Report report = visits.report(1);

		assertEquals(2, report.people());
		assertEquals(2, report.newPeople());
		assertEquals(1, report.signedInPeople());
		assertEquals(1, report.signIns());
		assertEquals(4, report.views());
		assertEquals(1, report.viewsWithoutCookie());
		assertEquals(List.of(new PageCount("/", 3, 2), new PageCount("/tree", 1, 1)),
			report.pages());
		assertEquals(LocalDate.parse("2026-10-15"), report.firstDay());
	}

	@Test
	void theBrowsersOfTheAdministratorAreLeftOut()
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", ANN);
		visits.viewed("/sign-in", ANN);
		visits.signedIn(ANN, Role.ADMIN);
		visits.viewed("/", BOB);

		Report report = visits.report(1);

		assertEquals(1, report.people());
		assertEquals(1, report.views());
		assertEquals(List.of(new PageCount("/", 1, 1)), report.pages());
	}

	@Test
	void theVisitsAreReadAgainAtTheNextStart() throws IOException
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", ANN);
		visits.signedIn(ANN, Role.USER);
		visits.signedIn(BOB, Role.ADMIN);
		visits.viewed("/tree", BOB);

		Report again = new Visits(folder, clock).report(1);

		assertEquals(visits.report(1), again);
		assertEquals(1, again.people());
		List<String> lines = Files.readAllLines(folder.resolve("visits-2026-10-15.tsv"));
		assertEquals(4, lines.size());
		assertEquals("2026-10-15T10:00:00Z\tview\t" + ANN + "\t/", lines.get(0));
		assertEquals("2026-10-15T10:00:00Z\tsign-in\t" + BOB + "\tadmin", lines.get(2));
	}

	@Test
	void whatCouldBreakALineIsNotNoted() throws IOException
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/a\tb", ANN);
		visits.viewed("/a\nb", ANN);
		visits.viewed("/", "not\ta number");

		Report report = visits.report(1);
		assertEquals(1, report.views());
		assertEquals(1, report.viewsWithoutCookie());
		assertEquals(1, Files.readAllLines(folder.resolve("visits-2026-10-15.tsv")).size());
	}

	@Test
	void linesThatAreNoneAreSkipped() throws IOException
	{
		Files.writeString(folder.resolve("visits-2026-10-15.tsv"), String.join("\n",
			"2026-10-15T09:00:00Z\tview\t" + ANN + "\t/",
			"garbage",
			"2026-10-15T09:00:00Z\tview\tnot-a-number\t/",
			"2026-10-15T09:00:00Z\tsign-in\t" + ANN + "\troot",
			"2026-10-15T09:00:00Z\tsomething\t" + ANN + "\t/",
			"2026-10-15T09:00:00Z\tview\t-\t/tree", ""));
		Files.writeString(folder.resolve("notes.tsv"), "anything");

		Report report = new Visits(folder, clock).report(1);

		assertEquals(1, report.people());
		assertEquals(2, report.views());
		assertEquals(0, report.signIns());
	}

	@Test
	void eachVisitorIsListedTheOneSeenLastFirst()
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", ANN);
		clock.advance(Duration.ofDays(1));
		visits.viewed("/tree", ANN);
		visits.viewed("/tree", ANN);
		visits.signedIn(ANN, Role.USER);
		clock.advance(Duration.ofMinutes(5));
		visits.viewed("/", BOB);
		visits.viewed("/", null);

		List<VisitorCount> visitors = visits.report(2).visitors();

		assertEquals(2, visitors.size());
		VisitorCount bob = visitors.get(0);
		assertEquals(BOB.substring(0, Visits.SHORT), bob.visitor());
		assertEquals(Instant.parse("2026-10-16T10:05:00Z"), bob.firstSeen());
		assertEquals(Instant.parse("2026-10-16T10:05:00Z"), bob.lastSeen());
		VisitorCount ann = visitors.get(1);
		assertEquals(ANN.substring(0, Visits.SHORT), ann.visitor());
		assertEquals(Instant.parse("2026-10-15T10:00:00Z"), ann.firstSeen());
		assertEquals(Instant.parse("2026-10-16T10:00:00Z"), ann.lastSeen());
		assertEquals(2, ann.days());
		assertEquals(3, ann.views());
		assertEquals(1, ann.signIns());
		assertEquals(List.of("/tree", "/"), List.copyOf(ann.pages().keySet()));
		assertEquals(2, ann.pages().get("/tree"));

		// the span counts, but the first time is of all the site remembers, also read again
		VisitorCount today = new Visits(folder, clock).report(1).visitors().get(1);
		assertEquals(1, today.days());
		assertEquals(2, today.views());
		assertEquals(Instant.parse("2026-10-15T10:00:00Z"), today.firstSeen());
	}

	@Test
	void aPersonIsNewOnTheFirstDayOnly()
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", ANN);
		clock.advance(Duration.ofDays(1));
		visits.viewed("/", ANN);
		visits.viewed("/", BOB);

		Report today = visits.report(1);
		assertEquals(2, today.people());
		assertEquals(1, today.newPeople());

		Report both = visits.report(2);
		assertEquals(2, both.people());
		assertEquals(2, both.newPeople());
		assertEquals(List.of(new DayCount(LocalDate.parse("2026-10-16"), 2, 1, 0, 0, 2),
			new DayCount(LocalDate.parse("2026-10-15"), 1, 1, 0, 0, 1)), both.days());
	}

	@Test
	void theDaysOlderThanThirteenMonthsAreDeleted()
	{
		Visits visits = new Visits(folder, clock);
		visits.viewed("/", ANN);
		Path first = folder.resolve("visits-2026-10-15.tsv");
		assertTrue(Files.exists(first));

		clock.advance(Visits.KEPT.minusDays(1));
		visits.viewed("/", BOB);
		assertTrue(Files.exists(first));
		assertEquals(2, visits.report(400).people());

		clock.advance(Duration.ofDays(1));
		visits.viewed("/", BOB);
		assertFalse(Files.exists(first));
		assertEquals(1, visits.report(400).people());
		assertEquals(1, new Visits(folder, clock).report(400).people());
	}

	@Test
	void withoutAFolderTheVisitsAreCountedInMemory() throws IOException
	{
		Path file = Files.writeString(folder.resolve("file"), "not a folder");
		Visits visits = new Visits(file.resolve("stats"), clock);
		assertNull(visits.getFolder());
		visits.viewed("/", ANN);

		assertEquals(1, visits.report(1).people());
	}

	@Test
	void aSpanWithoutVisitsHasItsDaysAtZero()
	{
		Report report = new Visits(null, clock).report(3);

		assertEquals(0, report.people());
		assertNull(report.firstDay());
		assertEquals(3, report.days().size());
		assertEquals(LocalDate.parse("2026-10-15"), report.days().get(0).date());
		assertEquals(LocalDate.parse("2026-10-13"), report.from());
	}
}
