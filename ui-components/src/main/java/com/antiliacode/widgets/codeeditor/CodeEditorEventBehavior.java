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
package com.antiliacode.widgets.codeeditor;

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableBiConsumer;
import org.danekja.java.util.function.serializable.SerializableConsumer;

/**
 * Hears on the server of what a {@link CodeEditor} tells, a {@link CodeEditorEvent}: the focus
 * lost, Ctrl+S pressed and so on. As the behaviors updating form components via Ajax do, it sends
 * the text with the event, which the editor validates and gives its model, then calls
 * {@link #onUpdate(org.apache.wicket.ajax.AjaxRequestTarget)}, or
 * {@link #onError(org.apache.wicket.ajax.AjaxRequestTarget, RuntimeException)} when the text is not
 * valid. For the events that come often, {@link CodeEditorEvent#INPUT} and
 * {@link CodeEditorEvent#SELECT}, it waits until the user paused for {@link #getDelay()}.
 * <p>
 * Several of them listen to an editor as the page needs, each to its event:
 *
 * <pre>
 * editor.add(new CodeEditorEventBehavior(CodeEditorEvent.SAVE)
 * {
 *     protected void onUpdate(AjaxRequestTarget target)
 *     {
 *         documents.save(editor.getModelObject());
 *         editor.markSaved(target);
 *     }
 * });
 * </pre>
 *
 * or, with lambdas, {@link #onUpdate(CodeEditorEvent, SerializableConsumer)}:
 *
 * <pre>
 * editor.add(CodeEditorEventBehavior.onUpdate(CodeEditorEvent.BLUR, target -&gt; target.add(status)));
 * </pre>
 *
 * @since 11.0.0
 */
public abstract class CodeEditorEventBehavior extends AjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	/** How long a frequent event waits for the user to pause, unless set otherwise. */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(500);

	private final CodeEditorEvent event;

	private Duration delay = DEFAULT_DELAY;

	/**
	 * @param event
	 *            the event of the editor the behavior hears of
	 */
	public CodeEditorEventBehavior(CodeEditorEvent event)
	{
		super(Args.notNull(event, "event").getEventName());
		this.event = event;
	}

	/**
	 * @return the event of the editor the behavior hears of
	 */
	public CodeEditorEvent getCodeEditorEvent()
	{
		return event;
	}

	/**
	 * @return how long a frequent event waits for the user to pause before it goes to the server
	 */
	public Duration getDelay()
	{
		return delay;
	}

	/**
	 * @param delay
	 *            how long a frequent event waits for the user to pause, {@code null} or zero for
	 *            not at all; the others go at once
	 * @return {@code this}
	 */
	public CodeEditorEventBehavior setDelay(Duration delay)
	{
		this.delay = delay;
		return this;
	}

	/**
	 * A behavior hearing of an event of the editor, calling a lambda once the text is valid and in
	 * the model.
	 *
	 * @param event
	 *            the event of the editor
	 * @param onUpdate
	 *            what to do then
	 * @return the behavior
	 */
	public static CodeEditorEventBehavior onUpdate(CodeEditorEvent event,
		SerializableConsumer<AjaxRequestTarget> onUpdate)
	{
		return onUpdate(event, onUpdate, null);
	}

	/**
	 * A behavior hearing of an event of the editor, calling a lambda once the text is valid and in
	 * the model, and another when it is not valid; the editor shows its errors either way.
	 *
	 * @param event
	 *            the event of the editor
	 * @param onUpdate
	 *            what to do when the text is valid
	 * @param onError
	 *            what to do when it is not, given the exception, if one was thrown; {@code null}
	 *            for nothing
	 * @return the behavior
	 */
	public static CodeEditorEventBehavior onUpdate(CodeEditorEvent event,
		SerializableConsumer<AjaxRequestTarget> onUpdate,
		SerializableBiConsumer<AjaxRequestTarget, RuntimeException> onError)
	{
		Args.notNull(onUpdate, "onUpdate");
		return new CodeEditorEventBehavior(event)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onUpdate.accept(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, RuntimeException e)
			{
				if (onError != null)
				{
					onError.accept(target, e);
				}
				else
				{
					super.onError(target, e);
				}
			}
		};
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);
		if (event.isFrequent() && delay != null && !delay.isZero() && !delay.isNegative())
		{
			// each new event postpones the request, until the user paused
			attributes.setThrottlingSettings(new ThrottlingSettings(delay, true));
		}
	}
}
