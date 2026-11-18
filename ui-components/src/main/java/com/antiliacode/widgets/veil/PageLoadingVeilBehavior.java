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
package com.antiliacode.widgets.veil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.IWrappedHeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * Veils a page while it loads, so that a click reaches none of its components before their scripts
 * have run: an Ajax link without its handler, a drop-down not built yet. Wicket's veil covers the
 * Ajax requests only, not that moment between the first paint and the end of the page's scripts.
 * <p>
 * A page has no tag of its own: the behavior goes on the component of the page's {@code <body>}
 * (or {@code <html>}), a {@link org.apache.wicket.markup.html.TransparentWebMarkupContainer} for
 * example, in a base page:
 *
 * <pre>
 * &lt;body wicket:id="body"&gt;
 *
 * add(new TransparentWebMarkupContainer("body").add(new PageLoadingVeilBehavior()));
 * </pre>
 *
 * The tag gets the class {@value #CSS_CLASS}, and the page the style sheet {@link #CSS}, which the
 * browser loads before it paints: from the first paint a transparent veil covers the window, with
 * the wait cursor, as Wicket's Ajax veil does, and a spinner shows only once the page takes longer
 * than a delay, 300 milliseconds. The script {@link #JS}, loaded deferred so that it holds up
 * neither the parsing nor the first paint, takes the class away once Wicket's domready handlers,
 * in which the components are initialized, have run. All of it holds under the strict Content
 * Security Policy: no inline script and no inline style, the spinner's delay and the timeout below
 * being CSS animations.
 * <p>
 * Should the script never take the class away, a script having failed or still loading, the veil
 * goes away by itself after a timeout, 10 seconds: the page is never locked for good. A browser
 * that runs no script reads the style sheet {@link #NOSCRIPT_CSS}, linked in a
 * {@code <noscript>}, and has no veil at all. A page's own style sheet changes the delay and the
 * timeout with the custom properties {@code --antilia-page-loading-delay} and
 * {@code --antilia-page-loading-timeout}, on {@code :root} for example.
 * <p>
 * The veil takes the mouse, not the keyboard: the content is not made {@code inert}. The tag could
 * carry {@code inert} from the first paint, but only a script takes it away, and no CSS timeout
 * does: a script that failed would leave the page dead for good. While inert, the content can take
 * no focus either, so the focus the components' own initialization gives, or an
 * {@code autofocus}, would be lost, and assistive technologies would find the page empty. Reaching
 * a control with the keyboard and pressing a key in the moment before the scripts have run is
 * rare, and does what the control does without its script, as its {@code href} or the submit of
 * its form.
 * <p>
 * The veil is for the page as the browser loads it: an Ajax update rendering the component again
 * adds neither the class nor the resources.
 */
public class PageLoadingVeilBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The class veiling the element carrying it until the page's scripts have run. */
	public static final String CSS_CLASS = "antilia-page-loading";

	/** The style sheet drawing the veil over the element with {@value #CSS_CLASS}. */
	public static final ResourceReference CSS = new CssResourceReference(
		PageLoadingVeilBehavior.class, "antilia-page-loading-veil.css");

	/** The style sheet taking the veil away in a browser that runs no script. */
	public static final ResourceReference NOSCRIPT_CSS = new CssResourceReference(
		PageLoadingVeilBehavior.class, "antilia-page-loading-veil-noscript.css");

	/** The script lifting the veil once the page's scripts have run. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		PageLoadingVeilBehavior.class, "antilia-page-loading-veil.js");

	/**
	 * @throws IllegalArgumentException
	 *             if the component is a page, which has no tag of its own
	 */
	@Override
	public void bind(Component component)
	{
		super.bind(component);
		if (component instanceof Page)
		{
			throw new IllegalArgumentException(PageLoadingVeilBehavior.class.getSimpleName() +
				" goes on the component of the page's <body> or <html>, a page has no tag of its" +
				" own: " + component.getClass().getName());
		}
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		if (isPageLoading(component))
		{
			tag.append("class", CSS_CLASS, " ");
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		if (isPageLoading(component))
		{
			response.render(CssHeaderItem.forReference(CSS));
			response.render(new NoScriptHeaderItem(CssHeaderItem.forReference(NOSCRIPT_CSS)));
			response.render(JavaScriptHeaderItem.forReference(JS).setDefer(true));
		}
	}

	/**
	 * @return whether the component renders with its page, rather than in an Ajax update
	 */
	private static boolean isPageLoading(Component component)
	{
		return component.getRequestCycle().find(IPartialPageRequestHandler.class).isEmpty();
	}

	/**
	 * A header item in a {@code <noscript>}, read by a browser that runs no script only. It wraps
	 * the item, so that the item gets the nonce of the Content Security Policy as any other.
	 */
	static class NoScriptHeaderItem extends HeaderItem implements IWrappedHeaderItem
	{
		private static final long serialVersionUID = 1L;

		private final HeaderItem wrapped;

		/**
		 * @param wrapped
		 *            the item in the {@code <noscript>}
		 */
		NoScriptHeaderItem(HeaderItem wrapped)
		{
			this.wrapped = Args.notNull(wrapped, "wrapped");
		}

		@Override
		public HeaderItem getWrapped()
		{
			return wrapped;
		}

		@Override
		public HeaderItem wrap(HeaderItem item)
		{
			return new NoScriptHeaderItem(item);
		}

		/**
		 * @return the tokens of the item in the {@code <noscript>}, apart from those of the same
		 *         item outside of one
		 */
		@Override
		public Iterable<?> getRenderTokens()
		{
			List<Object> tokens = new ArrayList<>();
			wrapped.getRenderTokens().forEach(token -> tokens.add("noscript-" + token));
			return tokens;
		}

		@Override
		public void render(Response response)
		{
			response.write("<noscript>");
			wrapped.render(response);
			response.write("</noscript>\n");
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof NoScriptHeaderItem item && wrapped.equals(item.wrapped);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash("noscript", wrapped);
		}

		@Override
		public String toString()
		{
			return "NoScriptHeaderItem(" + wrapped + ")";
		}
	}
}
