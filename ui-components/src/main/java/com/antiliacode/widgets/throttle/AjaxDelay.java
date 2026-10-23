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
package com.antiliacode.widgets.throttle;

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
