/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.dynamicdatatable;

/**
 * Messages of this type make the table fetch the rows of its current page again, and have the
 * navigation re-rendered if the number of rows or the current page changed. Every
 * {@link JsonBasedDynamicDataTable} registers it; {@link
 * JsonBasedDynamicDataTable#newRefreshMessage()} builds such a message.
 *
 * @since 11.0.0
 */
public final class RefreshMessageType implements IWebSocketMessageType
{
	private static final long serialVersionUID = 1L;

	/** The shared instance. */
	public static final RefreshMessageType INSTANCE = new RefreshMessageType();

	/** The id of the type. */
	public static final String TYPE_ID = "refresh";

	private RefreshMessageType()
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
		return "function (message) { Wicket.DynamicDataTable.instances[message.tableId].load(); }";
	}
}
