/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.tree;

import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;

import com.advanced.widgets.icon.IIcon;
import com.advanced.widgets.license.Licensing;

/**
 * A tree grid of Wicket components: a {@link TableTree} of wicket-extensions whose tree column,
 * a {@link TreeColumn} among the columns, renders the nodes of a {@link Tree}, with the icons of
 * {@link TreeIcon}. The table has a row of headers, from the headers of the columns, and says so
 * when it has no rows.
 * <p>
 * A click on a folder, or on its arrow, expands or collapses it, repainting the table through
 * Ajax; a click on a leaf selects it, repaints its row and the row of the leaf selected before,
 * and calls {@link #onSelect(AjaxRequestTarget, Object)}. Only leaves are selected; the row of the
 * leaf selected carries the CSS class {@code wicket-tree-node--selected}.
 * <p>
 * The table carries the CSS class {@value #CSS_CLASS} and takes its colors from the
 * {@link com.advanced.widgets.theme.Theme theme} it is rendered in, if any.
 * <p>
 * A private component: it renders only when the {@link Licensing license key} of the application
 * activates it, else a placeholder telling so.
 *
 * @param <T>
 *            the type of the nodes
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class TreeGrid<T, S> extends TableTree<T, S> implements INodeRenderer<T>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the tree grid. */
	public static final String CSS_CLASS = "wicket-tree-grid";

	private final TreeSupport<T> support = new TreeSupport<>();

	/**
	 * A tree grid showing all its rows.
	 *
	 * @param id
	 *            component id
	 * @param columns
	 *            the columns, one of them a {@link TreeColumn}
	 * @param provider
	 *            the provider of the nodes; one that is an {@link ISortStateLocator} makes the
	 *            sortable columns sortable
	 */
	public TreeGrid(String id, List<? extends IColumn<T, S>> columns, ITreeProvider<T> provider)
	{
		this(id, columns, provider, Long.MAX_VALUE, null);
	}

	/**
	 * @param id
	 *            component id
	 * @param columns
	 *            the columns, one of them a {@link TreeColumn}
	 * @param provider
	 *            the provider of the nodes; one that is an {@link ISortStateLocator} makes the
	 *            sortable columns sortable
	 * @param rowsPerPage
	 *            the number of rows of a page
	 * @param state
	 *            the folders expanded, {@code null} for a set of the table's own
	 */
	@SuppressWarnings("unchecked")
	public TreeGrid(String id, List<? extends IColumn<T, S>> columns, ITreeProvider<T> provider,
		long rowsPerPage, IModel<? extends Set<T>> state)
	{
		super(id, columns, provider, rowsPerPage, state);
		getTable().addTopToolbar(new HeadersToolbar<>(getTable(),
			provider instanceof ISortStateLocator<?> locator ? (ISortStateLocator<S>)locator
				: null));
		getTable().addBottomToolbar(new NoRecordsToolbar(getTable()));
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

	@Override
	protected Item<T> newRowItem(String id, int index, IModel<T> model)
	{
		return new Item<>(id, index, model)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				T node = getModelObject();
				if (isLeaf(node) && isSelected(node))
				{
					tag.append("class", "wicket-tree-node--selected", " ");
				}
			}
		};
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
	 * Gives the table an icon of the application's own.
	 *
	 * @param slot
	 *            the icon replaced
	 * @param icon
	 *            the icon, {@code null} for the table's own again
	 * @return {@code this}
	 */
	public TreeGrid<T, S> setIcon(TreeIcon slot, IIcon icon)
	{
		support.setIcon(slot, icon);
		return this;
	}

	/**
	 * @param node
	 *            the model of a node
	 * @return the model of its text in the tree column, by default the node itself, written as a
	 *         string
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
	 * @return the leaf selected, {@code null} if none is
	 */
	public T getSelected()
	{
		return support.getSelected();
	}

	/**
	 * Selects a leaf without telling {@link #onSelect(AjaxRequestTarget, Object)}, for example
	 * before the table is rendered.
	 *
	 * @param node
	 *            the leaf, {@code null} for none
	 * @return {@code this}
	 */
	public TreeGrid<T, S> setSelected(T node)
	{
		support.select(getProvider(), node);
		return this;
	}

	/**
	 * Selects a leaf, as a click on it does: repaints its row and the row of the leaf selected
	 * before, and calls {@link #onSelect(AjaxRequestTarget, Object)}.
	 *
	 * @param node
	 *            the leaf
	 * @param target
	 *            the Ajax request target
	 */
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
	 * Called when the user selects a leaf. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 * @param node
	 *            the leaf
	 */
	protected void onSelect(AjaxRequestTarget target, T node)
	{
	}

	@Override
	public void onNodeClick(AjaxRequestTarget target, T node)
	{
		if (isLeaf(node))
		{
			select(node, target);
		}
		else if (getState(node) == State.EXPANDED)
		{
			collapse(node);
		}
		else
		{
			expand(node);
		}
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		Licensing.configurePrivate(this);
	}

	@Override
	protected void renderPlaceholderTag(ComponentTag tag, Response response)
	{
		if (!Licensing.renderPlaceholder(this, tag, response))
		{
			super.renderPlaceholderTag(tag, response);
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		tag.append("class", CSS_CLASS, " ");
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(Tree.CSS));
	}

	@Override
	protected void onDetach()
	{
		support.detach();
		super.onDetach();
	}
}
