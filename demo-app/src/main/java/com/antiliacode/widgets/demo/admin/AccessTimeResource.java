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
package com.antiliacode.widgets.demo.admin;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.data.Access;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Receives the visitor's clock, which only the browser knows, into the {@link Access} of the
 * session: its time zone and its local time, posted once per session by the script that
 * {@link #renderHead(IHeaderResponse)} puts into every page. Robots that run no scripts leave no
 * access behind. The request binds the session, so the script leaves its promise in
 * {@code window.antiliaSession} for what needs the session, see
 * {@link com.antiliacode.widgets.demo.LiveStatusBehavior}.
 */
public class AccessTimeResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "api/access-time";

	/** The resource, as mounted at {@value #PATH}. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		AccessTimeResource.class, "access-time")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new AccessTimeResource();
		}
	};

	/** The longest time zone kept, as in the table. */
	private static final int MAXIMUM_ZONE_LENGTH = 100;

	/**
	 * Renders the script posting the visitor's clock, while the session's access lacks it.
	 *
	 * @param response
	 *            the header response of a page
	 */
	public static void renderHead(IHeaderResponse response)
	{
		if (!DemoSession.get().needsLocalTime())
		{
			return;
		}
		CharSequence url = RequestCycle.get().urlFor(REFERENCE, null);
		response.render(OnDomReadyHeaderItem.forScript(
			"(function() {" +
				"var now = new Date(), two = function(n) { return (n < 10 ? '0' : '') + n; };" +
				"var local = now.getFullYear() + '-' + two(now.getMonth() + 1) + '-' + two(now.getDate()) +" +
				" 'T' + two(now.getHours()) + ':' + two(now.getMinutes()) + ':' + two(now.getSeconds());" +
				"var zone = '';" +
				"try { zone = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch (e) {}" +
				"window.antiliaSession = fetch('" + url + "', { method: 'POST'," +
				" credentials: 'same-origin', body: new URLSearchParams({ zone: zone, local: local }) })" +
				".catch(function() {});" +
				"})();"));
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		ResourceResponse response = new ResourceResponse();
		response.disableCaching();
		if (!"POST".equals(((HttpServletRequest)attributes.getRequest()
			.getContainerRequest()).getMethod()))
		{
			response.setStatusCode(405);
			return response;
		}
		String zone = zone(attributes.getRequest().getPostParameters().getParameterValue("zone"));
		LocalDateTime local = localTime(
			attributes.getRequest().getPostParameters().getParameterValue("local"));
		DemoSession.get().recordLocalTime(access -> {
			access.setTimeZone(zone);
			access.setLocalTime(local);
		});
		response.setStatusCode(204);
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				((WebResponse)attributes.getResponse()).setContentLength(0);
			}
		});
		return response;
	}

	/**
	 * @return the time zone the browser sent, if it is one
	 */
	static String zone(StringValue value)
	{
		String zone = value.toString("");
		if (zone.isEmpty() || zone.length() > MAXIMUM_ZONE_LENGTH)
		{
			return null;
		}
		try
		{
			return ZoneId.of(zone).getId();
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/**
	 * @return the local time the browser sent, to the second, if it is one
	 */
	static LocalDateTime localTime(StringValue value)
	{
		try
		{
			return value.isEmpty() ? null
				: LocalDateTime.parse(value.toString()).truncatedTo(ChronoUnit.SECONDS);
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}
}
