/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Asks to confirm an {@link IconBasedRowAction} in the overlay of its table, and carries out the
 * action on the row, looked up again by its key, once the user confirms.
 *
 * @param <T>
 *            the type of the rows
 */
class RowActionConfirmationPanel<T> extends Panel
{
	private static final long serialVersionUID = 1L;

	RowActionConfirmationPanel(String id, IModel<String> question, String rowKey,
		IconBasedRowAction<T> action)
	{
		super(id);

		add(new Label("question", question));
		add(new AjaxLink<Void>("cancel")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				action.getTable().closeOverlay(target);
			}
		}.setBody(new ResourceModel("DynamicDataTable.cancel", "Cancel")));
		add(new AjaxLink<Void>("confirm")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				BaseDynamicDataTable<T, ?, ?> table = action.getTable();
				table.closeOverlay(target);
				T row = table.findRow(rowKey);
				if (row != null)
				{
					action.onRowAction(row, target);
				}
			}
		}.setBody(new ResourceModel("DynamicDataTable.confirm", "Confirm")));
	}
}
