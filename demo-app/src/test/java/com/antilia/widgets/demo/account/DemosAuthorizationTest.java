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
package com.antilia.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.core.request.handler.ListenerInvocationNotAllowedException;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.WicketTesterHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.admin.AdminPage;
import com.antilia.widgets.demo.data.Role;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.error.AccessDeniedPage;
import com.antilia.widgets.demo.register.RegisterPage;

/**
 * Keeps the demos for the visitors who signed in, and opens them to everybody in development.
 */
class DemosAuthorizationTest
{
	private WicketTester tester;

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(DemosAuthorization.OPEN_PROPERTY);
		System.clearProperty(WicketApplication.SIGN_IN_AS_PROPERTY);
	}

	private void start(RuntimeConfigurationType configuration)
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return configuration;
			}
		});
		Accounts.get().addConfigured("client:secret-one");
	}

	@Test
	void theHomePageAndTheRegistrationArePublic()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(HomePage.class);
		tester.assertRenderedPage(HomePage.class);
		tester.assertVisible("signInHint");
		tester.assertVisible("signIn");
		tester.assertInvisible("signOut");
		// the invitation's words do not take the place of the top bar's
		tester.assertContains("topbar__register-label\">Register<");
		tester.startPage(RegisterPage.class);
		tester.assertRenderedPage(RegisterPage.class);
	}

	@Test
	void aVisitorOpeningADemoSignsInFirstAndGoesOnToIt()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(SignInPage.class);

		FormTester form = tester.newFormTester("form");
		form.setValue("name", "Client");
		form.setValue("password", "secret-one");
		form.submit();

		tester.assertRenderedPage(DropdownPage.class);
		assertEquals("client", DemoSession.get().getAccount());
		tester.assertVisible("signOut");
		tester.assertLabel("signOut:account", "client");
		tester.assertInvisible("signIn");
		tester.assertInvisible("register");
	}

	@Test
	void aWrongPasswordIsTurnedAway()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(SignInPage.class);
		FormTester form = tester.newFormTester("form");
		form.setValue("name", "client");
		form.setValue("password", "secret-two");
		form.submit();

		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("The account or the password is wrong.");
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void signingInWithoutADemoToGoToGoesHome()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(SignInPage.class);
		FormTester form = tester.newFormTester("form");
		form.setValue("name", "client");
		form.setValue("password", "secret-one");
		form.submit();

		tester.assertRenderedPage(HomePage.class);
		tester.assertInvisible("signInHint");
	}

	@Test
	void signingOutClosesTheDemosAgain()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);
		DemoSession.get().signIn("client");
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);

		tester.clickLink("signOut");

		tester.assertRenderedPage(HomePage.class);
		assertFalse(DemoSession.get().isSignedIn());
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
	void theDemosAreOpenWithTheSystemProperty()
	{
		System.setProperty(DemosAuthorization.OPEN_PROPERTY, "true");
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
	}

	@Test
	void aCaptchaIsAskedAfterThreeFailedAttempts()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(SignInPage.class);
		for (int attempt = 0; attempt < SignInAttempts.BEFORE_CAPTCHA; attempt++)
		{
			tester.assertInvisible("form:captcha");
			signIn("client", "wrong-" + attempt);
			tester.assertErrorMessages("The account or the password is wrong.");
		}
		tester.assertVisible("form:captcha");

		// the right password alone is not enough any more
		signIn("client", "secret-one");
		tester.assertRenderedPage(SignInPage.class);
		assertFalse(DemoSession.get().isSignedIn());

		// wrong letters neither
		FormTester form = tester.newFormTester("form");
		form.setValue("name", "client");
		form.setValue("password", "secret-one");
		form.setValue("captcha:answer", "nope");
		form.submit();
		tester.assertErrorMessages("The letters are not those of the image. Try the new ones.");
		assertFalse(DemoSession.get().isSignedIn());

		SignInPage page = (SignInPage)tester.getLastRenderedPage();
		form = tester.newFormTester("form");
		form.setValue("name", "client");
		form.setValue("password", "secret-one");
		form.setValue("captcha:answer", page.getChallenge().toLowerCase());
		form.submit();
		tester.assertRenderedPage(HomePage.class);
		assertEquals("client", DemoSession.get().getAccount());
	}

	@Test
	void aCaptchaIsAskedForAnAccountGuessedFromNewSessions()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		for (int attempt = 0; attempt < SignInAttempts.BEFORE_CAPTCHA; attempt++)
		{
			tester.startPage(SignInPage.class);
			signIn("client", "wrong-" + attempt);
			// a robot starts each attempt in a new session
			tester.getSession().invalidateNow();
		}
		tester.startPage(SignInPage.class);
		signIn("client", "secret-one");
		assertFalse(DemoSession.get().isSignedIn());
		tester.assertVisible("form:captcha");
	}

	@Test
	void theCaptchaShowsOtherLetters()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);
		tester.startPage(SignInPage.class);
		for (int attempt = 0; attempt < SignInAttempts.BEFORE_CAPTCHA; attempt++)
		{
			signIn("nobody", "wrong");
		}
		SignInPage page = (SignInPage)tester.getLastRenderedPage();
		String first = page.getChallenge();

		tester.clickLink("form:captcha:pictured:another");

		tester.assertVisible("form:captcha");
		assertNotEquals(first, ((SignInPage)tester.getLastRenderedPage()).getChallenge());
	}

	@Test
	void atMostFiveUsersAreSignedInAtOnce()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);
		SignedInUsers users = SignedInUsers.get();
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			String id = "session-" + i;
			assertTrue(users.signIn("user" + i, Role.USER, () -> id));
		}

		tester.startPage(SignInPage.class);
		signIn("client", "secret-one");
		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("Too many users are signed in right now: at most 5 at once. " +
			"Please try again in a few minutes.");
		assertFalse(DemoSession.get().isSignedIn());

		// an administrator always signs in
		tester.startPage(SignInPage.class);
		signIn("admin", TestDatabase.ADMIN_PASSWORD);
		tester.assertRenderedPage(HomePage.class);
		DemoSession.get().signOut();

		// a user signing out makes room
		users.signedOut("session-1");
		tester.startPage(SignInPage.class);
		signIn("client", "secret-one");
		tester.assertRenderedPage(HomePage.class);
		assertEquals(SignedInUsers.MAXIMUM, users.count());
	}

	@Test
	void onlyAnAdministratorSeesTheAdministration()
	{
		start(RuntimeConfigurationType.DEVELOPMENT);

		// not even in development, where the demos are open
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(SignInPage.class);

		// a user who is not an administrator is turned away
		signIn("wicket", TestDatabase.WICKET_PASSWORD);
		tester.assertRenderedPage(AccessDeniedPage.class);
		tester.startPage(HomePage.class);
		tester.assertInvisible("admin");

		DemoSession.get().signOut();
		tester.startPage(AdminPage.class);
		signIn("admin", TestDatabase.ADMIN_PASSWORD);
		tester.assertRenderedPage(AdminPage.class);
		tester.assertVisible("admin");
		tester.assertContains("admin");
	}

	@Test
	void theAdministrationOpenedBeforeSigningOutIsGone()
	{
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(AdminPage.class);
		signIn("admin", TestDatabase.ADMIN_PASSWORD);
		tester.assertRenderedPage(AdminPage.class);
		String url = tester.urlFor(new RenderPageRequestHandler(
			new PageProvider(tester.getLastRenderedPage()))).toString();
		String accessesTab = WicketTesterHelper.findAjaxEventBehavior(
			tester.getComponentFromLastRenderedPage("tabs:tabs-container:tabs:1:link"), "click")
			.getCallbackUrl()
			.toString();
		DemoSession.get().signOut();

		// the page the administrator had open does not answer: its tab of the accesses stays shut
		tester.getRequest().setHeader("Wicket-Ajax", "true");
		tester.getRequest().setHeader("Wicket-Ajax-BaseURL", url);
		assertThrows(ListenerInvocationNotAllowedException.class,
			() -> tester.executeUrl(accessesTab));
		// nor is it shown again
		tester.executeUrl(url);
		tester.assertRenderedPage(SignInPage.class);

		// nor to a user who signs in in the same browser
		signIn("wicket", TestDatabase.WICKET_PASSWORD);
		tester.executeUrl(url);
		tester.assertRenderedPage(AccessDeniedPage.class);
	}

	@Test
	void locallyEverySessionIsTheAdministrators()
	{
		System.setProperty(WicketApplication.SIGN_IN_AS_PROPERTY, "admin");
		start(RuntimeConfigurationType.DEPLOYMENT);

		tester.startPage(HomePage.class);
		tester.assertLabel("signOut:account", "admin");
		tester.assertVisible("admin");
		assertTrue(tester.getLastRenderedPage().isPageStateless());
		tester.startPage(AdminPage.class);
		tester.assertRenderedPage(AdminPage.class);
		tester.startPage(DropdownPage.class);
		tester.assertRenderedPage(DropdownPage.class);
	}

	private void signIn(String name, String password)
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("name", name);
		form.setValue("password", password);
		form.submit();
	}
}
