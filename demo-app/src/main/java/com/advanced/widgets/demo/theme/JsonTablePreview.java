/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.demo.datatable.User;
import com.advanced.widgets.demo.datatable.UsersProvider;
import com.advanced.widgets.dynamicdatatable.DynamicColumn;
import com.advanced.widgets.dynamicdatatable.IDynamicColumn;
import com.advanced.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.SelectionColumn;
import com.advanced.widgets.dynamicdatatable.SortableDynamicColumn;

/**
 * A plain table of users rendered in the browser from JSON, for the theme editor's preview: no
 * progress and no updates pushed over websockets.
 */
class JsonTablePreview extends Panel
{
	private static final long serialVersionUID = 1L;

	JsonTablePreview(String id)
	{
		super(id);
		List<IDynamicColumn<User>> columns = List.of(new SelectionColumn<User>().setId("selection"),
			new SortableDynamicColumn<>(column("firstName", "{{firstName}}"), "firstName"),
			new SortableDynamicColumn<>(column("lastName", "{{lastName}}"), "lastName"),
			column("city", "{{city}}"), column("country", "{{countryName}}"));
		add(new JsonBasedDynamicDataTable<>("table", columns, new UsersProvider(), 10));
	}

	private static DynamicColumn<User> column(String headerKey, String template)
	{
		DynamicColumn<User> column = new DynamicColumn<>(new ResourceModel(headerKey), template);
		column.setId(headerKey);
		return column;
	}
}
