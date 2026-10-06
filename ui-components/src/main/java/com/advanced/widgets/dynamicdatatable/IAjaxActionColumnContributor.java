/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A contributor whose template carries an action, handled on the server when it is clicked.
 * <p>
 * The template has to contain an element with a {@code data-dt-action} attribute whose value is
 * {@link #getActionId()}, for example {@code <a href="#" data-dt-action="select">Select</a>}. A
 * click on it is dispatched by the {@link BaseDynamicDataTable} the contributor is registered with,
 * either through a {@link CompoundDynamicColumn} or with
 * {@link BaseDynamicDataTable#addActionContributor(IAjaxActionColumnContributor)}.
 *
 * @param <T>
 *            the type of the rows
 * @since 11.0.0
 */
public interface IAjaxActionColumnContributor<T> extends IDynamicColumnContributor<T>
{
	/**
	 * @return the value of the {@code data-dt-action} attribute this contributor handles, unique
	 *         within its table
	 */
	String getActionId();

	/**
	 * Called once when the contributor is registered with a table, for example to add the
	 * behaviors it needs to the table. Does nothing by default.
	 *
	 * @param table
	 *            the table dispatching the contributor's action
	 */
	default void bind(BaseDynamicDataTable<T, ?, ?> table)
	{
	}

	/**
	 * Called when the action is clicked on a row that still exists.
	 *
	 * @param row
	 *            the row the action was clicked on, never {@code null}
	 * @param target
	 *            the Ajax request target
	 */
	void onAction(T row, AjaxRequestTarget target);
}
