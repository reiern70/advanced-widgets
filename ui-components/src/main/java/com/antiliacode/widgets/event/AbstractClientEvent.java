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

import org.apache.wicket.util.lang.Args;

/**
 * What the events made ready share: the name, the delay and the function, which their setters
 * change.
 *
 * @param <T>
 *            the event, which its setters return
 * @since 11.0.0
 */
public abstract class AbstractClientEvent<T extends AbstractClientEvent<T>> implements IClientEvent
{
	private static final long serialVersionUID = 1L;

	private final String id;

	private int delay = NO_DELAY;

	private String clientFunction;

	/**
	 * @param id
	 *            the name of the event in the browser
	 * @param clientFunction
	 *            the JavaScript function the browser runs on the event, {@code null} for none
	 */
	protected AbstractClientEvent(String id, String clientFunction)
	{
		this.id = Args.notEmpty(id, "id");
		this.clientFunction = clientFunction;
	}

	/**
	 * @return {@code this}, as the event it is
	 */
	@SuppressWarnings("unchecked")
	protected final T self()
	{
		return (T)this;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
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

	@Override
	public String getClientFunction()
	{
		return clientFunction;
	}

	/**
	 * @param clientFunction
	 *            the JavaScript function the browser runs on the event,
	 *            {@code function(event, component, send) {...}}
	 * @return {@code this}
	 */
	public T setClientFunction(String clientFunction)
	{
		this.clientFunction = clientFunction;
		return self();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
