/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.admin;

import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.model.ResourceModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.antilia.widgets.dynamicdatatable.IHideableColumn;
import com.antilia.widgets.table.IMovableColumn;

/**
 * A value of a row of the administration's tables, written escaped, in a column the
 * administrator can hide and move; sorted by the database when it has a sort property.
 *
 * @param <T>
 *            the type of the rows
 */
class AdminColumn<T> extends LambdaColumn<T, String> implements IHideableColumn, IMovableColumn
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param headerKey
	 *            the key of the header in the package's bundle
	 * @param sortProperty
	 *            the property of the entity sorted by, {@code null} if the column is not sorted
	 * @param value
	 *            the value of a row
	 */
	AdminColumn(String headerKey, String sortProperty, SerializableFunction<T, ?> value)
	{
		super(new ResourceModel(headerKey), sortProperty, value);
	}
}
