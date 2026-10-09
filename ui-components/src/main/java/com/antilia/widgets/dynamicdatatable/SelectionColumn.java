/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;

/**
 * A column with a checkbox per row, selecting the row, and a checkbox in its header, selecting
 * every row of the page. The selection is kept by the table; see
 * {@link JsonBasedDynamicDataTable#getSelection()}.
 * <p>
 * The header checkbox shows whether all, some or none of the rows of the page are selected. Once
 * all of them are, the table offers to select every row of the provider.
 * <p>
 * The column is neither sortable, resizable nor hideable.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public class SelectionColumn<T> extends AbstractDynamicColumn<T>
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the checkbox of a row. */
	public static final String ROW_CHECKBOX_CLASS = "ant-data-table-select";

	/** The CSS class of the checkbox in the header. */
	public static final String PAGE_CHECKBOX_CLASS = "ant-data-table-select-page";

	/**
	 * Constructor, with the label of the header checkbox read from the
	 * {@code DynamicDataTable.selectPage} resource.
	 */
	public SelectionColumn()
	{
		super(new ResourceModel("DynamicDataTable.selectPage", "Select all rows on this page"));
		setCssClass("ant-data-table-selection");
	}

	/**
	 * Returns the checkbox without the label read for the table, which
	 * {@link #getTemplate(JsonBasedDynamicDataTable)} adds.
	 */
	@Override
	public String getTemplate()
	{
		return checkbox(null);
	}

	/**
	 * Returns the checkbox labeled with the {@code DynamicDataTable.selectRow} resource of the
	 * table, escaped.
	 */
	@Override
	public String getTemplate(JsonBasedDynamicDataTable<T, ?> table)
	{
		return checkbox(table.getString("DynamicDataTable.selectRow", null, "Select row"));
	}

	private static String checkbox(String label)
	{
		return "<input type=\"checkbox\" class=\"" + ROW_CHECKBOX_CLASS + "\" data-dt-action=\"" +
			BaseDynamicDataTable.SELECT_ACTION + "\" data-dt-action-unchecked=\"" +
			BaseDynamicDataTable.DESELECT_ACTION + "\"" +
			(label != null ? " aria-label=\"" + Strings.escapeMarkup(label) + "\"" : "") + ">";
	}

	/**
	 * Creates the checkbox selecting the page, labeled with the header text, escaped; nothing when
	 * the table's {@link SelectionMode} is not {@link SelectionMode#MANY}.
	 */
	@Override
	public Component newHeader(String id)
	{
		return new Label(id, getHeader())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
			{
				BaseDynamicDataTable<?, ?, ?> table = findParent(BaseDynamicDataTable.class);
				if (table != null && table.getSelectionMode() != SelectionMode.MANY)
				{
					// only one row can be selected: there is no page to select
					replaceComponentTagBody(markupStream, openTag, "");
					return;
				}
				replaceComponentTagBody(markupStream, openTag, "<input type=\"checkbox\" class=\"" +
					PAGE_CHECKBOX_CLASS + "\" data-dt-action=\"" +
					BaseDynamicDataTable.SELECT_PAGE_ACTION + "\" data-dt-action-unchecked=\"" +
					BaseDynamicDataTable.DESELECT_PAGE_ACTION + "\" aria-label=\"" +
					getDefaultModelObjectAsString() + "\">");
			}
		};
	}

	/**
	 * @return {@code false}
	 */
	@Override
	public boolean isResizable()
	{
		return false;
	}
}
