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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

/**
 * The addresses of the images of a text, in HTML, Markdown or Tiptap's JSON: those the editor knows
 * the text was shown with, and those a text saved holds.
 */
final class RichTextImageAddresses
{
	/** An image of HTML, as the cleaning writes it, or as Markdown holds it. */
	private static final Pattern IMG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);

	/** The address of an image of HTML, in quotes or not. */
	private static final Pattern SRC = Pattern.compile(
		"\\ssrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))", Pattern.CASE_INSENSITIVE);

	/** An image of Markdown: ![text](address "title"), the address in angle brackets or not. */
	private static final Pattern MARKDOWN = Pattern.compile(
		"!\\[(?:[^\\]\\\\]|\\\\.)*\\]\\(\\s*(?:<([^>\\n]*)>|([^\\s)]+))(?:\\s+(?:\"[^\"]*\"|'[^']*'))?\\s*\\)");

	/** A character entity of HTML, by its name or its number. */
	private static final Pattern ENTITY = Pattern.compile("&(?:#(\\d{1,7})|#[xX]([0-9a-fA-F]{1,6})|" +
		"(amp|quot|apos|lt|gt|#39));");

	private RichTextImageAddresses()
	{
	}

	/**
	 * @param text
	 *            a text, {@code null} for none
	 * @param format
	 *            its format
	 * @return the addresses of its images, each once, in their order
	 */
	static Set<String> of(String text, RichTextFormat format)
	{
		Set<String> urls = new LinkedHashSet<>();
		if (text == null || text.isBlank())
		{
			return urls;
		}
		switch (format)
		{
			case HTML -> html(text, urls);
			case MARKDOWN -> {
				Matcher matcher = MARKDOWN.matcher(text);
				while (matcher.find())
				{
					add(urls, matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
				}
				html(text, urls);
			}
			case JSON -> {
				try
				{
					json(new JSONTokener(text).nextValue(), urls);
				}
				catch (JSONException e)
				{
					// no JSON: no images
				}
			}
		}
		return urls;
	}

	private static void html(String text, Set<String> urls)
	{
		Matcher image = IMG.matcher(text);
		while (image.find())
		{
			Matcher src = SRC.matcher(image.group());
			if (src.find())
			{
				String value = src.group(1) != null ? src.group(1) : src.group(2) != null ?
					src.group(2) : src.group(3);
				add(urls, decode(value));
			}
		}
	}

	private static void json(Object node, Set<String> urls)
	{
		if (node instanceof JSONObject object)
		{
			if ("image".equals(object.opt("type")) &&
				object.opt("attrs") instanceof JSONObject attrs &&
				attrs.opt("src") instanceof String src)
			{
				add(urls, src);
			}
			if (object.opt("content") instanceof JSONArray content)
			{
				json(content, urls);
			}
		}
		else if (node instanceof JSONArray array)
		{
			for (int i = 0; i < array.length(); i++)
			{
				json(array.opt(i), urls);
			}
		}
	}

	private static void add(Set<String> urls, String url)
	{
		if (url != null && !url.isBlank())
		{
			urls.add(url.trim());
		}
	}

	/**
	 * @return a value of an attribute of HTML with its entities read
	 */
	static String decode(String value)
	{
		if (value == null || value.indexOf('&') < 0)
		{
			return value;
		}
		Matcher matcher = ENTITY.matcher(value);
		StringBuilder decoded = new StringBuilder();
		while (matcher.find())
		{
			String replacement;
			if (matcher.group(1) != null || matcher.group(2) != null)
			{
				int code = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) :
					Integer.parseInt(matcher.group(2), 16);
				replacement = Character.isValidCodePoint(code) ? Character.toString(code) :
					matcher.group();
			}
			else
			{
				replacement = switch (matcher.group(3))
				{
					case "amp" -> "&";
					case "quot" -> "\"";
					case "lt" -> "<";
					case "gt" -> ">";
					default -> "'";
				};
			}
			matcher.appendReplacement(decoded, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(decoded);
		return decoded.toString();
	}
}
