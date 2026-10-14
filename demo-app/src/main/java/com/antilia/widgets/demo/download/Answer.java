/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import java.time.Duration;

/**
 * How the server of the downloads demo answers when the browser asks for a file, to see the
 * buttons busy and the downloads that fail. The names are in the bundle of the page, under
 * {@code Answer.<name>}.
 */
public enum Answer
{
	/** With the file, at once. */
	AT_ONCE(Duration.ZERO),

	/** With the file, after a while. */
	SLOWLY(Duration.ofSeconds(3)),

	/** With an error, after a second. */
	FAILS(Duration.ofSeconds(1));

	private final Duration delay;

	Answer(Duration delay)
	{
		this.delay = delay;
	}

	/**
	 * @return how long the server waits before it answers
	 */
	public Duration getDelay()
	{
		return delay;
	}

	/**
	 * Waits as long as the server does before it answers.
	 */
	void await()
	{
		if (delay.isZero())
		{
			return;
		}
		try
		{
			Thread.sleep(delay);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
}
