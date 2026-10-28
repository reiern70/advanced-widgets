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
package com.antiliacode.widgets.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class LanguageToolProofreaderTest
{
	private HttpServer server;

	/** The form the server was posted last. */
	private Map<String, String> posted;

	private int status = 200;

	private String answer = "{}";

	@BeforeEach
	void start() throws IOException
	{
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v2/check", exchange -> {
			posted = new LinkedHashMap<>();
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			for (String pair : body.split("&"))
			{
				String[] parts = pair.split("=", 2);
				posted.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
					URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
			}
			posted.put("method", exchange.getRequestMethod());
			byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody())
			{
				out.write(bytes);
			}
		});
		server.start();
	}

	@AfterEach
	void stop()
	{
		server.stop(0);
	}

	private URI url()
	{
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v2/check");
	}

	@Test
	void asksAboutTheTextsAtOnceAndSplitsTheAnswer() throws IOException
	{
		answer = "{\"matches\":[" +
			"{\"offset\":6,\"length\":5,\"message\":\"Possible spelling mistake found.\"," +
			"\"replacements\":[{\"value\":\"world\"},{\"value\":\"word\"}]," +
			"\"rule\":{\"id\":\"MORFOLOGIK_RULE_EN_GB\",\"issueType\":\"misspelling\"}}," +
			"{\"offset\":13,\"length\":7,\"message\":\"Agreement\",\"replacements\":[{\"value\":" +
			"\"they are\"}],\"rule\":{\"id\":\"AGREEMENT\",\"issueType\":\"grammar\"}}," +
			"{\"offset\":23,\"length\":2,\"replacements\":[],\"rule\":{\"issueType\":\"whitespace\"}}," +
			"{\"offset\":\"x\",\"length\":2}," +
			"{\"offset\":24,\"length\":40}]}";
		LanguageToolProofreader proofreader = new LanguageToolProofreader(url())
			.setParameter("motherTongue", "de").setParameter("level", "picky")
			.setParameter("level", null).setTimeout(Duration.ofSeconds(5));

		List<List<RichTextIssue>> found = proofreader.check(List.of("Hello wrold", "they is",
			"a  b"), Locale.UK);
		assertEquals(List.of(
			List.of(new RichTextIssue(6, 11, RichTextIssueKind.SPELLING,
				"Possible spelling mistake found.", List.of("world", "word"), "MORFOLOGIK_RULE_EN_GB")),
			List.of(new RichTextIssue(0, 7, RichTextIssueKind.GRAMMAR, "Agreement",
				List.of("they are"), "AGREEMENT")),
			List.of(new RichTextIssue(1, 3, RichTextIssueKind.STYLE, null, List.of(), null))), found);
		assertEquals("POST", posted.get("method"));
		assertEquals("Hello wrold\n\nthey is\n\na  b", posted.get("text"));
		assertEquals("en-GB", posted.get("language"));
		assertEquals("de", posted.get("motherTongue"));
		assertTrue(!posted.containsKey("level"));
		assertEquals(Map.of("motherTongue", "de"), proofreader.getParameters());
	}

	@Test
	void letsLanguageToolFindTheLanguageAndFailsAsItDoes() throws IOException
	{
		LanguageToolProofreader proofreader = new LanguageToolProofreader(url());
		assertEquals(List.of(List.of()), proofreader.check(List.of("a"), null));
		assertEquals("auto", posted.get("language"));
		assertEquals(List.of(), proofreader.check(List.of(), null));

		status = 429;
		IOException failed = assertThrows(IOException.class, () -> proofreader.check(List.of("a"),
			Locale.GERMAN));
		assertEquals("LanguageTool answered 429", failed.getMessage());

		status = 200;
		answer = "<html>";
		assertThrows(IOException.class, () -> proofreader.check(List.of("a"), Locale.GERMAN));
	}

	@Test
	void setsNeitherTheTextNorTheLanguageAsParameters()
	{
		LanguageToolProofreader proofreader = new LanguageToolProofreader(
			LanguageToolProofreader.PUBLIC_API);
		assertThrows(IllegalArgumentException.class, () -> proofreader.setParameter("text", "a"));
		assertThrows(IllegalArgumentException.class, () -> proofreader.setParameter("language",
			"en"));
		assertEquals(LanguageToolProofreader.PUBLIC_API, proofreader.getUrl());
		assertEquals(LanguageToolProofreader.DEFAULT_TIMEOUT, proofreader.getTimeout());
		// kept in the page
		assertEquals(LanguageToolProofreader.PUBLIC_API,
			WicketObjects.cloneObject(proofreader).getUrl());
	}

	@Test
	void namesTheKindsOfLanguageToolsTypesOfIssues()
	{
		assertEquals(RichTextIssueKind.SPELLING, LanguageToolProofreader.kind("misspelling"));
		assertEquals(RichTextIssueKind.STYLE, LanguageToolProofreader.kind("typographical"));
		assertEquals(RichTextIssueKind.STYLE, LanguageToolProofreader.kind("style"));
		assertEquals(RichTextIssueKind.GRAMMAR, LanguageToolProofreader.kind("duplication"));
		assertEquals(RichTextIssueKind.GRAMMAR, LanguageToolProofreader.kind(null));
	}
}
