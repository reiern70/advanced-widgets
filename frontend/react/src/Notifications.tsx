/*
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

import { createContext, forwardRef, useContext, useImperativeHandle, useLayoutEffect, useMemo,
	useRef, type ReactNode } from 'react';

import { type NotificationHandle, type NotificationOptions, type NotificationPosition,
	Notifications as CoreNotifications, partClasses } from '@antilia/core';

import { useLook } from './Look.js';

/**
 * What a ref to {@link Notifications}, or {@link useNotifications}, can do.
 */
export interface NotificationsHandle {
	/**
	 * Shows a notification.
	 *
	 * @returns the notification shown, which the app may close; a closed one before the
	 *          notifications are mounted
	 */
	show(notification: NotificationOptions): NotificationHandle;
	/** Closes every notification shown. */
	closeAll(): void;
}

export interface NotificationsProps {
	/** Where notifications are stacked, top-right when left out. */
	position?: NotificationPosition;
	/**
	 * How long notifications stay, in milliseconds, 5000 when left out; 0 keeps them until the
	 * user closes them.
	 */
	timeout?: number;
	/**
	 * How long errors stay, in milliseconds, 0 when left out: errors stay until the user closes
	 * them.
	 */
	errorTimeout?: number;
	/** How many notifications a stack shows at most, 5 when left out; the oldest go first. */
	max?: number;
	/** Whether the countdown waits while the pointer is over a notification, true when left out. */
	pauseOnHover?: boolean;
	/** The title of the button closing a notification. */
	closeLabel?: string;
	/** The accessible name of the stacks. */
	label?: string;
	id?: string;
	/** CSS classes of the element of the notifications, next to antilia-notifications. */
	className?: string;
}

/**
 * @returns a notification that is not shown, the notifications not being mounted
 */
function notShown(): NotificationHandle {
	return { element: document.createElement('div'), closed: true, close: () => undefined };
}

/**
 * The notifications of the core, as toasts in a corner of the window: an element, empty in React,
 * the core stacks them in, in the theme it is in. A ref gets a {@link NotificationsHandle}, which
 * shows them; {@link NotificationsProvider} gives one to {@link useNotifications} too.
 */
export const Notifications = forwardRef<NotificationsHandle, NotificationsProps>(
	function Notifications(props, ref) {
		const { position, timeout, errorTimeout, max, pauseOnHover, closeLabel, label, id,
			className } = props;
		const element = useRef<HTMLDivElement>(null);
		const instance = useRef<CoreNotifications | null>(null);
		const look = useLook();

		useLayoutEffect(() => {
			const notifications = new CoreNotifications(element.current!, { position, timeout,
				errorTimeout, max, pauseOnHover, closeLabel, label, look });
			instance.current = notifications;
			return () => {
				notifications.destroy();
				instance.current = null;
			};
		}, [look]);

		useLayoutEffect(() => {
			instance.current?.configure({ position, timeout, errorTimeout, max, pauseOnHover,
				closeLabel, label });
		}, [position, timeout, errorTimeout, max, pauseOnHover, closeLabel, label]);

		useImperativeHandle(ref, () => ({
			show: (notification) => instance.current?.show(notification) ?? notShown(),
			closeAll: () => instance.current?.closeAll()
		}), []);

		return <div ref={element} id={id}
			className={[partClasses(look, 'notifications'), className].filter(Boolean).join(' ')} />;
	});

const Context = createContext<NotificationsHandle | null>(null);

export interface NotificationsProviderProps extends NotificationsProps {
	children?: ReactNode;
}

/**
 * Renders its children and {@link Notifications} after them, which {@link useNotifications} in
 * the children shows notifications with.
 */
export function NotificationsProvider({ children, ...props }: NotificationsProviderProps) {
	const ref = useRef<NotificationsHandle>(null);
	const handle = useMemo<NotificationsHandle>(() => ({
		show: (notification) => ref.current?.show(notification) ?? notShown(),
		closeAll: () => ref.current?.closeAll()
	}), []);
	return (
		<Context.Provider value={handle}>
			{children}
			<Notifications ref={ref} {...props} />
		</Context.Provider>
	);
}

/**
 * @returns the notifications of the {@link NotificationsProvider} around the component
 * @throws Error when there is none
 */
export function useNotifications(): NotificationsHandle {
	const handle = useContext(Context);
	if (!handle) {
		throw new Error('useNotifications() needs a NotificationsProvider around the component');
	}
	return handle;
}
