/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

import { createRef, StrictMode } from 'react';
import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { activate, DynamicDataTable, type DataTableQuery } from '@advanced-widgets/core';

import { DataTable, type DataTableColumnDef, type DataTableHandle } from '../src/index.js';
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

function rows(container: HTMLElement): string[][] {
	return Array.from(container.querySelectorAll('tbody tr'), (tr) =>
		Array.from(tr.querySelectorAll('td'), (td) => td.textContent ?? ''));
}

function headers(container: HTMLElement): string[] {
	return Array.from(container.querySelectorAll('tr.dynamic-data-table-headers th'),
		(th) => th.textContent ?? '');
}

beforeAll(async () => {
	await activate(demoKey);
});

afterEach(cleanup);

describe('DataTable', () => {
	it('shows the placeholder of a private component without a license key', async () => {
		const { container } = render(<DataTable columns={COLUMNS} loadRows={loadRows}
			licenseKey="AW1.not.valid" />);

		await waitFor(() => expect(container.querySelector('.wicket-unlicensed')).not.toBeNull());
		expect(container.querySelector('table')).toBeNull();
	});

	it('renders the skeleton of the Wicket table and the rows loadRows gives', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(<StrictMode><DataTable columns={COLUMNS} loadRows={load}
			rowsPerPage={25} /></StrictMode>);

		await waitFor(() => expect(rows(container).length).toBe(25));
		const table = container.querySelector('table')!;
		expect(table.className).toBe('dynamic-dataview dynamic-data-table');
		expect(headers(container)).toEqual(['Name▲▼', 'City']);
		expect(rows(container)[0]).toEqual(['Person 01', 'City 0']);
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 1 to 25 of 60');
		expect(Array.from(container.querySelector('thead .navigator')!.querySelectorAll('.goto'),
			(link) => link.textContent)).toEqual(['1', '2', '3']);
		expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25, sort: null,
			filter: undefined });
		expect(Object.keys(DynamicDataTable.instances).length).toBe(1);
	});

	it('pages, sorts and filters through loadRows', async () => {
		const load = vi.fn(loadRows);
		const { container, rerender } = render(<DataTable columns={COLUMNS} loadRows={load} />);
		await waitFor(() => expect(rows(container).length).toBe(25));

		fireEvent.click(container.querySelector('thead a.next')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 26'));
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 26 to 50 of 60');

		fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25,
			sort: { property: 'name', ascending: true }, filter: undefined }));
		fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));
		expect(container.querySelector('th[aria-sort]')?.getAttribute('aria-sort'))
			.toBe('descending');
		expect(container.querySelector('th[aria-sort]')?.className).toBe('wicket_orderDown');

		rerender(<DataTable columns={COLUMNS} loadRows={load} filter="Person 1" />);
		await waitFor(() => expect(rows(container).length).toBe(10));
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 1 to 10 of 10');
	});

	it('reports the actions of the rows and the selection', async () => {
		const onAction = vi.fn();
		const onSelectionChange = vi.fn();
		const { container } = render(<DataTable columns={COLUMNS} loadRows={loadRows}
			selection="many" onAction={onAction} onSelectionChange={onSelectionChange} />);
		await waitFor(() => expect(rows(container).length).toBe(25));

		fireEvent.click(container.querySelectorAll<HTMLElement>('tbody a[data-dt-action]')[2]);
		expect(onAction).toHaveBeenCalledWith('open', 'Person 03');

		fireEvent.click(container.querySelectorAll<HTMLInputElement>(
			'tbody .dynamic-data-table-select')[1]);
		expect(onSelectionChange).toHaveBeenLastCalledWith(['Person 02']);

		fireEvent.click(container.querySelector<HTMLInputElement>('.dynamic-data-table-select-page')!);
		expect(onSelectionChange.mock.lastCall![0].length).toBe(25);
		expect(onAction).toHaveBeenCalledTimes(1);
	});

	it('shows and hides the hideable columns with the column chooser', async () => {
		const { container } = render(<DataTable columns={COLUMNS} loadRows={loadRows} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		const chooser = container.querySelector('thead .dynamic-data-table-column-chooser')!;

		fireEvent.click(chooser.querySelector('[data-dt-column="number"]')!);
		fireEvent.click(chooser.querySelector('[data-dt-column="city"]')!);
		fireEvent.click(chooser.querySelector('[data-dt-apply]')!);

		await waitFor(() => expect(headers(container)).toEqual(['Name▲▼', '#']));
		await waitFor(() => expect(rows(container)[0]).toEqual(['Person 01', '1']));
	});

	it('repaints a row pushed to it, and through its ref', async () => {
		const ref = createRef<DataTableHandle>();
		let push: ((message: { type: 'updateRow'; key: string; data: unknown }) => void) | null =
			null;
		const { container } = render(<DataTable ref={ref} columns={COLUMNS} loadRows={loadRows}
			subscribe={(listener) => {
				push = listener;
				return () => {
					push = null;
				};
			}} />);
		await waitFor(() => expect(rows(container).length).toBe(25));

		act(() => push!({ type: 'updateRow', key: 'Person 02', data: { name: 'Pushed', city: 'X' } }));
		expect(rows(container)[1]).toEqual(['Pushed', 'X']);

		act(() => ref.current!.renderRow('Person 03', { name: 'Ref', city: 'Y' }));
		expect(rows(container)[2]).toEqual(['Ref', 'Y']);
	});

	it('scrolls infinitely, loading the next rows after those shown', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(<DataTable columns={COLUMNS} loadRows={load} infinite
			rowsPerPage={20} />);
		await waitFor(() => expect(rows(container).length).toBe(20));
		expect(container.querySelector('.navigator')).toBeNull();

		await act(() => Object.values(DynamicDataTable.instances)[0].loadMore());

		expect(load).toHaveBeenLastCalledWith({ first: 20, count: 20, sort: null,
			filter: undefined });
		expect(rows(container).length).toBe(40);
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 1 to 40 of 60');
	});

	it('pages, sorts and filters a local list of rows, and shows another list given', async () => {
		const list = PEOPLE.map((person) => ({ key: person.name, data: person }));
		const { container, rerender } = render(<DataTable columns={COLUMNS} rows={list} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 1 to 25 of 60');

		fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		fireEvent.click(container.querySelector('a.dynamic-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));

		rerender(<DataTable columns={COLUMNS} rows={list} filter="city 3" />);
		await waitFor(() => expect(rows(container).length).toBe(9));
		expect(rows(container)[0]).toEqual(['Person 60', 'City 3']);

		rerender(<DataTable columns={COLUMNS} rows={list.slice(0, 3)} filter="city 3" />);
		await waitFor(() => expect(rows(container).length).toBe(0));
		rerender(<DataTable columns={COLUMNS} rows={list.slice(0, 3)} />);
		await waitFor(() => expect(rows(container).length).toBe(3));
		expect(container.querySelector('.navigatorLabel')?.textContent).toBe('Showing 1 to 3 of 3');
	});
});
