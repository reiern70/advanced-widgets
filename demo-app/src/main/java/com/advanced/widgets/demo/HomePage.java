/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.advanced.widgets.demo.Demos.Demo;

public class HomePage extends BasePage {
	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		super(parameters);

		Label webSocketStatus = new Label("webSocketStatus", Model.of("Connecting"));
		webSocketStatus.setOutputMarkupId(true);
		add(webSocketStatus);

		add(new WebSocketBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConnect(ConnectedMessage message) {
				super.onConnect(message);

				// components can only be updated with a WebSocketRequestHandler, so push a message back to this page
				WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(message.getApplication());
				new WebSocketPushBroadcaster(webSocketSettings.getConnectionRegistry())
					.broadcast(message, new ConnectedPushMessage());
			}

			@Override
			protected void onPush(WebSocketRequestHandler handler, IWebSocketPushMessage message) {
				if (message instanceof ConnectedPushMessage) {
					webSocketStatus.setDefaultModelObject("Live");
					webSocketStatus.add(AttributeModifier.append("class", "ws-status--live"));
					handler.add(webSocketStatus);
				}
			}
		});

		add(new ListView<>("demos", Demos.all()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item) {
				Demo demo = item.getModelObject();
				BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", demo.page());
				item.add(link);
				link.add(new Label("category", demo.category()));
				link.add(DemoPage.newVisibilityBadge("visibility", demo));
				link.add(new Label("name", demo.name()));
				link.add(new Label("description", demo.description()));
			}
		});

		add(new Label("componentCount", ComponentCatalog.components().size()));

		add(new ListView<>("components", ComponentCatalog.components()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ComponentDescriptor> item) {
				ComponentDescriptor component = item.getModelObject();
				item.add(new Label("category", component.category()));
				item.add(new Label("name", component.name()));
				item.add(new Label("description", component.description()));
				item.add(new Label("status", component.status().getLabel())
					.add(AttributeModifier.append("class", component.status().cssClass())));
			}
		});

	}

	private static class ConnectedPushMessage implements IWebSocketPushMessage {
	}
}
