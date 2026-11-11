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
import org.apache.wicket.markup.html.form.FormComponent;

import com.github.openjson.JSONObject;

/**
 * An event updating the model of its component, a form component, via Ajax, without the rest of
 * the form: when its handler calls {@code send(values)}, the browser sends the values with what
 * the user entered in the component, which the component validates and its model gets, as Wicket's
 * {@link org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior} does, then the server calls
 * {@link #onEvent(JSONObject, FormComponent, AjaxRequestTarget)}, or
 * {@link #onError(JSONObject, FormComponent, AjaxRequestTarget, RuntimeException)} when what the
 * user entered is not valid. By default the handler is {@link #SEND_HANDLER}, sending what the
 * event told, the data of its {@code detail}.
 * <p>
 * The component is a {@link FormComponent}: a code editor saving its text on Ctrl+S, for example.
 * The values come from the browser: the server checks them before it trusts them.
 *
 * @see AjaxUpdateEvent
 * @since 11.0.0
 */
public interface IAjaxUpdateEvent extends IClientEvent
{
	/**
	 * @return the name of the JavaScript function the browser runs on the event,
	 *         {@link #SEND_HANDLER} by default
	 */
	@Override
	default String getEventClientHandlerName()
	{
		return SEND_HANDLER;
	}

	/**
	 * Called when the browser sent the event, once the model of the component got what the user
	 * entered.
	 *
	 * @param values
	 *            what the browser sent, an empty object for nothing
	 * @param component
	 *            the component updated
	 * @param target
	 *            the Ajax request target
	 */
	void onEvent(JSONObject values, FormComponent<?> component, AjaxRequestTarget target);

	/**
	 * Called when the browser sent the event, but what the user entered is not valid, or when
	 * updating the model threw. Throws the exception again by default, if there is one.
	 *
	 * @param values
	 *            what the browser sent, an empty object for nothing
	 * @param component
	 *            the component not updated
	 * @param target
	 *            the Ajax request target
	 * @param e
	 *            what was thrown, {@code null} when the input is not valid
	 */
	default void onError(JSONObject values, FormComponent<?> component, AjaxRequestTarget target,
		RuntimeException e)
	{
		if (e != null)
		{
			throw e;
		}
	}
}
