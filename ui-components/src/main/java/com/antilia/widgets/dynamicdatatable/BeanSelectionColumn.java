/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * The column of a {@link BeanBasedDynamicDataTable} selecting rows, the counterpart of
 * {@link SelectionColumn}: a checkbox in every row, checked if the row is selected, and one in the
 * header selecting the rows of the page. The labels are the resources
 * {@code DynamicDataTable.selectRow} and {@code DynamicDataTable.selectPage}, written escaped.
 *
 * @param <T>
 *            the type of the rows
 * @param <S>
 *            the type of the sort properties
 * @since 11.0.0
 */
public class BeanSelectionColumn<T, S> extends AbstractColumn<T, S> implements IFixedWidthColumn
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public BeanSelectionColumn()
	{
		super(new ResourceModel("DynamicDataTable.selectPage", "Select all rows on this page"));
	}

	@Override
	public Component getHeader(String componentId)
	{
		return new SelectionColumn<T>().newHeader(componentId);
	}

	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
		IModel<T> rowModel)
	{
		cellItem.add(new Label(componentId, () -> checkbox(cellItem, rowModel.getObject()))
			.setEscapeModelStrings(false));
	}

	@SuppressWarnings("unchecked")
	private String checkbox(Component cell, T row)
	{
		BaseDynamicDataTable<T, ?, ?> table = cell.findParent(BaseDynamicDataTable.class);
		String label = table.getString("DynamicDataTable.selectRow", null, "Select row");
		boolean selected = row != null && isSelected(table, row);
		return "<input type=\"checkbox\" class=\"" +
			Looks.of(table).getPartClasses(LookPart.TABLE_SELECT) +
			"\" data-dt-action=\"" + BaseDynamicDataTable.SELECT_ACTION +
			"\" data-dt-action-unchecked=\"" + BaseDynamicDataTable.DESELECT_ACTION +
			"\" aria-label=\"" + Strings.escapeMarkup(label) + "\"" + (selected ? " checked" : "") +
			">";
	}

	private static <T, K extends Serializable> boolean isSelected(
		BaseDynamicDataTable<T, K, ?> table, T row)
	{
		return table.getSelection().isSelected(table.getProvider().keyOf(row));
	}

	@Override
	public String getCssClass()
	{
		return "ant-data-table-selection";
	}
}
