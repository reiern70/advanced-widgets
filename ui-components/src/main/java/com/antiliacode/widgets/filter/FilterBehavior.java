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
 * Updates the model of a field of a filter via Ajax while the user types, such as the filter in
 * front of the navigation of a grid or above an infinite list: the request waits until the user
 * paused for {@link #getDelay()}, {@link #DEFAULT_DELAY} unless set otherwise. Every event within
 * the delay puts the request off again, as Wicket's {@link ThrottlingSettings} do with
 * {@code postponeTimerOnUpdate}, so a word typed quickly loads the rows once, not once per letter.
 * <p>
 * It hears of {@code input} by default, or of another event, {@code change} for a choice:
 *
 * <pre>
 * field.add(FilterBehavior.onFilter(target -&gt; {
 *     table.setCurrentPage(0);
 *     table.refresh(target);
 * }));
 * </pre>
 *
 * A filter the browser applies by itself, over rows it already has, needs no request and so no
 * delay.
 *
 * @since 11.0.0
 */
public abstract class FilterBehavior extends AjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	/** How long the request waits for the user to pause, unless set otherwise. */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(150);

	private Duration delay = DEFAULT_DELAY;

	/**
	 * A behavior hearing of {@code input}.
	 */
	public FilterBehavior()
	{
		this("input");
	}

	/**
	 * @param event
	 *            the event of the field the behavior hears of: {@code input} for a text,
	 *            {@code change} for a choice
	 */
	public FilterBehavior(String event)
	{
		super(event);
	}

	/**
	 * @return how long the request waits for the user to pause
	 */
	public Duration getDelay()
	{
		return delay;
	}

	/**
	 * @param delay
	 *            how long the request waits for the user to pause, {@code null} or zero for not at
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

	/**
	 * @param onFilter
	 *            called after the filter changed, its new value in the model of the field
	 * @return a behavior hearing of {@code input}
	 */
	public static FilterBehavior onFilter(SerializableConsumer<AjaxRequestTarget> onFilter)
	{
		return onFilter("input", onFilter);
	}

	/**
	 * @param event
	 *            the event of the field the behavior hears of: {@code input} for a text,
	 *            {@code change} for a choice
	 * @param onFilter
	 *            called after the filter changed, its new value in the model of the field
	 * @return a behavior hearing of the event
	 */
	public static FilterBehavior onFilter(String event,
		SerializableConsumer<AjaxRequestTarget> onFilter)
	{
		Args.notNull(onFilter, "onFilter");
		return new FilterBehavior(event)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				onFilter.accept(target);
			}
		};
	}
}
