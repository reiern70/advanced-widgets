/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.danekja.java.util.function.serializable.SerializableFunction;

/**
 * Base class for columns of a {@link JsonBasedDynamicDataTable}, holding the header, the CSS class
 * and the function returning the {@link #setExportValue(SerializableFunction) export value}.
 * Subclasses supply the template; see {@link IDynamicColumn} for how it is evaluated, and why it
 * has to be authored by the developer.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public abstract class AbstractDynamicColumn<T> implements IDynamicColumn<T>
{
	private static final long serialVersionUID = 1L;

	private final IModel<String> header;

	private String id;

	private String cssClass;

	private SerializableFunction<T, ?> exportValue;

	/**
	 * @param header
	 *            the header text, rendered escaped
	 */
	protected AbstractDynamicColumn(IModel<String> header)
	{
		this.header = Args.notNull(header, "header");
	}

	@Override
	public IModel<String> getHeader()
	{
		return header;
	}

	@Override
	public String getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id of the column, unique among the columns of its table, by which a
	 *            {@link ColumnState} refers to it
	 * @return {@code this}
	 * @since 11.0.0
	 */
	public AbstractDynamicColumn<T> setId(String id)
	{
		this.id = id;
		return this;
	}

	@Override
	public String getCssClass()
	{
		return cssClass;
	}

	/**
	 * @param cssClass
	 *            the CSS class put on the header and on every cell of the column
	 * @return {@code this}
	 */
	public AbstractDynamicColumn<T> setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
		return this;
	}

	/**
	 * Makes the column exportable, for example by a {@link CsvExportToolbarAction}.
	 *
	 * @param exportValue
	 *            returns the value of the column for a row, for example {@code Contact::getName};
	 *            {@code null} for a column that is not exported
	 * @return {@code this}
	 */
	public AbstractDynamicColumn<T> setExportValue(SerializableFunction<T, ?> exportValue)
	{
		this.exportValue = exportValue;
		return this;
	}

	@Override
	public boolean isExportable()
	{
		return exportValue != null;
	}

	@Override
	public Object getExportValue(T row)
	{
		return exportValue != null ? exportValue.apply(row) : null;
	}
}
