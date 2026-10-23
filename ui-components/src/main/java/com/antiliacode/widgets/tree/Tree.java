/*
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
package com.antiliacode.widgets.tree;

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.icon.IIcon;
import com.antiliacode.widgets.scrollbar.ScrollbarBehavior;
import com.antiliacode.widgets.theme.LookPart;
import com.antiliacode.widgets.theme.Looks;

/**
 * A tree of Wicket components: a {@link NestedTree} of wicket-extensions, whose nodes render the
 * rows of the trees rendered in the browser ({@link JsonBasedTree}), with icons of their own
 * ({@link TreeIcon}) in place of the images of the extensions' themes.
 * <p>
 * A node is a button holding its icon and its text: a click on a folder, or on its arrow, expands
 * or collapses it, through Ajax; a click on a leaf selects it, repaints the leaf selected before
 * and the new one, and calls {@link #onSelect(AjaxRequestTarget, Object)}, where the application
 * updates what shows the leaf. Only leaves are selected, unless
 * {@link #setSelectBranches(boolean)} has folders selected too: then a click on a folder selects
 * it, and expands it if it is collapsed, its arrow still expanding and collapsing it. The folders
 * tell screen readers whether they are expanded, the node selected that it is the current one.
 * <p>
 * The text of a node is the node itself, as a string, unless
 * {@link #newLabelModel(IModel)} says otherwise; its icon is a folder, open or closed, or a leaf,
 * unless {@link #getIcon(Object, boolean, boolean)} says otherwise, or the application gives
 * icons of its own with {@link #setIcon(TreeIcon, IIcon)}. The tooltips of the arrows are the
 * texts {@code Tree.expand} and {@code Tree.collapse} of the bundle of ui-components.
 * <p>
 * Given a {@link #setHeight(int, BodyHeightUnits) height}, the tree scrolls itself, with slim
 * scroll bars in the colors of the theme; else the page scrolls.
 * <p>
 * The tree carries the CSS class {@value #CSS_CLASS} and takes its colors from the
 * {@link com.antiliacode.widgets.theme.Theme theme} it is rendered in, if any.
 *
 * @param <T>
 *            the type of the nodes
 * @since 11.0.0
 */
public class Tree<T> extends NestedTree<T> implements INodeRenderer<T>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the tree. */
	public static final String CSS_CLASS = "antilia-tree";

	/** The style sheet of the trees and the tree grids, of Wicket components and of JSON. */
	public static final ResourceReference CSS = new CssResourceReference(Tree.class,
		"antilia-tree.css");

	private final TreeSupport<T> support = new TreeSupport<>();

	/**
	 * @param id
	 *            component id
	 * @param provider
	 *            the provider of the nodes
	 */
	public Tree(String id, ITreeProvider<T> provider)
	{
		this(id, provider, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param provider
	 *            the provider of the nodes
	 * @param state
	 *            the folders expanded, {@code null} for a set of the tree's own
	 */
	public Tree(String id, ITreeProvider<T> provider, IModel<? extends Set<T>> state)
	{
		super(id, provider, state);
		setOutputMarkupId(true);
	}

	@Override
	public AbstractTree<T> asTree()
	{
		return this;
	}

	@Override
	public Component newNodeComponent(String id, IModel<T> model)
	{
		return new TreeNode<>(id, this, model);
	}

	@Override
	protected Component newContentComponent(String id, IModel<T> model)
	{
		return new TreeNodeContent<>(id, this, model);
	}

	/**
	 * @param node
	 *            a node
	 * @param leaf
	 *            whether it is a leaf
	 * @param expanded
	 *            whether it is an expanded folder
	 * @return the icon of the node: by default a leaf, or a folder, open or closed
	 */
	@Override
	public IIcon getIcon(T node, boolean leaf, boolean expanded)
	{
		return support.getIcon(leaf, expanded);
	}

	@Override
	public IIcon getIcon(TreeIcon slot)
	{
		return support.getIcon(slot);
	}

	/**
	 * Gives the tree an icon of the application's own.
	 *
	 * @param slot
	 *            the icon replaced
	 * @param icon
	 *            the icon, {@code null} for the tree's own again
	 * @return {@code this}
	 */
	public Tree<T> setIcon(TreeIcon slot, IIcon icon)
	{
		support.setIcon(slot, icon);
		return this;
	}

	/**
	 * @param node
	 *            the model of a node
	 * @return the model of its text, by default the node itself, written as a string
	 */
	@Override
	public IModel<?> newLabelModel(IModel<T> node)
	{
		return node;
	}

	@Override
	public boolean isSelected(T node)
	{
		return support.isSelected(this, node);
	}

	/**
	 * @return the node selected, {@code null} if none is
	 */
	public T getSelected()
	{
		return support.getSelected();
	}

	/**
	 * Selects a node without telling {@link #onSelect(AjaxRequestTarget, Object)}, for example
	 * before the tree is rendered.
	 *
	 * @param node
	 *            the leaf, or a folder when they are selected too, {@code null} for none
	 * @return {@code this}
	 */
	public Tree<T> setSelected(T node)
	{
		support.select(getProvider(), node);
		return this;
	}

	/**
	 * Selects a node, as a click on it does: repaints it and the node selected before, and calls
	 * {@link #onSelect(AjaxRequestTarget, Object)}.
	 *
	 * @param node
	 *            the leaf, or a folder when they are selected too
	 * @param target
	 *            the Ajax request target
	 */
	@Override
	public void select(T node, AjaxRequestTarget target)
	{
		T previous = support.select(getProvider(), node);
		if (previous != null)
		{
			updateNode(previous, target);
		}
		updateNode(node, target);
		onSelect(target, node);
	}

	/**
	 * Called when the user selects a leaf, or a folder when they are selected too. Does nothing by
	 * default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param node
	 *            the node
	 */
	protected void onSelect(AjaxRequestTarget target, T node)
	{
	}

	@Override
	public boolean isSelectable(T node)
	{
		return isLeaf(node) || support.isSelectBranches();
	}

	/**
	 * @return whether folders are selected too, not only leaves
	 */
	public boolean isSelectBranches()
	{
		return support.isSelectBranches();
	}

	/**
	 * Has folders selected too, not only leaves: a click on a folder selects it, and expands it
	 * if it is collapsed; a click on its arrow still expands or collapses it.
	 *
	 * @param selectBranches
	 *            whether folders are selected too
	 * @return {@code this}
	 */
	public Tree<T> setSelectBranches(boolean selectBranches)
	{
		support.setSelectBranches(selectBranches);
		return this;
	}

	/**
	 * @return the height of the tree in {@link #getHeightUnits()}, or a negative number,
	 *         {@code -1} by default, when the tree is as high as its nodes
	 */
	public int getHeight()
	{
		return support.getHeight();
	}

	/**
	 * @return the unit of {@link #getHeight()}, {@link BodyHeightUnits#PIXELS} by default
	 */
	public BodyHeightUnits getHeightUnits()
	{
		return support.getHeightUnits();
	}

	/**
	 * Gives the tree a fixed height, within which it scrolls, with the slim scroll bars of a
	 * {@link ScrollbarBehavior} in the colors of the theme. Without a height, the tree is as high
	 * as its nodes.
	 *
	 * @param height
	 *            the height of the tree in the given units; any negative number, such as
	 *            {@code -1}, to let the tree take the height of its nodes, as by default
	 * @param units
	 *            the units of the height
	 * @return {@code this}
	 */
	public Tree<T> setHeight(int height, BodyHeightUnits units)
	{
		support.setHeight(this, height, units, null);
		return this;
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.TREE);
		support.onComponentTag(tag);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
		support.renderHead(this, response);
	}

	@Override
	protected void onDetach()
	{
		support.detach();
		super.onDetach();
	}
}
