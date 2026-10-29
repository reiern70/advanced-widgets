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
package com.antiliacode.widgets.demo.admin;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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
 * <p>
 * A visitor of the stateless home page has no session yet: the post makes one only when it
 * carries the proof, signed by the server, that a page of the site was shown less than
 * {@link #SHOWN_VALID} ago ({@value #SHOWN}). A post alone, without a page, makes neither a
 * session nor an access. The page passes on who a visitor told it is
 * ({@link DemoSession#VISITOR_PARAMETER}), which the new session takes.
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

	/** The parameter of the post proving that a page was shown: when, and signed. */
	static final String SHOWN = "shown";

	/** How long after a page was shown its script makes a session. */
	static final Duration SHOWN_VALID = Duration.ofMinutes(10);

	/** The key signing the time a page was shown, new at every start of the server. */
	private static final SecretKeySpec KEY = newKey();

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
		// who the visitor told it is goes along, for the session the post makes
		String visitor = RequestCycle.get().getRequest().getQueryParameters()
			.getParameterValue(DemoSession.VISITOR_PARAMETER).toOptionalString();
		PageParameters parameters = DemoSession.isVisitorName(visitor)
			? new PageParameters().add(DemoSession.VISITOR_PARAMETER, visitor) : null;
		CharSequence url = RequestCycle.get().urlFor(REFERENCE, parameters);
		response.render(OnDomReadyHeaderItem.forScript(
			"(function() {" +
				"var now = new Date(), two = function(n) { return (n < 10 ? '0' : '') + n; };" +
				"var local = now.getFullYear() + '-' + two(now.getMonth() + 1) + '-' + two(now.getDate()) +" +
				" 'T' + two(now.getHours()) + ':' + two(now.getMinutes()) + ':' + two(now.getSeconds());" +
				"var zone = '';" +
				"try { zone = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch (e) {}" +
				"window.antiliaSession = fetch('" + url + "', { method: 'POST'," +
				" credentials: 'same-origin', body: new URLSearchParams({ zone: zone, local: local," +
				" " + SHOWN + ": '" + shown(Instant.now()) + "' }) })" +
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
		IRequestParameters posted = attributes.getRequest().getPostParameters();
		DemoSession session = DemoSession.get();
		if (session.isTemporary())
		{
			// no session without a page shown: a request alone makes neither it nor an access
			if (!isShown(posted.getParameterValue(SHOWN).toOptionalString(), Instant.now()))
			{
				response.setStatusCode(204);
				response.setWriteCallback(empty());
				return response;
			}
			session.bind();
			session.identify(attributes.getRequest().getQueryParameters()
				.getParameterValue(DemoSession.VISITOR_PARAMETER).toOptionalString());
		}
		String zone = zone(posted.getParameterValue("zone"));
		LocalDateTime local = localTime(posted.getParameterValue("local"));
		session.recordLocalTime(access -> {
			access.setTimeZone(zone);
			access.setLocalTime(local);
		});
		response.setStatusCode(204);
		response.setWriteCallback(empty());
		return response;
	}

	/**
	 * @return what writes no body
	 */
	private static WriteCallback empty()
	{
		return new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				((WebResponse)attributes.getResponse()).setContentLength(0);
			}
		};
	}

	/**
	 * @param time
	 *            when a page was shown
	 * @return the proof of it the page's script posts: the second, and its signature
	 */
	static String shown(Instant time)
	{
		long second = time.getEpochSecond();
		return second + "." + sign(second);
	}

	/**
	 * @param shown
	 *            what a post says of the page shown
	 * @param now
	 *            the time now
	 * @return whether the server signed it, less than {@link #SHOWN_VALID} ago
	 */
	static boolean isShown(String shown, Instant now)
	{
		int dot = shown != null ? shown.indexOf('.') : -1;
		if (dot <= 0 || dot > 20)
		{
			return false;
		}
		long second;
		try
		{
			second = Long.parseLong(shown.substring(0, dot));
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		long age = now.getEpochSecond() - second;
		return age >= -60 && age <= SHOWN_VALID.toSeconds() && MessageDigest.isEqual(
			sign(second).getBytes(StandardCharsets.US_ASCII),
			shown.substring(dot + 1).getBytes(StandardCharsets.US_ASCII));
	}

	private static String sign(long second)
	{
		try
		{
			Mac mac = Mac.getInstance(KEY.getAlgorithm());
			mac.init(KEY);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
				mac.doFinal(Long.toString(second).getBytes(StandardCharsets.US_ASCII)));
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("Cannot sign with HmacSHA256", e);
		}
	}

	private static SecretKeySpec newKey()
	{
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		return new SecretKeySpec(key, "HmacSHA256");
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
