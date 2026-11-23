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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.CaptchaToken;

/**
 * Signs in over HTTP, as a browser does, for the tests of a server: the only way to a session,
 * which a visitor who only looks never gets.
 */
final class HttpSignIn
{
	private static final Pattern INPUT = Pattern.compile("<input[^>]*>");

	private HttpSignIn()
	{
	}

	/**
	 * @return the cookie of a session signed in as user, {@code JSESSIONID=...}
	 */
	static String cookie(int localPort) throws Exception
	{
		List<String> cookies = signIn(localPort).headers().allValues("Set-Cookie");
		return cookies.stream().filter(cookie -> cookie.startsWith("JSESSIONID="))
			.findFirst().orElseThrow(() -> new AssertionError(cookies.toString()))
			.split(";")[0];
	}

	/**
	 * Signs in as user, as a browser sends the form of the sign-in page, the letters of the
	 * captcha read from its token: the test runs in the server's JVM, which alone has the key.
	 */
	static HttpResponse<String> signIn(int localPort) throws Exception
	{
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
			.build();
		String page = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + localPort +
			"/sign-in")).build(), HttpResponse.BodyHandlers.ofString()).body();
		Matcher form = Pattern.compile("<form[^>]*register__form[^>]*>").matcher(page);
		assertTrue(form.find(), page);
		String action = attribute(form.group(), "action");
		Map<String, String> fields = new LinkedHashMap<>();
		Matcher input = INPUT.matcher(page.substring(form.end()));
		String token = null;
		while (input.find())
		{
			String tag = input.group();
			String name = attribute(tag, "name");
			if (name == null)
			{
				continue;
			}
			if ("username".equals(attribute(tag, "autocomplete")))
			{
				fields.put(name, Accounts.USER);
			}
			else if ("password".equals(attribute(tag, "type")))
			{
				fields.put(name, Accounts.USER_PASSWORD);
			}
			else if (name.endsWith("token"))
			{
				token = attribute(tag, "value");
				fields.put(name, token);
			}
		}
		fields.put("captcha:answer", CaptchaToken.letters(token, Instant.now()).orElseThrow());
		String body = fields.entrySet().stream()
			.map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" +
				URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8))
			.collect(Collectors.joining("&"));
		URI uri = URI.create("http://localhost:" + localPort + "/sign-in").resolve(action);
		return client.send(HttpRequest.newBuilder(uri)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.header("Origin", "http://localhost:" + localPort)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String attribute(String tag, String name)
	{
		Matcher matcher = Pattern.compile("\\s" + name + "=\"([^\"]*)\"").matcher(tag);
		return matcher.find() ? matcher.group(1).replace("&amp;", "&") : null;
	}

}
