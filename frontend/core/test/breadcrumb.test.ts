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

import { BOOTSTRAP_LOOK, Breadcrumb, breadcrumb, BREADCRUMB_COLLAPSED_ATTRIBUTE,
	BREADCRUMB_SEPARATORS, collapsedCrumbs } from '../src/index.js';

function crumb(name: string): string {
	return '<li class="antilia-breadcrumb-item"><a class="antilia-breadcrumb-link" href="#' + name +
		'"><span class="antilia-breadcrumb-label">' + name + '</span></a></li>';
}

function page(name: string): string {
	return '<li class="antilia-breadcrumb-item"><span class="antilia-breadcrumb-page" ' +
		'aria-current="page"><span class="antilia-breadcrumb-label">' + name + '</span></span></li>';
}

/**
 * @returns the element of a breadcrumb of crumbs leading to the pages of the names, the last the
 *          page shown
 */
function markup(...names: string[]): HTMLElement {
	document.body.innerHTML = '<nav id="b"><ol class="antilia-breadcrumb-list">' +
		names.slice(0, -1).map(crumb).join('') + page(names[names.length - 1]) + '</ol></nav>';
	return document.getElementById('b')!;
}

function list(): HTMLElement {
	return document.querySelector('.antilia-breadcrumb-list') as HTMLElement;
}

/** @returns the labels of the crumbs shown, "…" for the button */
function shown(): string[] {
	return (Array.from(list().children) as HTMLElement[]).filter((item) => !item.hidden)
		.map((item) => item.classList.contains('antilia-breadcrumb-more') ? '…' :
			item.querySelector('.antilia-breadcrumb-label')!.textContent!);
}

function more(): HTMLButtonElement {
	return document.querySelector('.antilia-breadcrumb-more-button') as HTMLButtonElement;
}

const TRAIL = ['Home', 'Products', 'Computers', 'Laptops', 'Gaming', 'Model X'];

describe('breadcrumb', () => {
	afterEach(() => {
		document.body.innerHTML = '';
	});

	it('collapses the crumbs after the first, showing the last maxItems - 1', () => {
		expect(collapsedCrumbs(6, 4)).toEqual({ from: 1, to: 3 });
		expect(collapsedCrumbs(4, 4)).toBeNull();
		expect(collapsedCrumbs(6, 0)).toBeNull();
		expect(collapsedCrumbs(6)).toBeNull();
		// two show at least: the first and the page shown
		expect(collapsedCrumbs(6, 1)).toEqual({ from: 1, to: 5 });
	});

	it('gives the breadcrumb its classes, its name and the separator', () => {
		const element = markup('Home', 'Laptops');
		const instance = new Breadcrumb(element);

		expect(element.className).toBe('antilia-breadcrumb antilia-breadcrumb-chevron');
		expect(element.getAttribute('aria-label')).toBe('Breadcrumb');
		expect(list().children[1].className)
			.toBe('antilia-breadcrumb-item antilia-breadcrumb-current');
		expect(list().children[0].classList.contains('antilia-breadcrumb-current')).toBe(false);
		expect(Breadcrumb.of('b')).toBe(instance);
		expect(instance.isCollapsed()).toBe(false);
		expect(document.querySelector('.antilia-breadcrumb-more')).toBeNull();

		instance.configure({ separator: 'slash' });
		expect(element.className).toBe('antilia-breadcrumb antilia-breadcrumb-slash');
		instance.configure({ separator: 'nonsense' as never });
		expect(instance.separator()).toBe('chevron');
		expect(BREADCRUMB_SEPARATORS).toEqual(['chevron', 'slash', 'arrow']);
	});

	it('keeps the name the page gave, unless told another one', () => {
		const element = markup('Home', 'Laptops');
		element.setAttribute('aria-label', 'Sie sind hier');
		const instance = new Breadcrumb(element);
		expect(element.getAttribute('aria-label')).toBe('Sie sind hier');

		instance.configure({ texts: { label: 'You are here' } });
		expect(element.getAttribute('aria-label')).toBe('You are here');
	});

	it('hides the crumbs in the middle behind a button saying how many there are', () => {
		const instance = new Breadcrumb(markup(...TRAIL), { maxItems: 4 });

		expect(shown()).toEqual(['Home', '…', 'Laptops', 'Gaming', 'Model X']);
		expect(instance.isCollapsed()).toBe(true);
		const hidden = list().querySelectorAll(`[${BREADCRUMB_COLLAPSED_ATTRIBUTE}]`);
		expect(hidden).toHaveLength(2);
		const button = more();
		expect(button.type).toBe('button');
		expect(button.textContent).toBe('…');
		expect(button.getAttribute('aria-label')).toBe('Show the whole path (2 more)');
		expect(button.title).toBe('Show the whole path (2 more)');
		expect(button.className).toBe('antilia-breadcrumb-more-button');
		expect(button.parentElement!.className).toBe('antilia-breadcrumb-item antilia-breadcrumb-more');
		expect(instance.items()).toHaveLength(6);
	});

	it('shows them in place on a click, the focus on the first of them', () => {
		const onExpand = vi.fn();
		const instance = new Breadcrumb(markup(...TRAIL), { maxItems: 3,
			texts: { more: '{0} hidden' } });
		expect(more().getAttribute('aria-label')).toBe('3 hidden');

		more().click();

		expect(shown()).toEqual(TRAIL);
		expect(instance.isCollapsed()).toBe(false);
		expect(document.activeElement).toBe(document.querySelector('a[href="#Products"]'));
		expect(more().parentElement!.hidden).toBe(true);

		// the page renders again: the trail stays as the user left it
		instance.refresh();
		expect(shown()).toEqual(TRAIL);
		instance.configure({ onExpand });
		instance.expand();
		expect(onExpand).not.toHaveBeenCalled();
	});

	it('collapses again when the crumbs change, as when the user went back', () => {
		const instance = new Breadcrumb(markup(...TRAIL), { maxItems: 3 });
		instance.expand(false);
		expect(shown()).toEqual(TRAIL);

		// back to Gaming: the page takes Model X away and Gaming is the page shown
		list().lastElementChild!.remove();
		instance.refresh();

		expect(shown()).toEqual(['Home', '…', 'Laptops', 'Gaming']);
		expect(list().lastElementChild!.classList.contains('antilia-breadcrumb-current')).toBe(true);

		// short enough: the button goes
		list().lastElementChild!.remove();
		list().lastElementChild!.remove();
		instance.refresh();
		expect(shown()).toEqual(['Home', 'Products', 'Computers']);
	});

	it('follows the crumbs the page adds by itself', async () => {
		new Breadcrumb(markup('Home', 'Products', 'Laptops'), { maxItems: 3 });
		expect(shown()).toEqual(['Home', 'Products', 'Laptops']);

		list().insertAdjacentHTML('beforeend', page('Gaming'));
		await Promise.resolve();

		expect(shown()).toEqual(['Home', '…', 'Laptops', 'Gaming']);
	});

	it('takes the button the server rendered, the crumbs already hidden', () => {
		markup(...TRAIL);
		const server = document.createElement('li');
		server.className = 'antilia-breadcrumb-item antilia-breadcrumb-more';
		server.innerHTML = '<button type="button" class="antilia-breadcrumb-more-button">…</button>';
		list().insertBefore(server, list().children[1]);
		[1, 2].forEach((index) => {
			const hidden = list().children[index + 1] as HTMLElement;
			hidden.hidden = true;
			hidden.setAttribute(BREADCRUMB_COLLAPSED_ATTRIBUTE, '');
		});

		const instance = Breadcrumb.attach('b', { maxItems: 4 })!;

		expect(list().querySelectorAll('.antilia-breadcrumb-more')).toHaveLength(1);
		expect(shown()).toEqual(['Home', '…', 'Laptops', 'Gaming', 'Model X']);
		expect(instance.items()).toHaveLength(6);

		more().click();
		expect(shown()).toEqual(TRAIL);

		// taken away: every crumb shows, the button the page has stays hidden
		instance.destroy();
		expect(Breadcrumb.of('b')).toBeNull();
		expect(server.isConnected).toBe(true);
		expect(server.hidden).toBe(true);
		expect(Breadcrumb.attach('nothing')).toBeNull();
	});

	it('shows every crumb and takes its button away when destroyed', () => {
		const element = markup(...TRAIL);
		const action = breadcrumb(element, { maxItems: 2 });
		expect(shown()).toEqual(['Home', '…', 'Model X']);

		action.update({ maxItems: 5 });
		expect(shown()).toEqual(['Home', '…', 'Computers', 'Laptops', 'Gaming', 'Model X']);

		action.destroy();
		expect(shown()).toEqual(TRAIL);
		expect(document.querySelector('.antilia-breadcrumb-more')).toBeNull();
		more()?.click();
	});

	it('does not hide the crumbs the page hid itself', () => {
		markup(...TRAIL);
		(list().children[3] as HTMLElement).hidden = true;
		const instance = new Breadcrumb(document.getElementById('b')!, { maxItems: 6 });

		instance.configure({ maxItems: 2 });
		instance.expand();

		expect(shown()).toEqual(['Home', 'Products', 'Computers', 'Gaming', 'Model X']);
	});

	it('puts the parts in the look of the page', () => {
		const element = markup('Home', 'Products', 'Laptops');
		const instance = new Breadcrumb(element, { look: BOOTSTRAP_LOOK, maxItems: 2 });

		expect(list().className).toBe('antilia-breadcrumb-list breadcrumb');
		expect(list().children[0].className).toBe('antilia-breadcrumb-item breadcrumb-item');
		expect(list().lastElementChild!.className)
			.toBe('antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-current active');
		expect(more().parentElement!.className)
			.toBe('antilia-breadcrumb-item breadcrumb-item antilia-breadcrumb-more');

		// back in the components' own look, Bootstrap's classes go
		instance.configure({ look: undefined });
		instance.options.look = undefined;
		instance.refresh();
		expect(list().className).toBe('antilia-breadcrumb-list');
		expect(list().lastElementChild!.className)
			.toBe('antilia-breadcrumb-item antilia-breadcrumb-current');
		expect(more().parentElement!.className).toBe('antilia-breadcrumb-item antilia-breadcrumb-more');
	});

	it('works on a breadcrumb without a list', () => {
		document.body.innerHTML = '<nav id="b"></nav>';
		const instance = new Breadcrumb(document.getElementById('b')!, { maxItems: 2 });
		expect(instance.items()).toEqual([]);
		instance.expand();
		expect(instance.isCollapsed()).toBe(false);
	});
});
