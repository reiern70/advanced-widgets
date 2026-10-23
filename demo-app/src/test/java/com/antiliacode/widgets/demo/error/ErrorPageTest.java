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
package com.antiliacode.widgets.demo.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.apache.wicket.Page;
import org.apache.wicket.settings.ApplicationSettings;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.WicketApplication;

/**
 * The pages telling what went wrong are the site's, and answer with the status of it.
 */
class ErrorPageTest extends WicketTestCase
{
	@Override
	protected WicketApplication newApplication()
	{
		return new WicketApplication();
	}

	@Test
	void wicketShowsThePagesOfTheSite()
	{
		ApplicationSettings settings = tester.getApplication().getApplicationSettings();

		assertEquals(InternalErrorPage.class, settings.getInternalErrorPage());
		assertEquals(PageExpiredPage.class, settings.getPageExpiredErrorPage());
		assertEquals(AccessDeniedPage.class, settings.getAccessDeniedPage());
	}

	@ParameterizedTest
	@CsvSource({ "NotFoundPage, 404, Page not found", "InternalErrorPage, 500, Something went wrong",
			"AccessDeniedPage, 403, Not for this account",
			"PageExpiredPage, 200, This page has expired" })
	void tellsWhatWentWrongWithItsStatus(String page, int status, String title) throws Exception
	{
		@SuppressWarnings("unchecked")
		Class<? extends Page> type = (Class<? extends Page>)Class
			.forName(ErrorPage.class.getPackageName() + "." + page);
		tester.startPage(type);

		assertEquals(status, tester.getLastResponse().getStatus());
		tester.assertContains("<h1>" + title + "</h1>");
		tester.assertContains("<title>" + title + " · ");
	}

	@Test
	void speaksTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(NotFoundPage.class);

		tester.assertContains("<h1>Seite nicht gefunden</h1>");
		tester.assertContains("Zurück zu den Demos");
	}
}
