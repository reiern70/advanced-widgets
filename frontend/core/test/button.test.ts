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
