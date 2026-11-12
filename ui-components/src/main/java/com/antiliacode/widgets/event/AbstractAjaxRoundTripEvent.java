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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.util.lang.Args;

/**
 * An event going to the server, which keeps what it changes of the attributes of its requests: the
 * method, its extra parameters, its dynamic extra parameters and the listeners of the call, which
 * {@link #updateAjaxAttributes(AjaxRequestAttributes)} gives them. Those of the built-in events
 * come so, as the event is made:
 *
 * <pre>
 * // the page veil leaves the request sending the state on blur alone, which says when it failed
 * editor.setEvent(new SendStateEvent(RichTextEditorEvents.BLUR).setExtraParameter("_nb", "true")
 *     .addAjaxCallListener(new AjaxCallListener().onFailure("console.warn('not sent');")));
 * // in the URL, with the time the browser sent it
 * editor.addEvent(AjaxEvent.of(RichTextEditorEvents.FOCUS, (values, target) -&gt; ...)
 *     .setMethod(AjaxRequestAttributes.Method.GET)
 *     .addDynamicExtraParameter("return {sentAt: Date.now()};"));
 * </pre>
 *
 * None of these go to the browser in the {@link #toJson() JSON} of the event: they are in the
 * attributes of its requests.
 *
 * @param <T>
 *            the event, which its setters return
 * @since 11.0.0
 */
public abstract class AbstractAjaxRoundTripEvent<T extends AbstractAjaxRoundTripEvent<T>>
	extends AbstractDelayedClientEvent<T> implements IAjaxRoundTripEvent
{
	private static final long serialVersionUID = 1L;

	private AjaxRequestAttributes.Method method;

	private final Map<String, String> extraParameters = new LinkedHashMap<>();

	private final List<String> dynamicExtraParameters = new ArrayList<>();

	private final List<AjaxCallListener> ajaxCallListeners = new ArrayList<>();

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
	protected AbstractAjaxRoundTripEvent(String eventName, String clientHandlerName,
		String clientSideCode)
	{
		super(eventName, clientHandlerName, clientSideCode);
	}

	/**
	 * @return the method of the requests of the event; {@code null}, as by default, for that of
	 *         the requests of all the events of the component, POST unless it changes it
	 */
	public AjaxRequestAttributes.Method getMethod()
	{
		return method;
	}

	/**
	 * @param method
	 *            the method of the requests of the event: GET puts the values in the URL, POST in
	 *            the body, for long values; {@code null} for that of the requests of all the
	 *            events of the component
	 * @return {@code this}
	 */
	public T setMethod(AjaxRequestAttributes.Method method)
	{
		this.method = method;
		return self();
	}

	/**
	 * @return the extra parameters of the requests of the event, of its own alone, by their names,
	 *         none at first
	 */
	public Map<String, String> getExtraParameters()
	{
		return Collections.unmodifiableMap(extraParameters);
	}

	/**
	 * Adds an extra parameter to the requests of the event, of its own alone: Wicket's Ajax puts it
	 * in the {@code ep} of the attributes of the request, where a page veil leaving some requests
	 * alone sees it, and the request carries it, in a beacon too.
	 *
	 * @param name
	 *            the name of the parameter
	 * @param value
	 *            its value; {@code null} to take the parameter away
	 * @return {@code this}
	 */
	public T setExtraParameter(String name, String value)
	{
		Args.notEmpty(name, "name");
		if (value == null)
		{
			extraParameters.remove(name);
		}
		else
		{
			extraParameters.put(name, value);
		}
		return self();
	}

	/**
	 * @return the bodies of the functions giving the dynamic extra parameters of the requests of
	 *         the event, in their order, none at first
	 */
	public List<String> getDynamicExtraParameters()
	{
		return Collections.unmodifiableList(dynamicExtraParameters);
	}

	/**
	 * Adds a dynamic extra parameter to the requests of the event: the body of a function of
	 * {@code attrs}, the attributes of the request, which the browser calls as it sends it, giving
	 * parameters by their names, or an array of {@code name} and {@code value}, as Wicket's
	 * {@link AjaxRequestAttributes#getDynamicExtraParameters()} takes them. The code goes into the
	 * page as it is: the page's own, never made of what a user entered.
	 *
	 * @param functionBody
	 *            the body of the function, {@code return {sentAt: Date.now()};} for example
	 * @return {@code this}
	 */
	public T addDynamicExtraParameter(String functionBody)
	{
		dynamicExtraParameters.add(Args.notEmpty(functionBody, "functionBody"));
		return self();
	}

	/**
	 * @return the listeners of the calls of the requests of the event, in their order, none at
	 *         first
	 */
	public List<AjaxCallListener> getAjaxCallListeners()
	{
		return Collections.unmodifiableList(ajaxCallListeners);
	}

	/**
	 * Adds a listener to the calls of the requests of the event: what the browser runs before the
	 * request goes, on its success, on its failure and once it is done, as Wicket's
	 * {@link AjaxCallListener} tells it.
	 *
	 * @param listener
	 *            the listener
	 * @return {@code this}
	 */
	public T addAjaxCallListener(AjaxCallListener listener)
	{
		ajaxCallListeners.add(Args.notNull(listener, "listener"));
		return self();
	}

	/**
	 * Gives the attributes the method, the extra parameters, the dynamic extra parameters and the
	 * listeners of the event. An event overriding this calls it.
	 */
	@Override
	public void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		if (method != null)
		{
			attributes.setMethod(method);
		}
		attributes.getExtraParameters().putAll(extraParameters);
		attributes.getDynamicExtraParameters().addAll(dynamicExtraParameters);
		attributes.getAjaxCallListeners().addAll(ajaxCallListeners);
	}
}
