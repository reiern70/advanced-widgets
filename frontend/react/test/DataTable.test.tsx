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

import { createRef, StrictMode } from 'react';
import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

import { activate, DynamicDataTable, type DataTableQuery } from '@antilia/core';

import { DataTable, type DataTableColumnDef, type DataTableHandle } from '../src/index.js';
// the key of the demo application, activating every component
import demoKey from '../../../demo-app/src/main/resources/META-INF/antilia-widgets.license?raw';

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
	return Array.from(container.querySelectorAll('tr.ant-data-table-headers th'),
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

		await waitFor(() => expect(container.querySelector('.antilia-unlicensed')).not.toBeNull());
		expect(container.querySelector('table')).toBeNull();
	});

	it('renders the skeleton of the Wicket table and the rows loadRows gives', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(<StrictMode><DataTable columns={COLUMNS} loadRows={load}
			rowsPerPage={25} /></StrictMode>);

		await waitFor(() => expect(rows(container).length).toBe(25));
		const table = container.querySelector('table')!;
		expect(table.className).toBe('ant-dataview ant-data-table');
		expect(headers(container)).toEqual(['Name▲▼', 'City']);
		expect(rows(container)[0]).toEqual(['Person 01', 'City 0']);
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));
		expect(Array.from(container.querySelector('thead .navigator')!.querySelectorAll('.goto'),
			(link) => link.textContent)).toEqual(['1', '2', '3']);
		// the rows per page early, in sight on a table wider than the window
		expect(Array.from(container.querySelector('thead .ant-data-table-navigation')!.children,
			(part) => part.className)).toEqual(['ant-data-table-page-size', 'ant-data-table-paging',
			'ant-data-table-actions']);
		expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25, sort: null,
			filter: undefined });
		expect(Object.keys(DynamicDataTable.instances).length).toBe(1);
	});

	it('pages, sorts and filters through loadRows', async () => {
		const load = vi.fn(loadRows);
		const { container, rerender } = render(<DataTable columns={COLUMNS} loadRows={load} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		// the rows show before React renders their count, and with it the link to the next page
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));

		fireEvent.click(container.querySelector('thead a.next')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 26'));
		await waitFor(() => expect(label(container)).toBe('Showing 26 to 50 of 60'));

		fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await waitFor(() => expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25,
			sort: { property: 'name', ascending: true }, filter: undefined }));
		fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));
		expect(container.querySelector('th[aria-sort]')?.getAttribute('aria-sort'))
			.toBe('descending');
		expect(container.querySelector('th[aria-sort]')?.className).toBe('wicket_orderDown');

		rerender(<DataTable columns={COLUMNS} loadRows={load} filter="Person 1" />);
		await waitFor(() => expect(rows(container).length).toBe(10));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 10 of 10'));
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
			'tbody .ant-data-table-select')[1]);
		expect(onSelectionChange).toHaveBeenLastCalledWith(['Person 02']);

		fireEvent.click(container.querySelector<HTMLInputElement>('.ant-data-table-select-page')!);
		expect(onSelectionChange.mock.lastCall![0].length).toBe(25);
		expect(onAction).toHaveBeenCalledTimes(1);
	});

	it('keeps the selection while paging, unless told to clear it, and clears it through its ref',
		async () => {
			const ref = createRef<DataTableHandle>();
			const onSelectionChange = vi.fn();
			const { container, rerender } = render(<DataTable ref={ref} columns={COLUMNS}
				loadRows={loadRows} selection="many" onSelectionChange={onSelectionChange} />);
			await waitFor(() => expect(rows(container).length).toBe(25));
			const checkboxes = () => container.querySelectorAll<HTMLInputElement>(
				'tbody .ant-data-table-select');

			fireEvent.click(checkboxes()[1]);
			fireEvent.click(container.querySelector('thead a.next')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 26'));
			fireEvent.click(container.querySelector('thead a.prev')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 01'));
			expect(checkboxes()[1].checked).toBe(true);
			expect(ref.current!.selectedKeys()).toEqual(['Person 02']);

			rerender(<DataTable ref={ref} columns={COLUMNS} loadRows={loadRows} selection="many"
				onSelectionChange={onSelectionChange} clearSelectionOnNavigation />);
			fireEvent.click(container.querySelector('thead a.next')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 26'));
			expect(onSelectionChange).toHaveBeenLastCalledWith([]);
			fireEvent.click(container.querySelector('thead a.prev')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 01'));
			expect(checkboxes()[1].checked).toBe(false);

			fireEvent.click(checkboxes()[3]);
			expect(onSelectionChange).toHaveBeenLastCalledWith(['Person 04']);
			act(() => ref.current!.clearSelection());
			expect(onSelectionChange).toHaveBeenLastCalledWith([]);
			expect(checkboxes()[3].checked).toBe(false);
			expect(ref.current!.selectedKeys()).toEqual([]);
		});

	it('counts the selected rows in the navigation, with a bin deselecting them all', async () => {
		const onSelectionChange = vi.fn();
		const { container, rerender } = render(<DataTable columns={COLUMNS} loadRows={loadRows}
			selection="many" onSelectionChange={onSelectionChange}
			texts={{ selectedCount: '{0} chosen' }} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		const checkboxes = () => container.querySelectorAll<HTMLInputElement>(
			'tbody .ant-data-table-select');
		const count = () => container.querySelector('thead .ant-data-table-selection-count');
		expect(count()).toBeNull();

		fireEvent.click(checkboxes()[1]);
		fireEvent.click(checkboxes()[2]);
		expect(count()!.querySelector('[role="status"]')!.textContent).toBe('2 chosen');
		const bin = count()!.querySelector<HTMLButtonElement>('.ant-data-table-selection-clear')!;
		expect(bin.getAttribute('aria-label')).toBe('Clear selection');

		fireEvent.click(bin);
		expect(onSelectionChange).toHaveBeenLastCalledWith([]);
		expect(checkboxes()[1].checked).toBe(false);
		expect(count()).toBeNull();

		rerender(<DataTable columns={COLUMNS} loadRows={loadRows} selection="many"
			onSelectionChange={onSelectionChange} selectionCount={false} />);
		fireEvent.click(checkboxes()[1]);
		expect(onSelectionChange).toHaveBeenLastCalledWith(['Person 02']);
		expect(count()).toBeNull();
	});

	it('shows the navigation above the rows, below them or both', async () => {
		const { container, rerender } = render(<DataTable columns={COLUMNS} loadRows={loadRows} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		expect(container.querySelector('thead tr.navigation')).not.toBeNull();
		expect(container.querySelector('tfoot')).toBeNull();

		rerender(<DataTable columns={COLUMNS} loadRows={loadRows} navigationPosition="both" />);
		expect(container.querySelectorAll('thead tr.navigation, tfoot tr.navigation').length)
			.toBe(2);

		rerender(<DataTable columns={COLUMNS} loadRows={loadRows} navigationPosition="bottom" />);
		expect(container.querySelector('thead tr.navigation')).toBeNull();
		expect(container.querySelector('tfoot tr.navigation')).not.toBeNull();
	});

	it('shows and hides the hideable columns with the column chooser', async () => {
		const { container } = render(<DataTable columns={COLUMNS} loadRows={loadRows} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		const chooser = container.querySelector('thead .ant-data-table-column-chooser')!;

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
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 40 of 60'));
	});

	it('pages, sorts and filters a local list of rows, and shows another list given', async () => {
		const list = PEOPLE.map((person) => ({ key: person.name, data: person }));
		const { container, rerender } = render(<DataTable columns={COLUMNS} rows={list} />);
		await waitFor(() => expect(rows(container).length).toBe(25));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));

		fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));

		rerender(<DataTable columns={COLUMNS} rows={list} filter="city 3" />);
		await waitFor(() => expect(rows(container).length).toBe(9));
		expect(rows(container)[0]).toEqual(['Person 60', 'City 3']);

		rerender(<DataTable columns={COLUMNS} rows={list.slice(0, 3)} filter="city 3" />);
		await waitFor(() => expect(rows(container).length).toBe(0));
		rerender(<DataTable columns={COLUMNS} rows={list.slice(0, 3)} />);
		await waitFor(() => expect(rows(container).length).toBe(3));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 3 of 3'));
	});
});
