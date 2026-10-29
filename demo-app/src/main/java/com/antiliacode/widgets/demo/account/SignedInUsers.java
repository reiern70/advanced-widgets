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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.wicket.Application;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Role;

/**
 * The users signed in now, so that at most {@value #MAXIMUM} accounts registered by visitors are
 * at once: such an account signs in while fewer others are signed in, or when it is signed in
 * already, in another browser say. Administrators, the accounts the demo always has and those the
 * server is started with (see {@link Accounts#isAlwaysSignedIn(String)}) always sign in and are
 * not counted, so that registered accounts cannot keep them out. A session counts from signing in
 * until it signs out, ends, or goes {@link WicketApplication#SESSION_TIMEOUT} without a request,
 * after which the servlet container ends it. Kept in memory, as the sessions are.
 */
public class SignedInUsers
{
	/** The most users signed in at once, those always signed in apart. */
	public static final int MAXIMUM = 5;

	/** A session signed in to an account, and when it made its last request. */
	private record Session(String account, Instant seen)
	{
	}

	/** The sessions signed in to accounts that are counted, by id. */
	private final Map<String, Session> sessions = new HashMap<>();

	private final Clock clock;

	private final Duration timeout;

	/** Whether an account, not an administrator's, always signs in and is not counted. */
	private final Predicate<String> alwaysSignedIn;

	public SignedInUsers()
	{
		this(Clock.systemUTC(), WicketApplication.SESSION_TIMEOUT,
			account -> Accounts.get().isAlwaysSignedIn(account));
	}

	/**
	 * @param clock
	 *            the clock of the requests
	 * @param timeout
	 *            how long a session lasts without a request
	 * @param alwaysSignedIn
	 *            whether an account, not an administrator's, always signs in and is not counted
	 */
	SignedInUsers(Clock clock, Duration timeout, Predicate<String> alwaysSignedIn)
	{
		this.clock = clock;
		this.timeout = timeout;
		this.alwaysSignedIn = alwaysSignedIn;
	}

	/**
	 * @return the users signed in to the running application
	 */
	public static SignedInUsers get()
	{
		return ((WicketApplication)Application.get()).getSignedInUsers();
	}

	/**
	 * Signs a session in when there is room for the account: then calls the sign in, which gives
	 * the session its new id, and counts the session under that id.
	 *
	 * @param account
	 *            the name of the account
	 * @param role
	 *            what the account may do
	 * @param signIn
	 *            signs the session in and returns its id
	 * @return whether the session signed in; {@code false} if {@value #MAXIMUM} other accounts are
	 *         signed in
	 */
	public boolean signIn(String account, Role role, Supplier<String> signIn)
	{
		// outside the lock, as it may read the table of the accounts
		boolean counted = isCounted(account, role);
		synchronized (this)
		{
			if (counted && !hasRoomFor(account))
			{
				return false;
			}
			String id = signIn.get();
			if (counted)
			{
				sessions.put(id, new Session(account, clock.instant()));
			}
			return true;
		}
	}

	/**
	 * @param account
	 *            the name of an account
	 * @param role
	 *            what the account may do
	 * @return whether the account may sign in now
	 */
	public boolean hasRoomFor(String account, Role role)
	{
		return !isCounted(account, role) || hasRoomFor(account);
	}

	private synchronized boolean hasRoomFor(String account)
	{
		forgetInactive();
		return sessions.values().stream().anyMatch(session -> session.account().equals(account)) ||
			count() < MAXIMUM;
	}

	/**
	 * @return whether the account counts among the {@value #MAXIMUM}: not an administrator's, nor
	 *         one always signed in
	 */
	private boolean isCounted(String account, Role role)
	{
		return role != Role.ADMIN && !alwaysSignedIn.test(account);
	}

	/**
	 * @return how many accounts are signed in now, those always signed in apart
	 */
	public synchronized long count()
	{
		forgetInactive();
		return sessions.values().stream().map(Session::account).distinct().count();
	}

	/**
	 * Notes that a session made a request, which keeps it counted.
	 *
	 * @param id
	 *            the id of the session
	 */
	public synchronized void seen(String id)
	{
		sessions.computeIfPresent(id, (key, session) -> new Session(session.account(),
			clock.instant()));
	}

	/**
	 * Stops counting a session, which signed out or ended.
	 *
	 * @param id
	 *            the id of the session
	 */
	public synchronized void signedOut(String id)
	{
		sessions.remove(id);
	}

	private void forgetInactive()
	{
		Instant ended = clock.instant().minus(timeout);
		sessions.values().removeIf(session -> session.seen().isBefore(ended));
	}
}
