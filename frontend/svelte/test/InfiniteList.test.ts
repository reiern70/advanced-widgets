/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, render, waitFor } from '@testing-library/svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { JsonInfiniteList, type InfiniteListData } from '@advanced-widgets/core';

import { InfiniteList } from '../src/index.js';

const TEXTS = { loading: 'Loading…', end: 'All ${count} shown', empty: 'None' };

function load(more: boolean): Promise<InfiniteListData> {
	const from = more ? 3 : 1;
	return Promise.resolve({ itemCount: 4, items: [0, 1].map((i) => ({
		key: 'k' + (from + i), number: from + i, data: { name: 'Item ' + (from + i) }
	})) });
}

function items(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('.json-infinite-list-item'),
		(item) => item.textContent ?? '');
}

afterEach(cleanup);

describe('InfiniteList', () => {
	it('renders the items it loads, and the next page', async () => {
		const { container } = render(InfiniteList, { load, template: '<p>{{name}}</p>',
			texts: TEXTS });

		await waitFor(() => expect(items(container)).toEqual(['Item 1', 'Item 2']));
		await Object.values(JsonInfiniteList.instances)[0].loadMore();

		expect(items(container)).toEqual(['Item 1', 'Item 2', 'Item 3', 'Item 4']);
	});

	it('reports the action of an item', async () => {
		const onaction = vi.fn();
		const { container } = render(InfiniteList, { load, texts: TEXTS, onaction,
			template: '<p data-dt-action="open">{{name}}</p>' });
		await waitFor(() => expect(items(container).length).toBe(2));

		container.querySelectorAll<HTMLElement>('.json-infinite-list-item')[0].click();

		expect(onaction).toHaveBeenCalledWith('open', 'k1');
	});

	it('reloads and repaints items through its instance', async () => {
		let name = 'Item';
		const named = (more: boolean) => load(more).then((data) => ({ ...data,
			items: data.items.map((item) => ({ ...item, data: { name: name + ' ' + item.number } })) }));
		const { container, component } = render(InfiniteList, { load: named,
			template: '<p>{{name}}</p>', texts: TEXTS });
		await waitFor(() => expect(items(container)).toEqual(['Item 1', 'Item 2']));

		component.renderItem('k2', { name: 'Changed' });
		expect(items(container)).toEqual(['Item 1', 'Changed']);

		name = 'Again';
		await component.reload();
		expect(items(container)).toEqual(['Again 1', 'Again 2']);
	});

	it('scrolls in the wrapper of its scroll bars when it has a height, and stops on unmount',
		async () => {
			const { container, unmount } = render(InfiniteList, { load, texts: TEXTS,
				template: '<p>{{name}}</p>', height: { value: 300, unit: 'px' } });
			await waitFor(() => expect(items(container).length).toBe(2));

			expect(container.querySelectorAll('.wicket-scrollbar .wicket-scrollbar-track').length)
				.toBe(1);
			unmount();
			expect(JsonInfiniteList.instances).toEqual({});
		});
});
