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
package com.antiliacode.widgets.menu;

import org.apache.wicket.Component;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.icon.IIcon;

/**
 * An item of a {@link Menu}: its label, maybe with an icon before it and a tooltip, and the link the
 * menu renders for it, which leads somewhere or does something. ui-components has an item for each
 * kind of link: a {@link BookmarkableMenuItem} leads to a page, an {@link AjaxMenuItem} calls the
 * server via Ajax and a {@link LinkMenuItem} with a request of its own. An item that is an
 * {@link IMenu} opens a submenu of its items instead, and a {@link MenuSeparator} is a line between
 * items.
 * <p>
 * The menu renders an item with a {@link MenuLinkPanel}: the link, with the icon and the label in
 * it and the tooltip as its title. An item of one's own returns any link of Wicket, such as a
 * DownloadLink, or any other component on an {@code <a>}. The icon and whether the item shows, may
 * be chosen and is selected are models, read each time the menu renders, so that they may change.
 *
 * @since 11.0.0
 */
public interface IMenuItem extends IDetachable
{
	/**
	 * @return the label of the item
	 */
	IModel<String> getLabel();

	/**
	 * @return the tooltip of the item, the title of its link; {@code null}, the default, for none
	 */
	default IModel<String> getTooltip()
	{
		return null;
	}

	/**
	 * @return the icon before the label, read each time the menu renders, so that the item may show
	 *         one icon or another; {@code null}, the default, or a model of {@code null} for none
	 */
	default IModel<IIcon> getIcon()
	{
		return null;
	}

	/**
	 * Creates the component of the item, which the menu puts the icon and the label in: usually a
	 * link, but any component whose markup is an {@code <a>} will do.
	 *
	 * @param id
	 *            the id the component must have
	 * @return the component
	 */
	Component getLink(String id);

	/**
	 * @return whether the menu shows the item, read each time the menu renders; always
	 *         {@code true} by default
	 */
	default IModel<Boolean> getVisible()
	{
		return ALWAYS;
	}

	/**
	 * @return whether the user may choose the item, read each time the menu renders; always
	 *         {@code true} by default: a disabled item shows, dimmed, and does nothing
	 */
	default IModel<Boolean> getEnabled()
	{
		return ALWAYS;
	}

	/**
	 * @return whether the item shows: its {@link #getVisible() model} says so
	 */
	default boolean isVisible()
	{
		return isTrue(getVisible());
	}

	/**
	 * @return whether the user may choose the item: its {@link #getEnabled() model} says so
	 */
	default boolean isEnabled()
	{
		return isTrue(getEnabled());
	}

	/**
	 * @return whether the item is the selected one, read each time the menu renders, which the menu
	 *         marks as current, and the items whose submenus hold it; never by default. Each item
	 *         says so of its own, so that several menus on a page, and items calling the server
	 *         via Ajax, have each their own selection.
	 */
	default IModel<Boolean> getSelected()
	{
		return NEVER;
	}

	/**
	 * @return whether the item is selected: its {@link #getSelected() model} says so
	 */
	default boolean isSelected()
	{
		IModel<Boolean> selected = getSelected();
		return selected != null && Boolean.TRUE.equals(selected.getObject());
	}

	@Override
	default void detach()
	{
	}

	/** The model of {@link #getVisible()} and {@link #getEnabled()} by default: always true. */
	IModel<Boolean> ALWAYS = () -> Boolean.TRUE;

	/** The model of {@link #getSelected()} by default: always false. */
	IModel<Boolean> NEVER = () -> Boolean.FALSE;

	/**
	 * @param model
	 *            a model of a flag, {@code null} counting as true
	 * @return whether the flag is set: the model is {@code null} or holds {@code true}
	 */
	static boolean isTrue(IModel<Boolean> model)
	{
		return model == null || Boolean.TRUE.equals(model.getObject());
	}
}
