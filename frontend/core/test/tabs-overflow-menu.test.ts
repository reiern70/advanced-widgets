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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { Tabs, TabsOverflowMenu, tabsOverflowMenu } from '../src/index.js';

function tabMarkup(name: string, attributes = ''): string {
	return '<div class="antilia-tab" data-tab="' + name + '" ' + attributes + '>' +
		'<button class="antilia-tab-button"><span class="antilia-tab-icon"><svg data-icon="' + name +
		'"></svg></span><span class="antilia-tab-title">Title ' + name + '</span></button></div>';
}

/**
 * @returns the element of tabs one to four, with their panels
 */
function markup(): HTMLElement {
	const names = ['one', 'two', 'three', 'four'];
	document.body.innerHTML = '<div id="t" class="antilia-tabs"><div class="antilia-tabs-list">' +
		names.map((name) => tabMarkup(name, name === 'four' ? 'data-closable="true"' : '')).join('') +
		'</div><div class="antilia-tabs-panels">' + names.map((name) =>
		'<div class="antilia-tabs-panel" data-tab="' + name + '"><input id="' + name +
			'-field"></div>').join('') + '</div></div>';
	return document.getElementById('t')!;
}

function list(): HTMLElement {
	return document.querySelector('.antilia-tabs-list')!;
}

/** Gives the list of tabs the sizes of one that overflows, or not. */
function overflowing(on: boolean): void {
	Object.defineProperty(list(), 'scrollWidth', { configurable: true, get: () => on ? 600 : 200 });
	Object.defineProperty(list(), 'clientWidth', { configurable: true, get: () => 200 });
}

function button(name: string): HTMLButtonElement {
	return document.querySelector('.antilia-tab[data-tab="' + name + '"] .antilia-tab-button')!;
}

function items(): HTMLElement[] {
	return Array.from(document.querySelectorAll<HTMLElement>(
		'.antilia-tabs-overflow .antilia-menu-submenu-link'));
}

/** Lets the mutation observers and the frame of the menu run. */
function settle(): Promise<void> {
	return new Promise((done) => setTimeout(done, 40));
}

afterEach(() => {
	document.body.innerHTML = '';
});

describe('TabsOverflowMenu', () => {
	it('shows a button at the end of the list while it overflows, opening a menu of all tabs',
		() => {
			const element = markup();
			const onSelect = vi.fn();
			const tabs = new Tabs(element, { onSelect });
			overflowing(true);
			const menu = new TabsOverflowMenu(element, { label: 'Every tab' });

			expect(list().lastElementChild).toBe(menu.container);
			expect(menu.overflows()).toBe(true);
			expect(menu.container.hidden).toBe(false);
			expect(menu.button.getAttribute('aria-label')).toBe('Every tab');
			expect(menu.button.title).toBe('Every tab');
			expect(menu.button.getAttribute('aria-haspopup')).not.toBeNull();
			expect(menu.names()).toEqual(['one', 'two', 'three', 'four']);
			// not a tab
			expect(tabs.names()).toEqual(['one', 'two', 'three', 'four']);
			expect(items().map((item) => item.textContent)).toEqual(['Title one', 'Title two',
				'Title three', 'Title four']);
			expect(items()[0].getAttribute('aria-current')).toBe('true');
			expect(items()[1].hasAttribute('aria-current')).toBe(false);
			expect(items()[2].querySelector('svg[data-icon="three"]')).not.toBeNull();

			menu.button.click();
			expect(menu.button.getAttribute('aria-expanded')).toBe('true');
			items()[2].click();
			expect(tabs.selected()).toBe('three');
			expect(onSelect).toHaveBeenCalledWith('three');
			expect(document.activeElement).toBe(button('three'));
			expect(menu.button.getAttribute('aria-expanded')).toBe('false');
		});

	it('hides the button while all tabs fit', () => {
		const element = markup();
		new Tabs(element);
		overflowing(false);
		const menu = new TabsOverflowMenu(element);
		expect(menu.overflows()).toBe(false);
		expect(menu.container.hidden).toBe(true);
		expect(menu.button.getAttribute('aria-label')).toBe('All tabs');

		overflowing(true);
		menu.update();
		expect(menu.overflows()).toBe(true);
		overflowing(false);
		menu.update();
		expect(menu.container.hidden).toBe(true);
	});

	it('follows the tabs: those added and closed, the titles, the marks and the selection',
		async () => {
			const element = markup();
			const tabs = new Tabs(element);
			overflowing(true);
			const menu = new TabsOverflowMenu(element);

			list().insertAdjacentHTML('beforeend', tabMarkup('five'));
			await settle();
			expect(menu.names()).toEqual(['one', 'two', 'three', 'four', 'five']);
			// still at the end
			expect(list().lastElementChild).toBe(menu.container);

			tabs.close('four');
			button('two').querySelector('.antilia-tab-title')!.textContent = 'Title two (3)';
			(document.getElementById('one-field') as HTMLInputElement).value = 'typed';
			document.getElementById('one-field')!.dispatchEvent(new Event('input', { bubbles: true }));
			tabs.select('five');
			await settle();
			expect(menu.names()).toEqual(['one', 'two', 'three', 'five']);
			expect(items()[1].textContent).toBe('Title two (3)');
			expect(items()[0].querySelector('.antilia-tabs-overflow-mark')).not.toBeNull();
			expect(items()[3].getAttribute('aria-current')).toBe('true');
			expect(items()[0].hasAttribute('aria-current')).toBe(false);
		});

	it('is found by its tabs, and taken away with its button', () => {
		const element = markup();
		new Tabs(element);
		const action = tabsOverflowMenu(element, { label: 'Tabs' });
		const menu = TabsOverflowMenu.of('t')!;
		expect(menu).not.toBeNull();
		action.update({ label: 'All of them' });
		expect(menu.button.getAttribute('aria-label')).toBe('All of them');
		action.destroy();
		expect(document.querySelector('.antilia-tabs-overflow')).toBeNull();
		expect(TabsOverflowMenu.of('t')).toBeNull();
		expect(TabsOverflowMenu.attach('missing')).toBeNull();
	});

	it('opens the menu beside the button for tabs down the side', async () => {
		const element = markup();
		new Tabs(element, { orientation: 'vertical' });
		Object.defineProperty(list(), 'scrollHeight', { configurable: true, get: () => 600 });
		Object.defineProperty(list(), 'clientHeight', { configurable: true, get: () => 200 });
		const menu = new TabsOverflowMenu(element);
		expect(menu.overflows()).toBe(true);
		expect(menu.container.classList.contains('antilia-menu-vertical')).toBe(true);
	});

	it('registers Wicket.TabsOverflowMenu', async () => {
		window.Wicket = {};
		await import('../src/wicket/tabs-overflow-menu.js');
		const wicketMenu = (window.Wicket as unknown as { TabsOverflowMenu: {
			attach(id: string, options?: object): TabsOverflowMenu | null;
			of(id: string): TabsOverflowMenu | null } }).TabsOverflowMenu;
		new Tabs(markup());
		const menu = wicketMenu.attach('t', { label: 'Alle Tabs' });
		expect(wicketMenu.of('t')).toBe(menu);
		expect(menu!.button.getAttribute('aria-label')).toBe('Alle Tabs');
	});
});
