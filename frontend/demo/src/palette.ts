/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */

/*
 * The Palette demo: skills in any order, the columns of a report in their own order while it
 * matters, under titles of their own and with numbered rows, some 250 countries drawn 30 rows at a
 * time, four palettes of them whose filters match in different ways, and a disabled palette.
 */

import type { PaletteTexts } from '@antilia/core';

import { format, labelOf, type Choice } from './data.js';

/** The texts of the demo, by the keys of the page's bundle. */
export interface PaletteDemoTexts {
	unordered: string;
	orderedTitle: string;
	disabledTitle: string;
	manyTitle: string;
	countries: string;
	/** How many countries are chosen, {0}, and which, {1}. */
	countriesChosen: string;
	skills: string;
	columns: string;
	ordered: string;
	numbered: string;
	filterable: string;
	/** The titles of the lists of the columns' palette. */
	columnsAvailable: string;
	columnsSelected: string;
	/** The palettes matching from the start of the names, anywhere and at their end. */
	matchTitle: string;
	matchHint: string;
	matchStart: string;
	matchAnywhere: string;
	matchEnd: string;
	matchWords: string;
	send: string;
	nothing: string;
	/** What is chosen: {0} the skills and {1} the columns. */
	choices: string;
	/** What was sent, {0} standing for it. */
	sent: string;
}

/** The choices of the demo, as the page has them. */
export interface PaletteValues {
	skills: string[];
	columns: string[];
	ordered: boolean;
	numbered: boolean;
	filterable: boolean;
	countries: string[];
	disabled: string[];
}

/** What the page gives the demo. */
export interface PaletteConfig {
	texts: PaletteDemoTexts;
	/** The texts of the palettes, as the Wicket ones have them. */
	palette: PaletteTexts;
	skills: Choice[];
	columns: Choice[];
	/** The countries, in the order of their names. */
	countries: Choice[];
	values: PaletteValues;
}

/** How the filters of the palettes of the countries' matching example match, and their label. */
export const MATCHES: ReadonlyArray<readonly ['start' | 'anywhere' | 'end' | 'words',
	'matchStart' | 'matchAnywhere' | 'matchEnd' | 'matchWords']> = [['start', 'matchStart'],
	['anywhere', 'matchAnywhere'], ['end', 'matchEnd'], ['words', 'matchWords']];

/**
 * @returns what is chosen, in words, as the Wicket page says it
 */
export function describe(config: PaletteConfig, values: PaletteValues): string {
	const { texts } = config;
	const list = (chosen: string[], choices: Choice[]) => chosen.length ?
		chosen.map((value) => labelOf(choices, value)).join(', ') : texts.nothing;
	return format(texts.choices, list(values.skills, config.skills),
		list(values.columns, config.columns));
}

/**
 * @returns how many countries are chosen, and which, as the Wicket page says it
 */
export function describeCountries(config: PaletteConfig, countries: string[]): string {
	return format(config.texts.countriesChosen, String(countries.length), countries.length ?
		countries.map((value) => labelOf(config.countries, value)).join(', ') :
		config.texts.nothing);
}
