/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.render.WebPageRenderer;

/**
 * Renders a page in the response to a GET without the cookie of a session, instead of redirecting
 * to the address of the page in the session (<code>/sign-in?0</code>). The session is never in
 * the URL (see <code>web.xml</code>), so a client keeping no cookies, such as a robot or a
 * monitor, came back without the session each time and was sent between <code>/sign-in</code>
 * and <code>/sign-in?0</code> for good. A browser gets its first page so too, with the cookie,
 * and is redirected as before from then on.
 */
public class CookielessPageRenderer extends WebPageRenderer
{
	/**
	 * @param handler
	 *            the handler rendering the page
	 */
	public CookielessPageRenderer(RenderPageRequestHandler handler)
	{
		super(handler);
	}

	@Override
	protected boolean isOnePassRender()
	{
		return super.isOnePassRender() || isWithoutSessionCookie(RequestCycle.get());
	}

	private static boolean isWithoutSessionCookie(RequestCycle cycle)
	{
		return cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request &&
			"GET".equals(request.getMethod()) && request.isRequestedSessionIdFromCookie() == false;
	}
}
