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

/**
 * A color of the palette of a {@link RichTextEditor}, for the text
 * ({@link RichTextFeature#TEXT_COLOR}) and for its background
 * ({@link RichTextFeature#TEXT_BACKGROUND}). The HTML has classes, not styles, which a Content
 * Security Policy without {@code 'unsafe-inline'} would block:
 * {@code <span class="antilia-color-red">} and {@code <mark class="antilia-background-yellow">}.
 * {@link RichTextEditor#CSS} colors them from the theme, in the editor and in a
 * {@link RichTextView}: a color of the text is its tone mixed with the color of the text of the
 * theme, a background its tone mixed with the surface, so that both read in light and dark themes.
 *
 * @since 11.0.0
 */
public enum RichTextColor
{
	GRAY("gray"),

	RED("red"),

	ORANGE("orange"),

	YELLOW("yellow"),

	GREEN("green"),

	BLUE("blue"),

	PURPLE("purple"),

	PINK("pink");

	/** The start of the class of a color of the text. */
	public static final String COLOR_CLASS = "antilia-color-";

	/** The start of the class of a color of the background. */
	public static final String BACKGROUND_CLASS = "antilia-background-";

	private final String name;

	RichTextColor(String name)
	{
		this.name = name;
	}

	/**
	 * @return the name of the color in the browser and in its classes
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the class coloring the text, {@code antilia-color-red}
	 */
	public String getColorClass()
	{
		return COLOR_CLASS + name;
	}

	/**
	 * @return the class coloring the background of the text, {@code antilia-background-red}
	 */
	public String getBackgroundClass()
	{
		return BACKGROUND_CLASS + name;
	}
}
