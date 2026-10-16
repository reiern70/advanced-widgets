/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.data.User;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The API of the administration: {@link UsersApiResource} lists, adds and removes accounts and
 * {@link AccessesApiResource} answers the accesses, for administrators only.
 */
class AdminApiTest
{
	private static final String ADMIN = "admin:$Macarena_4321";

	/** The body the next request sends. */
	private static String body = "";

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		});
		// the users as mounted, reading the body this test sends
		tester.getApplication().mountResource("test/admin/users", new ResourceReference(
			AdminApiTest.class, "users")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IResource getResource()
			{
				return new UsersApiResource()
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
	void listsAddsAndRemovesAccounts()
	{
		JSONObject list = request("GET", UsersApiResource.PATH, ADMIN, 200);
		assertEquals(3, list.getInt("total"));
		assertEquals("ADMIN", find(list.getJSONArray("users"), "admin").getString("role"));
		assertFalse(list.toString().contains("password"));

		body = "{\"name\": \"Ada@Example.com\", \"password\": \"Analytical-Engine-1843\"," +
			" \"firstName\": \"Ada\", \"lastName\": \"Lovelace\", \"country\": \"gb\"," +
			" \"language\": \"en\"}";
		JSONObject ada = request("POST", "test/admin/users", ADMIN, 201);
		assertEquals("ada@example.com", ada.getString("name"));
		assertEquals("ada@example.com", ada.getString("email"));
		assertEquals("USER", ada.getString("role"));
		assertEquals("GB", ada.getString("country"));
		assertTrue(ada.getBoolean("validated"));
		assertTrue(Accounts.get().authenticate("ada@example.com", "Analytical-Engine-1843")
			.isPresent());
		request("POST", "test/admin/users", ADMIN, 409);

		body = "{\"name\": \"client\", \"password\": \"Client-Secret-42\"," +
			" \"email\": \"Client@Example.org\"}";
		assertEquals("client@example.org",
			request("POST", "test/admin/users", ADMIN, 201).getString("email"));

		assertEquals(1, request("GET", UsersApiResource.PATH + "?email=example.org", ADMIN, 200)
			.getInt("total"));
		assertEquals("Lovelace", request("GET", UsersApiResource.PATH + "/ada@example.com",
			ADMIN, 200).getString("lastName"));

		request("DELETE", UsersApiResource.PATH + "/ada@example.com", ADMIN, 204);
		assertFalse(Accounts.get().find("ada@example.com").isPresent());
		request("DELETE", UsersApiResource.PATH + "/ada@example.com", ADMIN, 404);
		// not an administrator's
		request("DELETE", UsersApiResource.PATH + "/admin", ADMIN, 409);
		assertTrue(Accounts.get().find("admin").isPresent());
	}

	@Test
	void refusesWhatIsNotAnAccount()
	{
		body = "{\"name\": \"weak\", \"password\": \"password\"}";
		assertTrue(request("POST", "test/admin/users", ADMIN, 400).getString("error")
			.startsWith("password is needed"));
		body = "{\"password\": \"Analytical-Engine-1843\"}";
		assertEquals("name is needed", request("POST", "test/admin/users", ADMIN, 400)
			.getString("error"));
		body = "{\"name\": \"x\", \"password\": \"Analytical-Engine-1843\", \"role\": \"KING\"}";
		request("POST", "test/admin/users", ADMIN, 400);
		body = "{\"name\": \"x\", \"password\": \"Analytical-Engine-1843\", \"country\": \"ZZ\"}";
		request("POST", "test/admin/users", ADMIN, 400);
		body = "[1, 2]";
		request("POST", "test/admin/users", ADMIN, 400);
		assertEquals(3, TestDatabase.users().size());

		// JSON only, which no form of another site sends
		body = "{\"name\": \"x\", \"password\": \"Analytical-Engine-1843\"}";
		tester.getRequest().setHeader("Content-Type", "application/x-www-form-urlencoded");
		tester.getRequest().setMethod("POST");
		tester.getRequest().setHeader("Authorization", basic(ADMIN));
		tester.executeUrl("test/admin/users");
		assertEquals(415, tester.getLastResponse().getStatus());
	}

	@Test
	void onlyAdministratorsGetIn()
	{
		request("GET", UsersApiResource.PATH, null, 401);
		assertEquals("Basic realm=\"antilia-widgets\", charset=\"UTF-8\"",
			tester.getLastResponse().getHeader("WWW-Authenticate"));
		request("GET", UsersApiResource.PATH, "admin:wrong", 401);
		request("GET", UsersApiResource.PATH, "wicket:wicket4321", 403);
		request("GET", AccessesApiResource.PATH, "wicket:wicket4321", 403);

		// a session signed in as an administrator reads, but changes nothing
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("admin", Role.ADMIN);
		request("GET", UsersApiResource.PATH, null, 200);
		request("DELETE", UsersApiResource.PATH + "/wicket", null, 401);
		assertTrue(Accounts.get().find("wicket").isPresent());
	}

	@Test
	void guessingThePasswordStops()
	{
		for (int i = 0; i < 3; i++)
		{
			request("GET", UsersApiResource.PATH, "admin:guess-" + i, 401);
		}
		request("GET", UsersApiResource.PATH, ADMIN, 429);
	}

	@Test
	void answersTheAccessesFiltered()
	{
		record("ada@example.com", true, "200.46.55.1", "2026-10-15T10:00:00Z");
		record(Access.ROBOT, false, "66.249.66.5", "2026-10-15T11:00:00Z");
		record("user-claude-dev", false, "200.46.55.2", "2026-10-15T12:00:00Z");

		JSONObject all = request("GET", AccessesApiResource.PATH, ADMIN, 200);
		assertEquals(3, all.getInt("total"));
		JSONArray accesses = all.getJSONArray("accesses");
		// the latest first, without the id of the session
		assertEquals("user-claude-dev", accesses.getJSONObject(0).getString("user"));
		assertEquals("2026-10-15T12:00:00Z", accesses.getJSONObject(0).getString("accessed"));
		assertFalse(accesses.getJSONObject(0).has("sessionId"));

		String path = AccessesApiResource.PATH;
		assertEquals(1, request("GET", path + "?user=robot", ADMIN, 200).getInt("total"));
		assertEquals(1, request("GET", path + "?userLike=CLAUDE", ADMIN, 200).getInt("total"));
		assertEquals(1, request("GET", path + "?part=private", ADMIN, 200).getInt("total"));
		assertEquals(2, request("GET", path + "?address=200.46.55", ADMIN, 200).getInt("total"));
		assertEquals(2, request("GET", path + "?from=2026-10-15T11:00:00Z", ADMIN, 200)
			.getInt("total"));
		assertEquals(1, request("GET", path + "?to=2026-10-15T11:00:00Z", ADMIN, 200)
			.getInt("total"));
		JSONObject page = request("GET", path + "?size=2&page=1", ADMIN, 200);
		assertEquals(3, page.getInt("total"));
		assertEquals("ada@example.com", page.getJSONArray("accesses").getJSONObject(0)
			.getString("user"));

		request("GET", path + "?part=both", ADMIN, 400);
		request("GET", path + "?from=yesterday", ADMIN, 400);
		request("GET", path + "?size=5000", ADMIN, 400);
		request("DELETE", path, ADMIN, 405);
		assertEquals(3, Accesses.get().count());
	}

	@Test
	void answersTheOldAccessesFiltered()
	{
		record("ada@example.com", true, "200.46.55.1", "2026-09-15T10:00:00Z");
		record(Access.ROBOT, false, "66.249.66.5", "2026-09-15T11:00:00Z");
		record("user-claude-dev", false, "200.46.55.2", "2026-10-15T12:00:00Z");
		Accesses.get().archive(Instant.parse("2026-10-01T00:00:00Z"));

		String path = AccessesApiResource.OLD_PATH;
		JSONObject all = request("GET", path, ADMIN, 200);
		assertEquals(2, all.getInt("total"));
		assertEquals("robot", all.getJSONArray("accesses").getJSONObject(0).getString("user"));
		assertEquals(1, request("GET", path + "?part=private", ADMIN, 200).getInt("total"));
		assertEquals(1, request("GET", AccessesApiResource.PATH, ADMIN, 200).getInt("total"));
		request("GET", path, null, 401);
	}

	private static void record(String user, boolean privatePart, String address, String accessed)
	{
		Accesses.get().record(null,
			() -> new Access(null, address, "Mozilla/5.0", Instant.parse(accessed)), access -> {
				access.setUserName(user);
				access.setPrivatePart(privatePart);
			});
	}

	private JSONObject request(String method, String path, String credentials, int status)
	{
		MockHttpServletRequest request = tester.getRequest();
		request.setMethod(method);
		request.setHeader("Content-Type", "application/json");
		if (credentials != null)
		{
			request.setHeader("Authorization", basic(credentials));
		}
		tester.executeUrl(path);
		String answer = tester.getLastResponseAsString();
		assertEquals(status, tester.getLastResponse().getStatus(), answer);
		return answer.isEmpty() ? new JSONObject() : new JSONObject(answer);
	}

	private static String basic(String credentials)
	{
		return "Basic " +
			Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

	private static JSONObject find(JSONArray users, String name)
	{
		for (int i = 0; i < users.length(); i++)
		{
			if (users.getJSONObject(i).getString("name").equals(name))
			{
				return users.getJSONObject(i);
			}
		}
		throw new AssertionError("no " + name + " in " + users);
	}
}
