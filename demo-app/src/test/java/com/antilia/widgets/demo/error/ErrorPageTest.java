/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.apache.wicket.Page;
import org.apache.wicket.settings.ApplicationSettings;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.WicketApplication;

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
