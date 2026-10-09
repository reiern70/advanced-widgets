/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Dropdown, type DropdownOption } from '../src/index.js';
import Bound from './Bound.svelte';

const FRUITS: DropdownOption[] = ['Apple', 'Banana', 'Cherry'].map((name) => ({
	value: name.toLowerCase(),
	label: name
}));

function toggle(container: HTMLElement): HTMLButtonElement {
	return container.querySelector('.antilia-dropdown-toggle')!;
}

function items(container: HTMLElement): HTMLLIElement[] {
	return Array.from(container.querySelectorAll('.antilia-dropdown-option'));
}

function click(element: HTMLElement): void {
	element.click();
	flushSync();
}

afterEach(cleanup);

describe('Dropdown', () => {
	it('draws the core drop-down inside the wrapper Svelte renders', () => {
		const { container } = render(Dropdown, { options: FRUITS, value: 'banana' });

		const wrapper = container.querySelector('.antilia-dropdown')!;
		expect(wrapper.firstElementChild?.tagName).toBe('SELECT');
		expect(toggle(container).textContent).toBe('Banana');
		expect(items(container).map((item) => item.textContent)).toEqual(['Apple', 'Banana', 'Cherry']);
	});

	it('binds the value both ways', () => {
		const { container } = render(Bound, { options: FRUITS, value: 'apple' });

		click(toggle(container));
		click(items(container)[2]);
		expect(container.querySelector('p')?.textContent).toBe('"cherry"');
		expect(toggle(container).textContent).toBe('Cherry');

		click(container.querySelector<HTMLButtonElement>('.reset')!);
		expect(toggle(container).textContent).toBe('Apple');
	});

	it('shows the value once the options arrive after it, as when they are loaded', async () => {
		const { container, rerender } = render(Bound, { options: [], value: 'banana' });

		await rerender({ options: FRUITS });
		flushSync();

		expect(toggle(container).textContent).toBe('Banana');
		expect(container.querySelector('select')?.value).toBe('banana');
		expect(container.querySelector('p')?.textContent).toBe('"banana"');
	});

	it('shows the values of a multiple drop-down once its options arrive', async () => {
		const { container, rerender } = render(Bound, { options: [], value: ['cherry'],
			multiple: true });

		await rerender({ options: FRUITS });
		flushSync();

		expect(Array.from(container.querySelectorAll('.antilia-chip-text'),
			(chip) => chip.textContent)).toEqual(['Cherry']);
		expect(container.querySelector('p')?.textContent).toBe('["cherry"]');
	});

	it('binds the values of a multiple drop-down and keeps its list open', () => {
		const { container } = render(Bound, { options: FRUITS, value: [], multiple: true });

		click(toggle(container));
		click(items(container)[0]);
		click(items(container)[2]);

		expect(container.querySelector('p')?.textContent).toBe('["apple","cherry"]');
		expect(Array.from(container.querySelectorAll('.antilia-chip'),
			(chip) => chip.textContent)).toEqual(['Apple', 'Cherry']);
		expect(container.querySelector('.antilia-dropdown-open')).not.toBeNull();

		click(container.querySelector<HTMLButtonElement>('.reset')!);
		expect(container.querySelectorAll('.antilia-chip').length).toBe(0);
	});

	it('removes a value with the x of its chip, labelled with the text given', () => {
		const { container } = render(Bound, { options: FRUITS, value: ['apple', 'cherry'],
			multiple: true });
		const remove = container.querySelector<HTMLElement>('.antilia-chip-remove')!;
		expect(remove.getAttribute('aria-label')).toBe('Remove Apple');

		click(remove);

		expect(container.querySelector('p')?.textContent).toBe('["cherry"]');
	});

	it('calls onchange with the value', () => {
		const onchange = vi.fn();
		const { container } = render(Dropdown, { options: FRUITS, onchange });

		click(items(container)[1]);

		expect(onchange).toHaveBeenCalledWith('banana');
	});

	it('draws options and texts that change', async () => {
		const { container, rerender } = render(Dropdown, { options: FRUITS, filterThreshold: 10 });
		expect(container.querySelector('.antilia-dropdown-filter')).toBeNull();

		await rerender({ options: [...FRUITS, { value: 'date', label: 'Date' }], filterThreshold: 0,
			filter: 'Search' });

		expect(items(container).map((item) => item.textContent)).toContain('Date');
		expect(container.querySelector<HTMLInputElement>('.antilia-dropdown-filter')?.placeholder)
			.toBe('Search');
	});

	it('shows a disabled select as disabled', async () => {
		const { container, rerender } = render(Dropdown, { options: FRUITS });
		await rerender({ disabled: true });

		expect(toggle(container).disabled).toBe(true);
	});

	it('closes on Escape with the keyboard', async () => {
		const { container } = render(Dropdown, { options: FRUITS });
		await fireEvent.keyDown(toggle(container), { key: 'ArrowDown' });
		await fireEvent.keyDown(container.querySelector('.antilia-dropdown-list')!, { key: 'Escape' });

		expect(container.querySelector('.antilia-dropdown-open')).toBeNull();
		expect(document.activeElement).toBe(toggle(container));
	});

	it('leaves nothing behind when unmounted', () => {
		const { container, unmount } = render(Dropdown, { options: FRUITS });
		const listeners = vi.spyOn(document, 'removeEventListener');
		unmount();

		expect(container.querySelector('.antilia-dropdown')).toBeNull();
		expect(listeners).toHaveBeenCalledWith('mousedown', expect.any(Function));
	});
});
