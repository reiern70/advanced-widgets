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
package com.antiliacode.widgets.demo.resumable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.antiliacode.widgets.upload.ResumableUploadResourceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.demo.JettyTestCase;
import com.github.openjson.JSONObject;

/**
 * The tus protocol of the demo's resumable uploads, Wicket's resource of them, through Jetty and
 * Wicket's filter, as the React and Svelte apps send it with the query the page signed for them:
 * the methods HEAD, PATCH and DELETE reach the resource, the refusals carry the keys of their
 * errors, and the receipts resource hands over a file stored whole.
 */
class ResumableUploadHttpTest extends JettyTestCase
{
	private final HttpClient client = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NEVER).build();

	private String cookie;

	/** The signed query of the apps of the session. */
	private String query;

	@BeforeEach
	void openThePage() throws Exception
	{
		// the page binds the session the uploads belong to
		HttpResponse<String> page = send(HttpRequest.newBuilder(uri("/resumable-upload")).GET());
		cookie = page.headers().allValues("Set-Cookie").stream()
			.filter(value -> value.startsWith("JSESSIONID=")).map(value -> value.split(";")[0])
			.findFirst().orElseThrow(() -> new AssertionError(page.headers().map().toString()));
		// the page itself, where the first request is sent on to its version
		String body = page.statusCode() == 302 ? send(HttpRequest.newBuilder(uri("/")
			.resolve(header(page, "Location"))).GET()).body() : page.body();
		String marker = "data-config=\"";
		int start = body.indexOf(marker) + marker.length();
		String config = body.substring(start, body.indexOf('"', start))
			.replace("&quot;", "\"").replace("&amp;", "&").replace("&#039;", "'")
			.replace("&lt;", "<").replace("&gt;", ">");
		query = new JSONObject(config).getString("query");
	}

	private URI uri(String path)
	{
		return URI.create("http://localhost:" + localPort + path);
	}

	private HttpResponse<String> send(HttpRequest.Builder request) throws Exception
	{
		if (cookie != null)
		{
			request.header("Cookie", cookie);
		}
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpRequest.Builder tus(String path)
	{
		return HttpRequest.newBuilder(uri(path + "?" + query)).header("Tus-Resumable", "1.0.0");
	}

	private HttpResponse<String> receipt(String name) throws Exception
	{
		return send(HttpRequest.newBuilder(uri("/" + ReceiptsResource.PATH))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(BodyPublishers.ofString(ReceiptsResource.NAME_PARAMETER + "=" +
				URLEncoder.encode(name, StandardCharsets.UTF_8) + "&" +
				ReceiptsResource.CLIENT_NAME_PARAMETER + "=hello.txt")));
	}

	private static String header(HttpResponse<?> response, String name)
	{
		return response.headers().firstValue(name).orElse(null);
	}

	@Test
	void sendsAFileInChunksThroughJettyAndHandsItOver() throws Exception
	{
		// Wicket's filter answers OPTIONS itself, PATCH and DELETE among the methods it allows
		HttpResponse<String> options = send(HttpRequest.newBuilder(uri("/" +
			DemoResumableUploads.PATH)).method("OPTIONS", BodyPublishers.noBody()));
		assertEquals(200, options.statusCode());
		assertTrue(header(options, "Allow").contains("PATCH"), header(options, "Allow"));

		String metadata = "filename " + Base64.getEncoder().encodeToString(
			"hello.txt".getBytes(StandardCharsets.UTF_8));
		HttpResponse<String> created = send(tus("/" + DemoResumableUploads.PATH)
			.header("Upload-Length", "11").header("Upload-Metadata", metadata)
			.POST(BodyPublishers.noBody()));
		assertEquals(201, created.statusCode(), created.body());
		String location = header(created, "Location");
		assertTrue(location.startsWith("/" + DemoResumableUploads.PATH + "/"), location);

		HttpResponse<String> first = send(tus(location).header("Upload-Offset", "0")
			.header("Content-Type", "application/offset+octet-stream")
			.method("PATCH", BodyPublishers.ofString("hello ")));
		assertEquals(204, first.statusCode(), first.headers().map() + " " + first.body());
		assertEquals("6", header(first, "Upload-Offset"));

		HttpResponse<String> head = send(tus(location).method("HEAD", BodyPublishers.noBody()));
		assertEquals(200, head.statusCode());
		assertEquals("6", header(head, "Upload-Offset"));
		assertEquals("11", header(head, "Upload-Length"));
		assertTrue(header(head, "Cache-Control").contains("no-store"));

		// by POST, as through a proxy refusing PATCH
		HttpResponse<String> last = send(tus(location).header("Upload-Offset", "6")
			.header("X-HTTP-Method-Override", "PATCH")
			.header("Content-Type", "application/offset+octet-stream")
			.POST(BodyPublishers.ofString("world")));
		assertEquals(204, last.statusCode(), last.body());
		assertEquals("11", header(last, "Upload-Offset"));
		String name = header(last, ResumableUploadResourceReference.FILE_NAME_HEADER);

		HttpResponse<String> receipt = receipt(name);
		assertEquals(200, receipt.statusCode(), receipt.body());
		JSONObject json = new JSONObject(receipt.body());
		assertEquals("hello.txt", json.getString("name"));
		assertEquals(11, json.getLong("size"));
		assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
			json.getString("sha256"));
		// handed over once, and gone from the server
		assertEquals(404, receipt(name).statusCode());
		HttpResponse<String> done = send(tus(location).method("HEAD", BodyPublishers.noBody()));
		assertEquals("11", header(done, "Upload-Offset"), "whole, should the app ask again");
	}

	@Test
	void tellsWhyItRefusesByTheKeysOfTheErrors() throws Exception
	{
		HttpResponse<String> large = send(tus("/" + DemoResumableUploads.PATH)
			.header("Upload-Length", String.valueOf(200L * 1024 * 1024))
			.POST(BodyPublishers.noBody()));
		assertEquals(413, large.statusCode());
		assertEquals("resumableUploadTooLarge",
			header(large, ResumableUploadResourceReference.ERROR_HEADER));

		String signed = query;
		query = signed.replace("maxSize=", "maxSize=9");
		HttpResponse<String> raised = send(tus("/" + DemoResumableUploads.PATH)
			.header("Upload-Length", "1").POST(BodyPublishers.noBody()));
		assertEquals(403, raised.statusCode(), "a query the page did not sign");
		query = signed;

		HttpResponse<String> plain = send(HttpRequest.newBuilder(uri("/" +
			DemoResumableUploads.PATH + "?" + query)).header("Upload-Length", "1")
			.POST(BodyPublishers.noBody()));
		assertEquals(412, plain.statusCode(), "without Tus-Resumable, as a form of another site");
	}

	@Test
	void deletesAnUploadAndKeepsThoseOfASessionForIt() throws Exception
	{
		HttpResponse<String> created = send(tus("/" + DemoResumableUploads.PATH)
			.header("Upload-Length", "5").POST(BodyPublishers.noBody()));
		String location = header(created, "Location");

		String mine = cookie;
		String signed = query;
		cookie = null;
		openThePage();
		assertFalse(mine.equals(cookie));
		assertEquals(404, send(tus(location).method("HEAD", BodyPublishers.noBody()))
			.statusCode(), "another session");
		cookie = mine;
		query = signed;

		assertEquals(204, send(tus(location).DELETE()).statusCode());
		assertEquals(404, send(tus(location).method("HEAD", BodyPublishers.noBody()))
			.statusCode());
	}
}
