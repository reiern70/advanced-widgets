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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.apache.wicket.request.Request;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.DemoSession;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletResponse;

/**
 * The {@link Visits} as JSON, mounted by the application at {@value #PATH}, for the administrator:
 * a session signed in as the administrator, or a request with
 * {@code Authorization: Bearer <token>}, the token of the system property {@value #TOKEN_PROPERTY}
 * or the environment variable {@value #TOKEN_ENVIRONMENT_VARIABLE}; without one only the session
 * gets it. Anybody else is answered 403.
 *
 * <pre>
 * GET api/stats                          the last 30 days, today the last one
 * GET api/stats?days=7                   the last 7 days, 1 to 395
 * GET api/stats?from=2026-11-01&amp;to=2026-11-15
 * </pre>
 *
 * The days are in UTC. The answer holds the {@link Visits.Report}: how many people opened a page
 * or signed in, how many of them for the first time, how many signed in and how often, how many
 * pages were shown and how many of them to nobody, then each page, each day and each visitor, a
 * browser told by the first characters of the number of its cookie, with when it came first and
 * last, on how many days, and the pages it opened.
 */
public class StatsResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "api/stats";

	/** The system property holding the token of the requests. */
	public static final String TOKEN_PROPERTY = "antilia-widgets.stats-token";

	/** The environment variable holding the token of the requests. */
	public static final String TOKEN_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_STATS_TOKEN";

	/** The days answered when the request names none. */
	public static final int DEFAULT_DAYS = 30;

	/** The shortest token taken: a shorter one is as good as none. */
	static final int MINIMUM_TOKEN = 32;

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(StatsResource.class,
		"stats")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new StatsResource(token());
		}
	};

	/** The SHA-256 of the token, {@code null} if there is none. */
	private final byte[] token;

	/**
	 * @param token
	 *            the token of the requests, {@code null} or shorter than {@value #MINIMUM_TOKEN}
	 *            characters for none
	 */
	public StatsResource(String token)
	{
		this.token = token == null || token.length() < MINIMUM_TOKEN ? null : sha256(token);
	}

	/**
	 * @return the token of {@value #TOKEN_PROPERTY} or {@value #TOKEN_ENVIRONMENT_VARIABLE}
	 */
	static String token()
	{
		String value = System.getProperty(TOKEN_PROPERTY);
		return Strings.isEmpty(value) ? System.getenv(TOKEN_ENVIRONMENT_VARIABLE) : value;
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		ResourceResponse response = new ResourceResponse();
		response.disableCaching();
		if (!isAllowed(attributes))
		{
			return response.setError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		}
		Visits visits = Visits.get();
		LocalDate today = visits.today();
		LocalDate from;
		LocalDate to;
		try
		{
			PageParameters parameters = attributes.getParameters();
			StringValue days = parameters.get("days");
			StringValue fromValue = parameters.get("from");
			if (!fromValue.isEmpty())
			{
				from = LocalDate.parse(fromValue.toString());
				StringValue toValue = parameters.get("to");
				to = toValue.isEmpty() ? today : LocalDate.parse(toValue.toString());
			}
			else
			{
				int count = days.isEmpty() ? DEFAULT_DAYS : days.toInt();
				to = today;
				from = today.minusDays(count - 1L);
			}
		}
		catch (DateTimeParseException | StringValueConversionException e)
		{
			return response.setError(HttpServletResponse.SC_BAD_REQUEST,
				"days is a number of days, from and to are dates such as 2026-11-27");
		}
		long span = ChronoUnit.DAYS.between(from, to) + 1;
		if (span < 1 || span > Visits.KEPT.toDays())
		{
			return response.setError(HttpServletResponse.SC_BAD_REQUEST,
				"From 1 to " + Visits.KEPT.toDays() + " days, from before to");
		}
		String json = toJson(visits.report(from, to)).toString(2);
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write(json.getBytes(StandardCharsets.UTF_8));
			}
		});
		return response;
	}

	/**
	 * @return whether the request is the administrator's: in the session, or with the token
	 */
	private boolean isAllowed(Attributes attributes)
	{
		return hasToken(token, attributes.getRequest()) || DemoSession.get().isAdmin();
	}

	/**
	 * @param request
	 *            a request
	 * @return whether it carries the token of {@value #TOKEN_PROPERTY} or
	 *         {@value #TOKEN_ENVIRONMENT_VARIABLE}, as the administrator's tools send it: the
	 *         pages it asks for are not counted
	 */
	public static boolean hasToken(Request request)
	{
		String token = token();
		return token != null && token.length() >= MINIMUM_TOKEN &&
			hasToken(sha256(token), request);
	}

	private static boolean hasToken(byte[] token, Request request)
	{
		if (token == null || !(request instanceof WebRequest web))
		{
			return false;
		}
		String authorization = web.getHeader("Authorization");
		return authorization != null && authorization.startsWith("Bearer ") &&
			MessageDigest.isEqual(token, sha256(authorization.substring(7).trim()));
	}

	/**
	 * @return the report as JSON
	 */
	static JSONObject toJson(Visits.Report report)
	{
		JSONObject json = new JSONObject();
		json.put("from", report.from().toString());
		json.put("to", report.to().toString());
		json.put("firstDay", report.firstDay() == null ? JSONObject.NULL
			: report.firstDay().toString());
		json.put("generated", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		json.put("people", report.people());
		json.put("newPeople", report.newPeople());
		json.put("signedInPeople", report.signedInPeople());
		json.put("signIns", report.signIns());
		json.put("views", report.views());
		json.put("viewsWithoutCookie", report.viewsWithoutCookie());
		JSONArray pages = new JSONArray();
		for (Visits.PageCount page : report.pages())
		{
			JSONObject item = new JSONObject();
			item.put("page", page.page());
			item.put("views", page.views());
			item.put("people", page.people());
			pages.put(item);
		}
		json.put("pages", pages);
		JSONArray days = new JSONArray();
		for (Visits.DayCount day : report.days())
		{
			JSONObject item = new JSONObject();
			item.put("date", day.date().toString());
			item.put("people", day.people());
			item.put("newPeople", day.newPeople());
			item.put("signedInPeople", day.signedInPeople());
			item.put("signIns", day.signIns());
			item.put("views", day.views());
			days.put(item);
		}
		json.put("days", days);
		JSONArray visitors = new JSONArray();
		for (Visits.VisitorCount visitor : report.visitors())
		{
			JSONObject item = new JSONObject();
			item.put("visitor", visitor.visitor());
			item.put("firstSeen", String.valueOf(visitor.firstSeen()));
			item.put("lastSeen", String.valueOf(visitor.lastSeen()));
			item.put("days", visitor.days());
			item.put("views", visitor.views());
			item.put("signIns", visitor.signIns());
			JSONObject opened = new JSONObject();
			visitor.pages().forEach(opened::put);
			item.put("pages", opened);
			visitors.put(item);
		}
		json.put("visitors", visitors);
		return json;
	}

	private static byte[] sha256(String text)
	{
		try
		{
			return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
