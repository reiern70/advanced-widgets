/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Palette, type PaletteChoice } from '../src/index.js';
import BoundPalette from './BoundPalette.svelte';

const FRUITS: PaletteChoice[] = ['Apple', 'Banana', 'Cherry'].map((name) => ({
	value: name.toLowerCase(),
	label: name
}));

function texts(container: HTMLElement, side: 'available' | 'selected'): string[] {
	return Array.from(container.querySelectorAll(`.antilia-palette-${side} .antilia-palette-item`),
		(item) => item.querySelector('.antilia-palette-text')?.textContent ?? '');
}

function item(container: HTMLElement, text: string): HTMLElement {
	return Array.from(container.querySelectorAll<HTMLElement>('.antilia-palette-item'))
		.find((each) => each.querySelector('.antilia-palette-text')?.textContent === text)!;
}

function click(element: Element): void {
	(element as HTMLElement).click();
	flushSync();
}

afterEach(cleanup);

describe('Palette', () => {
	it('draws the core palette into the element Svelte renders', () => {
		const onchange = vi.fn();
		const { container } = render(Palette, { options: FRUITS, value: ['banana'],
			selectedTitle: 'Right', onchange });

		expect(container.querySelector('.antilia-palette')).not.toBeNull();
		expect(texts(container, 'available')).toEqual(['Apple', 'Cherry']);
		expect(texts(container, 'selected')).toEqual(['Banana']);
		expect(container.querySelectorAll('.antilia-palette-title')[1].textContent).toBe('Right');

		click(container.querySelector('.antilia-palette-remove-all')!);
		expect(onchange).toHaveBeenCalledWith([]);
	});

	it('numbers the rows when asked to, and stops when no longer asked', async () => {
		const { container, rerender } = render(Palette, { options: FRUITS, value: ['cherry'],
			numbered: true });

		expect(Array.from(container.querySelectorAll('.antilia-palette-number'),
			(number) => number.textContent)).toEqual(['1.', '2.', '1.']);

		await rerender({ numbered: false });
		flushSync();
		expect(container.querySelector('.antilia-palette-number')).toBeNull();
	});

	it('shows the filter only while filterable', async () => {
		const { container, rerender } = render(Palette, { options: FRUITS, filterThreshold: 0 });
		expect(container.querySelector('.antilia-palette-filter')).not.toBeNull();

		await rerender({ filterable: false });
		flushSync();
		expect(container.querySelector('.antilia-palette-filter')).toBeNull();
	});

	it('binds the value both ways and posts it in hidden fields', () => {
		const { container } = render(BoundPalette, { options: FRUITS, value: ['cherry'],
			ordered: true });

		click(item(container, 'Apple'));
		click(container.querySelector('.antilia-palette-add')!);
		expect(container.querySelector('p')?.textContent).toBe('["cherry","apple"]');
		expect(Array.from(container.querySelectorAll<HTMLInputElement>('input[name="fruits"]'),
			(input) => input.value)).toEqual(['cherry', 'apple']);

		click(container.querySelector('.reset')!);
		expect(texts(container, 'selected')).toEqual(['Cherry']);
	});

	it('shows the value once the options arrive after it, and follows disabled', async () => {
		const { container, rerender } = render(Palette, { options: [], value: ['cherry'] });

		await rerender({ options: FRUITS, disabled: true });
		flushSync();
		expect(texts(container, 'selected')).toEqual(['Cherry']);
		expect(container.querySelector<HTMLButtonElement>('.antilia-palette-add-all')?.disabled)
			.toBe(true);

		await rerender({ disabled: false, ordered: true });
		flushSync();
		expect(container.querySelector<HTMLButtonElement>('.antilia-palette-add-all')?.disabled)
			.toBe(false);
		expect(container.querySelector<HTMLElement>('.antilia-palette-order')?.hidden).toBe(false);
	});
});
