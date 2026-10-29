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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * The components of a page answer the site's own pages only, as the browser tells with
 * {@code Sec-Fetch-Site} or {@code Origin}: a page of another site neither signs the user out nor
 * sends a form. Links from other sites still open pages, and clients that tell nothing of where
 * their requests come from, which are no browsers, are answered. (The headers in lower case: the
 * mock request, unlike a server, tells them apart.)
 */
class CrossSiteRequestsTest
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
		tester.startPage(HomePage.class);
		DemoSession.get().signIn("wicket");
		tester.startPage(DropdownPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void aPageOfAnotherSiteDoesNotSignTheUserOut()
	{
		tester.getRequest().setHeader("sec-fetch-site", "cross-site");
		tester.newFormTester("signOut").submit();

		assertEquals(403, tester.getLastResponse().getStatus());
		assertTrue(DemoSession.get().isSignedIn());

		tester.startPage(DropdownPage.class);
		tester.getRequest().setHeader("Origin", "https://elsewhere.example");
		tester.newFormTester("signOut").submit();

		assertEquals(403, tester.getLastResponse().getStatus());
		assertTrue(DemoSession.get().isSignedIn());
	}

	@Test
	void thePagesOfTheSiteDo()
	{
		tester.getRequest().setHeader("sec-fetch-site", "same-origin");
		tester.newFormTester("signOut").submit();

		tester.assertRenderedPage(HomePage.class);
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void aClientTellingNothingOfWhereItComesFromIsAnswered()
	{
		tester.newFormTester("signOut").submit();

		tester.assertRenderedPage(HomePage.class);
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void aLinkOfAnotherSiteOpensAPage()
	{
		tester.getRequest().setHeader("sec-fetch-site", "cross-site");
		tester.getRequest().setHeader("sec-fetch-mode", "navigate");
		tester.getRequest().setHeader("sec-fetch-dest", "document");
		tester.startPage(DropdownPage.class);

		tester.assertRenderedPage(DropdownPage.class);
		assertEquals(200, tester.getLastResponse().getStatus());
	}
}
