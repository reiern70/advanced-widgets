/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.demo.theme;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.notification.Notification;
import com.antiliacode.widgets.notification.NotificationType;
import com.antiliacode.widgets.notification.Notifications;

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
