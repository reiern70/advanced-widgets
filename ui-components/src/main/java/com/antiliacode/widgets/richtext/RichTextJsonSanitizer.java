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

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;

/**
 * Cleans Tiptap's JSON, the document a {@link RichTextEditor} keeps in
 * {@link RichTextFormat#JSON}, of the links and images to addresses that are not safe: what an
 * editor of JSON cleans by default ({@link AbstractRichTextEditor#getJsonSanitizer()}).
 * <p>
 * A mark, a link for example, or a node, an image for example, whose {@code href} or {@code src}
 * goes to an address other than an http, https or mailto one, or a relative one
 * ({@link RichTextPolicy#isSafeUrl(String)}), is taken out: the link leaves its text as it is, the
 * image goes. A text that is no JSON object, or one nested deeper than {@value #MAX_DEPTH} levels,
 * gives {@code null}, nothing. The rest stays as it is: the texts, which a renderer writes as text,
 * and the attributes of the nodes, which a renderer of Tiptap's schema writes as its extensions
 * say.
 *
 * @since 10.11.0
 */
public class RichTextJsonSanitizer implements IRichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The sanitizer. */
	public static final RichTextJsonSanitizer INSTANCE = new RichTextJsonSanitizer();

	/** How deep objects and arrays may nest in the document. */
	public static final int MAX_DEPTH = 100;

	@Override
	public String sanitize(String json)
	{
		if (json == null)
		{
			return null;
		}
		if (depth(json) > MAX_DEPTH)
		{
			return null;
		}
		JSONObject document;
		try
		{
			JSONTokener tokener = new JSONTokener(json);
			if (!(tokener.nextValue() instanceof JSONObject object) || tokener.nextClean() != 0)
			{
				return null;
			}
			document = object;
		}
		catch (JSONException e)
		{
			return null;
		}
		return clean(document) ? document.toString() : json;
	}

	/**
	 * Takes the marks and the nodes going to addresses that are not safe out of a node and those
	 * in it.
	 *
	 * @return whether it took anything out
	 */
	private static boolean clean(JSONObject node)
	{
		boolean changed = false;
		JSONArray marks = node.optJSONArray("marks");
		if (marks != null)
		{
			changed |= removeUnsafe(marks);
		}
		JSONArray content = node.optJSONArray("content");
		if (content != null)
		{
			changed |= removeUnsafe(content);
			for (int i = 0; i < content.length(); i++)
			{
				if (content.opt(i) instanceof JSONObject child)
				{
					changed |= clean(child);
				}
			}
		}
		return changed;
	}

	/**
	 * @return whether it took out the marks or nodes of an array going to addresses that are not
	 *         safe
	 */
	private static boolean removeUnsafe(JSONArray array)
	{
		boolean changed = false;
		for (int i = array.length() - 1; i >= 0; i--)
		{
			if (array.opt(i) instanceof JSONObject item && !isSafe(item))
			{
				array.remove(i);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * @return whether the {@code href} and the {@code src} of a mark or a node, if it has them, go
	 *         to safe addresses
	 */
	private static boolean isSafe(JSONObject item)
	{
		JSONObject attributes = item.optJSONObject("attrs");
		if (attributes == null)
		{
			return true;
		}
		for (String name : new String[] { "href", "src" })
		{
			if (attributes.opt(name) instanceof String address && !address.isBlank() &&
				!RichTextPolicy.isSafeUrl(address))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return how deep the objects and arrays of a JSON text nest, read without parsing it
	 */
	private static int depth(String json)
	{
		int depth = 0;
		int deepest = 0;
		boolean inString = false;
		for (int i = 0; i < json.length(); i++)
		{
			char c = json.charAt(i);
			if (inString)
			{
				if (c == '\\')
				{
					i++;
				}
				else if (c == '"')
				{
					inString = false;
				}
			}
			else if (c == '"')
			{
				inString = true;
			}
			else if (c == '{' || c == '[')
			{
				deepest = Math.max(deepest, ++depth);
			}
			else if (c == '}' || c == ']')
			{
				depth--;
			}
		}
		return deepest;
	}
}
