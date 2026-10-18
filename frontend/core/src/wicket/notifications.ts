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

/*
 * antilia-notifications.js: registers Wicket.Notifications for the Notifications of ui-components,
 * which calls Wicket.Notifications.attach(id, options) each time it renders, then show(id,
 * notification) for each notification, on rendering or via Ajax. The notifications go when an
 * update of the page removes the element.
 */

import { Notifications, type NotificationHandle, type NotificationOptions,
	type NotificationsOptions } from '../notifications.js';
import { useWicketHost, wicket } from './host.js';

const instances = new Map<string, Notifications>();

const WicketNotifications = {

	/**
	 * Makes an element show notifications, again when it was rendered anew.
	 *
	 * @param id the markup id of the element
	 * @param options where notifications go, how long they stay and the texts
	 */
	attach(id: string, options: NotificationsOptions): void {
		const element = document.getElementById(id);
		const current = instances.get(id);
		if (current?.element === element) {
			current.configure(options);
			return;
		}
		current?.destroy();
		instances.delete(id);
		if (element) {
			instances.set(id, new Notifications(element, options));
		}
	},

	/**
	 * Shows a notification.
	 *
	 * @param id the markup id of the element of the notifications
	 * @param notification the type, the title, the body and how long it stays
	 * @returns the notification shown, nothing when the element is not in the page
	 */
	show(id: string, notification: NotificationOptions): NotificationHandle | undefined {
		return WicketNotifications.of(id)?.show(notification);
	},

	/**
	 * Closes every notification of an element.
	 *
	 * @param id the markup id of the element
	 */
	closeAll(id: string): void {
		WicketNotifications.of(id)?.closeAll();
	},

	/**
	 * @param id the markup id of the element
	 * @returns the notifications of the element, if it is in the page
	 */
	of(id: string): Notifications | undefined {
		const notifications = instances.get(id);
		if (notifications && !notifications.element.isConnected) {
			notifications.destroy();
			instances.delete(id);
			return undefined;
		}
		return notifications;
	},

	/**
	 * Takes the notifications of an element away.
	 *
	 * @param id the markup id of the element
	 */
	detach(id: string): void {
		instances.get(id)?.destroy();
		instances.delete(id);
	}
};

const Wicket = wicket();
if (Wicket.Notifications === undefined) {
	useWicketHost();
	Wicket.Notifications = WicketNotifications;
}
