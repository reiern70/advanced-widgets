/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.template;

import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The template engines that render JSON into markup in the browser, shared by the cells of a
 * {@link com.advanced.widgets.dynamicdatatable.JsonBasedDynamicDataTable}, the rows of an
 * {@link com.advanced.widgets.autocomplete.Autocomplete} and the items of a
 * {@link com.advanced.widgets.jsonlist.JsonBasedInfiniteList}. {@link #JS} defines
 * {@code Wicket.Templates.engines}, by name: the built-in {@code builtin} engine, which replaces
 * {@code {{path}}} with the escaped value and {@code {{{path}}}} with the raw one, and the engines
 * an {@link com.advanced.widgets.dynamicdatatable.ITemplateEngine} registers, such as
 * Handlebars.
 *
 * @since 11.0.0
 */
public final class Templates
{
	/** The script of the template engines, {@code Wicket.Templates}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Templates.class,
		"wicket-templates.js");

	private Templates()
	{
	}
}
