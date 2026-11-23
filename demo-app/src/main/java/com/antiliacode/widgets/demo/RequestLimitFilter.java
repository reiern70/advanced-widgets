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
package com.antiliacode.widgets.demo;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps robots from wearing the site out: each address gets {@value #BURST} requests at once and
 * {@value #PER_SECOND} more every second, a bucket of tokens; a request finding the bucket empty is
 * answered 429 Too Many Requests, with the seconds to wait in {@code Retry-After}, and goes no
 * further. People never get there: a page and all its scripts, styles and images, Ajax and all,
 * take far fewer. The address is the client's, as the servlet container tells it behind the
 * proxy, from its forwarded headers; the machine itself (loopback), as in development and the
 * browser tests, is never limited.
 * <p>
 * Kept in memory only, at most {@value #MAXIMUM_ADDRESSES} addresses, the ones seen least recently
 * going first; nothing is written anywhere, and an address is forgotten once its bucket is full
 * again.
 */
public class RequestLimitFilter implements Filter
{
	/** The requests an address makes at once. */
	public static final int BURST = 600;

	/** The requests an address makes every second on top, at most. */
	public static final int PER_SECOND = 10;

	/** The most addresses remembered. */
	static final int MAXIMUM_ADDRESSES = 10_000;

	/**
	 * The bucket of an address.
	 *
	 * @param tokens
	 *            the requests it may still make
	 * @param at
	 *            when it had them
	 */
	private record Bucket(double tokens, Instant at)
	{
	}

	private final Map<String, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest)
		{
			return size() > MAXIMUM_ADDRESSES;
		}
	};

	private final Clock clock;

	/** The requests counted, to forget the full buckets now and then. */
	private long requests;

	public RequestLimitFilter()
	{
		this(Clock.systemUTC());
	}

	/**
	 * @param clock
	 *            the clock of the requests, which tests move on
	 */
	RequestLimitFilter(Clock clock)
	{
		this.clock = clock;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException
	{
		Duration wait = take(request.getRemoteAddr());
		if (wait.isZero())
		{
			chain.doFilter(request, response);
			return;
		}
		HttpServletResponse http = (HttpServletResponse)response;
		http.setHeader("Retry-After", String.valueOf(Math.max(1, wait.toSeconds())));
		http.sendError(429, "Too many requests");
	}

	/**
	 * Takes a token from the bucket of an address.
	 *
	 * @param address
	 *            the address of the client, {@code null} if unknown
	 * @return zero if it had one, else how long until it has
	 */
	synchronized Duration take(String address)
	{
		if (address == null || isLoopback(address))
		{
			return Duration.ZERO;
		}
		Instant now = clock.instant();
		if (++requests % 1000 == 0)
		{
			// the addresses whose buckets are full again are as if never seen
			buckets.values().removeIf(bucket -> tokens(bucket, now) >= BURST);
		}
		Bucket bucket = buckets.get(address);
		double tokens = bucket == null ? BURST : tokens(bucket, now);
		if (tokens < 1)
		{
			buckets.put(address, new Bucket(tokens, now));
			return Duration.ofMillis((long)Math.ceil((1 - tokens) * 1000 / PER_SECOND));
		}
		buckets.put(address, new Bucket(tokens - 1, now));
		return Duration.ZERO;
	}

	/**
	 * @return the tokens of a bucket now, refilled since it was last seen
	 */
	private static double tokens(Bucket bucket, Instant now)
	{
		double seconds = Duration.between(bucket.at(), now).toMillis() / 1000.0;
		return Math.min(BURST, bucket.tokens() + seconds * PER_SECOND);
	}

	/**
	 * @return how many addresses are remembered, for tests
	 */
	synchronized int size()
	{
		return buckets.size();
	}

	private static boolean isLoopback(String address)
	{
		return address.startsWith("127.") || "::1".equals(address) ||
			"0:0:0:0:0:0:0:1".equals(address);
	}
}
