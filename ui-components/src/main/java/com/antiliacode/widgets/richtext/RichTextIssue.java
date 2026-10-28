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

import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * What a {@link IRichTextProofreader proofreader} found in a text: where it is, what it is, what
 * the proofreader says of it and what may take its place.
 *
 * @param from
 *            where it starts in the text, in its {@code char}s, as {@link String} counts them
 * @param to
 *            where it ends, after {@code from}
 * @param kind
 *            what it is
 * @param message
 *            what is wrong, {@code null} for nothing said
 * @param replacements
 *            what may take its place, the best first; none for no suggestion
 * @param rule
 *            the rule that found it, {@code null} for none: the user ignoring an issue ignores
 *            those of its rule and its text
 * @since 11.0.0
 */
public record RichTextIssue(int from, int to, RichTextIssueKind kind, String message,
	List<String> replacements, String rule) implements IClusterable
{
	/**
	 * Checks the issue, and keeps its replacements as they are now.
	 */
	public RichTextIssue
	{
		if (from < 0 || to <= from)
		{
			throw new IllegalArgumentException("An issue is from 0 or after, to after its start: " +
				from + " to " + to);
		}
		Args.notNull(kind, "kind");
		replacements = replacements == null ? List.of() : List.copyOf(replacements);
	}

	/**
	 * @return a word spelled wrong, with what may take its place
	 */
	public static RichTextIssue spelling(int from, int to, String... replacements)
	{
		return new RichTextIssue(from, to, RichTextIssueKind.SPELLING, null, List.of(replacements),
			null);
	}

	/**
	 * @return the issue as the editor in the browser takes it
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject().put("from", from).put("to", to).put("kind",
			kind.getName());
		if (message != null)
		{
			json.put("message", message);
		}
		if (!replacements.isEmpty())
		{
			json.put("replacements", new JSONArray(replacements));
		}
		if (rule != null)
		{
			json.put("rule", rule);
		}
		return json;
	}
}
