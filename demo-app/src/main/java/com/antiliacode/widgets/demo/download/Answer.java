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
package com.antiliacode.widgets.demo.download;

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
