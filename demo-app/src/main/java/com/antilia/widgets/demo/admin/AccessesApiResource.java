/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.AccessFilter;
import com.antilia.widgets.demo.data.Accesses;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Who accessed the site, for administrators (see {@link AdminApiResource}), mounted at
 * {@value #PATH}: {@code GET api/admin/accesses} answers the accesses, the latest first, a page
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
 */
public class AccessesApiResource extends AdminApiResource
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "api/admin/accesses";

	/** The reference the application mounts. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		AccessesApiResource.class, "admin-accesses")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new AccessesApiResource();
		}
	};

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
		Page<Access> accesses = Accesses.get().page(filter, PageRequest.of(page, size,
			Sort.by(Sort.Direction.DESC, "accessed").and(Sort.by(Sort.Direction.DESC, "id"))));
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
	static JSONObject toJson(Access access)
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
