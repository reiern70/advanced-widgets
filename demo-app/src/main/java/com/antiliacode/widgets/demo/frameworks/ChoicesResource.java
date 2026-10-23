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
package com.antiliacode.widgets.demo.frameworks;

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

import com.antiliacode.widgets.demo.Countries;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The choices of the React and Svelte demos of {@link FrameworkTabs} as JSON, mounted by the
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
