/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.chips;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The chips showing the chosen values of a multiple {@link com.advanced.widgets.dropdown.Dropdown
 * drop-down} or an {@link com.advanced.widgets.autocomplete.Autocomplete autocomplete}: a label in
 * the colors of the {@link com.advanced.widgets.theme.Theme theme} with an x removing the value.
 * {@code Wicket.Chips.create(text, removeLabel)} from {@link #JS} makes one.
 *
 * @since 11.0.0
 */
public final class Chips
{
	/** The script making the chips, {@code Wicket.Chips}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Chips.class,
		"wicket-chips.js");

	/** The style sheet of the chips. */
	public static final ResourceReference CSS = new CssResourceReference(Chips.class,
		"wicket-chips.css");

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
