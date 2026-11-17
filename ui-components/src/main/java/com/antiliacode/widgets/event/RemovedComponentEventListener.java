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
package com.antiliacode.widgets.event;

import org.apache.wicket.core.request.handler.ComponentNotFoundException;
import org.apache.wicket.core.request.handler.EmptyAjaxRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.lang.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers a request of a {@link ClientEventsBehavior client event} whose component is gone from
 * the page with an empty Ajax response, instead of the internal error page: an editor's state sent
 * as the user blurs it, or a moment after typing, may reach the server after the request that took
 * the editor away, the tab or the window it was in closed by the user for example. What the event
 * told is of a component that no longer is, and is dropped.
 * <p>
 * The browser sends nothing more for an element Wicket's Ajax removes, and Wicket's Ajax drops a
 * request whose component is no longer in the page as it goes; this is for the requests already
 * under way, those a page leaves in the browser after taking their component away on the server,
 * and the beacons sent as the page goes.
 * <p>
 * It answers the Ajax requests carrying {@link ClientEventsBehavior#EVENT_PARAMETER} alone; any
 * other {@link ComponentNotFoundException} goes on as before. The library's
 * {@link com.antiliacode.widgets.Initializer} adds it to each application.
 *
 * @since 11.0.0
 */
public class RemovedComponentEventListener implements IRequestCycleListener
{
	private static final Logger LOG = LoggerFactory.getLogger(RemovedComponentEventListener.class);

	@Override
	public IRequestHandler onException(RequestCycle cycle, Exception ex)
	{
		if (Exceptions.findCause(ex, ComponentNotFoundException.class) == null ||
			!isClientEvent(cycle.getRequest()))
		{
			return null;
		}
		LOG.debug("An event of a component gone from the page, dropped: {}", ex.getMessage());
		return EmptyAjaxRequestHandler.getInstance();
	}

	/**
	 * @return whether the request is one of a client event: via Ajax, telling the event's name
	 */
	private static boolean isClientEvent(Request request)
	{
		return request instanceof WebRequest web && web.isAjax() &&
			!web.getRequestParameters()
				.getParameterValue(ClientEventsBehavior.EVENT_PARAMETER)
				.isEmpty();
	}
}
