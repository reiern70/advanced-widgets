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
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An event submitting the form the component is in, via Ajax: an {@link IAjaxSubmitEvent} made
 * ready. Its handler is {@link #SEND_HANDLER} unless set otherwise.
 *
 * <pre>
 * editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE)
 * {
 *     public void onEvent(JSONObject values, Form&lt;?&gt; form, AjaxRequestTarget target)
 *     {
 *         articles.save(editor.getModelObject());
 *         editor.markSaved(target);
 *     }
 * });
 * editor.addEvent(AjaxSubmitEvent.of(RichTextEditorEvents.INPUT, (values, form, target) -&gt;
 * 	target.add(preview)).setDelay(500));
 * </pre>
 *
 * @since 11.0.0
 */
public abstract class AjaxSubmitEvent extends AbstractDelayedClientEvent<AjaxSubmitEvent>
	implements IAjaxSubmitEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * What the server does with an event once the form took what the user entered:
	 * {@link IAjaxSubmitEvent#onEvent(JSONObject, Form, AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IListener extends IClusterable
	{
		/**
		 * @param values
		 *            what the browser sent with the form
		 * @param form
		 *            the form submitted
		 * @param target
		 *            the Ajax request target
		 */
		void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target);
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 */
	protected AjaxSubmitEvent(String eventName)
	{
		super(eventName, SEND_HANDLER, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 */
	protected AjaxSubmitEvent(IClientEventName eventName)
	{
		this(nameOf(eventName));
	}

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 * @param listener
	 *            what the server does with the event once the form took what the user entered;
	 *            nothing when a field is not valid
	 * @return the event
	 */
	public static AjaxSubmitEvent of(String eventName, IListener listener)
	{
		Args.notNull(listener, "listener");
		return new AjaxSubmitEvent(eventName)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target)
			{
				listener.onEvent(values, form, target);
			}
		};
	}

	/**
	 * @param eventName
	 *            the name of the event
	 * @param listener
	 *            what the server does with the event once the form took what the user entered;
	 *            nothing when a field is not valid
	 * @return the event
	 */
	public static AjaxSubmitEvent of(IClientEventName eventName, IListener listener)
	{
		return of(nameOf(eventName), listener);
	}
}
