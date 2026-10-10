/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Palette, type PaletteChoice } from '../src/index.js';

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

function button(container: HTMLElement, name: string): HTMLButtonElement {
	return container.querySelector(`.antilia-palette-${name}`)!;
}

afterEach(cleanup);

describe('Palette', () => {
	it('draws the core palette into the element React renders', () => {
		const { container } = render(<Palette options={FRUITS} value={['banana']}
			onChange={() => {}} availableTitle="Left" />);

		expect(container.firstElementChild?.className).toBe('antilia-palette');
		expect(texts(container, 'available')).toEqual(['Apple', 'Cherry']);
		expect(texts(container, 'selected')).toEqual(['Banana']);
		expect(container.querySelector('.antilia-palette-title')?.textContent).toBe('Left');
	});

	it('numbers the rows when asked to, and stops when no longer asked', () => {
		const { container, rerender } = render(<Palette options={FRUITS} value={['cherry']}
			onChange={() => {}} numbered selectedTitle="Columns" />);

		expect(Array.from(container.querySelectorAll('.antilia-palette-number'),
			(number) => number.textContent)).toEqual(['1.', '2.', '1.']);
		expect(container.querySelectorAll('.antilia-palette-title')[1].textContent)
			.toBe('Columns');

		rerender(<Palette options={FRUITS} value={['cherry']} onChange={() => {}} />);
		expect(container.querySelector('.antilia-palette-number')).toBeNull();
	});

	it('shows the filter only while filterable', () => {
		const { container, rerender } = render(<Palette options={FRUITS} filterThreshold={0} />);
		expect(container.querySelector('.antilia-palette-filter')).not.toBeNull();

		rerender(<Palette options={FRUITS} filterThreshold={0} filterable={false} />);
		expect(container.querySelector('.antilia-palette-filter')).toBeNull();
	});

	it('keeps its own choice and posts it in hidden fields', () => {
		const onChange = vi.fn();
		const { container } = render(<Palette options={FRUITS} defaultValue={['cherry']} ordered
			name="fruits" onChange={onChange} />);

		fireEvent.click(item(container, 'Apple'));
		fireEvent.click(button(container, 'add'));

		expect(onChange).toHaveBeenCalledWith(['cherry', 'apple']);
		expect(texts(container, 'selected')).toEqual(['Cherry', 'Apple']);
		expect(Array.from(container.querySelectorAll<HTMLInputElement>('input[name="fruits"]'),
			(input) => input.value)).toEqual(['cherry', 'apple']);
	});

	it('follows a controlled value, and sets back a change its parent did not take', () => {
		function Controlled() {
			const [value, setValue] = useState(['apple']);
			return <>
				<Palette options={FRUITS} value={value} ordered onChange={setValue} />
				<button type="button" className="reset" onClick={() => setValue(['cherry'])} />
			</>;
		}
		const { container } = render(<Controlled />);

		fireEvent.click(item(container, 'Banana'));
		fireEvent.click(button(container, 'add'));
		expect(texts(container, 'selected')).toEqual(['Apple', 'Banana']);
		fireEvent.click(container.querySelector('.reset')!);
		expect(texts(container, 'selected')).toEqual(['Cherry']);
		cleanup();

		const fixed = render(<Palette options={FRUITS} value={['apple']} onChange={() => {}} />);
		fireEvent.click(button(fixed.container, 'add-all'));
		expect(texts(fixed.container, 'selected')).toEqual(['Apple']);
	});

	it('shows the value once the options arrive after it, and follows disabled', () => {
		const { container, rerender } = render(<Palette options={[]} value={['cherry']} />);

		rerender(<Palette options={FRUITS} value={['cherry']} disabled />);
		expect(texts(container, 'selected')).toEqual(['Cherry']);
		expect(button(container, 'add-all').disabled).toBe(true);

		rerender(<Palette options={FRUITS} value={['cherry']} ordered />);
		expect(button(container, 'add-all').disabled).toBe(false);
		expect(container.querySelector<HTMLElement>('.antilia-palette-order')?.hidden).toBe(false);
	});

	it('takes the palette away on unmount', () => {
		const { container, unmount } = render(<Palette options={FRUITS} />);
		const element = container.firstElementChild!;

		act(() => unmount());
		expect(element.children.length).toBe(0);
	});
});
