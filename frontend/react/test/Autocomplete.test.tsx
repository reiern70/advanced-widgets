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

import { StrictMode, useState } from 'react';
import { act, cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { AutocompleteSuggestion, AutocompleteValue } from '@antilia/core';

import { Autocomplete } from '../src/index.js';

const FRUITS: AutocompleteSuggestion[] = ['Apple', 'Apricot', 'Banana'].map((label) => ({
	id: label.toLowerCase(), label, data: { name: label }
}));

function search(term: string): Promise<AutocompleteSuggestion[]> {
	return Promise.resolve(FRUITS.filter((fruit) => fruit.label.toLowerCase()
		.startsWith(term.toLowerCase())));
}

async function type(container: HTMLElement, text: string): Promise<void> {
	const field = container.querySelector<HTMLInputElement>('.antilia-autocomplete-text')!;
	await act(async () => {
		field.value = text;
		field.dispatchEvent(new Event('input'));
		await vi.advanceTimersByTimeAsync(250);
	});
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
	it('marks the letters typed with highlightMatch', async () => {
		const { container } = render(<Autocomplete search={search} highlightMatch name="fruit" />);

		await type(container, 'ban');

		expect(options(container).map((option) => option.innerHTML))
			.toEqual(['<mark class="antilia-autocomplete-match">Ban</mark>ana']);
	});

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
		expect(container.querySelectorAll('.antilia-autocomplete-field').length).toBe(1);
	});

	it('hands each pick to onPick and keeps no chip, as a find bar', async () => {
		const onPick = vi.fn();
		const onChange = vi.fn();
		const { container } = render(<Autocomplete pick search={search} onPick={onPick}
			onChange={onChange} />);

		await type(container, 'ap');
		act(() => options(container)[1].click());

		expect(onPick).toHaveBeenCalledWith({ id: 'apricot', label: 'Apricot' });
		expect(onChange).not.toHaveBeenCalled();
		expect(chips(container)).toEqual([]);
		expect(container.querySelector<HTMLInputElement>('.antilia-autocomplete-text')?.value)
			.toBe('');
	});

	it('is a search box with freeText, its text given and reported', async () => {
		const onTextChange = vi.fn();
		const onChoose = vi.fn();
		function Search() {
			const [text, setText] = useState('pear');
			return <><Autocomplete freeText search={search} text={text} name="q"
				onTextChange={(next) => {
					onTextChange(next);
					setText(next);
				}} onChoose={onChoose} />
				<p>{text}</p>
				<button type="button" onClick={() => setText('lime')}>lime</button></>;
		}
		const { container } = render(<StrictMode><Search /></StrictMode>);
		const field = container.querySelector<HTMLInputElement>('.antilia-autocomplete-text')!;
		const hidden = container.querySelector<HTMLInputElement>('input[name="q"]')!;
		expect([field.value, hidden.value]).toEqual(['pear', 'pear']);

		await type(container, 'ap');
		expect(onTextChange).toHaveBeenLastCalledWith('ap');
		expect(container.querySelector('p')?.textContent).toBe('ap');
		// the text the parent gives back, as typed, leaves the suggestions open
		expect(options(container)).toHaveLength(2);

		act(() => options(container)[1].click());
		expect([field.value, hidden.value]).toEqual(['Apricot', 'Apricot']);
		expect(onTextChange).toHaveBeenLastCalledWith('Apricot');
		expect(onChoose).toHaveBeenCalledWith(FRUITS[1]);
		expect(chips(container)).toEqual([]);

		act(() => container.querySelector('button')!.click());
		expect([field.value, hidden.value]).toEqual(['lime', 'lime']);
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

		const remove = container.querySelector<HTMLElement>('.antilia-chip-remove')!;
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
