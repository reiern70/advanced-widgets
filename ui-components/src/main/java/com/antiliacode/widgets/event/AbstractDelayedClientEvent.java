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
package com.antiliacode.widgets.event;

import java.time.Duration;

/**
 * An event that may wait for the user to pause before it runs its handler: those going to the
 * server, so that typing sends one request, not one a key. On each event, the browser waits
 * {@link #getDelay()} for the next of its kind, each new one postponing it, then runs the handler
 * once. The delay goes to the browser in the {@link #toJson() JSON} of the event, as
 * {@code delay}.
 *
 * @param <T>
 *            the event, which its setters return
 * @since 11.0.0
 */
public abstract class AbstractDelayedClientEvent<T extends AbstractDelayedClientEvent<T>>
	extends AbstractClientEvent<T>
{
	private static final long serialVersionUID = 1L;

	/** The delay of an event that does not wait. */
	public static final int NO_DELAY = -1;

	private int delay = NO_DELAY;

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param clientHandlerName
	 *            the name of the JavaScript function the browser runs on the event, {@code null}
	 *            for none
	 * @param clientSideCode
	 *            the JavaScript function the browser runs on the event, in place of the one
	 *            named, {@code null} for none
	 */
	protected AbstractDelayedClientEvent(String eventName, String clientHandlerName,
		String clientSideCode)
	{
		super(eventName, clientHandlerName, clientSideCode);
	}

	/**
	 * @return how long the event waits for the next of its kind, in milliseconds, each new one
	 *         postponing it until they pause; {@link #NO_DELAY}, as by default, for not at all
	 */
	public int getDelay()
	{
		return delay;
	}

	/**
	 * @param delay
	 *            how long the event waits for the next of its kind, in milliseconds;
	 *            {@link #NO_DELAY}, or any number below 1, for not at all
	 * @return {@code this}
	 */
	public T setDelay(int delay)
	{
		this.delay = delay < 1 ? NO_DELAY : delay;
		return self();
	}

	/**
	 * @param delay
	 *            how long the event waits for the next of its kind; {@code null}, zero or less for
	 *            not at all
	 * @return {@code this}
	 */
	public T setDelay(Duration delay)
	{
		return setDelay(delay == null ? NO_DELAY : (int)Math.min(delay.toMillis(),
			Integer.MAX_VALUE));
	}
}
