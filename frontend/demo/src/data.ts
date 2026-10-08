/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * What the Wicket page gives the React and Svelte demos: the element they render into carries
 * the URL of the choices, a resource mounted by the demo application, and the texts in the
 * language of the session.
 */

import type { AutocompleteSuggestion, InfiniteListData } from '@advanced-widgets/core';

export interface Choice {
	value: string;
	label: string;
}

/** The choices served by the mounted resource, named in the language of the session. */
export interface Choices {
	countries: Choice[];
	skills: Choice[];
}

/** The texts of a demo, in the language of the session. */
export interface Texts {
	country: string;
	countryNone: string;
	skills: string;
	skillsNone: string;
	filter: string;
	noMatch: string;
	more: string;
	/** The label of the x of a chip, {0} standing for its text. */
	remove: string;
	/** What is chosen, with {0} for the country and {1} for the skills. */
	chosen: string;
	nothing: string;
	loading: string;
	colleagues: string;
	colleaguesNone: string;
	searching: string;
	people: string;
	listLoading: string;
	/** All users shown, ${count} standing for their number. */
	listEnd: string;
	listEmpty: string;
	/** Who was picked in the list, {0} standing for the user's ID. */
	picked: string;
}

/** Where a demo loads its data from: resources the Wicket application mounts. */
export interface Urls {
	/** The countries and the skills. */
	choices: string;
	/** The users, as {rows: [{key, number, data}], itemCount}, for first, count and filter. */
	users: string;
}

/** A user as the users resource sends it. */
export interface User {
	id: number;
	firstName: string;
	lastName: string;
	city: string;
	countryName: string;
}

/**
 * @param element the element a demo renders into
 * @returns the URL of the choices and the texts
 */
export function settingsOf(element: HTMLElement): { urls: Urls; texts: Texts } {
	return {
		urls: { choices: element.dataset.choices ?? '', users: element.dataset.users ?? '' },
		texts: JSON.parse(element.dataset.texts ?? '{}') as Texts
	};
}

/** The users of a page: rows of {key, number, data}, and how many there are in all. */
interface UserRows {
	rows: Array<{ key: string; number: number; data: User }>;
	itemCount: number;
}

async function loadUsers(url: string, first: number, count: number,
	filter?: string): Promise<UserRows> {
	const query = new URLSearchParams({ first: String(first), count: String(count) });
	if (filter) {
		query.set('filter', filter);
	}
	const response = await fetch(url + (url.indexOf('?') >= 0 ? '&' : '?') + query, {
		headers: { Accept: 'application/json' }
	});
	if (!response.ok) {
		throw new Error(`Loading ${url} failed: ${response.status}`);
	}
	return response.json() as Promise<UserRows>;
}

/**
 * @param url the URL of the users
 * @returns the search of an autocomplete: the first users whose name contains the text
 */
export function searchUsers(url: string): (term: string) => Promise<AutocompleteSuggestion[]> {
	return async (term) => (await loadUsers(url, 0, 8, term)).rows.map((row) => ({
		id: row.key,
		label: row.data.firstName + ' ' + row.data.lastName,
		data: row.data
	}));
}

/** The users shown or the next page of them, 20 at a time. */
const PAGE = 20;

/**
 * @param url the URL of the users
 * @returns the load of an infinite list of all users, remembering how many it shows
 */
export function pagesOfUsers(url: string): (more: boolean) => Promise<InfiniteListData> {
	let shown = 0;
	return async (more) => {
		const rows = await loadUsers(url, more ? shown : 0, more ? PAGE : Math.max(shown, PAGE));
		shown = (more ? shown : 0) + rows.rows.length;
		return { items: rows.rows, itemCount: rows.itemCount };
	};
}

/** The template of a user in the list; a click on the user picks it. */
export const USER_TEMPLATE = '<div class="framework-demo__user" data-dt-action="pick">' +
	'<b>{{firstName}} {{lastName}}</b> <span>{{city}}, {{countryName}}</span></div>';

/** The template of a user suggested by the autocomplete. */
export const SUGGESTION_TEMPLATE = '<b>{{firstName}} {{lastName}}</b> <small>{{city}}</small>';

/**
 * @param url the URL of the mounted resource
 * @returns the choices
 */
export async function loadChoices(url: string): Promise<Choices> {
	const response = await fetch(url, { headers: { Accept: 'application/json' } });
	if (!response.ok) {
		throw new Error(`Loading ${url} failed: ${response.status}`);
	}
	return response.json() as Promise<Choices>;
}

/**
 * @returns what is chosen, in words
 */
export function describe(texts: Texts, choices: Choices | null, country: string,
	skills: string[]): string {
	const label = (list: Choice[] | undefined, value: string) =>
		list?.find((choice) => choice.value === value)?.label ?? value;
	return texts.chosen
		.replace('{0}', country ? label(choices?.countries, country) : texts.nothing)
		.replace('{1}', skills.length ? skills.map((skill) => label(choices?.skills, skill))
			.join(', ') : texts.nothing);
}
