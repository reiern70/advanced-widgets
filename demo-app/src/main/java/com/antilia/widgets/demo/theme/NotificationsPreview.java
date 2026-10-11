/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.theme;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.notification.Notification;
import com.antilia.widgets.notification.NotificationType;
import com.antilia.widgets.notification.Notifications;

/**
 * Notifications of the theme editor's preview: one of each type, staying until closed, in the
 * colors of the edited theme, among them --antilia-theme-success, -error, -warning and -info. They
 * are stacked in the preview rather than in a corner of the window, so that they are next to the
 * pickers; a button shows them again once closed.
 */
class NotificationsPreview extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Notifications notifications;

	NotificationsPreview(String id)
	{
		super(id);
		notifications = new Notifications("notifications").setTimeout(Duration.ZERO)
			.setShowFeedback(false);
		add(notifications);
		add(new AjaxLink<Void>("again")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				notifications.closeAll();
				showAll();
			}
		}.add(new ButtonBehavior()));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		showAll();
	}

	/**
	 * Shows one notification of each type; the newest go on top, so the last is shown first.
	 */
	private void showAll()
	{
		List<NotificationType> types = new ArrayList<>(List.of(NotificationType.values()));
		Collections.reverse(types);
		for (NotificationType type : types)
		{
			notifications.show(Notification.of(type, getString("notification." + type.cssName()))
				.setTitle(getString("Notifications." + type.cssName())));
		}
	}
}
