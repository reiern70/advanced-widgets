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
package com.antiliacode.widgets.popover;

import java.util.Locale;

import com.antiliacode.widgets.theme.LookPart;

/**
 * What a {@link Popover} stands for, which sets the color of its stripe and its title, from the
 * {@link com.antiliacode.widgets.theme.Theme theme}.
 *
 * @since 11.0.0
 */
public enum PopoverVariant
{
	/** In the primary color of the theme. */
	DEFAULT,

	/** In the color of information. */
	INFO,

	/** In the color of what went well. */
	SUCCESS,

	/** In the color of what asks for attention. */
	WARNING,

	/** In the color of what went wrong. */
	ERROR;

	/**
	 * @return the name of the variant in the script
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the part of a look a popover is in this variant
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("POPOVER_" + name());
	}
}
