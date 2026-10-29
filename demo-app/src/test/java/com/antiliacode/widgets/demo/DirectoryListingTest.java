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
package com.antiliacode.widgets.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A folder of the web application lists none of its files: its images are served one by one, by
 * their names.
 */
class DirectoryListingTest extends JettyTestCase
{
	@ParameterizedTest
	@ValueSource(strings = { "/images/", "/images/demos/" })
	void aFolderListsNoFiles(String folder) throws Exception
	{
		HttpResponse<String> response = get(folder);

		assertEquals(403, response.statusCode());
		assertFalse(response.body().contains(".png"), response.body());
		assertFalse(response.body().contains("Directory: "), response.body());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/images/demos/buttons.png", "/favicon.svg" })
	void aFileOfAFolderIsServed(String file) throws Exception
	{
		HttpResponse<String> response = get(file);

		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/"));
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
