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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.util.io.IClusterable;

/**
 * A mention in a text, as the editor writes it,
 * {@code <span class="antilia-mention" data-mention="users" data-id="42">@Ann</span>}: the name of
 * its suggester, its id and its label. The HTML comes from the browser: a page acting on the
 * mentions of a text, telling the users mentioned for example, checks each id again.
 *
 * @param kind
 *            the name of the suggester, {@code users}
 * @param id
 *            the id of the item mentioned
 * @param label
 *            the text of the mention, {@code @Ann}
 * @since 11.0.0
 */
public record RichTextMention(String kind, String id, String label) implements IClusterable
{
	/** The class of a mention. */
	public static final String CLASS = "antilia-mention";

	private static final Pattern SPAN = Pattern.compile("<span\\b([^>]*)>([^<]*)</span>",
		Pattern.CASE_INSENSITIVE);

	private static final Pattern ENTITY = Pattern.compile(
		"&(?:([a-z]+)|#([0-9]{1,7})|#[xX]([0-9a-fA-F]{1,6}));");

	private static final Pattern ATTRIBUTE = Pattern.compile(
		"([a-zA-Z-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

	/**
	 * @param html
	 *            a text, cleaned as the editor's ({@link RichTextPolicy}), so that its mentions are
	 *            written as the editor writes them
	 * @return its mentions, in their order
	 */
	public static List<RichTextMention> of(String html)
	{
		List<RichTextMention> mentions = new ArrayList<>();
		if (html == null)
		{
			return mentions;
		}
		Matcher span = SPAN.matcher(html);
		while (span.find())
		{
			String kind = null;
			String id = null;
			boolean mention = false;
			Matcher attribute = ATTRIBUTE.matcher(span.group(1));
			while (attribute.find())
			{
				String value = unescape(attribute.group(2) != null ? attribute.group(2) :
					attribute.group(3));
				switch (attribute.group(1).toLowerCase())
				{
					case "class" -> mention = List.of(value.trim().split("\\s+")).contains(CLASS);
					case "data-mention" -> kind = value;
					case "data-id" -> id = value;
					default -> {
						// another attribute
					}
				}
			}
			if (mention && kind != null && id != null)
			{
				mentions.add(new RichTextMention(kind, id, unescape(span.group(2))));
			}
		}
		return mentions;
	}

	private static String unescape(String text)
	{
		Matcher entity = ENTITY.matcher(text);
		StringBuilder plain = new StringBuilder();
		while (entity.find())
		{
			String named = entity.group(1);
			String replacement;
			if (named != null)
			{
				replacement = switch (named)
				{
					case "lt" -> "<";
					case "gt" -> ">";
					case "quot" -> "\"";
					case "apos" -> "'";
					case "amp" -> "&";
					case "nbsp" -> "\u00a0";
					default -> entity.group();
				};
			}
			else
			{
				int code = entity.group(2) != null ? Integer.parseInt(entity.group(2)) :
					Integer.parseInt(entity.group(3), 16);
				replacement = Character.isValidCodePoint(code) ? Character.toString(code) :
					entity.group();
			}
			entity.appendReplacement(plain, Matcher.quoteReplacement(replacement));
		}
		entity.appendTail(plain);
		return plain.toString();
	}
}
