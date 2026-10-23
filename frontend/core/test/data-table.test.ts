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

import { afterEach, describe, expect, it, vi } from 'vitest';

import { DynamicDataTable, registerHandlebars, ResizableColumns, setHost, standaloneHost,
	Templates, type DataTableConfig, type DataTableData } from '../src/index.js';

const HEAD = '<thead><tr class="ant-data-table-headers"><th>Name</th><th>City</th></tr></thead>';

function rows(from: number, count: number): DataTableData['rows'] {
	return Array.from({ length: count }, (_value, i) => ({
		key: 'k' + (from + i), number: from + i,
		data: { name: 'User ' + (from + i), city: 'City <' + (from + i) + '>' }
	}));
}

function config(overrides: Partial<DataTableConfig> = {}): DataTableConfig {
	document.body.innerHTML = '<table id="table">' + HEAD + '<tbody></tbody></table>';
	return {
		id: 'table',
		engine: 'builtin',
		columns: [
			{ template: '<a data-dt-action="open">{{name}}</a> #{{@number}}' },
			{ template: '{{city}}', cssClass: 'city' }
		],
		...overrides
	};
}

function cells(): string[][] {
	return Array.from(document.querySelectorAll('#table tbody tr'), (tr) =>
		Array.from(tr.querySelectorAll('td'), (td) => td.textContent ?? ''));
}

afterEach(() => {
	Object.values(DynamicDataTable.instances).forEach((table) => table.destroy());
	setHost();
	delete Templates.engines.handlebars;
	document.body.innerHTML = '';
});

describe('DynamicDataTable', () => {
	it('renders the rows its load gives with the templates of the columns, escaped', async () => {
		const load = vi.fn((more: boolean) => Promise.resolve<DataTableData>({
			rows: rows(more ? 3 : 1, 2), itemCount: 4, currentPage: 0 }));
		const table = new DynamicDataTable(config({ load }), () => undefined);
		await vi.waitFor(() => expect(cells().length).toBe(2));

		expect(load).toHaveBeenCalledWith(false);
		expect(cells()).toEqual([['User 1 #1', 'City <1>'], ['User 2 #2', 'City <2>']]);
		expect(document.querySelector('#table tbody td.city')?.innerHTML).toBe('City &lt;1&gt;');
		expect(document.querySelector('#table tbody tr')?.id).toBe('table-row-k1');
		expect(table.itemCount).toBe(4);

		await table.loadMore();
		expect(load).toHaveBeenLastCalledWith(true);
		expect(cells().map((row) => row[0])).toEqual(['User 1 #1', 'User 2 #2', 'User 3 #3',
			'User 4 #4']);
	});

	it('scrolling infinitely, watches the last row of the first rows it loads', async () => {
		const observed: Element[] = [];
		const Observer = vi.fn(function (this: object) {
			Object.assign(this, { observe: (element: Element) => observed.push(element),
				disconnect: () => observed.splice(0) });
		});
		vi.stubGlobal('IntersectionObserver', Observer);
		try {
			const navigation = vi.fn();
			new DynamicDataTable(config({ infinite: true, load: () => Promise.resolve({
				rows: rows(1, 2), itemCount: 4 }) }), () => undefined, navigation);
			await vi.waitFor(() => expect(cells().length).toBe(2));

			expect(observed.map((element) => element.id)).toEqual(['table-row-k2']);
			expect(navigation).toHaveBeenCalledTimes(1);
		} finally {
			vi.unstubAllGlobals();
		}
	});

	it('fetches the rows from its data URL, the next page with more=true', async () => {
		const fetch = vi.spyOn(window, 'fetch').mockImplementation((url) => Promise.resolve(
			new Response(JSON.stringify({ rows: rows(String(url).includes('more') ? 3 : 1, 2),
				itemCount: 4 }))));
		const table = new DynamicDataTable(config({ dataUrl: '/rows?page=0' }), () => undefined);
		await vi.waitFor(() => expect(cells().length).toBe(2));
		await table.loadMore();

		expect(fetch.mock.calls.map((call) => call[0])).toEqual(['/rows?page=0',
			'/rows?page=0&more=true']);
		expect(cells().length).toBe(4);
		fetch.mockRestore();
	});

	it('reports the action of a clicked element with the key of its row', () => {
		const action = vi.fn();
		new DynamicDataTable(config(), action, null, null, { rows: rows(1, 2), itemCount: 2 });

		document.querySelectorAll<HTMLElement>('#table [data-dt-action]')[1].click();

		expect(action).toHaveBeenCalledWith('open', 'k2');
	});

	it('repaints a row in place, keeping its number', () => {
		const table = new DynamicDataTable(config(), () => undefined, null, null,
			{ rows: rows(1, 3), itemCount: 3 });

		table.renderRow('k2', { name: 'Changed', city: 'Here' });

		expect(cells()).toEqual([['User 1 #1', 'City <1>'], ['Changed #2', 'Here'],
			['User 3 #3', 'City <3>']]);
	});

	it('selects rows with their checkboxes and shows it in the header checkbox', () => {
		const action = vi.fn();
		const cfg = config({ columns: [{ template: '<input type="checkbox" ' +
			'class="ant-data-table-select" data-dt-action="select" ' +
			'data-dt-action-unchecked="deselect">' }] });
		document.querySelector('#table thead tr th')!.innerHTML =
			'<input type="checkbox" class="ant-data-table-select-page" data-dt-action="all">';
		const table = new DynamicDataTable(cfg, action, null, null,
			{ rows: rows(1, 2), itemCount: 2 });
		const boxes = document.querySelectorAll<HTMLInputElement>('#table tbody input');
		const page = document.querySelector<HTMLInputElement>('#table thead input')!;

		boxes[0].click();
		expect(action).toHaveBeenLastCalledWith('select', 'k1');
		expect(page.indeterminate).toBe(true);
		expect(Array.from(table.selectedKeys)).toEqual(['k1']);

		page.click();
		expect(action).toHaveBeenLastCalledWith('all', null);
		expect(Array.from(boxes, (box) => box.checked)).toEqual([true, true]);
		expect(document.querySelectorAll('#table tbody tr.selected').length).toBe(2);

		boxes[1].click();
		expect(action).toHaveBeenLastCalledWith('deselect', 'k2');
	});

	it('hands the messages pushed for it to the handlers of their types', () => {
		const listeners: Array<(message: unknown) => void> = [];
		setHost({ ...standaloneHost, onMessage(listener) {
			listeners.push(listener);
			return () => undefined;
		} });
		const refresh = vi.fn();
		new DynamicDataTable(config({ push: true }), () => undefined, null, { refresh },
			{ rows: [], itemCount: 0 });

		listeners.forEach((listener) => listener('{"tableId":"other","typeId":"refresh"}'));
		listeners.forEach((listener) => listener('not JSON'));
		listeners.forEach((listener) => listener('{"tableId":"table","typeId":"refresh"}'));

		expect(refresh).toHaveBeenCalledTimes(1);
		expect(refresh.mock.calls[0][0]).toEqual({ tableId: 'table', typeId: 'refresh' });
	});

	it('stops when an update of the page removes it', () => {
		const removing: Array<(element: Element) => void> = [];
		setHost({ ...standaloneHost, onNodeRemoving(listener) {
			removing.push(listener);
			return () => undefined;
		} });
		new DynamicDataTable(config(), () => undefined, null, null, { rows: [], itemCount: 0 });

		removing.forEach((listener) => listener(document.body));

		expect(DynamicDataTable.instances).toEqual({});
	});

	it('scrolls the rows in a body of a height, and puts them back when it stops', () => {
		const table = new DynamicDataTable(config({ bodyHeight: { value: 200, unit: 'px' },
			filler: true }), () => undefined, null, null, { rows: rows(1, 2), itemCount: 2 });
		const body = document.querySelector<HTMLElement>('#table .ant-data-table-body')!;
		expect(body.style.height).toBe('200px');
		expect(body.querySelectorAll('tr').length).toBe(2);

		table.destroy();

		const table2 = document.querySelector('#table')!;
		expect(table2.querySelector('.ant-data-table-body')).toBeNull();
		expect(table2.querySelectorAll(':scope > tbody').length).toBe(1);
		expect(table2.querySelectorAll(':scope > tbody > tr').length).toBe(2);
	});

	it('starts again without the widths of its resizable columns when they stop being resizable', () => {
		const first = config({ bodyHeight: { value: 200, unit: 'px' }, resize: {},
			layout: { adjustToParent: true } });
		const resizable = new DynamicDataTable(first, () => undefined, null, null,
			{ rows: rows(1, 2), itemCount: 2 });
		ResizableColumns.attach('table', { mode: 'stretch', widths: [100, 300] });
		const element = document.querySelector<HTMLTableElement>('#table')!;
		expect(element.classList.contains('antilia-resizable-columns')).toBe(true);

		resizable.destroy();
		expect(ResizableColumns.instances.table).toBeUndefined();
		expect(element.querySelectorAll(':scope > colgroup').length).toBe(0);
		expect(element.classList.contains('antilia-resizable-columns')).toBe(false);
		expect(element.style.width).toBe('');

		new DynamicDataTable({ ...first, resize: undefined,
			layout: { adjustToParent: true, proportions: [1, 3] } }, () => undefined, null, null,
			{ rows: rows(1, 2), itemCount: 2 });
		expect(Array.from(element.querySelectorAll<HTMLElement>(':scope > colgroup col'),
			(col) => col.style.width)).toEqual(['25%', '75%']);

		// the shares leave the filler its width: browsers lay out calc() in a col as no width
		new DynamicDataTable({ ...first, resize: undefined, filler: true,
			layout: { adjustToParent: true, proportions: [1, 3] } }, () => undefined, null, null,
			{ rows: rows(1, 2), itemCount: 2 });
		expect(Array.from(element.querySelectorAll<HTMLElement>(':scope > colgroup col'),
			(col) => col.style.width)).toEqual(['25%', '75%', '3em']);
	});

	it('tells when the user changed a field of an element', () => {
		document.body.innerHTML = '<form><input name="a" value="x"><select name="b">' +
			'<option>1</option><option>2</option></select></form>';
		const form = document.querySelector('form')!;
		expect(DynamicDataTable.isChanged(form)).toBe(false);

		form.querySelector('select')!.selectedIndex = 1;
		expect(DynamicDataTable.isChanged(form)).toBe(true);
	});

	it('renders with Handlebars once it is registered, the meta as @data', () => {
		registerHandlebars({ compile: (template) => (data, options) =>
			template + ':' + (data as { name: string }).name + ':' +
			(options?.data as { number: number }).number });
		new DynamicDataTable(config({ engine: 'handlebars',
			columns: [{ template: 'T' }] }), () => undefined, null, null,
			{ rows: rows(1, 1), itemCount: 1 });

		expect(cells()).toEqual([['T:User 1:1']]);
	});
});
