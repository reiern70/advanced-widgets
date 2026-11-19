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
package com.antiliacode.widgets.demo.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceStreamResource;

/**
 * Serves the style sheet of a framework of CSS from its web jar, for the pages in its look: only
 * the style sheet, never a script, from the application itself, as its content security policy
 * wants. The URL has the version of the web jar, {@code <path>/<version>/<file>}, so browsers fetch
 * the style sheet anew after an upgrade; any other URL is not found.
 */
public abstract class WebJarStyleSheetReference extends ResourceReference
{
	private static final long serialVersionUID = 1L;

	private static final int NOT_FOUND = 404;

	private final String file;

	private final String location;

	private final String version;

	/**
	 * @param path
	 *            the path the application mounts the reference at
	 * @param webJar
	 *            the name of the web jar, {@code bootstrap} for {@code org.webjars.npm:bootstrap}
	 * @param folder
	 *            the folder of the style sheet in the version's folder of the web jar, ending in
	 *            {@code /}; empty for the version's folder
	 * @param file
	 *            the name of the style sheet
	 */
	protected WebJarStyleSheetReference(String path, String webJar, String folder, String file)
	{
		super(WebJarStyleSheetReference.class, path);
		this.file = file;
		version = version(webJar);
		location = "META-INF/resources/webjars/" + webJar + "/" + version + "/" + folder + file;
	}

	/**
	 * @return the header item of the style sheet, before every other one, so that the style sheets
	 *         of the components and of the page come after it and win
	 */
	public HeaderItem headerItem()
	{
		return new PriorityHeaderItem(CssHeaderItem.forReference(this,
			new PageParameters().set(0, version).set(1, file), null));
	}

	@Override
	public IResource getResource()
	{
		return attributes -> {
			PageParameters parameters = attributes.getParameters();
			boolean known = parameters.getIndexedCount() == 2 &&
				version.equals(parameters.get(0).toString()) &&
				file.equals(parameters.get(1).toString());
			URL url = known ? WebJarStyleSheetReference.class.getClassLoader()
				.getResource(location) : null;
			if (url == null)
			{
				((WebResponse)attributes.getResponse()).sendError(NOT_FOUND, null);
				return;
			}
			new ResourceStreamResource(new UrlResourceStream(url)).respond(attributes);
		};
	}

	private static String version(String webJar)
	{
		try (InputStream in = WebJarStyleSheetReference.class.getClassLoader()
			.getResourceAsStream("META-INF/maven/org.webjars.npm/" + webJar + "/pom.properties"))
		{
			Properties properties = new Properties();
			properties.load(in);
			return properties.getProperty("version");
		}
		catch (IOException | RuntimeException e)
		{
			throw new WicketRuntimeException("The web jar " + webJar + " is missing", e);
		}
	}
}
