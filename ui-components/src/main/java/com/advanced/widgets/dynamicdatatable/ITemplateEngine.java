/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.io.IClusterable;

/**
 * The client-side engine a {@link JsonBasedDynamicDataTable} evaluates its column templates with.
 * <p>
 * An engine is registered in the browser under {@link #getName()} with
 * {@code Wicket.DynamicDataTable.engines[name] = {compile: function(template) {...}}},
 * where {@code compile} returns a {@code function(data, meta)} from a row's data to the cell's
 * markup. {@code meta} holds the row variables, {@code key} and {@code number}, which templates
 * refer to as {@code {{@key}}} and {@code {{@number}}}. The escaping of the values a template
 * refers to is the engine's responsibility.
 *
 * @since 11.0.0
 */
public interface ITemplateEngine extends IClusterable
{
	/**
	 * @return the name the engine is registered under in the browser
	 */
	String getName();

	/**
	 * Contributes whatever the engine needs in the browser, typically the script registering it.
	 * Called after the table's own script has been rendered.
	 *
	 * @param response
	 *            the header response
	 */
	default void renderHead(IHeaderResponse response)
	{
	}
}
