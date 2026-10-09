/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.model.IModel;

import com.antilia.widgets.icon.IIcon;

/**
 * What the nodes of a {@link Tree} and a {@link TreeGrid} ask of their tree: how to render a
 * node, whether it is selected, and what a click on it does.
 *
 * @param <T>
 *            the type of the nodes
 */
interface INodeRenderer<T>
{
	/**
	 * @return the tree
	 */
	AbstractTree<T> asTree();

	/**
	 * @return whether the node is a leaf, one without children
	 */
	default boolean isLeaf(T node)
	{
		return !asTree().getProvider().hasChildren(node);
	}

	/**
	 * @return the icon of a node
	 */
	IIcon getIcon(T node, boolean leaf, boolean expanded);

	/**
	 * @return the icon of a slot, the application's own or the tree's
	 */
	IIcon getIcon(TreeIcon slot);

	/**
	 * @return the text of a node
	 */
	IModel<?> newLabelModel(IModel<T> node);

	/**
	 * @return whether the node is the leaf selected
	 */
	boolean isSelected(T node);

	/**
	 * Called when the user clicks a node: a leaf is selected, a folder expanded or collapsed.
	 */
	void onNodeClick(AjaxRequestTarget target, T node);
}
