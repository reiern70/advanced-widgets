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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.antiliacode.widgets.demo.data.AbstractAccess;
import com.antiliacode.widgets.demo.data.AccessFilter;
import com.antiliacode.widgets.demo.data.Accesses;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Who accessed the site, for administrators (see {@link AdminApiResource}), mounted at
 * {@value #PATH}, and the old accesses at {@value #OLD_PATH}, those
 * {@link Accesses#archive moved} out of the recent ones after {@link Accesses#KEPT}:
 * {@code GET api/admin/accesses} answers the accesses, the latest first, a page
 * of {@code size} at a time ({@value AdminApiResource#DEFAULT_SIZE}, at most
 * {@value AdminApiResource#MAXIMUM_SIZE}), the first being {@code page=0}, filtered by
 * <ul>
 * <li>{@code user}: the user exactly, such as {@code admin}, {@code unknown}, {@code robot} or
 * {@code user-claude-dev};</li>
 * <li>{@code userLike}: a part of the user, ignoring case;</li>
 * <li>{@code part}: {@code public} or {@code private}, whether the session opened the demos;</li>
 * <li>{@code address}: a part of the address the visitor came from;</li>
 * <li>{@code from} and {@code to}: the accesses from a time and before another, as
 * {@code 2026-10-15T10:00:00Z}.</li>
 * </ul>
 * It answers {@code {"total": 145, "page": 0, "size": 100, "accesses": [...]}}, an access being
 * {@code {"id", "accessed", "user", "part", "address", "userAgent", "timeZone", "localTime"}}.
 * {@code GET api/admin/old-accesses} answers the old accesses alike.
 */
public class AccessesApiResource extends AdminApiResource
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "api/admin/accesses";

	/** Where the resource of the old accesses is mounted. */
	public static final String OLD_PATH = "api/admin/old-accesses";

	/** The reference the application mounts. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		AccessesApiResource.class, "admin-accesses")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new AccessesApiResource(false);
		}
	};

	/** The reference of the old accesses the application mounts. */
	public static final ResourceReference OLD_REFERENCE = new ResourceReference(
		AccessesApiResource.class, "admin-old-accesses")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new AccessesApiResource(true);
		}
	};

	private final boolean old;

	/**
	 * @param old
	 *            whether the resource answers the old accesses
	 */
	AccessesApiResource(boolean old)
	{
		this.old = old;
	}

	@Override
	protected Answer answer(String method, Attributes attributes)
	{
		if (!"GET".equals(method))
		{
			throw new Refusal(405, "The accesses are only read: GET");
		}
		PageParameters parameters = attributes.getParameters();
		AccessFilter filter = new AccessFilter();
		filter.setExactUser(text(parameters, "user"));
		filter.setUser(text(parameters, "userLike"));
		filter.setAddress(text(parameters, "address"));
		filter.setFrom(instant(parameters, "from"));
		filter.setTo(instant(parameters, "to"));
		String part = text(parameters, "part");
		if (part != null)
		{
			switch (part)
			{
				case "public" -> filter.setPrivatePart(false);
				case "private" -> filter.setPrivatePart(true);
				default -> throw new Refusal(400, "part is public or private");
			}
		}
		int page = number(parameters, "page", 0, 0, Integer.MAX_VALUE);
		int size = number(parameters, "size", DEFAULT_SIZE, 1, MAXIMUM_SIZE);
		PageRequest pageable = PageRequest.of(page, size,
			Sort.by(Sort.Direction.DESC, "accessed").and(Sort.by(Sort.Direction.DESC, "id")));
		Page<? extends AbstractAccess> accesses = old ? Accesses.get().pageOld(filter, pageable)
			: Accesses.get().page(filter, pageable);
		JSONArray rows = new JSONArray();
		accesses.forEach(access -> rows.put(toJson(access)));
		return new Answer(200, new JSONObject().put("total", accesses.getTotalElements())
			.put("page", page)
			.put("size", size)
			.put("accesses", rows));
	}

	/**
	 * @return the access as the API answers it, without the id of its session
	 */
	static JSONObject toJson(AbstractAccess access)
	{
		return new JSONObject().put("id", access.getId())
			.put("accessed", json(access.getAccessed()))
			.put("user", access.getUserName())
			.put("part", access.isPrivatePart() ? "private" : "public")
			.put("address", json(access.getAddress()))
			.put("userAgent", json(access.getUserAgent()))
			.put("timeZone", json(access.getTimeZone()))
			.put("localTime", json(access.getLocalTime()));
	}
}
