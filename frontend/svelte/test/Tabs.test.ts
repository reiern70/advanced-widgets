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

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { BOOTSTRAP_LOOK, LOOK_CONTEXT } from '../src/index.js';
import BoundTabs from './BoundTabs.svelte';

function tab(container: HTMLElement, name: string): HTMLElement {
	return container.querySelector('.antilia-tab[data-tab="' + name + '"]')!;
}

function button(container: HTMLElement, name: string): HTMLButtonElement {
	return tab(container, name).querySelector('.antilia-tab-button')!;
}

function panel(container: HTMLElement, name: string): HTMLElement {
	return container.querySelector('.antilia-tabs-panel[data-tab="' + name + '"]')!;
}

function selected(container: HTMLElement): string {
	return container.querySelector('output')!.textContent ?? '';
}

afterEach(() => {
	cleanup();
	document.querySelectorAll('.antilia-tabs-confirm').forEach((confirm) => confirm.remove());
});

describe('Tabs', () => {
	it('renders the tabs and their panels, binding the first one selected', () => {
		const { container } = render(BoundTabs);

		const root = container.querySelector('#t')!;
		expect(root.className).toContain('antilia-tabs antilia-tabs-horizontal antilia-tabs-tabs');
		expect(root.querySelector('.antilia-tabs-list')!.getAttribute('aria-label')).toBe('Documents');
		expect(button(container, 'one').getAttribute('aria-selected')).toBe('true');
		expect(button(container, 'one').querySelector('svg[data-icon="one"]')).not.toBeNull();
		expect(panel(container, 'one').textContent).toBe('Panel One');
		expect(panel(container, 'one').hidden).toBe(false);
		expect(panel(container, 'two').hidden).toBe(true);
		expect(panel(container, 'two').getAttribute('data-modified')).toBe('true');
		expect(tab(container, 'two').querySelector('.antilia-tab-mark')).not.toBeNull();
		expect(Array.from(tab(container, 'one').children, (child) => child.className))
			.toEqual(['antilia-tab-button', 'antilia-tab-actions', 'antilia-tab-close']);
		expect(tab(container, 'three').querySelector('.antilia-tab-close')).toBeNull();
		expect(selected(container)).toBe('one');
	});

	it('binds the tab the user selects, and selects the one the page binds', async () => {
		const { container } = render(BoundTabs, { orientation: 'vertical', design: 'underline' });
		expect(container.querySelector('.antilia-tabs-vertical.antilia-tabs-underline'))
			.not.toBeNull();

		await fireEvent.click(button(container, 'two'));
		flushSync();
		expect(selected(container)).toBe('two');

		await fireEvent.click(container.querySelector('.pick')!);
		flushSync();
		expect(button(container, 'three').getAttribute('aria-selected')).toBe('true');
		expect(panel(container, 'three').hidden).toBe(false);

		// an action does not select its tab
		await fireEvent.click(tab(container, 'one').querySelector('.info')!);
		flushSync();
		expect(container.querySelector('.infos')!.textContent).toBe('1');
		expect(selected(container)).toBe('three');
	});

	it('lets the page take a closed tab away, and add one it selects', async () => {
		const onclose = vi.fn();
		const { container } = render(BoundTabs, { onclose });

		await fireEvent.click(tab(container, 'one').querySelector('.antilia-tab-close')!);
		flushSync();
		expect(onclose).toHaveBeenCalledWith('one');
		expect(tab(container, 'one')).toBeNull();
		expect(selected(container)).toBe('two');

		await fireEvent.click(container.querySelector('.add')!);
		flushSync();
		expect(button(container, 'four').getAttribute('aria-selected')).toBe('true');
		expect(panel(container, 'four').hidden).toBe(false);
		expect(tab(container, 'four').querySelector('.antilia-tab-close')).not.toBeNull();

		await fireEvent.click(container.querySelector('.handle')!);
		flushSync();
		expect(tab(container, 'three').hidden).toBe(true);
		expect(onclose).toHaveBeenCalledTimes(1);
	});

	it('asks before closing a tab with changes, or as the page asks', async () => {
		const onclose = vi.fn();
		const { container } = render(BoundTabs, { onclose });

		await fireEvent.click(tab(container, 'two').querySelector('.antilia-tab-close')!);
		expect(document.querySelector('.antilia-tabs-confirm .modal-dialog-title')?.textContent)
			.toBe('Close Two?');
		await fireEvent.click(document.querySelector('[data-confirm="keep"]')!);
		await waitFor(() => expect(document.querySelector('.antilia-tabs-confirm')).toBeNull());
		expect(onclose).not.toHaveBeenCalled();

		cleanup();
		const confirmClose = vi.fn(() => true);
		const second = render(BoundTabs, { onclose, confirmClose });
		await fireEvent.click(tab(second.container, 'two').querySelector('.antilia-tab-close')!);
		await waitFor(() => expect(onclose).toHaveBeenCalledWith('two'));
		expect(confirmClose).toHaveBeenCalledWith('two', 'Two');
	});

	it('renders its parts in the look of its context', () => {
		const { container } = render(BoundTabs, { props: { design: 'pills' },
			context: new Map([[LOOK_CONTEXT, BOOTSTRAP_LOOK]]) });

		expect(container.querySelector('.antilia-tabs-list')!.className)
			.toBe('antilia-tabs-list nav nav-pills');
		expect(tab(container, 'one').className).toBe('antilia-tab nav-item');
		expect(tab(container, 'one').querySelector('.antilia-tab-close')!.className)
			.toBe('antilia-tab-close btn-close');
	});
});
