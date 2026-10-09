/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

/**
 * Shows how many rows are selected, such as "2 selected", in the navigation of the table, among
 * its {@link IDynamicToolbarAction actions}. Nothing is shown while no row is selected or the
 * table selects none. The text is the {@code DynamicDataTable.selectedCount} resource, with
 * {@code ${count}} standing for the number; it is re-rendered whenever the user changes the
 * selection.
 * <p>
 * The count can be turned off and on again without changing the actions of the table, with
 * {@link #setShown(boolean)}; the table has to be rendered again after that.
 *
 * <pre>
 * SelectionCountToolbarAction&lt;User, Long&gt; count = new SelectionCountToolbarAction&lt;&gt;();
 * table.addToolbarAction(count);
 * </pre>
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
public class SelectionCountToolbarAction<T, K extends Serializable>
	implements
		IDynamicToolbarAction<T, K>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the count. */
	public static final String CSS_CLASS = "ant-data-table-selection-count";

	private boolean shown = true;

	/**
	 * @return whether the count is shown
	 */
	public boolean isShown()
	{
		return shown;
	}

	/**
	 * @param shown
	 *            whether the count is shown
	 * @return this
	 */
	public SelectionCountToolbarAction<T, K> setShown(boolean shown)
	{
		this.shown = shown;
		return this;
	}

	@Override
	public Component newComponent(String id, BaseDynamicDataTable<T, K, ?> table)
	{
		return new Count(id, table, this);
	}

	/**
	 * The count, a component showing the selection of the table, which re-renders it when the
	 * selection changes.
	 */
	private static class Count extends Label implements ISelectionView
	{
		private static final long serialVersionUID = 1L;

		private final BaseDynamicDataTable<?, ?, ?> table;

		private final SelectionCountToolbarAction<?, ?> action;

		Count(String id, BaseDynamicDataTable<?, ?, ?> table, SelectionCountToolbarAction<?, ?> action)
		{
			super(id);
			this.table = table;
			this.action = action;
			setDefaultModel(() -> getString("DynamicDataTable.selectedCount",
				Model.ofMap(Map.of("count", table.getSelection().size())), "${count} selected"));
			setOutputMarkupPlaceholderTag(true);
		}

		@Override
		protected void onConfigure()
		{
			super.onConfigure();
			setVisible(action.isShown() && table.getSelectionMode() != SelectionMode.NONE &&
				table.getSelection().isEmpty() == false);
		}

		@Override
		protected void onComponentTag(ComponentTag tag)
		{
			super.onComponentTag(tag);
			tag.append("class", CSS_CLASS, " ");
			tag.put("role", "status");
		}
	}
}
