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

import { useState } from 'react';
import { act, cleanup, fireEvent, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { LookProvider, Popover, PopoverButton } from '../src/index.js';

afterEach(() => {
	Popover.closeAll();
	cleanup();
});

function Counter() {
	const [count, setCount] = useState(0);
	return <button type="button" className="count" onClick={() => setCount(count + 1)}>{count}</button>;
}

function popover(): HTMLElement | null {
	return document.querySelector('.antilia-popover');
}

describe('PopoverButton', () => {
	it('opens a popover pointing at it, with its content, which React goes on updating', () => {
		const opened = vi.fn();
		const closed = vi.fn();
		const { container } = render(<PopoverButton className="mine" aria-label="More"
			options={{ title: 'Details', body: 'Some text', placement: 'right-top', design: 'glass',
				onClose: closed }}
			content={<Counter />} onOpen={opened}>i</PopoverButton>);
		const button = container.querySelector<HTMLButtonElement>('button.mine')!;
		expect(button.type).toBe('button');
		expect(document.querySelector('.count')).toBeNull();

		fireEvent.click(button);

		const element = popover()!;
		expect(element.className).toContain('antilia-popover-glass');
		expect(element.querySelector('.antilia-popover-title')!.textContent).toBe('Details');
		const body = element.querySelector('.antilia-popover-body')!;
		expect(body.firstChild!.textContent).toBe('Some text');
		expect(body.lastElementChild!.firstElementChild!.className).toBe('count');
		expect(opened).toHaveBeenCalledWith(Popover.current());
		expect(button.getAttribute('aria-expanded')).toBe('true');
		fireEvent.click(element.querySelector('.count')!);
		expect(element.querySelector('.count')!.textContent).toBe('1');

		// a second click closes it, its content gone with it
		fireEvent.click(button);
		expect(popover()).toBeNull();
		expect(closed).toHaveBeenCalledTimes(1);
		expect(document.querySelector('.count')).toBeNull();
		fireEvent.click(button);
		expect(popover()!.querySelector('.count')!.textContent).toBe('0');
	});

	it('opens a popover of text only, unless its own click handler says no', () => {
		const { container, rerender } = render(<PopoverButton options={{ body: 'Text' }}>
			i</PopoverButton>);
		fireEvent.click(container.querySelector('button')!);
		expect(popover()!.querySelector('.antilia-popover-body')!.textContent).toBe('Text');
		Popover.closeAll();

		rerender(<PopoverButton options={{ body: 'Text' }}
			onClick={(event) => event.preventDefault()}>i</PopoverButton>);
		fireEvent.click(container.querySelector('button')!);
		expect(popover()).toBeNull();
	});

	it('closes its popover as it goes away', () => {
		const { container, unmount } = render(<PopoverButton options={{ body: 'Text' }}
			content={<em>rich</em>}>i</PopoverButton>);
		fireEvent.click(container.querySelector('button')!);
		expect(popover()).not.toBeNull();
		act(() => unmount());
		expect(popover()).toBeNull();
		expect(document.querySelector('em')).toBeNull();
	});

	it('opens its popover in the look of its provider', () => {
		const { container } = render(<LookProvider look="bootstrap">
			<PopoverButton options={{ title: 'Details', body: 'Text' }}>i</PopoverButton>
		</LookProvider>);
		fireEvent.click(container.querySelector('button')!);

		const element = popover()!;
		expect(element.classList.contains('antilia-look-bootstrap')).toBe(true);
		expect(element.getAttribute('data-antilia-look')).toBe('bootstrap');
		expect(element.querySelector('.antilia-popover-header')!.classList.contains('popover-header'))
			.toBe(true);
	});
});
