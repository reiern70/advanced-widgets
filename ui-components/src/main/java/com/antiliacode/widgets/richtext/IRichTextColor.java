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
import java.util.Set;

import org.apache.wicket.util.io.IClusterable;

/**
 * A color of the text of a {@link RichTextEditor} ({@link RichTextFeature#TEXT_COLOR}): a
 * {@code <span>} of {@link #getColorClass()}. The HTML has classes, not styles, which a Content
 * Security Policy without {@code 'unsafe-inline'} would block. The page's style sheets color the
 * class; the cleaning of the HTML ({@link RichTextPolicy}) keeps it, and no other class, on a
 * {@code <span>}.
 * <p>
 * The colors of the text are a palette of their own, apart from the
 * {@link IRichTextBackgroundColor colors of the background}: a text takes one of each, or either.
 * {@link RichTextColor#PALETTE} is the palette {@link RichTextEditor#CSS} colors; a page registers
 * colors of its own with {@link RichTextEditor#setColors(Collection)} and
 * {@link RichTextView#setColors(Collection)}, each by a name unique in the palette. The menu names
 * a color by the translation of {@code RichTextEditor.color} followed by its name capitalized,
 * {@code RichTextEditor.colorTeal} for {@code teal}, or by its name.
 *
 * @since 11.0.0
 */
public interface IRichTextColor extends IClusterable
{
	/**
	 * @return the name of the color, unique in its palette: what the editor keeps of it in
	 *         Tiptap's JSON, and in the browser
	 */
	String getName();

	/**
	 * @return the class coloring the text, on a {@code <span>}
	 */
	String getColorClass();

	/**
	 * A palette of colors of the text, checked: each with a name of letters, digits, {@code -} and
	 * {@code _}, from a letter, up to 40 of them, and with one CSS class, without escapes; no two
	 * of the same name or the same class.
	 *
	 * @param colors
	 *            the colors, in the order of the menu
	 * @return the colors, in that order, unmodifiable and serializable
	 * @throws IllegalArgumentException
	 *             for a color missing, a name or a class not allowed, or one twice
	 */
	static Set<IRichTextColor> palette(Collection<? extends IRichTextColor> colors)
	{
		return RichTextPalettes.check(colors, IRichTextColor::getName,
			IRichTextColor::getColorClass);
	}
}
