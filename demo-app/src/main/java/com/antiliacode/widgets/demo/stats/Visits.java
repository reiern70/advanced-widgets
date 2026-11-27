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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Role;

/**
 * The visits of the demo site, counted for its administrator: how many people open its pages, how
 * many of them sign in, and which pages they open. A person is a browser that sends the
 * {@link VisitorCookie} back, a random number and nothing else; a request without it, the very
 * first one of a browser or one of a client keeping no cookies, such as a robot, counts as a view
 * of its page but as nobody. The browsers that signed in as the administrator are left out of
 * every count, and the requests of the administrator's sessions are not even noted.
 * <p>
 * No database: each day has a text file in a folder, {@code visits-2026-11-27.tsv}, a line per
 * event, appended to as it happens, and read again when the application starts. A line holds the
 * time, what happened (a page shown, a sign-in), the visitor's number or {@code -}, and the mount
 * path of the page or the account signed in with. No address, no user agent, no name. The files
 * of the days older than {@link #KEPT} are deleted, as the cookie expires after as long. Without
 * a folder that can be written to, the visits are only counted in memory, until the application
 * stops.
 */
public class Visits
{
	private static final Logger LOG = LoggerFactory.getLogger(Visits.class);

	/** The system property holding the folder of the files of the visits. */
	public static final String FOLDER_PROPERTY = "antilia-widgets.stats-folder";

	/** The environment variable holding the folder of the files of the visits. */
	public static final String FOLDER_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_STATS_FOLDER";

	/** How long the visits are kept, as long as the {@link VisitorCookie} lasts: 13 months. */
	public static final Duration KEPT = Duration.ofDays(395);

	/** What a line notes: a page shown. */
	static final String VIEW = "view";

	/** What a line notes: a visitor signed in. */
	static final String SIGN_IN = "sign-in";

	/** The visitor of a request without the cookie. */
	private static final String NOBODY = "-";

	/** The name of the file of a day. */
	private static final Pattern FILE = Pattern.compile("visits-(\\d{4}-\\d{2}-\\d{2})\\.tsv");

	/** A page as noted: its mount path, nothing that could break a line. */
	private static final Pattern PAGE = Pattern.compile("/[A-Za-z0-9._/-]{0,100}|[A-Za-z0-9._$]{1,100}");

	/** What happened on a day. */
	private static final class Day
	{
		/** The views of each page, by visitor, {@value Visits#NOBODY} for the requests without one. */
		final Map<String, Map<String, Integer>> views = new TreeMap<>();

		/** The sign-ins as {@link Role#USER}, by visitor. */
		final Map<String, Integer> signIns = new HashMap<>();
	}

	/**
	 * What a page had in a span of days.
	 *
	 * @param page
	 *            its mount path, {@code /} for the home page
	 * @param views
	 *            how many times it was shown
	 * @param people
	 *            how many people it was shown to
	 */
	public record PageCount(String page, int views, int people) implements Serializable
	{
	}

	/**
	 * What a day had.
	 *
	 * @param date
	 *            the day, in UTC
	 * @param people
	 *            how many people opened a page or signed in
	 * @param newPeople
	 *            how many of them came for the first time the site remembers
	 * @param signedInPeople
	 *            how many people signed in
	 * @param signIns
	 *            how many times they did
	 * @param views
	 *            how many pages were shown, also to nobody
	 */
	public record DayCount(LocalDate date, int people, int newPeople, int signedInPeople,
		int signIns, int views) implements Serializable
	{
	}

	/**
	 * The visits of a span of days.
	 *
	 * @param from
	 *            the first day, in UTC
	 * @param to
	 *            the last day, in UTC
	 * @param firstDay
	 *            the first day the site remembers, {@code null} if none
	 * @param people
	 *            how many different people opened a page or signed in
	 * @param newPeople
	 *            how many of them came for the first time the site remembers
	 * @param signedInPeople
	 *            how many different people signed in
	 * @param signIns
	 *            how many times they did
	 * @param views
	 *            how many pages were shown
	 * @param viewsWithoutCookie
	 *            how many of them to nobody: the first page of a browser, or a client keeping no
	 *            cookies
	 * @param pages
	 *            the pages shown, the one shown most first
	 * @param days
	 *            each day, the last one first
	 */
	public record Report(LocalDate from, LocalDate to, LocalDate firstDay, int people,
		int newPeople, int signedInPeople, int signIns, int views, int viewsWithoutCookie,
		List<PageCount> pages, List<DayCount> days) implements Serializable
	{
	}

	/** The folder of the files, {@code null} to count in memory only. */
	private final Path folder;

	private final Clock clock;

	/** The days remembered, the first one first. */
	private final TreeMap<LocalDate, Day> days = new TreeMap<>();

	/** The visitors who signed in as the administrator, left out. */
	private final Set<String> administrators = new HashSet<>();

	/** The day the old files were last deleted. */
	private LocalDate forgotten;

	/** Whether writing failed, told once in the log. */
	private boolean failed;

	/**
	 * Counts the visits in the folder of {@value #FOLDER_PROPERTY} or
	 * {@value #FOLDER_ENVIRONMENT_VARIABLE}, by default {@code antilia-widgets-stats} in the
	 * temporary folder.
	 */
	public Visits()
	{
		this(defaultFolder(), Clock.systemUTC());
	}

	/**
	 * @param folder
	 *            the folder of the files, made if missing; {@code null} to count in memory only
	 * @param clock
	 *            the clock of the visits
	 */
	public Visits(Path folder, Clock clock)
	{
		this.clock = clock;
		this.folder = prepare(folder);
		read();
	}

	/**
	 * @return the visits of the running application
	 */
	public static Visits get()
	{
		return ((WicketApplication)Application.get()).getVisits();
	}

	/**
	 * @return the folder of {@value #FOLDER_PROPERTY} or {@value #FOLDER_ENVIRONMENT_VARIABLE},
	 *         else {@code antilia-widgets-stats} in the temporary folder
	 */
	public static Path defaultFolder()
	{
		String folder = System.getProperty(FOLDER_PROPERTY);
		if (Strings.isEmpty(folder))
		{
			folder = System.getenv(FOLDER_ENVIRONMENT_VARIABLE);
		}
		return Strings.isEmpty(folder)
			? new File(System.getProperty("java.io.tmpdir"), "antilia-widgets-stats").toPath()
			: Path.of(folder);
	}

	/**
	 * Notes a page shown.
	 *
	 * @param page
	 *            its mount path, {@code /} for the home page, or the name of its class
	 * @param visitor
	 *            the number of the {@link VisitorCookie} the request sent, {@code null} if none
	 */
	public void viewed(String page, String visitor)
	{
		if (page == null || !PAGE.matcher(page).matches())
		{
			return;
		}
		note(VIEW, visitor, page);
	}

	/**
	 * Notes a sign-in.
	 *
	 * @param visitor
	 *            the number of the {@link VisitorCookie} the request sent, {@code null} if none
	 * @param role
	 *            what the account signed in with may do
	 */
	public void signedIn(String visitor, Role role)
	{
		note(SIGN_IN, visitor, role == Role.ADMIN ? "admin" : "user");
	}

	/**
	 * @param from
	 *            the first day, in UTC
	 * @param to
	 *            the last day, in UTC
	 * @return the visits of the days
	 */
	public synchronized Report report(LocalDate from, LocalDate to)
	{
		forgetOld();
		// the first day of each visitor the site remembers
		Map<String, LocalDate> firstSeen = new HashMap<>();
		days.forEach((date, day) -> {
			day.views.values().forEach(visitors -> visitors.keySet()
				.forEach(visitor -> firstSeen.putIfAbsent(visitor, date)));
			day.signIns.keySet().forEach(visitor -> firstSeen.putIfAbsent(visitor, date));
		});

		Set<String> people = new HashSet<>();
		Set<String> signedInPeople = new HashSet<>();
		Map<String, int[]> pageViews = new TreeMap<>();
		Map<String, Set<String>> pagePeople = new HashMap<>();
		int signIns = 0;
		int views = 0;
		int viewsWithoutCookie = 0;
		List<DayCount> dayCounts = new ArrayList<>();
		for (LocalDate date = to; !date.isBefore(from); date = date.minusDays(1))
		{
			Day day = days.get(date);
			Set<String> dayPeople = new HashSet<>();
			Set<String> daySignedIn = new HashSet<>();
			int daySignIns = 0;
			int dayViews = 0;
			if (day != null)
			{
				for (Map.Entry<String, Map<String, Integer>> page : day.views.entrySet())
				{
					for (Map.Entry<String, Integer> visitor : page.getValue().entrySet())
					{
						String id = visitor.getKey();
						if (administrators.contains(id))
						{
							continue;
						}
						int count = visitor.getValue();
						dayViews += count;
						pageViews.computeIfAbsent(page.getKey(), key -> new int[1])[0] += count;
						if (NOBODY.equals(id))
						{
							viewsWithoutCookie += count;
						}
						else
						{
							dayPeople.add(id);
							pagePeople.computeIfAbsent(page.getKey(), key -> new HashSet<>())
								.add(id);
						}
					}
				}
				for (Map.Entry<String, Integer> visitor : day.signIns.entrySet())
				{
					String id = visitor.getKey();
					if (administrators.contains(id))
					{
						continue;
					}
					daySignIns += visitor.getValue();
					if (!NOBODY.equals(id))
					{
						dayPeople.add(id);
						daySignedIn.add(id);
					}
				}
			}
			LocalDate current = date;
			int dayNew = (int)dayPeople.stream().filter(id -> current.equals(firstSeen.get(id)))
				.count();
			dayCounts.add(new DayCount(date, dayPeople.size(), dayNew, daySignedIn.size(),
				daySignIns, dayViews));
			people.addAll(dayPeople);
			signedInPeople.addAll(daySignedIn);
			signIns += daySignIns;
			views += dayViews;
		}
		int newPeople = (int)people.stream().map(firstSeen::get)
			.filter(first -> !first.isBefore(from) && !first.isAfter(to)).count();
		List<PageCount> pages = pageViews.entrySet().stream()
			.map(page -> new PageCount(page.getKey(), page.getValue()[0],
				pagePeople.getOrDefault(page.getKey(), Set.of()).size()))
			.sorted(Comparator.comparingInt(PageCount::views).reversed()
				.thenComparing(PageCount::page))
			.toList();
		return new Report(from, to, days.isEmpty() ? null : days.firstKey(), people.size(),
			newPeople, signedInPeople.size(), signIns, views, viewsWithoutCookie, pages,
			List.copyOf(dayCounts));
	}

	/**
	 * @param count
	 *            how many days, today the last one
	 * @return the visits of the last days
	 */
	public Report report(int count)
	{
		LocalDate today = today();
		return report(today.minusDays(Math.max(1, count) - 1L), today);
	}

	/**
	 * @return today, in UTC
	 */
	public LocalDate today()
	{
		return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
	}

	/**
	 * @return the folder of the files, {@code null} if the visits are counted in memory only
	 */
	public Path getFolder()
	{
		return folder;
	}

	private synchronized void note(String what, String visitor, String detail)
	{
		forgetOld();
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		String id = VisitorCookie.isValid(visitor) ? visitor : NOBODY;
		LocalDate date = LocalDate.ofInstant(now, ZoneOffset.UTC);
		count(date, what, id, detail);
		write(date, now + "\t" + what + "\t" + id + "\t" + detail + "\n");
	}

	/**
	 * Counts an event in memory.
	 */
	private void count(LocalDate date, String what, String visitor, String detail)
	{
		if (VIEW.equals(what))
		{
			days.computeIfAbsent(date, key -> new Day()).views
				.computeIfAbsent(detail, key -> new HashMap<>())
				.merge(visitor, 1, Integer::sum);
		}
		else if (SIGN_IN.equals(what))
		{
			if ("admin".equals(detail))
			{
				if (!NOBODY.equals(visitor))
				{
					administrators.add(visitor);
				}
			}
			else
			{
				days.computeIfAbsent(date, key -> new Day()).signIns.merge(visitor, 1,
					Integer::sum);
			}
		}
	}

	private void write(LocalDate date, String line)
	{
		if (folder == null)
		{
			return;
		}
		try
		{
			Files.writeString(folder.resolve(fileName(date)), line, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			failed = false;
		}
		catch (IOException e)
		{
			if (!failed)
			{
				LOG.warn("Cannot write the visits to {}: they are counted in memory only", folder, e);
				failed = true;
			}
		}
	}

	/**
	 * Reads the files of the days kept, deleting the older ones.
	 */
	private synchronized void read()
	{
		forgetOld();
		if (folder == null)
		{
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "visits-*.tsv"))
		{
			for (Path file : files)
			{
				LocalDate date = dateOf(file);
				if (date == null || date.isBefore(firstKept()))
				{
					continue;
				}
				for (String line : Files.readAllLines(file, StandardCharsets.UTF_8))
				{
					String[] fields = line.split("\t", -1);
					if (fields.length == 4 &&
						(NOBODY.equals(fields[2]) || VisitorCookie.isValid(fields[2])) &&
						(VIEW.equals(fields[1]) && PAGE.matcher(fields[3]).matches() ||
							SIGN_IN.equals(fields[1]) &&
								("user".equals(fields[3]) || "admin".equals(fields[3]))))
					{
						count(date, fields[1], fields[2], fields[3]);
					}
				}
			}
		}
		catch (IOException e)
		{
			LOG.warn("Cannot read the visits of {}", folder, e);
		}
	}

	/**
	 * Deletes the days older than {@link #KEPT}, in memory and on disk, once a day.
	 */
	private void forgetOld()
	{
		LocalDate today = today();
		if (today.equals(forgotten))
		{
			return;
		}
		forgotten = today;
		LocalDate first = firstKept();
		days.headMap(first).clear();
		if (folder == null)
		{
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "visits-*.tsv"))
		{
			for (Path file : files)
			{
				LocalDate date = dateOf(file);
				if (date != null && date.isBefore(first))
				{
					Files.deleteIfExists(file);
				}
			}
		}
		catch (IOException e)
		{
			LOG.warn("Cannot delete the old visits of {}", folder, e);
		}
	}

	/**
	 * @return the first day kept
	 */
	private LocalDate firstKept()
	{
		return today().minusDays(KEPT.toDays() - 1);
	}

	/**
	 * @return the folder, made if missing, or {@code null} if it cannot be
	 */
	private static Path prepare(Path folder)
	{
		if (folder == null)
		{
			return null;
		}
		try
		{
			Files.createDirectories(folder);
			return folder;
		}
		catch (IOException e)
		{
			LOG.warn("Cannot make the folder of the visits {}: they are counted in memory only",
				folder, e);
			return null;
		}
	}

	/**
	 * @return the name of the file of a day
	 */
	static String fileName(LocalDate date)
	{
		return "visits-" + date + ".tsv";
	}

	/**
	 * @return the day of a file, {@code null} if it is none of the visits
	 */
	private static LocalDate dateOf(Path file)
	{
		Matcher matcher = FILE.matcher(file.getFileName().toString());
		if (!matcher.matches())
		{
			return null;
		}
		try
		{
			return LocalDate.parse(matcher.group(1));
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}
}
