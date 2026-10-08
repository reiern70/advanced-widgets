/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { StrictMode, useState } from 'react';
import { act, cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { AutocompleteSuggestion, AutocompleteValue } from '@advanced-widgets/core';

import { Autocomplete } from '../src/index.js';

const FRUITS: AutocompleteSuggestion[] = ['Apple', 'Apricot', 'Banana'].map((label) => ({
	id: label.toLowerCase(), label, data: { name: label }
}));

function search(term: string): Promise<AutocompleteSuggestion[]> {
	return Promise.resolve(FRUITS.filter((fruit) => fruit.label.toLowerCase()
		.startsWith(term.toLowerCase())));
}

async function type(container: HTMLElement, text: string): Promise<void> {
	const field = container.querySelector<HTMLInputElement>('.wicket-autocomplete-text')!;
	await act(async () => {
		field.value = text;
		field.dispatchEvent(new Event('input'));
		await vi.advanceTimersByTimeAsync(250);
	});
}

function options(container: HTMLElement): HTMLElement[] {
	return Array.from(container.querySelectorAll('.wicket-autocomplete-option'));
}

function chips(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.wicket-chip-text'),
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
	it('suggests with search and reports the chosen values', async () => {
		function Fruits() {
			const [fruits, setFruits] = useState<AutocompleteValue[]>([]);
			return <><Autocomplete multiple search={(term) => search(term)} value={fruits}
				onChange={setFruits} template="<b>{{name}}</b>" name="fruits" />
				<p>{fruits.map((fruit) => fruit.id).join(',')}</p></>;
		}
		const { container } = render(<StrictMode><Fruits /></StrictMode>);

		await type(container, 'ap');
		expect(options(container).map((option) => option.innerHTML))
			.toEqual(['<b>Apple</b>', '<b>Apricot</b>']);
		act(() => options(container)[1].click());
		await type(container, 'b');
		act(() => options(container)[0].click());

		expect(container.querySelector('p')?.textContent).toBe('apricot,banana');
		expect(chips(container)).toEqual(['Apricot', 'Banana']);
		expect(container.querySelector<HTMLInputElement>('input[name="fruits"]')?.value)
			.toBe('["apricot","banana"]');
		expect(container.querySelectorAll('.wicket-autocomplete-field').length).toBe(1);
	});

	it('shows the values the parent gives', () => {
		const { container, rerender } = render(<Autocomplete search={search}
			value={[{ id: 1, label: 'One' }]} />);
		expect(chips(container)).toEqual(['One']);

		rerender(<Autocomplete search={search} value={[{ id: 2, label: 'Two' }]} />);
		expect(chips(container)).toEqual(['Two']);
	});

	it('removes a value with the x of its chip', () => {
		const onChange = vi.fn();
		const { container } = render(<Autocomplete multiple search={search} onChange={onChange}
			value={[{ id: 1, label: 'One' }, { id: 2, label: 'Two' }]} remove="Drop {0}" />);

		const remove = container.querySelector<HTMLElement>('.wicket-chip-remove')!;
		expect(remove.getAttribute('aria-label')).toBe('Drop One');
		act(() => remove.click());

		expect(onChange).toHaveBeenCalledWith([{ id: 2, label: 'Two' }]);
	});

	it('leaves nothing behind when unmounted', () => {
		const { container, unmount } = render(<Autocomplete search={search} />);
		unmount();

		expect(container.innerHTML).toBe('');
	});
});
