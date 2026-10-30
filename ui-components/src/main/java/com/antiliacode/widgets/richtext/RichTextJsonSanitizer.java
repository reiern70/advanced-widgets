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
 * <p>
 * {@link #IMAGES_FROM_SITE_ONLY} keeps the images of the site alone: a node or a mark whose
 * {@code src} is not {@link RichTextPolicy#isSiteUrl(String) the site's own} is taken out too, an
 * image from another site for example. Links go to other sites as before.
 * <p>
 * It {@link #sanitizeAndReport(String) tells} of what it took out by the names of HTML: a link,
 * {@code a}, an image, {@code img}, another mark or node by its type; and of a text that is no
 * document as {@code json}, all of it taken out.
 *
 * @since 10.11.0
 */
public class RichTextJsonSanitizer implements IRichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The sanitizer. */
	public static final RichTextJsonSanitizer INSTANCE = new RichTextJsonSanitizer();

	/**
	 * The sanitizer keeping the images of the site alone.
	 *
	 * @since 10.11.0
	 */
	public static final RichTextJsonSanitizer IMAGES_FROM_SITE_ONLY = new RichTextJsonSanitizer(
		true);

	/** How deep objects and arrays may nest in the document. */
	public static final int MAX_DEPTH = 100;

	/** What a text that is no document is told of as. */
	private static final String NO_DOCUMENT = "json";

	/** Whether images go only to the site's own addresses. */
	private final boolean imagesFromSiteOnly;

	/**
	 * A sanitizer letting images go to other sites, as {@link #INSTANCE} does.
	 */
	public RichTextJsonSanitizer()
	{
		this(false);
	}

	/**
	 * @param imagesFromSiteOnly
	 *            whether images, the {@code src} of any node or mark, go only to the site's own
	 *            addresses
	 * @since 10.11.0
	 */
	public RichTextJsonSanitizer(boolean imagesFromSiteOnly)
	{
		this.imagesFromSiteOnly = imagesFromSiteOnly;
	}

	/**
	 * @return whether images, the {@code src} of any node or mark, go only to the site's own
	 *         addresses
	 * @since 10.11.0
	 */
	public boolean isImagesFromSiteOnly()
	{
		return imagesFromSiteOnly;
	}

	@Override
	public String sanitize(String json)
	{
		return sanitizeAndReport(json).text();
	}

	/**
	 * Cleans the JSON, telling of the links, {@code a}, and the images, {@code img}, it took out,
	 * and of a text that is no document, {@code json}.
	 */
	@Override
	public RichTextSanitized sanitizeAndReport(String json)
	{
		if (json == null)
		{
			return new RichTextSanitized(null, RichTextCleaningReport.EMPTY);
		}
		RichTextCleaningReport.Builder report = RichTextCleaningReport.builder();
		RichTextSanitized none = new RichTextSanitized(null,
			report.addElement(NO_DOCUMENT).build());
		if (depth(json) > MAX_DEPTH)
		{
			return none;
		}
		JSONObject document;
		try
		{
			JSONTokener tokener = new JSONTokener(json);
			if (!(tokener.nextValue() instanceof JSONObject object) || tokener.nextClean() != 0)
			{
				return none;
			}
			document = object;
		}
		catch (JSONException e)
		{
			return none;
		}
		report = RichTextCleaningReport.builder();
		return clean(document, report) ? new RichTextSanitized(document.toString(), report.build()) :
			new RichTextSanitized(json, RichTextCleaningReport.EMPTY);
	}

	/**
	 * Takes the marks and the nodes going to addresses that are not safe out of a node and those
	 * in it.
	 *
	 * @return whether it took anything out
	 */
	private boolean clean(JSONObject node, RichTextCleaningReport.Builder report)
	{
		boolean changed = false;
		JSONArray marks = node.optJSONArray("marks");
		if (marks != null)
		{
			changed |= removeUnsafe(marks, report);
		}
		JSONArray content = node.optJSONArray("content");
		if (content != null)
		{
			changed |= removeUnsafe(content, report);
			for (int i = 0; i < content.length(); i++)
			{
				if (content.opt(i) instanceof JSONObject child)
				{
					changed |= clean(child, report);
				}
			}
		}
		return changed;
	}

	/**
	 * @return whether it took out the marks or nodes of an array going to addresses that are not
	 *         safe
	 */
	private boolean removeUnsafe(JSONArray array, RichTextCleaningReport.Builder report)
	{
		boolean changed = false;
		for (int i = array.length() - 1; i >= 0; i--)
		{
			if (array.opt(i) instanceof JSONObject item && !isSafe(item))
			{
				array.remove(i);
				report.addElement(nameOf(item.optString("type")));
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * @return the name of HTML of a mark or a node of Tiptap: {@code a} for a link, {@code img}
	 *         for an image, its type otherwise
	 */
	private static String nameOf(String type)
	{
		return switch (type)
		{
			case "link" -> "a";
			case "image" -> "img";
			default -> type;
		};
	}

	/**
	 * @return whether the {@code href} and the {@code src} of a mark or a node, if it has them, go
	 *         to safe addresses; the {@code src} to the site's own if the images are the site's
	 *         alone
	 */
	private boolean isSafe(JSONObject item)
	{
		JSONObject attributes = item.optJSONObject("attrs");
		if (attributes == null)
		{
			return true;
		}
		for (String name : new String[] { "href", "src" })
		{
			if (attributes.opt(name) instanceof String address && !address.isBlank() &&
				!(imagesFromSiteOnly && name.equals("src") ? RichTextPolicy.isSiteUrl(address) :
					RichTextPolicy.isSafeUrl(address)))
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
