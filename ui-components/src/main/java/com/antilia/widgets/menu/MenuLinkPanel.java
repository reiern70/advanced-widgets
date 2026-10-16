/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.menu;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.theme.LookBehavior;
import com.antilia.widgets.theme.LookPart;

/**
 * The link of an {@link IMenuItem}, the one the item {@link IMenuItem#getLink(String) creates},
 * with the icon of the item and its label in it and its tooltip as its title. It shows while the
 * item {@link IMenuItem#isVisible() does} and is enabled while the item {@link IMenuItem#isEnabled()
 * is}. {@link Menu} renders its items with it; it serves wherever the link of an item is wanted on
 * its own, in a toolbar for example.
 *
 * @since 11.0.0
 */
public class MenuLinkPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final IMenuItem item;

	private final Component link;

	/**
	 * @param id
	 *            component id
	 * @param item
	 *            the item, which has a link
	 */
	public MenuLinkPanel(String id, IMenuItem item)
	{
		super(id);
		this.item = Args.notNull(item, "item");
		link = item.getLink("link");
		if (link == null)
		{
			throw new IllegalArgumentException("The item " + item + " has no link");
		}
		if (!(link instanceof MarkupContainer container))
		{
			throw new IllegalArgumentException("The link of the item " + item +
				" must be a container, to hold its icon and its label");
		}
		add(link);
		container.add(newIcon("icon", item));
		container.add(newLabel("label", item));
		IModel<String> tooltip = item.getTooltip();
		if (tooltip != null)
		{
			link.add(AttributeModifier.replace("title", assigned(tooltip, link)));
		}
	}

	/**
	 * @return the link of the item, the component it created
	 */
	public Component getLink()
	{
		return link;
	}

	/**
	 * @return the item
	 */
	public IMenuItem getItem()
	{
		return item;
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		setVisible(item.isVisible());
		setEnabled(item.isEnabled());
	}

	/**
	 * @param id
	 *            component id
	 * @param item
	 *            an item
	 * @return the icon of the item, its markup as is, read each time it renders; hidden when the
	 *         item has none
	 */
	public static Component newIcon(String id, IMenuItem item)
	{
		return new Label(id, () -> {
			IIcon icon = iconOf(item);
			return icon != null ? icon.getMarkup() : null;
		})
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(iconOf(item) != null);
			}
		}.setEscapeModelStrings(false).add(new LookBehavior(LookPart.MENU_ICON));
	}

	/**
	 * @return the icon of an item, {@code null} for none
	 */
	private static IIcon iconOf(IMenuItem item)
	{
		IModel<IIcon> icon = item.getIcon();
		return icon != null ? icon.getObject() : null;
	}

	/**
	 * @param id
	 *            component id
	 * @param item
	 *            an item
	 * @return the label of the item
	 */
	public static Component newLabel(String id, IMenuItem item)
	{
		return new Label(id, item.getLabel()).add(new LookBehavior(LookPart.MENU_LABEL));
	}

	/**
	 * @return the model as a component uses it: one taking the component, such as a
	 *         ResourceModel, given it
	 */
	static IModel<String> assigned(IModel<String> model, Component component)
	{
		return model instanceof IComponentAssignedModel<String> assigned ?
			assigned.wrapOnAssignment(component) : model;
	}
}
