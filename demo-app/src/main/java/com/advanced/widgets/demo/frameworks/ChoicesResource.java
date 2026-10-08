/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.frameworks;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.advanced.widgets.demo.Countries;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The choices of the React and Svelte demos of {@link FrameworksPage} as JSON, mounted by the
 * application at {@value #PATH}: the countries named in the language of the session, sorted by
 * their names, and some skills.
 *
 * <pre>
 * { "countries": [{ "value": "AF", "label": "Afghanistan" }, ...],
 *   "skills": [{ "value": "java", "label": "Java" }, ...] }
 * </pre>
 */
public class ChoicesResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "api/choices";

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(ChoicesResource.class,
		"choices")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new ChoicesResource();
		}
	};

	/** The skills, by their value and their name, which is the same in every language. */
	private static final List<String[]> SKILLS = List.of(new String[] { "java", "Java" },
		new String[] { "wicket", "Apache Wicket" }, new String[] { "typescript", "TypeScript" },
		new String[] { "react", "React" }, new String[] { "svelte", "Svelte" },
		new String[] { "css", "CSS" }, new String[] { "sql", "SQL" },
		new String[] { "docker", "Docker" });

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		Locale locale = Session.get().getLocale();
		String json = choices(locale).toString();

		ResourceResponse response = new ResourceResponse();
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.disableCaching();
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
	 * @param locale
	 *            the language of the names of the countries
	 * @return the choices
	 */
	static JSONObject choices(Locale locale)
	{
		Collator collator = Collator.getInstance(locale);
		JSONArray countries = new JSONArray();
		Arrays.stream(Locale.getISOCountries())
			.sorted(Comparator.comparing((String code) -> Countries.name(code, locale), collator))
			.forEach(code -> countries.put(choice(code, Countries.name(code, locale))));
		JSONArray skills = new JSONArray();
		SKILLS.forEach(skill -> skills.put(choice(skill[0], skill[1])));
		return new JSONObject().put("countries", countries).put("skills", skills);
	}

	private static JSONObject choice(String value, String label)
	{
		return new JSONObject().put("value", value).put("label", label);
	}
}
