/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.render.WebPageRenderer;

import com.antilia.widgets.demo.error.ErrorPage;

/**
 * Renders some pages in the response instead of redirecting to their address in the session
 * (<code>/sign-in?0</code>):
 * <ul>
 * <li>any page asked for with a GET without the cookie of a session. The session is never in the
 * URL (see <code>web.xml</code>), so a client keeping no cookies, such as a robot or a monitor,
 * came back without the session each time and was sent between <code>/sign-in</code> and
 * <code>/sign-in?0</code> for good. A browser gets its first page so too, with the cookie, and is
 * redirected as before from then on;</li>
 * <li>an {@link ErrorPage}, which so keeps the status of what went wrong, such as the 404 of an
 * address without a page, rather than a redirect answered with 200.</li>
 * </ul>
 */
public class DirectPageRenderer extends WebPageRenderer
{
	/**
	 * @param handler
	 *            the handler rendering the page
	 */
	public DirectPageRenderer(RenderPageRequestHandler handler)
	{
		super(handler);
	}

	@Override
	protected boolean isOnePassRender()
	{
		return super.isOnePassRender() || isWithoutSessionCookie(RequestCycle.get()) ||
			ErrorPage.class.isAssignableFrom(getPageProvider().getPageClass());
	}

	private static boolean isWithoutSessionCookie(RequestCycle cycle)
	{
		return cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request &&
			"GET".equals(request.getMethod()) && request.isRequestedSessionIdFromCookie() == false;
	}
}
