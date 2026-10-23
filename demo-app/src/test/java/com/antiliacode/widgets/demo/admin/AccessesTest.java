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
package com.antiliacode.widgets.demo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.data.Access;
import com.antiliacode.widgets.demo.data.Accesses;
import com.antiliacode.widgets.demo.data.OldAccess;
import com.antiliacode.widgets.demo.data.Role;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * Records one {@link Access} per session, with the visitor's clock from the browser, shows them on
 * the {@link AdminPage}, and moves the old ones to the {@link OldAccess old accesses}.
 */
class AccessesTest
{
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
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void aPublicVisitIsRecordedOnceWithTheVisitorsClock()
	{
		tester.startPage(HomePage.class);
		tester.assertContains(AccessTimeResource.PATH);
		assertNull(DemoSession.get().getAccessId());

		postTime("Europe/Madrid", "2026-10-10T08:15:30.123");

		Access access = access();
		assertEquals(Access.UNKNOWN, access.getUserName());
		assertFalse(access.isPrivatePart());
		assertEquals("Europe/Madrid", access.getTimeZone());
		assertEquals(LocalDateTime.parse("2026-10-10T08:15:30"), access.getLocalTime());
		assertNotNull(access.getAccessed());
		assertEquals(DemoSession.get().getId(), access.getSessionId());
		assertFalse(DemoSession.get().isTemporary());

		// the browser sends its clock once per session
		tester.startPage(HomePage.class);
		tester.assertContainsNot(AccessTimeResource.PATH);
		assertEquals(1, Accesses.get().count());
	}

	@Test
	void signingInAndOpeningADemoAreRecordedInTheSameAccess()
	{
		tester.startPage(HomePage.class);
		postTime("America/New_York", "2026-10-10T02:15:30");

		DemoSession.get().signIn("wicket", Role.USER);
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);

		Access access = access();
		assertEquals("wicket", access.getUserName());
		assertTrue(access.isPrivatePart());
		assertEquals("America/New_York", access.getTimeZone());
		// the new id of the session after signing in
		assertEquals(DemoSession.get().getId(), access.getSessionId());
		assertEquals(1, Accesses.get().count());
	}

	@Test
	void whatTheBrowserSendsIsChecked()
	{
		tester.startPage(HomePage.class);
		postTime("Not/AZone", "yesterday");

		Access access = access();
		assertNull(access.getTimeZone());
		assertNull(access.getLocalTime());

		assertEquals("UTC", AccessTimeResource.zone(StringValue.valueOf("UTC")));
		assertNull(AccessTimeResource.zone(StringValue.valueOf("")));
		assertNull(AccessTimeResource.zone(StringValue.valueOf("x".repeat(101))));
		assertNull(AccessTimeResource.localTime(StringValue.valueOf((String)null)));
	}

	@Test
	void theResourceOnlyTakesPosts()
	{
		tester.executeUrl(AccessTimeResource.PATH);

		assertEquals(405, tester.getLastResponse().getStatus());
		assertEquals(0, Accesses.get().count());
	}

	@Test
	void anAdministratorSeesTheAccesses()
	{
		tester.startPage(HomePage.class);
		postTime("Europe/Berlin", "2026-10-10T09:00:00");
		DemoSession.get().signIn("admin", Role.ADMIN);
		tester.startPage(DropdownPage.class);

		tester.startPage(AdminPage.class);
		tester.clickLink("tabs:tabs-container:tabs:1:link");

		tester.assertRenderedPage(AdminPage.class);
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("Europe/Berlin");
		tester.assertContains("2026-10-10 09:00:00");
		tester.assertContains(">admin<");
	}

	@Test
	void theUserComesFirstAndTheTimeOfTheServerBesideTheVisitors()
	{
		openAccesses(1);

		FilteredAdminTable<?> table = tester.getLastRenderedPage().visitChildren(
			FilteredAdminTable.class,
			(FilteredAdminTable<?> component, IVisit<FilteredAdminTable<?>> visit) -> visit
				.stop(component));
		assertEquals(List.of("user", "part", "address", "accessed", "localTime", "timeZone",
			"browser"), table.getColumns()
				.stream()
				.map(column -> ((AdminColumn<?>)column).getHeaderKey())
				.toList());
	}

	@Test
	void aVisitorTellsWhoItIs()
	{
		tester.startPage(HomePage.class,
			new PageParameters().add(DemoSession.VISITOR_PARAMETER, "claude-dev"));

		// recorded at once, the script and robots alike
		assertEquals("user-claude-dev", access().getUserName());
		assertFalse(DemoSession.get().isTemporary());
		postTime("UTC", "2026-10-15T10:00:00");
		assertEquals("user-claude-dev", access().getUserName());
		assertEquals(1, Accesses.get().count());

		// the account signed in with wins
		DemoSession.get().signIn("wicket", Role.USER);
		assertEquals("wicket", access().getUserName());
		tester.startPage(HomePage.class,
			new PageParameters().add(DemoSession.VISITOR_PARAMETER, "someone-else"));
		assertEquals("wicket", access().getUserName());
		assertEquals(1, Accesses.get().count());
	}

	@Test
	void whatAVisitorTellsIsChecked()
	{
		tester.startPage(HomePage.class, new PageParameters()
			.add(DemoSession.VISITOR_PARAMETER, "admin'); DROP TABLE accesses; --"));
		assertNull(DemoSession.get().getVisitor());
		assertNull(DemoSession.get().getAccessId());

		tester.startPage(HomePage.class,
			new PageParameters().add(DemoSession.VISITOR_PARAMETER, "x".repeat(41)));
		assertNull(DemoSession.get().getVisitor());
	}

	@Test
	void robotsAreToldApart()
	{
		tester.startPage(HomePage.class);
		tester.getRequest().setHeader("User-Agent",
			"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
		postTime("America/Los_Angeles", "2026-10-14T17:00:00");

		assertEquals(Access.ROBOT, access().getUserName());

		assertTrue(Access.isRobot("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
			"AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15 " +
			"(Applebot/0.1; +http://www.apple.com/go/applebot)"));
		assertTrue(Access.isRobot("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
			"(KHTML, like Gecko) HeadlessChrome/140.0.0.0 Safari/537.36"));
		assertTrue(Access.isRobot("curl/8.5.0"));
		assertFalse(Access.isRobot("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:155.0) " +
			"Gecko/20100101 Firefox/155.0"));
		assertFalse(Access.isRobot("Mozilla/5.0 (iPhone; CPU iPhone OS 18_7_8 like Mac OS X) " +
			"AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1"));
		assertFalse(Access.isRobot(null));
	}

	@Test
	void theAccessesAreFilteredByUserPartAndAddress()
	{
		record("ada@example.com", true, "200.46.55.1");
		record(Access.UNKNOWN, false, "66.249.66.5");
		record(Access.UNKNOWN, false, "66.249.66.4");
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("admin", Role.ADMIN);
		tester.startPage(AdminPage.class);
		tester.clickLink("tabs:tabs-container:tabs:1:link");
		// the private part at first, where those who signed in are
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("200.46.55.1");
		tester.assertContainsNot("66.249.66.5");
		filter("part", "", "change");
		tester.assertLabel("tabs:panel:count", "4");

		filter("user", "ADA", "input");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("200.46.55.1");
		tester.assertContainsNot("66.249.66.5");

		filter("user", "", "input");
		filter("part", "true", "change");
		tester.assertLabel("tabs:panel:count", "1");
		filter("part", "false", "change");
		tester.assertLabel("tabs:panel:count", "3");

		filter("part", "", "change");
		filter("address", "66.249.", "input");
		tester.assertLabel("tabs:panel:count", "2");
		filter("user", "unknown", "input");
		tester.assertLabel("tabs:panel:count", "2");
		filter("user", "admin", "input");
		tester.assertLabel("tabs:panel:count", "0");
	}

	@Test
	void theAccessesAreFilteredByTheTimeOfTheServer()
	{
		record("ada@example.com", true, "200.46.55.1", Instant.parse("2026-10-15T10:00:00Z"));
		record("ada@example.com", true, "200.46.55.1", Instant.parse("2026-10-15T11:00:30Z"));
		record("ada@example.com", true, "200.46.55.1", Instant.parse("2026-10-15T12:00:00Z"));
		// and the administrator's, now
		openAccesses(1);
		tester.assertLabel("tabs:panel:count", "4");

		// in UTC, as the table shows it
		filter("accessedFrom", "2026-10-15T11:00", "change");
		tester.assertLabel("tabs:panel:count", "3");
		// until the end of the minute
		filter("accessedUntil", "2026-10-15T11:00", "change");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("2026-10-15 11:00:30 UTC");
		filter("accessedFrom", "", "change");
		tester.assertLabel("tabs:panel:count", "2");
		filter("accessedUntil", "", "change");
		tester.assertLabel("tabs:panel:count", "4");
	}

	@Test
	void aButtonTellsAllAboutTheAccountOfAUser()
	{
		TestDatabase.accounts().create("ada@example.com", "Analytical-Engine-1843", Role.USER,
			user -> {
				user.setEmail("ada@example.com");
				user.setFirstName("Ada");
				user.setLastName("Lovelace");
			});
		record("ada@example.com", true, "200.46.55.1");
		record(Access.UNKNOWN, true, "66.249.66.5");
		record("user-claude-dev", true, "66.249.66.6");
		openAccesses(1);

		// only the accounts have one
		assertTrue(userCell("ada@example.com").get("info").isVisible());
		assertFalse(userCell(Access.UNKNOWN).get("info").isVisible());
		assertFalse(userCell("user-claude-dev").get("info").isVisible());
		assertTrue(userCell("admin").get("info").isVisible());

		Component info = userCell("ada@example.com").get("info");
		tester.clickLink(info);
		assertTrue(tester.getLastResponseAsString()
			.contains("Wicket.Popover.show(\"" + info.getMarkupId() + "\""));
		tester.assertContains("<th scope=\"row\">First name</th><td>Ada</td>");
		tester.assertContains("<th scope=\"row\">Last name</th><td>Lovelace</td>");
		tester.assertContains("<th scope=\"row\">Access</th><td>User</td>");

		// an account removed in between
		TestDatabase.accounts().remove(TestDatabase.accounts().find("ada@example.com").get());
		tester.clickLink(userCell("ada@example.com").get("info"));
		tester.assertContains("There is no such account any more.");
	}

	@Test
	void theOldAccessesMoveToTheirTableAndTab()
	{
		Instant now = Instant.now();
		record("ada@example.com", true, "200.46.55.1", now.minus(Duration.ofDays(11)));
		record("grace@example.org", true, "200.46.55.2", now.minus(Duration.ofDays(9)));
		Long oldest = record(Access.ROBOT, false, "66.249.66.5", now.minus(Duration.ofDays(30)))
			.getId();

		assertEquals(2, Accesses.get().archive(now.minus(Accesses.KEPT)));
		assertEquals(1, Accesses.get().count());
		assertEquals(2, Accesses.get().countOld());
		OldAccess robot = Accesses.get().findOld(oldest);
		// as it was, with its id
		assertEquals(Access.ROBOT, robot.getUserName());
		assertEquals("66.249.66.5", robot.getAddress());
		assertFalse(robot.isPrivatePart());
		assertNull(Accesses.get().find(oldest));
		// nothing more to move
		assertEquals(0, Accesses.get().archive(now.minus(Accesses.KEPT)));
		assertEquals(2, Accesses.get().countOld());

		openAccesses(2);
		tester.assertComponent("tabs:panel", AccessesPanel.class);
		tester.assertContains("older than 10 days");
		tester.assertLabel("tabs:panel:count", "1");
		tester.assertContains("200.46.55.1");
		filter("part", "", "change");
		tester.assertLabel("tabs:panel:count", "2");
		tester.assertContains("66.249.66.5");
		tester.assertContainsNot("200.46.55.2");
	}

	/**
	 * Signs in as the administrator and opens a tab of the administration.
	 */
	private void openAccesses(int tab)
	{
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("admin", Role.ADMIN);
		tester.startPage(DropdownPage.class);
		tester.startPage(AdminPage.class);
		tester.clickLink("tabs:tabs-container:tabs:" + tab + ":link");
	}

	private UserCell userCell(String user)
	{
		return tester.getLastRenderedPage().visitChildren(UserCell.class,
			(UserCell cell, IVisit<UserCell> visit) -> {
				if (user.equals(((Label)cell.get("name")).getDefaultModelObjectAsString()))
				{
					visit.stop(cell);
				}
			});
	}

	private static void record(String user, boolean privatePart, String address)
	{
		record(user, privatePart, address, Instant.now());
	}

	private static Access record(String user, boolean privatePart, String address,
		Instant accessed)
	{
		return Accesses.get().record(null,
			() -> new Access(null, address, "Mozilla/5.0", accessed), access -> {
				access.setUserName(user);
				access.setPrivatePart(privatePart);
			});
	}

	private void filter(String field, String value, String event)
	{
		AccessesFilterPanel panel = tester.getLastRenderedPage().visitChildren(
			AccessesFilterPanel.class,
			(AccessesFilterPanel component, IVisit<AccessesFilterPanel> visit) -> visit.stop(component));
		FormComponent<?> component = (FormComponent<?>)panel.get(field);
		tester.getRequest().getPostParameters().setParameterValue(component.getInputName(), value);
		tester.executeAjaxEvent(component, event);
	}

	private void postTime(String zone, String local)
	{
		MockHttpServletRequest request = tester.getRequest();
		request.setMethod("POST");
		request.setURL(request.getContextPath() + request.getServletPath() + "/" +
			AccessTimeResource.PATH);
		request.getPostParameters().setParameterValue("zone", zone);
		request.getPostParameters().setParameterValue("local", local);
		tester.processRequest();
		assertEquals(204, tester.getLastResponse().getStatus());
	}

	private Access access()
	{
		Long id = DemoSession.get().getAccessId();
		assertNotNull(id);
		return Accesses.get().find(id);
	}
}
