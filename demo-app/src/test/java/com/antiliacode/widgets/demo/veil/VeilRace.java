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
package com.antiliacode.widgets.demo.veil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * What the actions of {@link VeilRacePage} did, for the test driving it in a browser: how many
 * times each ran, the exceptions of the requests, and the gates a test holds an action at. The
 * page runs in the Jetty of the test, in its JVM and with its classes, so the test and the page
 * share these.
 */
final class VeilRace
{
	/** How long an action waits at its gate at most, so that a failing test does not hang. */
	private static final long GATE_SECONDS = 20;

	private static final Map<String, AtomicInteger> RUNS = new ConcurrentHashMap<>();

	private static final Map<String, CountDownLatch> GATES = new ConcurrentHashMap<>();

	private static final List<Exception> EXCEPTIONS = new CopyOnWriteArrayList<>();

	/** Records the exceptions of the requests, added to the application by the test. */
	static final IRequestCycleListener EXCEPTION_RECORDER = new IRequestCycleListener()
	{
		@Override
		public IRequestHandler onException(RequestCycle cycle, Exception ex)
		{
			EXCEPTIONS.add(ex);
			return null;
		}
	};

	private VeilRace()
	{
	}

	/**
	 * Forgets the runs, the exceptions and the gates, before a test.
	 */
	static void reset()
	{
		RUNS.clear();
		EXCEPTIONS.clear();
		GATES.values().forEach(CountDownLatch::countDown);
		GATES.clear();
	}

	/**
	 * Holds the next runs of an action at its gate, until {@link #open(String)}.
	 *
	 * @param action
	 *            the name of the action
	 */
	static void close(String action)
	{
		GATES.put(action, new CountDownLatch(1));
	}

	/**
	 * Lets the runs of an action held at its gate go on, and those to come.
	 *
	 * @param action
	 *            the name of the action
	 */
	static void open(String action)
	{
		CountDownLatch gate = GATES.remove(action);
		if (gate != null)
		{
			gate.countDown();
		}
	}

	/**
	 * Counts a run of an action, then waits at its gate while a test holds it.
	 *
	 * @param action
	 *            the name of the action
	 */
	static void run(String action)
	{
		RUNS.computeIfAbsent(action, name -> new AtomicInteger()).incrementAndGet();
		CountDownLatch gate = GATES.get(action);
		if (gate != null)
		{
			try
			{
				gate.await(GATE_SECONDS, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * @param action
	 *            the name of the action
	 * @return how many times it ran
	 */
	static int runs(String action)
	{
		AtomicInteger runs = RUNS.get(action);
		return runs == null ? 0 : runs.get();
	}

	/**
	 * @return the exceptions of the requests, in their order
	 */
	static List<Exception> exceptions()
	{
		return List.copyOf(EXCEPTIONS);
	}
}
