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
import java.util.Set;

/**
 * The colors of the background of the text of a {@link RichTextEditor} unless it is given its own
 * ({@link RichTextEditor#setBackgroundColors(java.util.Collection)}): gray, brown, red, orange,
 * yellow, lime, green, teal, cyan, blue, purple and pink,
 * {@link IRichTextBackgroundColor colors of the background} of the class
 * {@code antilia-background-yellow} for example. {@link RichTextEditor#CSS} colors them from the
 * theme, in the editor and in a {@link RichTextView}: each is its tone mixed with the surface of
 * the theme, so that the text on it reads in light and dark themes. The colors of the text are another
 * palette, {@link RichTextColor}.
 *
 * @since 11.0.0
 */
public enum RichTextBackgroundColor implements IRichTextBackgroundColor
{
	GRAY("gray"),

	BROWN("brown"),

	RED("red"),

	ORANGE("orange"),

	YELLOW("yellow"),

	LIME("lime"),

	GREEN("green"),

	TEAL("teal"),

	CYAN("cyan"),

	BLUE("blue"),

	PURPLE("purple"),

	PINK("pink");

	/** The start of the class of a color. */
	public static final String BACKGROUND_CLASS = "antilia-background-";

	/** All the colors, the palette of the colors of the background of an editor by default. */
	public static final Set<IRichTextBackgroundColor> PALETTE = IRichTextBackgroundColor.palette(
		List.of(values()));

	private final String name;

	RichTextBackgroundColor(String name)
	{
		this.name = name;
	}

	/**
	 * @return the name of the color in the browser and in its class
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * @return the class coloring the background of the text, {@code antilia-background-yellow}
	 */
	@Override
	public String getBackgroundClass()
	{
		return BACKGROUND_CLASS + name;
	}
}
