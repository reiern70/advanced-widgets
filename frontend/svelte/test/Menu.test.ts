/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LOOK_CONTEXT, Menu, type MenuEntry } from '../src/index.js';

function items(onclick = vi.fn()): MenuEntry[] {
	return [
		{ id: 'home', label: 'Home', href: '#home', icon: '<svg data-icon="home"></svg>',
			tooltip: 'Go home' },
		{ id: 'file', label: 'File', items: [
			{ id: 'new', label: 'New', onclick },
			{ id: 'export', label: 'Export', items: [{ id: 'pdf', label: 'PDF', current: true }] },
			{ separator: true },
			{ id: 'close', label: 'Close', disabled: true, onclick }
		] },
		{ separator: true },
		{ id: 'help', label: 'Help', href: '#help', disabled: true }
	];
}

function byText(container: HTMLElement, text: string): HTMLElement {
	return Array.from(container.querySelectorAll<HTMLElement>('.antilia-menu-label'))
		.find((label) => label.textContent === text)!.parentElement!;
}

afterEach(() => {
	cleanup();
});

describe('Menu', () => {
	it('renders the bar, its items and submenus, the core giving them their roles', () => {
		const { container } = render(Menu, { props: { id: 'm', label: 'Main', class: 'mine',
			items: items() } });

		const root = container.querySelector('#m')!;
		expect(root.className).toBe('antilia-menu antilia-menu-horizontal antilia-menu-bar mine');
		const bar = root.querySelector('.antilia-menu-list')!;
		expect(bar.getAttribute('role')).toBe('menubar');
		expect(bar.getAttribute('aria-label')).toBe('Main');
		const home = byText(container, 'Home');
		expect(home.tagName).toBe('A');
		expect(home.getAttribute('href')).toBe('#home');
		expect(home.getAttribute('title')).toBe('Go home');
		expect(home.querySelector('.antilia-menu-icon svg[data-icon="home"]')).not.toBeNull();
		expect(home.getAttribute('role')).toBe('menuitem');
		const file = byText(container, 'File');
		expect(file.getAttribute('aria-haspopup')).toBe('menu');
		expect(file.classList.contains('antilia-menu-current-parent')).toBe(true);
		expect((file.nextElementSibling as HTMLElement).hidden).toBe(true);
		const pdf = byText(container, 'PDF');
		expect(pdf.className).toBe('antilia-menu-submenu-link antilia-menu-current');
		expect(pdf.getAttribute('aria-current')).toBe('page');
		expect(container.querySelectorAll('.antilia-menu-separator').length).toBe(2);
		expect(byText(container, 'Help').hasAttribute('href')).toBe(false);
		expect(byText(container, 'Help').getAttribute('aria-disabled')).toBe('true');
	});

	it('opens a submenu on a click, and calls the item chosen and the page back', async () => {
		const onclick = vi.fn();
		const onselect = vi.fn();
		const { container } = render(Menu, { props: { items: items(onclick), onselect } });
		const file = byText(container, 'File');

		await fireEvent.click(file);
		expect(file.getAttribute('aria-expanded')).toBe('true');
		await fireEvent.click(byText(container, 'New'));
		expect(onclick).toHaveBeenCalledTimes(1);
		expect(onselect).toHaveBeenCalledWith(expect.objectContaining({ id: 'new' }));
		expect(file.getAttribute('aria-expanded')).toBe('false');

		await fireEvent.click(file);
		await fireEvent.click(byText(container, 'Close'));
		expect(onclick).toHaveBeenCalledTimes(1);
	});

	it('takes the look of the closest setLook', () => {
		const { container } = render(Menu, { props: { items: items(), design: 'underline' },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });

		expect(container.querySelector('.antilia-menu-list')!.className)
			.toBe('antilia-menu-list nav nav-underline');
		expect(byText(container, 'Home').className).toBe('antilia-menu-link nav-link');
		expect(byText(container, 'New').className).toBe('antilia-menu-submenu-link dropdown-item');
		expect(container.querySelector('.antilia-menu-submenu')!.className)
			.toBe('antilia-menu-submenu dropdown-menu');
	});

	it('goes down the side, opening its submenus in place, and follows its props', async () => {
		const { container, rerender } = render(Menu, { props: { items: items(),
			orientation: 'vertical', design: 'sidebar', inline: true } });

		const root = container.querySelector('.antilia-menu')!;
		expect(root.className).toBe('antilia-menu antilia-menu-vertical antilia-menu-sidebar ' +
			'antilia-menu-inline');
		expect(byText(container, 'File').getAttribute('aria-expanded')).toBe('true');

		await rerender({ items: [...items(), { id: 'more', label: 'More', onclick: () => {} }],
			inline: false });
		flushSync();
		expect(root.classList.contains('antilia-menu-inline')).toBe(false);
		expect(byText(container, 'File').getAttribute('aria-expanded')).toBe('false');
		expect(byText(container, 'More').getAttribute('role')).toBe('menuitem');
		expect(byText(container, 'File').classList.contains('antilia-menu-current-parent')).toBe(true);
	});

	it('gives bind:this the menu of the core', async () => {
		const { container, component } = render(Menu, { props: { items: items() } });
		const file = byText(container, 'File');
		await fireEvent.click(file);

		expect(component.core()!.isOpen(file)).toBe(true);
		component.closeAll();
		expect(file.getAttribute('aria-expanded')).toBe('false');
	});
});
