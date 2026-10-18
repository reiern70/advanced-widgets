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

import { ANTILIA_LOOK, Dropdown, dropdown, Scrollbar, setHost, standaloneHost, type Host } from '../src/index.js';

const FRUITS = ['Apple', 'Banana', 'Cherry'];

function page(html: string): void {
	document.body.innerHTML = html;
}

function select(options: string[] = FRUITS, attributes = ''): HTMLSelectElement {
	page(`<label for="s">Fruit</label><select id="s" ${attributes}>` +
		options.map((text) => `<option value="${text}">${text}</option>`).join('') + '</select>');
	return document.getElementById('s') as HTMLSelectElement;
}

function key(target: Element, name: string): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true }));
}

function texts(dropdown: Dropdown): string[] {
	return dropdown.visibleItems().map((item) => item.textContent ?? '');
}

afterEach(() => {
	setHost();
	document.body.innerHTML = '';
});

describe('Dropdown', () => {
	it('wraps the select and hides it, keeping it in the page', () => {
		const element = select();
		const instance = new Dropdown(element);

		expect(element.parentElement?.className).toBe('antilia-dropdown');
		expect(element.classList.contains('antilia-dropdown-native')).toBe(true);
		expect(element.getAttribute('aria-hidden')).toBe('true');
		expect(element.tabIndex).toBe(-1);
		expect(instance.toggle.textContent).toBe('Apple');
		expect(instance.toggle.getAttribute('aria-label')).toBe('Fruit');
		expect(texts(instance)).toEqual(FRUITS);
		expect(Dropdown.of('s')).toBe(instance);
	});

	it('uses the wrapper the select is in already, and leaves it on destroy', () => {
		page('<div class="antilia-dropdown" id="w"><select id="s"><option>A</option></select></div>');
		const element = document.getElementById('s') as HTMLSelectElement;
		const instance = new Dropdown(element);

		expect(element.parentElement?.id).toBe('w');
		instance.destroy();

		expect(document.getElementById('w')?.children.length).toBe(1);
		expect(element.parentElement?.id).toBe('w');
		expect(element.hasAttribute('aria-hidden')).toBe(false);
		expect(Dropdown.of(element)).toBeNull();
	});

	it('takes away the wrapper it made on destroy', () => {
		const element = select();
		new Dropdown(element).destroy();

		expect(element.parentElement).toBe(document.body);
		expect(document.querySelector('.antilia-dropdown')).toBeNull();
		expect(element.hasAttribute('class')).toBe(false);
	});

	it('chooses with the keyboard and fires input and change', () => {
		const element = select();
		const instance = new Dropdown(element, { placeholder: 'Choose' });
		const events: string[] = [];
		element.addEventListener('input', () => events.push('input'));
		element.addEventListener('change', () => events.push('change'));

		key(instance.toggle, 'ArrowDown');
		expect(instance.isOpen()).toBe(true);
		key(instance.list, 'ArrowDown');
		key(instance.list, 'Enter');

		expect(element.value).toBe('Banana');
		expect(instance.isOpen()).toBe(false);
		expect(instance.toggle.textContent).toBe('Banana');
		expect(events).toEqual(['input', 'change']);
	});

	it('closes with Escape without choosing', () => {
		const element = select();
		const instance = new Dropdown(element);
		instance.open();
		key(instance.list, 'ArrowDown');
		key(instance.list, 'Escape');

		expect(instance.isOpen()).toBe(false);
		expect(element.value).toBe('Apple');
	});

	it('filters from the threshold on', () => {
		const instance = new Dropdown(select(), { filterThreshold: 3, noMatch: 'Nothing' });
		instance.open();
		const filter = instance.filter!;
		filter.value = 'an';
		filter.dispatchEvent(new Event('input'));

		expect(texts(instance)).toEqual(['Banana']);
		filter.value = 'xyz';
		filter.dispatchEvent(new Event('input'));
		expect(texts(instance)).toEqual([]);
		expect(instance.empty.hidden).toBe(false);
		expect(instance.empty.textContent).toBe('Nothing');
	});

	it('has no filter below the threshold', () => {
		expect(new Dropdown(select()).filter).toBeNull();
	});

	it('toggles options of a multiple select and shows them as chips', () => {
		const element = select(['A', 'B', 'C', 'D', 'E'], 'multiple');
		const instance = new Dropdown(element, { more: '+{0} more' });
		instance.open();
		instance.items.forEach((item) => instance.choose(item));

		expect(Array.from(element.selectedOptions).map((option) => option.value))
			.toEqual(['A', 'B', 'C', 'D', 'E']);
		expect(instance.isOpen()).toBe(true);
		const chips = Array.from(instance.value.children).map((chip) => chip.textContent);
		expect(chips).toEqual(['A', 'B', 'C', '+2 more']);
		expect(instance.toggle.title).toBe('A, B, C, D, E');

		instance.choose(instance.items[0]);
		expect(element.options[0].selected).toBe(false);
	});

	it('deselects an option with the x of its chip or Backspace, firing change', () => {
		const element = select(['A', 'B', 'C'], 'multiple');
		element.options[0].selected = true;
		element.options[2].selected = true;
		const instance = new Dropdown(element, { remove: 'Drop {0}' });
		const changes: string[][] = [];
		element.addEventListener('change', () => changes.push(Array.from(element.selectedOptions,
			(option) => option.value)));

		const remove = instance.value.querySelector<HTMLElement>('.antilia-chip-remove')!;
		expect(remove.getAttribute('aria-label')).toBe('Drop A');
		remove.click();
		expect(instance.isOpen()).toBe(false);
		key(instance.toggle, 'Backspace');

		expect(changes).toEqual([['C'], []]);
		expect(instance.value.querySelector('.antilia-chip')).toBeNull();
	});

	it('shows chips without an x while the select is disabled', () => {
		const element = select(['A', 'B'], 'multiple disabled');
		element.options[1].selected = true;
		const instance = new Dropdown(element);

		expect(instance.value.querySelector('.antilia-chip-text')?.textContent).toBe('B');
		expect(instance.value.querySelector('.antilia-chip-remove')).toBeNull();
	});

	it('shows a choice made by someone else on change and on update()', () => {
		const element = select();
		const instance = new Dropdown(element);
		element.value = 'Cherry';
		element.dispatchEvent(new Event('change'));
		expect(instance.toggle.textContent).toBe('Cherry');

		element.value = 'Banana';
		instance.update();
		expect(instance.toggle.textContent).toBe('Banana');
	});

	it('draws added options and other texts on refresh() and configure()', () => {
		const element = select();
		const instance = new Dropdown(element);
		element.add(new Option('Date', 'Date'));
		instance.refresh();
		expect(texts(instance)).toEqual([...FRUITS, 'Date']);

		instance.configure({ filterThreshold: 0, filter: 'Search' });
		expect(instance.filter?.placeholder).toBe('Search');
		instance.configure({});
		expect(instance.filter).toBeNull();
	});

	it('tells on its toggle that the select is wrong, required or described, also on refresh()', () => {
		const element = select(FRUITS, 'aria-invalid="true" aria-required="true" aria-describedby="help"');
		const instance = new Dropdown(element);
		const toggle = element.parentElement?.querySelector('.antilia-dropdown-toggle') as HTMLElement;

		expect(toggle.getAttribute('aria-invalid')).toBe('true');
		expect(toggle.getAttribute('aria-required')).toBe('true');
		expect(toggle.getAttribute('aria-describedby')).toBe('help');

		element.removeAttribute('aria-invalid');
		element.removeAttribute('aria-describedby');
		instance.refresh();

		expect(toggle.hasAttribute('aria-invalid')).toBe(false);
		expect(toggle.getAttribute('aria-required')).toBe('true');
		expect(toggle.hasAttribute('aria-describedby')).toBe(false);
		instance.destroy();
	});

	it('does not open while the select is disabled', () => {
		const instance = new Dropdown(select(FRUITS, 'disabled'));
		instance.toggle.click();

		expect(instance.isOpen()).toBe(false);
		expect(instance.toggle.disabled).toBe(true);
		expect(instance.wrapper.classList.contains('antilia-dropdown-disabled')).toBe(true);
	});

	it('closes on a click outside', () => {
		const instance = new Dropdown(select());
		instance.open();
		document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

		expect(instance.isOpen()).toBe(false);
	});

	it('rebuilds when a new select is attached in the same wrapper', () => {
		const element = select();
		const first = new Dropdown(element);
		const second = Dropdown.attach('s')!;

		expect(Dropdown.of('s')).toBe(second);
		expect(second.wrapper).toBe(first.wrapper);
		expect(first.wrapper.querySelectorAll('.antilia-dropdown-toggle').length).toBe(1);
	});

	it('stops listening when the host removes the element', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = {
			...standaloneHost,
			onNodeRemoving(listener) {
				removing = listener;
				return () => {
					removing = undefined;
				};
			}
		};
		setHost(host);
		const instance = new Dropdown(select());
		const spy = vi.spyOn(instance, 'open');

		removing!(document.body);
		instance.toggle.click();

		expect(spy).not.toHaveBeenCalled();
		expect(removing).toBeUndefined();
	});

	it('is a Svelte action', () => {
		const element = select();
		const action = dropdown(element, { placeholder: 'Choose' });
		action.update({ filterThreshold: 0 });
		expect(Dropdown.of(element)?.filter).not.toBeNull();

		action.destroy();
		expect(Dropdown.of(element)).toBeNull();
	});

	it('scrolls its list with the slim scroll bars, the filter above them', () => {
		const instance = new Dropdown(select(), { filterThreshold: 0 });

		expect(instance.list.parentElement).toBe(instance.listBox);
		expect(instance.listBox.className).toBe('antilia-scrollbar');
		expect(instance.filter?.nextElementSibling).toBe(instance.listBox);
		expect(Scrollbar.of(instance.list)?.options).toEqual({ axis: 'y', look: ANTILIA_LOOK });

		instance.destroy();
		expect(Scrollbar.of(instance.list)).toBeUndefined();
	});
});
