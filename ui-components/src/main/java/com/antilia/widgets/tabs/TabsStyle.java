/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tabs;

/**
 * A look for the tabs of a {@link TabbedPanel}, applied by a {@link TabsStyleBehavior}. Every
 * look takes its colors from the {@link com.antilia.widgets.theme.Theme theme} the panel
 * is in, and has colors of its own outside a theme.
 *
 * @since 11.0.0
 */
public enum TabsStyle
{
	/** Tabs on top of the panel, the selected one joined to it. */
	TABS("antilia-tabs-tabs"),

	/** Rounded buttons, the selected one filled with the theme's primary color. */
	PILLS("antilia-tabs-pills"),

	/** Plain titles, the selected one underlined in the theme's primary color. */
	UNDERLINE("antilia-tabs-underline");

	private final String cssClass;

	TabsStyle(String cssClass)
	{
		this.cssClass = cssClass;
	}

	/**
	 * @return the CSS class putting the look on a tabbed panel
	 */
	public String getCssClass()
	{
		return cssClass;
	}
}
