/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antilia.widgets.table.ResizableColumnsBehavior.Mode;
import com.antilia.widgets.tree.JsonBasedTreeGrid;
import com.antilia.widgets.tree.TreeGrid;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The files of antilia-widgets in a {@link TreeGrid} of Wicket components and in a
 * {@link JsonBasedTreeGrid} rendered in the browser from JSON, one above the other, with their
 * kinds, sizes and the days they were changed; the file selected in either is shown next to them.
 * A drop-down above them chooses whether and how their columns are resized. The React and Svelte tabs show the same files in the TreeGrid of
 * {@code @antilia/react} and {@code @antilia/svelte}.
 */
public class TreeGridPage extends FilesDemoPage
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the columns of numbers. */
	private static final String NUMBER = "antilia-tree-number";

	/** The columns after the tree column, by the key of their header and of the data they show. */
	private static final List<String> COLUMNS = List.of("kind", "size", "modified");

	/** How the columns are resized, by the keys of the choices of the drop-down. */
	private static final List<String> RESIZING = List.of("none", "fit", "stretch");

	/** How the columns are resized at first. */
	private static final Mode RESIZED = Mode.FIT;

	private final TreeGrid<ProjectFile, String> treeGrid;

	private final JsonBasedTreeGrid<ProjectFile, Integer> jsonTreeGrid;

	/**
	 * Constructor.
	 */
	public TreeGridPage()
	{
		ProjectFilesProvider provider = new ProjectFilesProvider();

		treeGrid = new TreeGrid<>("treeGrid", columns(),
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
		treeGrid.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		treeGrid.setResizableColumns(RESIZED);
		treeGrid.setOutputMarkupId(true);
		withIcons(treeGrid::setIcon);
		getThemed().add(treeGrid);

		jsonTreeGrid = new JsonBasedTreeGrid<>(
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
		jsonTreeGrid.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		jsonTreeGrid.setResizableColumns(RESIZED);
		jsonTreeGrid.setOutputMarkupId(true);
		withIcons(jsonTreeGrid::setIcon);
		getThemed().add(jsonTreeGrid);

		getThemed().add(resizing("resize"));
	}

	/**
	 * @return the drop-down choosing whether and how the columns of both tree grids are resized,
	 *         repainting them
	 */
	private Dropdown<String> resizing(String id)
	{
		Dropdown<String> resizing = new Dropdown<>(id, LambdaModel.of(
			() -> keyOf(treeGrid.getResizableColumns()), this::resize), RESIZING,
			new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(String key)
				{
					return getString("resize." + key);
				}

				@Override
				public String getIdValue(String key, int index)
				{
					return key;
				}
			});
		resizing.setRequired(true);
		resizing.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				target.add(treeGrid, jsonTreeGrid);
			}
		});
		return resizing;
	}

	private void resize(String key)
	{
		Mode mode = "none".equals(key) ? null : Mode.valueOf(key.toUpperCase(Locale.ROOT));
		treeGrid.setResizableColumns(mode);
		jsonTreeGrid.setResizableColumns(mode);
	}

	private static String keyOf(Mode mode)
	{
		return mode != null ? mode.name().toLowerCase(Locale.ROOT) : "none";
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
	 * @return the height of the tree grids, lower than that of the trees, as two are shown one
	 *         above the other
	 */
	@Override
	protected int getTreeHeight()
	{
		return 384;
	}

	/**
	 * @return the configuration of the trees, with the header of the tree column and the other
	 *         columns, as the JSON tree grid has them, how their columns are resized at first and
	 *         the texts of the drop-down choosing it
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
		JSONObject config = super.getFrameworkConfig().put("header", getString("name"))
			.put("columns", columns).put("resizableColumns", keyOf(RESIZED));
		JSONObject texts = config.getJSONObject("texts").put("resize", getString("resize"));
		for (String key : RESIZING)
		{
			texts.put("resize." + key, getString("resize." + key));
		}
		return config;
	}
}
