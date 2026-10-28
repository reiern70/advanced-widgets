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

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.github.openjson.JSONObject;

/**
 * An event the server hears of via Ajax, without the form: when its
 * {@link #getClientFunction() function} calls {@code send(values)}, the browser sends the values,
 * and the server calls {@link #onEvent(String, JSONObject, AjaxRequestTarget)}. Without a
 * function, as by default, the browser sends what the event told, the data of its
 * {@code detail}: the size the user resized a rich text editor to, for example, as
 * {@code {"size": {"width": 640, "height": 320}}}.
 * <p>
 * The values come from the browser: the server checks them before it trusts them.
 *
 * @see AjaxEvent
 * @since 11.0.0
 */
public interface IAjaxEvent extends IClientEvent
{
	/**
	 * @return the JavaScript function the browser runs on the event,
	 *         {@code function(event, component, send) {...}}; {@code null}, as by default, for one
	 *         sending the data of the event's {@code detail}
	 */
	@Override
	default String getClientFunction()
	{
		return null;
	}

	/**
	 * Called when the browser sent the event.
	 *
	 * @param eventId
	 *            the name of the event, {@link #getId()}
	 * @param values
	 *            what the browser sent, an empty object for nothing
	 * @param target
	 *            the Ajax request target
	 */
	void onEvent(String eventId, JSONObject values, AjaxRequestTarget target);
}
