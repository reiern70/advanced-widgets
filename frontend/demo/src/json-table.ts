/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The JSON table demo: the users in a DataTable, from the users resource, filtered, sorted, paged
 * or scrolled infinitely; the actions of a row edit, delete and download the user in the app,
 * which keeps what it changes in the browser. The progress of the users shown advances as if the
 * server pushed it.
 */

import type { DataTableColumnDef, DataTablePush, DataTableQuery, DataTableRow,
	DataTableTexts } from '@advanced-widgets/core';

import { loadUsers, type User } from './data.js';
import { UserChanges, type UserFormTexts } from './json-list.js';

/** The texts of the demo, by the keys of the page's bundles. */
export interface JsonTableTexts extends UserFormTexts {
	filterLabel: string;
	'filter.placeholder': string;
	newUser: string;
	/** The title of the form of a user, ${firstName} and ${lastName} standing for the names. */
	editUser: string;
	delete: string;
	/** Whether to delete a user, ${firstName} and ${lastName} standing for the names. */
	'delete.confirm': string;
	/** That a user was deleted or added, {0} and {1} standing for the names. */
	deleted: string;
	added: string;
	/** What is selected: nothing, or {0} users. */
	'selection.none': string;
	'selection.some': string;
	/** Whether the rows are paged or scroll infinitely, as the options of the Wicket page say. */
	infiniteScroll: string;
	'infiniteScroll.off': string;
	'infiniteScroll.on': string;
	/** What the table says, in the language of the session. */
	table: DataTableTexts;
}

/** What the page gives the demo. */
export interface JsonTableConfig {
	texts: JsonTableTexts;
	/** The columns of the Wicket table, but its selection column, in its order. */
	columns: DataTableColumnDef[];
	/** The users, by first, count, filter and sort. */
	usersUrl: string;
	/** The choices, whose countries the form of a user offers. */
	choicesUrl: string;
	/** The license key of the application, activating the table in the browser. */
	licenseKey: string;
	rowsPerPage: number;
	pageSizes: number[];
	bodyHeight: { value: number; unit: string };
	/** How often the progress of some users shown advances, in milliseconds. */
	pushMillis: number;
}

/**
 * The users of the table: loaded from the users resource with what the app changed applied, the
 * users added first on the first page.
 */
export class TableUsers extends UserChanges {
	/** The keys of the rows the table shows. */
	shownKeys: string[] = [];

	constructor(private readonly url: string) {
		super();
	}

	/**
	 * Loads the rows the table asks for.
	 */
	readonly loadRows = async (query: DataTableQuery):
		Promise<{ rows: DataTableRow[]; itemCount: number }> => {
		const filter = query.filter ?? '';
		const loaded = await loadUsers(this.url, { first: query.first, count: query.count, filter,
			sort: query.sort?.property, ascending: query.sort?.ascending });
		const rows = this.apply(loaded.rows, filter, query.first === 0)
			.map((row) => ({ key: row.key, data: row.data }));
		this.shownKeys = query.first === 0 || !this.shownKeys.length ? rows.map((row) => row.key) :
			this.shownKeys.concat(rows.map((row) => row.key));
		return { rows, itemCount: this.itemCount(loaded.itemCount, filter) };
	};
}

/**
 * Advances the progress of some users the table shows now and then, as the server of the Wicket
 * demo pushes it over the web socket connection.
 *
 * @returns the subscription of a table to the pushes
 */
export function simulatedPushes(users: TableUsers, millis: number):
	(listener: (message: DataTablePush) => void) => () => void {
	return (listener) => {
		const timer = window.setInterval(() => {
			const keys = users.shownKeys;
			for (let i = 0; i < Math.min(3, keys.length); i++) {
				const key = keys[Math.floor(Math.random() * keys.length)];
				const user = users.get(key);
				if (user && (user.progress ?? 0) < 100) {
					const pushed = { ...user, progress: Math.min(100, (user.progress ?? 0) +
						5 + Math.floor(Math.random() * 15)) };
					users.update(key, pushed);
					listener({ type: 'updateRow', key, data: pushed });
				}
			}
		}, millis);
		return () => window.clearInterval(timer);
	};
}

/**
 * @returns the user as a jCard (RFC 7095), as the Wicket demo downloads it
 */
export function jCard(user: User): unknown {
	const properties: unknown[] = [
		['version', {}, 'text', '4.0'],
		['fn', {}, 'text', user.firstName + ' ' + user.lastName],
		['n', {}, 'text', [user.lastName, user.firstName, '', '', '']]
	];
	if (user.born) {
		properties.push(['bday', {}, 'date', user.born]);
	}
	if (user.homePhone) {
		properties.push(['tel', { type: ['home', 'voice'] }, 'uri', 'tel:' + user.homePhone]);
	}
	if (user.cellPhone) {
		properties.push(['tel', { type: ['cell', 'voice'] }, 'uri', 'tel:' + user.cellPhone]);
	}
	properties.push(['adr', {}, 'text', ['', '', user.address ?? '', user.city ?? '', '', '',
		user.countryName ?? '']]);
	return ['vcard', properties];
}

/**
 * Has the browser save the jCard of a user, as user-<id>.json.
 */
export function download(user: User): void {
	const blob = new Blob([JSON.stringify(jCard(user))], { type: 'application/vcard+json' });
	const url = URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = 'user-' + user.id + '.json';
	document.body.appendChild(link);
	link.click();
	link.remove();
	window.setTimeout(() => URL.revokeObjectURL(url), 0);
}
