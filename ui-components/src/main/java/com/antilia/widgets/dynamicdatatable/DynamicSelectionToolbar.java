/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * The bar offering to select every row of a {@link BaseDynamicDataTable} once every row of its page
 * is selected, and to clear that selection again. Shown only then; see
 * {@link BaseDynamicDataTable#getSelection()}.
 * <p>
 * The texts are the resources {@code DynamicDataTable.pageSelected},
 * {@code DynamicDataTable.selectAll}, {@code DynamicDataTable.allSelected} and
 * {@code DynamicDataTable.clearSelection}, with the variables {@code ${count}}, the number of rows
 * of the page, and {@code ${total}}, the number of all rows.
 *
 * @since 11.0.0
 */
public class DynamicSelectionToolbar extends AbstractDynamicToolbar
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param table
	 *            the table
	 */
	public DynamicSelectionToolbar(String id, BaseDynamicDataTable<?, ?, ?> table)
	{
		super(id, table);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();

		BaseDynamicDataTable<?, ?, ?> table = getTable();

		WebMarkupContainer span = new WebMarkupContainer("span");
		span.add(newColspan());
		add(span);

		IModel<Map<String, Long>> counts = () -> Map.of("count",
			(long)table.getCurrentPageKeys().size(), "total", table.getItemCount());

		WebMarkupContainer page = new WebMarkupContainer("page")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(table.isAllSelected() == false);
			}
		};
		span.add(page);
		page.add(new Label("message", new StringResourceModel("DynamicDataTable.pageSelected",
			this, counts).setDefaultValue("All ${count} rows on this page are selected.")));
		page.add(new AjaxLink<Void>("selectAll")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				table.selectAll(target, true);
			}
		}.setBody(new StringResourceModel("DynamicDataTable.selectAll", this, counts)
			.setDefaultValue("Select all ${total} rows")));

		WebMarkupContainer all = new WebMarkupContainer("all")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(table.isAllSelected());
			}
		};
		span.add(all);
		all.add(new Label("message", new StringResourceModel("DynamicDataTable.allSelected", this,
			counts).setDefaultValue("All ${total} rows are selected.")));
		all.add(new AjaxLink<Void>("clear")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				table.selectAll(target, false);
			}
		}.setBody(new ResourceModel("DynamicDataTable.clearSelection", "Clear selection")));
	}

	@Override
	protected void onConfigure()
	{
		super.onConfigure();
		BaseDynamicDataTable<?, ?, ?> table = getTable();
		setVisible(table.isAllSelected() || table.isPageSelected());
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.TABLE_SELECTION_TOOLBAR);
	}
}
