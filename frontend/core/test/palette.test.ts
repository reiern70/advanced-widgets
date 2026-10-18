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

import { insertAt, moveBy, Palette, palette, PALETTE_MATCHERS, setHost, type Host,
	type PaletteChoice, standaloneHost } from '../src/index.js';

const FRUITS: PaletteChoice[] = ['Apple', 'Banana', 'Cherry', 'Date'].map((label) => ({
	value: label.toLowerCase(),
	label
}));

function element(): HTMLDivElement {
	document.body.innerHTML = '<div id="p"></div>';
	return document.getElementById('p') as HTMLDivElement;
}

function select(selected: string[] = [], attributes = ''): HTMLSelectElement {
	document.body.innerHTML = `<form><select id="s" name="fruits" multiple ${attributes}>` +
		FRUITS.map((fruit) => `<option value="${fruit.value}"` +
			(selected.includes(fruit.value) ? ' selected' : '') + `>${fruit.label}</option>`)
			.join('') + '</select></form>';
	return document.getElementById('s') as HTMLSelectElement;
}

function label(item: HTMLLIElement): string {
	return item.querySelector('.antilia-palette-text')?.textContent ?? '';
}

function texts(list: { items: HTMLLIElement[] }): string[] {
	return list.items.map(label);
}

function item(list: { items: HTMLLIElement[] }, text: string): HTMLLIElement {
	return list.items.find((each) => label(each) === text)!;
}

function numbers(list: { items: HTMLLIElement[] }): string[] {
	return list.items.map((each) =>
		each.querySelector('.antilia-palette-number')?.textContent ?? '');
}

function click(target: Element, init: MouseEventInit = {}): void {
	target.dispatchEvent(new MouseEvent('click', { bubbles: true, ...init }));
}

function key(target: Element, name: string, init: KeyboardEventInit = {}): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true, ...init }));
}

function pointer(target: EventTarget, type: string, x: number, y: number): void {
	const event = new MouseEvent(type, { bubbles: true, clientX: x, clientY: y, button: 0 });
	Object.defineProperty(event, 'pointerId', { value: 1 });
	target.dispatchEvent(event);
}

function rect(target: Element, top: number, height: number, left = 0, width = 100): void {
	target.getBoundingClientRect = () => ({ top, bottom: top + height, left, right: left + width,
		height, width, x: left, y: top, toJSON: () => ({}) });
}

afterEach(() => {
	setHost();
	document.body.innerHTML = '';
});

describe('moveBy', () => {
	it('moves values up and down, a block as a whole, stopping at the edges', () => {
		const list = ['a', 'b', 'c', 'd', 'e'];
		expect(moveBy(list, new Set(['c']), false)).toEqual(['a', 'c', 'b', 'd', 'e']);
		expect(moveBy(list, new Set(['b', 'c']), true)).toEqual(['a', 'd', 'b', 'c', 'e']);
		expect(moveBy(list, new Set(['a', 'c']), false)).toEqual(['a', 'c', 'b', 'd', 'e']);
		expect(moveBy(list, new Set(['d', 'e']), true)).toEqual(list);
	});
});

describe('insertAt', () => {
	it('puts values in front of an index, taking them from where they were', () => {
		expect(insertAt(['a', 'b', 'c'], ['x'], 1)).toEqual(['a', 'x', 'b', 'c']);
		expect(insertAt(['a', 'b', 'c'], ['a'], 3)).toEqual(['b', 'c', 'a']);
		expect(insertAt(['a', 'b', 'c'], ['c', 'b'], 0)).toEqual(['c', 'b', 'a']);
	});
});

describe('Palette', () => {
	it('draws the available and the chosen ones with the arrows between them', () => {
		const onChange = vi.fn();
		const instance = new Palette(element(), { choices: FRUITS, selected: ['cherry'], onChange });

		expect(instance.element.classList.contains('antilia-palette')).toBe(true);
		expect(texts(instance.available)).toEqual(['Apple', 'Banana', 'Date']);
		expect(texts(instance.chosen)).toEqual(['Cherry']);
		expect(instance.available.title.textContent).toBe('Available');
		expect(instance.chosen.title.textContent).toBe('Selected');
		expect(instance.available.list.getAttribute('role')).toBe('listbox');
		expect(instance.buttons.add.getAttribute('aria-label')).toBe('Add');
		expect(instance.buttons.add.disabled).toBe(true);
		expect(instance.buttons.addAll.disabled).toBe(false);
		expect(instance.order.hidden).toBe(true);
		expect(Palette.of('p')).toBe(instance);
		expect(onChange).not.toHaveBeenCalled();
	});

	it('moves the highlighted ones with the arrows', () => {
		const onChange = vi.fn();
		const instance = new Palette(element(), { choices: FRUITS, onChange });

		click(item(instance.available, 'Banana'));
		click(item(instance.available, 'Date'), { ctrlKey: true });
		expect(instance.buttons.add.disabled).toBe(false);
		click(instance.buttons.add);

		expect(onChange).toHaveBeenLastCalledWith(['banana', 'date']);
		expect(texts(instance.available)).toEqual(['Apple', 'Cherry']);
		expect(texts(instance.chosen)).toEqual(['Banana', 'Date']);
		// what moved stays highlighted on its new side
		expect(item(instance.chosen, 'Banana').getAttribute('aria-selected')).toBe('true');

		click(instance.buttons.remove);
		expect(onChange).toHaveBeenLastCalledWith([]);

		click(instance.buttons.addAll);
		expect(onChange).toHaveBeenLastCalledWith(['apple', 'banana', 'cherry', 'date']);
		click(instance.buttons.removeAll);
		expect(instance.value()).toEqual([]);
	});

	it('highlights a range with Shift and moves an item with a double click', () => {
		const instance = new Palette(element(), { choices: FRUITS });

		click(item(instance.available, 'Apple'));
		click(item(instance.available, 'Cherry'), { shiftKey: true });
		expect(Array.from(instance.available.highlighted)).toEqual(['apple', 'banana', 'cherry']);

		item(instance.available, 'Date').dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
		expect(instance.value()).toEqual(['date']);
	});

	it('keeps the order of the choices unless the order matters', () => {
		const unordered = new Palette(element(), { choices: FRUITS, selected: ['date', 'apple'] });
		expect(unordered.value()).toEqual(['apple', 'date']);
		click(item(unordered.available, 'Banana'));
		click(unordered.buttons.add);
		expect(unordered.value()).toEqual(['apple', 'banana', 'date']);

		const ordered = new Palette(element(), { choices: FRUITS, selected: ['date', 'apple'],
			ordered: true });
		expect(ordered.value()).toEqual(['date', 'apple']);
		expect(ordered.order.hidden).toBe(false);
		click(item(ordered.available, 'Banana'));
		click(ordered.buttons.add);
		expect(ordered.value()).toEqual(['date', 'apple', 'banana']);
	});

	it('moves the chosen ones up and down while the order matters', () => {
		const onChange = vi.fn();
		const instance = new Palette(element(), { choices: FRUITS, ordered: true,
			selected: ['apple', 'banana', 'cherry'], onChange });

		expect(instance.buttons.up.disabled).toBe(true);
		click(item(instance.chosen, 'Cherry'));
		expect(instance.buttons.down.disabled).toBe(true);
		click(instance.buttons.up);
		expect(onChange).toHaveBeenLastCalledWith(['apple', 'cherry', 'banana']);
		click(instance.buttons.up);
		expect(instance.value()).toEqual(['cherry', 'apple', 'banana']);
		expect(instance.buttons.up.disabled).toBe(true);

		key(instance.chosen.list, 'ArrowDown', { altKey: true });
		expect(instance.value()).toEqual(['apple', 'cherry', 'banana']);
	});

	it('works with the keyboard', () => {
		const instance = new Palette(element(), { choices: FRUITS });
		const list = instance.available.list;

		key(list, 'ArrowDown');
		expect(instance.available.active).toBe('apple');
		key(list, 'ArrowDown');
		key(list, 'ArrowDown', { shiftKey: true });
		expect(Array.from(instance.available.highlighted)).toEqual(['banana', 'cherry']);
		key(list, 'Enter');
		expect(instance.value()).toEqual(['banana', 'cherry']);
		// the keyboard stays on the list, on the next item
		expect(instance.available.active).toBe('date');

		key(list, 'Home');
		key(list, ' ');
		expect(Array.from(instance.available.highlighted)).toEqual(['apple']);
		key(list, 'End');
		key(list, 'a', { ctrlKey: true });
		expect(Array.from(instance.available.highlighted)).toEqual(['apple', 'date']);

		document.body.focus();
		key(list, 'ArrowRight');
		expect(document.activeElement).toBe(instance.chosen.list);
		// what moved stays highlighted there: Space takes banana out of it
		key(instance.chosen.list, ' ');
		key(instance.chosen.list, 'Enter');
		expect(instance.value()).toEqual(['banana']);
		key(instance.chosen.list, 'ArrowLeft');
		expect(document.activeElement).toBe(list);
	});

	it('leaves disabled choices where they are', () => {
		const choices = FRUITS.map((fruit) => ({ ...fruit, disabled: fruit.value === 'banana' }));
		const instance = new Palette(element(), { choices, selected: ['apple'] });

		click(item(instance.available, 'Banana'));
		expect(instance.available.highlighted.size).toBe(0);
		click(instance.buttons.addAll);
		expect(instance.value()).toEqual(['apple', 'cherry', 'date']);
		expect(texts(instance.available)).toEqual(['Banana']);
		expect(instance.buttons.addAll.disabled).toBe(true);
	});

	it('does nothing while disabled', () => {
		const onChange = vi.fn();
		const instance = new Palette(element(), { choices: FRUITS, disabled: true, onChange });

		expect(instance.element.classList.contains('antilia-palette-disabled')).toBe(true);
		expect(instance.buttons.addAll.disabled).toBe(true);
		expect(instance.available.list.tabIndex).toBe(-1);
		click(item(instance.available, 'Apple'));
		key(instance.available.list, 'Enter');
		instance.transfer(instance.available, ['apple']);
		expect(onChange).not.toHaveBeenCalled();

		instance.setDisabled(false);
		click(instance.buttons.addAll);
		expect(onChange).toHaveBeenCalled();
	});

	it('filters the available ones from the threshold on', () => {
		const instance = new Palette(element(), { choices: FRUITS, filterThreshold: 4,
			filter: 'Find' });

		expect(instance.filter?.placeholder).toBe('Find');
		instance.filter!.value = 'an';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(texts(instance.available)).toEqual(['Banana']);
		// add all adds those shown
		click(instance.buttons.addAll);
		expect(instance.value()).toEqual(['banana']);
		instance.filter!.value = 'xyz';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(instance.available.empty.hidden).toBe(false);

		instance.configure({ filterThreshold: 10 });
		expect(instance.filter).toBeNull();
		instance.configure({ filterThreshold: 0, filterable: false });
		expect(instance.filter).toBeNull();
		instance.configure({ filterable: true });
		expect(instance.filter).not.toBeNull();
	});

	it('underlines what the filter matched, anywhere unless set otherwise', () => {
		const instance = new Palette(element(), { choices: FRUITS, filterThreshold: 0,
			selected: ['date'] });
		const search = (text: string) => {
			instance.filter!.value = text;
			instance.filter!.dispatchEvent(new Event('input'));
		};
		const matched = (list: { items: HTMLLIElement[] }) => list.items.map((each) =>
			Array.from(each.querySelectorAll('.antilia-palette-match'), (part) => part.textContent)
				.join('|'));

		search(' AN ');
		expect(texts(instance.available)).toEqual(['Banana']);
		expect(matched(instance.available)).toEqual(['an']);
		expect(instance.available.items[0].querySelector('.antilia-palette-text')!.innerHTML)
			.toBe('B<span class="antilia-palette-match">an</span>ana');
		search('');
		expect(matched(instance.available)).toEqual(['', '', '']);

		instance.configure({ match: 'start' });
		search('a');
		expect(texts(instance.available)).toEqual(['Apple']);
		expect(matched(instance.available)).toEqual(['A']);

		instance.configure({ match: 'end' });
		search('Y');
		expect(texts(instance.available)).toEqual(['Cherry']);
		expect(matched(instance.available)).toEqual(['y']);
		// the chosen ones are not filtered, nor underlined
		search('e');
		expect(texts(instance.available)).toEqual(['Apple']);
		expect(matched(instance.chosen)).toEqual(['']);

		instance.configure({ match: 'nothing such' });
		search('an');
		expect(texts(instance.available)).toEqual(['Banana']);
	});

	it('matches the start of the words, in their order, underlining each', () => {
		const words = PALETTE_MATCHERS.words;
		expect(words('South Georgia & South Sandwich Islands', 's ge')).toEqual([[0, 1], [6, 8]]);
		expect(words('South Georgia & South Sandwich Islands', 'S  s  i')).toEqual(
			[[0, 1], [16, 17], [31, 32]]);
		expect(words('Guinea-Bissau', 'bis')).toEqual([[7, 10]]);
		expect(words('Côte d’Ivoire', 'cô iv')).toEqual([[0, 2], [7, 9]]);
		expect(words('South Georgia', 'ge s')).toBeNull();
		expect(words('South Georgia', 'outh')).toBeNull();
		expect(words('New Zealand', 'new new')).toBeNull();

		const choices = ['New Zealand', 'Papua New Guinea', 'Niue'].map((label) =>
			({ value: label, label }));
		const instance = new Palette(element(), { choices, filterThreshold: 0, match: 'words' });
		instance.filter!.value = 'n g';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(texts(instance.available)).toEqual(['Papua New Guinea']);
		expect(instance.available.items[0].querySelector('.antilia-palette-text')!.innerHTML)
			.toBe('Papua <span class="antilia-palette-match">N</span>ew ' +
				'<span class="antilia-palette-match">G</span>uinea');
	});

	it('takes a matcher of its own, or the name of one put among the matchers', () => {
		// the first letter of each word
		const initials = (label: string, text: string): Array<[number, number]> | null => {
			const starts = Array.from(label.matchAll(/\b\w/g), (found) => found.index!);
			const parts: Array<[number, number]> = [];
			for (const letter of text.toLowerCase()) {
				const at = starts.find((start) => label[start].toLowerCase() === letter &&
					!parts.some(([from]) => from === start));
				if (at === undefined) {
					return null;
				}
				parts.push([at, at + 1]);
			}
			return parts;
		};
		const choices = ['New York', 'New Delhi', 'York'].map((label) => ({ value: label, label }));
		const instance = new Palette(element(), { choices, filterThreshold: 0, match: initials });
		instance.filter!.value = 'ny';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(texts(instance.available)).toEqual(['New York']);
		expect(Array.from(instance.available.items[0].querySelectorAll('.antilia-palette-match'),
			(part) => part.textContent)).toEqual(['N', 'Y']);

		Palette.matchers.initials = initials;
		try {
			instance.configure({ match: 'initials' });
			instance.filter!.value = 'd';
			instance.filter!.dispatchEvent(new Event('input'));
			expect(texts(instance.available)).toEqual(['New Delhi']);
		} finally {
			delete Palette.matchers.initials;
		}
	});

	it('underlines only the parts of the label a matcher gives, in order', () => {
		const instance = new Palette(element(), { choices: FRUITS, filterThreshold: 0,
			match: () => [[4, 99], [-3, 1], [2, 2], [3, 5]] });
		instance.filter!.value = 'x';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(texts(instance.available)).toEqual(['Apple', 'Banana', 'Cherry', 'Date']);
		expect(instance.available.items[1].querySelector('.antilia-palette-text')!.innerHTML)
			.toBe('<span class="antilia-palette-match">B</span>an' +
				'<span class="antilia-palette-match">an</span>' +
				'<span class="antilia-palette-match">a</span>');
	});

	it('takes other texts, choices and chosen ones', () => {
		const instance = new Palette(element(), { choices: FRUITS, selected: ['apple'] });

		instance.configure({ availableTitle: 'Frei', selectedTitle: 'Gewählt', add: 'Hinzufügen' });
		expect(instance.available.title.textContent).toBe('Frei');
		expect(instance.buttons.add.title).toBe('Hinzufügen');
		expect(instance.value()).toEqual(['apple']);

		instance.refresh(FRUITS.slice(1));
		expect(instance.value()).toEqual([]);
		expect(texts(instance.available)).toEqual(['Banana', 'Cherry', 'Date']);

		instance.update(['date', 'banana', 'unknown']);
		expect(instance.value()).toEqual(['banana', 'date']);
		instance.configure({ ordered: true });
		instance.update(['date', 'banana']);
		expect(instance.value()).toEqual(['date', 'banana']);
		instance.configure({ ordered: false });
		expect(instance.value()).toEqual(['banana', 'date']);
	});

	it('shows the numbers of the rows when asked to', () => {
		const instance = new Palette(element(), { choices: FRUITS, selected: ['cherry', 'apple'],
			ordered: true, numbered: true });
		expect(instance.element.classList.contains('antilia-palette-numbered')).toBe(true);
		expect(numbers(instance.available)).toEqual(['1.', '2.']);
		expect(numbers(instance.chosen)).toEqual(['1.', '2.']);
		expect(texts(instance.chosen)).toEqual(['Cherry', 'Apple']);
		expect(item(instance.chosen, 'Apple').querySelector('.antilia-palette-number')!
			.getAttribute('aria-hidden')).toBe('true');

		click(item(instance.chosen, 'Apple'));
		click(instance.buttons.up);
		expect(texts(instance.chosen)).toEqual(['Apple', 'Cherry']);
		expect(numbers(instance.chosen)).toEqual(['1.', '2.']);
		click(item(instance.available, 'Date'));
		click(instance.buttons.add);
		expect(numbers(instance.chosen)).toEqual(['1.', '2.', '3.']);

		instance.configure({ numbered: false });
		expect(instance.element.classList.contains('antilia-palette-numbered')).toBe(false);
		expect(instance.chosen.items[0].querySelector('.antilia-palette-number')).toBeNull();
		instance.destroy();
		expect(instance.element.hasAttribute('class')).toBe(false);
	});

	it('reorders the chosen ones by dragging them', () => {
		const onChange = vi.fn();
		const instance = new Palette(element(), { choices: FRUITS, ordered: true,
			selected: ['apple', 'banana', 'cherry'], onChange });
		rect(instance.available.box, 0, 200, 0, 100);
		rect(instance.chosen.box, 0, 200, 200, 100);
		instance.chosen.items.forEach((each, index) => rect(each, 30 + index * 20, 20, 200, 100));

		pointer(item(instance.chosen, 'Cherry'), 'pointerdown', 250, 75);
		pointer(document, 'pointermove', 250, 72);
		expect(document.querySelector('.antilia-palette-ghost')).toBeNull();
		pointer(document, 'pointermove', 250, 35);
		expect(document.querySelector('.antilia-palette-ghost')?.textContent).toBe('Cherry');
		expect(instance.chosen.list.firstElementChild?.className).toBe('antilia-palette-marker');
		expect(instance.chosen.box.classList.contains('antilia-palette-over')).toBe(true);
		pointer(document, 'pointerup', 250, 35);

		expect(onChange).toHaveBeenLastCalledWith(['cherry', 'apple', 'banana']);
		expect(document.querySelector('.antilia-palette-ghost')).toBeNull();
		expect(document.querySelector('.antilia-palette-marker')).toBeNull();
	});

	it('adds and removes by dragging to the other list', () => {
		const instance = new Palette(element(), { choices: FRUITS, ordered: true,
			selected: ['apple', 'banana'] });
		rect(instance.available.box, 0, 200, 0, 100);
		rect(instance.chosen.box, 0, 200, 200, 100);
		instance.chosen.items.forEach((each, index) => rect(each, 30 + index * 20, 20, 200, 100));

		// dropped between apple and banana
		pointer(item(instance.available, 'Date'), 'pointerdown', 50, 50);
		pointer(document, 'pointermove', 250, 52);
		pointer(document, 'pointerup', 250, 52);
		expect(instance.value()).toEqual(['apple', 'date', 'banana']);

		pointer(item(instance.chosen, 'Apple'), 'pointerdown', 250, 40);
		pointer(document, 'pointermove', 50, 40);
		pointer(document, 'pointerup', 50, 40);
		expect(instance.value()).toEqual(['date', 'banana']);

		// dropped outside both lists, nothing happens
		pointer(item(instance.chosen, 'Date'), 'pointerdown', 250, 40);
		pointer(document, 'pointermove', 150, 400);
		pointer(document, 'pointerup', 150, 400);
		expect(instance.value()).toEqual(['date', 'banana']);
	});

	it('draws long lists a page at a time, more as they scroll to their end', () => {
		const many: PaletteChoice[] = Array.from({ length: 75 }, (_value, index) => ({
			value: 'c' + index,
			label: 'Choice ' + index
		}));
		const instance = new Palette(element(), { choices: many });
		const list = instance.available.list;

		expect(instance.available.items.length).toBe(30);
		expect(instance.more('available')).toBe(true);
		expect(instance.available.items.length).toBe(60);
		expect(label(instance.available.items[59])).toBe('Choice 59');

		// scrolled near the end, the rest is drawn
		Object.defineProperty(list, 'clientHeight', { configurable: true, value: 200 });
		Object.defineProperty(list, 'scrollHeight', { configurable: true, value: 1200 });
		list.scrollTop = 1000;
		list.dispatchEvent(new Event('scroll'));
		expect(instance.available.items.length).toBe(75);
		expect(instance.more('available')).toBe(false);
		Object.defineProperty(list, 'clientHeight', { configurable: true, value: 0 });

		// the filter searches all choices, drawn or not, and draws its matches from the first
		instance.filter!.value = 'Choice 7';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(texts(instance.available)).toEqual(['Choice 7', 'Choice 70', 'Choice 71',
			'Choice 72', 'Choice 73', 'Choice 74']);
		instance.filter!.value = '';
		instance.filter!.dispatchEvent(new Event('input'));
		expect(instance.available.items.length).toBe(30);

		// the keyboard draws the rows it goes to
		key(list, 'End');
		expect(instance.available.items.length).toBe(75);
		expect(instance.available.active).toBe('c74');

		// add all adds them all, drawn or not
		instance.filter!.dispatchEvent(new Event('input'));
		click(instance.buttons.addAll);
		expect(instance.value().length).toBe(75);
		expect(instance.chosen.items.length).toBeGreaterThanOrEqual(30);
		expect(instance.available.items.length).toBe(0);
	});

	it('draws a page of the size asked for, or all rows for 0', () => {
		const many: PaletteChoice[] = Array.from({ length: 12 }, (_value, index) => ({
			value: 'c' + index,
			label: 'Choice ' + index
		}));
		const instance = new Palette(element(), { choices: many, pageSize: 5, numbered: true });

		expect(instance.available.items.length).toBe(5);
		instance.more('available');
		expect(numbers(instance.available).slice(-2)).toEqual(['9.', '10.']);
		// Shift with the arrows highlights past the rows drawn
		click(instance.available.items[9]);
		key(instance.available.list, 'ArrowDown', { shiftKey: true });
		key(instance.available.list, 'ArrowDown', { shiftKey: true });
		expect(instance.available.items.length).toBe(12);
		expect(Array.from(instance.available.highlighted)).toEqual(['c9', 'c10', 'c11']);

		instance.configure({ pageSize: 0 });
		expect(instance.chosen.items.length).toBe(0);
		instance.update(many.map((choice) => choice.value));
		expect(instance.chosen.items.length).toBe(12);
	});

	it('takes the palette away on destroy', () => {
		const instance = new Palette(element(), { choices: FRUITS });

		instance.destroy();
		expect(document.getElementById('p')?.children.length).toBe(0);
		expect(document.getElementById('p')?.hasAttribute('class')).toBe(false);
		expect(Palette.of('p')).toBeNull();
	});
});

describe('Palette over a select', () => {
	it('hides the select and takes its options and its choice', () => {
		const element = select(['banana']);
		const instance = Palette.attach('s')!;

		expect(element.parentElement?.className).toBe('antilia-palette');
		expect(element.classList.contains('antilia-palette-native')).toBe(true);
		expect(element.getAttribute('aria-hidden')).toBe('true');
		expect(texts(instance.chosen)).toEqual(['Banana']);
		expect(Palette.of(element)).toBe(instance);
		expect(Palette.attach('nothing')).toBeNull();
	});

	it('writes the choice into the select, in its order, and fires its events', () => {
		const element = select(['banana']);
		const instance = Palette.attach(element, { ordered: true, selected: ['banana'] })!;
		const events: string[] = [];
		element.addEventListener('input', () => events.push('input'));
		element.addEventListener('change', () => events.push('change'));

		click(item(instance.available, 'Date'));
		click(instance.buttons.add);
		click(item(instance.available, 'Apple'));
		click(instance.buttons.add);
		click(instance.buttons.up);

		expect(events).toEqual(['input', 'change', 'input', 'change', 'input', 'change']);
		const posted = new FormData(element.form!).getAll('fruits');
		expect(posted).toEqual(['banana', 'apple', 'date']);
		// the available ones keep the order of the choices
		expect(texts(instance.available)).toEqual(['Cherry']);
	});

	it('takes the order of the chosen ones from the options', () => {
		select(['apple', 'cherry']);
		const instance = Palette.attach('s', { ordered: true, selected: ['cherry', 'apple'] })!;

		expect(texts(instance.chosen)).toEqual(['Cherry', 'Apple']);
	});

	it('follows the select changed by someone else, and its options', () => {
		const element = select(['apple']);
		const instance = Palette.attach(element, { ordered: true })!;

		element.options[2].selected = true;
		element.dispatchEvent(new Event('change'));
		expect(instance.value()).toEqual(['apple', 'cherry']);

		element.options[0].remove();
		const option = new Option('Elderberry', 'elderberry', true, true);
		element.add(option);
		instance.refresh();
		expect(instance.value()).toEqual(['cherry', 'elderberry']);
		expect(texts(instance.available)).toEqual(['Banana', 'Date']);
	});

	it('follows the disabled select', () => {
		const element = select([], 'disabled');
		const instance = Palette.attach(element)!;

		expect(instance.isDisabled()).toBe(true);
		element.disabled = false;
		instance.update();
		expect(instance.buttons.addAll.disabled).toBe(false);
	});

	it('takes over the wrapper of the select Ajax replaced, and shows the select on destroy', () => {
		const element = select();
		Palette.attach(element);
		const replacement = element.cloneNode(true) as HTMLSelectElement;
		element.replaceWith(replacement);
		replacement.className = '';
		const instance = Palette.attach(replacement)!;

		expect(replacement.parentElement?.querySelectorAll('.antilia-palette-side').length).toBe(2);
		instance.destroy();
		expect(replacement.parentElement?.tagName).toBe('FORM');
		expect(document.querySelector('.antilia-palette')).toBeNull();
		expect(replacement.hasAttribute('aria-hidden')).toBe(false);
	});

	it('works as a Svelte action', () => {
		const element = select();
		const action = palette(element, { availableTitle: 'Left' });

		expect(document.querySelector('.antilia-palette-title')?.textContent).toBe('Left');
		action.update({ availableTitle: 'Links' });
		expect(document.querySelector('.antilia-palette-title')?.textContent).toBe('Links');
		action.destroy();
		expect(document.querySelector('.antilia-palette')).toBeNull();
	});

	it('stops listening when an update of the page removes it', () => {
		let removing: ((element: Element) => void) | undefined;
		const host: Host = { ...standaloneHost, onNodeRemoving: (listener) => {
			removing = listener;
			return () => undefined;
		} };
		setHost(host);
		const element = select();
		const instance = Palette.attach(element)!;
		const onChange = vi.fn();
		instance.options.onChange = onChange;

		removing!(document.body.firstElementChild!);
		click(instance.buttons.addAll);
		expect(onChange).not.toHaveBeenCalled();
	});
});
