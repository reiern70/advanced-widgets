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
package com.antiliacode.widgets.demo.stats;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.string.Strings;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The cookie telling one browser from another for the {@link Visits}: a random number, given with
 * the first page a browser opens, and nothing else; it lasts {@link Visits#KEPT}, 13 months, from
 * that page on, and is not made to last longer by the next ones. Out of reach of the scripts of
 * the page, sent along with the requests of the site's own pages only (SameSite=Lax), and over
 * HTTPS only on a site served over HTTPS.
 */
public final class VisitorCookie
{
	/** The name of the cookie. */
	public static final String NAME = "visitor";

	/** A number as the cookie holds it: 128 random bits in base64url, without padding. */
	private static final Pattern NUMBER = Pattern.compile("[A-Za-z0-9_-]{22}");

	private static final SecureRandom RANDOM = new SecureRandom();

	private VisitorCookie()
	{
	}

	/**
	 * @param number
	 *            what a cookie held
	 * @return whether it is a number as the cookie holds it
	 */
	public static boolean isValid(String number)
	{
		return number != null && NUMBER.matcher(number).matches();
	}

	/**
	 * @return the number the request of the cycle sent, {@code null} if none
	 */
	public static String read()
	{
		RequestCycle cycle = RequestCycle.get();
		return cycle == null ? null : read(cycle.getRequest());
	}

	/**
	 * @param request
	 *            a request
	 * @return the number it sent, {@code null} if none
	 */
	public static String read(Request request)
	{
		if (!(request instanceof WebRequest web))
		{
			return null;
		}
		Cookie cookie = web.getCookie(NAME);
		return cookie != null && isValid(cookie.getValue()) ? cookie.getValue() : null;
	}

	/**
	 * Gives the browser a number, unless the request sent one.
	 *
	 * @param cycle
	 *            the cycle of the request
	 */
	public static void give(RequestCycle cycle)
	{
		if (read(cycle.getRequest()) != null ||
			!(cycle.getResponse() instanceof WebResponse response))
		{
			return;
		}
		Cookie cookie = new Cookie(NAME, newNumber());
		cookie.setMaxAge((int)Visits.KEPT.toSeconds());
		cookie.setHttpOnly(true);
		cookie.setAttribute("SameSite", "Lax");
		String path = "/";
		if (cycle.getRequest().getContainerRequest() instanceof HttpServletRequest http)
		{
			cookie.setSecure(http.isSecure());
			if (!Strings.isEmpty(http.getContextPath()))
			{
				path = http.getContextPath();
			}
		}
		cookie.setPath(path);
		response.addCookie(cookie);
	}

	/**
	 * @return a new number
	 */
	public static String newNumber()
	{
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
