/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.icon.IIcon;

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
 * DownloadLink.
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
	 * @return the icon before the label; {@code null}, the default, for none
	 */
	default IIcon getIcon()
	{
		return null;
	}

	/**
	 * Creates the link of the item, which the menu puts the icon and the label in. Its markup is an
	 * {@code <a>}.
	 *
	 * @param id
	 *            the id the link must have
	 * @return the link
	 */
	AbstractLink getLink(String id);

	/**
	 * @return whether the menu shows the item, {@code true} by default
	 */
	default boolean isVisible()
	{
		return true;
	}

	/**
	 * @return whether the user may choose the item, {@code true} by default: a disabled item shows,
	 *         dimmed, and does nothing
	 */
	default boolean isEnabled()
	{
		return true;
	}

	/**
	 * @param page
	 *            the page the menu is on
	 * @return whether the item leads to the page, which the menu marks as current, and the items
	 *         whose submenus hold it; {@code false} by default
	 */
	default boolean isCurrent(Page page)
	{
		return false;
	}

	@Override
	default void detach()
	{
	}
}
