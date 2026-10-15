/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The session lives in a cookie only, which the scripts of the page cannot read: the session id
 * never shows in a URL, where a link copied or a Referer would give it away. A client without the
 * cookie gets the page it asked for rather than a redirect it would follow without the cookie.
 */
class SessionCookieTest extends JettyTestCase
{
	@Test
	void aRequestWithoutTheCookieGetsThePageWithoutTheSessionIdInItsLinks() throws Exception
	{
		HttpResponse<String> response = get("/sign-in");

		// no redirect to /sign-in?0, which a client keeping no cookies would never get past
		assertEquals(200, response.statusCode());
		assertFalse(response.body().toLowerCase().contains("jsessionid"), response.body());
	}

	@Test
	void aRequestWithTheCookieIsRedirectedToThePageWithoutTheSessionIdInTheUrl() throws Exception
	{
		String cookie = get("/sign-in").headers().allValues("Set-Cookie").stream()
			.filter(each -> each.startsWith("JSESSIONID=")).findFirst().orElseThrow()
			.split(";")[0];

		HttpResponse<String> response = get("/sign-in", cookie);

		assertEquals(302, response.statusCode());
		String location = response.headers().firstValue("Location").orElseThrow();
		assertTrue(location.matches(".*sign-in\\?\\d+"), location);
		assertFalse(location.toLowerCase().contains("jsessionid"), location);
	}

	@Test
	void theSessionCookieIsHttpOnlyAndSameSiteLax() throws Exception
	{
		HttpResponse<String> response = get("/sign-in");

		List<String> cookies = response.headers().allValues("Set-Cookie");
		String session = cookies.stream().filter(cookie -> cookie.startsWith("JSESSIONID="))
			.findFirst().orElseThrow(() -> new AssertionError(cookies.toString()));
		String attributes = session.toLowerCase();
		assertTrue(attributes.contains("; httponly"), session);
		assertTrue(attributes.contains("; samesite=lax"), session);
		assertFalse(attributes.contains("comment"), session);
	}

	private HttpResponse<String> get(String path, String... cookie) throws Exception
	{
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
			.build();
		HttpRequest.Builder request = HttpRequest
			.newBuilder(URI.create("http://localhost:" + localPort + path));
		if (cookie.length > 0)
		{
			request.header("Cookie", cookie[0]);
		}
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
