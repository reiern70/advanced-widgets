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
package com.antiliacode.widgets.split;

import java.util.Locale;

/**
 * A pane of a {@link SplitPanel}, the one a collapsible split panel has collapsed.
 *
 * @since 11.0.0
 */
public enum SplitPane
{
	/** The left or top pane. */
	FIRST,

	/** The right or bottom pane. */
	SECOND;

	/**
	 * @return the name of the pane in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param name
	 *            the name of a pane in the script, {@code first} or {@code second}
	 * @return the pane, {@code null} for any other name
	 */
	public static SplitPane fromCssName(String name)
	{
		for (SplitPane pane : values())
		{
			if (pane.cssName().equals(name))
			{
				return pane;
			}
		}
		return null;
	}
}
