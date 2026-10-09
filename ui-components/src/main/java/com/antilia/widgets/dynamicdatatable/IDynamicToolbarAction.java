/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.util.io.IClusterable;

/**
 * An action on the whole table, shown in its navigation after the paging links, for example
 * {@link CsvExportToolbarAction}. Registered with
 * {@link BaseDynamicDataTable#addToolbarAction(IDynamicToolbarAction)}, it is shown in every
 * {@link DynamicNavigationToolbar} of the table.
 *
 * @param <T>
 *            the type of the rows
 * @param <K>
 *            the type of the keys of the rows
 * @since 11.0.0
 */
@FunctionalInterface
public interface IDynamicToolbarAction<T, K extends Serializable> extends IClusterable
{
	/**
	 * Creates the component of the action, for example an icon button, once per toolbar showing
	 * it.
	 *
	 * @param id
	 *            the id the component must have; it is attached to a {@code <span>}
	 * @param table
	 *            the table
	 * @return the component
	 */
	Component newComponent(String id, BaseDynamicDataTable<T, K, ?> table);
}
