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
package com.antilia.widgets.demo.admin;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.Language;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.data.User;
import com.antilia.widgets.demo.data.UserFilter;
import com.antilia.widgets.demo.register.StrongPasswordValidator;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The accounts, for administrators (see {@link AdminApiResource}), mounted at {@value #PATH}:
 * <ul>
 * <li>{@code GET api/admin/users} lists them, the latest first, filtered as the table of the
 * users is by {@code name} (of the account, first or last), {@code email} and {@code country},
 * a page of {@code size} at a time ({@value AdminApiResource#DEFAULT_SIZE}, at most
 * {@value AdminApiResource#MAXIMUM_SIZE}), the first being {@code page=0}:
 * {@code {"total": 3, "page": 0, "size": 100, "users": [...]}};</li>
 * <li>{@code GET api/admin/users/<name>} answers one account;</li>
 * <li>{@code POST api/admin/users} adds one, sent as
 * {@code {"name": ..., "password": ..., "email": ..., "role": "USER", "firstName": ...,
 * "lastName": ..., "country": "ES", "language": "es"}}, of which the name and a strong password
 * are needed; the email of a name that is one is the name. The account opens the demos at once.
 * 201 with the account, 409 if there is one by that name;</li>
 * <li>{@code DELETE api/admin/users/<name>} removes an account, but an administrator's: 204.
 * Its accesses are kept; the accounts the demo always has come back when it starts.</li>
 * </ul>
 * An account is {@code {"id", "name", "email", "role", "firstName", "lastName", "country",
 * "language", "jobRole", "validated", "created"}}, never with its password.
 */
public class UsersApiResource extends AdminApiResource
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted; the name of an account follows it. */
	public static final String PATH = "api/admin/users";

	/** The reference the application mounts. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		UsersApiResource.class, "admin-users")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new UsersApiResource();
		}
	};

	private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

	/** The longest name or email, as in the table. */
	private static final int MAXIMUM_NAME = 254;

	/** The longest first or last name, as in the table. */
	private static final int MAXIMUM_PERSON_NAME = 60;

	@Override
	protected Answer answer(String method, Attributes attributes)
	{
		PageParameters parameters = attributes.getParameters();
		String name = parameters.getIndexedCount() > 0 ? parameters.get(0).toOptionalString()
			: null;
		switch (method)
		{
			case "GET":
				return name == null ? list(parameters) : new Answer(200, toJson(find(name)));
			case "POST":
				if (name != null)
				{
					throw new Refusal(405, "Post to " + PATH + " to add an account");
				}
				return add(readObject(attributes));
			case "DELETE":
				if (name == null)
				{
					throw new Refusal(405, "Delete " + PATH + "/<name> to remove an account");
				}
				return remove(find(name));
			default:
				throw new Refusal(405, "GET, POST or DELETE");
		}
	}

	private static Answer list(PageParameters parameters)
	{
		UserFilter filter = new UserFilter();
		filter.setName(text(parameters, "name"));
		filter.setEmail(text(parameters, "email"));
		String country = text(parameters, "country");
		filter.setCountry(country == null ? null : country.toUpperCase(Locale.ROOT));
		int page = number(parameters, "page", 0, 0, Integer.MAX_VALUE);
		int size = number(parameters, "size", DEFAULT_SIZE, 1, MAXIMUM_SIZE);
		Page<User> users = Accounts.get().page(filter, PageRequest.of(page, size,
			Sort.by(Sort.Direction.DESC, "created").and(Sort.by(Sort.Direction.DESC, "id"))));
		JSONArray rows = new JSONArray();
		users.forEach(user -> rows.put(toJson(user)));
		return new Answer(200, new JSONObject().put("total", users.getTotalElements())
			.put("page", page)
			.put("size", size)
			.put("users", rows));
	}

	private static User find(String name)
	{
		return Accounts.get()
			.find(name)
			.orElseThrow(() -> new Refusal(404, "There is no account " + name));
	}

	private static Answer add(JSONObject json)
	{
		String name = field(json, "name", MAXIMUM_NAME);
		if (name == null)
		{
			throw new Refusal(400, "name is needed");
		}
		String password = json.optString("password", null);
		if (!StrongPasswordValidator.isStrong(password))
		{
			throw new Refusal(400, "password is needed: " + StrongPasswordValidator.MINIMUM_LENGTH +
				" to " + StrongPasswordValidator.MAXIMUM_LENGTH +
				" characters, with a lower and an upper case letter, a digit and another character");
		}
		Role role = choice(json, "role", Role.class, Role.USER);
		String email = field(json, "email", MAXIMUM_NAME);
		if (email == null && name.contains("@"))
		{
			email = name;
		}
		if (email != null && !email.matches("[^@\\s]+@[^@\\s]+"))
		{
			throw new Refusal(400, "email is not an email");
		}
		String country = field(json, "country", 2);
		if (country != null && !COUNTRIES.contains(country.toUpperCase(Locale.ROOT)))
		{
			throw new Refusal(400, "country is an ISO 3166 code, such as ES");
		}
		Language language = language(json.optString("language", null));
		String firstName = field(json, "firstName", MAXIMUM_PERSON_NAME);
		String lastName = field(json, "lastName", MAXIMUM_PERSON_NAME);
		String theEmail = email == null ? null : email.toLowerCase(Locale.ROOT);
		Optional<User> user = Accounts.get().create(name, password, role, details -> {
			details.setEmail(theEmail);
			details.setFirstName(firstName);
			details.setLastName(lastName);
			details.setCountry(country == null ? null : country.toUpperCase(Locale.ROOT));
			details.setLanguage(language);
		});
		return new Answer(201, toJson(user.orElseThrow(
			() -> new Refusal(409, "There is an account " + Accounts.accountName(name)))));
	}

	private static Answer remove(User user)
	{
		if (user.getRole() == Role.ADMIN)
		{
			throw new Refusal(409, "An administrator's account is not removed");
		}
		Accounts.get().remove(user);
		return new Answer(204, null);
	}

	/**
	 * @return the text of the field, {@code null} when it is missing or blank
	 * @throws Refusal
	 *             if it is longer than the maximum
	 */
	private static String field(JSONObject json, String name, int maximum)
	{
		String value = json.optString(name, null);
		if (value == null || value.isBlank())
		{
			return null;
		}
		value = value.strip();
		if (value.length() > maximum)
		{
			throw new Refusal(400, name + " has at most " + maximum + " characters");
		}
		return value;
	}

	private static <E extends Enum<E>> E choice(JSONObject json, String name, Class<E> type,
		E fallback)
	{
		String value = json.optString(name, null);
		if (value == null || value.isBlank())
		{
			return fallback;
		}
		try
		{
			return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			throw new Refusal(400, name + " is one of " + Arrays.toString(type.getEnumConstants()));
		}
	}

	private static Language language(String code)
	{
		if (code == null || code.isBlank())
		{
			return null;
		}
		return Arrays.stream(Language.values())
			.filter(language -> language.getCode().equalsIgnoreCase(code.strip()))
			.findFirst()
			.orElseThrow(() -> new Refusal(400, "language is en, es or de"));
	}

	/**
	 * @return the account as the API answers it, without its password
	 */
	static JSONObject toJson(User user)
	{
		return new JSONObject().put("id", user.getId())
			.put("name", user.getName())
			.put("email", json(user.getEmail()))
			.put("role", user.getRole().name())
			.put("firstName", json(user.getFirstName()))
			.put("lastName", json(user.getLastName()))
			.put("country", json(user.getCountry()))
			.put("language", json(user.getLanguage() == null ? null : user.getLanguage().getCode()))
			.put("jobRole", json(user.getJobRole()))
			.put("validated", user.isValidated())
			.put("created", json(user.getCreated()));
	}
}
