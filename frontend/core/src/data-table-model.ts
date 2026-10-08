/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * What the React and Svelte tables share: their columns, queries, texts and pushed messages, the
 * selection column and the actions the core reports, as the Wicket tables name them.
 */

import type { DataTableColumn, DataTableRow } from './data-table.js';

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
	/** The placeholder shown without a license key activating the table. */
	unlicensed: string;
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
	unlicensed: 'This is a private component of UI Advanced Widgets: it needs a license key, ' +
		'which you get by registering.'
};

/**
 * The names a license key activates the React and Svelte tables by: their own and those of the
 * Wicket table they render as.
 */
export const DATA_TABLE_COMPONENTS = ['DataTable', 'JsonBasedDynamicDataTable',
	'BaseDynamicDataTable'];

/** The actions the core reports for the checkboxes: select, deselect, select and deselect the page. */
export const SELECTION_ACTIONS = ['_wicket-select', '_wicket-deselect', '_wicket-select-page',
	'_wicket-deselect-page'];

/** The action the core reports when the column chooser applies, the ids of the columns as key. */
export const SHOW_COLUMNS = '_wicket-show-columns';

/** The number of links to pages the navigation shows at most, as Wicket's. */
export const PAGE_LINKS = 10;

/**
 * @returns the column of the checkboxes selecting the rows
 */
export function selectionColumn(texts: Pick<DataTableTexts, 'selectRow'>): DataTableColumn {
	const label = texts.selectRow.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
		.replace(/</g, '&lt;');
	return {
		template: '<input type="checkbox" class="dynamic-data-table-select" data-dt-action="' +
			SELECTION_ACTIONS[0] + '" data-dt-action-unchecked="' + SELECTION_ACTIONS[1] +
			'" aria-label="' + label + '">',
		cssClass: 'dynamic-data-table-selection'
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
