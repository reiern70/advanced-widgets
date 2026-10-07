/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.util.Map;

import org.apache.wicket.protocol.ws.api.WebSocketBehavior;

/**
 * The web socket behavior of the demos, which connects with either {@link AjaxEngine}.
 * <p>
 * The setup script of Wicket's web socket support calls {@code jQuery.extend(...)}, so with the
 * plain JavaScript engine, where there is no jQuery, the page never connects. This behavior sets
 * the same values with {@code Object.assign(...)}, which needs no library.
 */
public abstract class DemoWebSocketBehavior extends WebSocketBehavior
{
	private static final long serialVersionUID = 1L;

	@Override
	protected String getWebSocketSetupScript(Map<String, Object> parameters)
	{
		return super.getWebSocketSetupScript(parameters).replace("jQuery.extend(",
			"Object.assign(");
	}
}
