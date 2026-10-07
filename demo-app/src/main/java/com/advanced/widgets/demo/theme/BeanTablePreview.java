/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.theme;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.demo.datatable.User;
import com.advanced.widgets.demo.datatable.UsersProvider;
import com.advanced.widgets.dynamicdatatable.BeanBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.BeanSelectionColumn;
import com.advanced.widgets.table.ResizableColumnsBehavior;

/**
 * A plain table of users rendered on the server by Wicket components, for the theme editor's
 * preview: no progress and no updates pushed over websockets. The columns can be resized.
 */
class BeanTablePreview extends Panel
{
	private static final long serialVersionUID = 1L;

	BeanTablePreview(String id)
	{
		super(id);
		List<IColumn<User, String>> columns = List.of(new BeanSelectionColumn<>(),
			new LambdaColumn<>(new ResourceModel("firstName"), "firstName", User::getFirstName),
			new LambdaColumn<>(new ResourceModel("lastName"), "lastName", User::getLastName),
			new LambdaColumn<>(new ResourceModel("city"), User::getCity),
			new LambdaColumn<>(new ResourceModel("country"), User::getCountryName));
		add(new BeanBasedDynamicDataTable<>("table", columns, new UsersProvider(), 10)
			.setAdjustToParentWidth(true)
			.add(new ResizableColumnsBehavior(ResizableColumnsBehavior.Mode.STRETCH)));
	}
}
