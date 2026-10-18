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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, Menu, menu, MENU_DESIGNS, MENU_X_PROPERTY,
	MENU_Y_PROPERTY } from '../src/index.js';

function key(target: Element, name: string, init: KeyboardEventInit = {}): KeyboardEvent {
	const event = new KeyboardEvent('keydown', { key: name, bubbles: true, cancelable: true,
		...init });
	target.dispatchEvent(event);
	return event;
}

function click(target: Element, detail = 1): MouseEvent {
	const event = new MouseEvent('click', { bubbles: true, cancelable: true, detail });
	target.dispatchEvent(event);
	return event;
}

/** A pointer event of a kind, a mouse's unless told otherwise. */
function pointer(type: string, target: Element, pointerType = 'mouse',
	relatedTarget: Element | null = null): void {
	const event = new MouseEvent(type, { bubbles: true, cancelable: true, relatedTarget });
	Object.defineProperty(event, 'pointerType', { value: pointerType });
	target.dispatchEvent(event);
}

function item(id: string, label: string, tag = 'button', attributes = ''): string {
	return '<' + tag + ' class="CLASS" id="' + id + '" ' + attributes +
		(tag === 'a' ? ' href="#' + id + '"' : '') + '><span class="antilia-menu-label">' + label +
		'</span></' + tag + '>';
}

function barItem(content: string): string {
	return '<li class="antilia-menu-item">' + content.replace('CLASS', 'antilia-menu-link') + '</li>';
}

function subItem(content: string): string {
	return '<li class="antilia-menu-submenu-item">' +
		content.replace('CLASS', 'antilia-menu-submenu-link') + '</li>';
}

function submenu(id: string, ...items: string[]): string {
	return '<ul class="antilia-menu-submenu" id="' + id + '">' + items.join('') + '</ul>';
}

const SEPARATOR = '<li class="antilia-menu-separator"></li>';

/**
 * @returns the element of a menu: Home, File (New, Export (PDF, CSV), Close), Edit (Undo, Paste
 *          disabled) and Help, a separator before Edit
 */
function markup(attributes: Record<string, string> = {}): HTMLElement {
	document.body.innerHTML = '<div id="m"><ul class="antilia-menu-list">' +
		barItem(item('home', 'Home', 'a', attributes.home ?? '')) +
		barItem(item('file', 'File', 'button', attributes.file ?? '') + submenu('file-menu',
			subItem(item('new', 'New')),
			subItem(item('export', 'Export') + submenu('export-menu',
				subItem(item('pdf', 'PDF', 'a', attributes.pdf ?? '')), subItem(item('csv', 'CSV')))),
			SEPARATOR,
			subItem(item('close', 'Close')))) +
		SEPARATOR +
		barItem(item('edit', 'Edit') + submenu('edit-menu', subItem(item('undo', 'Undo')),
			subItem(item('paste', 'Paste', 'button', 'aria-disabled="true"')))) +
		barItem(item('help', 'Help', 'a')) +
		'</ul></div><button id="outside">Outside</button>';
	return document.getElementById('m')!;
}

function byId(id: string): HTMLElement {
	return document.getElementById(id)!;
}

function isOpen(id: string): boolean {
	return byId(id).getAttribute('aria-expanded') === 'true' && !byId(id + '-menu').hidden;
}

/**
 * Makes a submenu as wide as given, and where its custom properties place it in an element placing
 * it that is off the corner of the window by some pixels.
 */
function placedAt(submenu: HTMLElement, width: number, offsetX: number, offsetY = 0): void {
	Object.defineProperty(submenu, 'offsetWidth', { value: width });
	submenu.getBoundingClientRect = () => {
		const left = parseFloat(submenu.style.getPropertyValue(MENU_X_PROPERTY)) + offsetX;
		const top = parseFloat(submenu.style.getPropertyValue(MENU_Y_PROPERTY)) + offsetY;
		return { left, top, width, height: 0, right: left + width, bottom: top, x: left, y: top,
			toJSON: () => ({}) } as DOMRect;
	};
}

/** Lets the mutation observers run. */
function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 0));
}

afterEach(() => {
	Menu.of('m')?.destroy();
	document.body.innerHTML = '';
	vi.useRealTimers();
});

describe('Menu', () => {
	it('makes a menu bar of the list, its items menu items and their lists menus', () => {
		const element = markup();
		const instance = new Menu(element, { label: 'Main' });

		expect(Menu.of('m')).toBe(instance);
		expect(element.className).toBe('antilia-menu antilia-menu-horizontal antilia-menu-bar');
		const bar = element.querySelector('.antilia-menu-list')!;
		expect(bar.getAttribute('role')).toBe('menubar');
		expect(bar.getAttribute('aria-orientation')).toBe('horizontal');
		expect(bar.getAttribute('aria-label')).toBe('Main');
		expect(byId('home').parentElement!.getAttribute('role')).toBe('none');
		expect(byId('home').getAttribute('role')).toBe('menuitem');
		expect(byId('home').hasAttribute('aria-haspopup')).toBe(false);
		const file = byId('file');
		expect(file.getAttribute('aria-haspopup')).toBe('menu');
		expect(file.getAttribute('aria-expanded')).toBe('false');
		expect(file.getAttribute('aria-controls')).toBe('file-menu');
		expect(byId('file-menu').getAttribute('role')).toBe('menu');
		expect(byId('file-menu').getAttribute('aria-labelledby')).toBe('file');
		expect(byId('file-menu').hidden).toBe(true);
		expect(byId('export-menu').hidden).toBe(true);
		expect(byId('pdf').getAttribute('role')).toBe('menuitem');
		const separators = element.querySelectorAll('.antilia-menu-separator');
		expect(separators[0].getAttribute('role')).toBe('separator');
		expect(separators[0].getAttribute('aria-orientation')).toBe('horizontal');
		expect(separators[1].getAttribute('aria-orientation')).toBe('vertical');
		// only the first item of the bar takes the focus with Tab
		expect(byId('home').getAttribute('tabindex')).toBe('0');
		expect(['file', 'edit', 'help', 'new', 'pdf'].map((id) => byId(id).tabIndex))
			.toEqual([-1, -1, -1, -1, -1]);
		expect((byId('new') as HTMLButtonElement).type).toBe('button');
	});

	it('marks the item of the page shown, and those whose submenus hold it', () => {
		markup({ pdf: 'aria-current="page"' });
		new Menu(byId('m'));

		expect(byId('pdf').classList.contains('antilia-menu-current')).toBe(true);
		expect(byId('export').classList.contains('antilia-menu-current-parent')).toBe(true);
		expect(byId('file').classList.contains('antilia-menu-current-parent')).toBe(true);
		expect(byId('edit').classList.contains('antilia-menu-current-parent')).toBe(false);
		// the item of the bar holding it takes the focus with Tab
		expect(byId('file').getAttribute('tabindex')).toBe('0');
		expect(byId('home').getAttribute('tabindex')).toBe('-1');
	});

	it('opens a submenu on a click of its item, and closes it on another', () => {
		markup();
		new Menu(byId('m'));

		click(byId('file'));
		expect(isOpen('file')).toBe(true);
		click(byId('edit'));
		expect(isOpen('edit')).toBe(true);
		expect(isOpen('file')).toBe(false);
		click(byId('edit'));
		expect(isOpen('edit')).toBe(false);
	});

	it('closes the submenus when the user chooses an item, telling the page', () => {
		markup();
		const onSelect = vi.fn();
		new Menu(byId('m'), { onSelect });

		click(byId('file'));
		click(byId('export'));
		expect(isOpen('export')).toBe(true);
		byId('csv').focus();
		click(byId('csv'));

		expect(onSelect).toHaveBeenCalledWith(byId('csv'));
		expect(isOpen('file')).toBe(false);
		expect(isOpen('export')).toBe(false);
		// the focus back on the item of the bar
		expect(document.activeElement).toBe(byId('file'));
	});

	it('closes the submenus on a click outside it, or the focus leaving it', () => {
		markup();
		new Menu(byId('m'));

		click(byId('file'));
		pointer('pointerdown', byId('outside'));
		expect(isOpen('file')).toBe(false);

		click(byId('file'));
		byId('new').dispatchEvent(new FocusEvent('focusout', { bubbles: true,
			relatedTarget: byId('outside') }));
		expect(isOpen('file')).toBe(false);
	});

	it('does nothing with a disabled item', () => {
		markup({ file: 'aria-disabled="true"' });
		const onSelect = vi.fn();
		new Menu(byId('m'), { onSelect });

		click(byId('file'));
		expect(isOpen('file')).toBe(false);
		click(byId('edit'));
		const event = click(byId('paste'));
		expect(event.defaultPrevented).toBe(true);
		expect(onSelect).not.toHaveBeenCalled();
		expect(isOpen('edit')).toBe(true);
	});

	it('moves along the bar with the arrow keys, Home and End', () => {
		markup();
		new Menu(byId('m'));
		byId('home').focus();

		expect(key(byId('home'), 'ArrowRight').defaultPrevented).toBe(true);
		expect(document.activeElement).toBe(byId('file'));
		key(byId('file'), 'ArrowRight');
		expect(document.activeElement).toBe(byId('edit'));
		key(byId('edit'), 'End');
		expect(document.activeElement).toBe(byId('help'));
		key(byId('help'), 'ArrowRight');
		expect(document.activeElement).toBe(byId('home'));
		key(byId('home'), 'ArrowLeft');
		expect(document.activeElement).toBe(byId('help'));
		key(byId('help'), 'Home');
		expect(document.activeElement).toBe(byId('home'));
		// the item the user is at takes the focus with Tab
		byId('edit').focus();
		expect(byId('edit').getAttribute('tabindex')).toBe('0');
		expect(byId('home').getAttribute('tabindex')).toBe('-1');
	});

	it('goes into a submenu with the arrow down, to its last item with the arrow up', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();

		key(byId('file'), 'ArrowDown');
		expect(isOpen('file')).toBe(true);
		expect(document.activeElement).toBe(byId('new'));
		key(byId('new'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('export'));
		key(byId('export'), 'ArrowDown');
		// past the separator
		expect(document.activeElement).toBe(byId('close'));
		key(byId('close'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('new'));
		key(byId('new'), 'ArrowUp');
		expect(document.activeElement).toBe(byId('close'));

		key(byId('close'), 'Escape');
		expect(isOpen('file')).toBe(false);
		expect(document.activeElement).toBe(byId('file'));
		key(byId('file'), 'ArrowUp');
		expect(document.activeElement).toBe(byId('close'));
	});

	it('opens a submenu of a submenu with the arrow right, closes it with the arrow left', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();
		key(byId('file'), 'ArrowDown');
		key(byId('new'), 'ArrowDown');

		key(byId('export'), 'ArrowRight');
		expect(isOpen('export')).toBe(true);
		expect(document.activeElement).toBe(byId('pdf'));
		key(byId('pdf'), 'ArrowLeft');
		expect(isOpen('export')).toBe(false);
		expect(isOpen('file')).toBe(true);
		expect(document.activeElement).toBe(byId('export'));
		key(byId('export'), 'ArrowRight');
		key(byId('pdf'), 'Escape');
		expect(isOpen('export')).toBe(false);
		expect(document.activeElement).toBe(byId('export'));
	});

	it('goes along the bar from a submenu with the arrows across', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();
		key(byId('file'), 'ArrowDown');

		// the arrow right on an item without a submenu: the next item of the bar, opened
		key(byId('new'), 'ArrowRight');
		expect(isOpen('file')).toBe(false);
		expect(isOpen('edit')).toBe(true);
		expect(document.activeElement).toBe(byId('undo'));
		// the arrow left in a submenu of the bar: the previous item of the bar, opened
		key(byId('undo'), 'ArrowLeft');
		expect(isOpen('edit')).toBe(false);
		expect(isOpen('file')).toBe(true);
		expect(document.activeElement).toBe(byId('new'));
	});

	it('opens the submenu of the next item of the bar while one is open', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();
		click(byId('file'));

		key(byId('file'), 'ArrowRight');
		expect(document.activeElement).toBe(byId('edit'));
		expect(isOpen('edit')).toBe(true);
		expect(isOpen('file')).toBe(false);
		key(byId('edit'), 'ArrowRight');
		expect(isOpen('edit')).toBe(false);
	});

	it('goes to the next item starting with a letter typed', () => {
		markup();
		new Menu(byId('m'));
		byId('home').focus();

		key(byId('home'), 'e');
		expect(document.activeElement).toBe(byId('edit'));
		key(byId('edit'), 'H');
		expect(document.activeElement).toBe(byId('help'));
		key(byId('help'), 'h');
		expect(document.activeElement).toBe(byId('home'));
	});

	it('closes the submenus on Tab, the focus going on from the bar', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();
		key(byId('file'), 'ArrowDown');

		const event = key(byId('new'), 'Tab');
		expect(event.defaultPrevented).toBe(false);
		expect(isOpen('file')).toBe(false);
		expect(document.activeElement).toBe(byId('file'));
	});

	it('goes into a submenu opened by the keys clicking its item', () => {
		markup();
		new Menu(byId('m'));
		byId('file').focus();

		// Enter or Space on a button: the browser clicks it, without a pointer
		click(byId('file'), 0);
		expect(isOpen('file')).toBe(true);
		expect(document.activeElement).toBe(byId('new'));
	});

	it('clicks a link with Space, and an item that is neither link nor button with Enter', () => {
		markup();
		new Menu(byId('m'));
		const clicked = vi.fn();
		byId('help').addEventListener('click', (event) => {
			event.preventDefault();
			clicked();
		});
		byId('help').focus();

		expect(key(byId('help'), ' ').defaultPrevented).toBe(true);
		expect(clicked).toHaveBeenCalledTimes(1);
		// Enter on a link: the browser follows it
		expect(key(byId('help'), 'Enter').defaultPrevented).toBe(false);
		expect(clicked).toHaveBeenCalledTimes(1);
	});

	it('opens the submenus of submenus as the mouse rests on their items', () => {
		vi.useFakeTimers();
		markup();
		new Menu(byId('m'));
		byId('file').focus();
		click(byId('file'));

		pointer('pointerover', byId('export'));
		expect(isOpen('export')).toBe(false);
		vi.advanceTimersByTime(200);
		expect(isOpen('export')).toBe(true);
		// the focus in the menu follows the pointer
		expect(document.activeElement).toBe(byId('export'));
		// a sibling closes it
		pointer('pointerover', byId('close'));
		vi.advanceTimersByTime(200);
		expect(isOpen('export')).toBe(false);
		// a touch does not
		pointer('pointerover', byId('export'), 'touch');
		vi.advanceTimersByTime(200);
		expect(isOpen('export')).toBe(false);
	});

	it('follows the mouse along the bar while a submenu of it is open', () => {
		vi.useFakeTimers();
		markup();
		new Menu(byId('m'));

		pointer('pointerover', byId('file'));
		vi.advanceTimersByTime(500);
		expect(isOpen('file')).toBe(false);
		click(byId('file'));
		pointer('pointerover', byId('edit'));
		vi.advanceTimersByTime(0);
		expect(isOpen('edit')).toBe(true);
		expect(isOpen('file')).toBe(false);
		pointer('pointerover', byId('help'));
		vi.advanceTimersByTime(0);
		expect(isOpen('edit')).toBe(false);
	});

	it('opens the submenus of the bar on hover when asked, closing them as the mouse leaves', () => {
		vi.useFakeTimers();
		markup();
		new Menu(byId('m'), { openOnHover: true });

		byId('outside').focus();
		pointer('pointerover', byId('file'));
		vi.advanceTimersByTime(200);
		expect(isOpen('file')).toBe(true);
		// the focus elsewhere in the page stays there
		pointer('pointerover', byId('export'));
		vi.advanceTimersByTime(200);
		expect(isOpen('export')).toBe(true);
		expect(document.activeElement).toBe(byId('outside'));
		// a click right after leaves it open
		click(byId('file'));
		expect(isOpen('file')).toBe(true);

		pointer('pointerout', byId('new'), 'mouse', byId('outside'));
		vi.advanceTimersByTime(200);
		expect(isOpen('file')).toBe(true);
		vi.advanceTimersByTime(300);
		expect(isOpen('file')).toBe(false);
	});

	it('places a submenu below its item of a bar along the top, and beside an item of a submenu', () => {
		markup();
		new Menu(byId('m'));
		const rect = (left: number, top: number, width: number, height: number) => () =>
			({ left, top, width, height, right: left + width, bottom: top + height, x: left, y: top,
				toJSON: () => ({}) }) as DOMRect;
		byId('file').getBoundingClientRect = rect(100, 10, 60, 30);
		byId('export').getBoundingClientRect = rect(100, 80, 150, 30);

		click(byId('file'));
		const fileMenu = byId('file-menu');
		expect(fileMenu.style.getPropertyValue(MENU_X_PROPERTY)).toBe('100px');
		expect(fileMenu.style.getPropertyValue(MENU_Y_PROPERTY)).toBe('40px');
		expect(fileMenu.getAttribute('data-placement')).toBe('below');

		click(byId('export'));
		const exportMenu = byId('export-menu');
		expect(exportMenu.style.getPropertyValue(MENU_X_PROPERTY)).toBe('250px');
		expect(exportMenu.style.getPropertyValue(MENU_Y_PROPERTY)).toBe('80px');
		expect(exportMenu.getAttribute('data-placement')).toBe('beside');
	});

	it('places a submenu on the left of its item without room on the right', () => {
		markup();
		new Menu(byId('m'));
		const width = document.documentElement.clientWidth || window.innerWidth;
		byId('export').getBoundingClientRect = () => ({ left: width - 120, top: 80, width: 110,
			height: 30, right: width - 10, bottom: 110, x: width - 120, y: 80,
			toJSON: () => ({}) }) as DOMRect;
		placedAt(byId('export-menu'), 200, 0);

		click(byId('file'));
		click(byId('export'));
		expect(byId('export-menu').style.getPropertyValue(MENU_X_PROPERTY))
			.toBe((width - 320) + 'px');
	});

	it('places a submenu where it ought to be in a transformed element that places it', () => {
		markup();
		new Menu(byId('m'));
		byId('file').getBoundingClientRect = () => ({ left: 100, top: 10, width: 60, height: 30,
			right: 160, bottom: 40, x: 100, y: 10, toJSON: () => ({}) }) as DOMRect;
		// the element placing it is 30 pixels right of the window's corner, and 5 below
		placedAt(byId('file-menu'), 150, 30, 5);

		click(byId('file'));
		expect(byId('file-menu').style.getPropertyValue(MENU_X_PROPERTY)).toBe('70px');
		expect(byId('file-menu').style.getPropertyValue(MENU_Y_PROPERTY)).toBe('35px');
	});

	it('goes down the side, its submenus popping up beside it', () => {
		markup();
		new Menu(byId('m'), { orientation: 'vertical', design: 'sidebar' });
		const bar = byId('m').querySelector('.antilia-menu-list')!;
		expect(byId('m').classList.contains('antilia-menu-vertical')).toBe(true);
		expect(byId('m').classList.contains('antilia-menu-sidebar')).toBe(true);
		expect(bar.getAttribute('aria-orientation')).toBe('vertical');
		byId('home').focus();

		key(byId('home'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('file'));
		key(byId('file'), 'ArrowRight');
		expect(isOpen('file')).toBe(true);
		expect(document.activeElement).toBe(byId('new'));
		key(byId('new'), 'ArrowLeft');
		expect(isOpen('file')).toBe(false);
		expect(document.activeElement).toBe(byId('file'));
	});

	it('opens the submenus in place down the side, as a tree, those of the current item first', () => {
		markup({ pdf: 'aria-current="page"' });
		new Menu(byId('m'), { orientation: 'vertical', inline: true });
		expect(byId('m').classList.contains('antilia-menu-inline')).toBe(true);
		expect(isOpen('file')).toBe(true);
		expect(isOpen('export')).toBe(true);
		expect(isOpen('edit')).toBe(false);

		// several open at once, a click closing one
		click(byId('edit'));
		expect(isOpen('edit')).toBe(true);
		expect(isOpen('file')).toBe(true);
		click(byId('file'));
		expect(isOpen('file')).toBe(false);

		// the keys go through the items shown
		byId('home').focus();
		key(byId('home'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('file'));
		key(byId('file'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('edit'));
		key(byId('edit'), 'ArrowDown');
		expect(document.activeElement).toBe(byId('undo'));
		key(byId('undo'), 'ArrowLeft');
		expect(document.activeElement).toBe(byId('edit'));
		key(byId('edit'), 'ArrowLeft');
		expect(isOpen('edit')).toBe(false);
		key(byId('edit'), 'ArrowRight');
		expect(isOpen('edit')).toBe(true);
		expect(document.activeElement).toBe(byId('edit'));
		key(byId('edit'), 'ArrowRight');
		expect(document.activeElement).toBe(byId('undo'));
		key(byId('undo'), 'End');
		expect(document.activeElement).toBe(byId('help'));
		key(byId('help'), 'Home');
		expect(document.activeElement).toBe(byId('home'));
		// a click outside leaves them open
		pointer('pointerdown', byId('outside'));
		expect(isOpen('edit')).toBe(true);
	});

	it('pops the submenus up along the top, even when asked to open them in place', () => {
		markup({ pdf: 'aria-current="page"' });
		const instance = new Menu(byId('m'), { inline: true });

		expect(instance.inline()).toBe(false);
		expect(byId('m').classList.contains('antilia-menu-inline')).toBe(false);
		expect(isOpen('file')).toBe(false);
	});

	it('gives its bar the classes of its design in the look, switching them', () => {
		markup({ pdf: 'aria-current="page"' });
		const instance = new Menu(byId('m'), { look: BOOTSTRAP_LOOK, design: 'pills' });
		const bar = byId('m').querySelector('.antilia-menu-list')!;

		expect(bar.className).toBe('antilia-menu-list nav nav-pills');
		expect(byId('pdf').classList.contains('active')).toBe(true);
		instance.configure({ design: 'underline' });
		expect(bar.className).toBe('antilia-menu-list nav nav-underline');
		expect(byId('m').classList.contains('antilia-menu-underline')).toBe(true);
		expect(byId('m').classList.contains('antilia-menu-pills')).toBe(false);
		instance.configure({ design: 'bar' });
		expect(bar.className).toBe('antilia-menu-list nav bg-body-tertiary border rounded');
		expect(MENU_DESIGNS).toEqual(['bar', 'pills', 'underline', 'sidebar']);
	});

	it('finds the items the page adds, and the item of the page shown as it changes', async () => {
		markup();
		new Menu(byId('m'));

		const list = byId('file-menu');
		list.insertAdjacentHTML('beforeend', subItem(item('print', 'Print')));
		byId('help').setAttribute('aria-current', 'page');
		await settle();
		expect(byId('print').getAttribute('role')).toBe('menuitem');
		expect(byId('print').tabIndex).toBe(-1);
		expect(byId('help').classList.contains('antilia-menu-current')).toBe(true);
	});

	it('opens the submenus in place as it starts doing so, closing those popping up', () => {
		markup({ pdf: 'aria-current="page"' });
		const instance = new Menu(byId('m'), { orientation: 'vertical' });
		click(byId('edit'));

		instance.configure({ inline: true });
		expect(isOpen('edit')).toBe(false);
		expect(isOpen('file')).toBe(true);
		instance.configure({ inline: false });
		expect(isOpen('file')).toBe(false);
	});

	it('goes away leaving the items, the submenus closed', () => {
		markup();
		const instance = new Menu(byId('m'), { design: 'pills' });
		click(byId('file'));

		instance.destroy();
		expect(Menu.of('m')).toBeNull();
		expect(isOpen('file')).toBe(false);
		expect(byId('m').className).toBe('antilia-menu');
		click(byId('file'));
		expect(isOpen('file')).toBe(false);
	});

	it('works as a Svelte action', () => {
		markup();
		const action = menu(byId('m'), { orientation: 'vertical' });
		expect(byId('m').classList.contains('antilia-menu-vertical')).toBe(true);
		action.update({ orientation: 'horizontal' });
		expect(byId('m').classList.contains('antilia-menu-horizontal')).toBe(true);
		action.destroy();
		expect(Menu.of('m')).toBeNull();
	});

	it('attaches to an element by its id, as Wicket does', () => {
		markup();
		expect(Menu.attach('nothing')).toBeNull();
		const instance = Menu.attach('m', { design: 'underline' });
		expect(Menu.of(byId('m'))).toBe(instance);
		expect(instance?.design()).toBe('underline');
	});
});
