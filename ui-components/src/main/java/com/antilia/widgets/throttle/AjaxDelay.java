/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.throttle;

import java.time.Duration;

/**
 * How long the server waits before taking on each Wicket Ajax request of a session, as a slow
 * network or a busy server would make it wait: see {@link Throttle}.
 *
 * @since 11.0.0
 */
public enum AjaxDelay
{
	/** No waiting. */
	NONE(0),
	/** Half a second. */
	HALF_SECOND(500),
	/** A second. */
	SECOND(1000),
	/** Two seconds. */
	TWO_SECONDS(2000),
	/** Five seconds. */
	FIVE_SECONDS(5000);

	private final Duration duration;

	AjaxDelay(long millis)
	{
		duration = Duration.ofMillis(millis);
	}

	/**
	 * @return how long the server waits
	 */
	public Duration getDuration()
	{
		return duration;
	}
}
