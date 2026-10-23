/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.demo.tree;

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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.ResourceModel;

import com.antiliacode.widgets.demo.DemoCases;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dynamicdatatable.BaseDynamicDataTable.BodyHeightUnits;
import com.antiliacode.widgets.table.ResizableColumnsBehavior.Mode;
import com.antiliacode.widgets.tree.JsonBasedTreeGrid;
import com.antiliacode.widgets.tree.TreeGrid;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The {@link TreeOfLife} in a {@link TreeGrid} of Wicket components and in a
 * {@link JsonBasedTreeGrid} rendered in the browser from JSON, one above the other, with the
 * scientific names, the ranks, the periods and the times of the groups and the species, both
 * selecting groups as well as species; the cards of what is selected in either are shown next to
 * them. A drop-down above them chooses whether and how their columns are resized. The React and
 * Svelte tabs show the same tree in the TreeGrid of {@code @antilia/react} and
 * {@code @antilia/svelte}.
 */
public class TreeGridPage extends TreeOfLifeDemoPage
{
	private static final long serialVersionUID = 1L;

	/** The CSS class of the column of the scientific names, written in italics. */
	private static final String SCIENTIFIC = "tree-demo-scientific";

	/** The columns after the tree column, by the key of their header and of the data they show. */
	private static final List<String> COLUMNS = List.of("scientific", "rank", "period", "lived");

	/** How the columns are resized, by the keys of the choices of the drop-down. */
	private static final List<String> RESIZING = List.of("none", "fit", "stretch");

	/** How the columns are resized at first. */
	private static final Mode RESIZED = Mode.FIT;

	private final TreeGrid<Taxon, String> treeGrid;

	private final JsonBasedTreeGrid<Taxon, String> jsonTreeGrid;

	/**
	 * Constructor.
	 */
	public TreeGridPage()
	{
		TreeOfLifeProvider provider = new TreeOfLifeProvider();

		DemoCases cases = new DemoCases("cases", this);
		getThemed().add(cases);
		Fragment treeGridCase = cases.addCase("case.treeGrid", "treeGridCase");
		Fragment jsonTreeGridCase = cases.addCase("case.jsonTreeGrid", "jsonTreeGridCase");

		treeGrid = new TreeGrid<>("treeGrid", columns(), provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IModel<?> newLabelModel(IModel<Taxon> taxon)
			{
				return () -> TreeOfLife.name(taxon.getObject(), getLocale());
			}

			@Override
			protected void onSelect(AjaxRequestTarget target, Taxon taxon)
			{
				show(taxon, target);
			}
		};
		treeGrid.setSelectBranches(true);
		expanded().forEach(treeGrid::expand);
		treeGrid.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		treeGrid.setResizableColumns(RESIZED);
		treeGrid.setOutputMarkupId(true);
		withIcons(treeGrid::setIcon);
		treeGridCase.add(treeGrid);

		jsonTreeGrid = new JsonBasedTreeGrid<>(
			"jsonTreeGrid", provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Object getNodeData(Taxon taxon)
			{
				return TreeOfLife.get().toJson(taxon, getLocale());
			}

			@Override
			protected void onSelect(AjaxRequestTarget target, Taxon taxon)
			{
				show(taxon, target);
			}
		};
		jsonTreeGrid.setNodeTemplate(TEMPLATE);
		jsonTreeGrid.setLabel(new ResourceModel("treeOfLife"));
		jsonTreeGrid.setSelectBranches(true);
		jsonTreeGrid.setTreeHeader(new ResourceModel("name"));
		for (String column : COLUMNS)
		{
			jsonTreeGrid.addColumn(new ResourceModel(column), "{{" + column + "}}",
				cssClassOf(column));
		}
		expanded().forEach(jsonTreeGrid::expand);
		jsonTreeGrid.setHeight(getTreeHeight(), BodyHeightUnits.PIXELS);
		jsonTreeGrid.setResizableColumns(RESIZED);
		jsonTreeGrid.setOutputMarkupId(true);
		withIcons(jsonTreeGrid::setIcon);
		jsonTreeGridCase.add(jsonTreeGrid);

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
	 * @return the columns of the tree grid of Wicket components: the tree, then the scientific
	 *         name, the rank, the periods and the times, as the cards show them
	 */
	private static List<IColumn<Taxon, String>> columns()
	{
		List<IColumn<Taxon, String>> columns = new ArrayList<>();
		columns.add(new TreeColumn<>(new ResourceModel("name")));
		for (String column : COLUMNS)
		{
			columns.add(newColumn(new ResourceModel(column), column));
		}
		return columns;
	}

	private static IColumn<Taxon, String> newColumn(IModel<String> header, String property)
	{
		String cssClass = cssClassOf(property);
		return new LambdaColumn<>(header,
			taxon -> TreeOfLife.card(taxon, Session.get().getLocale()).get(property))
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
		return "scientific".equals(column) ? SCIENTIFIC : null;
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
