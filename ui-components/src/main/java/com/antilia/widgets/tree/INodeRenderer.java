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
package com.antilia.widgets.tree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree.State;
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
	 * @return whether the node is the one selected
	 */
	boolean isSelected(T node);

	/**
	 * @return whether the node is selected when the user clicks it: a leaf, or a folder when the
	 *         tree selects folders too
	 */
	boolean isSelectable(T node);

	/**
	 * Selects a node, as a click on it does.
	 */
	void select(T node, AjaxRequestTarget target);

	/**
	 * Called when the user clicks a node: a node that can be selected is, a folder expanded if it
	 * is collapsed; a folder that cannot be selected is expanded or collapsed.
	 */
	default void onNodeClick(AjaxRequestTarget target, T node)
	{
		AbstractTree<T> tree = asTree();
		if (isSelectable(node))
		{
			select(node, target);
			if (!isLeaf(node) && tree.getState(node) != State.EXPANDED)
			{
				tree.expand(node);
			}
		}
		else if (tree.getState(node) == State.EXPANDED)
		{
			tree.collapse(node);
		}
		else
		{
			tree.expand(node);
		}
	}
}
