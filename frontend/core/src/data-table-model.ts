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

/*
 * What the React and Svelte tables share: their columns, queries, texts and pushed messages, the
 * selection column and the actions the core reports, as the Wicket tables name them.
 */

import type { DataTableColumn, DataTableRow } from './data-table.js';
import { ANTILIA_LOOK, partClasses, type Look } from './look.js';

/** A column of a React or Svelte table. */
export interface DataTableColumnDef {
	/** The id of the column, unique in the table. */
	id: string;
	/** The text of its header. */
	header: string;
	/** The template of its cells, rendered from the data of a row; {{@number}} is its number. */
	template: string;
	/** The CSS class of its cells. */
	cssClass?: string;
	/** The property the rows are sorted by when the user sorts by the column; not sortable without. */
	sort?: string;
	/** Whether the user can move the column. */
	movable?: boolean;
	/** Whether the user can resize the column; true by default. */
	resizable?: boolean;
	/**
	 * The narrowest the column gets, in pixels, when the columns are resized in 'stretch' mode and
	 * laid out for the width of the parent: a narrower parent, a phone for example, scrolls the
	 * table sideways rather than squeezing its columns.
	 */
	minWidth?: number;
	/** Whether the user can hide and show the column with the column chooser. */
	hideable?: boolean;
	/** Whether the column is shown at first; true by default. */
	visible?: boolean;
}

/** How the rows are sorted. */
export interface DataTableSort {
	property: string;
	ascending: boolean;
}

/** The rows a table asks for. */
export interface DataTableQuery {
	/** The index of the first row, counting from 0, and the number of rows. */
	first: number;
	count: number;
	sort: DataTableSort | null;
	filter?: string;
}

/** A change of the rows pushed to a table. */
export type DataTablePush =
	| { type: 'updateRow'; key: string; data: unknown }
	| { type: 'updateRows'; rows: DataTableRow[] }
	| { type: 'refresh' };

/** The texts of a table. */
export interface DataTableTexts {
	/** What the navigation says: {0} the first row shown, {1} the last one, {2} all of them. */
	showing: string;
	first: string;
	previous: string;
	next: string;
	last: string;
	/** The title of the link to a page, {0} standing for its number. */
	page: string;
	rowsPerPage: string;
	columns: string;
	apply: string;
	selectPage: string;
	selectRow: string;
	moveColumn: string;
	/** The accessible name of the handles resizing the columns. */
	resizeColumn: string;
	/** How many rows are selected, {0} standing for the number. */
	selectedCount: string;
	/** The title of the button of the count that deselects every row. */
	clearSelection: string;
}

/** The texts of a table in English, as the Wicket table has them. */
export const DATA_TABLE_TEXTS: DataTableTexts = {
	showing: 'Showing {0} to {1} of {2}',
	first: 'Go to first page',
	previous: 'Go to previous page',
	next: 'Go to next page',
	last: 'Go to last page',
	page: 'Go to page {0}',
	rowsPerPage: 'Rows per page',
	columns: 'Columns',
	apply: 'Apply',
	selectPage: 'Select all rows on this page',
	selectRow: 'Select row',
	moveColumn: 'Move the column: drag it, or use the arrow keys',
	resizeColumn: 'Resize column',
	selectedCount: '{0} selected',
	clearSelection: 'Clear selection'
};

/** The actions the core reports for the checkboxes: select, deselect, select and deselect the page. */
export const SELECTION_ACTIONS = ['_antilia-select', '_antilia-deselect', '_antilia-select-page',
	'_antilia-deselect-page'];

/** A waste bin, the icon of the button of the count that deselects every row (24 by 24, stroked). */
export const CLEAR_SELECTION_ICON = 'M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6';

/** The action the core reports when the column chooser applies, the ids of the columns as key. */
export const SHOW_COLUMNS = '_antilia-show-columns';

/** The number of links to pages the navigation shows at most, as Wicket's. */
export const PAGE_LINKS = 10;

/**
 * @param texts the label of the checkboxes
 * @param look the look of the table, whose classes the checkboxes and their column get
 * @returns the column of the checkboxes selecting the rows
 */
export function selectionColumn(texts: Pick<DataTableTexts, 'selectRow'>,
	look: Look = ANTILIA_LOOK): DataTableColumn {
	const label = texts.selectRow.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
		.replace(/</g, '&lt;');
	return {
		template: '<input type="checkbox" class="' + partClasses(look, 'table-select') +
			'" data-dt-action="' + SELECTION_ACTIONS[0] + '" data-dt-action-unchecked="' +
			SELECTION_ACTIONS[1] + '" aria-label="' + label + '">',
		cssClass: partClasses(look, 'table-selection')
	};
}

/**
 * @param page the page shown, counting from 0
 * @param pages the number of pages
 * @returns the pages the navigation links to: at most PAGE_LINKS around the one shown
 */
export function pageLinks(page: number, pages: number): number[] {
	const count = Math.min(PAGE_LINKS, pages);
	const start = Math.max(0, Math.min(page - Math.floor(count / 2), pages - count));
	return Array.from({ length: count }, (_, index) => start + index);
}

/** Rows a table loaded: those asked for and how many there are in all. */
export interface DataTableRows {
	rows: DataTableRow[];
	itemCount: number;
}

/**
 * Whether the data of a row matches the filter a table was given.
 */
export type DataTableFilter = (data: unknown, filter: string) => boolean;

/** Whether a text or number of the data contains the filter, ignoring case. */
const containsFilter: DataTableFilter = (data, filter) => {
	const wanted = filter.toLocaleLowerCase();
	return data !== null && typeof data === 'object' && Object.values(data).some((value) =>
		(typeof value === 'string' || typeof value === 'number') &&
		String(value).toLocaleLowerCase().includes(wanted));
};

function propertyOf(data: unknown, property: string): unknown {
	return data !== null && typeof data === 'object' ?
		(data as Record<string, unknown>)[property] : undefined;
}

/** Compares two values of a property, numbers as numbers, the missing ones last. */
function compare(a: unknown, b: unknown): number {
	if (a == null || b == null) {
		return a == null ? (b == null ? 0 : 1) : -1;
	}
	if (typeof a === 'number' && typeof b === 'number') {
		return a - b;
	}
	return String(a).localeCompare(String(b));
}

/**
 * The loadRows of a table showing a list it was given instead of loading its rows: the list is
 * filtered, sorted by the property of the data the sort names, and cut into the rows asked for, in
 * the browser. The list is read on every load, so the table shows what changed in it once it loads
 * again.
 *
 * @param rows the rows, read on every load
 * @param matches whether the data of a row matches the filter; by default whether one of its texts
 * or numbers contains it, ignoring case
 * @returns the loadRows of the table
 */
export function localRows(rows: () => readonly DataTableRow[],
	matches: DataTableFilter = containsFilter): (query: DataTableQuery) => Promise<DataTableRows> {
	return async (query) => {
		const filter = query.filter?.trim() ?? '';
		let found = filter ? rows().filter((row) => matches(row.data, filter)) : rows().slice();
		const sort = query.sort;
		if (sort) {
			const direction = sort.ascending ? 1 : -1;
			found = found.map((row, index) => ({ row, index })).sort((a, b) =>
				direction * compare(propertyOf(a.row.data, sort.property),
					propertyOf(b.row.data, sort.property)) || a.index - b.index)
				.map((sorted) => sorted.row);
		}
		return {
			rows: found.slice(query.first, query.first + query.count).map((row, index) => ({
				key: row.key, data: row.data, number: query.first + index + 1 })),
			itemCount: found.length
		};
	};
}
