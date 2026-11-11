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
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An event updating the model of its form component via Ajax, without the rest of the form: an
 * {@link IAjaxUpdateEvent} made ready. Its handler is {@link #SEND_HANDLER} unless set otherwise.
 *
 * <pre>
 * editor.addEvent(new AjaxUpdateEvent(CodeEditorEvent.SAVE)
 * {
 *     public void onEvent(JSONObject values, FormComponent&lt;?&gt; component,
 *         AjaxRequestTarget target)
 *     {
 *         documents.save(editor.getModelObject());
 *         editor.markSaved(target);
 *     }
 * });
 * editor.addEvent(AjaxUpdateEvent.of(CodeEditorEvent.INPUT, (values, component, target) -&gt;
 * 	target.add(preview)).setDelay(500));
 * </pre>
 *
 * @since 11.0.0
 */
public abstract class AjaxUpdateEvent extends AbstractDelayedClientEvent<AjaxUpdateEvent>
	implements IAjaxUpdateEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * What the server does with an event once the model of the component got what the user
	 * entered: {@link IAjaxUpdateEvent#onEvent(JSONObject, FormComponent, AjaxRequestTarget)}, as
	 * a lambda.
	 */
	@FunctionalInterface
	public interface IListener extends IClusterable
	{
		/**
		 * @param values
		 *            what the browser sent
		 * @param component
		 *            the component updated
		 * @param target
		 *            the Ajax request target
		 */
		void onEvent(JSONObject values, FormComponent<?> component, AjaxRequestTarget target);
	}

	/**
	 * What the server does when what the user entered is not valid:
	 * {@link IAjaxUpdateEvent#onError(JSONObject, FormComponent, AjaxRequestTarget,
	 * RuntimeException)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IErrorListener extends IClusterable
	{
		/**
		 * @param values
		 *            what the browser sent
		 * @param component
		 *            the component not updated
		 * @param target
		 *            the Ajax request target
		 * @param e
		 *            what was thrown, {@code null} when the input is not valid
		 */
		void onError(JSONObject values, FormComponent<?> component, AjaxRequestTarget target,
			RuntimeException e);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 */
	protected AjaxUpdateEvent(String eventName)
	{
		super(eventName, SEND_HANDLER, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 */
	protected AjaxUpdateEvent(IClientEventName eventName)
	{
		this(nameOf(eventName));
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param listener
	 *            what the server does with the event once the model got what the user entered;
	 *            nothing when it is not valid
	 * @return the event
	 */
	public static AjaxUpdateEvent of(String eventName, IListener listener)
	{
		return of(eventName, listener, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param listener
	 *            what the server does with the event once the model got what the user entered;
	 *            nothing when it is not valid
	 * @return the event
	 */
	public static AjaxUpdateEvent of(IClientEventName eventName, IListener listener)
	{
		return of(nameOf(eventName), listener, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param listener
	 *            what the server does with the event once the model got what the user entered
	 * @param errorListener
	 *            what the server does when it is not valid; {@code null} for nothing
	 * @return the event
	 */
	public static AjaxUpdateEvent of(IClientEventName eventName, IListener listener,
		IErrorListener errorListener)
	{
		return of(nameOf(eventName), listener, errorListener);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param listener
	 *            what the server does with the event once the model got what the user entered
	 * @param errorListener
	 *            what the server does when it is not valid; {@code null} for nothing
	 * @return the event
	 */
	public static AjaxUpdateEvent of(String eventName, IListener listener,
		IErrorListener errorListener)
	{
		Args.notNull(listener, "listener");
		return new AjaxUpdateEvent(eventName)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(JSONObject values, FormComponent<?> component,
				AjaxRequestTarget target)
			{
				listener.onEvent(values, component, target);
			}

			@Override
			public void onError(JSONObject values, FormComponent<?> component,
				AjaxRequestTarget target, RuntimeException e)
			{
				if (errorListener != null)
				{
					errorListener.onError(values, component, target, e);
				}
				else
				{
					super.onError(values, component, target, e);
				}
			}
		};
	}
}
