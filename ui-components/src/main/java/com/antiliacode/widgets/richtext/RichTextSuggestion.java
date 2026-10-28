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

import java.util.regex.Pattern;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An item a {@link IRichTextSuggester suggester} offers for what the user types: a user to
 * mention, a word to complete.
 *
 * @param id
 *            what a mention keeps, {@code data-id}: the id of a user, for example; letters,
 *            digits and {@code _ . @ : + -}, as the cleaning of the HTML keeps them
 * @param label
 *            what the list shows, and a mention after the character of its suggester
 * @param detail
 *            more, shown after the label: an e-mail address, a description; {@code null} for none
 * @param text
 *            what a completion inserts, {@code null} for its label
 * @since 11.0.0
 */
public record RichTextSuggestion(String id, String label, String detail, String text)
	implements IClusterable
{
	/** A name of a suggester, which its mentions keep as {@code data-mention}. */
	static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,40}");

	/** An id of a mention, as the cleaning of the HTML keeps it. */
	static final Pattern ID = Pattern.compile("[\\w.@:+-]{1,200}", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Checks the item.
	 */
	public RichTextSuggestion
	{
		Args.notEmpty(label, "label");
		if (id == null || !ID.matcher(id).matches())
		{
			throw new IllegalArgumentException("An id of letters, digits and _ . @ : + -, not " +
				id);
		}
	}

	/**
	 * @return an item of an id and a label
	 */
	public static RichTextSuggestion of(String id, String label)
	{
		return new RichTextSuggestion(id, label, null, null);
	}

	/**
	 * @return the item as the editor in the browser takes it
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject().put("id", id).put("label", label);
		if (detail != null)
		{
			json.put("detail", detail);
		}
		if (text != null)
		{
			json.put("text", text);
		}
		return json;
	}
}
