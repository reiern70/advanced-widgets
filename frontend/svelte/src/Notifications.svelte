<!--
  Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
  All rights reserved.
-->
<!--
  @component
  The notifications of the core, as toasts in a corner of the window: an element, empty in Svelte,
  the core stacks them in, in the theme it is in. Bound with bind:this, the component shows a
  notification with show(notification) and closes them all with closeAll().
-->
<script lang="ts">
	import { untrack } from 'svelte';
	import { type NotificationHandle, type NotificationOptions, type NotificationPosition,
		Notifications, partClasses } from '@antilia/core';

	import { getLook } from './look.js';

	const look = getLook();

	type Props = {
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
		class?: string;
	};

	let { position, timeout, errorTimeout, max, pauseOnHover, closeLabel, label, id,
		class: className = '' }: Props = $props();

	let element = $state<HTMLDivElement>();
	let instance: Notifications | null = null;

	$effect(() => {
		if (!element) {
			return;
		}
		const notifications = new Notifications(element, untrack(() => ({ position, timeout,
			errorTimeout, max, pauseOnHover, closeLabel, label, look })));
		instance = notifications;
		return () => {
			notifications.destroy();
			instance = null;
		};
	});

	$effect(() => {
		const next = { position, timeout, errorTimeout, max, pauseOnHover, closeLabel, label };
		untrack(() => instance?.configure(next));
	});

	/**
	 * Shows a notification.
	 *
	 * @returns the notification shown, which the app may close; a closed one when the
	 *          notifications are not mounted
	 */
	export function show(notification: NotificationOptions): NotificationHandle {
		return instance?.show(notification) ??
			{ element: document.createElement('div'), closed: true, close: () => undefined };
	}

	/** Closes every notification shown. */
	export function closeAll(): void {
		instance?.closeAll();
	}
</script>

<div bind:this={element} {id} class={[partClasses(look, 'notifications'), className].filter(Boolean).join(' ')}></div>
