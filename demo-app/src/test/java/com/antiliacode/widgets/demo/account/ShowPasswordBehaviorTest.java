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
package com.antiliacode.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.TestDatabase;
import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.register.RegisterPage;

/**
 * The password fields of the sign-in and the registration get the button showing the password:
 * its labels in the language of the page, and the script that adds it.
 */
class ShowPasswordBehaviorTest
{
	private WicketTester tester;

	@BeforeEach
	void start()
	{
		TestDatabase.clear();
		tester = new WicketTester(new WicketApplication());
	}

	@AfterEach
	void stop()
	{
		tester.destroy();
	}

	private TagTester field(String name)
	{
		return TagTester.createTagByAttribute(tester.getLastResponseAsString(), "name", name);
	}

	@Test
	void theSignInShowsThePassword()
	{
		tester.startPage(SignInPage.class);

		TagTester password = field("password");
		assertEquals("password", password.getAttribute("type"));
		assertEquals("Show password", password.getAttribute(ShowPasswordBehavior.SHOW_ATTRIBUTE));
		assertEquals("Hide password", password.getAttribute(ShowPasswordBehavior.HIDE_ATTRIBUTE));
		String response = tester.getLastResponseAsString();
		assertTrue(response.contains("ShowPasswordBehavior/show-password"), response);
		assertTrue(response.contains("AntiliaShowPassword.attach(\"" +
			password.getAttribute("id") + "\");"), response);
	}

	@Test
	void theRegistrationShowsBothPasswordsInGerman()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(RegisterPage.class);

		for (String name : new String[] { "password", "repeatedPassword" })
		{
			TagTester password = field(name);
			assertEquals("Passwort anzeigen",
				password.getAttribute(ShowPasswordBehavior.SHOW_ATTRIBUTE), name);
			assertEquals("Passwort verbergen",
				password.getAttribute(ShowPasswordBehavior.HIDE_ATTRIBUTE), name);
			assertTrue(tester.getLastResponseAsString().contains(
				"AntiliaShowPassword.attach(\"" + password.getAttribute("id") + "\");"), name);
		}
	}

	@Test
	void spanishToo()
	{
		tester.getSession().setLocale(Locale.of("es"));
		tester.startPage(SignInPage.class);

		assertEquals("Mostrar la contraseña",
			field("password").getAttribute(ShowPasswordBehavior.SHOW_ATTRIBUTE));
		assertEquals("Ocultar la contraseña",
			field("password").getAttribute(ShowPasswordBehavior.HIDE_ATTRIBUTE));
	}
}
