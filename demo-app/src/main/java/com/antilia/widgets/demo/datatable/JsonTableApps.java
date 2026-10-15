/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.datatable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;

import com.antilia.widgets.demo.frameworks.ChoicesResource;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.dynamicdatatable.IDynamicColumn;
import com.antilia.widgets.dynamicdatatable.ISortableDynamicColumn;
import com.antilia.widgets.dynamicdatatable.JacksonJsonSerializer;
import com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The configuration of the React and Svelte apps of the JSON grid, {@code jsonTable} in the bundles
 * built from {@code frontend/demo}, made from a Wicket grid of users: the
 * {@link DynamicDataTablePage} gives it to its React and Svelte tabs, the desktop demo to the React
 * and Svelte grids in its windows.
 */
public final class JsonTableApps
{
	/** The number of users in the local list of the apps' grids. */
	private static final int LOCAL_USERS = 200;

	private JsonTableApps()
	{
	}

	/**
	 * @param component
	 *            the component the texts are read for, the page mostly, in the language of its
	 *            session
	 * @param table
	 *            the Wicket grid whose columns the apps' grids show
	 * @return the columns of the table but its selection column, which the apps' tables have of
	 *         their own, with their templates; the texts of the table and of the JSON grid demo
	 *         in the language of the session; the URLs of the users and of the countries; the
	 *         local list of users and the sizes of the grid
	 */
	public static JSONObject config(Component component, JsonBasedDynamicDataTable<User, Long> table)
	{
		JSONArray columns = new JSONArray();
		for (IDynamicColumn<User> column : table.getColumns())
		{
			if ("selection".equals(column.getId()))
			{
				continue;
			}
			JSONObject json = new JSONObject()
				.put("id", column.getId() != null ? column.getId() : "column" + columns.length())
				.put("header", header(table, column))
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
			.put("texts", texts(component))
			.put("columns", columns)
			.put("usersUrl", FrameworkTabs.urlOf(component, UsersRowsResource.REFERENCE))
			.put("localUsers", localUsers())
			.put("choicesUrl", FrameworkTabs.urlOf(component, ChoicesResource.REFERENCE))
			.put("rowsPerPage", UsersTablePanel.ROWS_PER_PAGE)
			.put("pageSizes", new JSONArray(List.of(25, 50, 100, 500)))
			.put("bodyHeight", new JSONObject()
				.put("value", DynamicTableOptionsPanel.defaultHeight(BodyHeightUnits.PIXELS))
				.put("unit", "px"))
			.put("pushMillis", 3000);
	}

	/**
	 * @return the first {@value #LOCAL_USERS} users by first name, the local list of the apps'
	 *         grids, as JSON rows {@code [{key, number, data}]} as the users resource sends them
	 */
	private static JSONArray localUsers()
	{
		UsersProvider provider = new UsersProvider();
		List<Map<String, Object>> rows = new ArrayList<>();
		long number = 1;
		for (Iterator<? extends User> it = provider.iterator(0, LOCAL_USERS); it.hasNext();)
		{
			User user = it.next();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("key", provider.formatKey(user.getId()));
			row.put("number", number++);
			row.put("data", user);
			rows.add(row);
		}
		return new JSONArray(JacksonJsonSerializer.INSTANCE.toJson(rows));
	}

	/**
	 * @return the header of a column, read as the header component reads it, in the bundles of the
	 *         table and of the panel and page it is in
	 */
	private static String header(Component component, IDynamicColumn<User> column)
	{
		IModel<String> header = column.getHeader();
		if (header instanceof IComponentAssignedModel<String> assigned)
		{
			header = assigned.wrapOnAssignment(component);
		}
		String text = header.getObject();
		return text != null ? text : "";
	}

	private static JSONObject texts(Component component)
	{
		JSONObject texts = new JSONObject();
		for (String key : new String[] { "filter.placeholder", "newUser", "editUser", "delete",
			"delete.confirm", "deleted", "saved", "failed", "added" })
		{
			texts.put(key, string(component, UsersTablePanel.class, key));
		}
		texts.put("filterLabel", string(component, UsersTablePanel.class, "filter"));
		texts.put("required", component.getString("Required",
			Model.ofMap(Map.of("label", "{0}"))));
		for (String key : new String[] { "firstName", "lastName", "country", "city", "address" })
		{
			texts.put(key, string(component, UserEditPanel.class, "field." + key));
		}
		for (String key : new String[] { "save", "cancel" })
		{
			texts.put(key, string(component, UserEditPanel.class, key));
		}
		// what a new user's country shows until one is chosen, as in Wicket's drop-down
		texts.put("chooseOne", component.getString("null"));
		for (String key : new String[] { "infiniteScroll", "infiniteScroll.off",
			"infiniteScroll.on", "rowsSource", "rowsSource.local", "rowsSource.RESOURCE",
			"selection", "selection.NONE", "selection.ONE", "selection.MANY", "selectionCount",
			"selectionCount.shown", "selectionCount.hidden", "clearSelectionOnNavigation",
			"clearSelectionOnNavigation.off", "clearSelectionOnNavigation.on", "navigation",
			"navigation.TOP", "navigation.BOTTOM", "navigation.BOTH", "resize", "resize.NONE",
			"resize.STRETCH" })
		{
			texts.put(key, string(component, DynamicTableOptionsPanel.class, key));
		}
		JSONObject table = new JSONObject()
			.put("showing", component.getString("NavigatorLabel", Model.ofMap(Map.of("from",
				"{0}", "to", "{1}", "of", "{2}"))))
			.put("first", component.getString("PagingNavigator.first"))
			.put("previous", component.getString("PagingNavigator.previous"))
			.put("next", component.getString("PagingNavigator.next"))
			.put("last", component.getString("PagingNavigator.last"))
			.put("page", component.getString("PagingNavigation.page",
				Model.ofMap(Map.of("page", "{0}"))))
			.put("rowsPerPage", component.getString("DynamicDataTable.rowsPerPage"))
			.put("columns", component.getString("DynamicDataTable.chooseColumns"))
			.put("apply", component.getString("DynamicDataTable.applyColumns"))
			.put("selectPage", component.getString("DynamicDataTable.selectPage"))
			.put("selectRow", component.getString("DynamicDataTable.selectRow"))
			.put("moveColumn", component.getString("MovableColumnsBehavior.moveColumn"))
			.put("resizeColumn", component.getString("ResizableColumnsBehavior.resizeColumn"))
			.put("selectedCount", component.getString("DynamicDataTable.selectedCount",
				Model.ofMap(Map.of("count", "{0}"))))
			.put("clearSelection", component.getString("DynamicDataTable.clearSelection"))
			.put("unlicensed", component.getString("License.placeholder"));
		return FrameworkTabs.withDropdownTexts(component, texts).put("table", table);
	}

	/**
	 * @return a text of the bundles of a class and its superclasses, in the language of the
	 *         component's session; else as the component finds it
	 */
	private static String string(Component component, Class<?> scope, String key)
	{
		String text = new ClassStringResourceLoader(scope).loadStringResource(scope, key,
			component.getLocale(), component.getStyle(), component.getVariation());
		return text != null ? text : component.getString(key);
	}
}
