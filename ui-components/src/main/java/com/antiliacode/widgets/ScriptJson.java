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
package com.antiliacode.widgets;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Writes values into inline scripts: the scripts of {@code OnDomReadyHeaderItem.forScript},
 * {@code JavaScriptHeaderItem.forScript} or {@code AjaxRequestTarget.appendJavaScript}, which the
 * page holds in a {@code <script>} element or an Ajax response in a section of its XML.
 * <p>
 * JSON alone is not safe there: a text holding {@code </script>}, {@code <!--} or {@code ]]>} would
 * end the script, or change how the browser reads the rest of the page. So every value goes in as
 * JSON with {@code <}, {@code >}, {@code &}, the line separator U+2028 and the paragraph separator
 * U+2029 written as JSON's unicode escapes. The script reads the same value; the page sees no
 * markup in it. Only JSON's strings hold these characters, so escaping them never changes the
 * value.
 * <p>
 * Use {@link #quote(String)} for a text, {@link #of(JSONObject)} and {@link #of(JSONArray)} for
 * options, and {@link #escape(CharSequence)} for JSON written by another serializer. Not for
 * scripts: their operators, such as {@code &&}, would not survive.
 *
 * @since 11.0.0
 */
public final class ScriptJson
{
	private static final char LINE_SEPARATOR = 0x2028;

	private static final char PARAGRAPH_SEPARATOR = 0x2029;

	private ScriptJson()
	{
	}

	/**
	 * @param text
	 *            a text, may be {@code null}
	 * @return the text as a string literal of JavaScript, safe in an inline script; {@code ""} for
	 *         {@code null}, as {@link JSONObject#quote(String)}
	 */
	public static String quote(String text)
	{
		return escape(JSONObject.quote(text));
	}

	/**
	 * @param json
	 *            a JSON object, such as the options of a script
	 * @return the object as JSON, safe in an inline script; the text {@code null} for {@code null}
	 */
	public static String of(JSONObject json)
	{
		return json == null ? "null" : escape(json.toString());
	}

	/**
	 * @param json
	 *            a JSON array
	 * @return the array as JSON, safe in an inline script; the text {@code null} for {@code null}
	 */
	public static String of(JSONArray json)
	{
		return json == null ? "null" : escape(json.toString());
	}

	/**
	 * Escapes JSON written by another serializer, such as Jackson, for an inline script.
	 *
	 * @param json
	 *            the text of a JSON value, or of a string literal of JavaScript
	 * @return the same value, with {@code <}, {@code >}, {@code &}, U+2028 and U+2029 written as
	 *         unicode escapes
	 */
	public static String escape(CharSequence json)
	{
		if (json == null)
		{
			return null;
		}
		StringBuilder escaped = null;
		int length = json.length();
		for (int i = 0; i < length; i++)
		{
			char c = json.charAt(i);
			if (c == '<' || c == '>' || c == '&' || c == LINE_SEPARATOR || c == PARAGRAPH_SEPARATOR)
			{
				if (escaped == null)
				{
					escaped = new StringBuilder(length + 16).append(json, 0, i);
				}
				String hex = Integer.toHexString(c);
				escaped.append('\\').append('u').append("0000", hex.length(), 4).append(hex);
			}
			else if (escaped != null)
			{
				escaped.append(c);
			}
		}
		return escaped == null ? json.toString() : escaped.toString();
	}
}
