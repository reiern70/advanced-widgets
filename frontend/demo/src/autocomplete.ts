/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Autocomplete demo: an owner, a team and countries.
 */

import type { AutocompleteSuggestion, AutocompleteValue } from '@advanced-widgets/core';

import { format, loadChoices, type Choice } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface AutocompleteDemoTexts {
	single: string;
	multiple: string;
	owner: string;
	'owner.placeholder': string;
	team: string;
	'team.placeholder': string;
	countries: string;
	'countries.placeholder': string;
	send: string;
	nothing: string;
	/** What is chosen: {0} the owner, {1} the team and {2} the countries. */
	choices: string;
	/** What was sent, {0} standing for it. */
	sent: string;
	/** That the owner is required, as Wicket says it. */
	required: string;
	noMatch: string;
	searching: string;
	/** The label of the x of a chip, {0} standing for its text. */
	remove: string;
}

/** The values of the demo, as the page has them. */
export interface AutocompleteValues {
	owner: AutocompleteValue[];
	team: AutocompleteValue[];
	countries: AutocompleteValue[];
}

/** What the page gives the demo. */
export interface AutocompleteConfig {
	texts: AutocompleteDemoTexts;
	/** The template of a suggested user, as the Wicket page has it. */
	userTemplate: string;
	/** The template of a suggested country: its flag and name. */
	countryTemplate: string;
	values: AutocompleteValues;
	/** The users, searched by their first or last name. */
	usersUrl: string;
	/** The choices, whose countries are filtered in the browser. */
	choicesUrl: string;
}

/** The number of suggestions, as the Wicket autocomplete has it. */
export const MAX_SUGGESTIONS = 10;

/**
 * @param code the two letters of a country
 * @returns its flag: the letters as regional indicator symbols
 */
export function flag(code: string): string {
	return String.fromCodePoint(...Array.from(code.toUpperCase(),
		(letter) => 0x1F1E6 + letter.charCodeAt(0) - 65));
}

/**
 * @param url the URL of the choices
 * @returns the search of the countries: those whose name contains the text, loaded once
 */
export function searchCountries(url: string): (term: string) => Promise<AutocompleteSuggestion[]> {
	let countries: Promise<Choice[]> | undefined;
	return async (term) => {
		countries ??= loadChoices(url).then((choices) => choices.countries);
		const lower = term.toLowerCase();
		return (await countries)
			.filter((country) => country.label.toLowerCase().includes(lower))
			.slice(0, MAX_SUGGESTIONS)
			.map((country) => ({ id: country.value, label: country.label,
				data: { code: country.value, name: country.label, flag: flag(country.value) } }));
	};
}

/**
 * @returns what is chosen, in words, as the Wicket page says it
 */
export function describe(texts: AutocompleteDemoTexts, values: AutocompleteValues): string {
	const list = (chosen: AutocompleteValue[]) => chosen.length ?
		chosen.map((value) => value.label).join(', ') : texts.nothing;
	return format(texts.choices, list(values.owner), list(values.team), list(values.countries));
}
