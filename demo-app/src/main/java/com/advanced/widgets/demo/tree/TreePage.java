/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.tree.JsonBasedTree;
import com.advanced.widgets.tree.Tree;

/**
 * The files of advanced-widgets in a {@link Tree} of Wicket components and in a
 * {@link JsonBasedTree} rendered in the browser from JSON, side by side; the file selected in
 * either is shown next to them. The React and Svelte tabs show the same files in the Tree of
 * {@code @advanced-widgets/react} and {@code @advanced-widgets/svelte}.
 */
public class TreePage extends FilesDemoPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public TreePage()
	{
		ProjectFilesProvider provider = new ProjectFilesProvider();

		Tree<ProjectFile> tree = new Tree<>("tree", provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelect(AjaxRequestTarget target, ProjectFile file)
			{
				show(file, target);
			}
		};
		tree.expand(expanded());
		getThemed().add(tree);

		JsonBasedTree<ProjectFile, Integer> jsonTree = new JsonBasedTree<>("jsonTree", provider)
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
		jsonTree.setNodeTemplate(TEMPLATE);
		jsonTree.setLabel(new ResourceModel("files"));
		jsonTree.expand(expanded());
		getThemed().add(jsonTree);
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "tree";
	}
}
