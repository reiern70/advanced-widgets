/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antilia.widgets.demo.Demos.Demo;
import com.antilia.widgets.demo.account.DemosAuthorization;
import com.antilia.widgets.demo.account.SignInPage;
import com.antilia.widgets.demo.register.RegisterPage;

/**
 * The public part of the application: what the components are and do, with a screenshot of each
 * demo and the frameworks they run in. The demos themselves are for the visitors who signed in,
 * see {@link DemosAuthorization}.
 */
public class HomePage extends BasePage {
	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		super(parameters);

		Label webSocketStatus = new Label("webSocketStatus", new ResourceModel("connecting"));
		webSocketStatus.setOutputMarkupId(true);
		add(webSocketStatus);

		add(new DemoWebSocketBehavior() {
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
					webSocketStatus.setDefaultModel(new ResourceModel("live"));
					webSocketStatus.add(AttributeModifier.append("class", "ws-status--live"));
					handler.add(webSocketStatus);
				}
			}
		});

		add(newSignInHint("signInHint"));

		add(new ListView<>("demos", Demos.all()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item) {
				Demo demo = item.getModelObject();
				BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", demo.page());
				item.add(link);
				link.add(new ContextImage("image", demo.imagePath())
					.add(AttributeModifier.replace("alt", new ResourceModel(demo.nameKey()))));
				link.add(new Label("category", new ResourceModel(demo.categoryKey())));
				link.add(DemoPage.newVisibilityBadge("visibility", demo));
				link.add(new Label("name", new ResourceModel(demo.nameKey())));
				link.add(new Label("description", new ResourceModel(demo.descriptionKey())));
				link.add(new WebMarkupContainer("react").setVisible(demo.frameworkApps()));
				link.add(new WebMarkupContainer("svelte").setVisible(demo.frameworkApps()));
			}
		});

		add(new ListView<>("support", Demos.all()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item) {
				Demo demo = item.getModelObject();
				item.add(new Label("name", new ResourceModel(demo.nameKey())));
				item.add(new Label("category", new ResourceModel(demo.categoryKey())));
				item.add(DemoPage.newVisibilityBadge("visibility", demo));
				item.add(newSupported("wicket", true));
				item.add(newSupported("react", demo.frameworkApps()));
				item.add(newSupported("svelte", demo.frameworkApps()));
				item.add(newSupported("themes", true));
			}
		});
	}

	/**
	 * @return the invitation to sign in or register to try the demos, shown to visitors who have
	 *         not signed in while the demos are private
	 */
	private static Component newSignInHint(String id) {
		WebMarkupContainer hint = new WebMarkupContainer(id) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!DemoSession.get().isSignedIn() && !DemosAuthorization.isOpen());
			}
		};
		hint.add(new BookmarkablePageLink<>("signIn", SignInPage.class));
		hint.add(new BookmarkablePageLink<>("register", RegisterPage.class));
		return hint;
	}

	/**
	 * @return a check mark or a dash telling whether a demo supports something, with the words for
	 *         screen readers
	 */
	private static Component newSupported(String id, boolean supported) {
		return new Label(id, new ResourceModel(supported ? "supported" : "notSupported"))
			.add(AttributeModifier.replace("class", supported ? "support support--yes"
				: "support support--no"));
	}

	private static class ConnectedPushMessage implements IWebSocketPushMessage {
	}
}
