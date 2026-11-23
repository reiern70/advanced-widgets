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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.HomePage;
import com.antiliacode.widgets.demo.TestAccounts;
import com.antiliacode.widgets.demo.TestClock;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;

/**
 * Signs in on the stateless {@link SignInPage}: as {@value Accounts#USER}, whose password the page
 * shows, or as the administrator, always with the captcha; an address failing 10 times is blocked
 * for 30 minutes, and at most 10 sessions are signed in as {@value Accounts#USER}.
 */
class SignInPageTest
{
	private final TestClock clock = new TestClock();

	/** The failed attempts of the application, on the clock of the test. */
	private final SignInAttempts attempts = new SignInAttempts(clock);

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		tester = new WicketTester(new WicketApplication()
		{
			@Override
			public SignInAttempts getSignInAttempts()
			{
				return attempts;
			}
		});
		tester.getSession().setLocale(Locale.ENGLISH);
		tester.startPage(SignInPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/**
	 * @return the token of the captcha of the page shown last
	 */
	private String token()
	{
		return ((SignInPage)tester.getLastRenderedPage()).getCaptcha().getToken();
	}

	/**
	 * @return the letters of the captcha of the page shown last
	 */
	private String letters()
	{
		return CaptchaToken.letters(token(), Instant.now()).orElseThrow();
	}

	/**
	 * Sends the form of the page shown last.
	 */
	private void send(String name, String password, String captcha)
	{
		tester.cleanupFeedbackMessages();
		FormTester form = tester.newFormTester("form");
		form.setValue("name", name);
		form.setValue("password", password);
		form.setValue("captcha:answer", captcha);
		form.submit();
	}

	private void signInAsUser()
	{
		send("user", Accounts.USER_PASSWORD, letters());
	}

	@Test
	void aVisitorWhoOnlyLooksGetsNoSession()
	{
		assertTrue(tester.getLastRenderedPage().isPageStateless());
		assertTrue(tester.getSession().isTemporary());
	}

	@Test
	void thePageShowsTheAccountTheSessionsInUseAndThatNothingIsRecorded()
	{
		tester.assertLabel("user", "user");
		tester.assertLabel("userPassword", "$Amigo_MEllon_Friend_4321");
		tester.assertLabel("inUse", "0 of 10 sessions in use.");
		tester.assertContains("We keep no record of the people who visit this site.");
		// the captcha always, an image drawn from the token in its address
		tester.assertVisible("form:captcha");
		tester.assertContains(Pattern.quote("captcha?t="));
		assertFalse(tester.getLastResponseAsString().contains(letters()));
	}

	@Test
	void everybodySignsInAsUserWithTheCaptcha()
	{
		signInAsUser();

		tester.assertRenderedPage(HomePage.class);
		assertEquals("user", DemoSession.get().getAccount());
		assertFalse(DemoSession.get().isAdmin());
		assertEquals(1, SignedInUsers.get().count());
	}

	@Test
	void theAdministratorSignsInWithThePasswordOfTheServer()
	{
		send("admin", TestAccounts.ADMIN_PASSWORD, letters());

		tester.assertRenderedPage(HomePage.class);
		assertTrue(DemoSession.get().isAdmin());
		assertEquals(0, SignedInUsers.get().count());
	}

	@Test
	void aWrongCaptchaIsTurnedAwayWithoutCheckingThePassword()
	{
		String before = token();
		send("user", Accounts.USER_PASSWORD, "WRONG");

		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("The letters are not those of the image. Try the new ones.");
		assertFalse(DemoSession.get().isSignedIn());
		assertNotEquals(before, token(), "other letters");
	}

	@Test
	void theLettersAreTakenInLowerCaseToo()
	{
		send("user", Accounts.USER_PASSWORD, " " + letters().toLowerCase(Locale.ROOT) + " ");

		tester.assertRenderedPage(HomePage.class);
	}

	@Test
	void aCaptchaAnswersOnce()
	{
		String token = token();
		String letters = letters();
		send("user", "wrong-password", letters);
		tester.assertErrorMessages("The account or the password is wrong.");

		// the same token and letters again, as a robot would send them
		tester.cleanupFeedbackMessages();
		FormTester form = tester.newFormTester("form");
		form.setValue("captcha:token", token);
		form.setValue("name", "user");
		form.setValue("password", Accounts.USER_PASSWORD);
		form.setValue("captcha:answer", letters);
		form.submit();

		tester.assertErrorMessages("The letters are not those of the image. Try the new ones.");
		assertFalse(DemoSession.get().isSignedIn());
	}

	@Test
	void aWrongPasswordIsTurnedAway()
	{
		send("user", "not-the-password", letters());

		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("The account or the password is wrong.");
		assertFalse(DemoSession.get().isSignedIn());
		assertTrue(tester.getSession().isTemporary(), "still no session");
	}

	@Test
	void tenFailuresBlockTheAddressForThirtyMinutes()
	{
		for (int i = 0; i < SignInAttempts.MAXIMUM_FAILURES; i++)
		{
			send("user", "wrong-" + i, i % 2 == 0 ? letters() : "WRONG");
		}

		// the right password and letters are not even checked
		signInAsUser();
		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("Too many failed attempts: please try again in 30 minutes.");
		assertFalse(DemoSession.get().isSignedIn());

		clock.advance(SignInAttempts.BLOCKED);
		signInAsUser();
		tester.assertRenderedPage(HomePage.class);
	}

	@Test
	void anEleventhSessionWaitsButTheAdministratorSignsIn()
	{
		for (int i = 1; i <= SignedInUsers.MAXIMUM; i++)
		{
			String id = "session-" + i;
			assertTrue(SignedInUsers.get().signIn(Role.USER, () -> id));
		}
		tester.startPage(SignInPage.class);
		tester.assertLabel("inUse", "10 of 10 sessions in use.");

		signInAsUser();
		tester.assertRenderedPage(SignInPage.class);
		tester.assertErrorMessages("All 10 sessions are in use right now. Please try again in 30 " +
			"minutes.");
		assertFalse(DemoSession.get().isSignedIn());

		send("admin", TestAccounts.ADMIN_PASSWORD, letters());
		tester.assertRenderedPage(HomePage.class);
		assertTrue(DemoSession.get().isAdmin());
	}

	@Test
	void afterSigningInTheVisitorGoesOnToThePageAskedFor()
	{
		tester.startPage(SignInPage.class, new PageParameters().add(SignInPage.TO, "dropdown"));
		signInAsUser();

		tester.assertRenderedPage(DropdownPage.class);
	}

	@Test
	void anotherSiteIsNeverGoneOnTo()
	{
		for (String to : new String[] { "//evil.example.com", "https://evil.example.com",
			"/\\evil.example.com", "../admin" })
		{
			tester.startPage(SignInPage.class, new PageParameters().add(SignInPage.TO, to));
			signInAsUser();

			tester.assertRenderedPage(HomePage.class);
			DemoSession.get().signOut();
		}
	}
}
