/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

/**
 * A column of a {@link JsonBasedDynamicDataTable} with a fixed template. See {@link IDynamicColumn}
 * for how the template is evaluated, and why it has to be authored by the developer.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public class DynamicColumn<T> extends AbstractDynamicColumn<T>
{
	private static final long serialVersionUID = 1L;

	private final String template;

	/**
	 * @param header
	 *            the header text, rendered escaped
	 * @param template
	 *            the cell template, trusted markup authored by the developer
	 */
	public DynamicColumn(IModel<String> header, String template)
	{
		super(header);
		this.template = Args.notNull(template, "template");
	}

	@Override
	public String getTemplate()
	{
		return template;
	}
}
