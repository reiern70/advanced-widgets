/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * What Wicket's ModalDialog does in the browser, for React, Svelte or any page: the markup of the
 * dialog and of its default theme, so the same style sheets show it, and its behaviors. Escape
 * closes the dialog (closeOnEscape), as a click on the overlay around it may (closeOnClick); the
 * dialog takes the focus while open, keeps it, Tab and Shift+Tab going round its controls
 * (trapFocus), and gives it back to the element that had it when it closes. Dialogs opened over
 * another, nested or stacked, take over until they close, as in Wicket.
 */

/** The class of the element wrapping a dialog in the default theme of Wicket's ModalDialog. */
export const MODAL_THEME_CLASS = 'dialog-theme-default';

/** The classes of the parts of a dialog, as in the markup of Wicket's ModalDialog. */
export const MODAL_CLASSES = {
	overlay: 'modal-dialog-overlay',
	dialog: 'modal-dialog',
	content: 'modal-dialog-content',
	header: 'modal-dialog-header',
	body: 'modal-dialog-body',
	footer: 'modal-dialog-footer'
} as const;

/** The class marking the dialog that traps the focus, the one opened last. */
const TRAP_CLASS = 'modal-dialog-trap';

const FOCUSABLE = 'a[href], area[href], input:not([disabled]), select:not([disabled]), ' +
	'textarea:not([disabled]), button:not([disabled]), iframe, object, embed, [tabindex], ' +
	'[contenteditable]';

/**
 * How a dialog behaves, as the ModalDialog of Wicket is told to.
 */
export interface ModalOptions {
	/** Called when the user asks to close the dialog: Escape, or a click on the overlay. */
	onClose?: () => void;
	/** Whether Escape closes the dialog; true by default, as closeOnEscape(). */
	closeOnEscape?: boolean;
	/** Whether a click on the overlay closes the dialog; false by default, as closeOnClick(). */
	closeOnClick?: boolean;
	/** Whether the dialog keeps the focus; true by default, as trapFocus(). */
	trapFocus?: boolean;
}

/**
 * @returns the elements in a container the user can move the focus to with Tab
 */
export function focusableIn(container: HTMLElement): HTMLElement[] {
	return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE)).filter((element) =>
		element.getAttribute('tabindex') !== '-1' && !element.closest('[hidden], [inert]') &&
		(element.offsetWidth > 0 || element.offsetHeight > 0 || element.getClientRects().length > 0));
}

/**
 * The behaviors of an open dialog. Create it once the markup is in the page; destroy it when the
 * dialog closes, which gives the focus back.
 */
export class ModalDialog {
	/** The dialogs open, the last one on top. */
	private static readonly open: ModalDialog[] = [];

	readonly overlay: HTMLElement;

	readonly dialog: HTMLElement;

	private readonly options: ModalOptions;

	/** The element that had the focus when the dialog opened. */
	private readonly opener: Element | null;

	private readonly keyHandler = (event: KeyboardEvent) => this.onKeyDown(event);

	private readonly clickHandler = (event: MouseEvent) => this.onClick(event);

	private readonly focusHandler = () => this.pullFocus();

	private destroyed = false;

	/**
	 * @param overlay the overlay, the element of the class modal-dialog-overlay
	 * @param options how the dialog behaves
	 */
	constructor(overlay: HTMLElement, options: ModalOptions = {}) {
		this.overlay = overlay;
		this.dialog = overlay.querySelector<HTMLElement>('.' + MODAL_CLASSES.dialog) ?? overlay;
		this.options = options;
		this.opener = document.activeElement;
		this.overlay.addEventListener('keydown', this.keyHandler);
		this.overlay.addEventListener('click', this.clickHandler);
		if (this.trapsFocus()) {
			this.dialog.setAttribute('tabindex', '0');
			ModalDialog.open.forEach((other) => other.dialog.classList.remove(TRAP_CLASS));
			this.dialog.classList.add(TRAP_CLASS);
			document.addEventListener('focusin', this.focusHandler);
		}
		ModalDialog.open.push(this);
		if (this.trapsFocus()) {
			(focusableIn(this.dialog)[0] ?? this.dialog).focus();
		}
	}

	private trapsFocus(): boolean {
		return this.options.trapFocus !== false;
	}

	private isOnTop(): boolean {
		return ModalDialog.open[ModalDialog.open.length - 1] === this;
	}

	/**
	 * Escape closes the dialog, unless told otherwise; Tab and Shift+Tab go round its controls.
	 * An event handled stops here, so a dialog around this one does not close too.
	 */
	private onKeyDown(event: KeyboardEvent): void {
		if (!this.isOnTop() || !this.dialog.contains(event.target as Node)) {
			return;
		}
		if (event.key === 'Escape' && this.options.closeOnEscape !== false) {
			event.preventDefault();
			event.stopPropagation();
			this.options.onClose?.();
		} else if (event.key === 'Tab' && this.trapsFocus()) {
			const focusable = focusableIn(this.dialog);
			if (focusable.length === 0) {
				event.preventDefault();
				return;
			}
			const first = focusable[0];
			const last = focusable[focusable.length - 1];
			if (event.shiftKey && (event.target === first || event.target === this.dialog)) {
				event.preventDefault();
				last.focus();
			} else if (!event.shiftKey && (event.target === last || event.target === this.dialog)) {
				event.preventDefault();
				first.focus();
			}
		}
	}

	/**
	 * A click on the overlay itself, around the dialog, closes it if told to.
	 */
	private onClick(event: MouseEvent): void {
		if (event.target === this.overlay && this.isOnTop() && this.options.closeOnClick) {
			event.stopPropagation();
			this.options.onClose?.();
		}
	}

	/**
	 * Brings the focus back into the dialog on top when it went elsewhere.
	 */
	private pullFocus(): void {
		if (!this.isOnTop()) {
			return;
		}
		const active = document.activeElement;
		if (active !== this.dialog && !this.dialog.contains(active)) {
			(focusableIn(this.dialog)[0] ?? this.dialog).focus();
		}
	}

	/**
	 * Stops the behaviors of the dialog, which closed, and gives the focus back to the element
	 * that had it when the dialog opened, or to the dialog below.
	 */
	destroy(): void {
		if (this.destroyed) {
			return;
		}
		this.destroyed = true;
		this.overlay.removeEventListener('keydown', this.keyHandler);
		this.overlay.removeEventListener('click', this.clickHandler);
		document.removeEventListener('focusin', this.focusHandler);
		this.dialog.classList.remove(TRAP_CLASS);
		const index = ModalDialog.open.indexOf(this);
		if (index >= 0) {
			ModalDialog.open.splice(index, 1);
		}
		ModalDialog.open[ModalDialog.open.length - 1]?.dialog.classList.add(TRAP_CLASS);
		if (this.trapsFocus() && this.opener instanceof HTMLElement && this.opener.isConnected) {
			this.opener.focus();
		}
	}
}
