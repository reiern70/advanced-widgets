/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.protocol.ws.api.BaseWebSocketBehavior;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.demo.admin.AccessTimeResource;

/**
 * Connects the page to the {@link LiveResource} and turns the component it is added to, the
 * status, live when the resource pushes back: its text becomes the one of its
 * {@code data-live} attribute. Stateless, unlike a {@code WebSocketBehavior}: the connection
 * goes to the shared resource, not to the page.
 * <p>
 * Like {@link DemoWebSocketBehavior} it sets up the websocket without jQuery.
 *
 * @see AccessTimeResource
 */
public class LiveStatusBehavior extends BaseWebSocketBehavior
{
	private static final long serialVersionUID = 1L;

	public LiveStatusBehavior()
	{
		super(LiveResource.NAME, "");
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		response.render(OnDomReadyHeaderItem.forScript(String.format(
			"Wicket.Event.subscribe(Wicket.Event.Topic.WebSocket.Message, function(event, message) {" +
				"var status = document.getElementById('%s');" +
				"if (status && message === '%s') {" +
				"status.textContent = status.getAttribute('data-live');" +
				"status.classList.add('ws-status--live');" +
				"}});",
			component.getMarkupId(), LiveResource.LIVE)));
	}

	/**
	 * Wicket's websocket needs an HTTP session, which a visitor of the stateless home page does
	 * not have yet: the script of {@link AccessTimeResource} makes one and leaves the promise of
	 * it in {@code window.antiliaSession}. So the websocket connects once the scripts of the page
	 * ran and that promise settled.
	 */
	@Override
	protected String getWebSocketSetupScript(Map<String, Object> parameters)
	{
		String setup = super.getWebSocketSetupScript(parameters).replace("jQuery.extend(",
			"Object.assign(");
		return "setTimeout(function() {" +
			"(window.antiliaSession || Promise.resolve()).then(function() {" + setup + "});" +
			"}, 0);";
	}

	@Override
	protected CharSequence getSessionId(Component component)
	{
		// a page of a session that is not kept yet has no id to put into the URL
		CharSequence sessionId = component.getSession().isTemporary() ? ""
			: super.getSessionId(component);
		return Strings.isEmpty(sessionId) ? "" : sessionId;
	}

	@Override
	public boolean getStatelessHint(Component component)
	{
		return true;
	}
}
