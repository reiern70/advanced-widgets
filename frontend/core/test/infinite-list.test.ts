/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { afterEach, describe, expect, it, vi } from 'vitest';

import { JsonInfiniteList, setHost, standaloneHost, type InfiniteListConfig,
	type InfiniteListData } from '../src/index.js';

const TEXTS = { loading: 'Loading…', end: 'All ${count} shown', empty: 'None' };

function entries(from: number, count: number) {
	return Array.from({ length: count }, (_value, i) => ({
		key: 'k' + (from + i), number: from + i, data: { name: 'Item ' + (from + i) }
	}));
}

function config(overrides: Partial<InfiniteListConfig> = {}): InfiniteListConfig {
	document.body.innerHTML = '<div id="list"></div>';
	return { id: 'list', template: '<p data-dt-action="open">{{name}} #{{@number}}</p>',
		texts: TEXTS, ...overrides };
}

function texts(): string[] {
	return Array.from(document.querySelectorAll('.json-infinite-list-item'),
		(item) => item.textContent ?? '');
}

function status(): string {
	return document.querySelector('.json-infinite-list-status')?.textContent ?? '';
}

afterEach(() => {
	Object.values(JsonInfiniteList.instances).forEach((list) => list.destroy());
	setHost();
	document.body.innerHTML = '';
});

describe('JsonInfiniteList', () => {
	it('renders the items it is given with the template', () => {
		new JsonInfiniteList(config(), () => {}, null, { items: entries(1, 2), itemCount: 2 });

		expect(texts()).toEqual(['Item 1 #1', 'Item 2 #2']);
		const first = document.querySelector('.json-infinite-list-item')!;
		expect(first.getAttribute('aria-setsize')).toBe('2');
		expect(first.getAttribute('data-dt-action')).toBe('open');
		expect(status()).toBe('All 2 shown');
	});

	it('loads its items and the next page with load', async () => {
		const load = vi.fn((more: boolean): Promise<InfiniteListData> => Promise.resolve(
			more ? { items: entries(3, 2), itemCount: 4 } : { items: entries(1, 2), itemCount: 4 }));
		const list = new JsonInfiniteList(config({ load }), () => {});
		expect(status()).toBe('Loading…');
		await vi.waitFor(() => expect(texts().length).toBe(2));

		await list.loadMore();

		expect(load.mock.calls.map((call) => call[0])).toEqual([false, true]);
		expect(texts().length).toBe(4);
		expect(status()).toBe('All 4 shown');
	});

	it('says when there are no items', () => {
		new JsonInfiniteList(config(), () => {}, null, { items: [], itemCount: 0 });

		expect(status()).toBe('None');
	});

	it('reports an action with the key of its item, by click and by Enter', () => {
		const action = vi.fn();
		new JsonInfiniteList(config(), action, null, { items: entries(1, 2), itemCount: 2 });
		const items = document.querySelectorAll<HTMLElement>('.json-infinite-list-item');

		items[1].click();
		items[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

		expect(action.mock.calls).toEqual([['open', 'k2'], ['open', 'k1']]);
	});

	it('repaints one item in place', () => {
		const list = new JsonInfiniteList(config(), () => {}, null,
			{ items: entries(1, 2), itemCount: 2 });

		list.renderItem('k2', { name: 'Changed' });

		expect(texts()).toEqual(['Item 1 #1', 'Changed #2']);
	});

	it('hands the messages pushed for it to their handlers, and stops when removed', () => {
		const listeners: Record<string, (value: never) => void> = {};
		setHost({
			...standaloneHost,
			onMessage: (listener) => ((listeners.message = listener), () => delete listeners.message),
			onNodeRemoving: (listener) => ((listeners.removing = listener),
				() => delete listeners.removing)
		});
		const refresh = vi.fn();
		new JsonInfiniteList(config({ push: true }), () => {}, { refresh },
			{ items: entries(1, 1), itemCount: 1 });

		listeners.message('{"tableId":"other","typeId":"refresh"}' as never);
		listeners.message('{"tableId":"list","typeId":"refresh"}' as never);
		expect(refresh).toHaveBeenCalledTimes(1);

		listeners.removing(document.body as never);
		expect(JsonInfiniteList.instances.list).toBeUndefined();
		expect(listeners.message).toBeUndefined();
	});

	it('scrolls itself with the scroll bars when it has a height', () => {
		new JsonInfiniteList(config({ height: { value: 200, unit: 'px' } }), () => {}, null,
			{ items: entries(1, 1), itemCount: 1 });

		const element = document.getElementById('list')!;
		expect(element.style.height).toBe('200px');
		expect(element.parentElement?.classList.contains('wicket-scrollbar')).toBe(true);
	});
});
