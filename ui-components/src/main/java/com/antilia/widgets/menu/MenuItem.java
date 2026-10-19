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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.icon.IIcon;

/**
 * The base of the items of ui-components: the label, and the tooltip, the icon and whether the
 * item shows, may be chosen and is selected, which the setters change. The icon and the flags are
 * models, read each time the menu renders, so that they may change: an item enabled while a form
 * is valid, say, or showing one icon or another.
 *
 * @since 11.0.0
 */
public abstract class MenuItem implements IMenuItem
{
	private static final long serialVersionUID = 1L;

	private final IModel<String> label;

	private IModel<String> tooltip;

	private IModel<IIcon> icon;

	private IModel<Boolean> visible = ALWAYS;

	private IModel<Boolean> enabled = ALWAYS;

	private IModel<Boolean> selected = NEVER;

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
	public IModel<IIcon> getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon before the label, read each time the menu renders; {@code null}, or a
	 *            model of {@code null}, for none
	 * @return {@code this}
	 */
	public MenuItem setIcon(IModel<IIcon> icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * @param icon
	 *            the icon before the label, always the same; {@code null} for none
	 * @return {@code this}
	 */
	public MenuItem setIcon(IIcon icon)
	{
		return setIcon(icon == null ? null : Model.of(icon));
	}

	@Override
	public IModel<Boolean> getVisible()
	{
		return visible;
	}

	/**
	 * @param visible
	 *            whether the menu shows the item, read each time the menu renders; {@code null} for
	 *            always
	 * @return {@code this}
	 */
	public MenuItem setVisible(IModel<Boolean> visible)
	{
		this.visible = visible == null ? ALWAYS : visible;
		return this;
	}

	/**
	 * @param visible
	 *            whether the menu shows the item, always the same
	 * @return {@code this}
	 */
	public MenuItem setVisible(boolean visible)
	{
		return setVisible(Model.of(visible));
	}

	@Override
	public IModel<Boolean> getEnabled()
	{
		return enabled;
	}

	/**
	 * @param enabled
	 *            whether the user may choose the item, read each time the menu renders;
	 *            {@code null} for always
	 * @return {@code this}
	 */
	public MenuItem setEnabled(IModel<Boolean> enabled)
	{
		this.enabled = enabled == null ? ALWAYS : enabled;
		return this;
	}

	/**
	 * @param enabled
	 *            whether the user may choose the item, always the same
	 * @return {@code this}
	 */
	public MenuItem setEnabled(boolean enabled)
	{
		return setEnabled(Model.of(enabled));
	}

	@Override
	public IModel<Boolean> getSelected()
	{
		return selected;
	}

	/**
	 * @param selected
	 *            whether the item is the selected one, read each time the menu renders;
	 *            {@code null} for never
	 * @return {@code this}
	 */
	public MenuItem setSelected(IModel<Boolean> selected)
	{
		this.selected = selected == null ? NEVER : selected;
		return this;
	}

	/**
	 * @param selected
	 *            whether the item is the selected one, always the same
	 * @return {@code this}
	 */
	public MenuItem setSelected(boolean selected)
	{
		return setSelected(Model.of(selected));
	}

	@Override
	public void detach()
	{
		label.detach();
		if (tooltip != null)
		{
			tooltip.detach();
		}
		if (icon != null)
		{
			icon.detach();
		}
		visible.detach();
		enabled.detach();
		selected.detach();
	}
}
