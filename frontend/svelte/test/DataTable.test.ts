/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { DynamicDataTable, type DataTableQuery } from '@advanced-widgets/core';

import { activate, DataTable, type DataTableColumnDef } from '../src/index.js';
// the key of the demo application, activating every component
import demoKey from '../../../demo-app/src/main/resources/META-INF/advanced-widgets.license?raw';

interface Person {
	name: string;
	city: string;
}

const PEOPLE: Person[] = Array.from({ length: 60 }, (_, index) => ({
	name: 'Person ' + String(index + 1).padStart(2, '0'), city: 'City ' + (index % 7)
}));

const COLUMNS: DataTableColumnDef[] = [
	{ id: 'name', header: 'Name', template: '<a data-dt-action="open">{{name}}</a>',
		sort: 'name', movable: true },
	{ id: 'city', header: 'City', template: '{{city}}', movable: true, hideable: true },
	{ id: 'number', header: '#', template: '{{@number}}', hideable: true, visible: false }
];

function loadRows(query: DataTableQuery) {
	let people = PEOPLE.filter((person) => !query.filter || person.name.includes(query.filter));
	if (query.sort) {
		people = [...people].sort((a, b) => a.name.localeCompare(b.name) *
			(query.sort!.ascending ? 1 : -1));
	}
	return Promise.resolve({ itemCount: people.length, rows: people
		.slice(query.first, query.first + query.count)
		.map((person) => ({ key: person.name, data: person })) });
}

/** @returns the label of the navigation, which may follow the rows a moment later */
function label(container: HTMLElement): string | null | undefined {
	return container.querySelector('.navigatorLabel')?.textContent;
}

function rows(container: HTMLElement): string[][] {
	return Array.from(container.querySelectorAll('tbody tr'), (tr) =>
		Array.from(tr.querySelectorAll('td'), (td) => td.textContent ?? ''));
}

function headers(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('tr.dynamic-data-table-headers th'),
		(th) => (th.textContent ?? '').replace(/\s+/g, ''));
}

beforeAll(async () => {
	await activate(demoKey);
});

afterEach(cleanup);

describe('DataTable', () => {
	it('shows the placeholder of a private component without a license key', async () => {
		const { container } = render(DataTable, { columns: COLUMNS, loadRows,
			licenseKey: 'AW1.not.valid' });

		await waitFor(() => expect(container.querySelector('.wicket-unlicensed')).not.toBeNull());
		expect(container.querySelector('table')).toBeNull();
	});

	it('renders the skeleton of the Wicket table and the rows loadRows gives', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(DataTable, { columns: COLUMNS, loadRows: load });

		await waitFor(() => expect(rows(container).length).toBe(25));
		expect(container.querySelector('table')?.className).toBe('dynamic-dataview dynamic-data-table');
		expect(headers(container)).toEqual(['Name▲▼', 'City']);
		expect(rows(container)[0]).toEqual(['Person 01', 'City 0']);
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));
		expect(load).toHaveBeenCalledTimes(1);
		expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25, sort: null,
			filter: undefined });
	});

	it('pages, sorts and filters through loadRows', async () => {
		const load = vi.fn(loadRows);
		const { container, rerender } = render(DataTable, { columns: COLUMNS, loadRows: load });
		await waitFor(() => expect(rows(container).length).toBe(25));

		await fireEvent.click(container.querySelector('thead a.next')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 26'));
		await waitFor(() => expect(label(container)).toBe('Showing 26 to 50 of 60'));

		await fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25,
			sort: { property: 'name', ascending: true }, filter: undefined }));
		await fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));
		expect(container.querySelector('th[aria-sort]')?.className).toBe('wicket_orderDown');

		await rerender({ filter: 'Person 1' });
		await waitFor(() => expect(rows(container).length).toBe(10));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 10 of 10'));
	});

	it('reports the actions of the rows and the selection', async () => {
		const onaction = vi.fn();
		const onselectionchange = vi.fn();
		const { container } = render(DataTable, { columns: COLUMNS, loadRows, selection: 'many',
			onaction, onselectionchange });
		await waitFor(() => expect(rows(container).length).toBe(25));

		container.querySelectorAll<HTMLElement>('tbody a[data-dt-action]')[2].click();
		expect(onaction).toHaveBeenCalledWith('open', 'Person 03');

		container.querySelectorAll<HTMLInputElement>('tbody .dynamic-data-table-select')[1].click();
		expect(onselectionchange).toHaveBeenLastCalledWith(['Person 02']);

		container.querySelector<HTMLInputElement>('.dynamic-data-table-select-page')!.click();
		expect(onselectionchange.mock.lastCall![0].length).toBe(25);
		expect(onaction).toHaveBeenCalledTimes(1);
	});

	it('shows and hides the hideable columns with the column chooser', async () => {
		const { container } = render(DataTable, { columns: COLUMNS, loadRows });
		await waitFor(() => expect(rows(container).length).toBe(25));
		const chooser = container.querySelector('thead .dynamic-data-table-column-chooser')!;

		chooser.querySelector<HTMLElement>('[data-dt-column="number"]')!.click();
		chooser.querySelector<HTMLElement>('[data-dt-column="city"]')!.click();
		chooser.querySelector<HTMLElement>('[data-dt-apply]')!.click();
		flushSync();

		await waitFor(() => expect(headers(container)).toEqual(['Name▲▼', '#']));
		await waitFor(() => expect(rows(container)[0]).toEqual(['Person 01', '1']));
	});

	it('repaints a row pushed to it, and through its instance', async () => {
		let push: ((message: { type: 'updateRow'; key: string; data: unknown }) => void) | null =
			null;
		const { container, component } = render(DataTable, { columns: COLUMNS, loadRows,
			subscribe: (listener) => {
				push = listener;
				return () => {
					push = null;
				};
			} });
		await waitFor(() => expect(rows(container).length).toBe(25));

		push!({ type: 'updateRow', key: 'Person 02', data: { name: 'Pushed', city: 'X' } });
		expect(rows(container)[1]).toEqual(['Pushed', 'X']);

		component.renderRow('Person 03', { name: 'Instance', city: 'Y' });
		expect(rows(container)[2]).toEqual(['Instance', 'Y']);
	});

	it('scrolls infinitely, loading the next rows after those shown', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(DataTable, { columns: COLUMNS, loadRows: load,
			infinite: true, rowsPerPage: 20 });
		await waitFor(() => expect(rows(container).length).toBe(20));
		expect(container.querySelector('.navigator')).toBeNull();

		await Object.values(DynamicDataTable.instances)[0].loadMore();
		flushSync();

		expect(load).toHaveBeenLastCalledWith({ first: 20, count: 20, sort: null,
			filter: undefined });
		expect(rows(container).length).toBe(40);
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 40 of 60'));
	});

	it('pages, sorts and filters a local list of rows, and shows another list given', async () => {
		const list = PEOPLE.map((person) => ({ key: person.name, data: person }));
		const { container, rerender } = render(DataTable, { columns: COLUMNS, rows: list });
		await waitFor(() => expect(rows(container).length).toBe(25));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));

		await fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));

		await rerender({ filter: 'city 3' });
		await waitFor(() => expect(rows(container).length).toBe(9));
		expect(rows(container)[0]).toEqual(['Person 60', 'City 3']);

		await rerender({ rows: list.slice(0, 3), filter: undefined });
		await waitFor(() => expect(rows(container).length).toBe(3));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 3 of 3'));
	});
});
