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
 * The data the apps load from the resources the demo application mounts, and what they share.
 */

import type { AutocompleteSuggestion, InfiniteListData } from '@antilia/core';

export interface Choice {
	value: string;
	label: string;
}

/** The choices served by the choices resource, named in the language of the session. */
export interface Choices {
	countries: Choice[];
	skills: Choice[];
}

/** The texts of the drop-downs, as the Wicket ones have them. */
export interface DropdownTexts {
	filter: string;
	noMatch: string;
	more: string;
	/** The label of the x of a chip, {0} standing for its text. */
	remove: string;
}

/**
 * @return the texts of the drop-downs out of those of a demo, for the props of a Dropdown
 */
export function dropdownTexts(texts: DropdownTexts): DropdownTexts {
	const { filter, noMatch, more, remove } = texts;
	return { filter, noMatch, more, remove };
}

/** A user as the users resource sends it. */
export interface User {
	id: number;
	firstName: string;
	lastName: string;
	homePhone?: string;
	cellPhone?: string;
	born?: string;
	initials?: string;
	address?: string;
	city: string;
	country?: string;
	countryName: string;
	progress?: number;
}

/** A row of users: its key, its number counting from 1, and the user. */
export interface UserRow {
	key: string;
	number: number;
	data: User;
}

/** The users of a page and how many there are in all. */
export interface UserRows {
	rows: UserRow[];
	itemCount: number;
}

/** What to ask the users resource for. */
export interface UsersQuery {
	first: number;
	count: number;
	/** The text the first or the last name contains. */
	filter?: string;
	/** The property the users are sorted by, ascending unless told otherwise. */
	sort?: string;
	ascending?: boolean;
}

/**
 * @param pattern a text with {0}, {1}, ... standing for the values, as Wicket's resources have
 * @returns the text with the values in place
 */
export function format(pattern: string, ...values: Array<string | number>): string {
	return pattern.replace(/\{(\d+)\}/g, (match, index: string) =>
		index in values ? String(values[Number(index)]) : match);
}

/**
 * @param url the URL of a mounted resource
 * @returns what it answers, as JSON
 */
export async function fetchJson<T>(url: string): Promise<T> {
	const response = await fetch(url, { headers: { Accept: 'application/json' } });
	if (!response.ok) {
		throw new Error(`Loading ${url} failed: ${response.status}`);
	}
	return response.json() as Promise<T>;
}

/**
 * @param url the URL of the choices resource
 * @returns the choices
 */
export function loadChoices(url: string): Promise<Choices> {
	return fetchJson<Choices>(url);
}

/**
 * @param url the URL of the users resource
 * @returns the users asked for
 */
export function loadUsers(url: string, query: UsersQuery): Promise<UserRows> {
	const parameters = new URLSearchParams({ first: String(query.first),
		count: String(query.count) });
	if (query.filter) {
		parameters.set('filter', query.filter);
	}
	if (query.sort) {
		parameters.set('sort', query.sort);
		parameters.set('ascending', String(query.ascending ?? true));
	}
	return fetchJson<UserRows>(url + (url.indexOf('?') >= 0 ? '&' : '?') + parameters);
}

/**
 * @param url the URL of the users
 * @param max the number of users suggested at most
 * @returns the search of an autocomplete: the first users whose name contains the text
 */
export function searchUsers(url: string, max = 8):
	(term: string) => Promise<AutocompleteSuggestion[]> {
	return async (term) => (await loadUsers(url, { first: 0, count: max, filter: term })).rows
		.map((row) => ({
			id: row.key,
			label: row.data.firstName + ' ' + row.data.lastName,
			data: row.data
		}));
}

/**
 * @param url the URL of the users
 * @param query what else to ask for: the filter and the sort
 * @param page how many users to load at a time
 * @returns the load of an infinite list of the users, remembering how many it shows
 */
export function pagesOfUsers(url: string, query: () => Omit<UsersQuery, 'first' | 'count'> = () =>
	({}), page = 20): (more: boolean) => Promise<InfiniteListData & { items: UserRow[] }> {
	let shown = 0;
	return async (more) => {
		const rows = await loadUsers(url, { ...query(), first: more ? shown : 0,
			count: more ? page : Math.max(shown, page) });
		shown = (more ? shown : 0) + rows.rows.length;
		return { items: rows.rows, itemCount: rows.itemCount };
	};
}

/**
 * @returns the label of the choice of a value, or the value when there is no such choice
 */
export function labelOf(choices: Choice[] | undefined, value: string): string {
	return choices?.find((choice) => choice.value === value)?.label ?? value;
}
