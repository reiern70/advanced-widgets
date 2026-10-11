/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { AutocompleteSuggestion } from '@antilia/core';

import { Autocomplete } from '../src/index.js';
import BoundAutocomplete from './BoundAutocomplete.svelte';

const FRUITS: AutocompleteSuggestion[] = ['Apple', 'Apricot', 'Banana'].map((label) => ({
	id: label.toLowerCase(), label, data: { name: label }
}));

function search(term: string): Promise<AutocompleteSuggestion[]> {
	return Promise.resolve(FRUITS.filter((fruit) => fruit.label.toLowerCase()
		.startsWith(term.toLowerCase())));
}

async function type(container: HTMLElement, text: string): Promise<void> {
	const field = container.querySelector<HTMLInputElement>('.antilia-autocomplete-text')!;
	field.value = text;
	field.dispatchEvent(new Event('input'));
	await vi.advanceTimersByTimeAsync(250);
}

function click(element: HTMLElement): void {
	element.click();
	flushSync();
}

function options(container: HTMLElement): HTMLElement[] {
	return Array.from(container.querySelectorAll('.antilia-autocomplete-option'));
}

function chips(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.antilia-chip-text'),
		(chip) => chip.textContent ?? '');
}

beforeEach(() => {
	vi.useFakeTimers();
});

afterEach(() => {
	cleanup();
	vi.useRealTimers();
});

describe('Autocomplete', () => {
	it('binds the chosen values both ways', async () => {
		const { container } = render(BoundAutocomplete, { search, multiple: true });

		await type(container, 'ap');
		expect(options(container).map((option) => option.innerHTML))
			.toEqual(['<b>Apple</b>', '<b>Apricot</b>']);
		click(options(container)[1]);
		await type(container, 'b');
		click(options(container)[0]);

		expect(container.querySelector('p')?.textContent).toBe('apricot,banana');
		expect(chips(container)).toEqual(['Apricot', 'Banana']);

		click(container.querySelector<HTMLElement>('.reset')!);
		expect(chips(container)).toEqual(['X']);
		expect(container.querySelector<HTMLInputElement>('input[name="fruits"]')?.value)
			.toBe('["x"]');
	});

	it('calls onchange when a value is removed with the x of its chip', () => {
		const onchange = vi.fn();
		const { container } = render(Autocomplete, { search, multiple: true, onchange,
			value: [{ id: 1, label: 'One' }, { id: 2, label: 'Two' }] });

		click(container.querySelector<HTMLElement>('.antilia-chip-remove')!);

		expect(onchange).toHaveBeenCalledWith([{ id: 2, label: 'Two' }]);
	});

	it('hands each pick to onpick and keeps no chip, as a find bar', async () => {
		const onpick = vi.fn();
		const onchange = vi.fn();
		const { container } = render(Autocomplete, { search, pick: true, onpick, onchange });

		await type(container, 'ap');
		click(options(container)[1]);

		expect(onpick).toHaveBeenCalledWith({ id: 'apricot', label: 'Apricot' });
		expect(onchange).not.toHaveBeenCalled();
		expect(chips(container)).toEqual([]);
		expect(container.querySelector<HTMLInputElement>('.antilia-autocomplete-text')?.value)
			.toBe('');
	});

	it('leaves nothing behind when unmounted', () => {
		const { container, unmount } = render(Autocomplete, { search });
		unmount();

		expect(container.querySelector('.antilia-autocomplete')).toBeNull();
	});
});
