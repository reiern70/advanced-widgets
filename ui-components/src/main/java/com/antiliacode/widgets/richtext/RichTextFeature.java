/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * What the text of a {@link RichTextEditor} may hold. A feature left out is not in the editor's
 * schema, nor allowed by the {@link RichTextPolicy cleaning} of its HTML: pasting it brings its
 * text alone, and its tools do not show.
 *
 * @since 11.0.0
 */
public enum RichTextFeature
{
	/** Headings, of the {@link RichTextEditor#setHeadingLevels(int...) levels} of the editor. */
	HEADING("heading"),

	/** Bold text. */
	BOLD("bold"),

	/** Italic text. */
	ITALIC("italic"),

	/** Underlined text; not in Markdown, which has no syntax for it. */
	UNDERLINE("underline"),

	/** Struck through text. */
	STRIKE("strike"),

	/** Code in the text. */
	CODE("code"),

	/** Links, to http, https and mailto addresses, or relative ones. */
	LINK("link"),

	/** Bulleted lists. */
	BULLET_LIST("bulletList"),

	/** Numbered lists. */
	ORDERED_LIST("orderedList"),

	/** Lists of tasks, each with a check box. */
	TASK_LIST("taskList"),

	/** Quotes. */
	BLOCKQUOTE("blockquote"),

	/** Blocks of code, in a language. */
	CODE_BLOCK("codeBlock"),

	/** Horizontal lines. */
	HORIZONTAL_RULE("horizontalRule"),

	/** Tables, with a header row or not. */
	TABLE("table"),

	/** Images, of http and https addresses, or relative ones. */
	IMAGE("image"),

	/** The alignment of paragraphs, headings and cells, by classes; not in Markdown. */
	TEXT_ALIGN("textAlign");

	private final String name;

	RichTextFeature(String name)
	{
		this.name = name;
	}

	/**
	 * @return the name of the feature in the browser
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param format
	 *            how the editor keeps the text
	 * @return the features of an editor not told otherwise: all of them, but underlined text and
	 *         alignment for Markdown
	 */
	public static Set<RichTextFeature> defaults(RichTextFormat format)
	{
		Set<RichTextFeature> features = EnumSet.allOf(RichTextFeature.class);
		if (format == RichTextFormat.MARKDOWN)
		{
			features.remove(UNDERLINE);
			features.remove(TEXT_ALIGN);
		}
		return Collections.unmodifiableSet(features);
	}
}
