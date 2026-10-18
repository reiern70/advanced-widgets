/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.scrollbar;

import java.util.Locale;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
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
 * bars are hidden and slim bars in the colors of the {@link com.antilia.widgets.theme.Theme
 * theme} are drawn over its edges. The component keeps scrolling natively, with the wheel, the
 * keys and touch; the thumb can be dragged, and a click on the track pages towards the click.
 * <p>
 * The component needs a size and an {@code overflow} of {@code auto} or {@code scroll}. In the
 * browser, {@code Wicket.Scrollbar} from {@link #JS} puts its element into a
 * {@code div.antilia-scrollbar} holding the bars, so they do not scroll with the content; the
 * custom property {@code --antilia-scrollbar-size} of that div sets their thickness. Scripts give
 * any element the bars with {@code Wicket.Scrollbar.attach(element, {axis})}, as a
 * BaseDynamicDataTable does for its body; such elements only need the header items of
 * {@link #renderHeadItems(IHeaderResponse)}.
 * <p>
 * During an Ajax request the behavior scrolls its component: {@link #scrollToTop},
 * {@link #scrollToBottom}, {@link #scrollTo(IPartialPageRequestHandler, int, int) scrollTo} a
 * position and {@link #scrollToElement(IPartialPageRequestHandler, String) scrollToElement}, also
 * when the request renders the component again. They scroll smoothly if the behavior is
 * {@link #setSmooth(boolean) smooth}.
 *
 * @since 11.0.0
 */
public class ScrollbarBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script drawing the scroll bars, {@code Wicket.Scrollbar}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		ScrollbarBehavior.class, "antilia-scrollbar.js");

	/** The style sheet of the scroll bars. */
	public static final ResourceReference CSS = new CssResourceReference(ScrollbarBehavior.class,
		"antilia-scrollbar.css");

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

	private boolean smooth;

	private String header;

	private Component component;

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

	/**
	 * @return whether the scroll methods scroll smoothly rather than at once
	 */
	public boolean isSmooth()
	{
		return smooth;
	}

	/**
	 * @param smooth
	 *            whether the scroll methods scroll smoothly rather than at once
	 * @return {@code this}
	 */
	public ScrollbarBehavior setSmooth(boolean smooth)
	{
		this.smooth = smooth;
		return this;
	}

	/**
	 * @return the CSS selector of the header staying at the top of the component, {@code null}
	 *         for none, as by default
	 */
	public String getHeader()
	{
		return header;
	}

	/**
	 * Lets the vertical scroll bar run below an element at the top of the component that stays in
	 * place while the rest scrolls, such as the sticky head of a table.
	 *
	 * @param header
	 *            the CSS selector of that element, relative to the component, e.g.
	 *            {@code ":scope > table > thead"}; {@code null} for none
	 * @return {@code this}
	 */
	public ScrollbarBehavior setHeader(String header)
	{
		this.header = header;
		return this;
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
		this.component = component;
	}

	/**
	 * Scrolls the component to its start.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void scrollToTop(IPartialPageRequestHandler target)
	{
		scroll(target, "scrollToTop(" + smooth + ")");
	}

	/**
	 * Scrolls the component to its end.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void scrollToBottom(IPartialPageRequestHandler target)
	{
		scroll(target, "scrollToBottom(" + smooth + ")");
	}

	/**
	 * Scrolls the component vertically to a position.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param top
	 *            the pixels from the top
	 */
	public void scrollTo(IPartialPageRequestHandler target, int top)
	{
		scroll(target, "scrollTo(" + new JSONObject().put("top", top) + ", " + smooth + ")");
	}

	/**
	 * Scrolls the component to a position.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param top
	 *            the pixels from the top
	 * @param left
	 *            the pixels from the left
	 */
	public void scrollTo(IPartialPageRequestHandler target, int top, int left)
	{
		scroll(target, "scrollTo(" + new JSONObject().put("top", top).put("left", left) + ", " +
			smooth + ")");
	}

	/**
	 * Scrolls the component so that an element inside it starts at its top.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param selector
	 *            the CSS selector of the element, relative to the component, e.g.
	 *            {@code ":scope > p:nth-child(3)"}
	 */
	public void scrollToElement(IPartialPageRequestHandler target, String selector)
	{
		Args.notEmpty(selector, "selector");
		scroll(target, "scrollToElement(" + JSONObject.quote(selector) + ", " + smooth + ")");
	}

	/**
	 * Scrolls the component so that a component inside it starts at its top.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 * @param child
	 *            the component inside, which renders its markup id
	 */
	public void scrollToElement(IPartialPageRequestHandler target, Component child)
	{
		Args.notNull(child, "child");
		scrollToElement(target, "#" + child.getMarkupId());
	}

	private void scroll(IPartialPageRequestHandler target, String call)
	{
		Args.notNull(target, "target");
		if (component == null)
		{
			throw new IllegalStateException("The behavior is not bound to a component");
		}
		target.appendJavaScript("Wicket.Scrollbar.of(" +
			JSONObject.quote(component.getMarkupId()) + ")?." + call + ";");
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
		if (header != null)
		{
			options.put("header", header);
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
