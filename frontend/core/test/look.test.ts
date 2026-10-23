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

import { afterEach, describe, expect, it } from 'vitest';

import { ANTILIA_LOOK, BOOTSTRAP_LOOK, buttonClasses, composeLook, DESKTOP_CLASSES, Dropdown,
	LOOK_PARTS, lookClasses, lookNamed, lookOf, MODAL_CLASSES, NOTIFICATION_CLASSES, partClasses,
	registerLook, toggleParts } from '../src/index.js';

afterEach(() => {
	document.body.innerHTML = '';
});

describe('looks', () => {
	it('gives a button the classes of Bootstrap in the Bootstrap look', () => {
		expect(buttonClasses({ look: BOOTSTRAP_LOOK })).toBe('btn btn-secondary');
		expect(buttonClasses({ look: BOOTSTRAP_LOOK, variant: 'primary', size: 'large' }))
			.toBe('btn btn-primary btn-lg');
		expect(buttonClasses({ look: BOOTSTRAP_LOOK, variant: 'ghost', size: 'small',
			iconOnly: true, className: 'mine' })).toBe('btn btn-link btn-sm ant-button--icon-only mine');
		expect(buttonClasses({ look: ANTILIA_LOOK, variant: 'danger' }))
			.toBe('ant-button ant-button--danger ant-button--medium');
	});

	it('finds the look an element is in, the Antilia look outside any', () => {
		document.body.innerHTML = '<div data-antilia-look="bootstrap"><p><span id="in"></span></p>' +
			'<div data-antilia-look="antilia"><span id="nested"></span></div></div>' +
			'<span id="out"></span><span id="unknown" data-antilia-look="bulma"></span>';

		expect(lookOf(document.getElementById('in'))).toBe(BOOTSTRAP_LOOK);
		expect(lookOf(document.getElementById('nested'))).toBe(ANTILIA_LOOK);
		expect(lookOf(document.getElementById('out'))).toBe(ANTILIA_LOOK);
		expect(lookOf(document.getElementById('unknown'))).toBe(ANTILIA_LOOK);
		expect(lookOf(null)).toBe(ANTILIA_LOOK);
	});

	it('knows the looks registered, under their names', () => {
		const bulma = { name: 'bulma', classes: { 'button': 'button', 'button-primary': 'is-primary' } };
		registerLook(bulma);

		expect(lookNamed('bulma')).toBe(bulma);
		expect(lookClasses(bulma, 'button', 'button-primary', 'button-medium', false))
			.toBe('button is-primary');
	});

	it('adds the classes of a look to those of the element', () => {
		expect(partClasses(ANTILIA_LOOK, 'dialog-header')).toBe('modal-dialog-header');
		expect(partClasses(BOOTSTRAP_LOOK, 'dialog-header')).toBe('modal-dialog-header modal-header');
		expect(partClasses(BOOTSTRAP_LOOK, 'table-button', 'table-button-primary', false))
			.toBe('ant-data-table-button btn btn-sm ant-data-table-button-primary btn-primary');
		expect(partClasses(BOOTSTRAP_LOOK, 'table-page-link')).toBe('page-link');
	});

	it('names in the Antilia look the classes the scripts find the elements by', () => {
		const own = ANTILIA_LOOK.classes;
		expect([own['dialog'], own['dialog-header'], own['dialog-title'], own['dialog-close'],
			own['dialog-body'], own['dialog-footer']]).toEqual([MODAL_CLASSES.dialog,
			MODAL_CLASSES.header, MODAL_CLASSES.title, MODAL_CLASSES.close, MODAL_CLASSES.body,
			MODAL_CLASSES.footer]);
		expect([own['notification'], own['notification-header'], own['notification-close']])
			.toEqual([NOTIFICATION_CLASSES.notification, NOTIFICATION_CLASSES.header,
				NOTIFICATION_CLASSES.close]);
		expect([own['window'], own['desktop-taskbar'], own['desktop-menu-item']]).toEqual([
			DESKTOP_CLASSES.window, DESKTOP_CLASSES.taskbar, DESKTOP_CLASSES.menuItem]);
		// every part is of an aspect, and every look's part is one of the table
		for (const look of [ANTILIA_LOOK, BOOTSTRAP_LOOK]) {
			for (const part of Object.keys(look.classes)) {
				expect(LOOK_PARTS).toHaveProperty([part]);
			}
		}
	});

	it('makes a look of others, an aspect from each', () => {
		const mixed = composeLook(BOOTSTRAP_LOOK, { color: ANTILIA_LOOK, size: ANTILIA_LOOK });

		expect(mixed.name).toBe('bootstrap color:antilia size:antilia');
		expect(buttonClasses({ look: mixed, variant: 'primary', size: 'small' }))
			.toBe('btn ant-button--primary ant-button--small');
		expect(partClasses(mixed, 'notification', 'notification-error'))
			.toBe('antilia-notification alert antilia-notification-error');
		expect(composeLook(BOOTSTRAP_LOOK, { color: BOOTSTRAP_LOOK })).toBe(BOOTSTRAP_LOOK);

		document.body.innerHTML = '<div data-antilia-look="bootstrap color:antilia size:antilia">' +
			'<span id="in"></span></div>';
		const found = lookOf(document.getElementById('in'));
		expect(found.name).toBe(mixed.name);
		expect(found.classes).toEqual(mixed.classes);
		expect(lookOf(document.getElementById('in'))).toBe(found);
		expect(lookNamed('bootstrap color:unknown').classes).toEqual(BOOTSTRAP_LOOK.classes);
	});

	it('gives an element a state of a look and takes it away', () => {
		const row = document.createElement('tr');

		toggleParts(row, BOOTSTRAP_LOOK, true, 'table-row-selected');
		expect(row.className).toBe('selected table-active');
		toggleParts(row, BOOTSTRAP_LOOK, false, 'table-row-selected');
		expect(row.className).toBe('');
	});

	it('draws a dropdown with the classes of the look it is in', () => {
		document.body.innerHTML = '<div data-antilia-look="bootstrap"><select id="s">' +
			'<option value="a">A</option><option value="b">B</option></select></div>';

		new Dropdown(document.getElementById('s') as HTMLSelectElement);

		expect(document.querySelector('.antilia-dropdown-toggle')!.className)
			.toBe('antilia-dropdown-toggle form-select');
		expect(document.querySelector('.antilia-dropdown-menu')!.className)
			.toBe('antilia-dropdown-menu dropdown-menu');
		expect(document.querySelector('.antilia-dropdown-option')!.className)
			.toBe('antilia-dropdown-option dropdown-item');
	});
});
