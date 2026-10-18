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
package com.antilia.widgets.demo.datatable;

import com.antilia.widgets.icon.FontAwesomeIcon;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;

/**
 * The icons the actions of the dynamic table examples are drawn with.
 */
public enum ActionIcons
{
	/** The icons built into ui-components. */
	BUILT_IN,

	/** The solid icons of Font Awesome as inline SVG. */
	SVG,

	/** The solid icons of Font Awesome drawn by its font. */
	FONT_AWESOME;

	/**
	 * @return the icon of this set
	 */
	public IIcon pick(IIcon builtIn, SvgIcon svg, FontAwesomeIcon fontAwesome)
	{
		return switch (this)
		{
			case BUILT_IN -> builtIn;
			case SVG -> svg;
			case FONT_AWESOME -> fontAwesome;
		};
	}
}
