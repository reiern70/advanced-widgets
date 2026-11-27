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
package com.antiliacode.widgets.demo.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.Role;
import com.github.openjson.JSONObject;

/**
 * The {@link StatsResource}: the visits as JSON, for the administrator's session or with the
 * token, to nobody else.
 */
class StatsResourceTest
{
	private static final String TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final TestClock clock = new TestClock();

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		System.setProperty(StatsResource.TOKEN_PROPERTY, TOKEN);
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}

			@Override
			protected Visits newVisits()
			{
				return new Visits(null, clock);
			}
		});
		Visits.get().viewed("/", VisitorCookie.newNumber());
		Visits.get().viewed("/tree", null);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(StatsResource.TOKEN_PROPERTY);
	}

	private JSONObject json()
	{
		assertEquals(200, tester.getLastResponse().getStatus());
		assertTrue(tester.getLastResponse().getContentType().startsWith("application/json"));
		assertTrue(tester.getLastResponse().getHeader("Cache-Control").contains("no-store"));
		return new JSONObject(tester.getLastResponseAsString());
	}

	@Test
	void nobodyElseGetsThem()
	{
		tester.executeUrl(StatsResource.PATH);
		assertEquals(403, tester.getLastResponse().getStatus());

		DemoSession.get().signIn(Accounts.USER, Role.USER);
		tester.executeUrl(StatsResource.PATH);
		assertEquals(403, tester.getLastResponse().getStatus());
	}

	@Test
	void aWrongTokenIsTurnedAway()
	{
		tester.getRequest().setHeader("Authorization", "Bearer " + TOKEN.replace('0', '1'));
		tester.executeUrl(StatsResource.PATH);
		assertEquals(403, tester.getLastResponse().getStatus());
	}

	@Test
	void theTokenGetsTheLastThirtyDays()
	{
		tester.getRequest().setHeader("Authorization", "Bearer " + TOKEN);
		tester.executeUrl(StatsResource.PATH);

		JSONObject json = json();
		assertEquals("2026-09-16", json.getString("from"));
		assertEquals("2026-10-15", json.getString("to"));
		assertEquals("2026-10-15", json.getString("firstDay"));
		assertEquals(1, json.getInt("people"));
		assertEquals(2, json.getInt("views"));
		assertEquals(1, json.getInt("viewsWithoutCookie"));
		assertEquals(2, json.getJSONArray("pages").length());
		assertEquals("/", json.getJSONArray("pages").getJSONObject(0).getString("page"));
		assertEquals(30, json.getJSONArray("days").length());
		assertEquals(2, json.getJSONArray("days").getJSONObject(0).getInt("views"));
	}

	@Test
	void theAdministratorsSessionGetsThemByDaysOrFromTo()
	{
		DemoSession.get().signIn(Accounts.ADMIN, Role.ADMIN);

		tester.executeUrl(StatsResource.PATH + "?days=7");
		assertEquals(7, json().getJSONArray("days").length());

		tester.executeUrl(StatsResource.PATH + "?from=2026-10-01&to=2026-10-10");
		JSONObject json = json();
		assertEquals(10, json.getJSONArray("days").length());
		assertEquals(0, json.getInt("views"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "days=many", "days=0", "days=396", "from=yesterday",
		"from=2026-10-10&to=2026-10-01" })
	void whatIsNoSpanOfDaysIsABadRequest(String query)
	{
		tester.getRequest().setHeader("Authorization", "Bearer " + TOKEN);
		tester.executeUrl(StatsResource.PATH + "?" + query);
		assertEquals(400, tester.getLastResponse().getStatus());
	}

	@Test
	void aShortTokenIsNone()
	{
		System.setProperty(StatsResource.TOKEN_PROPERTY, "short");
		tester.getRequest().setHeader("Authorization", "Bearer short");
		tester.executeUrl(StatsResource.PATH);
		assertEquals(403, tester.getLastResponse().getStatus());
	}
}
