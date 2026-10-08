/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.advanced.widgets.license.Licensing;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * A tree grid whose rows are rendered in the browser from JSON: a {@link JsonBasedTree} shown as
 * a WAI-ARIA treegrid, the core's {@code JsonTree} with a table, which
 * {@code @advanced-widgets/react} and {@code @advanced-widgets/svelte} offer as
 * {@code TreeGrid}.
 * <p>
 * The first column is the tree column, headed by {@link #setTreeHeader(IModel)}: it holds the
 * arrow of a folder, the icon and the node template, indented by the level of the node. The
 * other columns, added with {@link #addColumn(IModel, String, String)}, each render a template
 * of their own from the same JSON of the node. The children of a folder follow its row; the arrow
 * keys move between the rows as in the tree, and the rest is as in the tree.
 * <p>
 * The table carries the CSS classes {@value JsonBasedTree#CSS_CLASS} and
 * {@value TreeGrid#CSS_CLASS}.
 * <p>
 * A private component: it renders only when the {@link Licensing license key} of the application
 * activates it, else a placeholder telling so.
 *
 * @param <T>
 *            the type of the nodes
 * @param <K>
 *            the type of the keys of the nodes
 * @since 11.0.0
 */
public class JsonBasedTreeGrid<T, K extends Serializable> extends JsonBasedTree<T, K>
{
	private static final long serialVersionUID = 1L;

	private final List<Column> columns = new ArrayList<>();

	private IModel<String> treeHeader;

	/**
	 * @param id
	 *            component id
	 * @param provider
	 *            supplies the nodes and resolves them back from their keys
	 */
	public JsonBasedTreeGrid(String id, IJsonTreeProvider<T, K> provider)
	{
		super(id, provider);
	}

	/**
	 * @param header
	 *            the header of the tree column
	 * @return {@code this}
	 */
	public JsonBasedTreeGrid<T, K> setTreeHeader(IModel<String> header)
	{
		treeHeader = wrap(header);
		return this;
	}

	/**
	 * Adds a column after the tree column and those added before.
	 *
	 * @param header
	 *            the header of the column
	 * @param template
	 *            the template of its cells, trusted markup evaluated against the JSON of the node
	 *            of a row
	 * @return {@code this}
	 */
	public JsonBasedTreeGrid<T, K> addColumn(IModel<String> header, String template)
	{
		return addColumn(header, template, null);
	}

	/**
	 * Adds a column after the tree column and those added before.
	 *
	 * @param header
	 *            the header of the column
	 * @param template
	 *            the template of its cells, trusted markup evaluated against the JSON of the node
	 *            of a row
	 * @param cssClass
	 *            the CSS class of its header and cells, {@code null} for none
	 * @return {@code this}
	 */
	public JsonBasedTreeGrid<T, K> addColumn(IModel<String> header, String template,
		String cssClass)
	{
		columns.add(new Column(wrap(Args.notNull(header, "header")),
			Args.notEmpty(template, "template"), cssClass));
		return this;
	}

	@Override
	String getTreeCssClass()
	{
		return TreeGrid.CSS_CLASS;
	}

	@Override
	protected JSONObject buildConfig()
	{
		JSONArray json = new JSONArray();
		for (Column column : columns)
		{
			JSONObject entry = new JSONObject().put("header", column.header.getObject())
				.put("template", column.template);
			if (column.cssClass != null)
			{
				entry.put("cssClass", column.cssClass);
			}
			json.put(entry);
		}
		return super.buildConfig().put("table", new JSONObject()
			.put("header", treeHeader != null ? treeHeader.getObject() : "")
			.put("columns", json));
	}

	@Override
	protected void onDetach()
	{
		if (treeHeader != null)
		{
			treeHeader.detach();
		}
		for (Column column : columns)
		{
			column.header.detach();
		}
		super.onDetach();
	}

	private record Column(IModel<String> header, String template, String cssClass)
		implements IClusterable
	{
	}
}
