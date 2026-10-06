/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dynamicdatatable;

import org.apache.wicket.util.lang.Args;

/**
 * Messages of this type repaint a single row of the table from the data they carry, leaving the
 * other rows alone. The row is found by its key; a message for a row the browser does not show,
 * for example one on another page, is ignored. Every {@link JsonBasedDynamicDataTable} registers
 * it; {@link JsonBasedDynamicDataTable#newUpdateRowMessage(Object)} builds such a message, and
 * {@link #newMessage(String, String, Object)} builds one without the table, for example on a
 * background thread.
 *
 * @since 11.0.0
 */
public final class UpdateRowMessageType implements IWebSocketMessageType
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final UpdateRowMessageType INSTANCE = new UpdateRowMessageType();

	/** The id of the type. */
	public static final String TYPE_ID = "updateRow";

	private UpdateRowMessageType()
	{
	}

	/**
	 * Builds a message repainting a row, sent as {@code tableId}, {@code typeId}, {@code key} and
	 * {@code data}. Every property the serializer produces for {@code data} reaches the browser.
	 *
	 * @param tableId
	 *            the markup id of the table
	 * @param key
	 *            the key of the row as the browser shows it, see
	 *            {@link JsonBasedDynamicDataTable#getRowKey(Object)}
	 * @param data
	 *            the row, serialized when the message is sent
	 * @return the message
	 */
	public static IWebSocketLightWeightMessage newMessage(String tableId, String key, Object data)
	{
		return new JsonBasedDynamicDataTable.UpdateRowMessage(Args.notEmpty(tableId, "tableId"),
			Args.notNull(key, "key"), data);
	}

	@Override
	public String getTypeId()
	{
		return TYPE_ID;
	}

	@Override
	public String getTypeFunction()
	{
		return "function (message) { " +
			"Wicket.DynamicDataTable.instances[message.tableId].renderRow(message.key, " +
			"message.data); }";
	}
}
