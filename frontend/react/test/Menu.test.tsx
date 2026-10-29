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

import { createRef, useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { LookProvider, Menu, type MenuEntry, type MenuHandle } from '../src/index.js';

afterEach(() => {
	cleanup();
});

function items(onClick = vi.fn()): MenuEntry[] {
	return [
		{ id: 'home', label: 'Home', href: '#home', icon: <svg />, tooltip: 'Go home' },
		{ id: 'file', label: 'File', items: [
			{ id: 'new', label: 'New', onClick },
			{ id: 'export', label: 'Export', items: [{ id: 'pdf', label: 'PDF', current: true }] },
			{ separator: true },
			{ id: 'close', label: 'Close', disabled: true, onClick }
		] },
		{ separator: true },
		{ id: 'help', label: 'Help', href: '#help', disabled: true }
	];
}

function byText(container: HTMLElement, text: string): HTMLElement {
	return Array.from(container.querySelectorAll<HTMLElement>('.antilia-menu-label'))
		.find((label) => label.textContent === text)!.parentElement!;
}

describe('Menu', () => {
	it('renders the bar, its items and submenus, the core giving them their roles', () => {
		const { container } = render(<Menu id="m" label="Main" className="mine" items={items()} />);

		const root = container.querySelector('#m')!;
		expect(root.className).toBe('antilia-menu antilia-menu-horizontal antilia-menu-bar mine');
		const bar = root.querySelector('.antilia-menu-list')!;
		expect(bar.getAttribute('role')).toBe('menubar');
		expect(bar.getAttribute('aria-label')).toBe('Main');
		const home = byText(container, 'Home');
		expect(home.tagName).toBe('A');
		expect(home.getAttribute('href')).toBe('#home');
		expect(home.getAttribute('title')).toBe('Go home');
		expect(home.className).toBe('antilia-menu-link');
		expect(home.querySelector('.antilia-menu-icon svg')).not.toBeNull();
		expect(home.getAttribute('role')).toBe('menuitem');
		const file = byText(container, 'File');
		expect(file.tagName).toBe('BUTTON');
		expect(file.getAttribute('aria-haspopup')).toBe('menu');
		expect(file.classList.contains('antilia-menu-current-parent')).toBe(true);
		expect(file.nextElementSibling!.className).toBe('antilia-menu-submenu');
		expect((file.nextElementSibling as HTMLElement).hidden).toBe(true);
		const pdf = byText(container, 'PDF');
		expect(pdf.className).toBe('antilia-menu-submenu-link antilia-menu-current');
		expect(pdf.getAttribute('aria-current')).toBe('page');
		expect(container.querySelectorAll('.antilia-menu-separator').length).toBe(2);
		const help = byText(container, 'Help');
		expect(help.hasAttribute('href')).toBe(false);
		expect(help.getAttribute('aria-disabled')).toBe('true');
	});

	it('opens a submenu on a click, and calls the item chosen and the page back', () => {
		const onClick = vi.fn();
		const onSelect = vi.fn();
		const { container } = render(<Menu items={items(onClick)} onSelect={onSelect} />);
		const file = byText(container, 'File');

		fireEvent.click(file);
		expect(file.getAttribute('aria-expanded')).toBe('true');
		expect((file.nextElementSibling as HTMLElement).hidden).toBe(false);
		fireEvent.click(byText(container, 'New'));
		expect(onClick).toHaveBeenCalledTimes(1);
		expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'new' }));
		expect(file.getAttribute('aria-expanded')).toBe('false');

		// a disabled item does nothing
		fireEvent.click(file);
		fireEvent.click(byText(container, 'Close'));
		expect(onClick).toHaveBeenCalledTimes(1);
		expect(onSelect).toHaveBeenCalledTimes(1);
	});

	it('takes the look of the closest LookProvider', () => {
		const { container } = render(<LookProvider look="bootstrap">
			<Menu items={items()} design="pills" />
		</LookProvider>);

		expect(container.querySelector('.antilia-menu-list')!.className)
			.toBe('antilia-menu-list nav nav-pills');
		expect(byText(container, 'Home').className).toBe('antilia-menu-link nav-link');
		expect(byText(container, 'New').className).toBe('antilia-menu-submenu-link dropdown-item');
		expect(byText(container, 'PDF').className)
			.toBe('antilia-menu-submenu-link dropdown-item antilia-menu-current active');
		expect(container.querySelector('.antilia-menu-submenu')!.className)
			.toBe('antilia-menu-submenu dropdown-menu');
		expect(container.querySelector('.antilia-menu-separator')!.className)
			.toBe('antilia-menu-separator dropdown-divider');
		expect(container.querySelector('.antilia-menu-item')!.className)
			.toBe('antilia-menu-item nav-item');
	});

	it('goes down the side, opening its submenus in place, those of the current item first', () => {
		const { container, rerender } = render(<Menu items={items()} orientation="vertical"
			design="sidebar" inline />);

		const root = container.querySelector('.antilia-menu')!;
		expect(root.className).toBe('antilia-menu antilia-menu-vertical antilia-menu-sidebar ' +
			'antilia-menu-inline');
		expect(byText(container, 'File').getAttribute('aria-expanded')).toBe('true');
		expect(byText(container, 'Export').getAttribute('aria-expanded')).toBe('true');

		rerender(<Menu items={items()} orientation="vertical" design="sidebar" />);
		expect(root.classList.contains('antilia-menu-inline')).toBe(false);
		expect(byText(container, 'File').getAttribute('aria-expanded')).toBe('false');
	});

	it('keeps the submenus open as the items change, finding new ones', () => {
		function Changing() {
			const [extra, setExtra] = useState(false);
			const entries: MenuEntry[] = [{ id: 'file', label: 'File', items: [
				{ id: 'new', label: 'New', onClick: () => setExtra(true) },
				...(extra ? [{ id: 'print', label: 'Print', onClick: () => {} }] : [])] }];
			return <Menu items={entries} />;
		}
		const { container } = render(<Changing />);
		const file = byText(container, 'File');
		fireEvent.click(file);
		// the item chosen closes the submenu, adding an item
		fireEvent.click(byText(container, 'New'));
		expect(byText(container, 'Print').getAttribute('role')).toBe('menuitem');
		fireEvent.click(file);
		expect(file.getAttribute('aria-expanded')).toBe('true');
	});

	it('gives a ref the menu of the core', () => {
		const ref = createRef<MenuHandle>();
		const { container } = render(<Menu ref={ref} items={items()} />);
		const file = byText(container, 'File');
		fireEvent.click(file);

		expect(ref.current!.menu!.isOpen(file)).toBe(true);
		act(() => ref.current!.closeAll());
		expect(file.getAttribute('aria-expanded')).toBe('false');
	});

	it('writes an icon of text as text, and iconMarkup as markup', () => {
		const { container } = render(<Menu items={[
			{ id: 'text', label: 'Text', icon: '<svg data-icon="text"></svg>' },
			{ id: 'markup', label: 'Markup', iconMarkup: '<svg data-icon="markup"></svg>' }
		]} />);

		const text = byText(container, 'Text').querySelector('.antilia-menu-icon')!;
		expect(text.textContent).toBe('<svg data-icon="text"></svg>');
		expect(text.querySelector('svg')).toBeNull();
		expect(byText(container, 'Markup')
			.querySelector('.antilia-menu-icon svg[data-icon="markup"]')).not.toBeNull();
	});
});
