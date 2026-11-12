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
 * An event the server hears of via Ajax, without the form: when its handler calls
 * {@code send(values)}, the browser sends the values, and the server calls
 * {@link #onEvent(JSONObject, AjaxRequestTarget)}. By default the handler is
 * {@link #SEND_HANDLER}, sending what the event told, the data of its {@code detail}: the size the
 * user resized a rich text editor to, for example, as
 * {@code {"size": {"width": 640, "height": 320}}}.
 * <p>
 * The values come from the browser: the server checks them before it trusts them.
 *
 * @see AjaxEvent
 * @since 11.0.0
 */
public interface IAjaxEvent extends IAjaxRoundTripEvent
{
	/**
	 * Called when the browser sent the event.
	 *
	 * @param values
	 *            what the browser sent, an empty object for nothing
	 * @param target
	 *            the Ajax request target
	 */
	void onEvent(JSONObject values, AjaxRequestTarget target);
}
