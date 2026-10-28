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

/**
 * Offers items for what the user types in a {@link RichTextEditor}, from the server: the users to
 * mention after {@code @}, the words that complete the one before the cursor on Ctrl+Space. The
 * editor asks it through {@link RichTextSuggestBehavior} as the user types; the ready-made
 * {@link RichTextSuggester} makes one of a function.
 *
 * @see RichTextEditor#addSuggester(IRichTextSuggester)
 * @since 11.0.0
 */
public interface IRichTextSuggester extends IClusterable
{
	/**
	 * @return its name, unique in the editor: the kind of its mentions ({@code data-mention}),
	 *         {@code users} for example; letters, digits, {@code _} and {@code -}
	 */
	String getName();

	/**
	 * @return the character starting its queries at the start of a word, {@code @} or {@code #};
	 *         {@code null} for a suggester completing the word before the cursor on Ctrl+Space
	 */
	String getTrigger();

	/**
	 * @return what choosing an item inserts: a mention by default with a character, its text
	 *         without
	 */
	default RichTextSuggestionInsert getInsert()
	{
		return getTrigger() == null ? RichTextSuggestionInsert.TEXT : RichTextSuggestionInsert.MENTION;
	}

	/**
	 * @param query
	 *            what the user typed after the character, or the word before the cursor; from the
	 *            browser, the user's
	 * @param max
	 *            the most items the editor shows
	 * @return the items, the best first
	 */
	List<RichTextSuggestion> suggest(String query, int max);
}
