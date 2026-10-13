/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * Notifications, as toasts: short messages stacked in a corner of the browser window, or in the
 * middle of its top or bottom edge, each with a header, holding an icon, a title and a button
 * closing it, and a body. A notification stays until the user closes it, or goes after a timeout,
 * which a bar along its bottom counts down; the countdown waits while the pointer is over the
 * notification or the focus in it. Errors stay until the user closes them, unless told otherwise.
 * Escape closes the notification that has the focus.
 *
 * A notification has a type: success, error, warning, info, each with its color and icon, or one
 * of the page's own, whose color the class antilia-notification-<type> gives in the custom
 * property --antilia-notification-color, and whose icon, if any, is given as the path of an SVG
 * of 24 by 24. Notifications are in the colors of the theme they are in.
 *
 * new Notifications(element, options) works on an element the page has, which React, Svelte and
 * Wicket render empty: the notifications go into stacks it adds to that element, one for each
 * position, so they are in the theme of the element. Errors and warnings are alerts, the others
 * status messages, which screen readers read out. Nothing is styled inline: the timeout is a CSS
 * custom property set through the CSSOM, so notifications work under a strict Content Security
 * Policy.
 */

import { onRemoving, type Unsubscribe } from './host.js';
import { LOOK_PARTS, lookOf, partClasses, type Look, type LookPart } from './look.js';

/** The types of notification that have a color and an icon of their own; any other is the page's. */
export const NOTIFICATION_TYPES = ['success', 'error', 'warning', 'info'] as const;

/** The type of a notification: one of NOTIFICATION_TYPES, or one of the page's own. */
export type NotificationType = typeof NOTIFICATION_TYPES[number] | (string & {});

/** Where notifications are stacked: a corner of the window, or the middle of its top or bottom. */
export type NotificationPosition = 'top-left' | 'top-center' | 'top-right' | 'bottom-left' |
	'bottom-center' | 'bottom-right';

/** The positions notifications are stacked in. */
export const NOTIFICATION_POSITIONS: readonly NotificationPosition[] = ['top-left', 'top-center',
	'top-right', 'bottom-left', 'bottom-center', 'bottom-right'];

/**
 * Why a notification closed: the user closed it, its timeout ran out, the page closed it, or
 * newer notifications pushed it out of a full stack.
 */
export type NotificationCloseReason = 'user' | 'timeout' | 'page' | 'overflow';

/**
 * A notification to show.
 */
export interface NotificationOptions {
	/** The type, info when left out. */
	type?: NotificationType;
	/** The title, in the header. */
	title?: string;
	/** The body: a text, or the page's own content. */
	body?: string | Node;
	/**
	 * Whether a body given as a string is markup rather than text, false when left out. Only for
	 * markup the page trusts: it goes into the notification as it is.
	 */
	html?: boolean;
	/**
	 * How long the notification stays, in milliseconds; 0 keeps it until the user closes it.
	 * When left out, the error timeout of the notifications for an error, their timeout for the
	 * others.
	 */
	timeout?: number;
	/** Whether the header has a button closing the notification, true when left out. */
	closable?: boolean;
	/** Where the notification is stacked, the position of the notifications when left out. */
	position?: NotificationPosition;
	/**
	 * The icon: the path of an SVG of 24 by 24, false for none. The icon of the type when left
	 * out; the page's own types have none.
	 */
	icon?: string | false;
	/** CSS classes of the notification, next to antilia-notification. */
	className?: string;
	/** Called once the notification closed, with the reason. */
	onClose?: (reason: NotificationCloseReason) => void;
}

/**
 * Where notifications go and how long they stay, unless a notification says otherwise.
 */
export interface NotificationsOptions {
	/** Where notifications are stacked, top-right when left out. */
	position?: NotificationPosition;
	/** The look of the notifications; that of the element they are in when left out (see lookOf). */
	look?: Look;
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
}

/**
 * A notification shown.
 */
export interface NotificationHandle {
	/** The element of the notification. */
	readonly element: HTMLElement;
	/** Whether the notification closed. */
	readonly closed: boolean;
	/** Closes the notification, unless it closed already. */
	close(): void;
}

/** The icons of the types, paths of SVGs of 24 by 24 (Material Icons, Apache License 2.0). */
export const NOTIFICATION_ICONS: Record<typeof NOTIFICATION_TYPES[number], string> = {
	success: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 ' +
		'1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
	error: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-' +
		'4h-2V7h2v6z',
	warning: 'M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z',
	info: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-' +
		'8h-2V7h2v2z'
};

/** The classes of the parts of the notifications and their states. */
export const NOTIFICATION_CLASSES = {
	host: 'antilia-notifications',
	stack: 'antilia-notifications-stack',
	notification: 'antilia-notification',
	header: 'antilia-notification-header',
	icon: 'antilia-notification-icon',
	title: 'antilia-notification-title',
	close: 'antilia-notification-close',
	body: 'antilia-notification-body',
	countdown: 'antilia-notification-countdown',
	timed: 'antilia-notification-timed',
	paused: 'antilia-notification-paused',
	leaving: 'antilia-notification-leaving'
} as const;

/** The custom property holding the timeout of a notification, which its countdown takes. */
export const NOTIFICATION_TIMEOUT_PROPERTY = '--antilia-notification-timeout';

/** How long a notification takes to leave, in milliseconds, as the style sheet animates it. */
export const NOTIFICATION_LEAVE_MS = 200;

const SVG = 'http://www.w3.org/2000/svg';

const DEFAULTS = { position: 'top-right' as NotificationPosition, timeout: 5000, errorTimeout: 0,
	max: 5,
	pauseOnHover: true, closeLabel: 'Close', label: 'Notifications' };

let counter = 0;

/**
 * @returns the type as a class name: lower case, without spaces
 */
function typeClass(type: string): string {
	return NOTIFICATION_CLASSES.notification + '-' +
		type.trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '-');
}

function isPosition(position: unknown): position is NotificationPosition {
	return NOTIFICATION_POSITIONS.includes(position as NotificationPosition);
}

/**
 * A notification shown, with its countdown.
 */
class Shown implements NotificationHandle {
	readonly element: HTMLElement;

	closed = false;

	/** The time left before it goes, in milliseconds, when timed. */
	private remaining: number;

	/** Whether it goes after a timeout. */
	private readonly timed: boolean;

	/** When the countdown last started. */
	private started = 0;

	private timer: ReturnType<typeof setTimeout> | undefined = undefined;

	/** Whether the pointer is over it, and whether the focus is in it. */
	private hovered = false;

	private focused = false;

	constructor(private readonly owner: Notifications, readonly position: NotificationPosition,
		private readonly options: NotificationOptions, timeout: number, closeLabel: string) {
		this.remaining = timeout;
		this.timed = timeout > 0;
		const type = options.type?.trim() || 'info';
		const look = owner.look;
		const element = document.createElement('div');
		// a type of the look's, such as error, has its colors; any other its class alone
		const part = 'notification-' + type as LookPart;
		const known = part in LOOK_PARTS;
		element.className = [partClasses(look, 'notification', known && part),
			!known && typeClass(type), options.className].filter(Boolean).join(' ');
		element.id = 'antilia-notification-' + ++counter;
		const urgent = type === 'error' || type === 'warning';
		element.setAttribute('role', urgent ? 'alert' : 'status');
		element.setAttribute('aria-atomic', 'true');
		element.dataset.type = type;

		const header = document.createElement('div');
		header.className = partClasses(look, 'notification-header');
		const icon = options.icon === undefined ?
			NOTIFICATION_ICONS[type as keyof typeof NOTIFICATION_ICONS] : options.icon;
		if (icon) {
			header.append(this.icon(icon, partClasses(look, 'notification-icon')));
		}
		const title = document.createElement('span');
		title.className = partClasses(look, 'notification-title');
		title.id = element.id + '-title';
		title.textContent = options.title ?? '';
		header.append(title);
		if (options.title) {
			element.setAttribute('aria-labelledby', title.id);
		}
		if (options.closable !== false) {
			const close = document.createElement('button');
			close.type = 'button';
			close.className = partClasses(look, 'notification-close');
			close.title = closeLabel;
			close.setAttribute('aria-label', closeLabel);
			close.textContent = '×';
			close.addEventListener('click', () => this.dismiss('user'));
			header.append(close);
		}
		element.append(header);

		if (options.body !== undefined && options.body !== '') {
			const body = document.createElement('div');
			body.className = partClasses(look, 'notification-body');
			if (typeof options.body === 'string' && options.html) {
				body.innerHTML = options.body;
			} else if (typeof options.body === 'string') {
				body.textContent = options.body;
			} else {
				body.append(options.body);
			}
			element.append(body);
		}

		if (timeout > 0) {
			element.classList.add(NOTIFICATION_CLASSES.timed);
			element.style.setProperty(NOTIFICATION_TIMEOUT_PROPERTY, timeout + 'ms');
			const countdown = document.createElement('div');
			countdown.className = partClasses(look, 'notification-countdown');
			element.append(countdown);
		}

		element.addEventListener('keydown', (event) => {
			if (event.key === 'Escape' && options.closable !== false) {
				event.preventDefault();
				event.stopPropagation();
				this.dismiss('user');
			}
		});
		element.addEventListener('pointerenter', () => {
			this.hovered = true;
			this.pause();
		});
		element.addEventListener('pointerleave', () => {
			this.hovered = false;
			this.start();
		});
		element.addEventListener('focusin', () => {
			this.focused = true;
			this.pause();
		});
		element.addEventListener('focusout', (event) => {
			if (!element.contains(event.relatedTarget as Node | null)) {
				this.focused = false;
				this.start();
			}
		});
		this.element = element;
	}

	private icon(path: string, classes: string): SVGSVGElement {
		const svg = document.createElementNS(SVG, 'svg');
		svg.setAttribute('class', classes);
		svg.setAttribute('viewBox', '0 0 24 24');
		svg.setAttribute('aria-hidden', 'true');
		svg.setAttribute('focusable', 'false');
		const shape = document.createElementNS(SVG, 'path');
		shape.setAttribute('d', path);
		svg.append(shape);
		return svg;
	}

	/**
	 * Starts the countdown, or goes on with it, unless the pointer is over the notification or
	 * the focus in it.
	 */
	start(): void {
		if (this.closed || !this.timed || this.timer !== undefined || this.focused ||
			(this.hovered && this.owner.pausesOnHover())) {
			return;
		}
		this.element.classList.remove(NOTIFICATION_CLASSES.paused);
		this.started = Date.now();
		this.timer = setTimeout(() => this.dismiss('timeout'), Math.max(0, this.remaining));
	}

	/**
	 * Stops the countdown while the focus is in the notification, or the pointer over it.
	 */
	private pause(): void {
		if (this.timer === undefined || (!this.focused && !this.owner.pausesOnHover())) {
			return;
		}
		clearTimeout(this.timer);
		this.timer = undefined;
		this.remaining -= Date.now() - this.started;
		this.element.classList.add(NOTIFICATION_CLASSES.paused);
	}

	close(): void {
		this.dismiss('page');
	}

	/**
	 * Closes the notification: it leaves, as the style sheet animates it, and is removed.
	 */
	dismiss(reason: NotificationCloseReason): void {
		if (this.closed) {
			return;
		}
		this.closed = true;
		clearTimeout(this.timer);
		this.timer = undefined;
		const element = this.element;
		const hadFocus = element.contains(document.activeElement);
		element.classList.add(NOTIFICATION_CLASSES.leaving);
		element.removeAttribute('role');
		this.owner.forget(this);
		if (hadFocus) {
			this.owner.focusAfter(this);
		}
		setTimeout(() => element.remove(), NOTIFICATION_LEAVE_MS);
		this.options.onClose?.(reason);
	}

	/** Takes the notification away at once, without telling anyone: the page goes. */
	discard(): void {
		this.closed = true;
		clearTimeout(this.timer);
		this.timer = undefined;
		this.element.remove();
	}
}

/**
 * The notifications of a page, or of a part of it.
 */
export class Notifications {
	readonly element: HTMLElement;

	options: NotificationsOptions;

	/** The stacks, by their position. */
	private readonly stacks = new Map<NotificationPosition, HTMLElement>();

	/** The notifications shown, oldest first. */
	private readonly shown: Shown[] = [];

	/** The look of the notifications, whose classes the elements they are made of get. */
	readonly look: Look;

	private stopWatchingRemoval: Unsubscribe | null = null;

	private destroyed = false;

	/**
	 * @param element the element the notifications go into, which the page renders empty
	 * @param options where notifications go, how long they stay and the texts
	 */
	constructor(element: HTMLElement, options: NotificationsOptions = {}) {
		this.element = element;
		this.options = { ...options };
		this.look = options.look ?? lookOf(element);
		element.classList.add(...partClasses(this.look, 'notifications').split(' '));
		this.stopWatchingRemoval = onRemoving(element, () => this.destroy());
	}

	/**
	 * Changes where notifications go, how long they stay and the texts; the notifications shown
	 * stay where they are.
	 *
	 * @param options the options to change, the others as they are
	 */
	configure(options: NotificationsOptions): void {
		this.options = { ...this.options, ...options };
		this.stacks.forEach((stack) => stack.setAttribute('aria-label', this.label()));
		this.element.querySelectorAll<HTMLElement>('.' + NOTIFICATION_CLASSES.close)
			.forEach((close) => {
				close.title = this.closeLabel();
				close.setAttribute('aria-label', this.closeLabel());
			});
	}

	private label(): string {
		return this.options.label ?? DEFAULTS.label;
	}

	private closeLabel(): string {
		return this.options.closeLabel ?? DEFAULTS.closeLabel;
	}

	/** @returns whether the countdown waits while the pointer is over a notification */
	pausesOnHover(): boolean {
		return this.options.pauseOnHover ?? DEFAULTS.pauseOnHover;
	}

	/**
	 * Shows a notification.
	 *
	 * @param notification the type, the title, the body and how long it stays
	 * @returns the notification shown, which the page may close
	 */
	show(notification: NotificationOptions): NotificationHandle {
		const position = isPosition(notification.position) ? notification.position :
			isPosition(this.options.position) ? this.options.position : DEFAULTS.position;
		const error = (notification.type?.trim() || 'info') === 'error';
		const timeout = Math.max(0, notification.timeout ?? (error ?
			this.options.errorTimeout ?? DEFAULTS.errorTimeout :
			this.options.timeout ?? DEFAULTS.timeout)) || 0;
		const shown = new Shown(this, position, notification, timeout, this.closeLabel());
		if (this.destroyed) {
			shown.closed = true;
			return shown;
		}
		const stack = this.stack(position);
		// the newest is next to the edge: on top in the stacks at the top, below at the bottom
		if (position.startsWith('top')) {
			stack.prepend(shown.element);
		} else {
			stack.append(shown.element);
		}
		this.shown.push(shown);
		const max = Math.max(1, this.options.max ?? DEFAULTS.max);
		const inStack = this.shown.filter((other) => other.position === position);
		inStack.slice(0, Math.max(0, inStack.length - max))
			.forEach((old) => old.dismiss('overflow'));
		shown.start();
		return shown;
	}

	/**
	 * @returns the notifications shown, oldest first
	 */
	notifications(): NotificationHandle[] {
		return [...this.shown];
	}

	/**
	 * Closes every notification shown.
	 */
	closeAll(): void {
		[...this.shown].forEach((shown) => shown.dismiss('page'));
	}

	/**
	 * @returns the stack of a position, added the first time a notification goes there
	 */
	private stack(position: NotificationPosition): HTMLElement {
		let stack = this.stacks.get(position);
		if (!stack || !stack.isConnected) {
			stack = document.createElement('section');
			stack.className = partClasses(this.look, 'notification-stack') + ' ' +
				NOTIFICATION_CLASSES.stack + '-' + position;
			stack.setAttribute('aria-label', this.label());
			stack.setAttribute('aria-live', 'polite');
			stack.setAttribute('aria-relevant', 'additions');
			this.element.append(stack);
			this.stacks.set(position, stack);
		}
		return stack;
	}

	/** Forgets a notification that closed. */
	forget(shown: Shown): void {
		const index = this.shown.indexOf(shown);
		if (index >= 0) {
			this.shown.splice(index, 1);
		}
	}

	/**
	 * Gives the focus, which a notification that closed had, to the close button of the next one
	 * in its stack, or of the previous one; takes it away when there is none.
	 */
	focusAfter(shown: Shown): void {
		const all = Array.from(shown.element.parentElement?.children ?? []);
		const index = all.indexOf(shown.element);
		const close = (element: Element) => element.classList.contains(
			NOTIFICATION_CLASSES.leaving) ? null :
			element.querySelector<HTMLElement>('.' + NOTIFICATION_CLASSES.close);
		const target = all.slice(index + 1).map(close).find(Boolean) ??
			all.slice(0, index).reverse().map(close).find(Boolean);
		if (target) {
			target.focus();
		} else if (document.activeElement instanceof HTMLElement) {
			document.activeElement.blur();
		}
	}

	/**
	 * Takes the notifications and their stacks away, at once and without calling their onClose:
	 * the page or the component goes.
	 */
	destroy(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		this.stopWatchingRemoval?.();
		this.stopWatchingRemoval = null;
		this.shown.splice(0).forEach((shown) => shown.discard());
		this.stacks.forEach((stack) => stack.remove());
		this.stacks.clear();
	}
}
