/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.tree;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * The {@link ProjectFiles} of the React and Svelte tree examples as JSON, mounted by the
 * application at {@value #PATH}: the files and folders at the root of the project, or, with
 * {@value #PARENT}, those of a folder, as the core's {@code JsonTree} reads them, their texts in
 * the language of the session.
 *
 * <pre>
 * { "nodes": [{ "key": "2", "leaf": false, "data": { "name": "demo-app", ... } }, ...] }
 * </pre>
 */
public class FilesResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "api/files";

	/** The parameter holding the id of the folder whose files to send. */
	public static final String PARENT = "parent";

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(FilesResource.class,
		"files")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new FilesResource();
		}
	};

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes)
	{
		Locale locale = Session.get().getLocale();
		String parent = attributes.getParameters().get(PARENT).toOptionalString();
		String json = nodes(parent, locale).toString();

		ResourceResponse response = new ResourceResponse();
		response.setContentType("application/json");
		response.setTextEncoding("UTF-8");
		response.disableCaching();
		response.setWriteCallback(new WriteCallback()
		{
			@Override
			public void writeData(Attributes attributes)
			{
				attributes.getResponse().write(json.getBytes(StandardCharsets.UTF_8));
			}
		});
		return response;
	}

	/**
	 * @param parent
	 *            the id of a folder, {@code null} for the root of the project
	 * @param locale
	 *            the language of the texts
	 * @return the files of the folder, none if there is no such folder
	 */
	static JSONObject nodes(String parent, Locale locale)
	{
		ProjectFiles files = ProjectFiles.get();
		List<ProjectFile> nodes;
		if (parent == null)
		{
			nodes = files.roots();
		}
		else
		{
			ProjectFile folder = parseId(parent) != null ? files.byId(parseId(parent)) : null;
			nodes = folder != null ? files.children(folder) : List.of();
		}
		JSONArray json = new JSONArray();
		for (ProjectFile file : nodes)
		{
			json.put(new JSONObject().put("key", String.valueOf(file.id()))
				.put("leaf", file.isLeaf())
				.put("data", new JSONObject(ProjectFiles.toJson(file, locale))));
		}
		return new JSONObject().put("nodes", json);
	}

	private static Integer parseId(String text)
	{
		try
		{
			return Integer.valueOf(text);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
