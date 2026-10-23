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

/*
 * What Wicket's ModalDialog does in the browser, for React, Svelte or any page: the markup of the
 * dialog and of its default theme, so the same style sheets show it, and its behaviors. Escape
 * closes the dialog (closeOnEscape), as a click on the overlay around it may (closeOnClick); the
 * dialog takes the focus while open, keeps it, Tab and Shift+Tab going round its controls
 * (trapFocus), and gives it back to the element that had it when it closes. Dialogs opened over
 * another, nested or stacked, take over until they close, as in Wicket. A dialog fits its content,
 * or comes in one of three sizes (size), or as wide as told (width).
 *
 * Beyond Wicket's: a button of the class modal-dialog-close in the header closes the dialog too,
 * as any element of it marked data-modal-close does, a Cancel button of the footer for example;
 * a function asked before the user closes it may keep it open (canClose), when a form in it was
 * modified for example; the header drags the dialog around (draggable) and a double click on it
 * folds the dialog to its header and unfolds it again (foldable), in the browser alone. A body in
 * the wrapper of the scroll bars, <div class="antilia-scrollbar"><div class="modal-dialog-body">,
 * scrolls with the slim scroll bars of the core.
 */

import { lookOf, toggleParts, type Look } from './look.js';
import { Scrollbar, SCROLLBAR_WRAPPER_CLASS, type ScrollbarHandle } from './scrollbar.js';

/** The class of the element wrapping a dialog in the default theme of Wicket's ModalDialog. */
export const MODAL_THEME_CLASS = 'dialog-theme-default';

/** The classes of the parts of a dialog, as in the markup of Wicket's ModalDialog, and its states. */
export const MODAL_CLASSES = {
	overlay: 'modal-dialog-overlay',
	dialog: 'modal-dialog',
	content: 'modal-dialog-content',
	header: 'modal-dialog-header',
	title: 'modal-dialog-title',
	close: 'modal-dialog-close',
	body: 'modal-dialog-body',
	footer: 'modal-dialog-footer',
	draggable: 'modal-dialog-draggable',
	dragging: 'modal-dialog-dragging',
	foldable: 'modal-dialog-foldable',
	folded: 'modal-dialog-folded',
	sized: 'modal-dialog-sized'
} as const;

/** The sizes of a dialog, from the smallest. */
export type ModalSize = 'small' | 'medium' | 'large';

/** The sizes of a dialog, from the smallest. */
export const MODAL_SIZES: readonly ModalSize[] = ['small', 'medium', 'large'];

/**
 * @returns the class giving a dialog a size, modal-dialog-small for example
 */
export function modalSizeClass(size: ModalSize): string {
	return MODAL_CLASSES.dialog + '-' + size;
}

/**
 * The attribute marking the other elements of a dialog that close it as its close button does,
 * asking canClose first: a Cancel button of the footer for example.
 */
export const CLOSE_ATTRIBUTE = 'data-modal-close';

/** The class marking the dialog that traps the focus, the one opened last. */
const TRAP_CLASS = 'modal-dialog-trap';

const FOCUSABLE = 'a[href], area[href], input:not([disabled]), select:not([disabled]), ' +
	'textarea:not([disabled]), button:not([disabled]), iframe, object, embed, [tabindex], ' +
	'[contenteditable]';

/** The controls in a header that neither drag nor fold the dialog. */
const CONTROLS = 'button, a[href], input, select, textarea';

/** The parts of a folded dialog that are hidden. */
const FOLDED_PARTS = '.' + MODAL_CLASSES.folded + ' :is(.' + MODAL_CLASSES.body + ', .' +
	MODAL_CLASSES.footer + ')';

/**
 * How a dialog behaves, as the ModalDialog of Wicket is told to, and beyond.
 */
export interface ModalOptions {
	/**
	 * Called when the user asks to close the dialog, with Escape, a click on the overlay or on
	 * the close button, and canClose lets it.
	 */
	onClose?: () => void;
	/**
	 * Asked before the user closes the dialog; returning false keeps it open, when a form in it
	 * was modified for example (see isModified). The dialog closes always when left out.
	 */
	canClose?: () => boolean;
	/** Whether Escape closes the dialog; true by default, as closeOnEscape(). */
	closeOnEscape?: boolean;
	/** Whether a click on the overlay closes the dialog; false by default, as closeOnClick(). */
	closeOnClick?: boolean;
	/** Whether the dialog keeps the focus; true by default, as trapFocus(). */
	trapFocus?: boolean;
	/** Whether dragging the header moves the dialog; true by default. */
	draggable?: boolean;
	/** Whether a double click on the header folds the dialog to it; true by default. */
	foldable?: boolean;
	/** The size of the dialog; it fits its content when left out. */
	size?: ModalSize;
	/**
	 * The width of the dialog, a CSS length such as '40em' or '600px', never wider than the
	 * browser window; it overrides the width of the size.
	 */
	width?: string;
	/** The look of the dialog; that of the element it is in when left out (see lookOf). */
	look?: Look;
}

/**
 * @returns the elements in a container the user can move the focus to with Tab
 */
export function focusableIn(container: HTMLElement): HTMLElement[] {
	return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE)).filter((element) =>
		element.getAttribute('tabindex') !== '-1' && !element.closest('[hidden], [inert]') &&
		!element.closest(FOLDED_PARTS) &&
		(element.offsetWidth > 0 || element.offsetHeight > 0 || element.getClientRects().length > 0));
}

/**
 * Whether the user changed a field in a container since it was rendered: a text field or text
 * area holds another value than its default one, a check box or radio button is checked unlike
 * its default, an option is selected unlike its default. A canClose function can ask it, to keep
 * a dialog with a modified form open: {@code () => !isModified(dialog) || confirm('Discard?')}.
 *
 * @param container the element holding the fields, a form or a dialog
 */
export function isModified(container: Element): boolean {
	return Array.from(container.querySelectorAll('input, textarea, select')).some(isFieldModified);
}

/**
 * Whether the user changed a field since it was rendered, as {@link isModified} tells for the
 * fields of a container.
 *
 * @param field a text field, a check box, a radio button, a text area or a select
 */
export function isFieldModified(field: Element): boolean {
	if (field instanceof HTMLInputElement) {
		if (field.type === 'checkbox' || field.type === 'radio') {
			return field.checked !== field.defaultChecked;
		}
		return !['button', 'submit', 'reset', 'image', 'hidden', 'file'].includes(field.type) &&
			field.value !== field.defaultValue;
	}
	if (field instanceof HTMLTextAreaElement) {
		return field.value !== field.defaultValue;
	}
	if (field instanceof HTMLSelectElement) {
		const options = Array.from(field.options);
		if (!field.multiple && !options.some((option) => option.defaultSelected)) {
			// a single select without a default starts on its first option
			return field.selectedIndex > 0;
		}
		return options.some((option) => option.selected !== option.defaultSelected);
	}
	return false;
}

/**
 * Puts a field back as it was rendered, undoing what the user changed, and tells the page as the
 * user changing it would, with the events input and change.
 *
 * @param field a field {@link isFieldModified} says was changed
 */
export function resetField(field: Element): void {
	if (field instanceof HTMLInputElement) {
		if (field.type === 'checkbox' || field.type === 'radio') {
			field.checked = field.defaultChecked;
		} else {
			field.value = field.defaultValue;
		}
	} else if (field instanceof HTMLTextAreaElement) {
		field.value = field.defaultValue;
	} else if (field instanceof HTMLSelectElement) {
		const options = Array.from(field.options);
		if (!field.multiple && !options.some((option) => option.defaultSelected)) {
			field.selectedIndex = options.length ? 0 : -1;
		} else {
			options.forEach((option) => {
				option.selected = option.defaultSelected;
			});
		}
	} else {
		return;
	}
	field.dispatchEvent(new Event('input', { bubbles: true }));
	field.dispatchEvent(new Event('change', { bubbles: true }));
}

function clamp(value: number, min: number, max: number): number {
	return Math.min(Math.max(value, min), Math.max(min, max));
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

	/** The look of the dialog, whose classes its parts get. */
	private readonly look: Look;

	/** The element that had the focus when the dialog opened. */
	private readonly opener: Element | null;

	private readonly keyHandler = (event: KeyboardEvent) => this.onKeyDown(event);

	private readonly clickHandler = (event: MouseEvent) => this.onClick(event);

	private readonly focusHandler = () => this.pullFocus();

	private readonly pointerHandler = (event: PointerEvent) => this.onPointerDown(event);

	private readonly doubleClickHandler = (event: MouseEvent) => this.onDoubleClick(event);

	private destroyed = false;

	private scrollbar: ScrollbarHandle | undefined = undefined;

	/**
	 * Gives the body of a dialog, in the wrapper of the scroll bars, its scroll bars; Wicket gives
	 * those of Wicket.Scrollbar, if any.
	 */
	static attachScrollbar: (body: HTMLElement, look?: Look) => ScrollbarHandle | undefined =
		(body, look) => Scrollbar.attach(body, { look });

	/**
	 * @param overlay the overlay, the element of the class modal-dialog-overlay
	 * @param options how the dialog behaves
	 */
	constructor(overlay: HTMLElement, options: ModalOptions = {}) {
		this.overlay = overlay;
		this.dialog = overlay.querySelector<HTMLElement>('.' + MODAL_CLASSES.dialog) ?? overlay;
		this.options = options;
		this.look = options.look ?? lookOf(this.dialog);
		this.opener = document.activeElement;
		this.overlay.addEventListener('keydown', this.keyHandler);
		this.overlay.addEventListener('click', this.clickHandler);
		this.dialog.addEventListener('pointerdown', this.pointerHandler);
		this.dialog.addEventListener('dblclick', this.doubleClickHandler);
		this.dialog.classList.toggle(MODAL_CLASSES.draggable, options.draggable !== false);
		this.dialog.classList.toggle(MODAL_CLASSES.foldable, options.foldable !== false);
		this.resize();
		if (this.trapsFocus()) {
			this.dialog.setAttribute('tabindex', '0');
			ModalDialog.open.forEach((other) => other.dialog.classList.remove(TRAP_CLASS));
			this.dialog.classList.add(TRAP_CLASS);
			document.addEventListener('focusin', this.focusHandler);
		}
		ModalDialog.open.push(this);
		const body = this.body();
		if (body?.parentElement?.classList.contains(SCROLLBAR_WRAPPER_CLASS)) {
			this.scrollbar = ModalDialog.attachScrollbar(body, this.look);
		}
		if (this.trapsFocus()) {
			this.firstFocusable().focus();
		}
	}

	/**
	 * Gives the dialog the classes of its size in its look, and its width.
	 */
	private resize(): void {
		const { size, width } = this.options;
		MODAL_SIZES.forEach((each) => toggleParts(this.dialog, this.look, each === size,
			`dialog-${each}`));
		this.dialog.classList.toggle(MODAL_CLASSES.sized, size !== undefined || !!width);
		this.dialog.style.width = width ?? '';
	}

	/**
	 * @returns the body of this dialog, not of one nested in it, if it has one
	 */
	private body(): HTMLElement | undefined {
		return Array.from(this.dialog.querySelectorAll<HTMLElement>('.' + MODAL_CLASSES.body))
			.find((body) => body.closest('.' + MODAL_CLASSES.dialog) === this.dialog);
	}

	private trapsFocus(): boolean {
		return this.options.trapFocus !== false;
	}

	private isOnTop(): boolean {
		return ModalDialog.open[ModalDialog.open.length - 1] === this;
	}

	/**
	 * @returns the control to focus first: the first one but the close button, which comes first
	 *          only when the dialog has no other; the dialog when it has none
	 */
	private firstFocusable(): HTMLElement {
		const focusable = focusableIn(this.dialog);
		return focusable.find((element) => !element.classList.contains(MODAL_CLASSES.close)) ??
			focusable[0] ?? this.dialog;
	}

	/**
	 * @returns the element of the class an event happened in, if it is a part of this dialog,
	 *          not of one nested in it
	 */
	private own(target: EventTarget | null, className: string): HTMLElement | null {
		const element = target instanceof Element ?
			target.closest<HTMLElement>('.' + className) : null;
		return element && element.closest('.' + MODAL_CLASSES.dialog) === this.dialog ?
			element : null;
	}

	/**
	 * @returns the element marked data-modal-close of this dialog an event happened in, such as a
	 *          Cancel button of its footer
	 */
	private ownCloser(target: EventTarget | null): HTMLElement | null {
		const element = target instanceof Element ?
			target.closest<HTMLElement>('[' + CLOSE_ATTRIBUTE + ']') : null;
		return element && element.closest('.' + MODAL_CLASSES.dialog) === this.dialog ?
			element : null;
	}

	/**
	 * Asks to close the dialog, as Escape and the close button do: calls onClose unless canClose
	 * says no.
	 *
	 * @returns whether onClose was called
	 */
	requestClose(): boolean {
		if (this.destroyed || this.options.canClose?.() === false) {
			return false;
		}
		this.options.onClose?.();
		return true;
	}

	/**
	 * @returns whether the dialog is folded to its header
	 */
	isFolded(): boolean {
		return this.dialog.classList.contains(MODAL_CLASSES.folded);
	}

	/**
	 * Folds the dialog to its header, or unfolds it. The focus in a part folded goes to the
	 * dialog.
	 *
	 * @param folded whether the dialog is folded; the opposite of now when left out
	 */
	fold(folded = !this.isFolded()): void {
		this.dialog.classList.toggle(MODAL_CLASSES.folded, folded);
		this.dialog.querySelector('.' + MODAL_CLASSES.header)?.setAttribute('aria-expanded',
			String(!folded));
		const active = document.activeElement;
		if (folded && active instanceof HTMLElement && active.closest(FOLDED_PARTS)) {
			if (!this.dialog.hasAttribute('tabindex')) {
				this.dialog.setAttribute('tabindex', '-1');
			}
			this.dialog.focus();
		}
	}

	/**
	 * Moves the dialog by a distance from where it is, keeping its header inside the browser
	 * window. The dialog is placed by its left and top, not transformed, so the fixed overlay of
	 * a dialog nested in it still covers the whole window.
	 *
	 * @param dx the horizontal distance, in pixels
	 * @param dy the vertical distance, in pixels
	 */
	moveBy(dx: number, dy: number): void {
		const rect = this.dialog.getBoundingClientRect();
		const header = this.dialog.querySelector('.' + MODAL_CLASSES.header);
		const headerHeight = header ? header.getBoundingClientRect().bottom - rect.top :
			rect.height;
		const root = document.compatMode === 'BackCompat' ? document.body :
			document.documentElement;
		const x = clamp(rect.left + dx, 0, root.clientWidth - rect.width);
		const y = clamp(rect.top + dy, 0, root.clientHeight - headerHeight);
		// left and top place the margin box, inside the overlay
		const origin = this.dialog.offsetParent?.getBoundingClientRect() ?? { left: 0, top: 0 };
		const style = getComputedStyle(this.dialog);
		this.dialog.style.left = (x - origin.left - (parseFloat(style.marginLeft) || 0)) + 'px';
		this.dialog.style.top = (y - origin.top - (parseFloat(style.marginTop) || 0)) + 'px';
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
			this.requestClose();
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
	 * A click on the overlay itself, around the dialog, closes it if told to; one on the close
	 * button of the dialog closes it.
	 */
	private onClick(event: MouseEvent): void {
		if (event.target === this.overlay && this.isOnTop() && this.options.closeOnClick) {
			event.stopPropagation();
			this.requestClose();
		} else if (this.own(event.target, MODAL_CLASSES.close) ?? this.ownCloser(event.target)) {
			event.preventDefault();
			event.stopPropagation();
			this.requestClose();
		}
	}

	/**
	 * A press on the header, not on a control in it, starts dragging the dialog.
	 */
	private onPointerDown(event: PointerEvent): void {
		const header = this.own(event.target, MODAL_CLASSES.header);
		if (!header || event.button !== 0 || this.options.draggable === false ||
			(event.target as Element).closest(CONTROLS)) {
			return;
		}
		let lastX = event.clientX;
		let lastY = event.clientY;
		header.setPointerCapture?.(event.pointerId);
		this.dialog.classList.add(MODAL_CLASSES.dragging);

		const move = (moveEvent: PointerEvent) => {
			this.moveBy(moveEvent.clientX - lastX, moveEvent.clientY - lastY);
			lastX = moveEvent.clientX;
			lastY = moveEvent.clientY;
		};
		const end = () => {
			this.dialog.classList.remove(MODAL_CLASSES.dragging);
			header.removeEventListener('pointermove', move);
			header.removeEventListener('pointerup', end);
			header.removeEventListener('pointercancel', end);
			header.removeEventListener('lostpointercapture', end);
		};
		header.addEventListener('pointermove', move);
		header.addEventListener('pointerup', end);
		header.addEventListener('pointercancel', end);
		header.addEventListener('lostpointercapture', end);
	}

	/**
	 * A double click on the header, not on a control in it, folds the dialog or unfolds it.
	 */
	private onDoubleClick(event: MouseEvent): void {
		if (this.options.foldable === false || !this.own(event.target, MODAL_CLASSES.header) ||
			(event.target as Element).closest(CONTROLS)) {
			return;
		}
		event.preventDefault();
		window.getSelection?.()?.removeAllRanges();
		this.fold();
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
			this.firstFocusable().focus();
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
		this.scrollbar?.destroy();
		this.overlay.removeEventListener('keydown', this.keyHandler);
		this.overlay.removeEventListener('click', this.clickHandler);
		this.dialog.removeEventListener('pointerdown', this.pointerHandler);
		this.dialog.removeEventListener('dblclick', this.doubleClickHandler);
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
