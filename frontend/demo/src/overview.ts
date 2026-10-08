/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The combined demo of the React and Svelte page: a country, skills and colleagues, and a list of
 * all users.
 */

import { labelOf, type Choices, type DropdownTexts } from './data.js';

/** The texts of the overview, in the language of the session. */
export interface OverviewTexts extends DropdownTexts {
	country: string;
	countryNone: string;
	skills: string;
	skillsNone: string;
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

/** What the page gives the overview. */
export interface OverviewConfig {
	/** The countries and the skills. */
	choicesUrl: string;
	/** The users, as {rows: [{key, number, data}], itemCount}, for first, count and filter. */
	usersUrl: string;
	texts: OverviewTexts;
}

/** The template of a user in the list; a click on the user picks it. */
export const USER_TEMPLATE = '<div class="framework-demo__user" data-dt-action="pick">' +
	'<b>{{firstName}} {{lastName}}</b> <span>{{city}}, {{countryName}}</span></div>';

/** The template of a user suggested by the autocomplete. */
export const SUGGESTION_TEMPLATE = '<b>{{firstName}} {{lastName}}</b> <small>{{city}}</small>';

/**
 * @returns what is chosen, in words
 */
export function describe(texts: OverviewTexts, choices: Choices | null, country: string,
	skills: string[]): string {
	return texts.chosen
		.replace('{0}', country ? labelOf(choices?.countries, country) : texts.nothing)
		.replace('{1}', skills.length ? skills.map((skill) => labelOf(choices?.skills, skill))
			.join(', ') : texts.nothing);
}
