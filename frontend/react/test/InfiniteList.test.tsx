/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createRef, StrictMode } from 'react';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { JsonInfiniteList, type InfiniteListData } from '@antilia/core';

import { InfiniteList, type InfiniteListHandle } from '../src/index.js';

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
	it('renders the items it loads into the list React renders', async () => {
		const { container } = render(<StrictMode><InfiniteList load={load}
			template="<p>{{name}}</p>" texts={TEXTS} /></StrictMode>);

		await waitFor(() => expect(items(container)).toEqual(['Item 1', 'Item 2']));
		expect(container.firstElementChild?.className).toBe('json-infinite-list');
		expect(Object.keys(JsonInfiniteList.instances).length).toBe(1);

		await act(() => Object.values(JsonInfiniteList.instances)[0].loadMore());
		expect(items(container)).toEqual(['Item 1', 'Item 2', 'Item 3', 'Item 4']);
		expect(container.querySelector('.json-infinite-list-status')?.textContent)
			.toBe('All 4 shown');
	});

	it('reports the action of an item', async () => {
		const onAction = vi.fn();
		const { container } = render(<InfiniteList load={load}
			template={'<p data-dt-action="open">{{name}}</p>'} texts={TEXTS} onAction={onAction} />);
		await waitFor(() => expect(items(container).length).toBe(2));

		container.querySelectorAll<HTMLElement>('.json-infinite-list-item')[1].click();

		expect(onAction).toHaveBeenCalledWith('open', 'k2');
	});

	it('reloads and repaints items through its ref', async () => {
		const ref = createRef<InfiniteListHandle>();
		let name = 'Item';
		const named = (more: boolean) => load(more).then((data) => ({ ...data,
			items: data.items.map((item) => ({ ...item, data: { name: name + ' ' + item.number } })) }));
		const { container } = render(<StrictMode><InfiniteList ref={ref} load={named}
			template="<p>{{name}}</p>" texts={TEXTS} /></StrictMode>);
		await waitFor(() => expect(items(container)).toEqual(['Item 1', 'Item 2']));

		act(() => ref.current!.renderItem('k2', { name: 'Changed' }));
		expect(items(container)).toEqual(['Item 1', 'Changed']);

		name = 'Again';
		await act(() => ref.current!.reload());
		expect(items(container)).toEqual(['Again 1', 'Again 2']);
	});

	it('scrolls in the wrapper of its scroll bars when it has a height', async () => {
		const { container, unmount } = render(<InfiniteList load={load} template="<p>{{name}}</p>"
			texts={TEXTS} height={{ value: 300, unit: 'px' }} />);
		await waitFor(() => expect(items(container).length).toBe(2));

		const wrapper = container.firstElementChild!;
		expect(wrapper.className).toContain('antilia-scrollbar');
		expect(wrapper.querySelectorAll('.antilia-scrollbar-track').length).toBe(1);

		unmount();
		expect(JsonInfiniteList.instances).toEqual({});
	});
});
