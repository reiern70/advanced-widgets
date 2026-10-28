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

import { cleanup, fireEvent, render, waitFor } from '@testing-library/svelte';
import { flushSync } from 'svelte';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { DynamicDataTable, type DataTableQuery } from '@antilia/core';

import { DataTable, type DataTableColumnDef } from '../src/index.js';

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
		(th) => (th.textContent ?? '').replace(/\s+/g, ''));
}

afterEach(cleanup);

describe('DataTable', () => {
	it('renders the skeleton of the Wicket table and the rows loadRows gives', async () => {
		const load = vi.fn(loadRows);
		const { container } = render(DataTable, { columns: COLUMNS, loadRows: load });

		await waitFor(() => expect(rows(container).length).toBe(25));
		expect(container.querySelector('table')?.className).toBe('ant-dataview ant-data-table');
		expect(headers(container)).toEqual(['Name▲▼', 'City']);
		expect(rows(container)[0]).toEqual(['Person 01', 'City 0']);
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 25 of 60'));
		// the rows per page early, in sight on a table wider than the window
		expect(Array.from(container.querySelector('thead .ant-data-table-navigation')!.children,
			(part) => part.className)).toEqual(['ant-data-table-page-size', 'ant-data-table-paging',
			'ant-data-table-actions']);
		expect(load).toHaveBeenCalledTimes(1);
		expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25, sort: null,
			filter: undefined });
	});

	it('gives the header cells of the columns their minimum widths', async () => {
		const columns = [{ ...COLUMNS[0], minWidth: 160 }, ...COLUMNS.slice(1)];
		const { container } = render(DataTable, { columns, loadRows, resizableColumns: 'stretch',
			adjustToParent: true });
		await waitFor(() => expect(rows(container).length).toBe(25));

		const cells = Array.from(container.querySelectorAll('thead tr:last-child th'));
		expect(cells.map((cell) => cell.getAttribute('data-min-width'))).toEqual(['160', null]);
	});

	it('pages, sorts and filters through loadRows', async () => {
		const load = vi.fn(loadRows);
		const { container, rerender } = render(DataTable, { columns: COLUMNS, loadRows: load });
		await waitFor(() => expect(rows(container).length).toBe(25));

		await fireEvent.click(container.querySelector('thead a.next')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 26'));
		await waitFor(() => expect(label(container)).toBe('Showing 26 to 50 of 60'));

		await fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await waitFor(() => expect(load).toHaveBeenLastCalledWith({ first: 0, count: 25,
			sort: { property: 'name', ascending: true }, filter: undefined }));
		await fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
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

		container.querySelectorAll<HTMLInputElement>('tbody .ant-data-table-select')[1].click();
		expect(onselectionchange).toHaveBeenLastCalledWith(['Person 02']);

		container.querySelector<HTMLInputElement>('.ant-data-table-select-page')!.click();
		expect(onselectionchange.mock.lastCall![0].length).toBe(25);
		expect(onaction).toHaveBeenCalledTimes(1);
	});

	it('keeps the selection while paging, unless told to clear it, and clears it on call',
		async () => {
			const onselectionchange = vi.fn();
			const { container, component, rerender } = render(DataTable, { columns: COLUMNS,
				loadRows, selection: 'many', onselectionchange });
			await waitFor(() => expect(rows(container).length).toBe(25));
			const checkboxes = () => container.querySelectorAll<HTMLInputElement>(
				'tbody .ant-data-table-select');

			checkboxes()[1].click();
			await fireEvent.click(container.querySelector('thead a.next')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 26'));
			await fireEvent.click(container.querySelector('thead a.prev')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 01'));
			expect(checkboxes()[1].checked).toBe(true);
			expect(component.selectedKeys()).toEqual(['Person 02']);

			// another prop keeps the instance and its selection
			await rerender({ clearSelectionOnNavigation: true });
			expect(component.selectedKeys()).toEqual(['Person 02']);
			await fireEvent.click(container.querySelector('thead a.next')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 26'));
			expect(onselectionchange).toHaveBeenLastCalledWith([]);
			await fireEvent.click(container.querySelector('thead a.prev')!);
			await waitFor(() => expect(rows(container)[0][1]).toBe('Person 01'));
			expect(checkboxes()[1].checked).toBe(false);

			checkboxes()[3].click();
			expect(onselectionchange).toHaveBeenLastCalledWith(['Person 04']);
			component.clearSelection();
			expect(onselectionchange).toHaveBeenLastCalledWith([]);
			expect(checkboxes()[3].checked).toBe(false);
			expect(component.selectedKeys()).toEqual([]);
		});

	it('counts the selected rows in the navigation, with a bin deselecting them all', async () => {
		const onselectionchange = vi.fn();
		const { container, rerender } = render(DataTable, { columns: COLUMNS, loadRows,
			selection: 'many', onselectionchange, texts: { selectedCount: '{0} chosen' } });
		await waitFor(() => expect(rows(container).length).toBe(25));
		const checkboxes = () => container.querySelectorAll<HTMLInputElement>(
			'tbody .ant-data-table-select');
		const count = () => container.querySelector('thead .ant-data-table-selection-count');
		expect(count()).toBeNull();

		checkboxes()[1].click();
		checkboxes()[2].click();
		flushSync();
		expect(count()!.querySelector('[role="status"]')!.textContent).toBe('2 chosen');
		const bin = count()!.querySelector<HTMLButtonElement>('.ant-data-table-selection-clear')!;
		expect(bin.getAttribute('aria-label')).toBe('Clear selection');

		await fireEvent.click(bin);
		expect(onselectionchange).toHaveBeenLastCalledWith([]);
		expect(checkboxes()[1].checked).toBe(false);
		expect(count()).toBeNull();

		await rerender({ selectionCount: false });
		checkboxes()[1].click();
		flushSync();
		expect(onselectionchange).toHaveBeenLastCalledWith(['Person 02']);
		expect(count()).toBeNull();
	});

	it('shows the navigation above the rows, below them or both', async () => {
		const { container, rerender } = render(DataTable, { columns: COLUMNS, loadRows });
		await waitFor(() => expect(rows(container).length).toBe(25));
		expect(container.querySelector('thead tr.navigation')).not.toBeNull();
		expect(container.querySelector('tfoot')).toBeNull();

		await rerender({ navigationPosition: 'both' });
		expect(container.querySelectorAll('thead tr.navigation, tfoot tr.navigation').length)
			.toBe(2);

		await rerender({ navigationPosition: 'bottom' });
		expect(container.querySelector('thead tr.navigation')).toBeNull();
		expect(container.querySelector('tfoot tr.navigation')).not.toBeNull();
	});

	it('shows and hides the hideable columns with the column chooser', async () => {
		const { container } = render(DataTable, { columns: COLUMNS, loadRows });
		await waitFor(() => expect(rows(container).length).toBe(25));
		const chooser = container.querySelector('thead .ant-data-table-column-chooser')!;

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

		await fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await fireEvent.click(container.querySelector('a.ant-data-table-sort')!);
		await waitFor(() => expect(rows(container)[0][0]).toBe('Person 60'));

		await rerender({ filter: 'city 3' });
		await waitFor(() => expect(rows(container).length).toBe(9));
		expect(rows(container)[0]).toEqual(['Person 60', 'City 3']);

		await rerender({ rows: list.slice(0, 3), filter: undefined });
		await waitFor(() => expect(rows(container).length).toBe(3));
		await waitFor(() => expect(label(container)).toBe('Showing 1 to 3 of 3'));
	});
});
