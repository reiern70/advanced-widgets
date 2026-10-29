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
package com.antiliacode.widgets.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import org.apache.wicket.protocol.ws.api.ConnectionRejected;
import org.apache.wicket.protocol.ws.api.IWebSocketConnectionFilter;
import org.apache.wicket.protocol.ws.api.WebSocketConnectionOriginFilter;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.register.ConfirmationMail;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lets a websocket connect only from a page of the site, as the browser tells with the
 * {@code Origin} of the handshake: its host is the host the handshake went to, by its
 * {@code Host}, or that of the address of the site
 * ({@value ConfirmationMail#SITE_URL_ENVIRONMENT_VARIABLE}). The host the handshake went to is
 * the one the browser asked for, also behind the proxy, which passes it on:
 * so the site connects from its own address, from {@code localhost} and from the address of the
 * machine in the local network alike, on any port. A handshake without an {@code Origin}, or from
 * another site, is refused as Wicket's {@link WebSocketConnectionOriginFilter} refuses it.
 */
public class WebSocketOriginFilter implements IWebSocketConnectionFilter
{
	@Override
	public ConnectionRejected doFilter(HttpServletRequest request)
	{
		// the request of a handshake knows the host it went to by its header only
		String hostHeader = header(request, "Host");
		String host = hostHeader != null ? hostOf("http://" + hostHeader)
			: request.getServerName();
		if (isAllowed(header(request, "Origin"), host, siteUrl()))
		{
			return null;
		}
		return new ConnectionRejected(WebSocketConnectionOriginFilter.POLICY_VIOLATION_ERROR_CODE,
			WebSocketConnectionOriginFilter.ORIGIN_MISMATCH);
	}

	/**
	 * @param origin
	 *            the {@code Origin} of a handshake
	 * @param host
	 *            the host the handshake went to
	 * @param site
	 *            the address of the site, {@code null} if it is not set
	 * @return whether the origin is a page of the site
	 */
	static boolean isAllowed(String origin, String host, String site)
	{
		String from = hostOf(origin);
		return from != null && (from.equalsIgnoreCase(host) || from.equalsIgnoreCase(hostOf(site)));
	}

	/**
	 * @return the value of a header of the request, whatever the case of its name; {@code null}
	 *         if it has none. The copy of a handshake hands out each value once.
	 */
	private static String header(HttpServletRequest request, String header)
	{
		Enumeration<String> names = request.getHeaderNames();
		while (names != null && names.hasMoreElements())
		{
			String name = names.nextElement();
			if (header.equalsIgnoreCase(name))
			{
				return request.getHeader(name);
			}
		}
		return null;
	}

	/**
	 * @return the host of an address, {@code null} if it has none
	 */
	private static String hostOf(String address)
	{
		if (Strings.isEmpty(address))
		{
			return null;
		}
		try
		{
			return new URI(address.strip()).getHost();
		}
		catch (URISyntaxException e)
		{
			return null;
		}
	}

	/**
	 * @return the address of the site, as the confirmation emails have it; {@code null} if it is
	 *         not set
	 */
	private static String siteUrl()
	{
		String site = System.getProperty(ConfirmationMail.SITE_URL_PROPERTY);
		return Strings.isEmpty(site) ? System.getenv(ConfirmationMail.SITE_URL_ENVIRONMENT_VARIABLE)
			: site;
	}
}
