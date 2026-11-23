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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.wicket.Application;

import com.antiliacode.widgets.demo.WicketApplication;

/**
 * The sessions signed in now as {@value Accounts#USER}, at most {@value #MAXIMUM} at once; the
 * administrator always signs in and takes no place. A session counts from signing in until it
 * signs out, ends, or goes {@link WicketApplication#SESSION_TIMEOUT} without a request, after which
 * the servlet container ends it. Kept in memory, as the sessions are, and only by the ids of the
 * sessions: nothing tells who is behind one.
 * <p>
 * A session keeps its place for {@link #PLACE_TIME} for sure. After that it goes on as long as
 * nobody else needs the place: a session signing in that finds every place taken takes the one
 * held longest beyond its time, and the session that held it no longer
 * {@link #holdsPlace(String) holds a place}, so that it is signed out at its next request. A few
 * sessions kept signed in can thus keep the others out for {@link #PLACE_TIME} at most, not for
 * good.
 */
public class SignedInUsers
{
	/** The most sessions signed in at once as {@value Accounts#USER}. */
	public static final int MAXIMUM = 10;

	/** How long a session keeps its place for sure, before another one may take it. */
	public static final Duration PLACE_TIME = Duration.ofMinutes(30);

	/**
	 * A session signed in: since when, and when it made its last request.
	 *
	 * @param since
	 *            when it signed in
	 * @param seen
	 *            when it made its last request
	 */
	public record Place(Instant since, Instant seen)
	{
	}

	/** The places, by the id of the session holding each. */
	private final Map<String, Place> places = new HashMap<>();

	private final Clock clock;

	private final Duration timeout;

	private final Duration placeTime;

	public SignedInUsers()
	{
		this(Clock.systemUTC(), WicketApplication.SESSION_TIMEOUT, PLACE_TIME);
	}

	/**
	 * @param clock
	 *            the clock of the requests
	 * @param timeout
	 *            how long a session lasts without a request
	 * @param placeTime
	 *            how long a session keeps its place for sure
	 */
	SignedInUsers(Clock clock, Duration timeout, Duration placeTime)
	{
		this.clock = clock;
		this.timeout = timeout;
		this.placeTime = placeTime;
	}

	/**
	 * @return the sessions signed in of the running application
	 */
	public static SignedInUsers get()
	{
		return ((WicketApplication)Application.get()).getSignedInUsers();
	}

	/**
	 * Signs a session in, when there is room for it: always as the administrator, who takes no
	 * place; else while fewer than {@value #MAXIMUM} sessions are signed in, or by taking the
	 * place held longest beyond {@link #PLACE_TIME}.
	 *
	 * @param role
	 *            what the account signing in may do
	 * @param signIn
	 *            signs the session in, and answers its id, a new one
	 * @return whether the session signed in
	 */
	public boolean signIn(Role role, Supplier<String> signIn)
	{
		if (role == Role.ADMIN)
		{
			signIn.get();
			return true;
		}
		synchronized (this)
		{
			forgetInactive();
			if (places.size() >= MAXIMUM)
			{
				Optional<String> overdue = overdue();
				if (overdue.isEmpty())
				{
					return false;
				}
				places.remove(overdue.get());
			}
			String id = signIn.get();
			Instant now = clock.instant();
			places.put(id, new Place(now, now));
			return true;
		}
	}

	/**
	 * @return whether a session signing in now as {@value Accounts#USER} finds a place
	 */
	public synchronized boolean hasRoom()
	{
		forgetInactive();
		return places.size() < MAXIMUM || overdue().isPresent();
	}

	/**
	 * @return how long until a place is free for sure, zero if one is now
	 */
	public synchronized Duration untilRoom()
	{
		forgetInactive();
		if (places.size() < MAXIMUM)
		{
			return Duration.ZERO;
		}
		Instant first = places.values().stream()
			.map(Place::since)
			.min(Comparator.naturalOrder())
			.orElseThrow();
		Duration wait = Duration.between(clock.instant(), first.plus(placeTime));
		return wait.isNegative() ? Duration.ZERO : wait;
	}

	/**
	 * @param id
	 *            the id of a session
	 * @return whether the session holds a place
	 */
	public synchronized boolean holdsPlace(String id)
	{
		forgetInactive();
		return places.containsKey(id);
	}

	/**
	 * @return how many sessions are signed in as {@value Accounts#USER} now
	 */
	public synchronized int count()
	{
		forgetInactive();
		return places.size();
	}

	/**
	 * @return the places held now, the one held longest first
	 */
	public synchronized List<Place> places()
	{
		forgetInactive();
		return places.values().stream().sorted(Comparator.comparing(Place::since)).toList();
	}

	/**
	 * Counts a request of a session: it keeps its place.
	 *
	 * @param id
	 *            the id of the session
	 */
	public synchronized void seen(String id)
	{
		places.computeIfPresent(id, (key, place) -> new Place(place.since(), clock.instant()));
	}

	/**
	 * Gives the place of a session back, when it signs out or ends.
	 *
	 * @param id
	 *            the id of the session
	 */
	public synchronized void signedOut(String id)
	{
		places.remove(id);
	}

	/**
	 * @return the session holding its place longest beyond {@link #PLACE_TIME}, if any
	 */
	private Optional<String> overdue()
	{
		Instant ended = clock.instant().minus(placeTime);
		return places.entrySet().stream()
			.filter(entry -> !entry.getValue().since().isAfter(ended))
			.min(Comparator.comparing(entry -> entry.getValue().since()))
			.map(Map.Entry::getKey);
	}

	private void forgetInactive()
	{
		Instant ended = clock.instant().minus(timeout);
		places.values().removeIf(place -> place.seen().isBefore(ended));
	}
}
