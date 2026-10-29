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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.dynamicdatatable.UpdateRowMessageType;

/**
 * Advances the progress of random users among those a page shows, on a timer, and pushes the
 * changed rows to the page over its web socket connection, at most once per {@link #FLUSH_MILLIS}
 * however often they change. Rows the page does not show are neither advanced nor pushed: the
 * page reports which rows it shows with {@link #show(String, int, Collection)}, whenever the
 * table, or the infinite list, sends rows to the browser.
 * <p>
 * One simulator runs per page, from the opening of the page's web socket connection to its
 * closing. The rows shown are kept for the last {@value #PAGES_PER_SESSION} pages of a session,
 * and for {@link #STALE} after a page of the session showed or advanced them last, so that the
 * pages whose web socket never connects leave nothing behind.
 */
public final class ProgressSimulator
{
	private static final Logger LOG = LoggerFactory.getLogger(ProgressSimulator.class);

	private static final long TICK_MILLIS = 100;

	private static final long FLUSH_MILLIS = 3000;

	private static final int USERS_PER_TICK = 3;

	private static final ScheduledExecutorService TIMER = Executors.newScheduledThreadPool(1,
		runnable -> {
			Thread thread = new Thread(runnable, "ant-table-progress");
			thread.setDaemon(true);
			return thread;
		});

	private static final ConcurrentMap<String, ProgressSimulator> RUNNING =
		new ConcurrentHashMap<>();

	/** The most pages of a session whose rows shown are kept. */
	static final int PAGES_PER_SESSION = 10;

	/** How long the rows shown are kept after a page of the session showed or advanced them. */
	static final Duration STALE = Duration.ofMinutes(10);

	/** The rows the pages show, by the id of their session. */
	private static final ConcurrentMap<String, Pages> SHOWN = new ConcurrentHashMap<>();

	static
	{
		TIMER.scheduleWithFixedDelay(() -> forget(System.currentTimeMillis()), 1, 1,
			TimeUnit.MINUTES);
	}

	private final Application application;

	private final String sessionId;

	private final int pageId;

	private final IKey key;

	private final String tableId;

	private final UsersDatabase database;

	/** The language of the page, which the pushed rows show the countries in. */
	private final Locale locale;

	private final Set<Long> changed = ConcurrentHashMap.newKeySet();

	private final List<ScheduledFuture<?>> tasks = new ArrayList<>();

	private ProgressSimulator(Application application, String sessionId, int pageId, IKey key,
		String tableId, UsersDatabase database, Locale locale)
	{
		this.locale = locale;
		this.application = application;
		this.sessionId = sessionId;
		this.pageId = pageId;
		this.key = key;
		this.tableId = tableId;
		this.database = database;
	}

	/**
	 * Starts simulating for a page, replacing a simulator already running for it.
	 */
	public static void start(Application application, String sessionId, int pageId, IKey key,
		String tableId, UsersDatabase database, Locale locale)
	{
		ProgressSimulator simulator = new ProgressSimulator(application, sessionId, pageId, key,
			tableId, database, locale);
		ProgressSimulator previous = RUNNING.put(id(sessionId, pageId), simulator);
		if (previous != null)
		{
			previous.cancel();
		}
		simulator.schedule();
	}

	/**
	 * Stops simulating for a page.
	 */
	public static void stop(String sessionId, int pageId)
	{
		ProgressSimulator simulator = RUNNING.remove(id(sessionId, pageId));
		SHOWN.computeIfPresent(String.valueOf(sessionId), (session, pages) -> {
			synchronized (pages)
			{
				pages.remove(pageId);
				return pages.isEmpty() ? null : pages;
			}
		});
		if (simulator != null)
		{
			simulator.cancel();
		}
	}

	/**
	 * Records which users a page shows; may be called before the simulator of the page starts.
	 */
	public static void show(String sessionId, int pageId, Collection<Long> ids)
	{
		List<Long> shown = List.copyOf(ids);
		SHOWN.compute(String.valueOf(sessionId), (session, pages) -> {
			Pages kept = pages != null ? pages : new Pages();
			synchronized (kept)
			{
				kept.put(pageId, shown);
				kept.touched = System.currentTimeMillis();
			}
			return kept;
		});
	}

	/**
	 * @return the users a page shows, as it recorded them last; none if it did not
	 */
	static List<Long> shown(String sessionId, int pageId)
	{
		Pages pages = SHOWN.get(String.valueOf(sessionId));
		if (pages == null)
		{
			return List.of();
		}
		synchronized (pages)
		{
			pages.touched = System.currentTimeMillis();
			return pages.getOrDefault(pageId, List.of());
		}
	}

	/**
	 * Forgets the rows shown by the sessions none of whose pages showed or advanced them for
	 * {@link #STALE}, and that run no simulator.
	 *
	 * @param now
	 *            the time now, in milliseconds
	 */
	static void forget(long now)
	{
		SHOWN.entrySet().removeIf(entry -> now - entry.getValue().touched > STALE.toMillis() &&
			RUNNING.keySet().stream().noneMatch(id -> id.startsWith(entry.getKey() + '/')));
	}

	private static String id(String sessionId, int pageId)
	{
		return sessionId + '/' + pageId;
	}

	private List<Long> shown()
	{
		return shown(sessionId, pageId);
	}

	private synchronized void schedule()
	{
		tasks.add(TIMER.scheduleAtFixedRate(this::advance, TICK_MILLIS, TICK_MILLIS,
			TimeUnit.MILLISECONDS));
		tasks.add(TIMER.scheduleAtFixedRate(this::flush, FLUSH_MILLIS, FLUSH_MILLIS,
			TimeUnit.MILLISECONDS));
	}

	private synchronized void cancel()
	{
		tasks.forEach(task -> task.cancel(false));
		tasks.clear();
	}

	private void advance()
	{
		List<Long> shown = shown();
		for (int i = 0; i < USERS_PER_TICK && shown.isEmpty() == false; i++)
		{
			Long id = shown.get(ThreadLocalRandom.current().nextInt(shown.size()));
			try
			{
				User user = database.get(id);
				int progress = user.getProgress() + ThreadLocalRandom.current().nextInt(1, 8);
				user.setProgress(progress > 100 ? 0 : progress);
				changed.add(id);
			}
			catch (RuntimeException deleted)
			{
				continue;
			}
		}
	}

	/**
	 * The rows shown by the last pages of a session, the page shown or advanced last coming last.
	 */
	private static final class Pages extends LinkedHashMap<Integer, List<Long>>
	{
		private static final long serialVersionUID = 1L;

		/** When a page of the session showed or advanced its rows last, in milliseconds. */
		private volatile long touched = System.currentTimeMillis();

		Pages()
		{
			super(16, 0.75f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, List<Long>> eldest)
		{
			return size() > PAGES_PER_SESSION;
		}
	}

	private void flush()
	{
		List<Long> ids = new ArrayList<>(changed);
		changed.removeAll(ids);
		ids.retainAll(shown());
		for (Long id : ids)
		{
			User user;
			try
			{
				user = database.get(id);
			}
			catch (RuntimeException deleted)
			{
				continue;
			}
			try
			{
				boolean sent = User.inLanguage(locale,
					() -> UpdateRowMessageType.newMessage(tableId, String.valueOf(id), user)
						.send(application, sessionId, key));
				if (sent == false)
				{
					stop(sessionId, pageId);
					return;
				}
			}
			catch (IOException | RuntimeException e)
			{
				LOG.debug("Pushing the progress of user {} failed, stopping", id, e);
				stop(sessionId, pageId);
				return;
			}
		}
	}
}
