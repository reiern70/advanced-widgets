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
package com.antiliacode.widgets.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.util.lang.Args;

/**
 * Notifications kept in the session until {@link Notifications} show them: on the next page of the
 * session that renders notifications, also after a redirect, or via Ajax in the next Ajax request
 * of a page with notifications. They need no component at hand, as the feedback messages of the
 * session, which become notifications too, but carry all a {@link Notification} has: a title, a
 * timeout, a position.
 *
 * <pre>
 * SessionNotifications.show(Notification.success("You are signed in.").setTitle("GitHub"));
 * throw new RestartResponseException(HomePage.class);
 * </pre>
 *
 * A temporary session, of stateless pages, is bound, so that the notifications last until the
 * next request.
 *
 * @since 11.0.0
 */
public final class SessionNotifications
{
	private static final MetaDataKey<ArrayList<Notification>> PENDING = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private SessionNotifications()
	{
	}

	/**
	 * Shows a notification on the next page of the session that renders notifications, or via Ajax
	 * in the next Ajax request of a page with notifications.
	 *
	 * @param notification
	 *            the notification
	 */
	public static void show(Notification notification)
	{
		Args.notNull(notification, "notification");
		Session session = Session.get();
		ArrayList<Notification> pending = session.getMetaData(PENDING);
		ArrayList<Notification> changed = pending != null ? new ArrayList<>(pending)
			: new ArrayList<>();
		changed.add(notification);
		if (session.isTemporary())
		{
			session.bind();
		}
		session.setMetaData(PENDING, changed);
	}

	/**
	 * @return the notifications of the session not shown yet, none outside a request
	 */
	public static List<Notification> getPending()
	{
		ArrayList<Notification> pending = Session.exists() ? Session.get().getMetaData(PENDING)
			: null;
		return pending != null ? List.copyOf(pending) : List.of();
	}

	/**
	 * @return the notifications of the session not shown yet, which the session forgets
	 */
	static List<Notification> take()
	{
		List<Notification> pending = getPending();
		if (!pending.isEmpty())
		{
			Session.get().setMetaData(PENDING, null);
		}
		return pending;
	}
}
