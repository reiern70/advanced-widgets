/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.theme;

import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * A color theme shared by the components of ui-components.
 * <p>
 * A theme is a CSS class defining a set of custom properties (CSS variables) named
 * {@code --antilia-theme-*}, which the style sheets of the components read: for example
 * {@code --antilia-theme-primary} for the header of a table and the bar of a progress bar. Put on
 * an element with a {@link ThemeBehavior}, the theme applies to every themable component inside
 * it, so one theme on the page styles them all alike. Themes are switched by class only, so they
 * work under a strict Content Security Policy.
 * <p>
 * An application can define a theme of its own as a CSS class setting the same properties; the
 * properties are listed in {@link #CSS}. An element in such a theme carries that class and
 * {@link #MARKER_CSS_CLASS}, which components use for the parts they style only inside a theme,
 * such as the header of a modal dialog.
 * <p>
 * A theme gives the colors only; the shapes and the CSS classes of the parts of the components are
 * an {@link ILook}'s. {@link #BOOTSTRAP} and {@link #BOOTSTRAP_DARK} take the colors of the
 * Bootstrap on the page, whatever its look.
 *
 * @since 11.0.0
 */
public enum Theme
{
	/** Wicket orange: orange headers on white and light orange rows. */
	DEFAULT("antilia-theme-default"),

	/** Bright blue. */
	BLUE("antilia-theme-blue"),

	/** Red. */
	RED("antilia-theme-red"),

	/** Grey. */
	GREY("antilia-theme-grey"),

	/** Green. */
	GREEN("antilia-theme-green"),

	/** Dark blue headers on white and light blue rows. */
	NAVY("antilia-theme-navy"),

	/** Light blue, softer than {@link #BLUE}. */
	LIGHT_BLUE("antilia-theme-light-blue"),

	/** Magenta headers on white and light pink-violet rows. */
	MAGENTA("antilia-theme-magenta"),

	/** Rose: dusty pink headers on white and blush rows, softer than {@link #MAGENTA}. */
	ROSE("antilia-theme-rose"),

	/** Light text on dark surfaces, with dark form controls. */
	DARK("antilia-theme-dark", true),

	/**
	 * The colors of the Bootstrap on the page, read from its custom properties ({@code --bs-*}),
	 * so a theme of Bootstrap's colors the components too; Bootstrap's light mode.
	 */
	BOOTSTRAP("antilia-theme-bootstrap"),

	/** The colors of the Bootstrap on the page in its dark mode. */
	BOOTSTRAP_DARK("antilia-theme-bootstrap-dark", true);

	/**
	 * The CSS class marking an element as being in a theme, put on it by {@link ThemeBehavior}
	 * next to the class of the theme.
	 */
	public static final String MARKER_CSS_CLASS = "antilia-theme";

	/** The style sheet defining the themes. */
	public static final ResourceReference CSS = new CssResourceReference(Theme.class,
		"antilia-theme.css");

	/**
	 * The attribute switching Bootstrap's color mode, {@code light} or {@code dark}, for Bootstrap's
	 * own components inside the element.
	 */
	public static final String BOOTSTRAP_MODE_ATTRIBUTE = "data-bs-theme";

	private final String cssClass;

	private final boolean dark;

	Theme(String cssClass)
	{
		this(cssClass, false);
	}

	Theme(String cssClass, boolean dark)
	{
		this.cssClass = cssClass;
		this.dark = dark;
	}

	/**
	 * @return the CSS class applying the theme
	 */
	public String getCssClass()
	{
		return cssClass;
	}

	/**
	 * @return whether the theme is dark: light text on dark surfaces
	 */
	public boolean isDark()
	{
		return dark;
	}

	/**
	 * @return whether the theme takes its colors from Bootstrap
	 */
	public boolean isBootstrap()
	{
		return this == BOOTSTRAP || this == BOOTSTRAP_DARK;
	}
}
