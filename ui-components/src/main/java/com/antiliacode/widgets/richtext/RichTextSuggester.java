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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

/**
 * A suggester made of a function: of mentions, after a character, or of completions, on
 * Ctrl+Space.
 *
 * <pre>
 * editor.addSuggester(RichTextSuggester.mentions("users", "@", (query, max) -&gt;
 *     users.find(query, max).stream().map(user -&gt; new RichTextSuggestion(user.getId(),
 *         user.getName(), user.getEmail(), null)).toList()));
 * editor.addSuggester(RichTextSuggester.words("terms", List.of("Wicket", "Tiptap")));
 * </pre>
 *
 * @since 11.0.0
 */
public class RichTextSuggester implements IRichTextSuggester
{
	private static final long serialVersionUID = 1L;

	/** What finds the items of a query. */
	@FunctionalInterface
	public interface Source extends IClusterable
	{
		/**
		 * @param query
		 *            what the user typed, the user's
		 * @param max
		 *            the most items shown
		 * @return the items, the best first
		 */
		List<RichTextSuggestion> suggest(String query, int max);
	}

	private final String name;

	private final String trigger;

	private final RichTextSuggestionInsert insert;

	private final Source source;

	/**
	 * @param name
	 *            its name, the kind of its mentions
	 * @param trigger
	 *            the character starting its queries, {@code null} for a completion on Ctrl+Space
	 * @param insert
	 *            what choosing an item inserts
	 * @param source
	 *            what finds the items
	 */
	public RichTextSuggester(String name, String trigger, RichTextSuggestionInsert insert,
		Source source)
	{
		Args.notNull(name, "name");
		if (!RichTextSuggestion.NAME.matcher(name).matches())
		{
			throw new IllegalArgumentException("A name of letters, digits, _ and -, not " + name);
		}
		if (trigger != null && (trigger.isEmpty() || trigger.codePointCount(0, trigger.length()) != 1 ||
			Character.isLetterOrDigit(trigger.codePointAt(0)) ||
			Character.isWhitespace(trigger.codePointAt(0))))
		{
			throw new IllegalArgumentException("A trigger is one character, no letter, digit or " +
				"blank: " + trigger);
		}
		this.name = name;
		this.trigger = trigger;
		this.insert = Args.notNull(insert, "insert");
		this.source = Args.notNull(source, "source");
	}

	/**
	 * @return a suggester of mentions after a character, {@code @} for example
	 */
	public static RichTextSuggester mentions(String name, String trigger, Source source)
	{
		return new RichTextSuggester(name, Args.notEmpty(trigger, "trigger"),
			RichTextSuggestionInsert.MENTION, source);
	}

	/**
	 * @return a suggester completing the word before the cursor, on Ctrl+Space
	 */
	public static RichTextSuggester completions(String name, Source source)
	{
		return new RichTextSuggester(name, null, RichTextSuggestionInsert.TEXT, source);
	}

	/**
	 * @return a suggester completing the word before the cursor from words given, those starting
	 *         with it whatever the case, in their order
	 */
	public static RichTextSuggester words(String name, Collection<String> words)
	{
		List<String> all = List.copyOf(words);
		return completions(name, (query, max) -> {
			String start = query.toLowerCase(Locale.ROOT);
			return all.stream().filter(word -> word.toLowerCase(Locale.ROOT).startsWith(start))
				.limit(max).map(word -> new RichTextSuggestion(String.valueOf(all.indexOf(word)),
					word, null, word)).toList();
		});
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getTrigger()
	{
		return trigger;
	}

	@Override
	public RichTextSuggestionInsert getInsert()
	{
		return insert;
	}

	@Override
	public List<RichTextSuggestion> suggest(String query, int max)
	{
		return source.suggest(query, max);
	}
}
