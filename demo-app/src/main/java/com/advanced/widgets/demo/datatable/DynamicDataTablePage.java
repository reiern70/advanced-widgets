/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.api.message.AbortedMessage;
import org.apache.wicket.protocol.ws.api.message.ClosedMessage;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.advanced.widgets.demo.DemoWebSocketBehavior;
import com.advanced.widgets.demo.frameworks.ChoicesResource;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;
import com.advanced.widgets.dynamicdatatable.AbstractDynamicToolbar;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.CompoundDynamicColumn;
import com.advanced.widgets.dynamicdatatable.DynamicColumn;
import com.advanced.widgets.dynamicdatatable.IDynamicColumn;
import com.advanced.widgets.dynamicdatatable.IHideableColumn;
import com.advanced.widgets.dynamicdatatable.ISortableDynamicColumn;
import com.advanced.widgets.dynamicdatatable.IWebSocketLightWeightMessage;
import com.advanced.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.advanced.widgets.dynamicdatatable.ProgressBarColumn;
import com.advanced.widgets.dynamicdatatable.ResourceDynamicColumn;
import com.advanced.widgets.dynamicdatatable.RowNumberColumn;
import com.advanced.widgets.dynamicdatatable.SelectionColumn;
import com.advanced.widgets.dynamicdatatable.SortableDynamicColumn;
import com.advanced.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.advanced.widgets.license.Licensing;
import com.advanced.widgets.table.IMovableColumn;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A table rendered in the browser from JSON, whose links are handled by the table itself, and
 * whose rows the server pushes over the page's web socket connection.
 */
public class DynamicDataTablePage extends DynamicTableExamplePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public DynamicDataTablePage()
	{
		add(new DemoWebSocketBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message)
			{
				ProgressSimulator.start(message.getApplication(), message.getSessionId(),
					getPageId(), message.getKey(), getTable().getMarkupId(),
					DatabaseLocator.getLargeDatabase(), getLocale());
			}

			@Override
			protected void onClose(ClosedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}

			@Override
			protected void onAbort(AbortedMessage message)
			{
				ProgressSimulator.stop(message.getSessionId(), getPageId());
			}
		});
	}

	/**
	 * The proportions of the columns: selection, number, actions, id, first and last name, born, address, phones, progress and
	 * the hidden phones, city and country.
	 */
	@Override
	protected double[] getColumnProportions()
	{
		return new double[] { 0.4, 0.4, 1.1, 0.5, 1.1, 1.1, 0.9, 2.0, 2.4, 1.3,
			1.1, 1.1, 1.0, 1.0 };
	}

	@Override
	protected BaseDynamicDataTable<User, Long, ?> newTable(String id,
		UsersProvider provider)
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
			DynamicDataTablePage.class, "IdColumn")
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
			new TemplateColumn("bornDate", "BornDateColumn", DynamicTableExamplePage::bornOn),
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
				return DynamicDataTablePage.this.newNavigationToolbar(this, id, position);
			}

			@Override
			protected void onRowsSent(List<Long> keys)
			{
				ProgressSimulator.show(getSession().getId(), getPage().getPageId(), keys);
			}

			@Override
			protected void onNotFoundAction(AjaxRequestTarget target, String action, Long key,
				User user)
			{
				onUserNotFound(target);
			}

			@Override
			protected void onSelectionChanged(AjaxRequestTarget target)
			{
				DynamicDataTablePage.this.onSelectionChanged(target);
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
		table.setPushEnabled(true);
		return table;
	}

	private JsonBasedDynamicDataTable<User, Long> getJsonTable()
	{
		return (JsonBasedDynamicDataTable<User, Long>)getTable();
	}

	/**
	 * Has the browser fetch the rows again, told so over the web socket connection.
	 */
	@Override
	protected void onUserAdded(User user, AjaxRequestTarget target)
	{
		push(target, getJsonTable().newRefreshMessage(),
			message("added.refreshed", user.getFirstName(), user.getLastName()));
	}

	@Override
	protected Component newPushRowsLink(String id)
	{
		return new AjaxLink<Void>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				List<User> rows = currentPageRows();
				Collections.reverse(rows);

				push(target, getJsonTable().newUpdateRowsMessage(rows),
					getString("pushed"));
			}
		};
	}

	private List<User> currentPageRows()
	{
		BaseDynamicDataTable<User, Long, ?> table = getTable();
		long first = table.getCurrentPage() * table.getItemsPerPage();
		long count = Math.min(table.getItemsPerPage(), table.getItemCount() - first);
		List<User> rows = new ArrayList<>();
		if (count > 0)
		{
			table.getProvider().iterator(first, count).forEachRemaining(rows::add);
		}
		return rows;
	}

	private void push(AjaxRequestTarget target, IWebSocketLightWeightMessage message,
		String success)
	{
		try
		{
			if (message.send(getTable()))
			{
				info(success);
			}
			else
			{
				warn(getString("noWebSocket"));
			}
		}
		catch (IOException e)
		{
			error(message("pushFailed", e.getMessage()));
		}
		target.add(get("feedback"));
	}

	/**
	 * A column the user can hide and move, reading its template from {@code templateName.html}
	 * next to this page.
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
			super(new ResourceModel(headerKey), DynamicDataTablePage.class, templateName);
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

	@Override
	protected String getFrameworkDemo()
	{
		return "jsonTable";
	}

	/**
	 * @return the columns of the table but its selection column, which the apps' tables have of
	 *         their own, with their templates; the texts of the table and of the page in the
	 *         language of the session; the URLs of the users and of the countries; and the
	 *         license key of the application, which activates the private table in the browser too
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JsonBasedDynamicDataTable<User, Long> table = getJsonTable();
		JSONArray columns = new JSONArray();
		for (IDynamicColumn<User> column : table.getColumns())
		{
			if ("selection".equals(column.getId()))
			{
				continue;
			}
			JSONObject json = new JSONObject()
				.put("id", column.getId() != null ? column.getId() : "column" + columns.length())
				.put("header", header(column))
				.put("template", column.getTemplate(table))
				.put("movable", table.isMovable(column))
				.put("resizable", column.isResizable())
				.put("hideable", table.isHideable(column))
				.put("visible", table.isColumnVisible(column));
			if (column.getCssClass() != null)
			{
				json.put("cssClass", column.getCssClass());
			}
			if (column instanceof ISortableDynamicColumn<?, ?> sortable && sortable.isSortable())
			{
				json.put("sort", String.valueOf(sortable.getSortProperty()));
			}
			columns.put(json);
		}
		return new JSONObject()
			.put("texts", texts())
			.put("columns", columns)
			.put("usersUrl", FrameworkTabs.urlOf(this, UsersRowsResource.REFERENCE))
			.put("choicesUrl", FrameworkTabs.urlOf(this, ChoicesResource.REFERENCE))
			.put("licenseKey", licenseKey())
			.put("rowsPerPage", ROWS_PER_PAGE)
			.put("pageSizes", new JSONArray(List.of(25, 50, 100, 500)))
			.put("bodyHeight", new JSONObject()
				.put("value", DynamicTableOptionsPanel.defaultHeight(BodyHeightUnits.PIXELS))
				.put("unit", "px"))
			.put("pushMillis", 3000);
	}

	/**
	 * @return the header of a column, read as the header component reads it
	 */
	private String header(IDynamicColumn<User> column)
	{
		IModel<String> header = column.getHeader();
		if (header instanceof IComponentAssignedModel<String> assigned)
		{
			header = assigned.wrapOnAssignment(this);
		}
		return header.getObject();
	}

	private JSONObject texts()
	{
		JSONObject texts = FrameworkTabs.texts(this, "filter.placeholder", "newUser", "editUser",
			"delete", "delete.confirm", "deleted", "selection.none", "selection.some");
		texts.put("filterLabel", getString("filter"));
		texts.put("added", getString("added.rendered"));
		texts.put("required", getString("Required", Model.ofMap(Map.of("label", "{0}"))));
		for (String key : new String[] { "firstName", "lastName", "country", "city", "address",
			"save", "cancel" })
		{
			texts.put(key, string(UserEditPanel.class, key));
		}
		for (String key : new String[] { "infiniteScroll", "infiniteScroll.off",
			"infiniteScroll.on" })
		{
			texts.put(key, string(DynamicTableOptionsPanel.class, key));
		}
		JSONObject table = new JSONObject()
			.put("showing", getString("NavigatorLabel", Model.ofMap(Map.of("from", "{0}", "to",
				"{1}", "of", "{2}"))))
			.put("first", getString("PagingNavigator.first"))
			.put("previous", getString("PagingNavigator.previous"))
			.put("next", getString("PagingNavigator.next"))
			.put("last", getString("PagingNavigator.last"))
			.put("page", getString("PagingNavigation.page", Model.ofMap(Map.of("page", "{0}"))))
			.put("rowsPerPage", getString("DynamicDataTable.rowsPerPage"))
			.put("columns", getString("DynamicDataTable.chooseColumns"))
			.put("apply", getString("DynamicDataTable.applyColumns"))
			.put("selectPage", getString("DynamicDataTable.selectPage"))
			.put("selectRow", getString("DynamicDataTable.selectRow"))
			.put("moveColumn", getString("MovableColumnsBehavior.moveColumn"))
			.put("unlicensed", getString("License.placeholder"));
		return FrameworkTabs.withDropdownTexts(this, texts).put("table", table);
	}

	/**
	 * @return a text of the bundles of a class, in the language of the session
	 */
	private String string(Class<?> scope, String key)
	{
		return new ClassStringResourceLoader(scope).loadStringResource(scope, key, getLocale(),
			getStyle(), getVariation());
	}

	/**
	 * @return the license key of the application, as it installs it
	 */
	private static String licenseKey()
	{
		try (InputStream key = DynamicDataTablePage.class.getClassLoader()
			.getResourceAsStream(Licensing.RESOURCE))
		{
			return key != null ? new String(key.readAllBytes(), StandardCharsets.UTF_8).strip()
				: "";
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}
}
