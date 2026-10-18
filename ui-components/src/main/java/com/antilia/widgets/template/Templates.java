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
package com.antilia.widgets.template;

import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * The template engines that render JSON into markup in the browser, shared by the cells of a
 * {@link com.antilia.widgets.dynamicdatatable.JsonBasedDynamicDataTable}, the rows of an
 * {@link com.antilia.widgets.autocomplete.Autocomplete} and the items of a
 * {@link com.antilia.widgets.jsonlist.JsonBasedInfiniteList}. {@link #JS} defines
 * {@code Wicket.Templates.engines}, by name: the built-in {@code builtin} engine, which replaces
 * {@code {{path}}} with the escaped value and {@code {{{path}}}} with the raw one, and the engines
 * an {@link com.antilia.widgets.dynamicdatatable.ITemplateEngine} registers, such as
 * Handlebars.
 *
 * @since 11.0.0
 */
public final class Templates
{
	/** The script of the template engines, {@code Wicket.Templates}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Templates.class,
		"antilia-templates.js");

	private Templates()
	{
	}
}
