/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.nio.file.Path;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.register.RegisterPage;

/**
 * Keeps the demos for the visitors who signed in, and opens them to everybody in development.
 */
class DemosAuthorizationTest
{
	@TempDir
	Path folder;

	private WicketTester tester;

	@AfterEach
	void stop()
	{
		tester.destroy();
		System.clearProperty(DemosAuthorization.OPEN_PROPERTY);
	}

	private void start(RuntimeConfigurationType configuration)
	{
		Accounts accounts = new Accounts(new File(folder.toFile(), "accounts.properties"),
			"client:secret-one");
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return configuration;
			}

			@Override
			protected Accounts newAccounts()
			{
				return accounts;
			}
		});
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
}
