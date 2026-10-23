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
package com.antiliacode.widgets.popover;

import java.util.Locale;

import com.antiliacode.widgets.theme.LookPart;

/**
 * The designs of a {@link Popover}, in the colors of the {@link com.antiliacode.widgets.theme.Theme theme}
 * and of its {@link PopoverVariant variant}, from the style sheet of the popovers.
 *
 * @since 11.0.0
 */
public enum PopoverDesign
{
	/** The surface of the theme with a stripe of its color along the top. */
	CARD,

	/** Filled with its color. */
	SOLID,

	/** Flat, drawn by a line of its color. */
	OUTLINE,

	/** Frosted, the page showing blurred through it. */
	GLASS,

	/** The colors of the theme the other way round, small, as a tooltip. */
	INVERSE,

	/** Tinted with its color, a bar along its side and a badge saying what it stands for. */
	CALLOUT,

	/** A border of its color and of the accent of the theme flowing into each other. */
	AURORA,

	/** A speech bubble, outlined in ink with a hard shadow. */
	COMIC,

	/** A line of its color glowing around it. */
	NEON;

	/**
	 * @return the name of the design in the script and in its CSS class,
	 *         {@code antilia-popover-<name>}
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look a popover is in this design
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("POPOVER_" + name());
	}
}
