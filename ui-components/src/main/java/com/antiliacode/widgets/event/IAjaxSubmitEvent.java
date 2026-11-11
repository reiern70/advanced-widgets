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
import org.apache.wicket.markup.html.form.Form;

import com.github.openjson.JSONObject;

/**
 * An event submitting the form the component is in, via Ajax: when its handler calls
 * {@code send(values)}, the browser sends the form with the values, the form validates its fields
 * and gives their models what the user entered, as when a button submits it, then the server calls
 * {@link #onEvent(JSONObject, Form, AjaxRequestTarget)}, or
 * {@link #onError(JSONObject, Form, AjaxRequestTarget)} when a field is not valid. By default the
 * handler is {@link #SEND_HANDLER}, sending what the event told, the data of its {@code detail}.
 * <p>
 * The component needs a form around it. The values come from the browser: the server checks them
 * before it trusts them.
 *
 * @see AjaxSubmitEvent
 * @since 11.0.0
 */
public interface IAjaxSubmitEvent extends IClientEvent
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
	 * Called when the browser sent the form with the event, once the models of its fields got what
	 * the user entered.
	 *
	 * @param values
	 *            what the browser sent with the form, an empty object for nothing
	 * @param form
	 *            the form submitted
	 * @param target
	 *            the Ajax request target
	 */
	void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target);

	/**
	 * Called when the browser sent the form with the event, but a field of it is not valid. Does
	 * nothing by default.
	 *
	 * @param values
	 *            what the browser sent with the form, an empty object for nothing
	 * @param form
	 *            the form submitted
	 * @param target
	 *            the Ajax request target
	 */
	default void onError(JSONObject values, Form<?> form, AjaxRequestTarget target)
	{
	}
}
