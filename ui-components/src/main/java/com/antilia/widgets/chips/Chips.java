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
package com.antilia.widgets.chips;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The chips showing the chosen values of a multiple {@link com.antilia.widgets.dropdown.Dropdown
 * drop-down} or an {@link com.antilia.widgets.autocomplete.Autocomplete autocomplete}: a label in
 * the colors of the {@link com.antilia.widgets.theme.Theme theme} with an x removing the value.
 * {@code Wicket.Chips.create(text, removeLabel)} from {@link #JS} makes one.
 *
 * @since 11.0.0
 */
public final class Chips
{
	/** The script making the chips, {@code Wicket.Chips}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Chips.class,
		"antilia-chips.js");

	/** The style sheet of the chips. */
	public static final ResourceReference CSS = new CssResourceReference(Chips.class,
		"antilia-chips.css");

	private Chips()
	{
	}

	/**
	 * Renders what the chips need in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
