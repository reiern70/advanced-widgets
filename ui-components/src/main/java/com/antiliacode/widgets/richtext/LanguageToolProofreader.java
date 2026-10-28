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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

/**
 * Checks texts with LanguageTool, open source (LGPL 2.1): its public server,
 * {@link #PUBLIC_API}, or one of the application's own, which runs apart
 * ({@code java -cp languagetool-server.jar org.languagetool.server.HTTPServer --port 8081}). The
 * library talks to it over HTTP, {@code POST /v2/check}, and carries none of it.
 * <p>
 * The texts are checked in one request, apart by blank lines, which LanguageTool takes as
 * paragraphs. The kind of an issue is the type of its rule: {@code misspelling} is spelling, those
 * of style ({@code style}, {@code register}, {@code typographical}, {@code whitespace}...) style,
 * the others grammar. The public server takes about 20 requests a minute, of 20 KB at most, and
 * hands the texts to a third party: for trying, not for production. Premium takes
 * {@code username} and {@code apiKey} as {@link #setParameter(String, String) parameters}.
 *
 * @since 11.0.0
 */
public class LanguageToolProofreader implements IRichTextProofreader
{
	private static final long serialVersionUID = 1L;

	/** The check of LanguageTool's public server. */
	public static final URI PUBLIC_API = URI.create("https://api.languagetool.org/v2/check");

	/** How long LanguageTool may take to answer, unless set otherwise. */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

	/** LanguageTool's types of issues that are of style; the others are of grammar. */
	private static final Set<String> STYLE = Set.of("style", "register", "locale-violation",
		"terminology", "typographical", "whitespace", "inconsistency");

	private final URI url;

	private final Map<String, String> parameters = new LinkedHashMap<>();

	private Duration timeout = DEFAULT_TIMEOUT;

	/**
	 * @param url
	 *            the address of LanguageTool's check: {@link #PUBLIC_API}, or
	 *            {@code http://localhost:8081/v2/check} for a server of the application's own
	 */
	public LanguageToolProofreader(URI url)
	{
		this.url = Args.notNull(url, "url");
	}

	/**
	 * @return the address of LanguageTool's check
	 */
	public URI getUrl()
	{
		return url;
	}

	/**
	 * @param name
	 *            a parameter of LanguageTool's check: {@code motherTongue}, {@code level}
	 *            ({@code picky}), {@code enabledRules}, {@code username}, {@code apiKey}...; not
	 *            {@code text} nor {@code language}, which the proofreader sets
	 * @param value
	 *            its value, {@code null} to take it away
	 * @return {@code this}
	 */
	public LanguageToolProofreader setParameter(String name, String value)
	{
		Args.notEmpty(name, "name");
		if ("text".equals(name) || "language".equals(name) || "data".equals(name))
		{
			throw new IllegalArgumentException("The proofreader sets " + name + " itself");
		}
		if (value == null)
		{
			parameters.remove(name);
		}
		else
		{
			parameters.put(name, value);
		}
		return this;
	}

	/**
	 * @return the parameters set, besides the text and the language
	 */
	public Map<String, String> getParameters()
	{
		return Collections.unmodifiableMap(parameters);
	}

	/**
	 * @return how long LanguageTool may take to answer
	 */
	public Duration getTimeout()
	{
		return timeout;
	}

	/**
	 * @param timeout
	 *            how long LanguageTool may take to answer, {@link #DEFAULT_TIMEOUT} by default
	 * @return {@code this}
	 */
	public LanguageToolProofreader setTimeout(Duration timeout)
	{
		this.timeout = Args.notNull(timeout, "timeout");
		return this;
	}

	@Override
	public List<List<RichTextIssue>> check(List<String> texts, Locale language) throws IOException
	{
		Args.notNull(texts, "texts");
		if (texts.isEmpty())
		{
			return List.of();
		}
		List<Integer> starts = new ArrayList<>(texts.size());
		StringBuilder text = new StringBuilder();
		for (String one : texts)
		{
			if (!starts.isEmpty())
			{
				text.append("\n\n");
			}
			starts.add(text.length());
			text.append(one);
		}
		Map<String, String> form = new LinkedHashMap<>(parameters);
		form.put("text", text.toString());
		form.put("language", language == null || language.getLanguage().isEmpty() ? "auto" :
			language.toLanguageTag());
		return issues(answer(post(form)), texts, starts);
	}

	/**
	 * Posts the form of a check to LanguageTool.
	 *
	 * @return its answer, JSON
	 * @throws IOException
	 *             when LanguageTool did not answer, or not with 200
	 */
	protected String post(Map<String, String> form) throws IOException
	{
		StringBuilder body = new StringBuilder();
		form.forEach((name, value) -> {
			if (body.length() > 0)
			{
				body.append('&');
			}
			body.append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=')
				.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		});
		HttpRequest request = HttpRequest.newBuilder(url).timeout(timeout)
			.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
			.build();
		HttpResponse<String> response;
		try
		{
			response = Client.INSTANCE.send(request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new InterruptedIOException("Interrupted while LanguageTool checked");
		}
		if (response.statusCode() != 200)
		{
			throw new IOException("LanguageTool answered " + response.statusCode());
		}
		return response.body();
	}

	private static JSONArray answer(String json) throws IOException
	{
		try
		{
			JSONArray matches = new JSONObject(json).optJSONArray("matches");
			return matches == null ? new JSONArray() : matches;
		}
		catch (JSONException e)
		{
			throw new IOException("LanguageTool answered no JSON", e);
		}
	}

	private static List<List<RichTextIssue>> issues(JSONArray matches, List<String> texts,
		List<Integer> starts)
	{
		List<List<RichTextIssue>> found = new ArrayList<>(texts.size());
		texts.forEach(text -> found.add(new ArrayList<>()));
		for (int index = 0; index < matches.length(); index++)
		{
			JSONObject match = matches.optJSONObject(index);
			if (match == null || !(match.opt("offset") instanceof Number offset) ||
				!(match.opt("length") instanceof Number length))
			{
				continue;
			}
			// the text the match is in; none across texts
			int text = starts.size() - 1;
			while (text > 0 && starts.get(text) > offset.intValue())
			{
				text--;
			}
			int from = offset.intValue() - starts.get(text);
			int to = from + length.intValue();
			if (from < 0 || to <= from || to > texts.get(text).length())
			{
				continue;
			}
			JSONObject rule = match.optJSONObject("rule");
			List<String> replacements = new ArrayList<>();
			JSONArray values = match.optJSONArray("replacements");
			for (int value = 0; values != null && value < values.length(); value++)
			{
				JSONObject replacement = values.optJSONObject(value);
				if (replacement != null && replacement.opt("value") instanceof String string)
				{
					replacements.add(string);
				}
			}
			found.get(text).add(new RichTextIssue(from, to,
				kind(rule == null ? null : rule.optString("issueType", null)),
				match.optString("message", null), replacements,
				rule == null ? null : rule.optString("id", null)));
		}
		return found;
	}

	/**
	 * @return the kind of an issue of LanguageTool, by the type of its rule
	 */
	static RichTextIssueKind kind(String issueType)
	{
		return "misspelling".equals(issueType) ? RichTextIssueKind.SPELLING :
			issueType != null && STYLE.contains(issueType) ? RichTextIssueKind.STYLE :
				RichTextIssueKind.GRAMMAR;
	}

	/** The client of all proofreaders, made once: it is not serializable. */
	private static final class Client
	{
		static final HttpClient INSTANCE = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	}
}
