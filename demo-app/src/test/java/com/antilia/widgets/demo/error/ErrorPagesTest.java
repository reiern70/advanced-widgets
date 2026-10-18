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
package com.antilia.widgets.demo.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.antilia.widgets.demo.JettyTestCase;

/**
 * What goes wrong shows in the look of the site, with its status, and the browser's tab shows the
 * cogs of the logo.
 */
class ErrorPagesTest extends JettyTestCase
{
	@Test
	void anAddressWithoutAPageAnswers404WithThePageOfTheSite() throws Exception
	{
		HttpResponse<String> response = get("/no-such-page");

		assertEquals(404, response.statusCode());
		String body = response.body();
		assertTrue(body.contains("Page not found"), body);
		assertTrue(body.contains("Antilia's Advanced Widgets"), body);
		assertFalse(body.contains("Powered by"), body);
	}

	@Test
	void theFaviconIsServedAsSvgAndAsIco() throws Exception
	{
		HttpResponse<String> svg = get("/favicon.svg");
		assertEquals(200, svg.statusCode());
		assertTrue(svg.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));
		assertEquals(200, get("/favicon.ico").statusCode());
		assertTrue(get("/").body().contains("<link rel=\"icon\" href=\"/favicon.svg\""));
	}

	private HttpResponse<String> get(String path) throws Exception
	{
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
			.build();
		return client.send(
			HttpRequest.newBuilder(URI.create("http://localhost:" + localPort + path)).build(),
			HttpResponse.BodyHandlers.ofString());
	}
}
