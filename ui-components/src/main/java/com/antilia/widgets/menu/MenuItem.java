/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.icon.IIcon;

/**
 * The base of the items of ui-components: the label, and the tooltip, the icon and whether the
 * item shows and may be chosen, which the setters change.
 *
 * @since 11.0.0
 */
public abstract class MenuItem implements IMenuItem
{
	private static final long serialVersionUID = 1L;

	private final IModel<String> label;

	private IModel<String> tooltip;

	private IIcon icon;

	private boolean visible = true;

	private boolean enabled = true;

	/**
	 * @param label
	 *            the label of the item
	 */
	protected MenuItem(IModel<String> label)
	{
		this.label = Args.notNull(label, "label");
	}

	@Override
	public IModel<String> getLabel()
	{
		return label;
	}

	@Override
	public IModel<String> getTooltip()
	{
		return tooltip;
	}

	/**
	 * @param tooltip
	 *            the tooltip of the item, {@code null} for none
	 * @return {@code this}
	 */
	public MenuItem setTooltip(IModel<String> tooltip)
	{
		this.tooltip = tooltip;
		return this;
	}

	@Override
	public IIcon getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon before the label, {@code null} for none
	 * @return {@code this}
	 */
	public MenuItem setIcon(IIcon icon)
	{
		this.icon = icon;
		return this;
	}

	@Override
	public boolean isVisible()
	{
		return visible;
	}

	/**
	 * @param visible
	 *            whether the menu shows the item
	 * @return {@code this}
	 */
	public MenuItem setVisible(boolean visible)
	{
		this.visible = visible;
		return this;
	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	/**
	 * @param enabled
	 *            whether the user may choose the item
	 * @return {@code this}
	 */
	public MenuItem setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		return this;
	}

	@Override
	public void detach()
	{
		label.detach();
		if (tooltip != null)
		{
			tooltip.detach();
		}
	}
}
