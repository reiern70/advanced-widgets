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
package com.antiliacode.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestAccounts;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;
import com.antiliacode.widgets.demo.error.AccessDeniedPage;

/**
 * Keeps the demos for the visitors who signed in, and opens them to everybody in development; a
 * visitor who does not sign in gets no session on the way.
 */
class DemosAuthorizationTest
{
	private WicketTester tester;

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(DemosAuthorization.OPEN_PARAMETER);
		System.clearProperty(WicketApplication.SIGN_IN_AS_PROPERTY);
	}

	private void start(RuntimeConfigurationType configuration)
	{
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return configuration;
			}
		});
	}

	@Test
	void theHomePageAndTheSignInArePublicAndKeepNoSession()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(HomePage.class);
		tester.assertRenderedPage(HomePage.class);
		tester.assertVisible("signInHint");
		tester.assertVisible("signIn");
		tester.assertInvisible("signOut");
		tester.assertLabel("signInHint:account", "as user, password $Amigo_MEllon_Friend_4321.");
		tester.assertLabel("signInHint:inUse", "0 of 10 sessions in use.");
		tester.assertContains("We keep no record of the people who visit this site.");
		// no websocket, which would need a session
		tester.assertInvisible("webSocketStatus");
		assertTrue(tester.getSession().isTemporary());
		tester.startPage(SignInPage.class);
		tester.assertRenderedPage(SignInPage.class);
		assertTrue(tester.getSession().isTemporary());
	}

	@Test
	void aVisitorOpeningADemoSignsInFirstAndGoesOnToIt()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);
		// the demo is a parameter of the page, not something kept in a session
		assertEquals("dropdown", tester.getLastRenderedPage().getPageParameters()
			.get(SignInPage.TO).toString());
		assertTrue(tester.getSession().isTemporary());

		signIn("User", Accounts.USER_PASSWORD);

		tester.assertRenderedPage(DropdownPage.class);
		assertEquals("user", DemoSession.get().getAccount());
		tester.assertVisible("signOut");
		tester.assertLabel("signOut:account", "user");
		tester.assertInvisible("signIn");
	}

	@Test
	void signingInWithoutADemoToGoToGoesHome()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(SignInPage.class);
		signIn("user", Accounts.USER_PASSWORD);

		tester.assertRenderedPage(HomePage.class);
		tester.assertInvisible("signInHint");
	}

	@Test
	void signingOutClosesTheDemosAgain()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);
		tester.startPage(HomePage.class);
		DemoSession.get().signIn(Accounts.USER, Role.USER);
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);

		tester.newFormTester("signOut").submit();

		tester.assertRenderedPage(HomePage.class);
		assertFalse(DemoSession.get().isSignedIn());
		assertEquals(0, SignedInUsers.get().count());
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);
	}

	@Test
	void theDemosAreOpenInDevelopment()
	{
		start(RuntimeConfigurationType.DEVELOPMENT);

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
		tester.startPage(HomePage.class);
		tester.assertInvisible("signInHint");
	}

	@Test
	void theDemosAreOpenWithTheInitParameterButNotWithTheSystemProperty()
	{
		System.setProperty(DemosAuthorization.OPEN_PARAMETER, "true");
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);

		((MockServletContext)tester.getServletContext())
			.addInitParameter(DemosAuthorization.OPEN_PARAMETER, "true");
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
	}

	@Test
	void onlyTheAdministratorSeesTheAdministration()
	{
		start(RuntimeConfigurationType.DEVELOPMENT);

		// not even in development, where the demos are open
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(SignInPage.class);

		// user is turned away
		signIn("user", Accounts.USER_PASSWORD);
		tester.assertRenderedPage(AccessDeniedPage.class);
		tester.startPage(HomePage.class);
		tester.assertInvisible("admin");

		DemoSession.get().signOut();
		tester.startPage(AdminPage.class);
		signIn("admin", TestAccounts.ADMIN_PASSWORD);
		tester.assertRenderedPage(AdminPage.class);
		tester.assertVisible("admin");
	}

	@Test
	void theAdministrationOpenedBeforeSigningOutIsGone()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(AdminPage.class);
		signIn("admin", TestAccounts.ADMIN_PASSWORD);
		tester.assertRenderedPage(AdminPage.class);
		String url = tester.urlFor(new RenderPageRequestHandler(
			new PageProvider(tester.getLastRenderedPage()))).toString();
		DemoSession.get().signOut();

		// the page the administrator had open is not shown again
		tester.executeUrl(url);
		tester.assertRenderedPage(SignInPage.class);

		// nor to user, signing in in the same browser
		signIn("user", Accounts.USER_PASSWORD);
		tester.executeUrl(url);
		tester.assertRenderedPage(AccessDeniedPage.class);
	}

	@Test
	void locallyEverySessionIsTheAdministrators()
	{
		System.setProperty(WicketApplication.SIGN_IN_AS_PROPERTY, "admin");
		start(RuntimeConfigurationType.DEVELOPMENT);

		tester.startPage(HomePage.class);
		tester.assertLabel("signOut:account", "admin");
		tester.assertVisible("admin");
		assertTrue(tester.getLastRenderedPage().isPageStateless());
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(AdminPage.class);
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
	}

	/**
	 * Signs in on the sign-in page shown last, the letters of its captcha typed right.
	 */
	private void signIn(String name, String password)
	{
		String token = ((SignInPage)tester.getLastRenderedPage()).getCaptcha().getToken();
		FormTester form = tester.newFormTester("form");
		form.setValue("name", name);
		form.setValue("password", password);
		form.setValue("captcha:answer", CaptchaToken.letters(token, Instant.now()).orElseThrow());
		form.submit();
	}
}
