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

import { cleanup, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { DatePicker } from '../src/index.js';
import Bound from './BoundDatePicker.svelte';

function field(container: HTMLElement): HTMLInputElement {
	return container.querySelector('.antilia-datepicker-text')!;
}

function click(element: HTMLElement): void {
	element.click();
	flushSync();
}

function day(container: HTMLElement, iso: string): HTMLButtonElement {
	return container.querySelector(`button[data-value="${iso}"]`)!;
}

afterEach(cleanup);

describe('DatePicker', () => {
	it('draws the core date picker inside the wrapper Svelte renders', () => {
		const { container } = render(DatePicker, { value: '2026-10-09', locale: 'de-DE',
			wrapperClass: 'mine', name: 'born' });

		const wrapper = container.querySelector('.antilia-datepicker')!;
		expect(wrapper.classList.contains('mine')).toBe(true);
		expect(wrapper.firstElementChild?.tagName).toBe('INPUT');
		expect(wrapper.firstElementChild?.getAttribute('name')).toBe('born');
		expect(field(container).value).toBe('09.10.2026');
	});

	it('binds the value both ways', () => {
		const { container } = render(Bound, { value: '2026-10-09' });

		click(container.querySelector<HTMLButtonElement>('.antilia-datepicker-toggle')!);
		click(day(container, '2026-10-20'));
		expect(container.querySelector('p')?.textContent).toBe('2026-10-20');
		expect(field(container).value).toBe('20.10.2026');

		click(container.querySelector<HTMLButtonElement>('.reset')!);
		expect(field(container).value).toBe('01.01.2026');
	});

	it('binds the value of a date and time picker', () => {
		const { container } = render(Bound, { value: '2026-10-09T08:05', time: true });
		expect(field(container).value).toBe('09.10.2026, 08:05');

		field(container).value = '1.2.2027 17:30';
		field(container).dispatchEvent(new Event('change'));
		flushSync();

		expect(container.querySelector('p')?.textContent).toBe('2027-02-01T17:30');
	});

	it('calls onchange and follows new options', async () => {
		const onchange = vi.fn();
		const { container, rerender } = render(DatePicker, { value: '2026-10-09', locale: 'de-DE',
			onchange, open: 'Datum wählen' });
		const toggle = container.querySelector<HTMLButtonElement>('.antilia-datepicker-toggle')!;
		expect(toggle.title).toBe('Datum wählen');

		field(container).value = '24.12.2026';
		field(container).dispatchEvent(new Event('change'));
		expect(onchange).toHaveBeenCalledWith('2026-12-24');

		await rerender({ value: '2026-12-24', locale: 'en-US', open: 'Choose', disabled: true });
		flushSync();
		expect(field(container).value).toBe('12/24/2026');
		expect(toggle.title).toBe('Choose');
		expect(field(container).disabled).toBe(true);
	});

	it('takes the picker away when it is destroyed', () => {
		const { container, unmount } = render(DatePicker, { value: '2026-10-09' });
		expect(container.querySelector('.antilia-datepicker-field')).not.toBeNull();

		unmount();
		expect(container.querySelector('.antilia-datepicker')).toBeNull();
	});
});
