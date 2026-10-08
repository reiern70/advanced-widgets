/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { isModified, ModalDialog } from '../src/index.js';

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

	it('closes with its close button, focusing the body first', () => {
		const onClose = vi.fn();
		const overlay = dialog('a');
		overlay.querySelector('.modal-dialog-content')!.insertAdjacentHTML('afterbegin',
			'<div class="modal-dialog-header"><h1 class="modal-dialog-title">Title</h1>' +
			'<button type="button" class="modal-dialog-close" aria-label="Close">×</button></div>');
		new ModalDialog(overlay, { onClose });

		expect(document.activeElement!.id).toBe('a-first');
		overlay.querySelector<HTMLElement>('.modal-dialog-close')!.click();
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('stays open when canClose says no', () => {
		const onClose = vi.fn();
		let allowed = false;
		const overlay = dialog('a');
		overlay.querySelector('.modal-dialog-content')!.insertAdjacentHTML('afterbegin',
			'<div class="modal-dialog-header"><button class="modal-dialog-close">×</button></div>');
		const modal = new ModalDialog(overlay, { onClose, canClose: () => allowed,
			closeOnClick: true });

		key(document.getElementById('a-first')!, 'Escape');
		overlay.click();
		overlay.querySelector<HTMLElement>('.modal-dialog-close')!.click();
		expect(modal.requestClose()).toBe(false);
		expect(onClose).not.toHaveBeenCalled();

		allowed = true;
		key(document.getElementById('a-first')!, 'Escape');
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('closes with the elements marked data-modal-close, as with the close button', () => {
		const onClose = vi.fn();
		const overlay = dialog('a');
		overlay.querySelector('.modal-dialog-content')!.insertAdjacentHTML('beforeend',
			'<div class="modal-dialog-footer"><button data-modal-close><span>Cancel</span>' +
			'</button></div>');
		new ModalDialog(overlay, { onClose, canClose: () => onClose.mock.calls.length === 0 });

		overlay.querySelector<HTMLElement>('[data-modal-close] span')!.click();
		overlay.querySelector<HTMLElement>('[data-modal-close]')!.click();
		expect(onClose).toHaveBeenCalledTimes(1);
	});

	it('folds to its header on a double click, unless told not to', () => {
		const overlay = dialog('a');
		const content = overlay.querySelector('.modal-dialog-content')!;
		content.innerHTML = '<div class="modal-dialog-header">Title</div><div ' +
			'class="modal-dialog-body"><input id="a-first"></div>';
		const header = content.querySelector('.modal-dialog-header')!;
		const element = overlay.querySelector('.modal-dialog')!;
		new ModalDialog(overlay);
		expect(document.activeElement!.id).toBe('a-first');

		header.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(element.classList.contains('modal-dialog-folded')).toBe(true);
		expect(header.getAttribute('aria-expanded')).toBe('false');
		expect(document.activeElement).toBe(element);
		header.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(element.classList.contains('modal-dialog-folded')).toBe(false);

		const other = dialog('b');
		other.querySelector('.modal-dialog-content')!.insertAdjacentHTML('afterbegin',
			'<div class="modal-dialog-header">Title</div>');
		new ModalDialog(other, { foldable: false });
		other.querySelector('.modal-dialog-header')!.dispatchEvent(
			new MouseEvent('dblclick', { bubbles: true }));
		expect(other.querySelector('.modal-dialog')!.classList.contains('modal-dialog-folded'))
			.toBe(false);
	});

	it('moves when its header is dragged, keeping the header in the window', () => {
		Object.defineProperty(document.documentElement, 'clientWidth', { configurable: true,
			value: 1000 });
		Object.defineProperty(document.documentElement, 'clientHeight', { configurable: true,
			value: 800 });
		const overlay = dialog('a');
		overlay.querySelector('.modal-dialog-content')!.insertAdjacentHTML('afterbegin',
			'<div class="modal-dialog-header">Title</div>');
		const element = overlay.querySelector<HTMLElement>('.modal-dialog')!;
		const header = overlay.querySelector<HTMLElement>('.modal-dialog-header')!;
		element.getBoundingClientRect = () => ({ left: 100, top: 100, width: 200, height: 150,
			right: 300, bottom: 250 }) as DOMRect;
		header.getBoundingClientRect = () => ({ bottom: 130 }) as DOMRect;
		const modal = new ModalDialog(overlay);
		expect(element.classList.contains('modal-dialog-draggable')).toBe(true);

		const pointer = (type: string, x: number, y: number) => {
			const event = new MouseEvent(type, { bubbles: true, clientX: x, clientY: y, button: 0 });
			Object.defineProperty(event, 'pointerId', { value: 1 });
			header.dispatchEvent(event);
		};
		pointer('pointerdown', 10, 10);
		pointer('pointermove', 40, 30);
		pointer('pointerup', 40, 30);
		pointer('pointermove', 90, 90);
		expect(element.style.translate).toBe('30px 20px');

		modal.moveBy(5000, 5000);
		// its box did not move in jsdom: from 100,100 at most by 1000 - 200 - 100 and
		// 800 - 30 - 100, on top of the 30px 20px dragged
		expect(element.style.translate).toBe('730px 690px');
	});

	it('tells whether the fields were modified', () => {
		document.body.innerHTML = '<form><input name="a" value="x"><input type="checkbox">' +
			'<select><option>1</option><option>2</option></select><textarea>t</textarea></form>';
		const form = document.querySelector('form')!;
		expect(isModified(form)).toBe(false);

		form.querySelector('select')!.selectedIndex = 1;
		expect(isModified(form)).toBe(true);
		form.querySelector('select')!.selectedIndex = 0;
		form.querySelector('input')!.value = 'y';
		expect(isModified(form)).toBe(true);
		form.querySelector('input')!.value = 'x';
		form.querySelector<HTMLInputElement>('[type=checkbox]')!.checked = true;
		expect(isModified(form)).toBe(true);
	});

	it('leaves the focus alone without a trap', () => {
		document.body.innerHTML = '<button id="opener">Open</button>';
		document.getElementById('opener')!.focus();
		const modal = new ModalDialog(dialog('a'), { trapFocus: false });

		expect(document.activeElement!.id).toBe('opener');
		modal.destroy();
	});
});
