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
package com.antiliacode.widgets.demo.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.RecordingMailer;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.data.IUserRepository;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.User;

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
		assertTrue(html.contains("Hola:"), html);
		assertTrue(html.contains("Confirmar mi correo"), html);
		assertTrue(html.contains("48 horas"), html);
		assertFalse(html.contains("wicket:"), html);
		assertTrue(mailer.last().text().contains("Hola:"), mailer.last().text());
		// nothing the visitor typed, not even the name
		assertFalse(html.contains("Ada"), html);
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
	void theButtonOfTheLinkConfirmsTheEmailOnce()
	{
		openTheLink();

		// opening the link, as programs checking the links of emails do, validates nothing
		tester.assertRenderedPage(EmailValidationPage.class);
		tester.assertVisible("confirm");
		tester.assertInvisible("validated");
		tester.assertInvisible("invalid");
		tester.assertContains("ada@example.com");
		assertFalse(Accounts.get().find("ada@example.com").orElseThrow().isValidated());
		openTheLink();
		tester.assertVisible("confirm");

		// the button does, with POST
		tester.newFormTester("confirm:form").submit();

		tester.assertRenderedPage(EmailValidationPage.class);
		tester.assertVisible("validated");
		tester.assertInvisible("confirm");
		tester.assertInvisible("invalid");
		assertTrue(Accounts.get().find("ada@example.com").orElseThrow().isValidated());
		// it does not sign in by itself
		assertFalse(DemoSession.get().isSignedIn());

		openTheLink();
		tester.assertVisible("invalid");
		tester.assertInvisible("confirm");
		tester.assertInvisible("validated");
	}

	@Test
	void theButtonOfALinkUsedMeanwhileSaysItDoesNotWork()
	{
		openTheLink();
		Accounts.get().validate(mailer.lastToken());

		tester.newFormTester("confirm:form").submit();

		tester.assertVisible("invalid");
		tester.assertInvisible("validated");
	}

	@Test
	void theConfirmationOfTheEmailIsShownOnlyAfterRegistering()
	{
		tester.startPage(EmailConfirmationPage.class);

		assertEquals(400, tester.getLastResponse().getStatus());
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

		openTheLink();
		tester.newFormTester("confirm:form").submit();
		tester.startPage(SignInPage.class);
		signIn();

		assertEquals("ada@example.com", DemoSession.get().getAccount());
	}

	@Test
	void signingInSendsANewLinkUnlessTheLimitsKeepItBack()
	{
		// the last link is old enough for a new one
		IUserRepository users = TestDatabase.context().getBean(IUserRepository.class);
		User ada = Accounts.get().find("ada@example.com").orElseThrow();
		ada.setValidation(ada.getValidationToken(), Instant.now().plus(Accounts.VALIDATION_TIME)
			.minus(Accounts.RESEND_AFTER).minusSeconds(60));
		users.save(ada);
		// the emails to ada@example.com this hour, but one
		for (int i = 2; i < RegistrationLimits.MAILS_PER_RECIPIENT; i++)
		{
			assertTrue(RegistrationLimits.get().mail(null, "ada@example.com"));
		}

		tester.startPage(SignInPage.class);
		signIn();
		assertEquals(2, mailer.getMails().size());
		String token = mailer.lastToken();

		// no more emails to ada@example.com this hour: the link sent last keeps working
		ada = Accounts.get().find("ada@example.com").orElseThrow();
		ada.setValidation(ada.getValidationToken(), Instant.now().plus(Accounts.VALIDATION_TIME)
			.minus(Accounts.RESEND_AFTER).minusSeconds(60));
		users.save(ada);
		tester.startPage(SignInPage.class);
		signIn();
		tester.assertContains("Please confirm your email first");
		assertEquals(2, mailer.getMails().size());
		assertTrue(Accounts.get().findValidation(token).isPresent());
	}

	private void openTheLink()
	{
		tester.startPage(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, mailer.lastToken()));
	}

	private void signIn()
	{
		FormTester form = tester.newFormTester("form");
		form.setValue("name", "ada@example.com");
		form.setValue("password", PASSWORD);
		form.submit();
	}
}
