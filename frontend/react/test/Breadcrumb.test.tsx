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

import { Breadcrumb, LookProvider, BOOTSTRAP_LOOK, type BreadcrumbHandle,
	type BreadcrumbItem } from '../src/index.js';

afterEach(() => {
	cleanup();
});

const TRAIL = ['Home', 'Products', 'Computers', 'Laptops', 'Gaming', 'Model X'];

function trail(names: string[], onClick?: (name: string) => void): BreadcrumbItem[] {
	return names.map((name) => ({ id: name, label: name, href: '#' + name,
		onClick: onClick && (() => onClick(name)) }));
}

/** @returns the labels of the crumbs shown, "…" for the button */
function shown(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll<HTMLElement>('.antilia-breadcrumb-list > li'))
		.filter((item) => !item.hidden)
		.map((item) => item.classList.contains('antilia-breadcrumb-more') ? '…' :
			item.querySelector('.antilia-breadcrumb-label')!.textContent!);
}

describe('Breadcrumb', () => {
	it('renders the crumbs, the last one the page shown', () => {
		const onSelect = vi.fn();
		const onClick = vi.fn();
		const { container } = render(<Breadcrumb id="b" className="mine" onSelect={onSelect} items={[
			{ id: 'home', label: 'Home', href: '#home', icon: <svg />, tooltip: 'Go home' },
			{ id: 'all', label: 'All', onClick },
			{ id: 'group', label: 'Group' },
			{ id: 'off', label: 'Off', href: '#off', disabled: true },
			{ id: 'here', label: 'Here', href: '#here', tooltip: 'You are here' }
		]} />);

		const nav = container.querySelector('#b')!;
		expect(nav.tagName).toBe('NAV');
		expect(nav.className).toBe('antilia-breadcrumb antilia-breadcrumb-chevron mine');
		expect(nav.getAttribute('aria-label')).toBe('Breadcrumb');
		const home = nav.querySelector('a[href="#home"]')!;
		expect(home.className).toBe('antilia-breadcrumb-link');
		expect(home.getAttribute('title')).toBe('Go home');
		expect(home.querySelector('.antilia-breadcrumb-icon svg')).not.toBeNull();
		const all = nav.querySelector('button.antilia-breadcrumb-link')!;
		expect(all.textContent).toBe('All');
		expect(nav.querySelector('.antilia-breadcrumb-text')!.textContent).toBe('Group');
		const off = nav.querySelectorAll('a.antilia-breadcrumb-link')[1];
		expect(off.hasAttribute('href')).toBe(false);
		expect(off.getAttribute('aria-disabled')).toBe('true');
		const here = nav.querySelector('[aria-current="page"]')!;
		expect(here.tagName).toBe('SPAN');
		expect(here.className).toBe('antilia-breadcrumb-page');
		expect(here.getAttribute('title')).toBe('You are here');
		expect(here.parentElement!.className).toBe('antilia-breadcrumb-item antilia-breadcrumb-current');

		fireEvent.click(all);
		expect(onClick).toHaveBeenCalledTimes(1);
		expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'all' }));
		fireEvent.click(off);
		expect(onSelect).toHaveBeenCalledTimes(1);
	});

	it('collapses a long trail and shows it on a click on "…"', () => {
		const onExpand = vi.fn();
		const { container } = render(<Breadcrumb items={trail(TRAIL)} maxItems={4}
			texts={{ more: '{0} more', label: 'Path' }} onExpand={onExpand} />);

		expect(shown(container)).toEqual(['Home', '…', 'Laptops', 'Gaming', 'Model X']);
		expect(container.querySelector('nav')!.getAttribute('aria-label')).toBe('Path');
		const more = container.querySelector<HTMLButtonElement>('.antilia-breadcrumb-more-button')!;
		expect(more.getAttribute('aria-label')).toBe('2 more');

		fireEvent.click(more);

		expect(shown(container)).toEqual(TRAIL);
		expect(onExpand).toHaveBeenCalledTimes(1);
		expect(document.activeElement).toBe(container.querySelector('a[href="#Products"]'));
	});

	it('collapses again when the user goes back along the trail', () => {
		function Drill() {
			const [path, setPath] = useState(TRAIL);
			return <Breadcrumb maxItems={3} items={trail(path, (name) =>
				setPath(path.slice(0, path.indexOf(name) + 1)))} />;
		}
		const { container } = render(<Drill />);
		fireEvent.click(container.querySelector('.antilia-breadcrumb-more-button')!);
		expect(shown(container)).toEqual(TRAIL);

		fireEvent.click(container.querySelector('a[href="#Gaming"]')!);

		expect(shown(container)).toEqual(['Home', '…', 'Laptops', 'Gaming']);
		expect(container.querySelector('[aria-current="page"]')!.textContent).toBe('Gaming');

		// a crumb hidden, as a script would follow it
		fireEvent.click(container.querySelector('a[href="#Products"]')!);
		expect(shown(container)).toEqual(['Home', 'Products']);
	});

	it('follows its separator and its look, and gives a handle', () => {
		const ref = createRef<BreadcrumbHandle>();
		const { container, rerender } = render(<LookProvider look={BOOTSTRAP_LOOK}>
			<Breadcrumb ref={ref} items={trail(['Home', 'Products', 'Laptops'])} maxItems={2} />
		</LookProvider>);

		const list = container.querySelector('ol')!;
		expect(list.className).toBe('antilia-breadcrumb-list breadcrumb');
		expect(list.lastElementChild!.className)
			.toBe('antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-current active');
		expect(ref.current!.breadcrumb!.isCollapsed()).toBe(true);

		rerender(<LookProvider look={BOOTSTRAP_LOOK}>
			<Breadcrumb ref={ref} items={trail(['Home', 'Products', 'Laptops'])} maxItems={2}
				separator="arrow" />
		</LookProvider>);
		expect(container.querySelector('nav')!.className)
			.toBe('antilia-breadcrumb antilia-breadcrumb-arrow');

		act(() => ref.current!.expand());
		expect(shown(container)).toEqual(['Home', 'Products', 'Laptops']);
	});

	it('writes an icon of text as text, and iconMarkup as markup', () => {
		const { container } = render(<Breadcrumb items={[
			{ id: 'text', label: 'Text', href: '#text', icon: '<svg data-icon="text"></svg>' },
			{ id: 'markup', label: 'Markup', href: '#markup',
				iconMarkup: '<svg data-icon="markup"></svg>' }
		]} />);

		const text = container.querySelector('a[href="#text"] .antilia-breadcrumb-icon')!;
		expect(text.textContent).toBe('<svg data-icon="text"></svg>');
		expect(text.querySelector('svg')).toBeNull();
		expect(container.querySelector('.antilia-breadcrumb-icon svg[data-icon="markup"]'))
			.not.toBeNull();
	});
});
