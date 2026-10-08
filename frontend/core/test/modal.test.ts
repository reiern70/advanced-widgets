/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { ModalDialog } from '../src/index.js';

beforeAll(() => {
	// jsdom lays nothing out: every element counts as shown
	vi.spyOn(HTMLElement.prototype, 'getClientRects').mockReturnValue(
		[{}] as unknown as DOMRectList);
});

afterEach(() => {
	document.body.innerHTML = '';
});

/**
 * @returns the overlay of a dialog holding two fields, added to the page
 */
function dialog(id: string, parent: HTMLElement = document.body): HTMLElement {
	const theme = document.createElement('div');
	theme.className = 'dialog-theme-default';
	theme.innerHTML = `<div class="modal-dialog-overlay" id="${id}"><div class="modal-dialog"
		role="dialog"><div class="modal-dialog-content"><input id="${id}-first"><button
		id="${id}-last">OK</button></div></div></div>`;
	parent.appendChild(theme);
	return theme.querySelector<HTMLElement>('.modal-dialog-overlay')!;
}

function key(target: Element, name: string, shiftKey = false): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key: name, shiftKey, bubbles: true,
		cancelable: true });
	target.dispatchEvent(event);
	return event;
}

describe('ModalDialog', () => {
	it('takes the focus, keeps it going round its controls and gives it back', () => {
		document.body.innerHTML = '<button id="opener">Open</button>';
		const opener = document.getElementById('opener')!;
		opener.focus();
		const modal = new ModalDialog(dialog('a'));

		expect(document.activeElement!.id).toBe('a-first');
		document.getElementById('a-last')!.focus();
		expect(key(document.activeElement!, 'Tab').defaultPrevented).toBe(true);
		expect(document.activeElement!.id).toBe('a-first');
		key(document.activeElement!, 'Tab', true);
		expect(document.activeElement!.id).toBe('a-last');

		opener.focus();
		expect(document.activeElement!.id).toBe('a-first');

		modal.destroy();
		expect(document.activeElement).toBe(opener);
	});

	it('closes on Escape, and on a click on the overlay when told to', () => {
		const onClose = vi.fn();
		const overlay = dialog('a');
		new ModalDialog(overlay, { onClose });

		overlay.click();
		expect(onClose).not.toHaveBeenCalled();
		key(document.getElementById('a-first')!, 'Escape');
		expect(onClose).toHaveBeenCalledTimes(1);

		const clicked = vi.fn();
		const other = dialog('b');
		new ModalDialog(other, { onClose: clicked, closeOnClick: true, closeOnEscape: false });
		key(document.getElementById('b-first')!, 'Escape');
		other.querySelector<HTMLElement>('.modal-dialog')!.click();
		expect(clicked).not.toHaveBeenCalled();
		other.click();
		expect(clicked).toHaveBeenCalledTimes(1);
	});

	it('lets the dialog on top handle the keys, nested in another', () => {
		const outerClose = vi.fn();
		const innerClose = vi.fn();
		const outer = dialog('outer');
		const outerModal = new ModalDialog(outer, { onClose: outerClose });
		const inner = dialog('inner', outer.querySelector<HTMLElement>('.modal-dialog-content')!);
		const innerModal = new ModalDialog(inner, { onClose: innerClose });

		expect(document.activeElement!.id).toBe('inner-first');
		key(document.activeElement!, 'Escape');
		expect(innerClose).toHaveBeenCalledTimes(1);
		expect(outerClose).not.toHaveBeenCalled();

		inner.parentElement!.remove();
		innerModal.destroy();
		key(document.getElementById('outer-first')!, 'Escape');
		expect(outerClose).toHaveBeenCalledTimes(1);
		outerModal.destroy();
	});

	it('leaves the focus alone without a trap', () => {
		document.body.innerHTML = '<button id="opener">Open</button>';
		document.getElementById('opener')!.focus();
		const modal = new ModalDialog(dialog('a'), { trapFocus: false });

		expect(document.activeElement!.id).toBe('opener');
		modal.destroy();
	});
});
