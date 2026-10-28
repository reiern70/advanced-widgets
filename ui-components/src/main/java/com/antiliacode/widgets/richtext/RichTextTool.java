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

/**
 * The tools of the toolbar of a {@link RichTextEditor} built in its script: a button formatting the
 * text, opening a menu (the text style, the colors, the table) or a form (a link, an image), or
 * undoing. A tool needs its {@link #getFeature() feature}: without it, it does not show. A
 * {@link IRichTextFeature feature} of a page's own brings tools of its own, {@link IRichTextTool}.
 *
 * @since 11.0.0
 */
public enum RichTextTool implements IRichTextTool
{
	/** The text style: a paragraph or a heading, in a menu. */
	HEADING("heading", RichTextFeature.HEADING),

	BOLD("bold", RichTextFeature.BOLD),

	ITALIC("italic", RichTextFeature.ITALIC),

	UNDERLINE("underline", RichTextFeature.UNDERLINE),

	STRIKE("strike", RichTextFeature.STRIKE),

	CODE("code", RichTextFeature.CODE),

	/**
	 * The color of the text selected, in a menu of the
	 * {@link RichTextEditor#setColors(java.util.Collection) palette}.
	 */
	TEXT_COLOR("textColor", RichTextFeature.TEXT_COLOR),

	/**
	 * The color of the background of the text selected, in a menu of the
	 * {@link RichTextEditor#setBackgroundColors(java.util.Collection) palette} of the backgrounds.
	 */
	TEXT_BACKGROUND("textBackground", RichTextFeature.TEXT_BACKGROUND),

	/** The link at the cursor, in a form: its address and whether it opens in a new tab. */
	LINK("link", RichTextFeature.LINK),

	BULLET_LIST("bulletList", RichTextFeature.BULLET_LIST),

	ORDERED_LIST("orderedList", RichTextFeature.ORDERED_LIST),

	TASK_LIST("taskList", RichTextFeature.TASK_LIST),

	BLOCKQUOTE("blockquote", RichTextFeature.BLOCKQUOTE),

	CODE_BLOCK("codeBlock", RichTextFeature.CODE_BLOCK),

	HORIZONTAL_RULE("horizontalRule", RichTextFeature.HORIZONTAL_RULE),

	/** A menu inserting a table, adding and deleting its rows and columns. */
	TABLE("table", RichTextFeature.TABLE),

	/** The image selected, or one to insert, in a form: its address and its text. */
	IMAGE("image", RichTextFeature.IMAGE),

	ALIGN_LEFT("alignLeft", RichTextFeature.TEXT_ALIGN),

	ALIGN_CENTER("alignCenter", RichTextFeature.TEXT_ALIGN),

	ALIGN_RIGHT("alignRight", RichTextFeature.TEXT_ALIGN),

	ALIGN_JUSTIFY("alignJustify", RichTextFeature.TEXT_ALIGN),

	/** Takes the formats away from the text selected. */
	CLEAR_FORMATTING("clearFormatting", null),

	UNDO("undo", null),

	REDO("redo", null);

	/** The toolbar of an editor not told otherwise, in groups. */
	public static final List<List<RichTextTool>> DEFAULT_TOOLBAR = List.of(List.of(HEADING),
		List.of(BOLD, ITALIC, UNDERLINE, STRIKE, CODE), List.of(TEXT_COLOR, TEXT_BACKGROUND),
		List.of(LINK),
		List.of(BULLET_LIST, ORDERED_LIST, TASK_LIST), List.of(BLOCKQUOTE, CODE_BLOCK,
			HORIZONTAL_RULE), List.of(TABLE, IMAGE), List.of(ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT,
				ALIGN_JUSTIFY), List.of(CLEAR_FORMATTING), List.of(UNDO, REDO));

	private final String name;

	private final RichTextFeature feature;

	RichTextTool(String name, RichTextFeature feature)
	{
		this.name = name;
		this.feature = feature;
	}

	/**
	 * @return the name of the tool in the browser
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * @return the feature the tool needs, {@code null} for none
	 */
	@Override
	public RichTextFeature getFeature()
	{
		return feature;
	}
}
