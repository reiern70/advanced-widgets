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
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An event the server hears of via Ajax, without the form: an {@link IAjaxEvent} made ready. Its
 * handler is {@link #SEND_HANDLER} unless set otherwise.
 *
 * <pre>
 * editor.addEvent(new AjaxEvent(RichTextEditorEvents.RESIZE)
 * {
 *     public void onEvent(JSONObject values, AjaxRequestTarget target)
 *     {
 *         JSONObject size = values.optJSONObject("size");
 *         ...
 *     }
 * });
 * editor.addEvent(AjaxEvent.of(RichTextEditorEvents.FOCUS, (values, target) -&gt; ...));
 * </pre>
 *
 * @since 11.0.0
 */
public abstract class AjaxEvent extends AbstractAjaxRoundTripEvent<AjaxEvent>
	implements IAjaxEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * What the server does with an event: {@link IAjaxEvent#onEvent(JSONObject,
	 * AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IListener extends IClusterable
	{
		/**
		 * @param values
		 *            what the browser sent
		 * @param target
		 *            the Ajax request target
		 */
		void onEvent(JSONObject values, AjaxRequestTarget target);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 */
	protected AjaxEvent(String eventName)
	{
		super(eventName, SEND_HANDLER, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 */
	protected AjaxEvent(IClientEventName eventName)
	{
		this(nameOf(eventName));
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param listener
	 *            what the server does with the event
	 * @return the event
	 */
	public static AjaxEvent of(String eventName, IListener listener)
	{
		Args.notNull(listener, "listener");
		return new AjaxEvent(eventName)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(JSONObject values, AjaxRequestTarget target)
			{
				listener.onEvent(values, target);
			}
		};
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param listener
	 *            what the server does with the event
	 * @return the event
	 */
	public static AjaxEvent of(IClientEventName eventName, IListener listener)
	{
		return of(nameOf(eventName), listener);
	}
}
