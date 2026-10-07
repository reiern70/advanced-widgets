/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.scrollbar;

import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * Gives a scrolling component scroll bars that look the same in every browser: its native scroll
 * bars are hidden and slim bars in the colors of the {@link com.advanced.widgets.theme.Theme
 * theme} are drawn over its edges. The component keeps scrolling natively, with the wheel, the
 * keys and touch; the thumb can be dragged, and a click on the track pages towards the click.
 * <p>
 * The component needs a size and an {@code overflow} of {@code auto} or {@code scroll}. In the
 * browser, {@code Wicket.Scrollbar} from {@link #JS} puts its element into a
 * {@code div.wicket-scrollbar} holding the bars, so they do not scroll with the content; the
 * custom property {@code --wicket-scrollbar-size} of that div sets their thickness. Scripts give
 * any element the bars with {@code Wicket.Scrollbar.attach(element, {axis})}, as a
 * BaseDynamicDataTable does for its body; such elements only need the header items of
 * {@link #renderHeadItems(IHeaderResponse)}.
 *
 * @since 11.0.0
 */
public class ScrollbarBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script drawing the scroll bars, {@code Wicket.Scrollbar}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ScrollbarBehavior.class, "wicket-scrollbar.js");

	/** The style sheet of the scroll bars. */
	public static final ResourceReference CSS = new CssResourceReference(ScrollbarBehavior.class,
		"wicket-scrollbar.css");

	/**
	 * The directions the scroll bars are drawn for.
	 */
	public enum Axis
	{
		/** Horizontally only. */
		X,

		/** Vertically only. */
		Y,

		/** Both ways. */
		BOTH,

		/** The directions in which the CSS of the component lets it scroll. */
		AUTO
	}

	private final Axis axis;

	/**
	 * Draws the scroll bars in the directions in which the component scrolls.
	 */
	public ScrollbarBehavior()
	{
		this(Axis.AUTO);
	}

	/**
	 * @param axis
	 *            the directions the scroll bars are drawn for
	 */
	public ScrollbarBehavior(Axis axis)
	{
		this.axis = Args.notNull(axis, "axis");
	}

	/**
	 * @return the directions the scroll bars are drawn for
	 */
	public Axis getAxis()
	{
		return axis;
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		JSONObject options = new JSONObject();
		if (axis != Axis.AUTO)
		{
			options.put("axis", axis.name().toLowerCase(Locale.ROOT));
		}
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Scrollbar.attach(" +
			JSONObject.quote(component.getMarkupId()) + ", " + options + ");"));
	}

	/**
	 * Renders what the scroll bars need in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contributeAjax(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
