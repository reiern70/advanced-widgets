/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.data.Access;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.dropdown.DropdownPage;

/**
 * Records one {@link Access} per session, with the visitor's clock from the browser, and shows
 * them on the {@link AccessesPage}.
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

		tester.startPage(AccessesPage.class);

		tester.assertRenderedPage(AccessesPage.class);
		tester.assertLabel("count", "1");
		tester.assertContains("Europe/Berlin");
		tester.assertContains("2026-10-10 09:00:00");
		tester.assertContains(">admin<");
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
