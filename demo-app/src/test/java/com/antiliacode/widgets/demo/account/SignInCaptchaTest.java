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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.DemoSession;
import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;

/**
 * The sign-in sent via Ajax, as the browser sends it, keeps the password typed when only the
 * captcha was wrong, and the captcha shows other letters, or a question instead, without sending
 * the form.
 */
class SignInCaptchaTest
{
	private static final String LETTERS_WRONG =
		"The letters are not those of the image. Try the new ones.";

	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication());
		tester.startPage(SignInPage.class);
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	/**
	 * Sends the form via Ajax, as the browser does. The messages shown before go first, as at the
	 * end of a request outside of the tester, which keeps them to be asserted.
	 */
	private void send(String name, String password, String answer)
	{
		tester.cleanupFeedbackMessages();
		FormTester form = tester.newFormTester("form");
		form.setValue("name", name);
		form.setValue("password", password);
		if (answer != null)
		{
			form.setValue("captcha:answer", answer);
		}
		tester.executeAjaxEvent("form", "submit");
	}

	private void failUntilTheCaptchaIsAsked()
	{
		for (int attempt = 0; attempt < SignInAttempts.BEFORE_CAPTCHA; attempt++)
		{
			send("wicket", "wrong-" + attempt, null);
		}
		tester.assertVisible("form:captcha");
	}

	private SignInPage page()
	{
		return (SignInPage)tester.getLastRenderedPage();
	}

	private String markupId(String path)
	{
		return tester.getComponentFromLastRenderedPage(path).getMarkupId();
	}

	/**
	 * @return whether the last Ajax response draws a component again
	 */
	private boolean drawn(String path)
	{
		return tester.getLastResponseAsString()
			.contains("<component id=\"" + markupId(path) + "\"");
	}

	/**
	 * @return the tag of a component in the last response
	 */
	private TagTester tag(String path)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id",
			markupId(path));
	}

	private void assertFocusOn(String path)
	{
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("Wicket.Focus.setFocusOnId('" + markupId(path) + "')"),
			response);
	}

	@Test
	void aWrongCaptchaKeepsTheNameAndThePasswordTyped()
	{
		failUntilTheCaptchaIsAsked();
		String letters = page().getChallenge();

		send("wicket", TestDatabase.WICKET_PASSWORD, "nope");

		tester.assertErrorMessages(LETTERS_WRONG);
		assertFalse(DemoSession.get().isSignedIn());
		// the field of the password is not drawn again: what was typed in it stays
		assertFalse(drawn("form:password"));
		assertTrue(drawn("form:errors"));
		assertTrue(drawn("form:captcha"));
		assertEquals("wicket", tag("form:name").getAttribute("value"));
		// other letters, and the field of the answer empty for them, with the focus
		assertNotEquals(letters, page().getChallenge());
		assertEquals("", tag("form:captcha:answer").getAttribute("value"));
		assertFocusOn("form:captcha:answer");

		send("wicket", TestDatabase.WICKET_PASSWORD, page().getChallenge().toLowerCase());
		assertTrue(DemoSession.get().isSignedIn());
		assertEquals("wicket", DemoSession.get().getAccount());
	}

	@Test
	void aWrongPasswordIsTypedAgain()
	{
		send("wicket", "wrong", null);

		tester.assertErrorMessages("The account or the password is wrong.");
		assertTrue(drawn("form:password"));
		assertEquals("", tag("form:password").getAttribute("value"));
		assertFocusOn("form:password");
		// the captcha is not asked yet
		tester.assertInvisible("form:captcha");

		send("wicket", "wrong", null);
		send("wicket", "wrong", null);
		// the third failure asks for it, and the password again
		assertTrue(drawn("form:captcha"));
		tester.assertVisible("form:captcha");
		assertTrue(drawn("form:password"));
		assertFocusOn("form:password");
	}

	@Test
	void aMissingNameKeepsThePassword()
	{
		send("", TestDatabase.WICKET_PASSWORD, null);

		tester.assertErrorMessages("Email or account is required.");
		assertFalse(drawn("form:password"));
		assertEquals("true", tag("form:name").getAttribute("aria-invalid"));
		assertFocusOn("form:name");
		// the mark of an error the field had while it was empty goes
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("document.getElementById(\"" + markupId("form:password") +
			"\").removeAttribute('aria-invalid');"), response);

		send("wicket", "", null);
		tester.assertErrorMessages("Password is required.");
		assertTrue(drawn("form:password"));
		assertEquals("true", tag("form:password").getAttribute("aria-invalid"));
		assertFocusOn("form:password");
	}

	@Test
	void otherLettersKeepWhatWasTyped()
	{
		failUntilTheCaptchaIsAsked();
		String letters = page().getChallenge();
		assertEquals(Captcha.LENGTH, letters.length());
		assertTrue(letters.chars().allMatch(letter -> Captcha.LETTERS.indexOf(letter) >= 0),
			letters);

		tester.clickLink("form:captcha:pictured:another", true);

		assertNotEquals(letters, page().getChallenge());
		// only the image and the field of the answer are drawn again
		assertTrue(drawn("form:captcha:pictured:image"));
		assertTrue(drawn("form:captcha:answer"));
		assertFalse(drawn("form:captcha"));
		assertFalse(drawn("form:name"));
		assertFalse(drawn("form:password"));
	}

	@Test
	void aQuestionCanBeAnsweredInsteadOfTheLetters()
	{
		failUntilTheCaptchaIsAsked();
		tester.assertContains("Hard to read\\? Answer a question instead");

		tester.clickLink("form:captcha:other", true);

		assertTrue(drawn("form:captcha"));
		assertFalse(drawn("form:password"));
		tester.assertInvisible("form:captcha:pictured");
		Captcha.Question question = page().getCaptcha().getQuestion();
		String[] numbers = { "zero", "one", "two", "three", "four", "five", "six", "seven",
				"eight", "nine", "ten" };
		tester.assertLabel("form:captcha:label", "How much is " + numbers[question.first()] +
			(question.sum() ? " plus " : " minus ") + numbers[question.second()] +
			"? Write it in digits.");
		assertEquals("numeric", tag("form:captcha:answer").getAttribute("inputmode"));
		assertFocusOn("form:captcha:answer");
		assertEquals(question.sum() ? question.first() + question.second()
			: question.first() - question.second(), question.answer());

		// a wrong answer asks another question
		send("wicket", TestDatabase.WICKET_PASSWORD, "99");
		tester.assertErrorMessages("The answer is wrong. Try the new question.");
		assertFalse(drawn("form:password"));
		assertNotNull(page().getCaptcha().getQuestion());

		send("wicket", TestDatabase.WICKET_PASSWORD, " " + page().getChallenge() + " ");
		assertTrue(DemoSession.get().isSignedIn());
	}

	@Test
	void theLettersComeBackInsteadOfTheQuestion()
	{
		failUntilTheCaptchaIsAsked();
		tester.clickLink("form:captcha:other", true);
		tester.assertContains("Type the letters of an image instead");

		tester.clickLink("form:captcha:other", true);

		tester.assertVisible("form:captcha:pictured");
		assertNull(page().getCaptcha().getQuestion());
		tester.assertLabel("form:captcha:label", "The letters of the image");
		assertEquals("characters", tag("form:captcha:answer").getAttribute("autocapitalize"));
		assertNull(tag("form:captcha:answer").getAttribute("inputmode"));
	}

	@Test
	void theQuestionIsAskedInTheLanguageOfThePage()
	{
		tester.getSession().setLocale(Locale.forLanguageTag("es"));
		tester.startPage(SignInPage.class);
		failUntilTheCaptchaIsAsked();
		tester.assertContains(
			"Demasiados intentos fallidos: escriba también las letras de la imagen.");

		tester.clickLink("form:captcha:other", true);

		Captcha.Question question = page().getCaptcha().getQuestion();
		String[] numbers = { "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete",
				"ocho", "nueve", "diez" };
		tester.assertLabel("form:captcha:label", "¿Cuánto es " + numbers[question.first()] +
			(question.sum() ? " más " : " menos ") + numbers[question.second()] +
			"? Escríbalo en cifras.");
		tester.assertContains("Demasiados intentos fallidos: responda también a la pregunta.");
	}
}
