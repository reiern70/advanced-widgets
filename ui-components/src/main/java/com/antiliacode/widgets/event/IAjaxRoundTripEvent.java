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

import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;

/**
 * An event going to the server by a round trip of Wicket's Ajax: when its handler calls
 * {@code send(values)}, the browser sends a request of Wicket's Ajax with the values; and
 * {@code send(values, attributes)} adds attributes of the handler's own to those of the event,
 * listeners of the call for example, {@code sh} on success and {@code fh} on failure. The request is
 * Wicket's own: its attributes, which the server renders with the event, tell the URL, that of the
 * Ajax behavior of the event's own on the component, the method, GET or POST, the form it submits,
 * if any, its extra parameters and its dynamic extra parameters, functions the browser calls as it
 * sends the request, and the listeners of the call, handling its success and its failure among
 * them. The browser knows nothing else of what the event does on the
 * server: an event staying in the browser is one that is not an {@code IAjaxRoundTripEvent}.
 * <p>
 * What the server does with it is one of three ways: {@link IAjaxEvent}, alone,
 * {@link IAjaxUpdateEvent}, updating the model of its component, or {@link IAjaxSubmitEvent},
 * submitting the form; an event is one of them.
 *
 * @see AbstractAjaxRoundTripEvent
 * @since 11.0.0
 */
public interface IAjaxRoundTripEvent extends IClientEvent
{
	/**
	 * The built-in function sending the data of what the event told, its {@code detail}: the
	 * handler of the events going to the server, unless they are given another. As every handler,
	 * it takes {@code (event, component, options, send)}; it calls {@code send} with the data of
	 * {@code event.detail}, leaving out what is no data, an editor for example.
	 */
	String SEND_HANDLER = "Wicket.ClientEvents.send";

	/**
	 * The built-in function sending the data of what the event told when the component has
	 * something the server was not sent yet: the state of an editor, or its text, changed since
	 * the editor last sent them, as {@code component.hasUnsentState()} tells. For a component
	 * without a state, it sends always. The editors send their state by it, by default. It takes
	 * {@code (event, component, options, send)}, as every handler.
	 */
	String SEND_STATE_HANDLER = "Wicket.ClientEvents.sendState";

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
	 * Changes the attributes of the requests of the event, of its own alone: the event has an Ajax
	 * behavior of its own on the component, whose
	 * {@code updateAjaxAttributes(AjaxRequestAttributes)} calls this last, so that two events, of
	 * one name or not, send their requests each its own way. As in an Ajax behavior of Wicket, it
	 * changes the {@link AjaxRequestAttributes#setMethod(AjaxRequestAttributes.Method) method},
	 * POST unless the event sets GET, the {@link AjaxRequestAttributes#getExtraParameters() extra
	 * parameters}, which Wicket's Ajax puts in the {@code ep} of the attributes, where a page veil
	 * leaving some requests alone sees them, the
	 * {@link AjaxRequestAttributes#getDynamicExtraParameters() dynamic extra parameters} and the
	 * {@link AjaxRequestAttributes#getAjaxCallListeners() listeners}, on success and on failure for
	 * example. The attributes have what the requests of all the events of the component have
	 * already, those of
	 * {@link ClientEventsBehavior#updateAjaxAttributes(AjaxRequestAttributes)}: keep them. Does
	 * nothing by default.
	 * <p>
	 * The extra parameters come back from the browser as the user's: the server checks them before
	 * it trusts them. The code of the dynamic extra parameters and of the listeners goes into the
	 * page as it is: the page's own, never made of what a user entered.
	 *
	 * @param attributes
	 *            the attributes of the requests of the event
	 */
	default void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
	}
}
