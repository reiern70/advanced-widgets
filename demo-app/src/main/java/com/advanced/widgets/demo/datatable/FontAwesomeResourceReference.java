/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.datatable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceStreamResource;

/**
 * Serves the style sheet and the fonts of the Font Awesome web jar, so the page can show
 * {@link com.advanced.widgets.icon.FontAwesomeIcon}s. The application mounts it
 * at {@value #PATH}; the style sheet finds the fonts next to it.
 */
public class FontAwesomeResourceReference extends ResourceReference
{
	private static final long serialVersionUID = 1L;

	public static final String PATH = "fontawesome";

	public static final FontAwesomeResourceReference INSTANCE = new FontAwesomeResourceReference();

	private static final String WEBJAR = "META-INF/resources/webjars/fortawesome__fontawesome-free/";

	private static final Pattern FILES = Pattern
		.compile("css/all\\.min\\.css|webfonts/[a-z0-9-]+\\.woff2");

	private static final int NOT_FOUND = 404;

	private static final String VERSION = version();

	private FontAwesomeResourceReference()
	{
		super(FontAwesomeResourceReference.class, PATH);
	}

	/**
	 * @return the header item of the Font Awesome style sheet
	 */
	static CssHeaderItem styleSheet()
	{
		return CssHeaderItem.forReference(INSTANCE,
			new PageParameters().set(0, "css").set(1, "all.min.css"), null);
	}

	@Override
	public IResource getResource()
	{
		return attributes -> {
			PageParameters parameters = attributes.getParameters();
			String file = parameters.getIndexedCount() == 2
				? parameters.get(0) + "/" + parameters.get(1) : "";
			URL url = FILES.matcher(file).matches() ? FontAwesomeResourceReference.class
				.getClassLoader().getResource(WEBJAR + VERSION + "/" + file) : null;
			if (url == null)
			{
				((WebResponse)attributes.getResponse()).sendError(
					NOT_FOUND, null);
				return;
			}
			new ResourceStreamResource(new UrlResourceStream(url)).respond(attributes);
		};
	}

	private static String version()
	{
		try (InputStream in = FontAwesomeResourceReference.class.getClassLoader()
			.getResourceAsStream(
				"META-INF/maven/org.webjars.npm/fortawesome__fontawesome-free/pom.properties"))
		{
			Properties properties = new Properties();
			properties.load(in);
			return properties.getProperty("version");
		}
		catch (IOException | RuntimeException e)
		{
			throw new WicketRuntimeException("The Font Awesome web jar is missing", e);
		}
	}
}
