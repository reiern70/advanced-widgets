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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * Each address gets {@value RequestLimitFilter#BURST} requests at once and
 * {@value RequestLimitFilter#PER_SECOND} more a second, then 429; see {@link RequestLimitFilter}.
 */
class RequestLimitFilterTest
{
	private final TestClock clock = new TestClock();

	private final RequestLimitFilter filter = new RequestLimitFilter(clock);

	@Test
	void anAddressGetsABurstThenWaits()
	{
		for (int i = 0; i < RequestLimitFilter.BURST; i++)
		{
			assertEquals(Duration.ZERO, filter.take("203.0.113.7"), "request " + i);
		}

		Duration wait = filter.take("203.0.113.7");
		assertTrue(wait.isPositive());
		assertTrue(wait.compareTo(Duration.ofSeconds(1)) <= 0, wait.toString());
		// another address is not limited
		assertEquals(Duration.ZERO, filter.take("198.51.100.1"));
	}

	@Test
	void theBucketFillsAgainEverySecond()
	{
		for (int i = 0; i < RequestLimitFilter.BURST; i++)
		{
			filter.take("203.0.113.7");
		}
		clock.advance(Duration.ofSeconds(1));

		for (int i = 0; i < RequestLimitFilter.PER_SECOND; i++)
		{
			assertEquals(Duration.ZERO, filter.take("203.0.113.7"), "request " + i);
		}
		assertTrue(filter.take("203.0.113.7").isPositive());
	}

	@Test
	void theMachineItselfIsNeverLimited()
	{
		for (int i = 0; i < 2 * RequestLimitFilter.BURST; i++)
		{
			assertEquals(Duration.ZERO, filter.take("127.0.0.1"));
			assertEquals(Duration.ZERO, filter.take("0:0:0:0:0:0:0:1"));
		}
		assertEquals(0, filter.size());
	}

	@Test
	void fullBucketsAreForgotten()
	{
		filter.take("203.0.113.7");
		clock.advance(Duration.ofMinutes(5));

		for (int i = 0; i < 1000; i++)
		{
			filter.take("198.51.100." + (i % 3));
		}

		assertEquals(3, filter.size());
	}
}
