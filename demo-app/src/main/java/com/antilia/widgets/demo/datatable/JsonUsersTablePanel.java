/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antilia.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.antilia.widgets.dynamicdatatable.DynamicColumn;
import com.antilia.widgets.dynamicdatatable.IDynamicColumn;
import com.antilia.widgets.dynamicdatatable.IHideableColumn;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.antilia.widgets.dynamicdatatable.ProgressBarColumn;
import com.antilia.widgets.dynamicdatatable.ResourceDynamicColumn;
import com.antilia.widgets.dynamicdatatable.RowNumberColumn;
import com.antilia.widgets.dynamicdatatable.SelectionColumn;
import com.antilia.widgets.dynamicdatatable.SortableDynamicColumn;
import com.antilia.widgets.table.IMovableColumn;

/**
 * The table of the users rendered in the browser from JSON, a {@link JsonBasedDynamicDataTable}:
 * most columns read their template from an .html file next to this class, the rows can come from
 * the mounted {@link UsersRowsResource} too.
 */
public class JsonUsersTablePanel extends UsersTablePanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param id
	 *            component id
	 * @param host
	 *            the page the table is on
	 */
	public JsonUsersTablePanel(String id, IUsersTableHost host)
	{
		super(id, host);
	}

	/**
	 * The proportions of the columns: selection, number, actions, id, first and last name, born,
	 * address, phones, progress and the hidden phones, city and country.
	 */
	@Override
	public double[] getColumnProportions()
	{
		return new double[] { 0.4, 0.4, 1.1, 0.5, 1.1, 1.1, 0.9, 2.0, 2.4, 1.3,
			1.1, 1.1, 1.0, 1.0 };
	}

	@Override
	@SuppressWarnings("unchecked")
	public JsonBasedDynamicDataTable<User, Long> getTable()
	{
		return (JsonBasedDynamicDataTable<User, Long>)super.getTable();
	}

	@Override
	protected BaseDynamicDataTable<User, Long, ?> newTable(String id, UsersProvider provider)
	{
		List<IDynamicColumn<User>> columns = new ArrayList<>();
		columns.add(new SelectionColumn<User>().setId("selection"));
		columns.add(new RowNumberColumn<User>(Model.of("#")).setId("number"));
		columns.add(new CompoundDynamicColumn<>(new ResourceModel("actions"), newRowActions())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isResizable()
			{
				return false;
			}
		}.setId("actions"));
		columns.add(new ResourceDynamicColumn<User>(new ResourceModel("id"),
			JsonUsersTablePanel.class, "IdColumn")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isResizable()
			{
				return false;
			}
		}.setExportValue(User::getId).setId("id"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("firstName", "FirstNameColumn", User::getFirstName), "firstName"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("lastName", "LastNameColumn", User::getLastName), "lastName"));
		columns.add(new SortableDynamicColumn<>(
			new TemplateColumn("bornDate", "BornDateColumn", UsersTablePanel::bornOn),
			"bornDate"));
		columns.add(new TemplateColumn("address", "AddressColumn",
			user -> user.getAddress() + ", " + user.getCity() + ", " +
				user.getCountryName()));
		columns.add(new PhonesColumn(new ResourceModel("phones"))
			.setExportValue(user -> user.getHomePhone() + " / " + user.getCellPhone())
			.setId("phones"));
		columns.add(new ProgressColumn());
		List<IDynamicColumn<User>> hidden = List.of(
			new ValueColumn("homePhone", "{{homePhone}}", User::getHomePhone),
			new ValueColumn("cellPhone", "{{cellPhone}}", User::getCellPhone),
			new ValueColumn("city", "{{city}}", User::getCity),
			new ValueColumn("country", "{{countryName}}", User::getCountryName));
		columns.addAll(hidden);

		JsonBasedDynamicDataTable<User, Long> table = new JsonBasedDynamicDataTable<>(id,
			columns, provider, ROWS_PER_PAGE)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected AbstractDynamicToolbar newNavigationToolbar(String id,
				ToolbarPosition position)
			{
				return JsonUsersTablePanel.this.newNavigationToolbar(this, id, position);
			}

			@Override
			protected void onRowsSent(List<Long> keys)
			{
				JsonUsersTablePanel.this.onRowsSent(keys);
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				User user)
			{
				onUserNotFound(target);
			}

			@Override
			protected void addRowsParameters(PageParameters parameters)
			{
				String filter = provider.getFilter();
				if (filter != null && !filter.isBlank())
				{
					parameters.set(UsersRowsResource.FILTER, filter);
				}
			}
		};
		// the rows can come from the mounted resource too, see the setting "Rows from"
		table.setRowsResource(UsersRowsResource.REFERENCE);
		hidden.forEach(column -> table.setColumnVisible(column, false));
		return table;
	}

	/**
	 * Called with the keys of the rows the table sent the browser. Does nothing by default.
	 *
	 * @param keys
	 *            the keys of the rows
	 */
	protected void onRowsSent(List<Long> keys)
	{
	}

	/**
	 * A column the user can hide and move, reading its template from {@code templateName.html}
	 * next to this class.
	 */
	private static class TemplateColumn extends ResourceDynamicColumn<User>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		TemplateColumn(String headerKey, String templateName,
			SerializableFunction<User, ?> exportValue)
		{
			super(new ResourceModel(headerKey), JsonUsersTablePanel.class, templateName);
			setId(headerKey);
			setExportValue(exportValue);
		}
	}

	/**
	 * A column the user can hide and move, showing one value of the user.
	 */
	private static class ValueColumn extends DynamicColumn<User>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ValueColumn(String headerKey, String template, SerializableFunction<User, ?> exportValue)
		{
			super(new ResourceModel(headerKey), template);
			setId(headerKey);
			setExportValue(exportValue);
		}
	}

	/**
	 * The progress of the user, a column the user can hide and move.
	 */
	private static class ProgressColumn extends ProgressBarColumn<User>
		implements
			IHideableColumn,
			IMovableColumn
	{
		private static final long serialVersionUID = 1L;

		ProgressColumn()
		{
			super(new ResourceModel("progress"), "progress");
			setId("progress");
		}
	}
}
