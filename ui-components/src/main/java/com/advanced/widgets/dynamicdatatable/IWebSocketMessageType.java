/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.util.io.IClusterable;

/**
 * A type of {@link IWebSocketLightWeightMessage} pushed to a {@link JsonBasedDynamicDataTable},
 * together with the browser-side function handling it.
 * <p>
 * The type is registered per table, with
 * {@link JsonBasedDynamicDataTable#addMessageType(IWebSocketMessageType)}: the table's client-side
 * instance hands the messages of the type addressed to it to the type's function, if the table
 * {@link JsonBasedDynamicDataTable#setPushEnabled(boolean) accepts pushes}. {@link
 * UpdateRowsMessageType}, {@link UpdateRowMessageType} and {@link RefreshMessageType} are
 * registered by default.
 * <p>
 * {@link #getTypeFunction()} is written into the page as is and executed as JavaScript. It has to
 * be authored by the developer and must never contain anything derived from user input.
 *
 * @since 11.0.0
 */
public interface IWebSocketMessageType extends IClusterable
{
	/**
	 * @return the id messages of this type carry as their {@code typeId}
	 */
	String getTypeId();

	/**
	 * @return a JavaScript expression evaluating to the function handling messages of this type,
	 *         either a function literal or the name of a function defined elsewhere on the page.
	 *         It is called as {@code function(message)} with the parsed message, and finds the
	 *         table it is addressed to as
	 *         {@code Wicket.DynamicDataTable.instances[message.tableId]}.
	 */
	String getTypeFunction();
}
