/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.WebSocketResource;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;

/**
 * The websocket of the home page's status: pushes {@value #LIVE} back to every browser that
 * connects, which then shows that the site is live. A shared resource rather than a behavior of
 * the page, so that the home page stays stateless.
 *
 * @see LiveStatusBehavior
 */
public class LiveResource extends WebSocketResource
{
	/** The name of the shared resource. */
	public static final String NAME = "live";

	/** The message pushed to a browser that connected. */
	public static final String LIVE = "live";

	@Override
	protected void onConnect(ConnectedMessage message)
	{
		super.onConnect(message);

		// the connection can only be written to with a WebSocketRequestHandler, so push a message
		// back to it
		WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(message.getApplication());
		new WebSocketPushBroadcaster(webSocketSettings.getConnectionRegistry())
			.broadcast(message, new ConnectedPushMessage());
	}

	@Override
	protected void onPush(WebSocketRequestHandler handler, IWebSocketPushMessage message)
	{
		if (message instanceof ConnectedPushMessage)
		{
			handler.push(LIVE);
		}
	}

	private static class ConnectedPushMessage implements IWebSocketPushMessage
	{
	}
}
