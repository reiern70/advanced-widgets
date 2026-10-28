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

/**
 * An event a component listens to in the browser, by its {@link #getId() name}: one the browser
 * fires, {@code input} for example, or one a script of the component fires,
 * {@code antilia-rich-text-editor:resize} for example. A {@link ClientEventsBehavior} listens to
 * the events of its component.
 * <p>
 * On each event, the browser waits {@link #getDelay()} for the next of its kind, then runs the
 * {@link #getClientFunction() function of the event}, JavaScript taking
 * {@code (event, component, send)}:
 * <ul>
 * <li>{@code event}, the event of the browser, its {@code detail} what a script told with it;</li>
 * <li>{@code component}, what the component is in the browser, the editor of a rich text editor
 * for example, else its element;</li>
 * <li>{@code send(values)}, which tells the server the values, an object, for the events going to
 * the server, {@link IAjaxEvent} and {@link IAjaxSubmitEvent}, and does nothing for the others.</li>
 * </ul>
 * An event of this interface alone stays in the browser: it keeps the size of an editor in a
 * cookie, for example.
 * <p>
 * The name of the event goes to the browser as a string, quoted; the function, as code: the page
 * writes it, never from what a user entered. What the browser sends back, the values of the Ajax
 * events, is the user's: the server checks it before it trusts it, and escapes it where it shows
 * it, as Wicket's labels do.
 *
 * <pre>
 * editor.addEvent(new ClientEvent(RichTextEditorEvents.RESIZE,
 * 	"function(event) { document.cookie = 'size=' + JSON.stringify(event.detail.size); }"));
 * </pre>
 *
 * @see ClientEvent
 * @since 11.0.0
 */
public interface IClientEvent extends IClusterable
{
	/** The delay of an event that does not wait. */
	int NO_DELAY = -1;

	/**
	 * @return the name of the event in the browser, which the component listens to
	 */
	String getId();

	/**
	 * @return how long the event waits for the next of its kind, in milliseconds, each new one
	 *         postponing it until they pause; {@link #NO_DELAY}, as by default, for not at all
	 */
	default int getDelay()
	{
		return NO_DELAY;
	}

	/**
	 * The function goes into the page as it is, as code: it is the page's own, never made of
	 * what a user entered, which would run in the browsers of others.
	 *
	 * @return the JavaScript function the browser runs on the event,
	 *         {@code function(event, component, send) {...}}
	 */
	String getClientFunction();

	/**
	 * Renders what the function of the event needs in the browser, a script of its own for
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
