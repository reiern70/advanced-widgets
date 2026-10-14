/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it } from 'vitest';

import { BUTTON_BUSY_CLASS, guardBusyButtons, isButtonBusy, setButtonBusy } from '../src/index.js';

let removeGuard: (() => void) | undefined;

afterEach(() => {
	removeGuard?.();
	removeGuard = undefined;
	document.body.innerHTML = '';
});

function button(): HTMLButtonElement {
	document.body.innerHTML = '<button type="button" class="btn btn-primary">' +
		'<span class="ant-button__icon"></span><span class="ant-button__label">Save</span></button>';
	return document.querySelector('button')!;
}

describe('busy buttons', () => {
	it('marks a button busy for the eye and for screen readers, and done again', () => {
		const save = button();

		setButtonBusy(save, true);
		expect(save.classList.contains(BUTTON_BUSY_CLASS)).toBe(true);
		expect(save.getAttribute('aria-busy')).toBe('true');
		expect(save.classList.contains('btn-primary')).toBe(true);
		expect(isButtonBusy(save)).toBe(true);

		setButtonBusy(save, false);
		expect(save.classList.contains(BUTTON_BUSY_CLASS)).toBe(false);
		expect(save.hasAttribute('aria-busy')).toBe(false);
		expect(isButtonBusy(save)).toBe(false);
		expect(() => setButtonBusy(null, true)).not.toThrow();
	});

	it('stops the clicks on a busy button, on it or on its text, before its listeners', () => {
		const save = button();
		let clicks = 0;
		save.addEventListener('click', () => clicks++);
		removeGuard = guardBusyButtons();
		expect(guardBusyButtons()).toBe(removeGuard);

		save.click();
		expect(clicks).toBe(1);

		setButtonBusy(save, true);
		save.click();
		save.querySelector<HTMLElement>('.ant-button__label')!.click();
		expect(clicks).toBe(1);

		setButtonBusy(save, false);
		save.click();
		expect(clicks).toBe(2);
	});

	it('lets the clicks through once the guard is removed', () => {
		const save = button();
		let clicks = 0;
		save.addEventListener('click', () => clicks++);
		guardBusyButtons()();

		setButtonBusy(save, true);
		save.click();
		expect(clicks).toBe(1);
	});
});
