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
package com.antiliacode.widgets.demo.register;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.WicketApplication;

/**
 * Limits what registering costs the site, and how often the site mails anybody: at most
 * {@value #REGISTRATIONS_PER_ADDRESS} registrations from one address and
 * {@value #REGISTRATIONS} from everybody in the last {@link #WINDOW}, and at most
 * {@value #MAILS_PER_ADDRESS} emails asked from one address, {@value #MAILS_PER_RECIPIENT} to one
 * recipient and {@value #MAILS} to everybody in the last {@link #WINDOW}. It also hands out the
 * challenges of the {@link RegisterPage}, each of which answers once, so that one form solved
 * cannot be sent again and again.
 * <p>
 * Kept in memory, at most {@value #MAXIMUM_KEYS} addresses, recipients and challenges each, the
 * ones seen least recently going first: a restart forgets them.
 */
public class RegistrationLimits
{
	/** The time the limits count in, the last hour. */
	public static final Duration WINDOW = Duration.ofHours(1);

	/** The most registrations from one address in the {@link #WINDOW}. */
	public static final int REGISTRATIONS_PER_ADDRESS = 3;

	/** The most registrations from everybody in the {@link #WINDOW}. */
	public static final int REGISTRATIONS = 30;

	/** The most emails asked from one address in the {@link #WINDOW}. */
	public static final int MAILS_PER_ADDRESS = 5;

	/** The most emails to one recipient in the {@link #WINDOW}. */
	public static final int MAILS_PER_RECIPIENT = 3;

	/** The most emails to everybody in the {@link #WINDOW}. */
	public static final int MAILS = 50;

	/** How long a challenge of the register page may be answered. */
	public static final Duration CHALLENGE_TIME = Duration.ofHours(1);

	/** The most addresses, recipients and challenges remembered each. */
	static final int MAXIMUM_KEYS = 10_000;

	/** The key of the limits of everybody. */
	private static final String EVERYBODY = "";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final Clock clock;

	private final Window registrationsByAddress = new Window(REGISTRATIONS_PER_ADDRESS);

	private final Window registrations = new Window(REGISTRATIONS);

	private final Window mailsByAddress = new Window(MAILS_PER_ADDRESS);

	private final Window mailsByRecipient = new Window(MAILS_PER_RECIPIENT);

	private final Window mails = new Window(MAILS);

	/** The challenges handed out and not answered yet, and when. */
	private final Map<String, Instant> challenges = new Bounded<>();

	public RegistrationLimits()
	{
		this(Clock.systemUTC());
	}

	/**
	 * @param clock
	 *            the clock of the limits, which tests move on
	 */
	public RegistrationLimits(Clock clock)
	{
		this.clock = clock;
	}

	/**
	 * @return the limits of the running application
	 */
	public static RegistrationLimits get()
	{
		return ((WicketApplication)Application.get()).getRegistrationLimits();
	}

	/**
	 * Counts a registration, if the limits let it through.
	 *
	 * @param address
	 *            the address it comes from, {@code null} if unknown
	 * @return whether it may go on
	 */
	public synchronized boolean register(String address)
	{
		Instant now = clock.instant();
		if (!registrations.allows(EVERYBODY, now) ||
			!Strings.isEmpty(address) && !registrationsByAddress.allows(address, now))
		{
			return false;
		}
		registrations.add(EVERYBODY, now);
		if (!Strings.isEmpty(address))
		{
			registrationsByAddress.add(address, now);
		}
		return true;
	}

	/**
	 * Counts an email, if the limits let it through.
	 *
	 * @param address
	 *            the address of the request that sends it, {@code null} if unknown
	 * @param recipient
	 *            whom it goes to
	 * @return whether it may be sent
	 */
	public synchronized boolean mail(String address, String recipient)
	{
		Instant now = clock.instant();
		String to = Strings.isEmpty(recipient) ? EVERYBODY : recipient.toLowerCase(Locale.ROOT);
		if (!mails.allows(EVERYBODY, now) || !mailsByRecipient.allows(to, now) ||
			!Strings.isEmpty(address) && !mailsByAddress.allows(address, now))
		{
			return false;
		}
		mails.add(EVERYBODY, now);
		mailsByRecipient.add(to, now);
		if (!Strings.isEmpty(address))
		{
			mailsByAddress.add(address, now);
		}
		return true;
	}

	/**
	 * @return a new challenge, to be answered once within {@link #CHALLENGE_TIME}
	 */
	public synchronized String newChallenge()
	{
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		challenges.put(challenge, clock.instant());
		return challenge;
	}

	/**
	 * Answers a challenge, which then answers no more.
	 *
	 * @param challenge
	 *            the challenge, {@code null} for none
	 * @return whether it was handed out, not answered yet and not too old
	 */
	public synchronized boolean answer(String challenge)
	{
		if (challenge == null)
		{
			return false;
		}
		Instant handedOut = challenges.remove(challenge);
		return handedOut != null && !handedOut.plus(CHALLENGE_TIME).isBefore(clock.instant());
	}

	/**
	 * A map of at most {@value RegistrationLimits#MAXIMUM_KEYS} keys, the ones seen least
	 * recently going first.
	 */
	private static class Bounded<V> extends LinkedHashMap<String, V>
	{
		private static final long serialVersionUID = 1L;

		Bounded()
		{
			super(16, 0.75f, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, V> eldest)
		{
			return size() > MAXIMUM_KEYS;
		}
	}

	/**
	 * The times of what happened in the last {@link RegistrationLimits#WINDOW}, by key, and how
	 * many it allows.
	 */
	private static class Window
	{
		private final int maximum;

		private final Map<String, Deque<Instant>> times = new Bounded<>();

		Window(int maximum)
		{
			this.maximum = maximum;
		}

		boolean allows(String key, Instant now)
		{
			Deque<Instant> recent = times.get(key);
			if (recent == null)
			{
				return true;
			}
			Instant start = now.minus(WINDOW);
			while (!recent.isEmpty() && !recent.peekFirst().isAfter(start))
			{
				recent.removeFirst();
			}
			return recent.size() < maximum;
		}

		void add(String key, Instant now)
		{
			times.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(now);
		}
	}
}
