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
 * class to the component's tag and contributing {@link Theme#CSS}; and an {@link ILook}, if given,
 * by adding the class {@code antilia-look-<name>} of each look it is made of and the attribute
 * {@code data-antilia-look}, and rendering what the look needs.
 * <p>
 * The theme and the look are read from models each time the component renders, so re-rendering
 * the component, for example via Ajax, switches them.
 *
 * @since 11.0.0
 */
public class ThemeBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The attribute naming the look of an element and of everything inside it. */
	public static final String LOOK_ATTRIBUTE = "data-antilia-look";

	/** The start of the class naming a look an element is in: antilia-look-bootstrap, ... */
	public static final String LOOK_CLASS_PREFIX = "antilia-look-";

	private final IModel<Theme> theme;

	private final IModel<? extends ILook> look;

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
	 *            the theme
	 * @param look
	 *            the look
	 */
	public ThemeBehavior(Theme theme, ILook look)
	{
		this(Model.of(Args.notNull(theme, "theme")), Model.of(Args.notNull(look, "look")));
	}

	/**
	 * @param theme
	 *            the theme, none if the model holds {@code null}
	 */
	public ThemeBehavior(IModel<Theme> theme)
	{
		this(theme, null);
	}

	/**
	 * @param theme
	 *            the theme, none if the model holds {@code null}
	 * @param look
	 *            the look, none if {@code null} or if the model holds {@code null}: then the
	 *            components inside are in the look of those around
	 */
	public ThemeBehavior(IModel<Theme> theme, IModel<? extends ILook> look)
	{
		this.theme = Args.notNull(theme, "theme");
		this.look = look;
	}

	/**
	 * @return the look the behavior puts on the component, {@code null} for none
	 */
	public ILook getLook()
	{
		return look != null ? look.getObject() : null;
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		Theme current = theme.getObject();
		if (current != null)
		{
			tag.append("class", Theme.MARKER_CSS_CLASS + " " + current.getCssClass(), " ");
			if (current.isBootstrap())
			{
				tag.put(Theme.BOOTSTRAP_MODE_ATTRIBUTE, current.isDark() ? "dark" : "light");
			}
		}
		ILook currentLook = getLook();
		if (currentLook != null)
		{
			// a class for each look it is made of, so the style sheet of each applies
			for (ILook each : currentLook.getLooks())
			{
				tag.append("class", LOOK_CLASS_PREFIX + each.getName(), " ");
			}
			tag.put(LOOK_ATTRIBUTE, currentLook.getName());
			currentLook.onComponentTag(tag, current);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(Theme.CSS));
		ILook currentLook = getLook();
		if (currentLook != null)
		{
			currentLook.renderHead(response);
		}
	}

	@Override
	public void detach(Component component)
	{
		theme.detach();
		if (look != null)
		{
			look.detach();
		}
	}
}
