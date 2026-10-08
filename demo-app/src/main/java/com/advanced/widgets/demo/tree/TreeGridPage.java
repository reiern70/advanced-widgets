/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.tree.JsonBasedTreeGrid;
import com.advanced.widgets.tree.TreeGrid;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The files of advanced-widgets in a {@link TreeGrid} of Wicket components and in a
 * {@link JsonBasedTreeGrid} rendered in the browser from JSON, one above the other, with their
 * kinds, sizes and the days they were changed; the file selected in either is shown next to them.
 * The React and Svelte tabs show the same files in the TreeGrid of
 * {@code @advanced-widgets/react} and {@code @advanced-widgets/svelte}.
 */
public class TreeGridPage extends FilesDemoPage
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the columns of numbers. */
	private static final String NUMBER = "wicket-tree-number";

	/** The columns after the tree column, by the key of their header and of the data they show. */
	private static final List<String> COLUMNS = List.of("kind", "size", "modified");

	/**
	 * Constructor.
	 */
	public TreeGridPage()
	{
		ProjectFilesProvider provider = new ProjectFilesProvider();

		TreeGrid<ProjectFile, String> treeGrid = new TreeGrid<>("treeGrid", columns(),
			provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelect(AjaxRequestTarget target, ProjectFile file)
			{
				show(file, target);
			}
		};
		treeGrid.expand(expanded());
		getThemed().add(treeGrid);

		JsonBasedTreeGrid<ProjectFile, Integer> jsonTreeGrid = new JsonBasedTreeGrid<>(
			"jsonTreeGrid", provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Object getNodeData(ProjectFile file)
			{
				return ProjectFiles.toJson(file, getLocale());
			}

			@Override
			protected void onSelect(AjaxRequestTarget target, ProjectFile file)
			{
				show(file, target);
			}
		};
		jsonTreeGrid.setNodeTemplate(TEMPLATE);
		jsonTreeGrid.setLabel(new ResourceModel("files"));
		jsonTreeGrid.setTreeHeader(new ResourceModel("name"));
		for (String column : COLUMNS)
		{
			jsonTreeGrid.addColumn(new ResourceModel(column), "{{" + column + "}}",
				cssClassOf(column));
		}
		jsonTreeGrid.expand(expanded());
		getThemed().add(jsonTreeGrid);
	}

	/**
	 * @return the columns of the tree grid of Wicket components: the tree, then the kind, the
	 *         size and the day changed, as the details of a file show them
	 */
	private static List<IColumn<ProjectFile, String>> columns()
	{
		List<IColumn<ProjectFile, String>> columns = new ArrayList<>();
		columns.add(new TreeColumn<>(new ResourceModel("name")));
		for (String column : COLUMNS)
		{
			columns.add(newColumn(new ResourceModel(column), column));
		}
		return columns;
	}

	private static IColumn<ProjectFile, String> newColumn(IModel<String> header, String property)
	{
		String cssClass = cssClassOf(property);
		return new LambdaColumn<>(header,
			file -> ProjectFiles.toJson(file, Session.get().getLocale()).get(property))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getCssClass()
			{
				return cssClass;
			}
		};
	}

	private static String cssClassOf(String column)
	{
		return "size".equals(column) ? NUMBER : null;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "treeGrid";
	}

	/**
	 * @return the configuration of the trees, with the header of the tree column and the other
	 *         columns, as the JSON tree grid has them
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONArray columns = new JSONArray();
		for (String column : COLUMNS)
		{
			JSONObject json = new JSONObject().put("header", getString(column))
				.put("template", "{{" + column + "}}");
			if (cssClassOf(column) != null)
			{
				json.put("cssClass", cssClassOf(column));
			}
			columns.put(json);
		}
		return super.getFrameworkConfig().put("header", getString("name")).put("columns", columns);
	}
}
