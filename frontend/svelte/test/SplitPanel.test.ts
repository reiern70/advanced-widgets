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

import { cleanup, fireEvent, render } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import BoundSplitPanel from './BoundSplitPanel.svelte';

function split(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-split')!;
}

function divider(container: HTMLElement): HTMLElement {
	return container.querySelector('.antilia-split-divider')!;
}

function shares(container: HTMLElement): [string, string] {
	const style = split(container).style;
	return [style.getPropertyValue('--antilia-split-first'),
		style.getPropertyValue('--antilia-split-second')];
}

afterEach(cleanup);

describe('SplitPanel', () => {
	it('renders the panes and the divider, which the core makes a separator', () => {
		const { container } = render(BoundSplitPanel);

		expect(split(container).className).toBe('antilia-split antilia-split-horizontal');
		expect(Array.from(split(container).children, (child) => child.className)).toEqual([
			'antilia-split-pane antilia-split-first', 'antilia-split-divider',
			'antilia-split-pane antilia-split-second']);
		expect(container.querySelector('.antilia-split-first')?.textContent).toBe('One');
		expect(container.querySelector('.antilia-split-second')?.textContent).toBe('Two');
		expect(divider(container).getAttribute('role')).toBe('separator');
		expect(divider(container).getAttribute('aria-label')).toBe('Resize');
		expect(shares(container)).toEqual(['40', '60']);
	});

	it('binds the position the user gives the divider', async () => {
		const onresized = vi.fn();
		const { container } = render(BoundSplitPanel, { onresized });

		await fireEvent.keyDown(divider(container), { key: 'ArrowRight' });
		flushSync();

		expect(onresized).toHaveBeenCalledWith(45);
		expect(container.querySelector('output')?.textContent).toBe('45');
	});

	it('moves the divider when the page changes the position', async () => {
		const { container } = render(BoundSplitPanel);

		await fireEvent.click(container.querySelector('.reset')!);
		flushSync();

		expect(shares(container)).toEqual(['20', '80']);
		expect(divider(container).getAttribute('aria-valuenow')).toBe('20');
	});

	it('changes its orientation and whether it can be resized', async () => {
		const { container, rerender } = render(BoundSplitPanel);

		await rerender({ orientation: 'vertical', resizable: false });

		expect(split(container).className).toBe(
			'antilia-split antilia-split-vertical antilia-split-fixed');
		expect(divider(container).getAttribute('aria-orientation')).toBe('horizontal');
		expect(divider(container).getAttribute('aria-disabled')).toBe('true');
	});

	it('binds the pane the user collapses, and restores it when the page says', async () => {
		const { container } = render(BoundSplitPanel, { collapsible: true });

		await fireEvent.click(container.querySelector('.antilia-split-collapse-second')!);
		flushSync();
		expect(container.querySelector('.collapsed')?.textContent).toBe('second');
		expect(split(container).classList.contains('antilia-split-collapsed-second')).toBe(true);

		await fireEvent.click(container.querySelector('.restore')!);
		flushSync();
		expect(split(container).classList.contains('antilia-split-collapsed-second')).toBe(false);
	});

	it('keeps the collapsed pane when Svelte writes its classes again', async () => {
		const { container, rerender } = render(BoundSplitPanel, { collapsible: true,
			collapsed: 'first' });

		await rerender({ orientation: 'vertical' });

		expect(split(container).className).toBe('antilia-split antilia-split-vertical ' +
			'antilia-split-collapsible antilia-split-collapsed-first');
	});

	it('binds a position in pixels', async () => {
		const { container } = render(BoundSplitPanel, { position: '200px' });

		expect(split(container).classList.contains('antilia-split-pixels')).toBe(true);
		await fireEvent.keyDown(divider(container), { key: 'Home' });
		flushSync();
		expect(container.querySelector('output')?.textContent).toBe('0px');
	});
});
