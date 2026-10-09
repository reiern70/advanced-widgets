/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

/**
 * Messages of this type replace the rows of the table with the ones they carry, regardless of the
 * current page. Every {@link JsonBasedDynamicDataTable} registers it;
 * {@link JsonBasedDynamicDataTable#newUpdateRowsMessage(java.util.List)} builds such a message.
 *
 * @since 11.0.0
 */
public final class UpdateRowsMessageType implements IWebSocketMessageType
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final UpdateRowsMessageType INSTANCE = new UpdateRowsMessageType();

	/** The id of the type. */
	public static final String TYPE_ID = "updateRows";

	private UpdateRowsMessageType()
	{
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
			"Wicket.DynamicDataTable.instances[message.tableId].render(message.rows); }";
	}
}
