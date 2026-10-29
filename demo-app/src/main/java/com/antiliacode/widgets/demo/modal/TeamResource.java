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
package com.antiliacode.widgets.demo.modal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.demo.DemoSession;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * The team of the modal dialog example as JSON, mounted by the application at {@value #PATH}: the
 * React and Svelte apps add, edit and delete the members the Wicket page does, those of the
 * visitor's session, validated with the messages of the Wicket form.
 *
 * <pre>
 * GET    api/team         { "members": [{ "id": 1, "name": "Ada", "email": "...", "role": "developer",
 *                           "roleName": "Development" }, ...] }
 * POST   api/team         { "name": ..., "email": ..., "role": ... } -&gt; 201 { "member": ... }
 * PUT    api/team?id=1    { "name": ..., "email": ..., "role": ... } -&gt; 200 { "member": ... }
 * DELETE api/team?id=1    -&gt; 204
 * </pre>
 *
 * A member that is not valid is answered with 400 and {@code { "errors": { "email": "..." } }}, as
 * is a member more than a team has ({@value TeamStore#MAX_MEMBERS}), a member that is gone with
 * 404, and a body longer than {@value #MAX_BODY} bytes with 413, unread.
 */
public class TeamResource implements IResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "api/team";

	/** The parameter holding the id of the member to edit or delete. */
	public static final String ID = "id";

	/** The longest body of a request, in bytes: a member is a few short fields. */
	static final int MAX_BODY = 8 * 1024;

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(TeamResource.class,
		"team")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new TeamResource();
		}
	};

	/**
	 * What the resource answers: a status and, unless there is nothing to say, JSON.
	 */
	record Answer(int status, JSONObject json)
	{
	}

	/**
	 * Answers the request, with its status and JSON: written here, as AbstractResource writes no
	 * body with a status of 300 or more, and a member not valid is answered with 400 and its
	 * messages.
	 */
	@Override
	public void respond(Attributes attributes)
	{
		HttpServletRequest request = (HttpServletRequest)attributes.getRequest()
			.getContainerRequest();
		WebResponse response = (WebResponse)attributes.getResponse();
		String body = readBody(request);
		if (body == null)
		{
			response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			response.disableCaching();
			return;
		}
		DemoSession session = DemoSession.get();
		session.bind();
		Answer answer = answer(request.getMethod(), attributes.getParameters().get(ID).toLong(0),
			body, session.getTeam(), session.getLocale());

		response.setStatus(answer.status());
		response.disableCaching();
		if (answer.json() != null)
		{
			response.setContentType("application/json;charset=UTF-8");
			response.write(answer.json().toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Reads the body of a request that adds or updates a member, at most {@value #MAX_BODY} bytes.
	 *
	 * @return the body, empty for the other requests; {@code null} if it is longer
	 */
	static String readBody(HttpServletRequest request)
	{
		if (!"POST".equals(request.getMethod()) && !"PUT".equals(request.getMethod()))
		{
			return "";
		}
		// refused at once when the request says it is longer
		if (request.getContentLengthLong() > MAX_BODY)
		{
			return null;
		}
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		try
		{
			InputStream in = request.getInputStream();
			for (int read; (read = in.read(buffer)) >= 0;)
			{
				body.write(buffer, 0, read);
				if (body.size() > MAX_BODY)
				{
					return null;
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return body.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Answers a request.
	 *
	 * @param method
	 *            the HTTP method
	 * @param id
	 *            the id of the member to update or delete
	 * @param body
	 *            the body of the request, the fields of the member to add or update
	 * @param team
	 *            the team of the session
	 * @param locale
	 *            the language of the messages and of the names of the roles
	 * @return the answer
	 */
	static Answer answer(String method, long id, String body, TeamStore team, Locale locale)
	{
		switch (method)
		{
			case "GET":
				return new Answer(HttpServletResponse.SC_OK, members(team, locale));
			case "POST":
			case "PUT":
				TeamMember member = read(body);
				if (member == null)
				{
					return new Answer(HttpServletResponse.SC_BAD_REQUEST,
						new JSONObject().put("errors", new JSONObject()));
				}
				boolean added = "POST".equals(method);
				member.setId(added ? 0 : id);
				Map<String, String> errors = TeamStore.validate(member, locale);
				if (!errors.isEmpty())
				{
					return new Answer(HttpServletResponse.SC_BAD_REQUEST,
						new JSONObject().put("errors", new JSONObject(errors)));
				}
				TeamMember saved;
				try
				{
					saved = team.save(member);
				}
				catch (IllegalStateException full)
				{
					// shown with the name, the first field of the form
					return new Answer(HttpServletResponse.SC_BAD_REQUEST, new JSONObject()
						.put("errors", new JSONObject().put("name", TeamStore.full(locale))));
				}
				if (saved == null)
				{
					return new Answer(HttpServletResponse.SC_NOT_FOUND, null);
				}
				return new Answer(added ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK,
					new JSONObject().put("member", toJson(saved, locale)));
			case "DELETE":
				return new Answer(team.delete(id) ? HttpServletResponse.SC_NO_CONTENT
					: HttpServletResponse.SC_NOT_FOUND, null);
			default:
				return new Answer(HttpServletResponse.SC_METHOD_NOT_ALLOWED, null);
		}
	}

	/**
	 * @return the members of the team, their roles named in the language
	 */
	static JSONObject members(TeamStore team, Locale locale)
	{
		JSONArray members = new JSONArray();
		team.list().forEach(member -> members.put(toJson(member, locale)));
		return new JSONObject().put("members", members);
	}

	static JSONObject toJson(TeamMember member, Locale locale)
	{
		return new JSONObject().put("id", member.getId())
			.put("name", member.getName())
			.put("email", member.getEmail())
			.put("role", member.getRole())
			.put("roleName", TeamStore.text("role." + member.getRole(), locale));
	}

	/**
	 * @return the member the body of a request holds, {@code null} if it holds none
	 */
	private static TeamMember read(String body)
	{
		try
		{
			JSONObject json = new JSONObject(body);
			TeamMember member = new TeamMember();
			member.setName(json.optString("name", "").strip());
			member.setEmail(json.optString("email", "").strip());
			member.setRole(json.optString("role", ""));
			return member;
		}
		catch (JSONException e)
		{
			return null;
		}
	}
}
