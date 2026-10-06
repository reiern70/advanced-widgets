/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.util.io.IClusterable;

/**
 * Turns the rows of a {@link JsonBasedDynamicDataTable} into JSON.
 * <p>
 * Whatever a row serializes to is sent to the browser, where every column template can read it.
 * A property that must not reach the browser has to be left out here.
 *
 * @since 11.0.0
 */
@FunctionalInterface
public interface IJsonSerializer extends IClusterable
{
	/**
	 * @param value
	 *            a {@link java.util.Map} or {@link java.util.List} containing the rows, or a row
	 * @return the JSON
	 */
	String toJson(Object value);
}
