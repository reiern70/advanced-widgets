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
package com.antiliacode.widgets.tabs;

import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookPart;

/**
 * A look for the tabs of a {@link TabbedPanel}, applied by a {@link TabsStyleBehavior}. Every
 * look takes its colors from the {@link com.antiliacode.widgets.theme.Theme theme} the panel
 * is in, and has colors of its own outside a theme.
 *
 * @since 11.0.0
 */
public enum TabsStyle
{
	/** Tabs on top of the panel, the selected one joined to it. */
	TABS(LookPart.TABS_TABS, LookPart.TAB_ROW_TABS),

	/** Rounded buttons, the selected one filled with the theme's primary color. */
	PILLS(LookPart.TABS_PILLS, LookPart.TAB_ROW_PILLS),

	/** Plain titles, the selected one underlined in the theme's primary color. */
	UNDERLINE(LookPart.TABS_UNDERLINE, LookPart.TAB_ROW_UNDERLINE);

	private final LookPart part;

	private final LookPart rowPart;

	TabsStyle(LookPart part, LookPart rowPart)
	{
		this.part = part;
		this.rowPart = rowPart;
	}

	/**
	 * @return the CSS class putting the look on a tabbed panel
	 */
	public String getCssClass()
	{
		return Look.ANTILIA.getCssClass(part);
	}

	/**
	 * @return the part of a look the tabbed panel is in this style
	 */
	public LookPart getPart()
	{
		return part;
	}

	/**
	 * @return the part of a look the row of the tabs is in this style
	 */
	public LookPart getRowPart()
	{
		return rowPart;
	}
}
