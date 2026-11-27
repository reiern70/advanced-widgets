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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.Role;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;
import com.antiliacode.widgets.demo.stats.Visits.PageCount;

import jakarta.servlet.http.Cookie;

/**
 * {@link PageViews}: each page shown counts, by its mount path, and the browser gets its
 * {@link VisitorCookie} with the first; the public pages stay stateless, and the administrator's
 * pages are not counted.
 */
class PageViewsTest
{
	private final TestClock clock = new TestClock();

	private WicketTester tester;

	@BeforeEach
	void start()
	{
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
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private Cookie visitorCookie()
	{
		return tester.getLastResponse().getCookies().stream()
			.filter(cookie -> VisitorCookie.NAME.equals(cookie.getName()))
			.findFirst().orElse(null);
	}

	@Test
	void theFirstPageGivesTheCookieAndTheNextOnesCountThePerson()
	{
		tester.startPage(HomePage.class);

		Cookie cookie = visitorCookie();
		assertNotNull(cookie);
		assertTrue(VisitorCookie.isValid(cookie.getValue()));
		assertEquals(395 * 24 * 60 * 60, cookie.getMaxAge());
		assertTrue(cookie.isHttpOnly());
		assertEquals("Lax", cookie.getAttribute("SameSite"));
		// the home page stays stateless
		assertTrue(tester.getLastRenderedPage().isPageStateless());
		assertTrue(tester.getSession().isTemporary());
		Visits.Report first = Visits.get().report(1);
		assertEquals(1, first.views());
		assertEquals(1, first.viewsWithoutCookie());
		assertEquals(0, first.people());

		tester.startPage(HomePage.class);
		// not given again, nor made to last longer
		assertEquals(null, visitorCookie());
		DemoSession.get().signIn(Accounts.USER, Role.USER);
		tester.startPage(DropdownPage.class);

		Visits.Report report = Visits.get().report(1);
		assertEquals(3, report.views());
		assertEquals(1, report.people());
		assertEquals(List.of(new PageCount("/", 2, 1), new PageCount("/dropdown", 1, 1)),
			report.pages());
	}

	@Test
	void theAjaxOfAPageCountsNothingAndAPageWithoutMountCountsByItsClass()
	{
		tester.startPage(AjaxPage.class);
		tester.clickLink("link", true);
		tester.clickLink("link", true);

		assertEquals(List.of(new PageCount("AjaxPage", 1, 0)), Visits.get().report(1).pages());
	}

	/**
	 * A page with an Ajax link, mounted nowhere.
	 */
	public static class AjaxPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		public AjaxPage()
		{
			add(new AjaxLink<Void>("link")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					target.add(this);
				}
			}.setOutputMarkupId(true));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream(
				"<html><body><a wicket:id=\"link\">link</a></body></html>");
		}
	}

	@Test
	void thePagesOfTheAdministratorAreNotCounted()
	{
		DemoSession.get().signIn(Accounts.ADMIN, Role.ADMIN);
		tester.startPage(HomePage.class);
		tester.startPage(AdminPage.class);

		assertEquals(0, Visits.get().report(1).views());
		assertFalse(Visits.get().report(1).pages().stream()
			.anyMatch(page -> page.page().equals("/admin")));
	}
}
