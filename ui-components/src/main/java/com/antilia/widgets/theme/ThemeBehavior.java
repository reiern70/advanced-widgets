/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.theme;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Args;

/**
 * Applies a {@link Theme} to a component and everything inside it, by adding the theme's CSS
 * class to the component's tag and contributing {@link Theme#CSS}.
 * <p>
 * The theme is read from a model each time the component renders, so re-rendering the component,
 * for example via Ajax, switches it.
 *
 * @since 11.0.0
 */
public class ThemeBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	private final IModel<Theme> theme;

	/**
	 * @param theme
	 *            the theme
	 */
	public ThemeBehavior(Theme theme)
	{
		this(Model.of(Args.notNull(theme, "theme")));
	}

	/**
	 * @param theme
	 *            the theme, none if the model holds {@code null}
	 */
	public ThemeBehavior(IModel<Theme> theme)
	{
		this.theme = Args.notNull(theme, "theme");
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		Theme current = theme.getObject();
		if (current != null)
		{
			tag.append("class", Theme.MARKER_CSS_CLASS + " " + current.getCssClass(), " ");
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(Theme.CSS));
	}

	@Override
	public void detach(Component component)
	{
		theme.detach();
	}
}
