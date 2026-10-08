/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.tree;

import java.util.EnumMap;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.advanced.widgets.icon.IIcon;

/**
 * What a {@link Tree} and a {@link TreeGrid} share: the leaf selected, kept as the model the
 * provider gives it, and the icons of the application's own.
 *
 * @param <T>
 *            the type of the nodes
 */
final class TreeSupport<T> implements IDetachable
{
	private static final long serialVersionUID = 1L;

	private final Map<TreeIcon, IIcon> icons = new EnumMap<>(TreeIcon.class);

	private IModel<T> selected;

	/**
	 * @return the icon of a slot, the application's own or the tree's
	 */
	IIcon getIcon(TreeIcon slot)
	{
		return icons.getOrDefault(slot, slot);
	}

	void setIcon(TreeIcon slot, IIcon icon)
	{
		if (icon == null)
		{
			icons.remove(slot);
		}
		else
		{
			icons.put(slot, icon);
		}
	}

	/**
	 * @return the icon of a node by its kind: a leaf, an expanded or a collapsed folder
	 */
	IIcon getIcon(boolean leaf, boolean expanded)
	{
		return getIcon(leaf ? TreeIcon.LEAF : expanded ? TreeIcon.FOLDER_OPEN : TreeIcon.FOLDER);
	}

	T getSelected()
	{
		return selected != null ? selected.getObject() : null;
	}

	boolean isSelected(AbstractTree<T> tree, T node)
	{
		if (selected == null || node == null)
		{
			return false;
		}
		IModel<T> model = tree.getProvider().model(node);
		try
		{
			return model.equals(selected);
		}
		finally
		{
			model.detach();
		}
	}

	/**
	 * @param node
	 *            the node to select, {@code null} for none
	 * @return the node selected before, {@code null} if none was
	 */
	T select(ITreeProvider<T> provider, T node)
	{
		T previous = getSelected();
		selected = node != null ? provider.model(node) : null;
		return previous;
	}

	@Override
	public void detach()
	{
		if (selected != null)
		{
			selected.detach();
		}
	}
}
