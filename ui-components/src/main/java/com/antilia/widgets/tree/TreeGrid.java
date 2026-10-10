/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.tree;

import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
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
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;

import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.scrollbar.ScrollbarBehavior;
import com.antilia.widgets.table.IResizableColumnsTable;
import com.antilia.widgets.table.ResizableColumnsBehavior;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;

/**
 * A tree grid of Wicket components: a {@link TableTree} of wicket-extensions whose tree column,
 * a {@link TreeColumn} among the columns, renders the nodes of a {@link Tree}, with the icons of
 * {@link TreeIcon}. The table has a row of headers, from the headers of the columns, and says so
 * when it has no rows.
 * <p>
 * A click on a folder, or on its arrow, expands or collapses it, repainting the table through
 * Ajax; a click on a leaf selects it, repaints its row and the row of the leaf selected before,
 * and calls {@link #onSelect(AjaxRequestTarget, Object)}. Only leaves are selected, unless
 * {@link #setSelectBranches(boolean)} has folders selected too: then a click on a folder selects
 * it, and expands it if it is collapsed, its arrow still expanding and collapsing it. The row of
 * the node selected carries the CSS class {@code antilia-tree-node--selected}.
 * <p>
 * Given a {@link #setHeight(int, BodyHeightUnits) height}, the rows scroll below the row of
 * headers, which stays in place, with slim scroll bars in the colors of the theme. The columns
 * can be made {@link #setResizableColumns(Mode) resizable}.
 * <p>
 * The table carries the CSS class {@value #CSS_CLASS} and takes its colors from the
 * {@link com.antilia.widgets.theme.Theme theme} it is rendered in, if any.
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
	public static final String CSS_CLASS = "antilia-tree-grid";

	private final TreeSupport<T> support = new TreeSupport<>();

	private ResizableColumnsBehavior resizableColumns;

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

	/**
	 * @return a table keeping the widths the user resized its columns to, whose rows render
	 *         their markup ids, as {@link #updateNode(Object, IPartialPageRequestHandler)} needs
	 */
	@Override
	protected DataTable<T, S> newDataTable(String id, List<? extends IColumn<T, S>> columns,
		IDataProvider<T> dataProvider, long rowsPerPage)
	{
		return new Grid(id, columns, dataProvider, rowsPerPage);
	}

	/**
	 * Repaints the table, the rows of a folder expanded or collapsed following its row, but not
	 * the element around it, which keeps where it is scrolled to.
	 */
	@Override
	public void updateBranch(T node, IPartialPageRequestHandler target)
	{
		target.add(getTable());
	}

	@Override
	public AbstractTree<T> asTree()
	{
		return this;
	}

	/**
	 * @return the height of the table in {@link #getHeightUnits()}, or a negative number,
	 *         {@code -1} by default, when the table is as high as its rows
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
	 * Gives the tree grid a fixed height, within which its rows scroll below the row of headers,
	 * which stays in place, with the slim scroll bars of a {@link ScrollbarBehavior} in the colors
	 * of the theme. Without a height, the tree grid is as high as its rows.
	 *
	 * @param height
	 *            the height in the given units; any negative number, such as {@code -1}, to let
	 *            the tree grid take the height of its rows, as by default
	 * @param units
	 *            the units of the height
	 * @return {@code this}
	 */
	public TreeGrid<T, S> setHeight(int height, BodyHeightUnits units)
	{
		support.setHeight(this, height, units, ":scope > table > thead");
		return this;
	}

	/**
	 * @return how the user resizes the columns, {@code null}, as by default, when they cannot be
	 *         resized
	 */
	public Mode getResizableColumns()
	{
		return resizableColumns != null ? resizableColumns.getMode() : null;
	}

	/**
	 * Lets the user resize the columns by dragging the edges of their headers, or with the arrow
	 * keys on them, with a {@link ResizableColumnsBehavior} on the table, which keeps the widths
	 * when it is rendered again: those the user gave the columns, and those they took by their
	 * content, so expanding a folder does not change them. A change of the mode forgets the
	 * widths; the table is to be rendered again.
	 *
	 * @param mode
	 *            how the table's width follows its columns, {@code null} for columns that cannot
	 *            be resized
	 * @return {@code this}
	 */
	public TreeGrid<T, S> setResizableColumns(Mode mode)
	{
		if (resizableColumns != null)
		{
			getTable().remove(resizableColumns);
			resizableColumns = null;
		}
		if (getTable() instanceof TreeGrid<?, ?>.Grid grid)
		{
			grid.widths = null;
		}
		if (mode != null)
		{
			getTable().add(resizableColumns = new ResizableColumnsBehavior(mode).setKeepWidths(true));
		}
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
				if (isSelectable(node) && isSelected(node))
				{
					tag.append("class", "antilia-tree-node--selected", " ");
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
	 * @return the node selected, {@code null} if none is
	 */
	public T getSelected()
	{
		return support.getSelected();
	}

	/**
	 * Selects a node without telling {@link #onSelect(AjaxRequestTarget, Object)}, for example
	 * before the table is rendered.
	 *
	 * @param node
	 *            the leaf, or a folder when they are selected too, {@code null} for none
	 * @return {@code this}
	 */
	public TreeGrid<T, S> setSelected(T node)
	{
		support.select(getProvider(), node);
		return this;
	}

	/**
	 * Selects a node, as a click on it does: repaints its row and the row of the node selected
	 * before, and calls {@link #onSelect(AjaxRequestTarget, Object)}.
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
	public TreeGrid<T, S> setSelectBranches(boolean selectBranches)
	{
		support.setSelectBranches(selectBranches);
		return this;
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
		support.onComponentTag(tag);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(Tree.CSS));
		support.renderHead(this, response);
	}

	@Override
	protected void onDetach()
	{
		support.detach();
		super.onDetach();
	}

	/**
	 * The table of the tree grid: its rows render their markup ids, and it keeps the widths the
	 * user resized its columns to.
	 */
	private class Grid extends DataTable<T, S> implements IResizableColumnsTable
	{
		private static final long serialVersionUID = 1L;

		private double[] widths;

		Grid(String id, List<? extends IColumn<T, S>> columns, IDataProvider<T> dataProvider,
			long rowsPerPage)
		{
			super(id, columns, dataProvider, rowsPerPage);
			setOutputMarkupId(true);
		}

		@Override
		protected Item<T> newRowItem(String id, int index, IModel<T> model)
		{
			Item<T> item = TreeGrid.this.newRowItem(id, index, model);
			item.setOutputMarkupId(true);
			return item;
		}

		@Override
		public double[] getShownColumnWidths()
		{
			return widths;
		}

		@Override
		public void columnsResized(AjaxRequestTarget target, double[] widths)
		{
			this.widths = widths;
		}
	}
}
