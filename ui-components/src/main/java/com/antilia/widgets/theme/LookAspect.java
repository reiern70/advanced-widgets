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
package com.antilia.widgets.theme;

import java.util.Locale;

/**
 * What a {@link LookPart} says about its element. A {@link CompoundLook} takes each aspect from a
 * look of its choice: Bootstrap's classes with the colors and the sizes of Antilia's, for example.
 *
 * @since 11.0.0
 */
public enum LookAspect
{
	/** What the element is: a dialog's header, a table, a button... */
	BASE,
	/** The colors of the element: a primary button, a notification of an error... */
	COLOR,
	/** The size of the element: a small button, a large dialog... */
	SIZE,
	/** A state the element is in: the option of a list that is active, a row selected... */
	STATE;

	/**
	 * @return the name of the aspect in the browser, such as {@code color}
	 */
	public String getName()
	{
		return name().toLowerCase(Locale.ROOT);
	}
}
