/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

/**
 * A row above or below the rows of a {@link BaseDynamicDataTable}, such as its navigation, rendered
 * by Wicket. A toolbar is a panel attached to a {@code <tr>}; its markup holds the cells, usually
 * one spanning all columns, see {@link #newColspan()}.
 * <p>
 * Add a toolbar of your own with {@link BaseDynamicDataTable#addTopToolbar(AbstractDynamicToolbar)}
 * or {@link BaseDynamicDataTable#addBottomToolbar(AbstractDynamicToolbar)}. The table re-renders
 * its toolbars via Ajax whenever its rows change, see {@link #refresh(IPartialPageRequestHandler)}.
 *
 * @since 11.0.0
 */
public abstract class AbstractDynamicToolbar extends Panel
{
	private static final long serialVersionUID = 1L;

	private final BaseDynamicDataTable<?, ?, ?> table;

	/**
	 * Constructor for a toolbar added to a table, with an id the table generates.
	 *
	 * @param table
	 *            the table the toolbar belongs to
	 */
	protected AbstractDynamicToolbar(BaseDynamicDataTable<?, ?, ?> table)
	{
		this(Args.notNull(table, "table").newToolbarId(), table);
	}

	/**
	 * @param id
	 *            component id
	 * @param table
	 *            the table the toolbar belongs to
	 */
	protected AbstractDynamicToolbar(String id, BaseDynamicDataTable<?, ?, ?> table)
	{
		super(id);
		this.table = Args.notNull(table, "table");
		setOutputMarkupPlaceholderTag(true);
	}

	/**
	 * @return the table the toolbar belongs to
	 */
	public BaseDynamicDataTable<?, ?, ?> getTable()
	{
		return table;
	}

	/**
	 * @return a behavior setting the {@code colspan} of a cell to the number of cells of a row:
	 *         one per shown column, and the empty one at the end while the columns can be resized
	 */
	protected Behavior newColspan()
	{
		return AttributeModifier.replace("colspan",
			(IModel<String>)() -> String.valueOf(table.getCellCount()));
	}

	/**
	 * Re-renders the toolbar after the rows of the table changed, for example after paging. Adds
	 * the toolbar to the target by default.
	 *
	 * @param target
	 *            the handler of the current Ajax or web socket request
	 */
	public void refresh(IPartialPageRequestHandler target)
	{
		target.add(this);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "tr");
		super.onComponentTag(tag);
	}
}
