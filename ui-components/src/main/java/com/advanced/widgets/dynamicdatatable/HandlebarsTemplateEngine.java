/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.advanced.widgets.template.Templates;

/**
 * Evaluates the column templates with <a href="https://handlebarsjs.com/">Handlebars</a>.
 * <p>
 * Wicket does not ship Handlebars: the application has to put it on the page itself, before the
 * table. Handlebars compiles templates to functions at runtime, so under a Content Security Policy
 * it needs {@code unsafe-eval}. Its {@code {{ }}} escapes HTML and {@code {{{ }}}} does not.
 *
 * @since 11.0.0
 */
public class HandlebarsTemplateEngine implements ITemplateEngine
{
	private static final long serialVersionUID = 1L;

	private static final ResourceReference ADAPTER = new JavaScriptResourceReference(
		HandlebarsTemplateEngine.class, "handlebars-adapter.js")
	{
		private static final long serialVersionUID = 1L;

		@Override
		public List<HeaderItem> getDependencies()
		{
			List<HeaderItem> dependencies = super.getDependencies();
			dependencies.add(JavaScriptHeaderItem.forReference(Templates.JS));
			return dependencies;
		}
	};

	@Override
	public String getName()
	{
		return "handlebars";
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(ADAPTER));
	}
}
