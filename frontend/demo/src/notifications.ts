/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Notifications demo: a form choosing the type, the title, the message, text or HTML, the
 * position and the timeout of a notification, and showing it; and a small form whose save says
 * how it went with a notification, as the feedback messages of the Wicket page do.
 */

import type { NotificationOptions, NotificationPosition } from '@antilia/core';

import { format, type DropdownTexts } from './data.js';

/** The types the demo shows: the four of the component and one of the page's own, note. */
export const DEMO_TYPES = ['success', 'error', 'warning', 'info', 'note'] as const;

export type DemoType = typeof DEMO_TYPES[number];

/**
 * The timeouts the demo offers, in milliseconds: -1 for the one of the notifications, 5 seconds and
 * errors until closed; 0 keeps a notification until it is closed.
 */
export const DEMO_TIMEOUTS = [-1, 3000, 5000, 10000, 0] as const;

/** The icon of the page's own type, note: a light bulb (Material Icons, Apache License 2.0). */
export const NOTE_ICON = 'M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 ' +
	'2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 ' +
	'0-3.86-3.14-7-7-7z';

/** The texts of the demo, by the keys of the page's bundle. */
export interface NotificationsDemoTexts extends DropdownTexts {
	showTitle: string;
	feedbackTitle: string;
	type: string;
	title: string;
	message: string;
	/** The label of the checkbox saying the message is HTML. */
	html: string;
	position: string;
	timeout: string;
	show: string;
	closeAll: string;
	name: string;
	save: string;
	/** The error when the name is left empty: {0} the label of the field. */
	required: string;
	/** The message of a save: {0} the name. */
	saved: string;
	/** The names of the types, which are the titles of the feedback messages too. */
	types: Record<DemoType, string>;
	positions: Record<NotificationPosition, string>;
	/** The names of the timeouts, by their milliseconds. */
	timeouts: Record<string, string>;
	/** The title of the button closing a notification. */
	close: string;
	/** The accessible name of the stacks of notifications. */
	label: string;
}

/** What the page gives the demo. */
export interface NotificationsConfig {
	texts: NotificationsDemoTexts;
	type: DemoType;
	title: string;
	message: string;
	html: boolean;
	position: NotificationPosition;
	timeout: number;
}

/** What the form of the demo says to show. */
export interface DemoNotification {
	type: DemoType;
	title: string;
	message: string;
	/** Whether the message is HTML. */
	html: boolean;
	position: NotificationPosition;
	/** The timeout, -1 for the one of the notifications. */
	timeout: number;
}

/**
 * @returns the notification the form says to show
 */
export function notification(form: DemoNotification): NotificationOptions {
	return { type: form.type, title: form.title, body: form.message, html: form.html,
		position: form.position, timeout: form.timeout < 0 ? undefined : form.timeout,
		icon: form.type === 'note' ? NOTE_ICON : undefined };
}

/**
 * @returns the notification saving the name gives: an error when it is empty, as the required
 *          field of the Wicket page says, a success otherwise
 */
export function saved(texts: NotificationsDemoTexts, name: string): NotificationOptions {
	return name.trim() ?
		{ type: 'success', title: texts.types.success, body: format(texts.saved, name.trim()) } :
		{ type: 'error', title: texts.types.error, body: format(texts.required, texts.name) };
}
