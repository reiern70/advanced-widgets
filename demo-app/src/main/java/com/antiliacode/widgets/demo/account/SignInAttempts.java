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

import org.apache.wicket.Application;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.WicketApplication;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The failed attempts to sign in, by the address they came from, whatever the account tried:
 * after {@value #MAXIMUM_FAILURES} failures an address is blocked for {@link #BLOCKED}, and its
 * attempts are turned away without checking the password. Not by account: everybody shares
 * {@value Accounts#USER}, which a robot could otherwise lock for everybody.
 * <p>
 * The failures of an address are forgotten {@link #BLOCKED} after the last one, or once its block
 * is over; at most {@value #MAXIMUM_ADDRESSES} addresses are remembered, the ones seen least
 * recently going first. Kept in memory only: nothing is written anywhere, and a restart forgets
 * them.
 */
public class SignInAttempts
{
	/** The failed attempts from an address that block it. */
	public static final int MAXIMUM_FAILURES = 10;

	/** How long an address stays blocked, and how long its failures are remembered. */
	public static final Duration BLOCKED = Duration.ofMinutes(30);

	/** The most addresses remembered. */
	static final int MAXIMUM_ADDRESSES = 10_000;

	/**
	 * The failures of an address.
	 *
	 * @param count
	 *            how many
	 * @param last
	 *            when the last one was
	 */
	private record Failures(int count, Instant last)
	{
	}

	private final Map<String, Failures> addresses = new LinkedHashMap<>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Failures> eldest)
		{
			return size() > MAXIMUM_ADDRESSES;
		}
	};

	private final Clock clock;

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
	 * @return the attempts of the running application
	 */
	public static SignInAttempts get()
	{
		return ((WicketApplication)Application.get()).getSignInAttempts();
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
	 * Counts a failed attempt to sign in.
	 *
	 * @param address
	 *            the address it came from, {@code null} if unknown
	 */
	public synchronized void failed(String address)
	{
		if (Strings.isEmpty(address))
		{
			return;
		}
		Instant now = clock.instant();
		Failures before = current(address, now);
		addresses.put(address, new Failures(before == null ? 1 : before.count() + 1, now));
	}

	/**
	 * Forgets the failures of an address that signed in.
	 *
	 * @param address
	 *            the address, {@code null} if unknown
	 */
	public synchronized void succeeded(String address)
	{
		if (!Strings.isEmpty(address))
		{
			addresses.remove(address);
		}
	}

	/**
	 * @param address
	 *            the address an attempt comes from, {@code null} if unknown
	 * @return how long the address is still blocked; zero if it may try now
	 */
	public synchronized Duration retryAfter(String address)
	{
		if (Strings.isEmpty(address))
		{
			return Duration.ZERO;
		}
		Instant now = clock.instant();
		Failures failures = current(address, now);
		if (failures == null || failures.count() < MAXIMUM_FAILURES)
		{
			return Duration.ZERO;
		}
		return Duration.between(now, failures.last().plus(BLOCKED));
	}

	/**
	 * @return how many addresses are remembered, for tests
	 */
	synchronized int size()
	{
		return addresses.size();
	}

	/**
	 * @return the failures of an address that are not forgotten yet, {@code null} if none
	 */
	private Failures current(String address, Instant now)
	{
		Failures failures = addresses.get(address);
		if (failures != null && !failures.last().plus(BLOCKED).isAfter(now))
		{
			addresses.remove(address);
			return null;
		}
		return failures;
	}
}
