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
package com.antilia.widgets.tree;

import com.antilia.widgets.icon.IIcon;

/**
 * The icons of the trees of Antilia's Advanced Widgets: line icons drawn for them as inline SVG, one em
 * high, stroked in the current text color and hidden from screen readers. They need no style
 * sheet, font or image, and are the same the trees rendered in the browser draw
 * ({@code TREE_ICONS} of {@code tree.ts}).
 * <p>
 * Each one is also a slot a tree takes an icon of the application's own for, with
 * {@link Tree#setIcon(TreeIcon, IIcon)}, {@link TreeGrid#setIcon(TreeIcon, IIcon)} and
 * {@link JsonBasedTree#setIcon(TreeIcon, IIcon)}.
 *
 * @since 11.0.0
 */
public enum TreeIcon implements IIcon
{
	/** The arrow of a folder, pointing to the end of the line, turned down when expanded. */
	TOGGLE("<path d=\"M9 6l6 6-6 6\"/>", "2.5"),

	/** A collapsed folder. */
	FOLDER("<path d=\"M3 6.5A1.5 1.5 0 0 1 4.5 5H9l2 2.5h8.5A1.5 1.5 0 0 1 21 9v8.5a1.5 1.5 " +
		"0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z\"/>", "2"),

	/** An expanded folder. */
	FOLDER_OPEN("<path d=\"M3 17.5v-11A1.5 1.5 0 0 1 4.5 5H9l2 2.5h7A1.5 1.5 0 0 1 19.5 9v1.5\"/>" +
		"<path d=\"M3 17.5l2.6-6.1A1.5 1.5 0 0 1 7 10.5h13.2a1 1 0 0 1 .9 1.4l-2.6 6.2a1.5 1.5 0 0 " +
		"1-1.4.9H4.5A1.5 1.5 0 0 1 3 17.5z\"/>", "2"),

	/** A leaf: a sheet of paper. */
	LEAF("<path d=\"M6 3h8l4 4v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z\"/>" +
		"<path d=\"M14 3v4h4M8.5 12h7M8.5 16h7\"/>", "2");

	private final String markup;

	TreeIcon(String paths, String strokeWidth)
	{
		markup = "<svg viewBox=\"0 0 24 24\" width=\"1em\" height=\"1em\" aria-hidden=\"true\" " +
			"focusable=\"false\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"" +
			strokeWidth + "\" stroke-linecap=\"round\" stroke-linejoin=\"round\">" + paths +
			"</svg>";
	}

	@Override
	public String getMarkup()
	{
		return markup;
	}
}
