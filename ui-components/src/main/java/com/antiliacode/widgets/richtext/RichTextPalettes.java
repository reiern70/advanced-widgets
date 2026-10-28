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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The check of a palette of {@link IRichTextColor colors of the text} or of
 * {@link IRichTextBackgroundColor colors of the background}, as the editor in the browser checks
 * it too.
 */
final class RichTextPalettes
{
	/** What a name may be: letters, digits, {@code -} and {@code _}, from a letter. */
	static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,39}");

	/** What a class may be: one CSS class, without escapes. */
	static final Pattern CLASS = Pattern.compile("-?[_A-Za-z][_A-Za-z0-9-]{0,99}");

	private RichTextPalettes()
	{
	}

	/**
	 * @param colors
	 *            the colors, in the order of the menu
	 * @param name
	 *            the name of a color
	 * @param css
	 *            the class of a color
	 * @return the colors, in that order, unmodifiable and serializable
	 * @throws IllegalArgumentException
	 *             for a color missing, a name or a class not allowed, or one twice
	 */
	static <C> Set<C> check(Collection<? extends C> colors, Function<? super C, String> name,
		Function<? super C, String> css)
	{
		if (colors == null)
		{
			throw new IllegalArgumentException("The palette is missing");
		}
		Set<String> names = new HashSet<>();
		Set<String> classes = new HashSet<>();
		Set<C> palette = new LinkedHashSet<>();
		for (C color : colors)
		{
			if (color == null)
			{
				throw new IllegalArgumentException("A color of the palette is missing");
			}
			String named = name.apply(color);
			if (named == null || !NAME.matcher(named).matches())
			{
				throw new IllegalArgumentException("Not a name of a color: " + named);
			}
			if (!names.add(named))
			{
				throw new IllegalArgumentException("Two colors are named " + named);
			}
			String className = css.apply(color);
			if (className == null || !CLASS.matcher(className).matches())
			{
				throw new IllegalArgumentException("Not a class of the color " + named + ": " +
					className);
			}
			if (!classes.add(className))
			{
				throw new IllegalArgumentException("The class " + className + " is of two colors");
			}
			palette.add(color);
		}
		return Collections.unmodifiableSet(palette);
	}
}
