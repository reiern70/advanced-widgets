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
package com.antiliacode.widgets.filter;

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

/**
 * Makes a field of a filter, above a grid or a list whose rows come from the server, ask for the
 * rows again once the user paused: on {@code input} by default, or on another event
 * ({@code change} for a choice). Each event within {@link #getDelay()} puts the request off again,
 * so a word typed quickly costs one request, not one per letter.
 *
 * <pre>
 * field.add(FilterBehavior.onFilter(target -&gt; target.add(table)));
 * choice.add(FilterBehavior.onFilter("change", target -&gt; target.add(table)));
 * </pre>
 *
 * A filter that works in the browser only, over rows it already has, has no reason to wait.
 *
 * @since 11.0.0
 */
public abstract class FilterBehavior extends AjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	/** How long the filter waits for the user to pause, unless set otherwise. */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(150);

	/** The event a filter listens to, unless told otherwise. */
	public static final String DEFAULT_EVENT = "input";

	private Duration delay = DEFAULT_DELAY;

	/**
	 * A filter on {@link #DEFAULT_EVENT}, for a text.
	 */
	public FilterBehavior()
	{
		this(DEFAULT_EVENT);
	}

	/**
	 * @param event
	 *            the event of the field that filters, {@code change} for a choice
	 */
	public FilterBehavior(String event)
	{
		super(event);
	}

	/**
	 * @return how long the filter waits for the user to pause before it asks the server
	 */
	public Duration getDelay()
	{
		return delay;
	}

	/**
	 * @param delay
	 *            how long the filter waits for the user to pause, {@code null} or zero for not at
	 *            all
	 * @return {@code this}
	 */
	public FilterBehavior setDelay(Duration delay)
	{
		this.delay = delay;
		return this;
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
	{
		super.updateAjaxAttributes(attributes);
		if (delay != null && !delay.isZero() && !delay.isNegative())
		{
			// each new event postpones the request, until the user paused
			attributes.setThrottlingSettings(new ThrottlingSettings(delay, true));
		}
	}

	@Override
	protected void onUpdate(AjaxRequestTarget target)
	{
		onFilter(target);
	}

	/**
	 * Called once the filter changed and the user paused: the field's model holds the new value.
	 *
	 * @param target
	 *            the target of the request
	 */
	protected abstract void onFilter(AjaxRequestTarget target);

	/**
	 * @param onFilter
	 *            what to do once the user paused typing
	 * @return a filter on {@link #DEFAULT_EVENT}
	 */
	public static FilterBehavior onFilter(SerializableConsumer<AjaxRequestTarget> onFilter)
	{
		return onFilter(DEFAULT_EVENT, onFilter);
	}

	/**
	 * @param event
	 *            the event of the field that filters, {@code change} for a choice
	 * @param onFilter
	 *            what to do once the user paused
	 * @return a filter on the event
	 */
	public static FilterBehavior onFilter(String event,
		SerializableConsumer<AjaxRequestTarget> onFilter)
	{
		Args.notNull(onFilter, "onFilter");
		return new FilterBehavior(event)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onFilter(AjaxRequestTarget target)
			{
				onFilter.accept(target);
			}
		};
	}
}
