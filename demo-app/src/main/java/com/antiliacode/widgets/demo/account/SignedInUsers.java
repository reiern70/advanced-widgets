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
import java.util.function.Supplier;

import org.apache.wicket.Application;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Role;

/**
 * The users signed in now, so that at most {@value #MAXIMUM} are at once: an account signs in
 * while fewer other accounts are signed in, or when it is signed in already, in another browser
 * say. Administrators always sign in and are not counted. A session counts from signing in until
 * it signs out, ends, or goes {@link WicketApplication#SESSION_TIMEOUT} without a request, after
 * which the servlet container ends it. Kept in memory, as the sessions are.
 */
public class SignedInUsers
{
	/** The most users signed in at once, administrators apart. */
	public static final int MAXIMUM = 5;

	/** A session signed in to an account, and when it made its last request. */
	private record Session(String account, Instant seen)
	{
	}

	/** The sessions signed in to accounts that are not administrators', by id. */
	private final Map<String, Session> sessions = new HashMap<>();

	private final Clock clock;

	private final Duration timeout;

	public SignedInUsers()
	{
		this(Clock.systemUTC(), WicketApplication.SESSION_TIMEOUT);
	}

	/**
	 * @param clock
	 *            the clock of the requests
	 * @param timeout
	 *            how long a session lasts without a request
	 */
	SignedInUsers(Clock clock, Duration timeout)
	{
		this.clock = clock;
		this.timeout = timeout;
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
	public synchronized boolean signIn(String account, Role role, Supplier<String> signIn)
	{
		if (!hasRoomFor(account, role))
		{
			return false;
		}
		String id = signIn.get();
		if (role != Role.ADMIN)
		{
			sessions.put(id, new Session(account, clock.instant()));
		}
		return true;
	}

	/**
	 * @param account
	 *            the name of an account
	 * @param role
	 *            what the account may do
	 * @return whether the account may sign in now
	 */
	public synchronized boolean hasRoomFor(String account, Role role)
	{
		forgetInactive();
		return role == Role.ADMIN ||
			sessions.values().stream().anyMatch(session -> session.account().equals(account)) ||
			count() < MAXIMUM;
	}

	/**
	 * @return how many accounts are signed in now, administrators apart
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
