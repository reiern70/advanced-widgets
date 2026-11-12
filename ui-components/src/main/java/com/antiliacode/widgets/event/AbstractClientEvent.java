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
 * What the events made ready share: the name and the handler, a function named or code of the
 * event's own, which their setters change.
 *
 * @param <T>
 *            the event, which its setters return
 * @since 11.0.0
 */
public abstract class AbstractClientEvent<T extends AbstractClientEvent<T>> implements IClientEvent
{
	private static final long serialVersionUID = 1L;

	private final String eventName;

	private String clientHandlerName;

	private String clientSideCode;

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
	protected AbstractClientEvent(String eventName, String clientHandlerName,
		String clientSideCode)
	{
		this.eventName = Args.notEmpty(eventName, "eventName");
		this.clientHandlerName = clientHandlerName;
		this.clientSideCode = clientSideCode;
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @return its name in the browser
	 */
	static String nameOf(IClientEventName eventName)
	{
		return Args.notNull(eventName, "eventName").getEventName();
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
	public String getEventName()
	{
		return eventName;
	}

	@Override
	public String getEventClientHandlerName()
	{
		return clientHandlerName;
	}

	/**
	 * @param clientHandlerName
	 *            the name of the JavaScript function the browser runs on the event, a global or a
	 *            path to one, {@code Articles.showModified} for example; {@code null} for none
	 * @return {@code this}
	 */
	public T setEventClientHandlerName(String clientHandlerName)
	{
		this.clientHandlerName = clientHandlerName;
		return self();
	}

	@Override
	public String getEventHandlerClientSideCode()
	{
		return clientSideCode;
	}

	/**
	 * @param clientSideCode
	 *            the JavaScript function the browser runs on the event,
	 *            {@code function(event, component, options, send) {...}}, in place of the one
	 *            named; {@code null} for none
	 * @return {@code this}
	 */
	public T setEventHandlerClientSideCode(String clientSideCode)
	{
		this.clientSideCode = clientSideCode;
		return self();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + eventName + "]";
	}
}
