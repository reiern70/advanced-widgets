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
 * The {@link TreeOfLife} of the React and Svelte tree examples as JSON, mounted by the
 * application at {@value #PATH}: the kingdoms, or, with {@value #PARENT}, the groups and the
 * species of a group, as the core's {@code JsonTree} reads them, their texts in the language of
 * the session.
 *
 * <pre>
 * { "nodes": [{ "key": "animals", "leaf": false, "data": { "name": "Animals", ... } }, ...] }
 * </pre>
 */
public class TreeOfLifeResource extends AbstractResource
{
	private static final long serialVersionUID = 1L;

	/** The path the resource is mounted at. */
	public static final String PATH = "api/tree-of-life";

	/** The parameter holding the id of the group whose groups and species to send. */
	public static final String PARENT = "parent";

	/** The reference of the resource, to mount it and to link to it. */
	public static final ResourceReference REFERENCE = new ResourceReference(
		TreeOfLifeResource.class, "tree-of-life")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public IResource getResource()
		{
			return new TreeOfLifeResource();
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
	 *            the id of a group, {@code null} for the kingdoms
	 * @param locale
	 *            the language of the texts
	 * @return the groups and the species of the group, none if there is no such group
	 */
	static JSONObject nodes(String parent, Locale locale)
	{
		TreeOfLife tree = TreeOfLife.get();
		List<Taxon> nodes;
		if (parent == null)
		{
			nodes = tree.roots();
		}
		else
		{
			Taxon group = tree.byId(parent);
			nodes = group != null ? tree.children(group) : List.of();
		}
		JSONArray json = new JSONArray();
		for (Taxon taxon : nodes)
		{
			json.put(new JSONObject().put("key", taxon.id())
				.put("leaf", taxon.isLeaf())
				.put("data", new JSONObject(tree.toJson(taxon, locale))));
		}
		return new JSONObject().put("nodes", json);
	}
}
