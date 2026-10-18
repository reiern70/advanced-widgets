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
package com.antilia.widgets.tabs;

import java.util.Locale;

import com.antilia.widgets.theme.Look;
import com.antilia.widgets.theme.LookPart;

/**
 * The designs of the tabs of {@link Tabs}, in the colors of the {@link com.antilia.widgets.theme.Theme
 * theme} they are in. Every design works in both {@link TabsOrientation orientations}; some are made
 * for tabs along the top, others for tabs down the side. The first three are those
 * {@link TabsStyle} gives Wicket's TabbedPanel too.
 *
 * @since 11.0.0
 */
public enum TabsDesign
{
	/** Tabs joined to the panel. */
	TABS,

	/** Rounded buttons, the selected one filled with the theme's primary color. */
	PILLS,

	/** Plain titles, the selected one underlined in the theme's primary color. */
	UNDERLINE,

	/** A segmented control: the tabs on a track, the selected one raised. For tabs along the top. */
	SEGMENTED,

	/** Folder tabs, overlapping, the selected one in front of the panel. For tabs along the top. */
	FOLDER,

	/** Cards lifted from the page, the selected one most. For tabs along the top. */
	LIFTED,

	/** The side bar of an application, an accent on the selected entry. For tabs down the side. */
	SIDEBAR,

	/**
	 * Numbered steps joined by a line, those before the selected one done. For tabs down the side.
	 */
	STEPS,

	/** Cards, the selected one pointing at the panel. For tabs down the side. */
	CARDS;

	/**
	 * @return the name of the design in the script
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the CSS class putting the design on the tabs, that of its part in
	 *         {@link Look#ANTILIA}
	 */
	public String getCssClass()
	{
		return Look.ANTILIA.getCssClass(getPart());
	}

	/**
	 * @return the part of a look the tabs are in this design
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("TABS_" + name());
	}

	/**
	 * @return the part of a look the list of the tabs is in this design, such as Bootstrap's
	 *         {@code nav nav-tabs}; {@code null} for a design of the components' own, which has
	 *         none
	 */
	public LookPart getListPart()
	{
		for (LookPart part : LookPart.values())
		{
			if (part.name().equals("TABS_LIST_" + name()))
			{
				return part;
			}
		}
		return null;
	}
}
