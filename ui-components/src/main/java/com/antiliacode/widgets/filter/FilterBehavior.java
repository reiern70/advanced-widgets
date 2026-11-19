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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableConsumer;

import com.antiliacode.widgets.ScriptJson;
import com.antiliacode.widgets.veil.LocalVeilBehavior;
import com.antiliacode.widgets.veil.PageVeilBehavior;

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
 * <p>
 * The filter's requests leave a page veil alone ({@link PageVeilBehavior#noVeil}), so that the
 * page stays usable while the user types; {@link #setVeilPage(boolean)} veils it again. What the
 * filter fills, though, should not be clicked while its rows are stale: the components given to
 * {@link #veil(Component...)} are veiled while the filter's request runs, by their
 * {@link LocalVeilBehavior}, which they get if they have none. The field itself stays usable.
 *
 * <pre>
 * field.add(FilterBehavior.onFilter(target -&gt; target.add(table)).veil(table, deleteSelected));
 * // a filter in a toolbar of a data table: the rows, the toolbar and the field left usable
 * field.add(FilterBehavior.onFilter(target -&gt; table.refresh(target))
 * 	.veil(table.getRowsBody()));
 * </pre>
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

	private boolean veilPage;

	private final List<Component> veiled = new ArrayList<>();

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

	/**
	 * @return whether the filter's requests veil the page, as a page veil veils the other requests:
	 *         not unless set
	 */
	public boolean isVeilPage()
	{
		return veilPage;
	}

	/**
	 * @param veilPage
	 *            whether the filter's requests veil the page: by default they leave it alone, so
	 *            that the page stays usable while the user types
	 * @return {@code this}
	 */
	public FilterBehavior setVeilPage(boolean veilPage)
	{
		this.veilPage = veilPage;
		return this;
	}

	/**
	 * Veils the components while the filter's request runs: the table or list it fills, and the
	 * components acting on its rows, such as the buttons acting on the table's selection. Each
	 * gets a {@link LocalVeilBehavior} unless it has one, as a data table and its rows have. None
	 * may hold the field itself, which its veil would cover: a filter in a toolbar of a data table
	 * veils the table's rows, {@code table.getRowsBody()}, not the table.
	 *
	 * @param components
	 *            the components to veil, on the same page as the filter
	 * @return {@code this}
	 */
	public FilterBehavior veil(Component... components)
	{
		for (Component component : Args.notNull(components, "components"))
		{
			veiled.add(Args.notNull(component, "component"));
			component.setOutputMarkupId(true);
		}
		return this;
	}

	/**
	 * @return the components veiled while the filter's request runs
	 */
	public List<Component> getVeiled()
	{
		return Collections.unmodifiableList(veiled);
	}

	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);
		for (Component each : veiled)
		{
			if (each instanceof MarkupContainer container && container.contains(component, true))
			{
				throw new IllegalStateException("The filter " + component.getPageRelativePath() +
					" is inside " + each.getPageRelativePath() + ", whose veil would cover it: " +
					"veil what it fills inside, such as the rows of a table, table.getRowsBody()");
			}
			if (each.getBehaviors(LocalVeilBehavior.class).isEmpty())
			{
				each.add(new LocalVeilBehavior());
			}
		}
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
		if (!veilPage)
		{
			PageVeilBehavior.noVeil(attributes);
		}
		if (!veiled.isEmpty())
		{
			// raised as the request leaves, lowered once it is done, whatever its answer
			attributes.getAjaxCallListeners()
				.add(new AjaxCallListener().onBeforeSend(veilScript("show"))
					.onDone(veilScript("hide")));
		}
	}

	/**
	 * @param function
	 *            {@code show} or {@code hide}
	 * @return the script raising or lowering the local veils of the components veiled, by
	 *         {@code Wicket.Veil} of wicket-extensions; nothing on a page without it
	 */
	private CharSequence veilScript(String function)
	{
		StringBuilder script = new StringBuilder("if (Wicket.Veil) {");
		for (Component each : veiled)
		{
			script.append("Wicket.Veil.")
				.append(function)
				.append('(')
				.append(ScriptJson.quote(each.getMarkupId()))
				.append(");");
		}
		return script.append('}');
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
