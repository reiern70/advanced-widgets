/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;

import com.advanced.widgets.clipboard.ClipboardCopyBehavior;
import com.advanced.widgets.dynamicdatatable.IHideableColumn;
import com.advanced.widgets.dynamicdatatable.ResourceDynamicColumn;
import com.advanced.widgets.table.IMovableColumn;

/**
 * Shows a contact's phone numbers. The template is read from {@code PhonesColumn.html}, or from
 * {@code PhonesColumn_de.html} for German, the way a panel's markup is.
 */
public class PhonesColumn extends ResourceDynamicColumn<Contact>
	implements
		IHeaderContributor,
		IHideableColumn,
		IMovableColumn
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param header
	 *            the header text
	 */
	public PhonesColumn(IModel<String> header)
	{
		super(header);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		ClipboardCopyBehavior.renderHeadItems(response);
	}
}
