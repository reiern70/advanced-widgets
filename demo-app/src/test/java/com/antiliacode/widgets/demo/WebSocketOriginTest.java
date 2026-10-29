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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * The websocket of the home page, in the server: it connects from a page of the site and pushes
 * that the site is live; from a page of another site it is closed at once, see
 * {@link WebSocketOriginFilter}.
 */
class WebSocketOriginTest extends JettyTestCase
{
	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void connectsFromAPageOfTheSite() throws Exception
	{
		assertEquals(LiveResource.LIVE, connect("http://localhost:" + localPort));
	}

	@Test
	void closesAtOnceFromAPageOfAnotherSite() throws Exception
	{
		assertEquals("closed 1008", connect("https://elsewhere.example"));
	}

	/**
	 * Connects to the websocket of the home page in a session, from a page of an origin.
	 *
	 * @return the first message, or how the connection was closed
	 */
	private String connect(String origin) throws Exception
	{
		// a session, as the page has, from a page that keeps one
		String cookie = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" +
			localPort + "/sign-in")).build(), HttpResponse.BodyHandlers.discarding())
			.headers().allValues("Set-Cookie").stream()
			.filter(each -> each.startsWith("JSESSIONID="))
			.findFirst()
			.orElseThrow()
			.split(";")[0];
		CompletableFuture<String> first = new CompletableFuture<>();
		WebSocket socket = client.newWebSocketBuilder()
			.header("Cookie", cookie)
			.header("Origin", origin)
			.buildAsync(URI.create("ws://localhost:" + localPort + "/wicket/websocket?" +
				"resourceName=" + LiveResource.NAME + "&wicket-ajax-baseurl=." +
				"&wicket-app-name=wicket.demo-app"), new WebSocket.Listener()
				{
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
						boolean last)
					{
						first.complete(data.toString());
						return null;
					}

					@Override
					public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
						String reason)
					{
						first.complete("closed " + statusCode);
						return null;
					}
				})
			.get(10, TimeUnit.SECONDS);
		try
		{
			return first.get(10, TimeUnit.SECONDS);
		}
		finally
		{
			socket.abort();
		}
	}
}
