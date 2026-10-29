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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The failed attempts to sign in, by account and by the address they came from, on the site and
 * on the administration's API alike, even from sessions that start anew each time. They ask for
 * a captcha after {@value #BEFORE_CAPTCHA} failures on an account; from
 * {@value #BEFORE_WAITING} failures on, the account waits before the next attempt, a second at
 * first, then twice as long after each failure, {@link #LONGEST_WAIT} at most; an address waits
 * likewise from {@value #BEFORE_ADDRESS_WAITING} failures on, whatever the accounts tried. While
 * an account or an address waits, attempts are turned away without checking the password; nobody
 * is locked out for longer than {@link #LONGEST_WAIT}.
 * <p>
 * The names of accounts that do not exist are counted apart, so that a flood of them never pushes
 * out the failures of a real account, and they get a captcha and wait as accounts do, so that
 * neither tells whether an account exists. The failures of an account are forgotten
 * {@link #ACCOUNT_MEMORY} after the last one, or when it signs in, those of an address
 * {@link #ADDRESS_MEMORY} after the last one; at most {@value #MAXIMUM_ENTRIES} accounts, names and
 * addresses each are remembered, the ones seen least recently going first. Kept in memory: a
 * restart forgets them.
 */
public class SignInAttempts
{
	/** The failed attempts on an account before a captcha is asked. */
	public static final int BEFORE_CAPTCHA = 3;

	/** The failed attempts on an account before it waits. */
	public static final int BEFORE_WAITING = 5;

	/** The failed attempts from an address before it waits. */
	public static final int BEFORE_ADDRESS_WAITING = 20;

	/** The first wait, which doubles at each failure after it. */
	public static final Duration FIRST_WAIT = Duration.ofSeconds(1);

	/** The longest wait. */
	public static final Duration LONGEST_WAIT = Duration.ofMinutes(15);

	/** How long the failures on an account are remembered after the last one. */
	static final Duration ACCOUNT_MEMORY = Duration.ofHours(24);

	/** How long the failures from an address are remembered after the last one. */
	static final Duration ADDRESS_MEMORY = Duration.ofHours(1);

	/** The most accounts, names or addresses remembered each; a robot cannot fill the memory. */
	static final int MAXIMUM_ENTRIES = 10_000;

	private final Clock clock;

	/** The failures on the accounts that exist, by name. */
	private final Failures accounts = new Failures(BEFORE_WAITING, ACCOUNT_MEMORY);

	/** The failures on names that are no account. */
	private final Failures unknownNames = new Failures(BEFORE_WAITING, ACCOUNT_MEMORY);

	/** The failures by the address they came from. */
	private final Failures addresses = new Failures(BEFORE_ADDRESS_WAITING, ADDRESS_MEMORY);

	public SignInAttempts()
	{
		this(Clock.systemUTC());
	}

	/**
	 * @param clock
	 *            the clock of the attempts, which tests move on
	 */
	public SignInAttempts(Clock clock)
	{
		this.clock = clock;
	}

	/**
	 * @return the address the current request came from, as the servlet container tells it
	 *         (behind the proxy, from its forwarded headers); {@code null} without a request
	 */
	public static String address()
	{
		RequestCycle cycle = RequestCycle.get();
		if (cycle != null &&
			cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request)
		{
			return request.getRemoteAddr();
		}
		return null;
	}

	/**
	 * Counts a failed attempt to sign in to an account.
	 *
	 * @param name
	 *            the account as typed
	 * @param address
	 *            the address the attempt came from, {@code null} if unknown
	 * @param known
	 *            whether there is an account by that name
	 */
	public synchronized void failed(String name, String address, boolean known)
	{
		Instant now = clock.instant();
		if (!Strings.isEmpty(name))
		{
			(known ? accounts : unknownNames).failed(Accounts.accountName(name), now);
		}
		if (!Strings.isEmpty(address))
		{
			addresses.failed(address, now);
		}
	}

	/**
	 * Forgets the failures of an account, which was signed in to. Those of the address stay.
	 *
	 * @param name
	 *            the account
	 */
	public synchronized void succeeded(String name)
	{
		if (!Strings.isEmpty(name))
		{
			accounts.forget(Accounts.accountName(name));
		}
	}

	/**
	 * @param name
	 *            the account as typed
	 * @return whether signing in to the account asks for a captcha
	 */
	public synchronized boolean needsCaptcha(String name)
	{
		if (Strings.isEmpty(name))
		{
			return false;
		}
		String key = Accounts.accountName(name);
		Instant now = clock.instant();
		return Math.max(accounts.count(key, now), unknownNames.count(key, now)) >= BEFORE_CAPTCHA;
	}

	/**
	 * @param name
	 *            the account as typed
	 * @param address
	 *            the address the attempt comes from, {@code null} if unknown
	 * @return how long the account or the address still waits before the next attempt; zero if
	 *         it may try now
	 */
	public synchronized Duration retryAfter(String name, String address)
	{
		Instant now = clock.instant();
		Instant until = now;
		if (!Strings.isEmpty(name))
		{
			String key = Accounts.accountName(name);
			until = latest(until, accounts.waitsUntil(key, now));
			until = latest(until, unknownNames.waitsUntil(key, now));
		}
		if (!Strings.isEmpty(address))
		{
			until = latest(until, addresses.waitsUntil(address, now));
		}
		return Duration.between(now, until);
	}

	private static Instant latest(Instant one, Instant other)
	{
		return other.isAfter(one) ? other : one;
	}

	/**
	 * How long to wait after a failure.
	 *
	 * @param failures
	 *            the failures so far, this one included
	 * @param before
	 *            the failures before waiting
	 * @return the wait, zero before waiting
	 */
	static Duration waitAfter(int failures, int before)
	{
		if (failures < before)
		{
			return Duration.ZERO;
		}
		int doublings = Math.min(failures - before, 30);
		Duration wait = FIRST_WAIT.multipliedBy(1L << doublings);
		return wait.compareTo(LONGEST_WAIT) > 0 ? LONGEST_WAIT : wait;
	}

	/**
	 * The failures by one kind of key, accounts, names or addresses: how many, the last one, and
	 * until when the key waits. The keys seen least recently go first once there are
	 * {@value SignInAttempts#MAXIMUM_ENTRIES}.
	 */
	private static class Failures
	{
		private record Tally(int count, Instant last, Instant waitsUntil)
		{
		}

		private final int beforeWaiting;

		private final Duration memory;

		private final Map<String, Tally> entries = new LinkedHashMap<>(16, 0.75f, true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Tally> eldest)
			{
				return size() > MAXIMUM_ENTRIES;
			}
		};

		Failures(int beforeWaiting, Duration memory)
		{
			this.beforeWaiting = beforeWaiting;
			this.memory = memory;
		}

		void failed(String key, Instant now)
		{
			Tally entry = current(key, now);
			int count = entry == null ? 1 : entry.count() + 1;
			entries.put(key, new Tally(count, now, now.plus(waitAfter(count, beforeWaiting))));
		}

		void forget(String key)
		{
			entries.remove(key);
		}

		int count(String key, Instant now)
		{
			Tally entry = current(key, now);
			return entry == null ? 0 : entry.count();
		}

		Instant waitsUntil(String key, Instant now)
		{
			Tally entry = current(key, now);
			return entry == null ? now : entry.waitsUntil();
		}

		/**
		 * @return the failures of the key, {@code null} if there are none or they are forgotten
		 */
		private Tally current(String key, Instant now)
		{
			Tally entry = entries.get(key);
			if (entry != null && entry.last().plus(memory).isBefore(now))
			{
				entries.remove(key);
				return null;
			}
			return entry;
		}
	}
}
