/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import java.io.Serializable;

/**
 * An object that knows its own key, for example the primary key of an entity.
 * <p>
 * The rows of an {@link IIdentifiableDynamicDataProvider} implement it, which takes their key from
 * {@link #getId()}. The object itself does not have to be serializable, its key does.
 *
 * @param <K>
 *            the type of the key
 * @since 11.0.0
 */
public interface IIdentifiable<K extends Serializable>
{
	/**
	 * @return the key, never {@code null}, unique among the objects of its kind and the same for
	 *         as long as the object exists
	 */
	K getId();
}
