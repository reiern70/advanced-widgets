/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The JSON grid demo: the users in a DataTable whose rows are either a local list of JSON the page
 * gave, filtered, sorted and paged in the browser, or loaded from the users resource, which
 * creates, changes and deletes them too. Settings on top choose where the rows come from, paging
 * or infinite scrolling, the selection, its count and resizable columns. The actions of a row
 * edit, delete and download the user; the progress of the users loaded from the resource
 * advances as if the server pushed it.
 */

import type { DataTableColumnDef, DataTablePush, DataTableQuery, DataTableRow,
	DataTableTexts } from '@antilia/core';

import { loadUsers, type Choice, type User, type UserRow } from './data.js';
import type { UserFields, UserFormTexts } from './json-list.js';

/** Where the rows come from: a local list of JSON, or the users resource. */
export type RowsFrom = 'local' | 'resource';

/** How many rows can be selected. */
export type Selection = 'none' | 'one' | 'many';

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
	/** That a user was deleted, added or saved, {0} and {1} standing for the names. */
	deleted: string;
	added: string;
	saved: string;
	/** That the users resource failed, {0} standing for why. */
	failed: string;
	/** How many users are selected, ${count} standing for the number, as the Wicket grid says. */
	selectedCount: string;
	/** The settings, named and offered as the settings of the Wicket page. */
	rowsSource: string;
	'rowsSource.local': string;
	'rowsSource.RESOURCE': string;
	infiniteScroll: string;
	'infiniteScroll.off': string;
	'infiniteScroll.on': string;
	selection: string;
	'selection.NONE': string;
	'selection.ONE': string;
	'selection.MANY': string;
	selectionCount: string;
	'selectionCount.shown': string;
	'selectionCount.hidden': string;
	resize: string;
	'resize.NONE': string;
	'resize.STRETCH': string;
	/** What the table says, in the language of the session. */
	table: DataTableTexts;
}

/** What the page gives the demo. */
export interface JsonTableConfig {
	texts: JsonTableTexts;
	/** The columns of the Wicket grid, but its selection column, in its order. */
	columns: DataTableColumnDef[];
	/** The users resource: by first, count, filter and sort, and one by key to change it. */
	usersUrl: string;
	/** The users of the local list, as JSON. */
	localUsers: UserRow[];
	/** The choices, whose countries the form of a user offers. */
	choicesUrl: string;
	rowsPerPage: number;
	pageSizes: number[];
	bodyHeight: { value: number; unit: string };
	/** How often the progress of some users shown advances, in milliseconds. */
	pushMillis: number;
}

/** The settings of the grid, as the drop-downs on top choose them. */
export interface GridSettings {
	rowsFrom: RowsFrom;
	infinite: boolean;
	selection: Selection;
	selectionCount: boolean;
	resizable: boolean;
}

/** The settings the demo starts with, those the Wicket grid starts with. */
export const DEFAULT_SETTINGS: GridSettings = {
	rowsFrom: 'resource', infinite: false, selection: 'many', selectionCount: true, resizable: true
};

/** A setting: its key in the settings, its label and its choices, values as text. */
export interface SettingDef {
	key: keyof GridSettings;
	label: string;
	options: { value: string; label: string }[];
}

/**
 * @returns the drop-downs on top of the grid, in the language of the page
 */
export function settingDefs(texts: JsonTableTexts): SettingDef[] {
	return [
		{ key: 'rowsFrom', label: texts.rowsSource, options: [
			{ value: 'resource', label: texts['rowsSource.RESOURCE'] },
			{ value: 'local', label: texts['rowsSource.local'] }] },
		{ key: 'infinite', label: texts.infiniteScroll, options: [
			{ value: 'false', label: texts['infiniteScroll.off'] },
			{ value: 'true', label: texts['infiniteScroll.on'] }] },
		{ key: 'selection', label: texts.selection, options: [
			{ value: 'none', label: texts['selection.NONE'] },
			{ value: 'one', label: texts['selection.ONE'] },
			{ value: 'many', label: texts['selection.MANY'] }] },
		{ key: 'selectionCount', label: texts.selectionCount, options: [
			{ value: 'true', label: texts['selectionCount.shown'] },
			{ value: 'false', label: texts['selectionCount.hidden'] }] },
		{ key: 'resizable', label: texts.resize, options: [
			{ value: 'true', label: texts['resize.STRETCH'] },
			{ value: 'false', label: texts['resize.NONE'] }] }
	];
}

/**
 * @returns the settings with one of them chosen, from the value of its drop-down
 */
export function withSetting(settings: GridSettings, key: keyof GridSettings,
	value: string): GridSettings {
	const typed = typeof DEFAULT_SETTINGS[key] === 'boolean' ? value === 'true' : value;
	return { ...settings, [key]: typed };
}

/**
 * @returns how many users are selected, as the navigation shows it, or nothing for none
 */
export function selectedCount(texts: JsonTableTexts, count: number): string {
	return count ? texts.selectedCount.replace('${count}', String(count)) : '';
}

/**
 * @returns whether the first or the last name of a user contains the filter, as the users
 *          resource filters them
 */
export function matchesName(data: unknown, filter: string): boolean {
	const user = data as User;
	const lower = filter.toLocaleLowerCase();
	return user.firstName.toLocaleLowerCase().includes(lower) ||
		user.lastName.toLocaleLowerCase().includes(lower);
}

/**
 * The users of the local list: changed in the browser only, a new list after every change, so
 * the table shows it again.
 */
export class LocalUsers {
	private nextId: number;

	constructor(public rows: UserRow[]) {
		this.nextId = Math.max(0, ...rows.map((row) => row.data.id)) + 1;
	}

	get(key: string | null): User | undefined {
		return this.rows.find((row) => row.key === key)?.data;
	}

	/**
	 * Saves the fields of a user: a new one comes first.
	 *
	 * @returns the user saved
	 */
	save(key: string | null, fields: UserFields, countries: Choice[]): User {
		const named = { ...fields, countryName: countries.find((country) =>
			country.value === fields.country)?.label ?? fields.country,
		initials: (fields.firstName.charAt(0) + fields.lastName.charAt(0)).toUpperCase() };
		if (key === null) {
			const user = { id: this.nextId++, progress: 0, ...named } as User;
			this.rows = [{ key: String(user.id), number: 1, data: user }, ...this.rows];
			return user;
		}
		const user = { ...this.get(key)!, ...named };
		this.rows = this.rows.map((row) => row.key === key ? { ...row, data: user } : row);
		return user;
	}

	delete(key: string): void {
		this.rows = this.rows.filter((row) => row.key !== key);
	}
}

/** The answer of the users resource to a change: the user saved, by its key. */
interface Saved {
	key: string;
	data: User;
}

async function send(url: string, method: string, body?: unknown): Promise<Response> {
	const response = await fetch(url, { method, headers: { Accept: 'application/json',
		'Content-Type': 'application/json' }, body: body === undefined ? undefined :
		JSON.stringify(body) });
	if (!response.ok) {
		const error = await response.json().catch(() => null) as { error?: string } | null;
		throw new Error(error?.error ?? method + ' ' + url + ': ' + response.status);
	}
	return response;
}

function keyed(url: string, key: string): string {
	return url + (url.indexOf('?') >= 0 ? '&' : '?') + 'key=' + encodeURIComponent(key);
}

/**
 * The users of the users resource: loaded a page at a time, created, changed and deleted on the
 * server, as POST, PUT and DELETE of the resource. Remembers the users shown, whose progress the
 * pushes advance.
 */
export class ResourceUsers {
	private readonly shown = new Map<string, User>();

	constructor(private readonly url: string) {
	}

	get(key: string | null): User | undefined {
		return key === null ? undefined : this.shown.get(key);
	}

	/** The keys of the users shown. */
	shownKeys(): string[] {
		return Array.from(this.shown.keys());
	}

	/**
	 * Loads the rows the table asks for.
	 */
	readonly loadRows = async (query: DataTableQuery):
		Promise<{ rows: DataTableRow[]; itemCount: number }> => {
		const loaded = await loadUsers(this.url, { first: query.first, count: query.count,
			filter: query.filter, sort: query.sort?.property, ascending: query.sort?.ascending });
		if (query.first === 0) {
			this.shown.clear();
		}
		loaded.rows.forEach((row) => this.shown.set(row.key, row.data));
		return { rows: loaded.rows.map((row) => ({ key: row.key, data: row.data })),
			itemCount: loaded.itemCount };
	};

	/**
	 * Saves the fields of a user: creates it with a POST, or changes it with a PUT.
	 *
	 * @returns the user as the server saved it
	 */
	async save(key: string | null, fields: UserFields): Promise<User> {
		const response = await send(key === null ? this.url : keyed(this.url, key),
			key === null ? 'POST' : 'PUT', fields);
		const saved = await response.json() as Saved;
		this.shown.set(saved.key, saved.data);
		return saved.data;
	}

	/** Deletes a user with a DELETE. */
	async delete(key: string): Promise<void> {
		await send(keyed(this.url, key), 'DELETE');
		this.shown.delete(key);
	}

	/** Changes a user shown, as a pushed change does. */
	update(key: string, user: User): void {
		this.shown.set(key, user);
	}
}

/**
 * Advances the progress of some users the table shows now and then, as the server of the Wicket
 * demo pushes it over the web socket connection.
 *
 * @returns the subscription of a table to the pushes
 */
export function simulatedPushes(users: ResourceUsers, millis: number):
	(listener: (message: DataTablePush) => void) => () => void {
	return (listener) => {
		const timer = window.setInterval(() => {
			const keys = users.shownKeys();
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
