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

import { describe, expect, it } from 'vitest';

import { buttonClasses, localRows, type DataTableRow } from '../src/index.js';

const ROWS: DataTableRow[] = [
	{ key: 'a', data: { name: 'Carla', age: 31 } },
	{ key: 'b', data: { name: 'ana', age: 9 } },
	{ key: 'c', data: { name: 'Bruno', age: 120 } },
	{ key: 'd', data: { name: 'Dora' } }
];

describe('localRows', () => {
	it('pages a list in its order, numbering the rows', async () => {
		const load = localRows(() => ROWS);

		const loaded = await load({ first: 1, count: 2, sort: null });

		expect(loaded.itemCount).toBe(4);
		expect(loaded.rows).toEqual([{ key: 'b', number: 2, data: ROWS[1].data },
			{ key: 'c', number: 3, data: ROWS[2].data }]);
	});

	it('sorts texts by locale and numbers as numbers, the missing values last', async () => {
		const load = localRows(() => ROWS);
		const keys = async (property: string, ascending: boolean) => (await load({ first: 0,
			count: 10, sort: { property, ascending } })).rows.map((row) => row.key);

		expect(await keys('name', true)).toEqual(['b', 'c', 'a', 'd']);
		expect(await keys('age', true)).toEqual(['b', 'a', 'c', 'd']);
		expect(await keys('age', false)).toEqual(['d', 'c', 'a', 'b']);
	});

	it('filters by any text or number, ignoring case, or as it is told', async () => {
		expect((await localRows(() => ROWS)({ first: 0, count: 10, sort: null, filter: ' AN ' }))
			.rows.map((row) => row.key)).toEqual(['b']);
		expect((await localRows(() => ROWS)({ first: 0, count: 10, sort: null, filter: '12' }))
			.rows.map((row) => row.key)).toEqual(['c']);

		const byAge = localRows(() => ROWS, (data, filter) =>
			String((data as { age?: number }).age) === filter);
		const loaded = await byAge({ first: 0, count: 10, sort: null, filter: '9' });
		expect(loaded.itemCount).toBe(1);
		expect(loaded.rows[0].key).toBe('b');
	});

	it('reads the list on every load', async () => {
		let list = ROWS.slice(0, 1);
		const load = localRows(() => list);
		expect((await load({ first: 0, count: 10, sort: null })).itemCount).toBe(1);

		list = ROWS;
		expect((await load({ first: 0, count: 10, sort: null })).itemCount).toBe(4);
	});
});

describe('buttonClasses', () => {
	it('names the variant and the size, secondary and medium by default', () => {
		expect(buttonClasses()).toBe('ant-button ant-button--secondary ant-button--medium');
		expect(buttonClasses({ variant: 'danger', size: 'small', iconOnly: true,
			className: 'mine' })).toBe(
			'ant-button ant-button--danger ant-button--small ant-button--icon-only mine');
	});
});
