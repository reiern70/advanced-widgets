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
package com.antiliacode.widgets.demo.datatable;

import java.util.Locale;
import java.util.Set;

import org.apache.wicket.Session;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.dynamicdatatable.IDynamicDataProvider;
import com.antiliacode.widgets.dynamicdatatable.JsonRowsResource;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * Serves the users of the JSON grid demo from a mounted resource, {@value #PATH}, filtered by the
 * parameter {@value #FILTER} as the grid's filter does. It creates, changes and deletes users
 * too, as the React and Svelte grids do: a user sent is {@code {firstName, lastName, country,
 * city, address}}, every field required, the country an ISO 3166 code, the same fields the edit
 * form of the Wicket grid has. The users are shared by every visitor, at most
 * {@value UsersDatabase#MAX_USERS}: a user more is refused with 400. Like the demos, the users
 * are for visitors who signed in, unless the demos are open: the others are answered 403, here
 * as by {@link DemosAuthorization}.
 */
public class UsersRowsResource extends JsonRowsResource<User, Long>
{
	private static final long serialVersionUID = 1L;

	/** Where the resource is mounted. */
	public static final String PATH = "api/users";

	/** The parameter holding the filter by first or last name. */
	public static final String FILTER = "filter";

	/** The longest value of a field. */
	static final int MAX_LENGTH = 100;

	private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

	/** The reference the application mounts and the table fetches from. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		UsersRowsResource.class, "users")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new UsersRowsResource();
		}
	};

	@Override
	protected boolean isAllowed(Attributes attributes, String method)
	{
		return DemoSession.get().isSignedIn() || DemosAuthorization.isOpen();
	}

	@Override
	protected IDynamicDataProvider<User, Long> newProvider(PageParameters parameters)
	{
		// the session, if the browser has one, so that the countries are named in its language
		Session.get();
		UsersProvider provider = new UsersProvider();
		provider.setFilter(parameters.get(FILTER).toOptionalString());
		return provider;
	}

	@Override
	protected User createRow(String json)
	{
		User user = fill(new User(), json);
		try
		{
			DatabaseLocator.getLargeDatabase().save(user);
		}
		catch (IllegalStateException full)
		{
			throw new IllegalArgumentException("The demo keeps at most " +
				UsersDatabase.MAX_USERS + " users: delete some first", full);
		}
		return user;
	}

	@Override
	protected User updateRow(User user, String json)
	{
		// validated in full before any field changes, so a bad request leaves the user as it was
		User changed = fill(new User(), json);
		user.setFirstName(changed.getFirstName());
		user.setLastName(changed.getLastName());
		user.setCountry(changed.getCountry());
		user.setCity(changed.getCity());
		user.setAddress(changed.getAddress());
		DatabaseLocator.getLargeDatabase().update(user);
		return user;
	}

	@Override
	protected void deleteRow(User user)
	{
		DatabaseLocator.getLargeDatabase().delete(user);
	}

	/**
	 * @return the user with the fields of the JSON
	 * @throws IllegalArgumentException
	 *             if it is not JSON, or a field is missing, blank, too long or not a country
	 */
	private static User fill(User user, String json)
	{
		JSONObject fields;
		try
		{
			fields = new JSONObject(json);
		}
		catch (JSONException e)
		{
			throw new IllegalArgumentException("The user is not JSON", e);
		}
		user.setFirstName(field(fields, "firstName"));
		user.setLastName(field(fields, "lastName"));
		String country = field(fields, "country").toUpperCase(Locale.ROOT);
		if (COUNTRIES.contains(country) == false)
		{
			throw new IllegalArgumentException("Unknown country: " + country);
		}
		user.setCountry(country);
		user.setCity(field(fields, "city"));
		user.setAddress(field(fields, "address"));
		return user;
	}

	private static String field(JSONObject fields, String name)
	{
		Object value = fields.opt(name);
		String text = value instanceof String string ? string.strip() : "";
		if (text.isEmpty())
		{
			throw new IllegalArgumentException(name + " is required");
		}
		if (text.length() > MAX_LENGTH)
		{
			throw new IllegalArgumentException(name + " is longer than " + MAX_LENGTH);
		}
		return text;
	}
}
