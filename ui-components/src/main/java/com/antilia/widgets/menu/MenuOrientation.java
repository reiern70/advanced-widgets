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
package com.antilia.widgets.menu;

import java.util.Locale;

import com.antilia.widgets.theme.LookPart;

/**
 * Where the bar of a {@link Menu} is.
 *
 * @since 11.0.0
 */
public enum MenuOrientation
{
	/** Along the top: its items side by side, their submenus popping up below them. */
	HORIZONTAL,

	/** Down the side: its items one above the other, their submenus beside them or in place. */
	VERTICAL;

	/**
	 * @return the name of the orientation in the script and in the CSS classes
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look the menu is in this orientation
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("MENU_" + name());
	}
}
