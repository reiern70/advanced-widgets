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
import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, Breadcrumb, LOOK_CONTEXT, type BreadcrumbItem } from '../src/index.js';

const TRAIL = ['Home', 'Products', 'Computers', 'Laptops', 'Gaming', 'Model X'];

function trail(names: string[]): BreadcrumbItem[] {
	return names.map((name) => ({ id: name, label: name, href: '#' + name }));
}

/** @returns the labels of the crumbs shown, "…" for the button */
function shown(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll<HTMLElement>('.antilia-breadcrumb-list > li'))
		.filter((item) => !item.hidden)
		.map((item) => item.classList.contains('antilia-breadcrumb-more') ? '…' :
			item.querySelector('.antilia-breadcrumb-label')!.textContent!);
}

afterEach(() => {
	cleanup();
});

describe('Breadcrumb', () => {
	it('renders the crumbs, the last one the page shown', () => {
		const onclick = vi.fn();
		const onselect = vi.fn();
		const { container } = render(Breadcrumb, { props: { id: 'b', class: 'mine', onselect, items: [
			{ id: 'home', label: 'Home', href: '#home', icon: '<svg data-icon="home"></svg>',
				tooltip: 'Go home' },
			{ id: 'all', label: 'All', onclick },
			{ id: 'group', label: 'Group' },
			{ id: 'off', label: 'Off', href: '#off', disabled: true },
			{ id: 'here', label: 'Here', href: '#here' }
		] } });

		const nav = container.querySelector('#b')!;
		expect(nav.tagName).toBe('NAV');
		expect(nav.className).toBe('antilia-breadcrumb antilia-breadcrumb-chevron mine');
		expect(nav.getAttribute('aria-label')).toBe('Breadcrumb');
		const home = nav.querySelector('a[href="#home"]')!;
		expect(home.className).toBe('antilia-breadcrumb-link');
		expect(home.getAttribute('title')).toBe('Go home');
		expect(home.querySelector('.antilia-breadcrumb-icon svg[data-icon="home"]')).not.toBeNull();
		expect(nav.querySelector('.antilia-breadcrumb-text')!.textContent!.trim()).toBe('Group');
		const off = nav.querySelectorAll('a.antilia-breadcrumb-link')[1];
		expect(off.hasAttribute('href')).toBe(false);
		expect(off.getAttribute('aria-disabled')).toBe('true');
		const here = nav.querySelector('[aria-current="page"]')!;
		expect(here.className).toBe('antilia-breadcrumb-page');
		expect(here.parentElement!.className).toBe('antilia-breadcrumb-item antilia-breadcrumb-current');

		fireEvent.click(nav.querySelector('button.antilia-breadcrumb-link')!);
		expect(onclick).toHaveBeenCalledTimes(1);
		expect(onselect).toHaveBeenCalledWith(expect.objectContaining({ id: 'all' }));
	});

	it('collapses a long trail, shows it on a click on "…" and collapses it again when it changes',
		async () => {
			const onexpand = vi.fn();
			const { container, rerender } = render(Breadcrumb, { props: { items: trail(TRAIL),
				maxItems: 4, texts: { more: '{0} more' }, onexpand } });

			expect(shown(container)).toEqual(['Home', '…', 'Laptops', 'Gaming', 'Model X']);
			const more = container.querySelector<HTMLButtonElement>('.antilia-breadcrumb-more-button')!;
			expect(more.getAttribute('aria-label')).toBe('2 more');

			await fireEvent.click(more);
			expect(shown(container)).toEqual(TRAIL);
			expect(onexpand).toHaveBeenCalledTimes(1);
			expect(document.activeElement).toBe(container.querySelector('a[href="#Products"]'));

			await rerender({ items: trail(TRAIL.slice(0, 5)) });
			flushSync();
			expect(shown(container)).toEqual(['Home', '…', 'Computers', 'Laptops', 'Gaming']);
		});

	it('follows its separator and its look', async () => {
		const { container, rerender } = render(Breadcrumb, {
			props: { items: trail(['Home', 'Products', 'Laptops']) },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });

		const list = container.querySelector('ol')!;
		expect(list.className).toBe('antilia-breadcrumb-list breadcrumb');
		expect(list.lastElementChild!.className)
			.toBe('antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-current active');

		await rerender({ separator: 'slash' });
		expect(container.querySelector('nav')!.className)
			.toBe('antilia-breadcrumb antilia-breadcrumb-slash');
	});
});
