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

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.SignInAttempts;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.data.User;
import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A resource of the administration's API, answering JSON to administrators only: a request sends
 * the name and the password of an administrator's account with HTTP Basic authentication, or, to
 * read only, comes from a session signed in as an administrator. A wrong password counts as a
 * failed attempt to sign in to the account, from the address of the request, as on the site (see
 * {@link SignInAttempts}): while the account or the address waits after too many failures, the
 * API answers 429 with {@code Retry-After}, the seconds to wait, without checking the password.
 * A change is sent as JSON, which no form of another site can send.
 * <p>
 * A field without a value is left out of the JSON. An error is answered with its status and
 * {@code {"error": "..."}}.
 */
abstract class AdminApiResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The most rows of a page. */
	static final int MAXIMUM_SIZE = 1000;

	/** The rows of a page unless the request asks for another number. */
	static final int DEFAULT_SIZE = 100;

	/** The longest body of a request. */
	private static final int MAXIMUM_BODY = 10_000;

	/**
	 * A request the resource cannot answer: its status and why.
	 */
	static class Refusal extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		private final int status;

		/** How long to wait before asking again, {@code null} if it is not said. */
		private final Duration retryAfter;

		Refusal(int status, String message)
		{
			this(status, message, null);
		}

		Refusal(int status, String message, Duration retryAfter)
		{
			super(message, null, false, false);
			this.status = status;
			this.retryAfter = retryAfter;
		}
	}

	/**
	 * What the resource answers: a status and JSON, {@code null} for none.
	 *
	 * @param status
	 *            the status
	 * @param json
	 *            a {@link JSONObject} or a {@link com.github.openjson.JSONArray}, or
	 *            {@code null}
	 */
	record Answer(int status, Object json)
	{
	}

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		String method = request.getMethod();
		Answer answer;
		Duration retryAfter = null;
		try
		{
			authorize(request, method);
			answer = answer(method, attributes);
		}
		catch (Refusal refusal)
		{
			answer = new Answer(refusal.status,
				new JSONObject().put("error", refusal.getMessage()));
			retryAfter = refusal.retryAfter;
		}
		return respond(attributes, answer, retryAfter);
	}

	/**
	 * @param method
	 *            the method of the request, as GET or POST
	 * @param attributes
	 *            the attributes of the request
	 * @return the answer
	 * @throws Refusal
	 *             if the request cannot be answered
	 */
	protected abstract Answer answer(String method, Attributes attributes);

	/**
	 * Lets an administrator in, else throws the refusal.
	 */
	private void authorize(HttpServletRequest request, String method)
	{
		String header = request.getHeader("Authorization");
		if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6))
		{
			if ("GET".equals(method) && DemoSession.get().isAdmin())
			{
				return;
			}
			throw new Refusal(401, "Send the account and password of an administrator");
		}
		String credentials;
		try
		{
			credentials = new String(Base64.getDecoder().decode(header.substring(6).strip()),
				StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e)
		{
			throw new Refusal(401, "The credentials are not Base64");
		}
		int colon = credentials.indexOf(':');
		String name = colon < 0 ? credentials : credentials.substring(0, colon);
		String password = colon < 0 ? "" : credentials.substring(colon + 1);
		SignInAttempts attempts = ((WicketApplication)WicketApplication.get()).getSignInAttempts();
		String address = SignInAttempts.address();
		Duration wait = attempts.retryAfter(name, address);
		if (wait.isPositive())
		{
			throw new Refusal(429, "Too many failed attempts: try again later", wait);
		}
		Accounts accounts = Accounts.get();
		Optional<User> user = accounts.authenticate(name, password);
		if (user.isEmpty())
		{
			attempts.failed(name, address, accounts.find(name).isPresent());
			throw new Refusal(401, "The account or the password is wrong");
		}
		if (user.get().getRole() != Role.ADMIN)
		{
			throw new Refusal(403, "Only administrators use the API");
		}
		attempts.succeeded(name);
	}

	/**
	 * @return the body of the request, JSON: an object
	 * @throws Refusal
	 *             if it is not JSON, an object, or too long
	 */
	protected JSONObject readObject(Attributes attributes)
	{
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		String type = request.getHeader("Content-Type");
		if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json"))
		{
			throw new Refusal(415, "Send JSON, with the content type application/json");
		}
		try
		{
			return new JSONObject(readBody(attributes));
		}
		catch (RuntimeException e)
		{
			throw new Refusal(400, "The body is not a JSON object");
		}
	}

	/**
	 * @return the body of the request, at most {@value #MAXIMUM_BODY} characters
	 */
	protected String readBody(Attributes attributes)
	{
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		StringBuilder body = new StringBuilder();
		char[] buffer = new char[4096];
		try
		{
			Reader reader = request.getReader();
			for (int read; (read = reader.read(buffer)) >= 0;)
			{
				body.append(buffer, 0, read);
				if (body.length() > MAXIMUM_BODY)
				{
					throw new Refusal(413, "The body is too long");
				}
			}
		}
		catch (IOException e)
		{
			throw new Refusal(400, "The body could not be read");
		}
		return body.toString();
	}

	/**
	 * @return the number of the parameter, the default when it is missing
	 * @throws Refusal
	 *             if it is not a number from the minimum to the maximum
	 */
	static int number(PageParameters parameters, String name, int fallback, int minimum,
		int maximum)
	{
		StringValue value = parameters.get(name);
		if (value.isEmpty())
		{
			return fallback;
		}
		try
		{
			int number = Integer.parseInt(value.toString().strip());
			if (number >= minimum && number <= maximum)
			{
				return number;
			}
		}
		catch (NumberFormatException e)
		{
			// refused below
		}
		throw new Refusal(400, name + " is a number from " + minimum + " to " + maximum);
	}

	/**
	 * @return the time of the parameter, as 2026-10-15T10:00:00Z; {@code null} when it is missing
	 * @throws Refusal
	 *             if it is not a time
	 */
	static Instant instant(PageParameters parameters, String name)
	{
		String value = parameters.get(name).toOptionalString();
		if (Strings.isEmpty(value))
		{
			return null;
		}
		try
		{
			return Instant.parse(value.strip());
		}
		catch (DateTimeParseException e)
		{
			throw new Refusal(400, name + " is a time such as 2026-10-15T10:00:00Z");
		}
	}

	/**
	 * @return the text of the parameter, {@code null} when it is missing or blank
	 */
	static String text(PageParameters parameters, String name)
	{
		String value = parameters.get(name).toOptionalString();
		return Strings.isEmpty(value) || value.isBlank() ? null : value.strip();
	}

	/**
	 * @return the value as the API writes it, a time as 2026-10-15T10:00:00Z; {@code null}, which
	 *         leaves the field out, for none
	 */
	static String json(Object value)
	{
		return value == null ? null : value.toString();
	}

	/**
	 * @param retryAfter
	 *            how long to wait before asking again, said in seconds in {@code Retry-After};
	 *            {@code null} to say nothing
	 */
	private static ResourceResponse respond(Attributes attributes, Answer answer,
		Duration retryAfter)
	{
		ResourceResponse response = new ResourceResponse();
		response.setStatusCode(answer.status());
		response.disableCaching();
		if (answer.status() == 401)
		{
			response.getHeaders().addHeader("WWW-Authenticate",
				"Basic realm=\"antilia-widgets\", charset=\"UTF-8\"");
		}
		if (retryAfter != null)
		{
			long seconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
			response.getHeaders().addHeader("Retry-After", String.valueOf(seconds));
		}
		byte[] bytes = answer.json() == null ? null
			: answer.json().toString().getBytes(StandardCharsets.UTF_8);
		if (bytes != null)
		{
			response.setContentType("application/json");
			response.setTextEncoding("UTF-8");
			if (answer.status() >= 300)
			{
				// Wicket writes no body for a status from 300 on, so the error is written here
				attributes.getResponse().write(bytes);
			}
			else
			{
				response.setContentLength(bytes.length);
			}
		}
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				if (bytes != null && answer.status() < 300)
				{
					attributes.getResponse().write(bytes);
				}
			}
		});
		return response;
	}
}
