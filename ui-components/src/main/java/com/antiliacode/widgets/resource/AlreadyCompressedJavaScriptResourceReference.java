/*
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
package com.antiliacode.widgets.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * A reference to a script the build has minified already, which Wicket serves without its own
 * compressor, after the scripts it depends on. The build writes a {@code .min.js} next to every
 * script of ui-components, which Wicket serves in deployment mode; Wicket's compressor, which in
 * deployment mode strips the scripts named without {@code .min.}, would only strip them again, and
 * would break the regular expressions of the bundles of the editors. In development mode Wicket
 * serves the script as the build wrote it, readable, and compresses nothing.
 *
 * <pre>
 * public static final ResourceReference HIGHLIGHT_JS = new AlreadyCompressedJavaScriptResourceReference(
 * 	RichTextEditor.class, "antilia-rich-text-highlight.js", RichTextEditor.JS);
 * </pre>
 *
 * @since 11.0.0
 */
public class AlreadyCompressedJavaScriptResourceReference extends JavaScriptResourceReference
{
	private static final long serialVersionUID = 1L;

	private final ResourceReference[] dependencies;

	/**
	 * @param scope
	 *            the class the script is next to
	 * @param name
	 *            the name of the script
	 * @param dependencies
	 *            the scripts it needs, which pages render before it
	 */
	public AlreadyCompressedJavaScriptResourceReference(Class<?> scope, String name,
		ResourceReference... dependencies)
	{
		super(scope, name);
		Args.notNull(dependencies, "dependencies");
		for (ResourceReference dependency : dependencies)
		{
			Args.notNull(dependency, "dependency");
		}
		this.dependencies = dependencies.clone();
	}

	@Override
	public JavaScriptPackageResource getResource()
	{
		JavaScriptPackageResource resource = super.getResource();
		resource.setCompress(false);
		return resource;
	}

	@Override
	public List<HeaderItem> getDependencies()
	{
		List<HeaderItem> items = new ArrayList<>(super.getDependencies());
		for (ResourceReference dependency : dependencies)
		{
			items.add(JavaScriptHeaderItem.forReference(dependency));
		}
		return items;
	}
}
