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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import BoundResizable from './BoundResizable.svelte';

function box(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-resizable')!;
}

function handle(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-resizable-handle')!;
}

afterEach(cleanup);

describe('Resizable', () => {
	it('renders a div the core adds a handle to, in the size given', () => {
		const { container } = render(BoundResizable);

		expect(box(container).id).toBe('r');
		expect(box(container).className.split(' ')).toEqual(['notes', 'antilia-resizable',
			'antilia-resizable-both', 'antilia-resizable-sized-height']);
		expect(box(container).firstElementChild?.textContent).toBe('Text');
		expect(box(container).style.getPropertyValue('--antilia-resizable-height')).toBe('120px');
		expect(handle(container).getAttribute('aria-label')).toBe('Resize the notes');
	});

	it('binds the size the user gives it', async () => {
		const onresized = vi.fn();
		const { container } = render(BoundResizable, { onresized });

		await fireEvent.keyDown(handle(container), { key: 'ArrowDown' });
		flushSync();

		expect(onresized).toHaveBeenCalledWith({ width: null, height: 130 });
		expect(container.querySelector('output')?.textContent).toBe(' × 130');
	});

	it('resizes when the page changes the size, and takes other options', async () => {
		const { container, rerender } = render(BoundResizable);

		await fireEvent.click(container.querySelector('.reset')!);
		flushSync();
		expect(box(container).style.getPropertyValue('--antilia-resizable-height')).toBe('300px');

		await rerender({ axis: 'horizontal', enabled: false, className: 'other' });
		flushSync();
		expect(box(container).classList.contains('antilia-resizable-horizontal')).toBe(true);
		expect(box(container).classList.contains('other')).toBe(true);
		expect(handle(container).hidden).toBe(true);
	});

	it('takes itself away when unmounted', () => {
		const { container, unmount } = render(BoundResizable);
		const element = box(container);

		unmount();
		expect(element.querySelector('.antilia-resizable-handle')).toBeNull();
	});
});
