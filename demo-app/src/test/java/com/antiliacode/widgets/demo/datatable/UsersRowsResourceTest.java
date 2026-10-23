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
package com.antiliacode.widgets.demo.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;
import com.github.openjson.JSONObject;

/**
 * Creates, changes and deletes users through the users resource, as the React and Svelte grids
 * do, every field of a user checked.
 */
class UsersRowsResourceTest
{
	/** The body the next request sends. */
	private static String body = "";

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication());
		// the resource as mounted, reading the body this test sends
		tester.getApplication().mountResource("test/users", new ResourceReference(
			UsersRowsResourceTest.class, "users")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IResource getResource()
			{
				return new UsersRowsResource()
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected String readBody(Attributes attributes)
					{
						return body;
					}
				};
			}
		});
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void createsChangesAndDeletesAUser()
	{
		body = "{\"firstName\":\" Ada \",\"lastName\":\"Lovelace\",\"country\":\"gb\"," +
			"\"city\":\"London\",\"address\":\"1 St James's Square\"}";
		JSONObject created = request("POST", "test/users", 201);
		String key = created.getString("key");
		JSONObject user = created.getJSONObject("data");
		assertEquals("Ada", user.getString("firstName"));
		assertEquals("GB", user.getString("country"));
		assertEquals("United Kingdom", user.getString("countryName"));
		long id = Long.parseLong(key);
		assertEquals("Lovelace", DatabaseLocator.getLargeDatabase().get(id).getLastName());

		body = body.replace("Lovelace", "King");
		assertEquals("King", request("PUT", "test/users?key=" + key, 200).getJSONObject("data")
			.getString("lastName"));
		assertEquals("King", DatabaseLocator.getLargeDatabase().get(id).getLastName());

		tester.getRequest().setMethod("DELETE");
		tester.executeUrl("test/users?key=" + key);
		assertEquals(204, tester.getLastResponse().getStatus());
		assertThrows(IllegalArgumentException.class,
			() -> DatabaseLocator.getLargeDatabase().get(id), "the user is gone");
	}

	@Test
	void refusesAUserWithAFieldMissingTooLongOrNotACountry()
	{
		User before = DatabaseLocator.getLargeDatabase().get(1);
		String lastName = before.getLastName();

		body = "{\"firstName\":\"Ada\",\"country\":\"GB\",\"city\":\"London\",\"address\":\"x\"}";
		assertEquals("lastName is required", request("PUT", "test/users?key=1", 400)
			.getString("error"));
		body = "{\"firstName\":\"Ada\",\"lastName\":\"" + "x".repeat(101) + "\"," +
			"\"country\":\"GB\",\"city\":\"London\",\"address\":\"x\"}";
		assertEquals("lastName is longer than 100", request("PUT", "test/users?key=1", 400)
			.getString("error"));
		body = "{\"firstName\":\"Ada\",\"lastName\":\"King\",\"country\":\"ZZ\"," +
			"\"city\":\"London\",\"address\":\"x\"}";
		assertEquals("Unknown country: ZZ", request("PUT", "test/users?key=1", 400)
			.getString("error"));
		body = "[1, 2]";
		request("POST", "test/users", 400);

		assertEquals(lastName, DatabaseLocator.getLargeDatabase().get(1).getLastName(),
			"a refused change leaves the user as it was");
	}

	private JSONObject request(String method, String path, int status)
	{
		tester.getRequest().setMethod(method);
		tester.executeUrl(path);
		assertEquals(status, tester.getLastResponse().getStatus(), tester.getLastResponseAsString());
		return new JSONObject(tester.getLastResponseAsString());
	}
}
