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
 * An event staying in the browser, which runs its handler there: an {@link IClientEvent} made
 * ready, with code of its own or the name of a function of the page.
 *
 * <pre>
 * editor.addEvent(new ClientEvent(RichTextEditorEvents.MODIFIED,
 * 	"function(event) { document.title = (event.detail.modified ? '* ' : '') + 'Article'; }"));
 * editor.addEvent(ClientEvent.handledBy(RichTextEditorEvents.MODIFIED, "Articles.showModified"));
 * // the editor sends its state on blur no more
 * editor.setEvent(ClientEvent.nothing(RichTextEditorEvents.BLUR));
 * </pre>
 *
 * @since 11.0.0
 */
public class ClientEvent extends AbstractClientEvent<ClientEvent>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param clientSideCode
	 *            the JavaScript function the browser runs on the event,
	 *            {@code function(event, component, send, options) {...}}
	 */
	public ClientEvent(String eventName, String clientSideCode)
	{
		super(eventName, null, Args.notEmpty(clientSideCode, "clientSideCode"));
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param clientSideCode
	 *            the JavaScript function the browser runs on the event,
	 *            {@code function(event, component, send, options) {...}}
	 */
	public ClientEvent(IClientEventName eventName, String clientSideCode)
	{
		this(nameOf(eventName), clientSideCode);
	}

	/**
	 * For the constructors and the factories, which give the event its handler.
	 */
	private ClientEvent(String eventName, String clientHandlerName, String clientSideCode)
	{
		super(eventName, clientHandlerName, clientSideCode);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param clientHandlerName
	 *            the name of the JavaScript function of the page the browser runs on the event,
	 *            {@code Articles.showModified} for example, taking
	 *            {@code (event, component, send, options)}
	 * @return the event
	 */
	public static ClientEvent handledBy(String eventName, String clientHandlerName)
	{
		return new ClientEvent(eventName, Args.notEmpty(clientHandlerName, "clientHandlerName"),
			null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param clientHandlerName
	 *            the name of the JavaScript function of the page the browser runs on the event
	 * @return the event
	 */
	public static ClientEvent handledBy(IClientEventName eventName, String clientHandlerName)
	{
		return handledBy(nameOf(eventName), clientHandlerName);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @return an event doing nothing, by {@link #NO_HANDLER}: in place of the events a component
	 *         listens to by default, given it by {@code setEvent} for example
	 */
	public static ClientEvent nothing(String eventName)
	{
		return handledBy(eventName, NO_HANDLER);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @return an event doing nothing, by {@link #NO_HANDLER}
	 */
	public static ClientEvent nothing(IClientEventName eventName)
	{
		return nothing(nameOf(eventName));
	}
}
