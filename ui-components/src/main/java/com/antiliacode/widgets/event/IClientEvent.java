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

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.io.IClusterable;

import com.github.openjson.JSONObject;

/**
 * An event a component listens to in the browser, by its {@link #getEventName() name}: one the
 * browser fires, {@code input} for example, or one a script of the component fires,
 * {@code antilia-rich-text-editor:resize} for example. A {@link ClientEventsBehavior} listens to
 * the events of its component, as many as it needs for each name.
 * <p>
 * The event goes to the browser as {@link #toJson() JSON}, its properties, and on each event the
 * browser runs the handler of the event, a JavaScript function taking
 * {@code (event, component, options, send)}:
 * <ul>
 * <li>{@code event}, the event of the browser, its {@code detail} what a script told with it;</li>
 * <li>{@code component}, what the component is in the browser, the editor of a rich text editor
 * for example, its text by {@code getText()}, else its element;</li>
 * <li>{@code options}, the JSON of the event, by which the properties of an event of its own
 * configure its handler;</li>
 * <li>{@code send(values, attributes)}, which tells the server the values, an object, for the
 * events going to the server, the {@link IAjaxRoundTripEvent}s, by a round trip of Wicket's Ajax
 * with the attributes of their requests, and does nothing for the others. A component may send
 * more with them: an editor sends its state, and its text when the user changed it.
 * {@code attributes}, if given, are attributes of Wicket's Ajax adding to those of the event:
 * listeners of the call, {@code sh} and {@code fh} on success and on failure for example, extra
 * parameters, {@code ep}, or another method, {@code m}.</li>
 * </ul>
 * A handler takes what it needs of them, the first ones: one staying in the browser needs no
 * {@code send}.
 * The handler is either {@link #getEventHandlerClientSideCode() code of the event's own} or the
 * {@link #getEventClientHandlerName() name of a function} of the page, which a script of the page
 * defines, the browser finding it when the event comes: an event needs one of the two, and the
 * code runs when it has both. {@link #NO_HANDLER} is built in, doing nothing; the events going to
 * the server have two more, {@link IAjaxRoundTripEvent#SEND_HANDLER} and
 * {@link IAjaxRoundTripEvent#SEND_STATE_HANDLER}.
 * <p>
 * An event of this interface alone stays in the browser: it keeps the size of an editor in a
 * cookie, for example. It runs its handler at once; an event telling a {@code delay} in its JSON,
 * as the {@link AbstractDelayedClientEvent}s do, waits for the next of its kind first.
 * <p>
 * The name of the event and the name of the function go to the browser as strings, quoted; the
 * code, as code: the page writes it, never from what a user entered. What the browser sends back,
 * the values of the Ajax events, is the user's: the server checks it before it trusts it, and
 * escapes it where it shows it, as Wicket's labels do.
 *
 * <pre>
 * editor.addEvent(new ClientEvent(RichTextEditorEvents.RESIZE,
 * 	"function(event) { document.cookie = 'size=' + JSON.stringify(event.detail.size); }"));
 * editor.addEvent(ClientEvent.handledBy(RichTextEditorEvents.MODIFIED, "Articles.showModified"));
 * </pre>
 *
 * @see ClientEvent
 * @since 11.0.0
 */
public interface IClientEvent extends IClusterable
{
	/**
	 * The built-in function doing nothing: for an event of a name to do nothing, in place of what
	 * the component does on it by default, the state an editor sends on blur for example.
	 */
	String NO_HANDLER = "Wicket.ClientEvents.nothing";

	/**
	 * @return the name of the event in the browser, which the component listens to
	 */
	String getEventName();

	/**
	 * The name goes into the page as a string, the browser finding the function by it when the
	 * event comes: a script of the page defines it, before or after the component renders.
	 *
	 * @return the name of the JavaScript function the browser runs on the event, a global or a
	 *         path to one, {@code Articles.showModified} for example, taking
	 *         {@code (event, component, options, send)}: the event of the browser, what the
	 *         component is in the browser, the JSON of the event, and
	 *         {@code send(values, attributes)}, sending the values to the server, as
	 *         {@link IClientEvent} tells;
	 *         {@code this} is the element of the component. {@code null}, as by default, for none
	 */
	default String getEventClientHandlerName()
	{
		return null;
	}

	/**
	 * The code goes into the page as it is: it is the page's own, never made of what a user
	 * entered, which would run in the browsers of others.
	 *
	 * @return the JavaScript function the browser runs on the event,
	 *         {@code function(event, component, options, send) {...}}, in place of the one
	 *         {@link #getEventClientHandlerName() named}, taking what it takes;
	 *         {@code null}, as by default, for none
	 */
	default String getEventHandlerClientSideCode()
	{
		return null;
	}

	/**
	 * What the event tells the browser, which the handler gets as its {@code options}: by default
	 * the properties of the event, by its public getters, {@code getX()} and {@code isX()} giving
	 * {@code x}, without those giving {@code null}. So the browser gets {@code eventName},
	 * {@code eventClientHandlerName}, {@code eventHandlerClientSideCode}, the code as code, and what
	 * an event adds, the {@code delay} of an {@link AbstractDelayedClientEvent} for example. An enum
	 * goes as its name, a value that is no number, text, boolean, collection, map or array as its
	 * {@code toString()}: an event leaves out what the browser should not see by overriding this.
	 * <p>
	 * The {@link ClientEventsBehavior} adds {@code eventName} and {@code index}, which no event
	 * changes, and, for an {@link IAjaxRoundTripEvent}, the attributes of its requests as
	 * {@code ajax}, which no event gives. An event overriding this gives other properties, or
	 * fewer: Wicket's {@link org.apache.wicket.ajax.json.JSONFunction} writes a value as code. The
	 * code goes into the page as it is: the page's own, never made of what a user entered.
	 *
	 * @return the event as JSON
	 */
	default JSONObject toJson()
	{
		return ClientEventJson.of(this);
	}

	/**
	 * Renders what the handler of the event needs in the browser, a script of its own for
	 * example. Does nothing by default.
	 *
	 * @param component
	 *            the component listening to the event
	 * @param response
	 *            the header response
	 */
	default void renderHead(Component component, IHeaderResponse response)
	{
	}
}
