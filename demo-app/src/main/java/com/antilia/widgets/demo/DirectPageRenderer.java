/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.request.Url.QueryParameter;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.render.WebPageRenderer;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.demo.error.ErrorPage;

/**
 * Renders some pages in the response instead of redirecting to their address in the session
 * (<code>/sign-in?0</code>):
 * <ul>
 * <li>the address of a page in a session, such as <code>/sign-in?0</code>, asked for with a GET
 * without the cookie of a session. The session is never in the URL (see <code>web.xml</code>), so a
 * client keeping no cookies, such as a robot or a monitor, comes back without the cookie it was
 * redirected with, and was sent between <code>/sign-in</code> and <code>/sign-in?0</code> for good.
 * A browser's first request, <code>/sign-in</code> without a page, is redirected as always, to the
 * page in its new session, which a reload so finds again;</li>
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
		return super.isOnePassRender() || isBackWithoutSessionCookie(RequestCycle.get()) ||
			ErrorPage.class.isAssignableFrom(getPageProvider().getPageClass());
	}

	private static boolean isBackWithoutSessionCookie(RequestCycle cycle)
	{
		if (!(cycle.getRequest().getContainerRequest() instanceof HttpServletRequest request) ||
			!"GET".equals(request.getMethod()) || request.isRequestedSessionIdFromCookie())
		{
			return false;
		}
		// the page's id first in the query, ?0 or ?0-1.-link, as Wicket writes it
		List<QueryParameter> query = cycle.getRequest().getUrl().getQueryParameters();
		return !query.isEmpty() && query.get(0).getName().matches("\\d+(-.*)?") &&
			Strings.isEmpty(query.get(0).getValue());
	}
}
