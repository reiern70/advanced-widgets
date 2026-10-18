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
package com.antilia.widgets.demo.theme;

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
 * Serves the style sheet of the Bootstrap web jar, for the pages in the Bootstrap look or a
 * Bootstrap theme; only the style sheet, never Bootstrap's script. The application mounts it at
 * {@value #PATH}.
 */
public class BootstrapResourceReference extends ResourceReference
{
	private static final long serialVersionUID = 1L;

	public static final String PATH = "bootstrap";

	public static final BootstrapResourceReference INSTANCE = new BootstrapResourceReference();

	private static final String WEBJAR = "META-INF/resources/webjars/bootstrap/";

	private static final String FILE = "bootstrap.min.css";

	private static final int NOT_FOUND = 404;

	private static final String VERSION = version();

	private BootstrapResourceReference()
	{
		super(BootstrapResourceReference.class, PATH);
	}

	/**
	 * @return the header item of Bootstrap's style sheet, before every other one, so that the
	 *         style sheets of the components and of the page come after it and win
	 */
	public static HeaderItem styleSheet()
	{
		return new PriorityHeaderItem(CssHeaderItem.forReference(INSTANCE,
			new PageParameters().set(0, VERSION).set(1, FILE), null));
	}

	@Override
	public IResource getResource()
	{
		return attributes -> {
			PageParameters parameters = attributes.getParameters();
			// the version in the path, so that browsers fetch the style sheet anew after an upgrade
			boolean known = parameters.getIndexedCount() == 2 &&
				VERSION.equals(parameters.get(0).toString()) &&
				FILE.equals(parameters.get(1).toString());
			URL url = known ? BootstrapResourceReference.class.getClassLoader()
				.getResource(WEBJAR + VERSION + "/dist/css/" + FILE) : null;
			if (url == null)
			{
				((WebResponse)attributes.getResponse()).sendError(NOT_FOUND, null);
				return;
			}
			new ResourceStreamResource(new UrlResourceStream(url)).respond(attributes);
		};
	}

	private static String version()
	{
		try (InputStream in = BootstrapResourceReference.class.getClassLoader()
			.getResourceAsStream("META-INF/maven/org.webjars.npm/bootstrap/pom.properties"))
		{
			Properties properties = new Properties();
			properties.load(in);
			return properties.getProperty("version");
		}
		catch (IOException | RuntimeException e)
		{
			throw new WicketRuntimeException("The Bootstrap web jar is missing", e);
		}
	}
}
