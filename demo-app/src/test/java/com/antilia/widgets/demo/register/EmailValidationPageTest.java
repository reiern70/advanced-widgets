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
package com.antilia.widgets.demo.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.DemoSession;
import com.antilia.widgets.demo.HomePage;
import com.antilia.widgets.demo.RecordingMailer;
import com.antilia.widgets.demo.TestDatabase;
import com.antilia.widgets.demo.WicketApplication;
import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.account.SignInPage;
import com.antilia.widgets.demo.data.Language;

/**
 * Confirms the email of a registered account with the link of its {@link ConfirmationMail} on
 * the {@link EmailValidationPage}; until then the account does not sign in.
 */
class EmailValidationPageTest
{
	private static final String PASSWORD = "Analytical-Engine-1843";

	private WicketTester tester;

	private RecordingMailer mailer;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		WicketApplication application = new WicketApplication()
		{
			@Override
			public RuntimeConfigurationType getConfigurationType()
			{
				return RuntimeConfigurationType.DEPLOYMENT;
			}
		};
		tester = new WicketTester(application);
		mailer = RecordingMailer.install(application);
		// as the register page does
		tester.startPage(HomePage.class);
		String token = Accounts.get().register("ada@example.com", PASSWORD, user -> {
			user.setFirstName("Ada");
			user.setLanguage(Language.SPANISH);
		}).orElseThrow();
		ConfirmationMail.send(Accounts.get().find("ada@example.com").orElseThrow(), token);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	@Test
	void theEmailIsRenderedByWicketInTheLanguageOfTheAccount()
	{
		assertEquals("ada@example.com", mailer.last().to());
		assertEquals("Confirme su correo para Antilia Widgets", mailer.last().subject());
		String html = mailer.last().html();
		assertTrue(html.contains("Hola Ada:"), html);
		assertTrue(html.contains("Confirmar mi correo"), html);
		assertTrue(html.contains("48 horas"), html);
		assertFalse(html.contains("wicket:"), html);
		assertTrue(mailer.last().text().contains("Hola Ada:"), mailer.last().text());
	}

	@Test
	void theLinkPointsAtTheSiteConfigured()
	{
		System.setProperty(ConfirmationMail.SITE_URL_PROPERTY, "https://example.com/");
		try
		{
			assertEquals("https://example.com/validate-email?token=abc",
				ConfirmationMail.link("abc"));
		}
		finally
		{
			System.clearProperty(ConfirmationMail.SITE_URL_PROPERTY);
		}
	}

	@Test
	void theLinkConfirmsTheEmailOnce()
	{
		tester.startPage(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, mailer.lastToken()));

		tester.assertRenderedPage(EmailValidationPage.class);
		tester.assertVisible("validated");
		tester.assertInvisible("invalid");
		assertTrue(Accounts.get().find("ada@example.com").orElseThrow().isValidated());
		// it does not sign in by itself
		assertFalse(DemoSession.get().isSignedIn());

		tester.startPage(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, mailer.lastToken()));
		tester.assertVisible("invalid");
		tester.assertInvisible("validated");
	}

	@Test
	void aWrongLinkSaysSo()
	{
		tester.startPage(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, "nonsense"));
		tester.assertVisible("invalid");

		tester.startPage(EmailValidationPage.class);
		tester.assertVisible("invalid");
		assertFalse(Accounts.get().find("ada@example.com").orElseThrow().isValidated());
	}

	@Test
	void anAccountSignsInOnlyOnceTheEmailIsConfirmed()
	{
		tester.startPage(SignInPage.class);
		signIn();

		tester.assertRenderedPage(SignInPage.class);
		tester.assertContains("Please confirm your email first");
		assertFalse(DemoSession.get().isSignedIn());
		// the link of a moment ago still works: no new email yet
		assertEquals(1, mailer.getMails().size());

		tester.startPage(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, mailer.lastToken()));
		tester.startPage(SignInPage.class);
		signIn();

		assertEquals("ada@example.com", DemoSession.get().getAccount());
	}

	private void signIn()
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("name", "ada@example.com");
		form.setValue("password", PASSWORD);
		form.submit();
	}
}
