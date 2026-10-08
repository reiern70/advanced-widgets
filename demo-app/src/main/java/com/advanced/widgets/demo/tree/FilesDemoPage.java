/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.tree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * What the tree examples share: the {@link ProjectFiles} shown in a tree of Wicket components and
 * in one rendered from JSON, the folder {@code frontend} expanded, and the file selected in either
 * shown next to them, repainted through Ajax. Their React and Svelte apps load the files from the
 * {@link FilesResource} and show the file selected themselves.
 */
public abstract class FilesDemoPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The template of a node of the trees rendered from JSON: the name of the file. */
	static final String TEMPLATE = "{{name}}";

	/** The folder expanded at first. */
	static final String EXPANDED = "frontend";

	private static final ResourceReference CSS = new CssResourceReference(FilesDemoPage.class,
		"tree-demo.css");

	private final WebMarkupContainer themed;

	private final FileDetailsPanel details;

	/** The id of the file selected, {@code null} while none is. */
	private Integer selected;

	protected FilesDemoPage()
	{
		themed = newThemedContainer("themed");
		add(themed);
		details = new FileDetailsPanel("details",
			() -> selected != null ? ProjectFiles.get().byId(selected) : null);
		themed.add(details);
	}

	/**
	 * @return the container of the trees and the details, in the session's theme
	 */
	protected WebMarkupContainer getThemed()
	{
		return themed;
	}

	/**
	 * Shows a file next to the trees, as when the user selects it in one of them.
	 */
	protected void show(ProjectFile file, AjaxRequestTarget target)
	{
		selected = file.id();
		target.add(details);
	}

	/**
	 * @return the folder expanded at first
	 */
	protected static ProjectFile expanded()
	{
		return ProjectFiles.get().byPath(EXPANDED);
	}

	/**
	 * @return the configuration the React and Svelte apps share: the texts in the language of the
	 *         session, those of the trees too, the URL of the files, the template of a node, the
	 *         folder expanded at first
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "files", "selectedFile", "noneSelected", "name",
			"path", "kind", "size", "modified");
		JSONObject tree = new JSONObject()
			.put("loading", getString("Tree.loading"))
			.put("empty", getString("Tree.empty"))
			.put("unlicensed", getString("License.placeholder"));
		return new JSONObject()
			.put("texts", texts.put("tree", tree))
			.put("filesUrl", FrameworkTabs.urlOf(this, FilesResource.REFERENCE))
			.put("template", TEMPLATE)
			.put("expanded", new JSONArray().put(String.valueOf(expanded().id())));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
	}
}
