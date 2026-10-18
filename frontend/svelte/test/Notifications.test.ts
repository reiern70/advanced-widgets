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

import { cleanup, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { Notifications } from '../src/index.js';

afterEach(() => {
	cleanup();
	vi.useRealTimers();
});

function titles(container: ParentNode, position = 'top-right'): string[] {
	return Array.from(container.querySelectorAll('.antilia-notifications-stack-' + position +
		' .antilia-notification:not(.antilia-notification-leaving) .antilia-notification-title'),
	(title) => title.textContent ?? '');
}

describe('Notifications', () => {
	it('renders an empty element the core stacks the notifications shown in', () => {
		const { container, component } = render(Notifications, { id: 'n', class: 'mine',
			closeLabel: 'Zu' });

		const element = container.querySelector('#n')!;
		expect(element.className).toBe('antilia-notifications mine');
		expect(element.children).toHaveLength(0);

		const shown = component.show({ type: 'warning', title: 'Careful', body: 'Look.' });
		expect(titles(element)).toEqual(['Careful']);
		expect(shown.element.getAttribute('role')).toBe('alert');
		expect(element.querySelector<HTMLButtonElement>('.antilia-notification-close')!.title)
			.toBe('Zu');
	});

	it('takes the position, timeout and texts as they change', async () => {
		vi.useFakeTimers();
		const { container, component, rerender } = render(Notifications, { timeout: 0 });
		component.show({ title: 'Stays' });

		await rerender({ position: 'bottom-center', timeout: 1000, errorTimeout: 2000,
			closeLabel: 'Cerrar' });
		flushSync();
		component.show({ title: 'Goes' });
		component.show({ type: 'error', title: 'Later', position: 'top-left' });

		expect(titles(container)).toEqual(['Stays']);
		expect(titles(container, 'bottom-center')).toEqual(['Goes']);
		expect(container.querySelector<HTMLButtonElement>('.antilia-notification-close')!.title)
			.toBe('Cerrar');
		vi.advanceTimersByTime(1000);
		expect(titles(container, 'bottom-center')).toEqual([]);
		expect(titles(container, 'top-left')).toEqual(['Later']);
		vi.advanceTimersByTime(1000);
		expect(titles(container, 'top-left')).toEqual([]);

		component.closeAll();
		expect(titles(container)).toEqual([]);
	});

	it('takes the notifications away when unmounted', () => {
		const onClose = vi.fn();
		const { component, unmount } = render(Notifications);
		const shown = component.show({ title: 'Here', onClose });

		unmount();
		expect(shown.element.isConnected).toBe(false);
		expect(onClose).not.toHaveBeenCalled();
		expect(component.show({ title: 'Too late' }).closed).toBe(true);
		component.closeAll();
	});
});
