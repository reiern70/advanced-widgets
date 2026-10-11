/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Autocomplete, Scrollbar, type AutocompleteSuggestion } from '../src/index.js';

const FRUITS: AutocompleteSuggestion[] = ['Apple', 'Apricot', 'Banana'].map((label) => ({
	id: label.toLowerCase(), label, data: { name: label }
}));

function search(term: string): Promise<AutocompleteSuggestion[]> {
	return Promise.resolve(FRUITS.filter((fruit) => fruit.label.toLowerCase()
		.startsWith(term.toLowerCase())));
}

function input(): HTMLInputElement {
	document.body.innerHTML = '<label for="f">Fruit</label><input type="hidden" id="f">';
	return document.getElementById('f') as HTMLInputElement;
}

async function type(instance: Autocomplete, text: string): Promise<void> {
	instance.text.value = text;
	instance.text.dispatchEvent(new Event('input'));
	await vi.advanceTimersByTimeAsync(250);
}

function key(target: Element, name: string): void {
	target.dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true }));
}

function chips(instance: Autocomplete): string[] {
	return Array.from(instance.chips.querySelectorAll('.antilia-chip-text'),
		(chip) => chip.textContent ?? '');
}

beforeEach(() => {
	vi.useFakeTimers();
});

afterEach(() => {
	vi.useRealTimers();
	document.body.innerHTML = '';
});

describe('Autocomplete', () => {
	it('lists the suggestions of search, rendered with the template', async () => {
		const instance = new Autocomplete(input(), { search, template: '<b>{{name}}</b> {{@id}}' });

		await type(instance, 'ap');

		expect(instance.isOpen()).toBe(true);
		expect(Array.from(instance.list.children, (item) => item.innerHTML))
			.toEqual(['<b>Apple</b> apple', '<b>Apricot</b> apricot']);
		expect(instance.text.getAttribute('aria-label')).toBe('Fruit');
	});

	it('chooses with the keyboard, writes the ids and fires change', async () => {
		const element = input();
		const instance = new Autocomplete(element, { search, multiple: true });
		const changes: string[] = [];
		element.addEventListener('change', () => changes.push(element.value));

		await type(instance, 'ap');
		key(instance.text, 'ArrowDown');
		key(instance.text, 'Enter');
		await type(instance, 'b');
		key(instance.text, 'Enter');

		expect(chips(instance)).toEqual(['Apricot', 'Banana']);
		expect(changes).toEqual(['["apricot"]', '["apricot","banana"]']);
		expect(instance.isOpen()).toBe(false);
	});

	it('leaves out the suggestions chosen already and says when nothing matches', async () => {
		const instance = new Autocomplete(input(), { search, multiple: true, noMatch: 'Nothing',
			chosen: [{ id: 'apple', label: 'Apple' }] });

		await type(instance, 'ap');
		expect(Array.from(instance.list.children, (item) => item.textContent)).toEqual(['Apricot']);

		await type(instance, 'x');
		expect(instance.status.textContent).toBe('Nothing');
	});

	it('takes one value in place of the text field, until it is removed', () => {
		const element = input();
		const instance = new Autocomplete(element, { chosen: [{ id: 1, label: 'One' }],
			remove: 'Drop {0}' });
		expect(instance.text.hidden).toBe(true);
		expect(element.value).toBe('[1]');

		instance.chips.querySelector<HTMLElement>('.antilia-chip-remove')!.click();

		expect(instance.text.hidden).toBe(false);
		expect(element.value).toBe('');
	});

	it('removes the last value with Backspace in the empty field', () => {
		const instance = new Autocomplete(input(), { multiple: true,
			chosen: [{ id: 1, label: 'One' }, { id: 2, label: 'Two' }] });

		key(instance.text, 'Backspace');

		expect(chips(instance)).toEqual(['One']);
	});

	it('only reports a pick, as a find bar, and keeps no chip', async () => {
		const element = input();
		const instance = new Autocomplete(element, { search, pick: true, multiple: true,
			chosen: [{ id: 'banana', label: 'Banana' }] });
		const picked: unknown[] = [];
		element.addEventListener('change', () => picked.push(element.value));

		expect(chips(instance)).toEqual([]);
		expect(element.value).toBe('');
		await type(instance, 'ap');
		key(instance.text, 'Enter');

		expect(picked).toEqual(['["apple"]']);
		expect(instance.picked).toEqual({ id: 'apple', label: 'Apple' });
		expect(chips(instance)).toEqual([]);
		expect(instance.text.value).toBe('');
		expect(instance.text.hidden).toBe(false);
		expect(instance.wrapper.classList.contains('antilia-autocomplete-pick')).toBe(true);

		// still suggested: the page decides what it leaves out
		await type(instance, 'ap');
		expect(instance.suggestions.map((suggestion) => suggestion.id)).toEqual(['apple', 'apricot']);
		key(instance.text, 'ArrowDown');
		key(instance.text, 'Enter');
		expect(picked).toEqual(['["apple"]', '["apricot"]']);

		// nothing to remove, and nothing to show
		key(instance.text, 'Backspace');
		instance.setChosen([{ id: 'x', label: 'X' }]);
		expect(picked).toHaveLength(2);
		expect(chips(instance)).toEqual([]);
	});

	it('asks the url with the term when no search is given', async () => {
		const fetch = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(FRUITS) });
		vi.stubGlobal('fetch', fetch);
		const instance = new Autocomplete(input(), { url: '/fruits?lang=en' });

		await type(instance, 'b a');

		expect(fetch.mock.calls[0][0]).toBe('/fruits?lang=en&term=b%20a');
		vi.unstubAllGlobals();
	});

	it('shows other chosen values without firing change, and is taken away by destroy', () => {
		const element = input();
		const instance = new Autocomplete(element, { multiple: true });
		const change = vi.fn();
		element.addEventListener('change', change);

		instance.setChosen([{ id: 'x', label: 'X' }]);
		expect(chips(instance)).toEqual(['X']);
		expect(change).not.toHaveBeenCalled();
		expect(Autocomplete.of('f')).toBe(instance);

		instance.destroy();
		expect(element.parentElement).toBe(document.body);
		expect(Autocomplete.of('f')).toBeNull();
	});

	it('scrolls its list of suggestions with the slim scroll bars', () => {
		const instance = new Autocomplete(input(), { search });

		expect(instance.list.parentElement?.className).toBe('antilia-scrollbar');
		expect(Scrollbar.of(instance.list)?.options).toEqual({ axis: 'y' });

		instance.destroy();
		expect(Scrollbar.of(instance.list)).toBeUndefined();
	});
});
