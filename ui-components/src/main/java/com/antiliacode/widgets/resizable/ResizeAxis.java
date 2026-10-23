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
package com.antiliacode.widgets.resizable;

import java.util.Locale;

/**
 * The dimensions the user resizes an element in, with a {@link ResizableBehavior} or in a
 * {@link com.antiliacode.widgets.codeeditor.CodeEditor}.
 *
 * @since 11.0.0
 */
public enum ResizeAxis
{
	/** The height alone, by the bottom edge. */
	VERTICAL,

	/** The width alone, by the right edge. */
	HORIZONTAL,

	/** Both, by the bottom right corner. */
	BOTH;

	/**
	 * @return the name of the axis in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return whether the user changes the width
	 */
	public boolean isHorizontal()
	{
		return this != VERTICAL;
	}

	/**
	 * @return whether the user changes the height
	 */
	public boolean isVertical()
	{
		return this != HORIZONTAL;
	}

	/**
	 * @param name
	 *            the name of an axis in the script, such as {@code vertical}
	 * @return the axis, {@code null} for none known
	 */
	public static ResizeAxis fromCssName(String name)
	{
		for (ResizeAxis axis : values())
		{
			if (axis.cssName().equals(name))
			{
				return axis;
			}
		}
		return null;
	}
}
